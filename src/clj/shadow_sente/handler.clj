(ns shadow-sente.handler
  (:require
   [compojure.core :refer [GET POST defroutes]]
   [compojure.route :refer [resources]]
   [hiccup.page :refer [html5 include-js include-css]]
   [ring.middleware.anti-forgery :as anti-forgery]
   [ring.middleware.defaults]
   [ring.middleware.keyword-params]
   [clojure.core.async :as async  :refer (<! <!! >! >!! put! chan go go-loop)]
   [ring.middleware.reload :refer [wrap-reload]]
   [ring.util.response :refer [resource-response]]
   [shadow.http.push-state :as push-state]
   [taoensso.sente :as sente]
   [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]))


(let [;; Serializtion format, must use same val for client + server:
      packer :edn ; Default packer, a good choice in most cases
      ;; (sente-transit/get-transit-packer) ; Needs Transit dep

      chsk-server
      (sente/make-channel-socket-server!
       (get-sch-adapter) {:packer packer})

      {:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      chsk-server]

  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )


(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id)


(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data client-id]}]
  ;; (-event-msg-handler ev-msg) ; Handle event-msgs on a single thread
  (future (-event-msg-handler ev-msg))) ; Handle event-msgs on a thread pool


(defmethod -event-msg-handler :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (println "Unhandled event on server: %s %s" id ?data))


(sente/start-server-chsk-router!
 ch-chsk event-msg-handler)


(defn index-html [req]
  (html5
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
    (include-css "http://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/3.3.5/css/bootstrap.min.css"
                 "vendor/css/material-design-iconic-font.min.css"
                 "vendor/css/re-com.css"
                 "http://fonts.googleapis.com/css?family=Roboto:300,400,500,700,400italic"
                 "http://fonts.googleapis.com/css?family=Roboto+Condensed:400,300")]
   [:body
    [:div#app {:data-csrf-token (:anti-forgery-token req)}]
    (include-js "/js/compiled/app.js")]))

(defroutes routes
  (GET "/" req (index-html req))
  (GET "/index" req (index-html req))
  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post req))
  (resources "/"))

(def dev-handler (-> #'routes
                     wrap-reload
                     ring.middleware.keyword-params/wrap-keyword-params
                     ring.middleware.params/wrap-params
                     (ring.middleware.defaults/wrap-defaults
                      ring.middleware.defaults/site-defaults)
                     ))

(def handler routes)
