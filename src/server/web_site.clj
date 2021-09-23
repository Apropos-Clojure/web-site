(ns server.web-site
  (:require
    [clojure.java.io :as io]
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
    [ring.adapter.jetty :as jetty]))

(set! *warn-on-reflection* true)

(defn read-edn-file
  [file-name]
  (try
    (some-> file-name io/resource io/as-file slurp read-string)
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
  [request]
  (some->> (get-in request [:parameters :body])
           save-episode-data
           (assoc {:status 200} :body)))

(defn handle-read-request
  [request]
  (let [result (some->> (get-in request [:parameters :path])
                        episode-data
                        (assoc {:status 200} :body))]
    (println result)
    result))

(def read-response [:map
                    [:number int?]
                    [:title string?]
                    [:hosts string?]])

(def save-response [:map [:number int?]])

(def api-route
  ["/api"
   ["/episodes"
    ["" {:swagger {:tags ["episodes"]}
         :post    {:summary    "Save data for the episode"
                   :parameters {:body [:map [:number int?]]}
                   :responses  {200 {:body save-response}}
                   :handler    handle-save-request}
         :get     {:summary    "List all episodes"
                   :responses  {200 {:body save-response}}
                   :handler    handle-save-request}}]
    ["/:number"
     {:swagger {:tags ["episodes"]}
      :get     {:summary    "Fetch data for the specific episode number"
                :parameters {:path [:map [:number int?]]}
                :responses  {200 {:body read-response}}
                :handler    handle-read-request}}]]])

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
    (ring/router [swagger-route api-route] router-config)
    (ring/routes (swagger-ui/create-swagger-ui-handler
                   {:path   "/"
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

