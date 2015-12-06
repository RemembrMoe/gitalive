(ns gitalive.styles
  (:require [garden.def :refer [defrule defstyles]]
            [garden.stylesheet :refer [rule]]
            [garden.selectors :as s]
            [garden.units :as u :refer [px percent]]))

(defstyles base
  (let [* (rule :*)
        body (rule :body)
        h* (apply rule (map #(str "h" %) (range 1 7)))]
    (vector
     (body
       {:font-family "\"Source Code Pro\",serif"
        :font-size (px 14)
        :padding 0
        :margin 0
        :background-color ""})
     (h*
      {:margin 0
       :padding 0
       :font-weight 300
       :padding-bottom (px 10)}))))

(defstyles layout
  (let [header (rule :header.layout)
        content (rule :.content.layout)
        component-top-level (s/> :#container :*)]
    (vector
     [:html :body :#container component-top-level {:height (percent 100)}]
     [component-top-level {:display "flex"
                           :flex-direction "column"}]
     (header
       {:font-family "\"Source Code Pro\", serif"
        :padding (px 10)
        :color "black"
        :font-weight 400
        :border-bottom "2px solid"}
       [:h1 {:font-weight 300
             :font-size (px 40)}])
     (content
      {:padding-top (px 40)
       :max-width (px 800)
       :width (percent 100)
       :align-self "center"
       :flex 1
       :padding-left (px 30)
       :padding-right (px 30)
       :box-sizing "border-box"
       :display "flex"
       :flex-direction "column"})
     [(s/> (first ((rule :.content.layout))) :div)
      {:padding-top (px 30)}])))

(defstyles form-new-repo
  (let [form (rule :.component-form [:form])]
    (form
      [:.group {:padding (px 10)}]
      [:label :input {:width (percent 30)
                      :display "inline-block"}])))

(defstyles queue
  (let [component (rule :.component-queue)]
    (component
     {:display "flex"
      :flex-direction "column"
      :align-self "stretch"
      :flex 1}

     [:.queue {:padding (px 30)
               :border "1px solid #C3C3C3"
               :flex 1
               :align-self "stretch"
               :margin 0
               :padding-bottom 0
               :list-style "none"
               :background-color "#FBFBFB"}]

     [:li {:margin-bottom (px 15)}]

     [:.entry {:border "1px solid #ECEBEB"
               :padding (px 15)
               :background-color "white"
               :box-shadow "0 0 4px rgba(212, 212, 212, 0.64)"
               :border-left "5px solid black"}
      [:&.processing {:border-left-color "orange"}]
      [:&.finished {:border-left-color "#9CDC9C"}]])))

(defstyles login-page
  (let [container (rule :#login-page)]
    (container
     [:.form-wrapper {:display "flex"
                      :align-items "center"
                      :justify-content "center"
                      :flex-direction "column"
                      :height "100%"}]
     [:form {:display "flex"
             :flex-direction "column"}]
     [:fieldset {:display "inline-flex"
                 :margin-bottom (px 50)
                 :height (px 132)
                 :padding 0}]
     [:.logo {:background-image "url(/img/aztrana-logo-white.png)"
              :background-size "contain"
              :background-repeat "no-repeat"
              :margin-bottom (px 50)
              :height (px 150)
              :width (px 70)}]
     [:input :fieldset {:text-align "center"
                        :background-color "white"
                        :border-radius (px 10)
                        :border "none"
                        :padding-top (px 15)
                        :padding-bottom (px 15)
                        :width (px 400)}]
     [:submit {:height (px 62)}]

     {:background-image "url(/img/night-sky.jpg)"
      :background-size "cover"
      :position "fixed"
      :top 0
      :bottom 0
      :right 0
      :left 0})))

(defstyles screen
  (let [body (rule :body)]
    [base
     layout
     form-new-repo
     queue]))
