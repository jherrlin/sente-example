(ns shadow-sente.core
  (:require-macros
   [cljs.core.async.macros :as asyncm :refer (go go-loop)])
  (:require
   [cljs.core.async :as async :refer (<! >! put! chan)]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [shadow-sente.config :as config]
   [shadow-sente.events :as events]
   [shadow-sente.views :as views]
   [taoensso.sente :as sente :refer (cb-success?)]
   ))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root)


  (let [?csrf-token (when-let [el (.getElementById js/document "app")]
                      (.getAttribute el "data-csrf-token"))]
    (if (not ?csrf-token)
      (js/console.log "CSRF token not found or session-id not set. Websocket wont start.")
      (try
        (let [{:keys [chsk ch-recv send-fn state]}
              (sente/make-channel-socket! "/chsk" ; Note the same path as before
                                          ?csrf-token
                                          {:host "localhost:3000"})]
          (def chsk       chsk)
          (def ch-chsk    ch-recv) ; ChannelSocket's receive channel
          (def chsk-send! send-fn)
          (def chsk-state state)   ; Watchable, read-only atom


          (defmulti -event-msg-handler :id)

          (defn event-msg-handler
            "Wraps `-event-msg-handler` with logging, error catching, etc."
            [{:as ev-msg :keys [id ?data]}]
            (js/console.log "incomming on ws from server" id ?data)
            (-event-msg-handler ev-msg))

          (defmethod -event-msg-handler :default ; Default/fallback case (no other matching handler)
            [{:as ev-msg :keys [event ?data]}]
            (js/console.log "john-debug default:" ev-msg))


          (sente/start-client-chsk-router!
           ch-chsk event-msg-handler)
          )
        (catch js/Error e
          (js/console.log e)))))

  )
