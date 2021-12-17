(ns quartet-service.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [quartet-service.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "-=[quartet-service started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "-=[quartet-service has shut down successfully]=-"))
   :middleware wrap-dev})
