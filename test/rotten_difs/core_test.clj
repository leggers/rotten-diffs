(ns rotten-difs.core-test
  (:require [clojure.test :refer :all]
            [rotten-difs.core :refer :all]))

(def li-with-single-release
  {:tag :li, :attrs nil, :content '({:tag :i, :attrs nil, :content ({:tag :a, :attrs {:title "Zero Charisma", :href "/wiki/Zero_Charisma"}, :content ("Zero Charisma")})} " (2013)")})

(def single-release-link
  {:tag :a, :attrs {:title "Zero Charisma", :href "/wiki/Zero_Charisma"}, :content '("Zero Charisma")})

(def single-release-map
  {:title "Zero Charisma", :year "2013"})

(def li-with-tv-release
  {:tag :li, :attrs nil, :content '({:tag :i, :attrs nil, :content ({:tag :a, :attrs {:title "12 Men of Christmas", :href "/wiki/12_Men_of_Christmas"}, :content ("12 Men of Christmas")})} " (2009) (" {:tag :a, :attrs {:title "Television film", :href "/wiki/Television_film"}, :content ("TV")} ")")})

(def li-tv-name-link
  {:tag :a, :attrs {:title "12 Men of Christmas", :href "/wiki/12_Men_of_Christmas"}, :content '("12 Men of Christmas")})

(def multi-release-li
  {:tag :li, :attrs nil, :content '({:tag :i, :attrs nil, :content ("20,000 Leagues Under the Sea")} ": (" {:tag :a, :attrs {:class "mw-redirect", :title "20,000 lieues sous les mers (film)", :href "/wiki/20,000_lieues_sous_les_mers_(film)"}, :content ("1907")} ", " {:tag :a, :attrs {:title "20,000 Leagues Under the Sea (1916 film)", :href "/wiki/20,000_Leagues_Under_the_Sea_(1916_film)"}, :content ("1916")} ", " {:tag :a, :attrs {:title "20,000 Leagues Under the Sea (1954 film)", :href "/wiki/20,000_Leagues_Under_the_Sea_(1954_film)"}, :content ("1954")} ", " {:tag :a, :attrs {:title "20,000 Leagues Under the Sea (1997 Hallmark film)", :href "/wiki/20,000_Leagues_Under_the_Sea_(1997_Hallmark_film)"}, :content ("1997 Hallmark")} ", & " {:tag :a, :attrs {:title "20,000 Leagues Under the Sea (1997 Village Roadshow film)", :href "/wiki/20,000_Leagues_Under_the_Sea_(1997_Village_Roadshow_film)"}, :content ("1997 Village Roadshow")} ")")})

(def multi-release-map-list
  '({:title "20,000 Leagues Under the Sea", :year "1907"} {:title "20,000 Leagues Under the Sea", :year "1916"} {:title "20,000 Leagues Under the Sea", :year "1954"} {:title "20,000 Leagues Under the Sea", :year "1997"} {:title "20,000 Leagues Under the Sea", :year "1997"}))

(deftest wiki-url-test
  (testing "WikiURL builder"
    (is (= (make-wiki-list-url "_A")
           "http://en.wikipedia.org/wiki/List_of_films:_A"))))

(deftest get-link-content-test
  (testing "get-link-content method"
    (is (= "12 Men of Christmas"
           (get-link-content li-tv-name-link)))))

(deftest test-getting-single-release-name
  (testing "get-single-release-name function"
    (is (= (get-single-release-name li-with-single-release)
           "Zero Charisma"))))

(deftest test-getting-single-release-year
  (testing "getting-single-release-year funtion"
    (is (= "2013"
           (get-single-release-year li-with-single-release)))))

(deftest get-single-release-test
  (testing "get-single-release function"
    (is (= single-release-map
           (get-single-release li-with-single-release)))))

(deftest multi-release-with-normal-single-release
  (testing "multiple-releases? function with normal single release li"
    (is (= false
           (multiple-releases? li-with-single-release)))))

(deftest multiple-releases-function
  (testing "multiple-releases? function with single release"
    (is (= false
           (multiple-releases? li-with-single-release)))))

(deftest multi-release-with-tv-single-release
  (testing "multiple-releases? function with weird single release li with tv release info"
    (is (= false
           (multiple-releases? li-with-tv-release)))))

(deftest test-li-to-map-with-single-release
  (testing "li-to-map function with single release"
        (is (= {:title "Zero Charisma", :year "2013"}
               (li-to-map li-with-single-release)))))

(deftest get-multi-release-title-test
  (testing "get-multi-release-title function test"
    (is (= "20,000 Leagues Under the Sea"
           (get-multi-release-title multi-release-li)))))

(deftest multi-release-with-multiple-releases-test
  (testing "multiple-releases? function with multiple releases"
    (is (= true
           (multiple-releases? multi-release-li)))))

(deftest test-li-to-map-with-multiple-releases
  (testing "li-to-map function with multiple releases"
    (is (= multi-release-map-list
           (li-to-map multi-release-li)))))