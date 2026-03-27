(ns transit.events
  (:require [re-frame.core :as re-frame]
            [transit.services.trains :as trains]
            [transit.routing :as routing]))

(re-frame/reg-event-fx
 ::initialize-db
 (fn [_ _]
   (routing/setup-router!)
   {:db {:name "Public Transit App"
         :active-page :home}}))

(re-frame/reg-event-fx
 ::search-request-update
 (fn [{:keys [db]} [_ field value]]
   {:db (assoc-in db [:search :request field] value)}))

(re-frame/reg-event-db
 ::search
 (fn [{:keys [search] :as db} _]
   (let [{:keys [from destination date]} (:request search)]
     (-> db
         (assoc-in [:search :results] (trains/search from destination date))
         (assoc :active-page :search)))))

(re-frame/reg-event-db
 :set-active-page
 (fn [db [_ page]]
   (assoc db :active-page page)))