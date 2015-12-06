(ns gitalive.server
  (:require [org.httpkit.server :as http-kit]
            [gitalive.queue :as queue]
            [gitalive.handler :refer [app init destroy
                                      start-channel-socket-router!]])
  (:gen-class))

(defonce ^:private web-server (atom nil))

(defn parse-port [port]
  (when port
    (cond
      (string? port) (Integer/parseInt port)
      (number? port) port
      :else (throw (Exception. (str "invalid port value: " port))))))

(defn http-port [port]
  (parse-port (or port 3000)))

(defn start-web-server!* [ring-handler port]
  (println "Starting web server (http-kit)...")
  (let [http-kit-stop-fn (http-kit/run-server ring-handler {:port port})]
    {:server nil
     :port (:local-port (meta http-kit-stop-fn))
     :stop-fn (fn [] (http-kit-stop-fn :timeout 100))}))

(defn stop-web-server! []
  (when-let [f @web-server] ((:stop-fn f))))
(defn start-web-server! [& [port]]
  (stop-web-server!)
  (let [{:keys [stop-fn port] :as server-map}
        (start-web-server!* (var app) port)
        uri (format "http://localhost:%s/" port)]
    (println "Web server is running at " uri)
    ;; (try
    ;;   (.browse (java.awt.Desktop/getDesktop) (java.net.URI. uri))
    ;;   (catch java.awt.HeadlessException _))
    (reset! web-server server-map)))

(defonce router (atom nil))

(defn stop-router! [] (when-let [stop-f @router] (stop-f)))
(defn start-router! []
  (println "Start channel socket router")
  (stop-router!)
  (reset! router (start-channel-socket-router!)))

(defn stop-queue! [] (queue/reset-queue!))
(defn start-queue! []
  (println "Start queue and pub/sub")
  (stop-queue!))

(defn stop-app! []
  ;; (stop-nrepl)
  (stop-web-server!)
  (shutdown-agents))

(defn start-app! [[port]]
  (let [port (http-port port)]
    (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app!))
    ;; (start-nrepl)
    (start-queue!)
    (start-router!)
    (start-web-server! port)))

(defn -main [& args]
  (start-app! args))
