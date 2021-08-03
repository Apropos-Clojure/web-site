(ns server.web-site-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [clj-http.client :as client]
            [cheshire.core :as json]
            [server.test-fixtures :as fixtures]))

(use-fixtures :each fixtures/web-server)

(def base-url "http://localhost:3001/api/")

(defn as-json
  [body]
  (json/parse-string body true))

(deftest get-episode-data
  (testing "that episode data is fetched properly"
    (let [n 42
          {:keys [status body]} (client/get (str base-url "episodes/" n))
          {:keys [number]} (as-json body)]
      (is (= 200 status))
      (is (= n number)))))

(deftest save-episode-data
  (testing "that episode data is saved properly"
    (let [n   1
          url (str base-url "episodes")
          {:keys [status body]} (client/post url {:form-params  {:number 1}
                                                  :content-type :json})
          {:keys [number]} (as-json body)]
      (is (= 200 status))
      (is (= n number)))))