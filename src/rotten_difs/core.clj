(ns rotten-difs.core
  (:require [clj-http.client :as http])
  (:require [clojure.data.json :as json])
  (:require [net.cgrand.enlive-html :as html])
  (:require [ring.util.codec :as c]))

(def wikipedia-list-url-base "http://en.wikipedia.org/wiki/List_of_films:")
(def suffix-list [ "_numbers" "_A" "_B" "_C" "_D" "_E" "_F" "_G" "_H" "_I" "_J-K"
                  "_L" "_M" "_N-O" "_P" "_Q" "_R" "_S" "_T" "_U-V-W" "_X-Y-Z" ])
(def rt-base-url
  "http://api.rottentomatoes.com/api/public/v1.0/movies.json?apikey=4j3pebccyj3f9brmn8k4q4yt")


; Wikipedia part
(defn html-get-url
  [url]
  (html/html-resource (java.net.URL. url)))

(defn make-wiki-list-url
  [suffix]
  (str wikipedia-list-url-base suffix))

(defn select-all-content-uls
  [html-data]
  (html/select html-data [:div#mw-content-text :> :ul]))

(defn uls-with-movies
  [html-data]
  (drop-last (select-all-content-uls html-data))) ; Last is "See Also" ul, which we don't want.

(defn all-lis
  [html-data]
  (html/select html-data [:li]))

(defn li-contains-ul?
  [li-object]
  (= () (html/select li-object [:ul])))

(defn extract-movie-lis
  [movie-uls]
  (filter li-contains-ul? (all-lis movie-uls)))

(defn get-lis-of-movies-from-wiki-url
  [list-page-url]
  (extract-movie-lis (uls-with-movies (html-get-url list-page-url))))

(defn get-single-release-name
  [li]
  (first (:content (first (:content (first (:content li)))))))

(defn parse-int
  [string]
  (re-find #"\d+" string))

(defn get-sigle-release-year
  [li]
  (parse-int (second (:content li))))

(defn make-map
  [title year]
  {:title title
   :year year})

(defn get-single-release
  [li]
  (make-map (get-single-release-name li)
            (get-sigle-release-year li)))

(defn make-map-from-title-and-link
  [title link]
  (make-map title
            (parse-int (first (:content link)))))

(defn get-multi-release-title
  [li]
  (first (:content (first (:content li)))))

(defn get-all-releases
  [li]
  (let [title (get-multi-release-title li)]
    (map (fn [link] (make-map-from-title-and-link title link))
         (html/select li [:a]))))

(defn multiple-releases?
  [li]
  (< 1 (count (html/select li [:a]))))

(defn li-to-map
  [li]
  (if (multiple-releases? li)
    (get-all-releases li)
    (get-single-release li)))

(defn movie-data-from-wiki-url
  [url]
  (flatten (map li-to-map (get-lis-of-movies-from-wiki-url url))))


; Rotten Tomatoes part
(defn http-get-url
  [url]
  (http/get url))

(defn parse-json-str
  [content]
  (json/read-str content
                 :key-fn keyword))

(defn search-url
  [title]
  (str rt-base-url (str "&q=" (c/url-encode title))))

(defn search-for-movie
  [title]
  (:movies (parse-json-str (:body (http-get-url (search-url title))))))

(defn not-dvd-only?
  [movie]
  (< 0 (count (dissoc (:release_dates movie) :dvd))))

(defn remove-dvd-only-releases
  [movies]
  (filter not-dvd-only? movies))