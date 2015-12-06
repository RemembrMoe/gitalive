(ns gitalive.templates
  #?(:cljs
     (:require-macros [hiccups.core :refer [html]]))
  (:require
   #?@(:clj [[hiccup.page :refer [html5]]]
       :cljs [[hiccups.runtime]])))

#?(:cljs
   (defn html5 [content]
     (str "<!doctype html>"
          (html content))))

(defn index []
  (html5
   [:html
    [:head
     ;; [:link {:rel "stylesheet" :href "/css/screen.css"}]
     ]
    [:body
     [:div#container]
     [:script {:type "text/javascript" :src "/js/main.js"}]]]))
