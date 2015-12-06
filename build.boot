(set-env!
 :source-paths #{"src"}
 :resource-paths #{"resources"}
 :dependencies '[[clj-time "0.11.0"]
                 [clj-jgit "0.8.8"]
                 [reagent "0.5.1"]
                 [garden "1.2.5"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [compojure "1.4.0"]
                 [http-kit "2.1.19"]
                 [com.taoensso/sente "1.6.0"]
                 [hiccup "1.0.5"]
                 [hiccups "0.3.0"]

                 ;; Dev tools
                 [weasel "0.7.0"]
                 [adzerk/boot-cljs "1.7.48-6" :scope "test"]
                 [adzerk/boot-cljs-repl "0.2.0" :scope "test"]
                 [adzerk/boot-reload "0.4.1" :scope "test"]
                 [pandeiro/boot-http "0.7.0" :scope "test"]
                 [crisptrutski/boot-cljs-test "0.2.0-SNAPSHOT" :scope "test"]
                 [org.clojure/tools.namespace "0.2.11" :scope "test"]
                 [dgellow/boot-hiccup "0.2.0-SNAPSHOT" :scope "test"]
                 [org.martinklepsch/boot-garden "1.2.5-7"]

                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.145"]
                 [org.clojure/core.async "0.2.374"]])

(require
 '[adzerk.boot-cljs :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload :refer [reload]]
 '[crisptrutski.boot-cljs-test  :refer [test-cljs]]
 '[dgellow.boot-hiccup :refer [hiccup]]
 '[org.martinklepsch.boot-garden :refer [garden]])

(task-options! pom {:project 'dgellow/gitalive
                    :version "0.1.0-SNAPHOT"}
               aot {:namespace #{'gitalive.server}}
               jar {:main 'gitalive.server})

(deftask deps [])

(deftask dev
  "Start dev front environment: cljs live-reload, nRepl, etc"
  []
  (task-options! garden {:pretty-print true})
  (comp (watch)
        (speak)
        (reload :on-jsload 'gitalive.front/main)
        (cljs-repl)
        (cljs :source-map true :optimizations :none)))

(deftask build-clj
  "Build backend package"
  []
  (comp (aot)
        (pom)
        (uber)
        (jar)))

(deftask build-cljs
  "Compile frontend"
  []
  (comp (cljs :optimizations :advanced)))

(deftask build
  "Build and package the project"
  []
  (comp (build-cljs)
        (build-clj)))
