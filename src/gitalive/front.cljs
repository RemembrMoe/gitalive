(ns gitalive.front
  (:require-macros [cljs.core.async.macros :as async-m :refer [go go-loop]])
  (:require [reagent.core :as r]
            [cljs.core.async :as async :refer [<! >! put! chan]]
            [taoensso.sente :as sente :refer [cb-success?]]))

;; State management
(def app-state (r/atom {:queue []}))

(defn put-state! [k v]
  (swap! app-state assoc k v))

(defn update-in-state! [ks f & args]
  (clojure.core/swap!
   app-state
   #(apply (partial update-in % ks f) args)))

(defn assoc-in-state! [ks v]
  (swap! app-state assoc-in ks v))

(defn insert-in-state! [k v]
  (put-state! k (conj (k @app-state) v)))

;; Channel socket
(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/channel" {:type :auto})]
  (def chsk chsk)
  ;; Receive channel
  (def ch-chsk ch-recv)
  ;; Send API callback
  (def chsk-send! send-fn)
  ;; Shared state. Watchable and read-only atom
  (def chsk-state state))

;; Server push
(defmulti handle-server-push (fn [id data] id))

(defmethod handle-server-push :queue/update
  [id data]
  (js/console.log "[:queue/update data] : " (clj->js data))
  (when data
    (put-state! :queue data)))

(defmethod handle-server-push :default
  [id ev-msg]
  (js/console.warn "Unhandled push")
  (js/console.log "id" id)
  (js/console.log "ev-msg" (clj->js ev-msg)))

;; Server events
(defmulti handle-server-events :id)

(defmethod handle-server-events :chsk/recv
  [{:as ev-msg :keys [id ?data]}]
  (js/console.log "Push event from server: " (clj->js ev-msg) ev-msg)
  (handle-server-push (first ?data) (flatten (rest ?data))))

(defmethod handle-server-events :default ; Fallback
  [{:keys [event id]}]
  (js/console.warn "Unhandled event")
  (js/console.log "id: %s" (clj->js id))
  (js/console.log "event: %s" (clj->js event)))

;; Components
(defn component-form []
  (r/create-class
   {:display-name "form-new-entry"
    :reagent-render
    (let [val-name (r/atom "bailer")
          val-clone-url (r/atom "https://github.com/dgellow/bailer.git")]
      (fn []
        [:div.component-form
         [:h2 "Register a repository"]
         [:form {:on-submit
                 #(do (.preventDefault %)
                      (chsk-send! [:queue/new-repo
                                   {:clone-url @val-clone-url
                                    :name @val-name}]))}
          [:div.group
           [:label "Name"]
           [:input {:type "text" :value @val-name
                    :name "repo-name"
                    :on-change
                    #(reset! val-name (-> % .-target .-value))}]]
          [:div.group
           [:label "Clone url"]
           [:input {:type "text" :value @val-clone-url
                    :name "repo-clone-url"
                    :on-change
                    #(reset! val-clone-url (-> % .-target .-value))}]]
          [:input.submit {:type "submit"}]]]))}))

(defn component-entry
  [{id :id status :status user :user repo :repo date :date}]
  [:div.entry {:class-name status}
   [:h3.repo-name (:name repo)]
   [:div.data
    [:div.repo-url [:a {:href (:clone-url repo)} (:clone-url repo)]]
    [:div.id (str " #" (first (clojure.string/split id #"-")))]]])

(defn component-queue []
  (r/create-class
   {:display-name "queue"
    :component-did-mount
    (fn [_]
      (chsk-send!
       [:queue/fetch] 5000
       (fn [response]
         (if (sente/cb-success? response)
           (when response
             (js/console.log (clj->js response))
             (put-state! :queue response))
           (js/console.error response)))))
    :reagent-render
    (fn []
      (let [data (:queue @app-state)
            _ (js/console.log (clj->js data))
            entries (->> (:queue @app-state)
                         (sort-by :date)
                         reverse
                         (map component-entry)
                         (map #(vector :li {:key (js/Math.random)} %)))]
        [:div.component-queue
         [:h2 "Queue"]
         [:ul.queue entries]]))}))

;; Pages
(defn page-basic []
  (fn [cmpnt]
    [:div
     [:header.layout
      [:h1 "git:alive"]]
     [:div.content.layout
      [component-form]
      [component-queue]]]))

;; Entry point
(def chsk-router (atom nil))
(defn stop-chsk-router! [] (when-let [stop-f @chsk-router] (stop-f)))
(defn start-chsk-router! []
  (stop-chsk-router!)
  (reset! chsk-router (sente/start-chsk-router! ch-chsk handle-server-events)))

(defn start! []
  (js/console.log "Start channel socket router")
  (start-chsk-router!))

(defn main []
  (start!)
  (r/render [page-basic]
            (.getElementById js/document "container")))

;; (js/window.setInterval
;;  (fn []
;;    (chsk-send!
;;     [:test/ping :alive?] 5000
;;     (fn [response]
;;       (if (sente/cb-success? response)
;;         (js/console.log response)
;;         (js/console.error response)))))
;;  10000)
