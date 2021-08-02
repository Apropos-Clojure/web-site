(ns server.web-site-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [clj-http.client :as client]
            [server.test-fixtures :as fixtures]))

(use-fixtures :each fixtures/web-server)

(def base-url "http://localhost:3000/api/")

(deftest get-episode-data
  (testing "that episode data is fetched properly"
    (let [n 1
          {:keys [status body]} (client/get (str base-url "episodes/" n))
          {:keys [number]} (cheshire.core/parse-string body true)]
      (is (= 200 status))
      (is (= n number)))))

(deftest save-episode-data
  (testing "that episode data is saved properly"
    (let [n 1
          {:keys [status body]} (client/post (str base-url "episodes")
                                             {:form-params {:number 1}
                                              :content-type :json})
          {:keys [number]} (cheshire.core/parse-string body true)]
      (is (= 200 status))
      (is (= n number)))))