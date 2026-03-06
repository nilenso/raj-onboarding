(ns api.handlers.core)


(defn status-handler [_req]
  {:status 200
   :body "OK"
   :headers {"Content-Type" "text/html"}})
