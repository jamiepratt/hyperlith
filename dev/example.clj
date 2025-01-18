(ns example
  (:gen-class)
  (:require [hyperlith.core :as h]
            [clojure.string :as str]))

(def schema
  (merge ;; Using merge we can define each logical entity separately
    #:user
    {:sid {:db/unique      :db.unique/identity
           :db/valueType   :db.type/string
           :db/cardinality :db.cardinality/one}}
    #:message
    {:id      {:db/unique      :db.unique/identity
               :db/valueType   :db.type/string
               :db/cardinality :db.cardinality/one}
     :user    {:db/valueType   :db.type/ref
               :db/cardinality :db.cardinality/one}
     :content {:db/valueType   :db.type/string
               :db/cardinality :db.cardinality/one
               :db/fulltext    true}}))

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
        [:p {:id id} content])
      [:input {:type "text" :data-bind "message"}]
      [:button
       {:data-on-click "@post('/send'); $message = ''"} "send"]]]))

(defn action-send-message [{:keys             [sid db]
                            {:keys [message]} :body}]
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
  (h/start-app {:routes routes :db (h/create-db "db" schema)}))

(comment
  (def serv (-main))
  ;; (clojure.java.browse/browse-url "http://localhost:8080/")

  ;; stop server
  (serv)
  )

(comment
  ;; probably want a way to access the db from repl
  (def db (h/create-db "db" schema))

  (get-messages db)
  )
