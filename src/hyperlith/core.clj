(ns hyperlith.core
  (:require [hyperlith.impl.session :refer [wrap-session]]
            [hyperlith.impl.headers :refer [default-headers]]
            [hyperlith.impl.json :refer [wrap-parse-json-body]]
            [hyperlith.impl.datastar :as ds]
            [hiccup2.core :as h]
            [org.httpkit.server :as hk]
            [datalevin.core :as d]
            [datalevin.lmdb :as l])
  (:import (datalevin.db DB)
           (datalevin.storage Store)))

(def ^:private connections_ (atom {}))

(defn- refresh-all! []
  (run! (fn [f] (f)) (vals @connections_)))

(defn- send! [ch event close-after-send?]
  (hk/send! ch {:status  200
                :headers (assoc default-headers
                           "Content-Type"  "text/event-stream"
                           "Cache-Control" "no-store")
                :body    event}
    close-after-send?))

;; DB
(def q d/q)
(def transact! d/transact!)
(def update-schema d/update-schema)

(defn new-uid
  "Allows us to change id implementation if need be."
  []
  (str (random-uuid)))

(defn backup-copy
  "Make a backup copy of the database. `dest-dir` is the destination
  data directory path. Will compact while copying if `compact?` is true."
  [conn dest-dir compact?]
  (let [lmdb (.-lmdb ^Store (.-store ^DB conn))]
    (println "Copying...")
    (l/copy lmdb dest-dir compact?)
    (println "Copied database.")))

;; HTML
(defmacro html-str [hiccup]
  `(-> (h/html ~hiccup) str))

;; ROUTING
(defn router [db routes not-found-handler]
  (let [routes            (merge ds/default-routes routes)
        default-handler   (fn [_] {:status 303 :headers {"Location" "/"}})
        not-found-handler (or not-found-handler default-handler)]
    (fn [req]
      ((routes [(:request-method req) (:uri req)] not-found-handler)
       (assoc req :db db)))))

;; HANDLERS
(defn shim-handler [opts]
  (let [resp (ds/build-shim-page-resp opts)]
    (fn handler [_req] resp)))

(defn action-handler [thunk]
  (fn handler [req]
    (thunk req)
    {:status  204
     :headers default-headers}))

(defn render-handler [render-fn]
  (fn handler [req]
    (hk/as-channel req
      {:on-open
       (fn on-open [ch]
         ;; Get latest render on connect/reconnect
         (send! ch (ds/merge-fragments (render-fn req)) false)
         ;; store render function in connections_
         (swap! connections_ assoc ch
           (fn render []
             (send! ch (ds/merge-fragments (render-fn req)) false))))
       :on-close
       (fn on-close [ch _] (swap! connections_ dissoc ch))})))

;; APP
(defn start-app [{:keys [routes not-found-handler port schema
                         csrf-secret]}]
  (let [db          (d/get-conn "db" schema
                      {:validate-data?    true
                       :closed-schema?    true
                       :auto-entity-time? true})
        stop-server (-> (router db routes not-found-handler)
                      (wrap-session csrf-secret)
                      wrap-parse-json-body
                      (hk/run-server {:port (or port 8080)}))]
    (d/listen! db :refresh-on-change
      (fn [_] (refresh-all!)))
    {:db-conn db
     :stop    (fn stop []
                (d/close db)
                (stop-server))}))

;; Compress SSE stream
;; reactive using signals
;; solve CSS
