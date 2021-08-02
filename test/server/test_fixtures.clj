(ns server.test-fixtures
  (:require [server.web-site :as server])
  (:import (org.eclipse.jetty.server Server)))

(defn web-server [f]
  (let [server ^Server (server/start 3000)]
    (f)
    (.stop server)))