(ns transit.services.trains)

(def ^:private stations {2389 "Somewhere"
                         2383 "Somewhere Else"})

(def ^:private trains [{:id #uuid "a3f1b2c4-5d6e-7f80-9a1b-2c3d4e5f6a7b"
                        :name "Something Express"
                        :schedule [{:id #uuid "b4e2c3d5-6e7f-8091-ab2c-3d4e5f6a7b8c"
                                    :stops [{:day 0
                                             :station 2389
                                             :arrival [9 20]
                                             :departure [9 30]}
                                            {:day 0
                                             :station 2383
                                             :arrival [11 0]}]}
                                   {:id #uuid "c5f3d4e6-7f80-9102-bc3d-4e5f6a7b8c9d"
                                    :stops [{:day 0
                                             :station 2389
                                             :arrival [23 20]
                                             :departure [23 30]}
                                            {:day 1
                                             :station 2383
                                             :arrival [1 0]}]}]}])

(def ^:private schedule [{:train-id #uuid "a3f1b2c4-5d6e-7f80-9a1b-2c3d4e5f6a7b"
                          :schedule-id #uuid "c5f3d4e6-7f80-9102-bc3d-4e5f6a7b8c9d"
                          :start-date "2026-05-27"
                          :stops [{:day 0
                                   :station 2389
                                   :arrival [9 20]
                                   :departure [9 30]}
                                  {:day 0
                                   :station 2383
                                   :arrival [11 0]}]}
                         {:train-id #uuid "a3f1b2c4-5d6e-7f80-9a1b-2c3d4e5f6a7b"
                          :schedule-id #uuid "c5f3d4e6-7f80-9102-bc3d-4e5f6a7b8c9d"
                          :start-date "2026-04-27"
                          :stops [{:day 0
                                   :station 2389
                                   :arrival [23 20]
                                   :departure [23 30]}
                                  {:day 1
                                   :station 2383
                                   :arrival [1 0]}]}])
