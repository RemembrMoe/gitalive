(ns gitalive.handler
  (:require [clojure.core.async :refer [<! <!! chan go-loop thread]]
            [ring.util.response :as response]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit
             :refer [sente-web-server-adapter]]
            [gitalive.templates :refer [index]]
            [gitalive.queue :as queue]))

(defn init []
  (do
    ;; (timbre/merge-config!
    ;;  {:level (if (env :dev) :trace :info)})
    (println
     (str "\n-=[gitalive started successfully"
          ;; (when (env :dev) " using the development profile")
          "]=-"))))

(defn destroy []
  (println "gitalive is shutting down...")
  (println "shutdown complete!"))

;; (def app-routes
;;   (routes
;;    (wrap-routes #'home-routes middleware/wrap-csrf)
;;    (route/not-found
;;     (:body
;;      (error-page {:status 404
;;                   :title "page not found"})))))

;; (def app (middleware/wrap-base #'app-routes))

(defonce sente-socket
  (sente/make-channel-socket!
   sente-web-server-adapter
   {:user-id-fn (fn [ring-req] (:client-id ring-req))}))

(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      sente-socket]
  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  ;; Receive channel
  (def ch-chsk ch-recv)
  ;; Send API callback
  (def chsk-send! send-fn)
  ;; Atom of connected clients. Watchable and read-only
  (def connected-uids connected-uids))


(defroutes app-routes
  (route/resources "/public/")
  (GET "/" [] (index))
  ;; Sente's routes
  (GET "/channel" req (ring-ajax-get-or-ws-handshake req))
  (POST "/channel" req (ring-ajax-post req)))

(defn get-session-uid [req]
  (get-in [:session :uid] req))

;; Handle Sente's events
(defmulti handle-client-events :id)

(defmethod handle-client-events :queue/fetch
  [{:keys [id uid client-id ?data ?reply-fn]}]
  (println "[" id " ?data] : " ?data)
  (when ?reply-fn
    (?reply-fn (reverse (sort-by :date (queue/obtain))))))

(defmethod handle-client-events :queue/new-repo
  [{:keys [id uid client-id ?data ?reply-fn]}]
  (println "[" id " ?data] : " ?data)
  (let [ret (queue/add-new-entry queue/a-user ?data)]
    (when ?reply-fn
      (?reply-fn ret))))

(defmethod handle-client-events :default
  [{:keys [event id ?data send-fn ?reply-fn uid ring-req client-id]
    :as ev-msg}]
  (when ?reply-fn
    (?reply-fn {:umatched-event-as-echoed-from-from-server event})))

(queue/watch :update-queue
             (fn [key ref old-val new-val]
               (println "[watcher :update-queue] : " key ref)
               (doseq [uid (:any @connected-uids)]
                 (chsk-send! uid [:queue/update new-val]))))

(defn start-channel-socket-router! []
  (sente/start-chsk-router! ch-chsk handle-client-events))

;; Ring handler
(def app
  (let [ring-defaults-config
        (assoc-in site-defaults [:security :anti-forgery]
                  {:read-token (fn [req] (-> req :params :csrf-token))})]
    (-> app-routes
       (#(wrap-defaults % ring-defaults-config))
       wrap-session
       wrap-keyword-params
       wrap-params)))
