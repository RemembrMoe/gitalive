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
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:link {:rel "stylesheet" :type "text/css" :href "https://fonts.googleapis.com/css?family=Source+Code+Pro:400,200,300,500,700,900,600"}]
     [:link {:rel "stylesheet" :href "/css/screen.css"}]]
    [:body
     [:div#container]
     [:script {:type "text/javascript" :src "/js/main.js"}]]]))
