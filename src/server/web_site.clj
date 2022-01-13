(ns server.web-site
  (:require
   [clojure.java.io :as io]
   [clojure.pprint :as pprint :refer [pprint]]
    [muuntaja.core :as m]
    [reitit.ring :as ring]
    [reitit.coercion.malli :as rcm]
    [reitit.ring.coercion :as coercion]
    [reitit.ring.malli]
    [reitit.spec :as rs]
    [reitit.swagger :as swagger]
    [reitit.swagger-ui :as swagger-ui]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.parameters :as parameters]
    [ring.adapter.jetty :as jetty]
    [clojure.string :as string]
    [hiccup.core :as h]
    [hiccup.page :as page])
  (:import (java.io File)))

(set! *warn-on-reflection* true)

(defn read-edn-file
  [file-name]
  (try
    (some-> file-name io/resource io/as-file slurp read-string)
    (catch Exception _)))

(defn list-episode-ids
  [dir-name]
  (try
    (some->> dir-name io/resource io/as-file file-seq
             (keep (fn [^File f]
                     (when (.isFile f)
                       (-> f
                           (.getName)
                           (string/replace #"\.edn$" "")
                           Integer/parseInt))))
             sort)
    (catch Exception _)))

(defn save-episode-data
  [{:keys [number]}]
  {:number number})

(defn episode-data
  [{:keys [number]}]
  (let [file-name (str "episodes/" number ".edn")]
    (merge {:number  number
            :url     (str "/episodes/" number)
            :api-url (str "/api/episodes/" number)}
           (read-edn-file file-name))))

(def swagger-route
  ["/swagger.json"
   {:get {:no-doc  true
          :swagger {:info {:title       "Apropos episodes API"
                           :description "with [malli](https://github.com/metosin/malli) and reitit-ring"}
                    :tags [{:name "episodes", :description "episodes api"}]}
          :handler (swagger/create-swagger-handler)}}])

(defn handle-save-request
  [{:keys [parameters]}]
  (some->> parameters :body save-episode-data (assoc {:status 200} :body)))

(defn handle-read-request
  [{:keys [parameters]}]
  (some->> parameters :path episode-data (assoc {:status 200} :body)))

(defn list-episodes []
  (some->> (list-episode-ids "episodes")
           (map #(assoc {} :number %))
           (map episode-data)))

(defn handle-all-episodes-request
  [_]
  (some->> (list-episodes)
           (hash-map :episodes)
           (hash-map :status 200 :body)))


(def read-response
  [:map
   [:number int?]
   [:description string?]
   [:title string?]
   [:video-id string?]
   [:recording-date string?]
   [:hosts string?]])

(def all-episodes-response
  [:map [:episodes [:vector read-response]]])

(def save-response [:map [:number int?]])

(def api-route
  ["/api"
   ["/episodes"
    ["" {:swagger {:tags ["episodes"]}
         :get     {:summary   "List all episodes"
                   :responses {200 {:body all-episodes-response}}
                   :handler   handle-all-episodes-request}}]
    ["/:number"
     {:swagger {:tags ["episodes"]}
      :get     {:summary    "Fetch data for the specific episode number"
                :parameters {:path [:map [:number int?]]}
                :responses  {200 {:body read-response}}
                :handler    handle-read-request}}]]])

(defn vimeo-embed [id]
  (h/html
   (if (nil? id)
     [:div "Video coming soon."]
     [:iframe
      {:src (str "https://player.vimeo.com/video/" id)
       :width 640
       :height 360
       :frameborder 0
       :allow "autoplay; fullscreen; picture-in-picture"
       :allowfullscreen true}])))

(defn handle-homepage [_req]
  {:status 200
   :body (page/html5
          {:lang "en"}
          [:head
           [:title "Apropos"]
           [:link {:rel "stylesheet"
                   :href "https://unpkg.com/sakura.css/css/sakura.css"
                   :type "text/css"}]
           ]
          [:body
           [:img {:src "/images/apropro.png" :alt "Apropro" :title "Apropro"}]
           [:h1 {:style "text-align: center"}
            [:a {:href "/"} "Apropos"]]
           [:h2 "Episodes"]
           (let [episodes (reverse (list-episodes))
                 recent (first episodes)
                 others (rest episodes)]
             [:div
              [:h3 [:a {:href (str "/episode/" (:number recent))}
                    (:title recent) " " (:recording-date recent)]
               [:div
                (vimeo-embed (:video-id recent))]]
              (for [episode others]
                [:h3 [:a {:href (str "/episode/" (:number episode))}
                      (:title episode) " " (:recording-date episode)]])])])
   :headers {}})

(defn handle-episode-page [req]
  (let [_preq (with-out-str (pprint req))
        episode-id (:episode-id (:path-params req))
        episode (episode-data {:number episode-id})]
    {:status 200
     :body (page/html5
            {:lang "en"}
            [:head
             [:title "Apropos"]
             [:link {:rel "stylesheet"
                   :href "https://unpkg.com/sakura.css/css/sakura.css"
                   :type "text/css"}]]
            [:body
             [:img {:src "/images/apropro.png" :alt "Apropro" :title "Apropro"}]
             [:h1 [:a {:href "/"} "Apropos"]]
             [:h2 (:title episode)]
             [:div (:recording-date episode)]
             [:div (:hosts episode)]
             [:div (:description episode)]
             [:div (vimeo-embed (:video-id episode))]])
     :headers {}}))

(def pages-route
  [["/" {:get {:summary "Homepage"
               :responses {200 {:body :string}}
               :handler handle-homepage}}]
   ["/episode/:episode-id" {:get {:summary "Single episode"
                                  :responses {200 {:body :string}}
                                  :handler handle-episode-page}}]
   ["/images/*" (ring/create-resource-handler {:path "/"})]])

(def router-config
  {:validate rs/validate
   :data     {:coercion   rcm/coercion
              :muuntaja   m/instance
              :middleware [swagger/swagger-feature
                           muuntaja/format-middleware
                           parameters/parameters-middleware
                           coercion/coerce-exceptions-middleware
                           coercion/coerce-request-middleware
                           coercion/coerce-response-middleware]}})

(def app
  (ring/ring-handler
    (ring/router [pages-route swagger-route api-route] router-config)
    (ring/routes (swagger-ui/create-swagger-ui-handler
                   {:path   "/swagger"
                    :config {:validatorUrl     nil
                             :operationsSorter "alpha"}})
                 (ring/create-default-handler))))

(defn start
  [port]
  (jetty/run-jetty (fn [req] (app req)) {:port port, :join? false}))

(defn safe-port
  [port]
  (try (when-let [num (cond (string? port) (Integer/parseInt port)
                            (int? port) port)]
         (when (and (pos-int? num) (> 65535 num)) num))
       (catch Exception _)))

(defn -main [& _args]
  (let [port (or (some-> (System/getenv "PORT") safe-port) 3000)]
    (start port)))

