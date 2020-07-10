(defproject brightspace-adapter "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-http "3.10.1"]
                 [metosin/compojure-api "2.0.0-alpha31"]
                 [aero "1.1.6"]
                 [ring/ring-jetty-adapter "1.7.0"]]
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler brightspace-adapter.core/app
         :adapter {:ssl? true
                   :ssl-port 8443
                   :keystore "selfsigned.jks"
                   :key-password "changeit"}}
  :main ^:skip-aot brightspace-adapter.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
