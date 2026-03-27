(ns transit.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :active-page
 (fn [db _]
   (:active-page db)))

(re-frame/reg-sub
 :search-results
 (fn [db _]
   (get-in db [:search :results])))