(ns transit.pages.home
  (:require [re-frame.core :as re-frame]
            [transit.events :as events]))

(defn- search-panel []
  [:form {:on-submit (fn [e]
                       (.preventDefault e)
                       (re-frame/dispatch [::events/search]))}
   [:input {:name "from" :type "text" :on-change #(re-frame/dispatch [::events/search-request-update :from (.. % -target -value)])}]
   [:input {:name "destination" :type "text" :on-change #(re-frame/dispatch [::events/search-request-update :destination (.. % -target -value)])}]
   [:input {:name "departuredate" :type "date" :on-change #(re-frame/dispatch [::events/search-request-update :date (.. % -target -value)])}]
   [:input {:type "submit" :value "Search"}]])

(defn home-page []
  [:div
   [:h1 "Welcome to the Public Transit App!"]
   [search-panel]])
