(ns rotten-difs.core
  (:require [clj-http.client :as http])
  (:require [clojure.data.json :as json]))

(def api-key "4j3pebccyj3f9brmn8k4q4yt")
(def ^:dynamic *base-url* "http://api.rottentomatoes.com/api/public/v1.0.json?apikey=")