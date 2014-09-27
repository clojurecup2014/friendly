(ns friendly.core)

(enable-console-print!)

;; HELPERS ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn by-id [elem-id]
  (.getElementById js/document elem-id))

;; MAIN APP ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn launch-app []
  (let [app (by-id "app")]
    (set! (.-innerHTML app) "<h1>Loaded from CLJS!</h1>")))

(launch-app)


