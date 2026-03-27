(ns transit.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as re-frame]
            [transit.events :as events]
            [transit.subs]))

(defn- search-panel []
  [:form {:on-submit (fn [e]
                       (.preventDefault e)
                       (re-frame/dispatch [::events/search]))}
   [:input {:name "from" :type "text" :on-change #(re-frame/dispatch [::events/search-request-update :from (.. % -target -value)])}]
   [:input {:name "destination" :type "text" :on-change #(re-frame/dispatch [::events/search-request-update :destination (.. % -target -value)])}]
   [:input {:name "departuredate" :type "date" :on-change #(re-frame/dispatch [::events/search-request-update :date (.. % -target -value)])}]
   [:input {:type "submit" :value "Search"}]])

(defn- home-page []
  [:div
   [:h1 "Welcome to the Public Transit App!"]
   [search-panel]])

(defn- about-page []
  [:div
   [:h1 "About"]])

(defn- search-result [{:keys [train]}]
  [:span (:name train)])

(defn- search-page []
  (let [search-results @(re-frame/subscribe [:search-results])]
    [:div
     [:h1 "Search Results"]
     (for [result search-results]
     [:li
       [search-result result]])]))

(defn- main-page []
  (let [active-page @(re-frame/subscribe [:active-page])]
    (case active-page
      :home [home-page]
      :about [about-page]
      :search [search-page]
      [home-page])))

(defn- ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!) ;; TODO: if not dev
  (let [app (.getElementById js/document "app")]
    (rdom/unmount-component-at-node app)
    (rdom/render [main-page] app))
  (js/console.log "Mounting root..."))

(defn init []
  (re-frame.core/dispatch-sync [::events/initialize-db])
  (mount-root))
