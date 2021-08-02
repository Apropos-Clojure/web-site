(ns server.web-site
  (:require [muuntaja.core :as m]
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

(defn save-episode-data
  ;; TODO: Integrate with the DB
  [{:keys [number]}]
  {:number number})

(defn episode-data
  ;; TODO: Integrate with the DB
  [{:keys [number]}]
  {:number number})

(def routes
  [["/swagger.json"
    {:get {:no-doc  true
           :swagger {:info {:title       "Apropos episodes API"
                            :description "with [malli](https://github.com/metosin/malli) and reitit-ring"}
                     :tags [{:name "episodes", :description "episodes api"}]}
           :handler (swagger/create-swagger-handler)}}]
   ["/api"
    ["/episodes"
     ["" {:swagger {:tags ["episodes"]}
          :post    {:summary    "Persist data for the episode"
                    :parameters {:body [:map [:number int?]]}
                    :responses  {200 {:body [:map [:number int?]]}}
                    :handler    (fn [request]
                                  (some->> (get-in request [:parameters :body])
                                           (save-episode-data)
                                           (assoc {:status 200} :body)))}}]
     ["/:number"
      {:swagger {:tags ["episodes"]}
       :get     {:summary    "Fetch data for the specific episode number"
                 :parameters {:path [:map [:number int?]]}
                 :responses  {200 {:body [:map [:number int?]]}}
                 :handler    (fn [request]
                               (some->> (get-in request [:parameters :path])
                                        (episode-data)
                                        (assoc {:status 200} :body)))}}]]]])

(def app
  (ring/ring-handler
    (ring/router
      routes
      {:validate rs/validate
       :data     {:coercion   rcm/coercion
                  :muuntaja   m/instance
                  :middleware [swagger/swagger-feature
                               muuntaja/format-middleware
                               parameters/parameters-middleware
                               coercion/coerce-exceptions-middleware
                               coercion/coerce-request-middleware
                               coercion/coerce-response-middleware]}})
    (ring/routes
      (swagger-ui/create-swagger-ui-handler
        {:path   "/"
         :config {:validatorUrl     nil
                  :operationsSorter "alpha"}})
      (ring/create-default-handler))))

(defn start
  [port]
  (jetty/run-jetty #'app {:port port, :join? false}))

(defn safe-port
  [port-string]
  (try (let [num (Integer/parseInt port-string)]
         (when (pos-int? num) num))
       (catch Exception _)))

(defn -main [& _args]
  (let [port (or (some-> (System/getenv "PORT") safe-port) 3000)]
    (start port)))
