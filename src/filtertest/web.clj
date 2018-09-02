(ns filtertest.web
  (:gen-class)
  (:import
    [org.apache.commons.io FileUtils])
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.util.response :as resp]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :refer [redirect]]
            [environ.core :refer [env]]
            [clojure.java.io :refer [file output-stream input-stream resource]]
            [cheshire.core :refer :all]
            [ring.util.request :refer [body-string]]
            [cheshire.core :refer :all]
            [embedded-kafka.core :refer :all]
            [gregor.core :as gregor]
            [clojure.java.io :refer [file]]
            [gregor.core :as gregor]
            [clojure.string :refer [lower-case]]))

(defn notfound
  []
  {:status  404
   :headers {"Content-Type" "text/plain"}
   :body    "Hello 404"})

(def filters (atom [{:id 1 :messages [] :topic "books" :q "cisp"}
                    {:id 2 :messages [] :topic "time" :q "waste"}]))

(defn jsonresponse [body]
  {:headers {"Content-Type" "application/json"} :body (generate-string body)})

(def topics ["books" "time" "topic3" "topic4"])

(def kafka
  (delay
    (binding [kafka-config {"broker.id"                        "0"
                            "listeners"                        "PLAINTEXT://localhost:9999"
                            "bootstrap.servers"                "localhost:9999"
                            "zookeeper.connect"                "127.0.0.1:2182"
                            "zookeeper-port"                   "2182"
                            "log.flush.interval.messages"      "1"
                            "auto.create.topics.enable"        "false"
                            "group.id"                         "consumer"
                            "auto.offset.reset"                "earliest"
                            "retry.backoff.ms"                 "500"
                            "message.send.max.retries"         "5"
                            "auto.commit.enable"               "false"
                            "max.poll.records"                 "50"
                            "offsets.topic.replication.factor" "1"
                            "log.dir"                          (.getAbsolutePath (file (tmp-dir "kafka-log")))}]
      (FileUtils/deleteDirectory (file (tmp-dir)))
      (let [zk (create-zookeeper)
            kafka (create-broker)]
        (.startup kafka)
        (let [producer (gregor/producer (get kafka-config "bootstrap.servers") kafka-config)
              consumer (gregor/consumer (get kafka-config "bootstrap.servers")
                                        (get kafka-config "group.id")
                                        []
                                        kafka-config)]
          (doseq [t topics]
            (gregor/create-topic {:connection-string "127.0.0.1:2182"} t {}))
          (gregor/subscribe consumer topics)
          {:zk zk :kafka kafka :producer producer :consumer consumer})))))

(defn msgloop []
  (future
    (while (> (count @filters) 0)
      (println "-------")
      (doseq [t topics q (map #(:q %) @filters)]
        (println (str "send " t " " q))
        (.get (gregor/send (:producer @kafka) t q))
        )
      (doseq [r (gregor/poll (:consumer @kafka) 1000)]
        (println (str "receive " (:topic r) " " (:value r)))
        (swap! filters (fn [old] (map (fn [f]
                                        (update-in f [:messages] #(if (and (= (:topic r) (:topic f)) (.contains (lower-case (:value r)) (lower-case (:q f)))) (conj % (:value r)) %))) old))))
      (Thread/sleep 10000)
      )))

(defroutes app-routes
           (POST "/filter" request (do
                                     (swap! filters (fn [old] (conj old (conj {:id (inc (apply max (map :id old))) :messages []} (parse-string (body-string request) true)))))
                                     (jsonresponse @filters)))
           (GET "/filter" request (let [id (:id (:params request)) filtrs @filters]
                                    (jsonresponse (if (nil? id) filtrs (filter #(= (read-string id) (:id %)) filtrs)))))
           (DELETE "/delete" request (do
                                       (swap! filters (fn [old] (remove #(= (read-string (:id (:params request))) (:id %)) old)))
                                       (jsonresponse @filters)))
           (route/resources "/")
           (route/not-found (notfound)))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (handler/site #'app-routes) {:port port :join? false}))
  (msgloop))

;;(.get (gregor/send (:producer @kafka) "books" "cisp test"))
