(ns hyperlith.core
  (:require [hyperlith.impl.util :as u]
            [hyperlith.impl.session :refer [wrap-session]]
            [hyperlith.impl.headers :refer [default-headers]]
            [hiccup2.core :as h]
            [clojure.java.io :as io]
            [org.httpkit.server :as hk]
            [clojure.string :as str])
  (:import (java.io InputStream)))

;;; - INTERNAL -

(def ^:private doctype-html5 "<!DOCTYPE html>")

(def ^:private datastar-source-map
  {:status  200
   :headers (assoc default-headers
              "Content-Type"     "text/javascript"
              "Content-Encoding" "gzip")
   :body    (-> "datastar.js.map" io/resource slurp u/gzip)})

(def ^:private datastar
  {:path    "/datastar-v1.0.0-beta.1.js"
   :status  200
   :headers (assoc default-headers
              "Content-Type"     "text/javascript"
              "Content-Encoding" "gzip")
   :body    (-> "datastar.js" io/resource slurp u/gzip)})

(def ^:private icon
  {:path    "/icon.png"
   :status  200
   :headers (assoc default-headers "Content-Type" "image/png")
   :body    (-> "icon.png" io/resource io/input-stream
              InputStream/.readAllBytes)})

(def ^:private default-routes
  {[:get (datastar :path)]   (fn [_] datastar)
   [:get "/datastar.js.map"] (fn [_] datastar-source-map)
   [:get (icon :path)]       (fn [_] icon)})

(defn- merge-fragments [fragments]
  (str "event: datastar-merge-fragments\ndata: fragments "
              (str/replace fragments "\n" "\ndata: fragments ")
              "\n\n\n"))

(defn- send! [ch event close-after-send?]
  (hk/send! ch {:status  200
                :headers (assoc default-headers
                           "Content-Type"  "text/event-stream"
                           "Cache-Control" "no-store")
                :body    event}
    close-after-send?))

(defn- build-shim-page-resp [{:keys [path]}]
  {:status  200
   :headers (assoc default-headers "Content-Encoding" "gzip")
   :body
   (->> (h/html
          [:html  {:lang "en"}
           [:head
            [:meta {:charset "UTF-8"}]
            [:link {:rel  "icon" :type "image/png"
                    :href (icon :path)}]
            ;; Scripts
            [:script {:defer true :type "module"
                      :src   (datastar :path)}]
            ;; Enables responsiveness on mobile devices
            [:meta {:name    "viewport"
                    :content "width=device-width, initial-scale=1.0"}]]
           [:body {:data-on-load
                   (str "@post('" (if (= path "/") "" path)
                     "/updates')")}
            [:noscript "Your browser does not support JavaScript!"]
            [:main {:id "morph"}]]])
     (str doctype-html5)
     u/gzip)})

(def ^:private connections_ (atom {}))

;;; - PUBLIC API - 

(defmacro html-str [hiccup]
  `(-> (h/html ~hiccup) str))

(defn router [routes not-found-handler]
  (let [routes            (merge default-routes routes)
        default-handler   (fn [_] {:status 303 :headers {"Location" "/"}})
        not-found-handler (or not-found-handler default-handler)]
    (fn [req]
      ((routes [(:request-method req) (:uri req)] not-found-handler) req))))

(defn start-app [{:keys [routes not-found-handler port]}]
  (-> (router routes not-found-handler)
    wrap-session
    (hk/run-server {:port (or port 8080)})))

(defn shim-handler [opts]
  (let [resp (build-shim-page-resp opts)]
    (fn handler [_req] resp)))

(defn action-handler [thunk]
  (fn handler [req]
    (cond-> req
      :body (update :body u/json-stream->edn)
      true  thunk)
    {:status  204
     :headers default-headers}))

(defn render-handler [render-fn]
  (fn handler [req]
    (hk/as-channel req
      {:on-open
       (fn on-open [ch]
         ;; Get latest render on connect/reconnect
         (send! ch (merge-fragments (render-fn req)) false)
         ;; store render function in connections_
         (swap! connections_ assoc ch
           (fn render []
             (send! ch (merge-fragments (render-fn req)) false))))
       :on-close
       (fn on-close [ch _] (swap! connections_ dissoc ch))})))

(defn refresh-all! []
  (run! (fn [f] (f)) (vals @connections_)))
