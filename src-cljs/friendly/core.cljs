(ns friendly.core
  (:require [reagent.core :as reagent :refer [atom]]
            [ajax.core :refer [GET POST]]
            [secretary.core :as secretary :include-macros true :refer [defroute]]
            [goog.events :as events])
  (:import goog.History
           goog.history.EventType))

(enable-console-print!)

;; GLOBAL STATE ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def user (atom {}))

;; HELPERS ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn by-id [elem-id]
  (.getElementById js/document elem-id))

(defn error-handler [response]
  (.log js/console (str "ERROR: " response)))

;; UI ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn active? [screen current]
  (if (= screen current) "active" ""))

(defn toolbar []
  [:div.container-fluid
   [:div.navbar-header
    [:button.navbar-toggle.collapsed
     {:type "button" :data-toggle "collapse" :data-target ".navbar-collapse"}
     [:span.sr-only "Toggle navigation"]
     [:span.icon-bar] [:span.icon-bar] [:span.icon-bar]]
    [:a.navbar-brand {:href "#"} "Friendly Reader"]]
   [:div {:class "navbar-collapse collapse"}
    (when (@user "email")
      [:ul.nav.navbar-nav.navbar-right
       [:li.dropdown
        [:a.dropdown-toggle {:href "" :data-toggle "dropdown"}
         [:img {:height 24 :width 24 :src (@user "gravatar")
                :style {:margin "0px 8px 0px"}}
          (@user "email")
          [:span.caret ""]]
         [:ul.dropdown-menu {:role "menu"}
          [:li
           [:a {:href "/logout"}
            [:span {:class "glyphicon glyphicon-off"}] " Logout"]]]]]])
    ]
   ])

;; ROUTING ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(secretary/set-config! :prefix "#")

(defroute home-path "/home" []
  (if (@user "email")
    (swap! user assoc :screen :home)
    (GET "/api/userinfo" {:handler (fn [data]
                                     (println (str data))
                                     (reset! user (assoc data :screen :home)))
                          :error-handler (fn [response]
                                           (reset! user {:screen :home}))
                          ;; :response-format :json
                          ;; :keywords? true
                          })))

(defroute "*" []
  (secretary/dispatch! "/home"))

;; Quick and dirty history configuration.
(let [h (History.)]
  (goog.events/listen h EventType.NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h
    (.setEnabled true)))

(secretary/dispatch! "/")

;; SCREEN LOGIC ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn message-screen [message]
  [:div
   [:div.row
    [:div.alert.alert-info message]]])

(defn main-screen []
  (message-screen (str "Main screen for user " (@user "email"))))

(defn login-screen []
  [:div.container {:style {:padding-top "20px"}}
   [:div.jumbotron
    [:h1 "Friendly Reader"]
    [:p.lead "An user friendly reader to follow the news of your favourite blogs and websites."]
    [:p.lead "Written in Clojure, no registration required."]
    [:p
     [:a.btn.btn-lg.btn-danger {:href "/login/google" :role "button"}
      [:i.glyphicon.glyphicon-user ""] " Log in with Google"]
     ]]])

(defn home-screen []
  [:div
   (if (@user "email")
     (main-screen)
     (login-screen))
   ])

(defn show-app []
  (let [screen (:screen @user)
        screen-fns {:home home-screen :message message-screen}
        screen-fn (get screen-fns screen home-screen)]
    (screen-fn)))

;; MAIN APP ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(reagent/render-component (fn [] [toolbar]) (by-id "navbar"))
(reagent/render-component (fn [] [show-app]) (by-id "app"))
