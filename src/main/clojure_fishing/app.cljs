(ns clojure-fishing.app
  (:require [clojure-fishing.components :refer [input]]
            [clojure-fishing.env :as env]
            [clojure-fishing.login :as login]
            ;; [goog.string :as gstring :refer [format]]
            [goog.string :as gstring]
            ;; [goog.string.format]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [reitit.coercion.spec :as rts]
            [reitit.core :as rt]
            [reitit.frontend.easy :as rtfe]
            ["@supabase/supabase-js" :as sb]))

(defonce supabase-client (r/atom (sb/createClient env/supabase-url env/supabase-anon-key)))
(defonce items (r/atom []))
(defonce route (r/atom nil))
(defonce search-term (r/atom ""))

(defn search
  "Returns a promise with the search results"
  [q]
  (-> @supabase-client
      ;; (.rpc "search_projects" #js {:q q})
      (.from "vw_project")
      (.select "*")
      ;; TODO: couldn't get the project_search function to
      ;; work but this seems fast enough for now...but can't
      ;; search tags
      ;; (.or (gstring/format "description.ilike.%%%s%%,name.ilike.%%%s%%" q q))
      (.or (str "description.ilike.%%" q "%%,name.ilike.%%" q "%%"))
      (.order "name")))

(defn search-input []
  [:input {:type "search"
           :placeholder "Go fishing..."
           :className "flex-1"
           :value @search-term
           :onChange (fn [e]
                       (let [q (.. e -target -value)]
                         (reset! search-term q)
                         (if (and q (not (zero? (.-length q))))
                           (-> (search q)
                               (.then #(reset! items (. % -data)))
                               ;; TODO: handle error
                               (.catch #(reset! items %)))
                           (reset! items []))))}])

(defn search-params
  ([]
   (search-params (.. js/window -location -href)))
  ([url]
   (-> url js/URL. .-searchParams)))

(defn is-logged-in []
  (not (nil? (-> @supabase-client .-auth .user))))

(defn navbar []
  [:div {:id "navbar" :className "p-2 flex flex-row w-full justify-end"}
   (when (is-logged-in)
     [:a
      {:href "/logout"
       :className ""}
      "Logout"])])

(defn tag [text]
  [:div {:className "px-2 py-1 rounded-full bg-gray-100 text-xs"} text])

(defn list-item [item]
  [:li
   {:className "py-4 sm:px-4 flex-1"}
   [:div {:className "flex-col items-start"}
    [:div {:className "flex-col sm:flex-row items-baseline"}
     [:span {:className "text-xl"} (. item -name)]
     [:span {:className "text-sm text-gray-500 sm:px-8"}
      (when-let [repo-url (aget item "repo_url")]
        [:a {:href repo-url} repo-url])]
     [:span {:className "text-sm text-gray-500 sm:px-8"}
      (when-let [stars-count (aget item "stars_count")]
        (str stars-count " stars"))]]

    (if-let [description (aget item "description")]
      [:div {:className "pt-2"} description]
      (gstring/unescapeEntities "&nbsp;"))
    ;; [:div
    ;;  (for [text (aget item "tags")]
    ;;    [:div {:className "mx-1"}
    ;;     [tag text]])]
    ]])

(defn item-list []
  [:ul {:className "flex-1"}
   (for [item @items]
     ^{:key (:name item)}
     [:div {:className "my-2 flex-1 flex-col"}
      [list-item item]
      [:div {:className "border-gray-100 border  w-full"}]])])

(defn header []
  [:div  {:className "flex-1 justify-center items-center md:py-8"}
   [:img {:src "/img/fishing-clipart-fly-fishing-2.jpg"
          :width 200
          :height 160}]
   [:h1 {:className "font-title text-7xl font-light pr-10 md:pr-0"} "Clojure Fishing"]])

(defn home-page []
  [:div {:id "home-page"
         :className "flex flex-col w-full"}
   [navbar]
   [:div {:id "container"
          :className "flex-col xl:px-96 lg:px-64 md:px-24 sm:px-12 px-8 md:pb-8 md:pt-0"}
    [header]
    [:div {:className "w-full flex-col"}
     [search-input {:className "w-full bg-red-500"}]
     (if (or (not= @search-term "") (> (count @items) 0))
       [:span {:className "text-sm text-gray-400"} (str (count @items) " matches")]
       (when (= @search-term "")
         [:div {:className "full-w justify-center text-lg text-gray-500 py-12 text-center italic"}
          "Search for open-source Clojure projects"]))]

    [:div {:className "md:py-8"}
     [item-list]]]])

(defn logout-page []
  (-> @supabase-client .-auth .signOut)
  (set!  (.. js/window -location -href) "/"))

(defn current-page []
  (when @route
    (let [view (:view (:data @route))]
      [view @route])))

(def routes
  [["/" {:name ::home
         :view home-page}]
   ["/login" {:name ::login
              :view login/login-page}]
   ["/logout" {:name ::logout
               :view logout-page}]])

(defn mount-root []
  (rtfe/start!
   (rt/router routes {:data {:coercion rts/coercion}})
   (fn [m] (reset! route m))
    ;; set to false to enable HistoryAPI
   {:use-fragment false})
  (rdom/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
