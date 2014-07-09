(ns rotten-difs.core
  (:require [clj-http.client :as http])
  (:require [clojure.data.json :as json])
  (:require [net.cgrand.enlive-html :as html])
  (:require [ring.util.codec :as c]))

(def wikipedia-list-url-base "http://en.wikipedia.org/wiki/List_of_films:")
(def suffix-list [ "_numbers" "_A" "_B" "_C" "_D" "_E" "_F" "_G" "_H" "_I" "_J-K"
                  "_L" "_M" "_N-O" "_P" "_Q-R" "_S" "_T" "_U-V-W" "_X-Y-Z" ])
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

(defn get-link-content
  [link]
  (first (:content link)))

(defn get-link-title
  [link]
  (:title (:attrs link)))

(defn get-single-release-name
  [li]
  (get-link-title (first (html/select li [:a]))))

(defn extract-numbers
  [string]
  (if (not (empty? string)) (re-find #"\d+" string)))

(defn get-single-release-year
  [li]
  (extract-numbers (first (filter extract-numbers
                                  (filter #(instance? java.lang.String %)
                                          (:content li))))))

(defn make-map
  [title year]
  {:title title
   :year year})

(defn get-single-release
  [li]
  (make-map (get-single-release-name li)
            (get-single-release-year li)))

(defn make-map-from-title-and-link
  [title link]
  (make-map title
            (extract-numbers (get-link-content link))))

(defn get-multi-release-title
  [li]
  (first (:content (first (:content li)))))

(defn get-all-releases
  [li]
  (let [title (get-multi-release-title li)]
    (map #(make-map-from-title-and-link title %)
         (html/select li [:a]))))

(defn multiple-releases?
  [li]
  (< 1 (count (filter #(re-matches #"\d+" (get-link-content %)) (html/select li [:a])))))

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

(defn exact-title-match
  [title movies]
  (if (> (count movies) 1)
    (filter #(= title (:title %)) movies)
    movies))

(defn only-with-ratings
  [movies]
  (filter #(< 0 (get-in % [:ratings :critics_score])) movies))

(defn get-right-year
  [movies year]
  (if (> (count movies) 1)
    (filter #(= year (:year %)) movies)
    movies))

(defn clean-results
  [movies year title]
  (exact-title-match title
                     (get-right-year (only-with-ratings movies)
                                     year)))

(defn find-movie
  [movie-map]
  (try
    (clean-results (search-for-movie (:title movie-map))
                   (read-string (:year movie-map))
                   (:title movie-map))
    (catch Exception e
      (pr movie-map)
      (print " threw exception: ")
      (.printStackTrace e))))


; Putting them together
(defn aggregate-data
  [current-results new-data]
  (concat (:audience-favored current-results)
        (:critic-favored current-results)
        new-data))

(defn sort-by-review-dispairity
  [movie-data]
  (sort-by #(:difference %) movie-data))

(defn update-results
  [current-results new-data]
  (let [sorted-aggregate (sort-by-review-dispairity (aggregate-data current-results
                                                                    new-data))]
    {:critic-favored (take 25 sorted-aggregate),
     :audience-favored (take-last 25 sorted-aggregate)}))

(defn extract-wanted-data
  [rt-movie]
  {:title (:title rt-movie),
   :year (:year rt-movie),
   :poster (get-in rt-movie [:posters :thumbnail]),
   :audience-score (get-in rt-movie [:ratings :audience_score]),
   :critics-score (get-in rt-movie [:ratings :critics_score]),
   :rating (get rt-movie :mpaa_rating), ; This is sketchy; what other ratings are given?
   :difference (- (get-in rt-movie [:ratings :audience_score]) ; "Audience is always right mentality."
                  (get-in rt-movie [:ratings :critics_score]))}) ; High-scorers mean audience liked them but critics didn't.

(defn notify-too-many-results
  [movie-map rt-movie]
  (print movie-map)
  (print " did not get just one result! Got: ")
  (println rt-movie)
  {})

(defn notify-no-results
  [movie-map]
  (print "No suitable results found for ")
  (println movie-map)
  {})

(defn get-data
  [movie-map]
  (let [rt-movie (find-movie movie-map)]
    (case (count rt-movie)
      0 (notify-no-results movie-map)
      1 (extract-wanted-data (first rt-movie))
      (notify-too-many-results movie-map rt-movie))))

(defn movie-data-with-review
  [movie-map]
  (try
    (merge {:wiki-title (:title movie-map), :wiki-year (:year movie-map)}
           (get-data movie-map))
    (catch Exception e
      (pr movie-map)
      (print " threw exception: ")
      (.printStackTrace e))))

(defn get-movie-differences-for-suffix
  [suffix]
  (remove #(nil? (:difference %))
          (map movie-data-with-review (movie-data-from-wiki-url (make-wiki-list-url suffix)))))

(defn find-movies-with-greatest-review-discrepancies
  [suffixes]
  (loop [remaining-suffixes suffixes
         results {}]
    (if (empty? remaining-suffixes)
      results
      (let [[suffix & remaining] remaining-suffixes]
        (recur remaining
               (concat results
                               (get-movie-differences-for-suffix suffix)))))))