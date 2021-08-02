(ns dev
  (:require
    [clojure.tools.namespace.repl :as repl]
    [server.web-site :as server])
  (:import (org.eclipse.jetty.server Server)))

(repl/set-refresh-dirs "dev" "src")

(defonce server (atom nil))

(defn go []
  (reset! server (server/start 3000)))

(defn reset []
  (do (when-let [server ^Server (deref server)]
        (.stop server))
      (repl/refresh :after 'dev/go)))

