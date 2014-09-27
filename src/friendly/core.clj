(ns friendly.core
  (:require [cemerick.friend :as friend]
            (cemerick.friend [openid :as openid]
                             [workflows :as workflows])
            [ring.middleware.reload :as reload]
            [ring.util.response :as response])
  (:use [compojure.core :only [defroutes GET POST DELETE]]
        [ring.middleware.file-info :only [wrap-file-info]]
        [ring.middleware.json :only [wrap-json-response wrap-json-body]]
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.resource :only [wrap-resource]]
        [ring.middleware.session :only [wrap-session]]
        [org.httpkit.server :only [run-server]]))

;; HELPERS ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn in-dev? []
  (= "denis" (System/getenv "USER")))

;; ROUTES ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defroutes routes
  (GET "/" []
       {:status 200
        :body (slurp "resources/public/index.html")})

  (GET "/logout" request
       (friend/logout* (response/redirect "/")))

  )

;; MIDDLEWARE ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def friend-configuration
  {:allow-anon? true
   :default-landing-uri "/"
   :workflows [(openid/workflow
                :openid-uri "/login-openid"
                :realm (if in-dev? "http://localhost:3000" "http://friendly.clojurecup.com/")
                :credential-fn identity)]})

(defn wrap-logging [handler]
  (fn [{:keys [remote-addr request-method uri] :as request}]
    (println remote-addr (.toUpperCase (name request-method)) uri)
    (handler request)))

(def app (-> routes
             (friend/authenticate friend-configuration)
             (wrap-resource "public") ;; serve from "resources/public"
             (wrap-resource "/META-INF/resources") ;; resources from WebJars
             (wrap-json-body {:keywords? true})
             wrap-json-response
             wrap-params
             wrap-file-info ;; sends the right headers for static files
             wrap-session ;; required fof openid to save data in the session
             wrap-logging))

;; HTTP-Kit based

(defn -main [& args] ;; entry point, lein run will pick up and start from here
  (let [handler (if in-dev?
                  (reload/wrap-reload app) ;; only reload when dev
                  app)]
    (println "Running server...")
    (run-server handler {:port 3000})))
