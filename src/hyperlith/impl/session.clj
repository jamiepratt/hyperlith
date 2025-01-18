(ns hyperlith.impl.session
  (:require [hyperlith.impl.util :as u]))

(defn get-sid [req]
  (some->> (get-in req [:headers "cookie"])
    (re-find #"__Host-sid=([^;^ ]+)")
    second))

(defn wrap-session
  [handler]
  (fn [req]
    (let [sid (get-sid req)]
      (cond
        ;; User already has session
        sid
        (handler (assoc req :session/id sid))

        ;; :get request and user does not have session we create one
        (= (:request-method req) :get)
        (let [new-sid (when-not sid (u/random-unguessable-uid))]
          (-> (handler (assoc req :session/id new-sid))
            (assoc-in [:headers "Set-Cookie"]
              ;; This cookie won't be set on local host on chrome as
              ;; it's using secure needs to be true and local host
              ;; does not have HTTPS. SameSite is set to lax as it
              ;; allows the same cookie session to be used following a
              ;; link from another site.
              (str "__Host-sid=" new-sid
                "; Path=/; Secure; HttpOnly; SameSite=Lax"))))

        ;; not a :get request and user does not have session we redirect
        :else
        {:status 403}))))

