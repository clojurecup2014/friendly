(ns friendly.core
  (:require [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

;; HELPERS ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn by-id [elem-id]
  (.getElementById js/document elem-id))

;; GLOBAL STATE ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def user (atom {:email "denis@clojurecup.com"
                 :icon "https://secure.gravatar.com/avatar/d9dbd0d79255e58265b5f7597e15eb9a?s=24"}))

(defn toolbar []
  [:div.container-fluid
   [:div.navbar-header
    [:button.navbar-toggle.collapsed
     {:type "button" :data-toggle "collapse" :data-target ".navbar-collapse"}
     [:span.sr-only "Toggle navigation"]
     [:span.icon-bar] [:span.icon-bar] [:span.icon-bar]]
    [:a.navbar-brand {:href "#"} "Friendly Reader"]]
   [:div {:class "navbar-collapse collapse"}
    (when (:email @user)
      [:ul.nav.navbar-nav.navbar-right
       [:li.dropdown
        [:a.dropdown-toggle {:href "" :data-toggle "dropdown"}
         [:img {:height 24 :width 24 :src (:icon @user) :style {:margin "0px 8px 0px"}}
          (:email @user)
          [:span.caret ""]]
         [:ul.dropdown-menu {:role "menu"}
          [:li
           [:a {:href "/logout"}
            [:span {:class "glyphicon glyphicon-off"}] " Logout"]]]]]])
    ]
   ])


;; MAIN APP ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(reagent/render-component (fn [] [toolbar]) (by-id "navbar"))
