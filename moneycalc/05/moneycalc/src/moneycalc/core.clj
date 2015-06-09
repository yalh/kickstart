(ns moneycalc.core
  (:require [org.httpkit.server :as server]
            [org.httpkit.client :as client]
            [clojure.string :as string]
            [hiccup.core :as h]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))


;; Access currency exchange rate with Yahoo Finance web API

(def url "http://download.finance.yahoo.com/d/quotes.csv?s=%s%s=X&f=pc1")

(defn query-money-exchange-stats
  "Returns a map with keys `rate` and `change` whose values
  represent the conversion rate and change between two currencies
  as floating point numbers (e.g. {:rate 1.1283, :change -0.002}).
  Typical symbols are EUR, USD, CAD, NZD, JPY, AUD, NOK.
  Returns nil if no rate can be found."
  [to-symbol from-symbol]
  (let [url       (format url to-symbol from-symbol)
        response @(client/get url)
        body      (slurp (:body response))
        [rate change] (map read-string (string/split body #","))]
    (if (number? rate)
      {:rate rate :change change})))

;; uncomment to test it
#_(query-money-exchange-stats "EUR" "USD")


(defn render-rate
  [stats to-symbol from-symbol]
  [:tr
   [:td from-symbol]
   [:td to-symbol]
   [:td (:rate stats)]
   [:td (:change stats)]])


(defn handler
  [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (h/html [:body
                  [:h1 "Hello Hiccup World"]
                  [:table
                   [:thead [:tr [:th "From"] [:th "To"] [:th "Rate"] [:th "Change"]]]
                   [:body (for [t ["EUR" "USD"] f ["JPY" "AUD"]]
                            (if-let [stats (query-money-exchange-stats t f)]
                              (render-rate stats t f)))]]])})



(def app (wrap-defaults #'handler site-defaults))


;; -------------------------------------------------------------------
;; http server start/stop infrastructure

(defonce http-server (atom nil))

(defn stop!
  "Stops the http server if started."
  []
  (when-let [shutdown-fn @http-server]
    (shutdown-fn)
    (reset! http-server nil)
    :stopped))


(defn start!
  "Starts http server, which is reachable on http://localhost:8080"
  []
  (stop!)
  (reset! http-server (server/run-server #'app {:port 8080}))
  :started)


(stop!)
(start!)
