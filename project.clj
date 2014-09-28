(defproject agg "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[ragtime/ragtime.lein "0.3.6"]]
  :ragtime {:migrations ragtime.sql.files/migrations
            :database "jdbc:mysql://192.168.59.103:3306/agg?user=agg&password=agg"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.apache.kafka/kafka_2.10 "0.8.1.1" :exclusions [[com.sun.jmx/jmxri] [com.sun.jdmk/jmxtools] [javax.jms/jms]]]
                 [clj-kafka "0.2.6-0.8" :exclusions [[org.apache.kafka/kafka_2.10] [org.apache.zookeeper/zookeeper]]]
                 [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
                 [ragtime/ragtime.sql.files "0.3.6"]
                 [org.clojure/java.jdbc "0.3.5"]
                 [mysql/mysql-connector-java "5.1.32"]
                 [clj-time "0.8.0"]])
