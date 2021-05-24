(ns user
  (:require [shadow.cljs.devtools.api :as shadow]))


(defn watch []
  (shadow/watch :app))

(defn stop []
  (shadow/stop-worker :app))

(defn repl []
  (shadow/repl :app))

(defn browser-repl []
  (shadow/browser-repl :app))

(defn go []
  (shadow/watch :app)
  (shadow/browser-repl))

;; (defn release [server]
;;   (shadow/release :my-build)
;;   (sh "rsync" "-arzt" "path/to/output-dir" server))
