(ns gitalive.git
  (:require [clj-jgit.porcelain :refer [git-clone-full git-branch-list]]
            [clj-time.core :as t]
            [clj-time.format :as f]))

(def root-dir "data")

(defn make-path [user repo]
  (let [current-time-str (f/unparse (f/formatters :basic-date-time-no-ms)
                                    (t/now))
        user-name (:name user)
        repo-name (:name repo)]
    (clojure.string/join
     "/"
     (list root-dir user-name repo-name current-time-str))))

(defn clone [clone-url path]
  (let [repo-obj (git-clone-full clone-url path)]
    repo-obj))

(defn snapshot-repo [user repo]
  (let [path (make-path user repo)
        clone-obj (clone (:clone-url repo) path)]
    (:repo clone-obj)))

(defn get-branches [repo-obj]
  (let [local-branches (git-branch-list repo-obj)
        remote-branches (git-branch-list repo-obj :remote)
        all-branches (git-branch-list repo-obj :all)
        get-names (fn [branches] (map #(.getName %) branches))]
    {:local (get-names local-branches)
     :remote (get-names remote-branches)
     :all (get-names all-branches)}))
