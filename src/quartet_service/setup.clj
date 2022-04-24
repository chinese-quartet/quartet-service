(ns quartet-service.setup
  (:require [remote-fs.core :as remote-fs]
            [tservice-core.core :as plugin-sys]
            [clojure.tools.logging :as log]
            [mount.core :as mount]
            [quartet-service.config :refer [env make-minio-link]]
            [quartet-service.db.handler :as db-handler]))

;; Connect remote filesystem (such as oss, minio)
(defn connect-fs!
  []
  (doseq [service (:fs-services env)]
    (log/info (format "Connect %s service" (:fs-service service)))
    (if (= (:default-fs-service env) (:fs-service service))
      (remote-fs/setup-connection (:fs-service service)
                                  (:fs-endpoint service)
                                  (:fs-access-key service)
                                  (:fs-secret-key service))
      (remote-fs/setup-connection (:fs-service service)
                                  (:fs-endpoint service)
                                  (:fs-access-key service)
                                  (:fs-secret-key service)))))

(mount/defstate setup-fs
  :start
  (connect-fs!)
  :stop
  (comment "May be we need to add disconnect function"))

(mount/defstate plugin-sys
  :start
  (do
    (plugin-sys/setup-custom-plugin-dir (:plugin-rootdir env))
    (plugin-sys/setup-custom-workdir-root (:workdir env))
    (plugin-sys/setup-plugin-configs (:plugins env))
    (plugin-sys/setup-custom-fns db-handler/create-task! 
                                 db-handler/update-task!
                                 make-minio-link)
    (plugin-sys/start-plugins!))
  :stop
  (plugin-sys/stop-plugins!))
