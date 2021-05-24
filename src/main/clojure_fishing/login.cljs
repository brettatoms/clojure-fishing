(ns clojure-fishing.login
  (:require [clojure-fishing.components :refer [input]]))

(defn login [event]
  (.preventDefault event)
  (.stopPropagation event)
  (js/console.log "LOGGING IN"))

(defn login-form []
  [:form {:className "flex flex-col justify-center"
          :onSubmit login}
   [input {:type "text" :placeholder "email"}]
   [input {:type "text"
           :placeholder "password"}]
   [input {:type "submit" :value "Login"}]])

(defn login-page []
  [:div {:className "login-page w-screen h-screen"}
   [:div {:className "flex-1 justify-center"}
    [login-form]]])


;; (defn login-page []
;;   [:div {:className "login-page"}])
