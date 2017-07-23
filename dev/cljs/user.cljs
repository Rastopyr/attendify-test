(ns cljs.user
  (:require [attendify-test-app.core]
            [attendify-test-app.system :as system]))

(def go system/go)
(def reset system/reset)
(def stop system/stop)
(def start system/start)
