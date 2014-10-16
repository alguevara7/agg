(ns agg.core-test
  (:require [clojure.test :refer :all]
            [agg.core :refer :all]
            [clojure.core.async :refer [chan go <!! >!! alts!! timeout close! onto-chan]]))


(deftest put-state-in-out-channel

  (testing "after processing n messages"
    (let [in (chan)
          out (chan)
          x 10]
      (agg in (fn [[r d] v] [(+ r v) (+ r v)]) 0 out x 1000)
      (onto-chan in (map #(hash-map :value % :offset %) (range x)) false)
      (let [[val _] (alts!! [out (timeout 1000)])]
        (is (= (apply + (range x))
               (:delta val)))
        (is (= (apply + (range x))
               (:result val)))
        (is (= (- x 1)
               (:offset val))))
      (close! in)))

  (testing "after timed out waiting for a message"
    (let [in (chan)
          out (chan)
          messages [{:value 3 :offset 0} {:value 0.14159 :offset 1}]]
      (agg in (fn [[r d] v] [(+ r v) (+ r v)]) 0 out 9999 100)
      (onto-chan in messages false)
      (let [[val _] (alts!! [out (timeout 1000)])]
        (is (= (apply + (map :value messages))
               (:delta val)))
        (is (= (apply + (map :value messages))
               (:result val)))
        (is (= (- (count messages) 1)
               (:offset val))))
      (close! in)))

  (testing "when in channel is closed"
    (let [in (chan)
          out (chan)
          messages [{:value 3 :offset 0} {:value 0.14159 :offset 1}]]
      (agg in (fn [[r d] v] [(+ r v) (+ r v)]) 0 out 9999 100000)
      (<!! (onto-chan in messages))
      (close! in)
      (let [[val _] (alts!! [out (timeout 1000)])]
        (is (= (apply + (map :value messages))
               (:delta val)))
        (is (= (apply + (map :value messages))
               (:result val)))
        (is (= (- (count messages) 1)
               (:offset val)))))))

(run-tests)
