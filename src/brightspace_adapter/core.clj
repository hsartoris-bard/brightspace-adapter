(ns brightspace-adapter.core
  (:require [compojure.api.sweet :refer :all]
            [brightspace-adapter.oauth :as oauth]
            [aero.core :as aero]
            [clojure.pprint :refer [pprint]]
            [clojure.data.json :as json]
            [clj-http.client :as http]
            [ring.util.http-response :refer [ok found]])
  (:import java.util.UUID)
  (:gen-class))

(def debug true)


; read configuration from resources/config.edn
; by using clojure.java.io/resource we also can retrieve from classpath
(def config (-> (clojure.java.io/resource "config.edn")
                (aero/read-config {:profile :dev})))

(when debug (pprint config))

(def auth-handler (oauth/handler (:oauth config)))

(defn get-user [id]
  (printf "Attempting to retrieve user %s\n" id)
  (let [params {:query-params {:orgDefinedId id}
                :oauth-token (oauth/access-token auth-handler)
                :unexceptional-status #(or (<= 200 % 299)
                                           (= % 404))}
        url (get-in config [:api :users])
        {body :body status :status} (http/get url params)]
    (if (= status 404)
      (do (printf "User %s not found\n" id)
          nil)
      (-> body
          (json/read-str :key-fn keyword)
          first))))

(def app
  (api
    (GET "/" [] (found (:prefix config)))
    (context 
      (:prefix config) []
      (GET "/callback" []
           :query-params [code state]
           (let [res (oauth/complete-auth auth-handler code state)]
             (pprint res)
             (found "/")))
      (POST "/user" []
            :body-params [eduPersonPrincipalName
                          givenName
                          mail
                          sn
                          cn]
            (if-let [user (get-user eduPersonPrincipalName)]
              (ok user)
              "User doesn't exist yet!"))

      (GET "/:id" []
           :path-params [id]
           (ok (get-user id)))

      (GET "/" []
           (if (not (oauth/auth-completed? auth-handler))
             (found (oauth/gen-auth-uri auth-handler))
             (ok (get-user "901377171@bard.edu")))))))
