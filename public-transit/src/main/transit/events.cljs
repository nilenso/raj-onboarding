(ns transit.events
  (:require [re-frame.core :as re-frame]
            [transit.routing :as routing]))

(re-frame/reg-event-fx
 ::initialize-db
 (fn [_ _]
   (routing/setup-router!)
   {:db {:name "Public Transit App"
         :active-page :home}}))

(re-frame/reg-event-fx
 ::search-phrase-updated
 (fn [{:keys [db]} [_ search-phrase]]
   {:db (assoc db :search-phrase search-phrase)}))

(re-frame/reg-event-fx
 ::search
 (fn [{:keys [db]} _]
   {:db (-> db
            (assoc :search-results ["fast train" "slow train"])
            (assoc :active-page :search))}))

(re-frame/reg-event-db
 :set-active-page
 (fn [db [_ page]]
   (assoc db :active-page page)))