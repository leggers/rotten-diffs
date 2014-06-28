(ns rotten-difs.core
  (:require [clj-http.client :as http])
  (:require [clojure.data.json :as json])
  (:require [net.cgrand.enlive-html :as html]))

(def api-key "4j3pebccyj3f9brmn8k4q4yt")
(def wikipedia-list-url-base "http://en.wikipedia.org/w/api.php?format=json&action=query&prop=revisions&rvprop=content&titles=List%20of%20films:")
(def film-list-url-endings [
   "_numbers" "_A" "_B" "_C" "_D" "_E" "_F" "_G" "_H" "_I" "_J" "_K" "_L" "_M" "_N"
    "_O" "_P" "_Q" "_R" "_S" "_T" "_U" "_V" "_W" "_X" "_Y" "_Z"
   ])
(def ^:dynamic *base-url* "http://api.rottentomatoes.com/api/public/v1.0.json?apikey=")

(defn get-url
  [url]
  (http/get url))

(defn append-api-key
  [url]
  (str url api-key))

(defn get-base-url
  []
  (get-url (append-api-key *base-url*)))

(defn get-response-body
  [url]
  (get (get-url url) :body))

(defn get-base-response-body
  []
  (get-response-body (append-api-key *base-url*)))

(defn as-json
  [content]
  (json/read-str content
                 :key-fn keyword))

(defn get-list-of-moves-from-list-page
  [list-page-url]
  (:pages
      (:query
        (as-json
            (get-response-body list-page-url)))))