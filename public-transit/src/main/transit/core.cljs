(ns transit.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as re-frame]
            [transit.events :as events]
            [transit.subs]
            [transit.pages.home :refer [home-page]]
            [transit.pages.about :refer [about-page]]
            [transit.pages.search :refer [search-page]]))

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
