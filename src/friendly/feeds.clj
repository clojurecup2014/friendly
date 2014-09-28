(ns friendly.feeds
  (:require [clj-http.client :as client]
            [hickory.core :as hick])
  (:import [java.net URL]
           [java.io File InputStreamReader]
           [com.rometools.rome.io SyndFeedInput WireFeedInput XmlReader]
           [com.rometools.opml.feed.opml Opml]
           [com.rometools.rome.feed.synd SyndFeed]))

(defn potential-rss-addresses
  "Generate addresses (perhaps invalid) which may contain RSS resources"
  [address]
  (let [clean (-> address
                  (.replaceAll "https://" "")
                  (.replaceAll "http://" "")
                  (.replaceAll "^www." ""))]
    (doall
     (for [proto  ["http://" "https://"]
           subdom ["" "www." "blog." "rss."]
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

(defn feed [url]
  (let [input (SyndFeedInput.)
        feed  (.build input (XmlReader. (URL. url)))]
    feed))

(defn feed-title [url]
  (.getTitle (feed url)))

(defn find-favicon [address]
  (let [url   (URL. address)
        home  (str (.getProtocol url) "://" (.getHost url))
        dummy (str (.getProtocol url) "://" (.getHost url) "/favicon.ico")]
    (try
      (let [tree  (-> (client/get home) :body hick/parse hick/as-hickory)
            head  (-> tree second second second :content first :content)
            links (filter #(= :link (:tag %)) head)
            icons (filter #(#{"shortcut icon" "icon"} (:rel %)) (map :attrs links))]
        (if (= [] icons)
          dummy
          (str home
               (if (.endsWith home "/") "" "/")
               (-> icons first :href))))
      (catch Exception e dummy))))

(defn posts [url]
  (let [entries (.getEntries (feed url))]
    (doall
     (for [entry entries]
       {:title  (.getTitle entry)
        :author (.getAuthor entry)
        :url    (.getLink entry)
        :body   (try
                  (-> entry .getContents first .getValue)
                  (catch Exception e ""))
        }))))
