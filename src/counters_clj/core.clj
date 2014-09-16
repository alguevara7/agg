(ns counters-clj.core
  (require [clj-kafka.zk :refer :all]
           [clj-kafka.core :refer :all]
           [clj-kafka.producer :refer :all]
           [clj-kafka.consumer.zk :refer :all]))

(def discovered-config (brokers {"zookeeper.connect" "192.168.59.104:2181"}))

discovered-config

(def discovered-broker-list (broker-list discovered-config))

discovered-broker-list

(def p (producer {"metadata.broker.list" discovered-broker-list
                  "serializer.class" "kafka.serializer.DefaultEncoder"
                  "partitioner.class" "kafka.producer.DefaultPartitioner"}))

(for [i (range 20000)]
  (send-message p (message "topic" (.getBytes "this is my message"))))


(def config {"zookeeper.connect" "192.168.59.104:2181"
             "group.id" "clj-kafka.consumer"
             "auto.offset.reset" "smallest"
             "auto.commit.enable" "false"})


(topics config)


(with-resource [c (consumer config)]
  shutdown
  (take 100 (messages c "topic")))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))


;$KAFKA_HOME/bin/kafka-console-producer.sh --topic=topic --broker-list=192.168.59.103:9093,192.168.59.103:9092,192.168.59.103:9091
