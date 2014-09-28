(ns friendly.feeds
  (:require [clj-http.client :as client]
            [hickory.core :as hick]))

(defn potential-rss-addresses
  "Generate addresses (perhaps invalid) which may contain RSS resources"
  [address]
  (let [clean (-> address
                  (.replaceAll "https://" "")
                  (.replaceAll "http://" "")
                  (.replaceAll "^www." ""))]
    (doall
     (for [proto  ["http://" "https://"]
           subdom ["www." "blog." "rss." ""]
           path   ["" "/blog"]]
       (str proto subdom clean path)))))

(defn find-alternate-links [address]
  (try
    (let [tree (-> (client/get address) :body hick/parse hick/as-hickory)
          head (-> tree second second second :content first :content)
          links (filter #(= :link (:tag %)) head)
          alts (filter #(= "alternate" (:rel %)) (map :attrs links))]
      alts)
    (catch Exception e nil)))

(defn find-rss [address]
  (let [potentials (potential-rss-addresses address)
        found (->> (pmap find-alternate-links potentials)
                   flatten
                   (map :href)
                   (apply hash-set)
                   (filter (complement nil?)))]
    (first found)))
