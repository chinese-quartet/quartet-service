(ns quartet-service.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[quartet-service started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[quartet-service has shut down successfully]=-"))
   :middleware identity})
