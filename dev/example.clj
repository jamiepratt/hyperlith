(ns example
  (:gen-class)
  (:require [hyperlith.core :as l]))

(defonce state_
  (let [state_ (atom {:messages []})]
    (add-watch state_ :refresh-on-change (fn [& _] (l/refresh-all!)))
    state_))

(defn render-home [_req]
  (l/html-str
    [:main#morph
     [:div
      (for [message (@state_ :messages)]
        [:p message])
      [:div
       [:input {:type "text" :data-bind "message"}]]
      [:button
       {:data-on-click "@post('/submit'); $message = ''"} "submit"]]]))

(defn action-change-message [req]
  (swap! state_ update :messages conj (-> req :body :message)))

(def routes
  {[:get "/"]         (l/shim-handler   {:path "/"})
   [:post "/updates"] (l/render-handler #'render-home)
   [:post "/submit"]  (l/action-handler #'action-change-message)})

(defn -main [& _]
  (l/start-app {:routes routes}))

;; csrf

(comment
  (def serv (-main))
  ;; (clojure.java.browse/browse-url "http://localhost:8080/")

  ;; stop server
  (serv)
  )
