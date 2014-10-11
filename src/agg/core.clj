(ns agg.core
  (:require [clojure.core.async :as async :refer [chan go <! >! >!! <!! go-loop alts! timeout onto-chan buffer]]))

(defn agg [in f init out n msecs]
  "
  'in' input channel, events will be read from it
  'f' function that aggregates the value of events into a result
  'init' initial state
  'out' output channel, aggregate results will be writen to it
  "
  (go (loop [{:keys [result delta i flushed-at] :as state} {:result init :i 0 :flushed-at 0}]
        ;;(println state)
        (let [t (timeout msecs)
              [{:keys [value offset] :as message} ch] (alts! [in t])]
          (if (nil? message)
            (if (= ch t)
              (if (> i flushed-at)
                (when (>! out state)
                  (recur (-> state (dissoc :delta) (assoc :flushed-at i))))
                (recur state))
              (when (> i flushed-at) (>! out state)))
            (let [[new-result new-delta] (f [result delta] value)
                  {new-i :i :as new-state} (-> state
                                               (assoc :result new-result)
                                               (assoc :delta new-delta)
                                               (assoc :offset offset)
                                               (update-in [:i] inc))]
              (if (zero? (rem new-i n))
                (when (>! out new-state)
                  (recur (-> new-state (dissoc :delta) (assoc :flushed-at new-i))))
                (recur new-state)))
            )))))

(defn sample-from-offset [f init-offset period ch]
  "
  'period' in milliseconds
  'f' function to sample. returns a map of the form {:value x :offset n}
  'ch' channel to write sampled value to
  "
  (go (loop [offset init-offset]
        (let [data (f offset)]
          (if (coll? data)
            (do (<! (onto-chan ch data false))
              (when-not (.closed? ch)
                (<! (async/timeout period))
                (recur (:offset (last data)))))
            (when (>! ch data)
              (<! (async/timeout period))
              (recur (inc offset))))))))

(defn sample [f period ch]
  (go (loop []
        (when (>! ch (f))
          (<! (async/timeout period))
          (recur)))))

(defn subscribe [ch f]
  (go (loop []
        (when-let [v (<! ch)]
          (f v)
          (recur)))))
