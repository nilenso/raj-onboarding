(ns transit.routing
  (:require [bidi.bidi :as bidi]
            [pushy.core :as pushy]
            [re-frame.core :as re-frame]))

(def routes
    ["/" {"" :home
          "about" :about
          "search" :search}])

(defn- parse-url [url]
  (bidi/match-route routes url))

(defn- dispatch-route [matched-route]
  (let [page (:handler matched-route)]
    (re-frame/dispatch [:set-active-page page])))

(defn setup-router! []
  (let [history (pushy/pushy dispatch-route parse-url)]
    (pushy/start! history)))