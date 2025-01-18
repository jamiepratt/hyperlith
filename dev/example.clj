(ns example
  (:gen-class)
  (:require [hyperlith.core :as h]))

(defonce state_
  (let [state_ (atom {:messages []})]
    (add-watch state_ :refresh-on-change (fn [& _] (h/refresh-all!)))
    state_))

(defn render-home [_req]
  (h/html-str
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
  {[:get "/"]         (h/shim-handler   {:path "/"})
   [:post "/updates"] (h/render-handler #'render-home)
   [:post "/submit"]  (h/action-handler #'action-change-message)})

(defn -main [& _]
  (h/start-app {:routes routes}))

;; csrf

(comment
  (def serv (-main))
  ;; (clojure.java.browse/browse-url "http://localhost:8080/")

  ;; stop server
  (serv)
  )
