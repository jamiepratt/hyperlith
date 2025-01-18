(ns hyperlith.impl.db
  (:require [datalevin.core :as d]
            [datalevin.lmdb :as l])
  (:import [datalevin.db DB]
           [datalevin.storage Store]))

(defn create-db [name schema]
  (d/get-conn name schema
    {:validate-data?    true
     :closed-schema?    true
     ;; Adds created-at and updated-at
     :auto-entity-time? true}))

(def q d/q)
(def transact! d/transact!)
(def listen! d/listen!)
(def unlisten! d/unlisten!)
(def update-schema d/update-schema)

(defn backup-copy
  "Make a backup copy of the database. `dest-dir` is the destination
  data directory path. Will compact while copying if `compact?` is true."
  [conn dest-dir compact?]
  (let [lmdb (.-lmdb ^Store (.-store ^DB conn))]
    (println "Copying...")
    (l/copy lmdb dest-dir compact?)
    (println "Copied database.")))
