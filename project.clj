(defproject friendly "0.1.0-SNAPSHOT"
  :description "Friendly RSS Reader"
  :url "http://friendly.clojurecup.com/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [;; server
                 [org.clojure/clojure "1.6.0"]
                 [clj-http "1.0.0"]
                 [ring/ring-core "1.3.1"]
                 [ring/ring-devel "1.3.1"]
                 [ring/ring-json "0.3.1"]
                 [compojure "1.1.8"]
                 [com.cemerick/friend "0.2.1"]
                 [org.clojure/data.json "0.2.5"]
                 [http-kit "2.0.0"]

                 ;; web resources, css, etc
                 [org.webjars/bootstrap "3.2.0"]
                 [org.webjars/bootswatch-readable "3.2.0-1"]
                 [org.webjars/react "0.11.1"]
                 [org.webjars/jquery "1.11.1"]
                 [org.webjars/font-awesome "4.2.0"]

                 ;; client side
                 [org.clojure/clojurescript "0.0-2311"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [whoops/reagent "0.4.3"]
                 [secretary "1.2.1"]
                 [cljs-ajax "0.2.6"]]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]]

  :source-paths ["src", "src-cljs"]

  :cljsbuild
  {:builds [{:id "dev"
             :source-paths ["src-cljs"]
             :compiler {:output-to "resources/public/js/app.js"
                        :output-dir "resources/public/js/out"
                        :optimizations :none
                        :source-map true}}]}
  :main friendly.core)
