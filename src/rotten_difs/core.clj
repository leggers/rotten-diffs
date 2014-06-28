(ns rotten-difs.core
  (:require [clj-http.client :as http])
  (:require [clojure.data.json :as json])
  (:require [net.cgrand.enlive-html :as html]))

(def api-key "4j3pebccyj3f9brmn8k4q4yt")
(def wikipedia-list-url-base "http://en.wikipedia.org/wiki/List_of_films:")
(def suffix-list [
   "_numbers" "_A" "_B" "_C" "_D" "_E" "_F" "_G" "_H" "_I" "_J" "_K" "_L" "_M" "_N"
    "_O" "_P" "_Q" "_R" "_S" "_T" "_U" "_V" "_W" "_X" "_Y" "_Z"
   ])
(def ^:dynamic *base-url* "http://api.rottentomatoes.com/api/public/v1.0.json?apikey=")
(def ^:dynamic *list-item-selector*
  #{[:div#mw-content-text :> :ul :> :li]
   [:div#mw-content-text :> :ul :> :li :> :ul :> :li] ; For movie series, see "Asterix Series" on http://en.wikipedia.org/wiki/List_of_films:_A
   ;(html/but [:div#mw-content-text :> :ul :> :li :> :ul])
   })


; Wikipedia part
(defn html-get-url
  [url]
  (html/html-resource (java.net.URL. url)))

(defn make-wiki-list-url
  [suffix]
  (str wikipedia-list-url-base suffix))

(defn select-list-of-movies
  [html-data]
  (html/select html-data *list-item-selector*))

(defn get-list-of-movies-from-list-page
  [list-page-url]
  (select-list-of-movies (html-get-url list-page-url)))


; Rotten Tomatoes part
(defn http-get-url
  [url]
  (http/get url))

(defn as-json
  [content]
  (json/read-str content
                 :key-fn keyword))

(defn append-rt-api-key
  [url]
  (str url api-key))

(defn make-base-url
  []
  (http-get-url (append-rt-api-key *base-url*)))

(defn get-response-body
  [url]
  (get (http-get-url url) :body))

(defn get-base-response-body
  []
  (get-response-body (make-base-url)))