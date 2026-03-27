(ns transit.pages.search
  (:require [re-frame.core :as re-frame]))

(defn- search-result [{:keys [train]}]
  [:span (:name train)])

(defn search-page []
  (let [search-results @(re-frame/subscribe [:search-results])]
    [:div
     [:h1 "Search Results"]
     (for [result search-results]
     [:li
       [search-result result]])]))
