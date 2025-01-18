(ns example.main
  (:gen-class)
  (:require [hyperlith.core :as h]
            [clojure.string :as str]
            [example.schema :refer [schema]]))

(defn get-messages [db]
  (h/q '[:find ?id ?content ?created-at
         :where
         [?m :message/id ?id]
         [?m :message/content ?content]
         [?m :db/created-at ?created-at]
         :order-by [?created-at :desc]
         :limit 100]
    @db))

(defn render-home [{:keys [db] :as _req}]
  (h/html-str
    [:main#morph
     [:div
      (for [[id content] (get-messages db)]
        [:p {:id id} content])]
     [:input {:type "text" :data-bind "message"}]
     [:button
      {:data-on-click "@post('/send'); $message = ''"} "send"]]))

(defn action-send-message [{:keys [sid db] {:keys [message]} :body}]
  (when-not (str/blank? message)
    (h/transact! db
      [{:user/sid sid}
       {:message/id      (h/new-uid)
        :message/user    [:user/sid sid]
        :message/content message}])))

(def routes
  {[:get "/"]         (h/shim-handler   {:path "/"})
   [:post "/updates"] (h/render-handler #'render-home)
   [:post "/send"]  (h/action-handler #'action-send-message)})

(defn -main [& _]
  (h/start-app {:routes routes :schema schema}))

(comment
  (def server (-main))
  ;; (clojure.java.browse/browse-url "http://localhost:8080/")

  ;; stop server
  (let [stop (server :stop)] (stop))

  ;; query outside of handler
  (get-messages (:db-conn server))
  ,)
