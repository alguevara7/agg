(ns agg.core
  (:require [clojure.core.async :as async :refer [chan go <! >! go-loop alts! timeout]]))

(defn stage-agg [in f init out]
  "
  'in' input channel, events will be read from it
  'f' function that aggregates the value of events into a result
  'init' initial state
  'out' output channel, aggregate results will be writen to it
  "
  (go (loop [state init
             {:keys [boot value offset] :as message} {:boot true}]
        (when message
          (if (true? boot)
            (recur init (<! in))
            (let [new-state {:result (f (:result state) value) :offset offset}]
              (>! out new-state)
              (recur new-state (<! in))))))))


(defn stage-flush [in out n msecs]
  (go (loop [state {:i 0}]
        (println state)
        (let [t (timeout msecs)
              [message ch] (alts! [in t])]
          (if-not (nil? message)
            (let [{:keys [i] :as new-state} (-> state (assoc :message message) (update-in [:i] inc))]
              (if (and (> i 0) (zero? (rem i n)))
                (do (>! out message) (recur (assoc new-state :flushed-at i)))
                (recur new-state)))
            (let [{:keys [i message]} state]
              (if (= ch t)
                (if (> i (get state :flushed-at 0))
                  (do (>! out message) (recur (assoc state :flushed-at i)))
                  (recur state))
                (when (> i (get state :flushed-at 0)) (>! out message)))))))))

(defn agg [in f init out n msecs]
  (let [in-out (chan (async/sliding-buffer 1))]
    (stage-agg in f init in-out)
    (stage-flush in-out out n msecs)))

(defn sample-from-offset [f init-offset period ch]
  "
  'period' in milliseconds
  'f' function to sample. returns a map of the form {:value x :offset n}
  'ch' channel to write sampled value to
  "
  (go (loop [offset init-offset]
        (when (>! ch (f offset))
          (<! (async/timeout period))
          (recur (inc offset))))))

(defn sample [f period ch]
  (go (loop []
        (when (>! ch (f))
          (<! (async/timeout period))
          (recur)))))

(defn subscribe [f ch]
  (go (loop []
        (when-let [v (<! ch)]
          (f v)
          (recur)))))


