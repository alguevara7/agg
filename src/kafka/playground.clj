(ns counters-clj.core
  (require [clojure.string :as s]
           [clj-kafka.zk :refer :all]
           [clj-kafka.core :refer :all]
           [clj-kafka.producer :refer :all]
           [clj-kafka.consumer.simple :as simple]
           [kafka.producer :as producer])
  (import [kafka.admin TopicCommand]))

(TopicCommand/main (into-array String (s/split "--zookeeper 192.168.59.103:2181 --create --topic ad_view_event --partitions 20 --replication-factor 1" #"\s")))

(def discovered-config (brokers {"zookeeper.connect" "192.168.59.103:2181"}))

(def discovered-broker-list (broker-list discovered-config))

(def p (producer {"metadata.broker.list" discovered-broker-list
   ;;                "serializer.class" "kafka.serializer.DefaultEncoder"
                  "partitioner.class" "kafka.producer.ByteArrayPartitioner"
                  }))

(send-message p (message "ad_view_event" (.getBytes "0") (.getBytes "this is my message")))

(def c (simple/consumer "192.168.59.103" 9091 "zaphod"))

(simple/latest-topic-offset c "ad_view_event" 19)

(simple/topic-meta-data c ["ad_view_event"])

(simple/messages c "zaphod" "ad_view_event" 0 0 1024)
