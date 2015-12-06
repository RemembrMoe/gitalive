(ns gitalive.queue
  (:require [clojure.core.async :as async
             :refer [chan go go-loop >! <! <!! >!! pub sub]]
            [gitalive.git :refer [snapshot-repo]]
            [clj-time.core :as t]
            [clj-time.format :as f]))

(defn dbug [when id prfx] (println "[" when "] : " id " : " prfx ))

(def a-user {:name "dgellow"})

(def a-repo {:clone-url "https://github.com/dgellow/bailer.git"
             :name "bailer"})

;; Queue
(defonce ^:private entries-queue (ref []))

(defn watch [k f]
  (add-watch entries-queue k f))

(defn obtain [] @entries-queue)

(defn reset-queue! []
  (dosync (ref-set entries-queue [])))

(defn ^:private add-entry [queue-ref entry]
  (dosync
   (alter queue-ref conj entry)))

(defn assoc-in-entry [queue-ref entry-id key val]
  (dosync
   (alter queue-ref
          (fn [x]
            (let [entries @queue-ref
                  entry-idx
                  (first (keep-indexed
                          (fn [idx entry]
                            (when (= entry-id (:id entry))
                              idx))
                          entries))]
              (assert entry-idx (str "Entry not found. Given id : " entry-id))
              (assoc-in entries [entry-idx key] val))))))

;; Pub / Sub
(def chan-dispatch (chan 100))
(def chan-new (chan 100))
(def chan-process (chan 1000))
(def chan-finish (chan 100))

(def publication (pub chan-dispatch :msg-type))

(sub publication :new-entry chan-new)
(sub publication :process chan-process)
(sub publication :finish chan-finish)

(defn take-and-welcome [queue-ref dispatch-ch from-ch]
  (go-loop []
    (let [{:keys [user repo] :as v} (<! from-ch)
          v-enhanced (assoc v :status :new
                            :id (str (java.util.UUID/randomUUID))
                            :date (f/unparse (:basic-date-time-no-ms f/formatters)  (t/now)))]
      (when v
        (dbug "welcome" (:id v-enhanced) "start")
        (add-entry queue-ref v-enhanced)
        (dbug "welcome" (:id v-enhanced) "after add-entry")
        (>! dispatch-ch (assoc v-enhanced :msg-type :process))))

    (recur)))

(defn take-and-process [queue-ref dispatch-ch from-ch]
  (go-loop []
    (let [{:keys [id user repo] :as v} (<! from-ch)
          dbg (partial dbug "process" id)]
      (when v
        (dbg "start")
        (assoc-in-entry queue-ref id :status :processing)
        (dbg "after assoc-in")
        (snapshot-repo user repo)
        (dbg "after snapshot")
        (>! dispatch-ch (assoc v :msg-type :finish))
        (dbg "after dispatch")))
    (recur)))

(defn take-and-finish [queue-ref from-ch]
  (go-loop []
    (let [{:keys [id user repo] :as v} (<! from-ch)]
      (dbug "finish" id (str "before start : " v))
      (when v
        (dbug "finish" id "start")
        (assoc-in-entry queue-ref id :status :finished)
        (dbug "finish" id "after assoc-in-entry")))
    (recur)))

;; Start channels listeners
(take-and-welcome entries-queue chan-dispatch chan-new)
(take-and-process entries-queue chan-dispatch chan-process)
(take-and-finish entries-queue chan-finish)

(defn add-new-entry [user repo]
  (go (>! chan-dispatch
          {:user user :repo repo :msg-type :new-entry})))
