(defproject filtertest "1.0"
  
  :dependencies [[org.clojure/clojure "1.9.0"]

                 [embedded-kafka "0.6.0"]
                 ;;[io.weft/gregor "0.6.0"]
                 [org.apache.kafka/kafka_2.11 "2.0.0"]
                 [com.fasterxml.jackson.core/jackson-core "2.8.3"]
                 ;;[org.apache.kafka/kafka_2.11 "1.1.1"]
                 [org.slf4j/slf4j-simple "1.8.0-alpha2"]


                 [cheshire "4.0.3"]
                 [compojure "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [environ "1.0.0"]
                 ]
  :min-lein-version "2.0.0"
  :plugins [[environ/environ.lein "0.3.1"]]
  :hooks [environ.leiningen.hooks]
  :uberjar-name "filtertest-standalone.jar"
  :profiles {:production {:env {:production true}}})
