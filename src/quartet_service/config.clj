(ns quartet-service.config
  (:require
   [clojure.spec.alpha :as s]
   [expound.alpha :refer [expound-str]]
   [cprop.core :refer [load-config]]
   [cprop.source :as source]
   [clojure.string :as clj-str]
   [clojure.tools.logging :as log]
   [clojure.java.io :refer [file]]
   [mount.core :refer [args defstate]]))

(defstate env
  :start (load-config :merge [(args)
                              (source/from-system-props)
                              (source/from-env)]))

#_:clj-kondo/ignore
(defn which-database
  ([database-url]
   {:pre [(s/valid? ::database-url database-url)]
    :post [(s/valid? ::database %)]}
   (let [database (re-find (re-matcher #"jdbc:sqlite|jdbc:postgresql|jdbc:h2"
                                       database-url))]
     (if database
       (clj-str/replace database #"^jdbc:" "")
       "sqlite")))
  ([] (which-database (:database-url env))))

(defn get-migration-config
  [env]
  (merge {:migration-dir (str "migrations/" (which-database (:database-url env)))}
         (select-keys env [:database-url :init-script])))

;; -------------------------------- Config Spec --------------------------------
(defn exists?
  [filepath]
  (.exists (file filepath)))

;; More details on https://stackoverflow.com/q/13621307
(s/def ::port (s/int-in 1024 65535))

(s/def ::nrepl-port (s/int-in 1024 65535))

(s/def ::database-url (s/and string? #(some? (re-matches #"jdbc:(sqlite|postgresql|h2):.*" %))))

(s/def ::database #{"postgresql" "sqlite" "h2"})


(s/def ::workdir (s/and string? exists?))

(s/def ::plugin-rootdir (s/and string? exists?))

;; Service
(s/def ::fs-service #{"minio" "oss" "s3"})

(s/def ::fs-endpoint #(some? (re-matches #"https?:\/\/.*" %)))

(s/def ::fs-access-key string?)

(s/def ::fs-secret-key string?)

(s/def ::fs-rootdir (s/nilable (s/and string? exists?)))

(s/def ::service (s/keys :req-un [::fs-service ::fs-endpoint ::fs-access-key ::fs-secret-key]
                         :opt-un [::fs-rootdir]))

(s/def ::fs-services (s/coll-of ::service))

(s/def ::default-fs-service #{"minio" "oss" "s3"})

(s/def ::plugins (s/map-of keyword? map?))

(s/def ::enable-cors boolean?)

(s/def ::cors-origins (s/nilable (s/coll-of string?)))

(s/def ::config (s/keys :req-un [::port ::database-url ::workdir ::plugin-rootdir
                                 ::fs-services ::default-fs-service]
                        :opt-un [::nrepl-port ::plugins ::cors-origins ::enable-cors]))

(defn get-minio-rootdir
  [env]
  (let [fs-services (:fs-services env)
        fs-rootdir (->> fs-services
                        (filter #(= (:fs-service %) "minio"))
                        (first)
                        (:fs-rootdir))
        fs-rootdir (or fs-rootdir "")]
    fs-rootdir))

(defn make-minio-link
  "Replace an absolute path with minio link."
  [abspath]
  (let [minio-rootdir (get-minio-rootdir env)
        trimmed (str (clj-str/replace minio-rootdir #"/$" "") "/")]
    (clj-str/replace abspath (re-pattern trimmed) "minio://")))

(defn check-fs-root!
  [env]
  (let [fs-rootdir (get-minio-rootdir env)
        workdir (:workdir env)]
    (when-not (clj-str/starts-with? workdir fs-rootdir)
      (log/error (format "workdir(%s) must be the child directory of fs-rootdir(%s)"
                         workdir
                         fs-rootdir))
      (System/exit 1))))

(defn check-config
  [env]
  (let [config (select-keys env [:port :nrepl-port :database-url :workdir
                                 :plugin-rootdir :fs-services
                                 :default-fs-service :plugins])]
    (check-fs-root! env)
    (when (not (s/valid? ::config config))
      (log/error "Configuration errors:\n" (expound-str ::config config))
      (System/exit 1))))
