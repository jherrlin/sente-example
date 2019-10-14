(ns shadow-sente.server
  (:require [shadow-sente.handler :refer [handler]]
            [config.core :refer [env]]
            [org.httpkit.server :as hk]
            )
  (:gen-class))

(defn -main [& [port]]
  (let [parse-port (fn [port]
                     (try
                       (Integer/parseInt port)
                       (catch Exception e port)))]
    (hk/run-server handler {:port (or (parse-port (:port env))
                                      (parse-port "3000"))})))
