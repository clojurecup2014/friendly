(ns friendly.core
  (:require [clj-http.client :as client]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [cemerick.friend :as friend]
            [friend-oauth2.workflow :as oauth2]
            [friend-oauth2.util :refer [format-config-uri]]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [ring.adapter.jetty :as jetty]
            [ring.middleware.reload :as reload]
            [ring.util.response :as response])
  (:use [ring.middleware.file-info :only [wrap-file-info]]
        [ring.middleware.json :only [wrap-json-response wrap-json-body]]
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.resource :only [wrap-resource]]
        [ring.middleware.session :only [wrap-session]]
        [org.httpkit.server :only [run-server]])

  (:import [java.security MessageDigest]))

;; HELPERS ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; MD5 helper from https://gist.github.com/jizhang/4325757
(defn md5 [s]
  (let [algorithm (MessageDigest/getInstance "MD5")
        size (* 2 (.getDigestLength algorithm))
        raw (.digest algorithm (.getBytes s))
        sig (.toString (BigInteger. 1 raw) 16)
        padding (apply str (repeat (- size (count sig)) "0"))]
    (str padding sig)))

(defn gravatar [email]
  (str "https://secure.gravatar.com/avatar/" (md5 email) "?s=24"))

(def config
  (read-string (slurp "resources/config.edn")))

(defn in-dev? []
  (= :development (:env config)))

;; Default options for clj-http client
(def clj-http-opts
  {:as :json, :coerce :always, :content-type :json, :throw-exceptions :false})

;; OAUTH2 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Don't forget to configure the Google consent screen for this app
;; See also http://stackoverflow.com/a/25762444/483566

(defn credential-fn
  "Looks for the user email using the Google+ API after login with Google"
  [token]
  (let [access-token (:access-token token)
        gplus-addr "https://www.googleapis.com/plus/v1/people/me?access_token="
        gplus-info (client/get (str gplus-addr access-token) clj-http-opts)
        email (-> gplus-info :body :emails first :value)]
    {:identity token :email email :roles #{::user}}))

(def client-config
  {;; APP: friendly-reader-777
   :client-id "981371857470-e6nscou0m1393krkrgttqqmtcr6ne36e.apps.googleusercontent.com"
   :client-secret "5TTsogg_Z-5kxTusdXy7E208"
   :callback {:domain "http://localhost:3000" :path "/oauth2callback"}})

(def uri-config
  {:authentication-uri {:url "https://accounts.google.com/o/oauth2/auth"
                        :query {:client_id (:client-id client-config)
                                :response_type "code"
                                :redirect_uri (format-config-uri client-config)
                                :scope "https://www.googleapis.com/auth/userinfo#email"
                                }}

   :access-token-uri {:url "https://accounts.google.com/o/oauth2/token"
                      :query {:client_id (:client-id client-config)
                              :client_secret (:client-secret client-config)
                              :grant_type "authorization_code"
                              :redirect_uri (format-config-uri client-config)}}})

(defn session-email
  "Find the email stored in the session"
  [request]
  (let [token (get-in request [:session :cemerick.friend/identity :current :access-token])]
    (get-in request [:session :cemerick.friend/identity :authentications
                     {:access-token token}
                     :email])))

;; ROUTES ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defroutes ring-app

  (GET "/" request
       (println "Session in '/' contains:" (:session request))
       {:status 200
        :body (slurp "resources/public/index.html")})

  (GET "/api/userinfo" request
       (let [token (get-in request [:session :cemerick.friend/identity :current :access-token])
             email (session-email request)
             gravatar (gravatar email)]
         (friend/authorize #{::user}
                           {:status 200
                            :body {:email email :token token :gravatar gravatar}})))

  ;; Just login to obtain your email info in credential-fn and redirect to the root
  (GET "/login/google" request
       (friend/authorize #{::user} (response/redirect "/")))

  (friend/logout (ANY "/logout" request (ring.util.response/redirect "/"))))

;; MIDDLEWARE ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def friend-configuration
  {:allow-anon? true
   :workflows [(oauth2/workflow
                {:client-config client-config
                 :uri-config uri-config
                 :credential-fn credential-fn})]})

(defn wrap-logging [handler]
  (fn [{:keys [remote-addr request-method uri] :as request}]
    (println remote-addr (.toUpperCase (name request-method)) uri)
    (handler request)))

(def app (-> ring-app
              (friend/authenticate friend-configuration)
              (wrap-resource "public") ;; serve from "resources/public"
              (wrap-resource "/META-INF/resources") ;; resources from WebJars
              (wrap-json-body {:keywords? true})
              wrap-json-response
              wrap-params
              wrap-file-info ;; sends the right headers for static files
              wrap-logging
              wrap-session ;; required fof openid to save data in the session
              handler/site))

;; HTTP-Kit based

(defn -main [& args] ;; entry point, lein run will pick up and start from here
  (let [handler (if (in-dev?)
                  (reload/wrap-reload app) ;; only reload when dev
                  app)]
    (println "Running Friendly HTTP Kit server...")
    (run-server app {:port 3000})))

