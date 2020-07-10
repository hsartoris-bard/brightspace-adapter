(ns brightspace-adapter.core
  (:require [compojure.api.sweet :refer :all]
            [aero.core :as aero]
            [clojure.pprint :refer [pprint]]
            [ring.util.http-response :refer [ok]])
  (:gen-class))

(def debug true)

; read configuration from resources/config.edn
; by using clojure.java.io/resource we also can retrieve from classpath
(def config (-> (clojure.java.io/resource "config.edn")
                (aero/read-config {:profile :dev})))

(when debug (pprint config))

(def app
  (api
    (context 
      (:base-url config) []
      (GET "/" []
           (ok "Yup")))))


;(def app
;  (api
;    (context 
;      "/brightspace" []



;(defn -main
;  "I don't do a whole lot ... yet."
;  [& args]
;  (println "Hello, World!"))
