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
(def profile :prod)

; read configuration from resources/config.edn
; by using clojure.java.io/resource we also can retrieve from classpath
(def config (-> (clojure.java.io/resource "config.edn")
                (aero/read-config {:profile profile})))

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

(defn create-user [user]
  (when debug (pprint user))
  (let [params {:body (json/write-str user)
                :oauth-token (oauth/access-token auth-handler)
                :content-type :json}
        url (get-in config [:api :users])]
        ;url "http://cas02:5001/"]
    (-> (http/post url params)
        :body
        (json/read-str :key-fn keyword))))


(def app
  (let [{prefix :prefix} config]
    (api
      (GET "/" [] (found (str prefix "/setup")))
      (context 
        prefix []
        (GET "/callback" []
             :query-params [code state]
             (let [res (oauth/complete-auth auth-handler code state)]
               (pprint res)
               (found (str prefix "/setup"))))

        (GET "/setup" []
             (if (not (oauth/auth-completed? auth-handler))
               (found (oauth/gen-auth-uri auth-handler))
               (ok (get-user "901377171@bard.edu"))))

        (POST "/user" []
              :body-params [eduPersonPrincipalName
                            employeeType
                            givenName
                            mail
                            sn
                            uid]
              ; https://docs.valence.desire2learn.com/res/user.html#User.CreateUserData
              (if-let [user (get-user eduPersonPrincipalName)]
                (ok user)
                (-> {:OrgDefinedId eduPersonPrincipalName
                     :FirstName givenName
                     :MiddleName nil
                     :LastName sn
                     :ExternalEmail mail
                     :UserName uid
                     :RoleId (case employeeType
                               "student" 110 ; 'learner' role
                               "faculty" 109 ; 'instructor' role
                               "staff"   114 ; 'facilitator' role
                               111) ; default read-only)
                     :IsActive true
                     :SendCreationEmail false}
                    create-user
                    ok)))))))

        ;(GET "/refresh" []
        ;     (let [tok (oauth/refresh auth-handler)]
        ;       (println tok)
        ;       (ok tok)))

        ;(GET "/:id" []
        ;     :path-params [id]
        ;     (ok (get-user id)))))))

