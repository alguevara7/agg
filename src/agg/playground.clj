(ns counters-clj.core
  (require [clj-kafka.zk :refer :all]
           [clj-kafka.core :refer :all]
           [clj-kafka.producer :refer :all]
           [clj-kafka.consumer.simple :as simple]
           [kafka.producer :as producer]))

(def discovered-config (brokers {"zookeeper.connect" "192.168.59.103:2181"}))

(def discovered-broker-list (broker-list discovered-config))

(def p (producer {"metadata.broker.list" discovered-broker-list
  ;;                "serializer.class" "kafka.serializer.DefaultEncoder"
                    "partitioner.class" "kafka.producer.ByteArrayPartitioner"
                  }))



(send-message p (message "topic" (.getBytes "1") (.getBytes "this is my message")))

(def c1 (simple/consumer "192.168.59.103" 9091 "zaphod"))
(def c2 (simple/consumer "192.168.59.103" 9092 "zaphod"))

(simple/latest-topic-offset c2 "topic" 0)

(simple/latest-topic-offset c1 "topic" 1)

(simple/topic-meta-data c1 ["topic"])

(simple/messages c2 "zaphod" "topic" 0 0 999999)
;$KAFKA_HOME/bin/kafka-console-producer.sh --topic=topic --broker-list=192.168.59.103:9093,192.168.59.103:9092,192.168.59.103:9091
