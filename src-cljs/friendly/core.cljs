(ns friendly.core
  (:require [reagent.core :as reagent :refer [atom]]
            [ajax.core :refer [GET POST]]
            [secretary.core :as secretary :include-macros true :refer [defroute]]
            [goog.events :as events]
            [hickory.core :as hickory])
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

(defn reset-location! [fragment] ;; eg "#/home"
  (set! (.-href (.-location js/document)) fragment))

(declare discover-feed remove-feed)

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
   [:div.navbar-collapse.collapse

    (when (@user "email")

      [:ul.nav.navbar-nav.navbar-right

       [:li
        [:form.navbar-form {:role "form"}
         [:div.form-group {:style {:padding-right "10px"}}
          [:a.btn.btn-success {:href "#/add"}
           [:span.fa.fa-plus-square ""] " Add site"]]

         [:div.form-group {:style {:padding-right "10px"}}
          [:button.btn.btn-warning {:type "button"}
           [:span.fa.fa-check ""] " Mark as read"]]

         ]]

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

(defn message-screen [message]
  [:div
   [:div.row
    [:div.alert.alert-info message]]])

(defn welcome-screen []
  [:div.row {:style {:padding-top "20px"}}
   [:div.jumbotron
    [:h2 "Welcome to Friendly Reader!"]
    [:p.lead "You can add websites to follow by pressing the "
          [:a.btn.btn-sm.btn-success {:href "#/add"}
           [:span.fa.fa-plus-square ""] " Add site"]
    " button."]
    [:p.lead "I've already added a few suggested websites for you, they are on the column to the left."]
    ]])

(defn add-feed-screen []
  [:div.well
   [:h3 "Track a website"]
   [:p.lead (str "Type or paste the address of a website in the field below."
                 "The program will add it to your list of websites.")]
   [:form {:role "form"}
    [:div.form-group
     [:label {:for "rss-address"} "Website to track"]
      [:input.form-control {:id "new-address" :type "text"
                            :placeholder "Enter the website or feed address here"}]]
    [:button.btn.btn-success
     {:type "button"
      :onClick (fn []
                 (let [new-address (.-value (by-id "new-address"))]
                   (when-not (zero? (count new-address))
                     (discover-feed new-address))))}
     [:span.fa.fa-plus-square ""] " Track this website"
     ]]])

;; HACK!
;; $("#body-N")[0].innerHTML = $("#body-N")[0].innerText
(defn post-bodies []
  (println "post bodies")
  (let [posts (get-in @user [:data "posts"])]
    (doall
     (for [i    (range (count posts))
           post (nth posts i)
           :let [id (str "body-" i) elem (by-id id)]]
       (if elem
         (let [div (.createElement js/document "DIV")]
           (try
             (set! (.-innerHTML div) (post "author"))
             (.appendChild elem div)
             (catch js/Error e nil))
           ""))))))

(defn show-feed-screen []
  (let [index (@user :index)
        feed  ((@user "feeds") index)
        url   (feed "url")]

    [:div

    ;; Feed title
     [:div.list-group
      [:div.list-group-item
       [:h2 (feed "title")
        [:a.btn.btn-danger.pull-right {:href "#" :on-click (fn [] (remove-feed url))}
         [:span.fa.fa-remove ""] " Remove this feed"]]]]

     ;; (.setTimeout js/window (post-bodies) 2000) ;; HACK
     [:div.list-group
      (let [posts (get-in @user [:data "posts"])]
        (for [i (range (count posts))
              :let [post (nth posts i)
                    frag (hickory/parse-fragment (post "body"))
                    hicc (map hickory/as-hiccup frag)
                    ]]
          ^{:key (str "body-" i)}
          [:a.list-group-item {:href (post "url") :target "_blank"}
           [:h4.list-group-item-heading
            [:strong (post "title")]
            (when-not (= "" (post "author")) [:small (str " by " (post "author"))])]
           [:p.list-group-item-text
            (post "body")]]
          ))]
     ]))

(defn main-screen []
  (let [detail-fns {:home      welcome-screen
                    :add       add-feed-screen
                    :show-feed show-feed-screen
                    :loading   (fn [] (message-screen "loading..."))}
        detail-key (get @user :screen :home)
        detail-fn  (detail-fns detail-key)]
    [:div
     [:div.row
      [:div.sidebar.nav-pills.nav-stacked.col-sm-3.col-md-2

       ;; SIDEBAR HOME
       [:ul.nav.nav-sidebar
        [:li {:class (if (#{:home :add} detail-key) "active" "")}
         [:a {:href "#"} [:span {:class "glyphicon glyphicon-home"}] " Home"]]
        ]

       ;; SIDEBAR LIST OF FEEDS
       [:ul.nav.nav-sidebar
        (for [i (range (count (@user "feeds"))) :let [feed ((@user "feeds") i)]]
          ^{:key (str i (feed "title"))}
          [:li {:class (if (= (str i) (@user :index)) "active" "")}
           [:a {:href (str "#/feeds/" i)}
            [:img {:src (feed "favicon") :height 16 :width 16}]
            [:span.badge.pull-right (feed "unread")] (str " " (feed "title"))]])
        ]
       ]
      [:div.container.main.col-sm-9.col-sm-offset-3.col-md-10.col-md-offset-2.col-sm-height

       ;; DETAIL (PAGE CENTER)
       [:div {:style {:padding-top "12px"}}
        (detail-fn)
        ]]]]))

(defn login-screen []
  [:div.container
   [:div.jumbotron
    [:h1 "Friendly Reader"]
    [:p.lead (str "An user friendly reader to follow the news of your favourite "
                  "blogs and websites, from the comfort of your hammock.")]
    [:p.lead "Written in Clojure, no registration required."]
    [:p
     [:a.btn.btn-lg.btn-danger {:href "/login/github" :role "button"}
      [:i.glyphicon.glyphicon-user ""] " Log in with Github"]
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

;; SERVER REQUESTS ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn discover-feed [url]
  (println "discover-feed: " url)
  (secretary/dispatch! (str "/discover/" url)))

(defn update-user []
  (GET "/api/userinfo"
       {:handler (fn [data]
                   (reset! user (into @user data)))
        :error-handler (fn [response]
                         (println "ERROR: " (str response)))}))

(defn remove-feed [url]
  (let [feeds    (@user "feeds")
        filtered (into [] (filter #(not (= url (% "url"))) feeds))]
    (POST "/api/delete" {:params {:url url}
                         :format :json
                         :handler (fn [data] (update-user))
                         :error-handler (fn [data] (println data))})
    ))

;; ROUTING ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(secretary/set-config! :prefix "#")

(defroute home-path "/home" []
  (if (@user "email")
    (swap! user assoc :screen :home)
    (GET "/api/userinfo" {:handler (fn [data]
                                     (.setInterval js/window update-user 60000)
                                     (reset! user (assoc data :screen :home)))
                          :error-handler (fn [response]
                                           (reset! user {:screen :home}))
                          })))

(defroute "/add" []
  (swap! user assoc :screen :add))

(defroute "/discover/:url" [url]
  (swap! user assoc :screen :loading)
  (POST "/api/discover" {:params {:url url}
                         :format :json
                         :handler (fn [data]
                                    (update-user) ;; to update feeds data
                                    (reset-location! "#/")
                                    (swap! user assoc :screen :home))
                         :error-handler (fn [resp]
                                          (reset-location! "#/")
                                          (swap! user assoc :screen :home))}))

(defroute "/feeds/:index" [index]
  (let [i (.parseInt js/window index)
        feeds (@user "feeds")
        feed (-> feeds (nth i))
        url (get feed "url")]
    (GET "/api/feed" {:params {:url url}
                      :format :json
                      :handler (fn [data]
                                 (swap! user assoc :screen :show-feed
                                        :index index :data data))
                      :error-handler (fn [resp]
                                       (reset-location! "#/")
                                       (swap! user assoc :screen :home))})))
(defroute "*" []
  (secretary/dispatch! "/home"))

;; Quick and dirty history configuration.
(let [h (History.)]
  (goog.events/listen h EventType.NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h
    (.setEnabled true)))

(secretary/dispatch! "/")

;; MAIN APP ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(reagent/render-component (fn [] [toolbar]) (by-id "navbar"))
(reagent/render-component (fn [] [show-app]) (by-id "app"))
