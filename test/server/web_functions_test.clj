(ns server.web-functions-test
  (:require [clojure.test :refer [deftest testing is]]
            [server.web-site :as web-site]))

(deftest fetch-data-test
  (testing "Fetch gives back the correct data"
    (let [n 1
          {:keys [number]} (web-site/episode-data {:number n})]
      (is (= n number)))))

