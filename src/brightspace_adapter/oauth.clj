(ns brightspace-adapter.oauth
  (:require [clojure.data.json :as json]
            [ring.util.codec :refer [url-encode]]
            [clj-http.client :as http])
  (:import java.util.UUID)
  (:gen-class))


;; helpers
(defn gen-csrf [] (str (java.util.UUID/randomUUID)))

(defn curr-time-secs []
  (quot (System/currentTimeMillis) 1000))

(defn post->json
  "Simple wrapper for common operations"
  [uri params]
  (-> (http/post uri params)
      (get :body)
      (json/read-str :key-fn keyword)))

;; the actual meat of it

(defprotocol OAuth20
  (update-state 
    [this token-response]
    "Internal convenience function for dealing with token responses.")
  (gen-auth-uri 
    [this] 
    "Completes an OAuth2.0 Authorization URI with requisite query params.")
  (complete-auth
    [this code csrf]
    "Given response from remote server, validates CSRF token and completes
    authentication.")
  (auth-completed?
    [this]
    "Checks to see if authentication is finished.")
  (refresh
    [this]
    "Makes a request to the issuing server to refresh the access and refresh
    tokens, consuming the existing refresh token in the process.")
  (access-token
    [this]
    "Retrieves access token, calling refresh if necessary."))

(defrecord OAuthHandler [authorize-uri token-uri redirect-uri client-id client-secret scope state]
  OAuth20
  (update-state [this {expires-in :expires_in :as token-response}]
    (-> token-response
        (dissoc :expires_in)
        (assoc :expires-at (+ expires-in (curr-time-secs)))
        (->> (swap! state merge))))

  (gen-auth-uri [this]
    (let [csrf (gen-csrf)]
      (printf "Generated new CSRF token: %s\n" csrf)
      (swap! state assoc :csrf csrf)
      (format "%s?response_type=code&redirect_uri=%s&client_id=%s&scope=%s&state=%s"
              authorize-uri 
              (url-encode redirect-uri) 
              (url-encode client-id) 
              (url-encode scope)
              (url-encode csrf))))

  (complete-auth [this code csrf]
    (printf "Received auth code: %s\n" code)
    (printf "Received CSRF token %s\n" csrf)
    (if (not= csrf (:csrf @state))
      (throw (Exception. "CSRF tokens don't match!"))
      (let [params {:form-params {:code code
                                  :grant_type "authorization_code"
                                  :redirect_uri redirect-uri}
                    :basic-auth [client-id client-secret]}]
        (->> (post->json token-uri params)
             (update-state this)))))

  (refresh [this]
    (println "Refreshing tokens...")
    (let [params {:form-params {:grant_type "refresh_token"
                                :refresh_token (:refresh_token @state)
                                :scope scope}
                  :basic-auth [client-id client-secret]}]
      (->> (post->json token-uri params)
           (update-state this)
           (get :access_token))))

  (access-token [this]
    (cond
      (not (auth-completed? this)) (throw (Exception. "Authentication not completed yet!"))
      (< (+ 30 (curr-time-secs))
         (:expires-at @state)) (:access_token @state)
      :else (refresh this)))

  (auth-completed? [this]
    (some? (get @state :expires-at))))

(defn handler [oauth-config]
  (-> oauth-config
      (assoc :state (atom {}))
      map->OAuthHandler))

