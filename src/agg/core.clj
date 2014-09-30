(ns agg.core
  (:require [clojure.core.async :as async :refer [chan go <! >! sliding-buffer]]))

;; retrieve events in batches

;; figure out creation of channels ... agg should handle it ...

;; stop when outout channel is closed

;; handle exceptions

(defn do-agg [chi f init cho n]
  "
  'chi' input channel, events will be read from it
  'f' function that aggregates the value of events into a result
  'cho' output channel, aggregate results will be writen to it
  "
  (go (loop [state init
             {:keys [action value offset] :as event} {:action :boot}
             i 0]
        (when event ;;exit when channel is closed
          (case action
            :boot (recur state (<! chi) i)
            :process (let [new-state {:result (f (:result state) value) :offset offset}
                           new-i (inc i)]
                       (if (>= new-i n)
                         (recur new-state {:action :flush} new-i)
                         (recur new-state (<! chi) new-i)))
            :flush (do (>! cho state) ;; TODO flush changed entries only ! need a set to store keys, flush when count >= n.
                       (recur state (<! chi) 0))
            (recur state (<! chi) i))))))

(defn sample-iterate [f init-offset period ch]
  "'period' in milliseconds
  'f' function to sample
  'ch' channel to write sampled value to

  TODO: stop sampling when output channel is closed
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

(defn subscribe [f period ch]
  (go (loop []
        (when-let [v (<! ch)]
          (f v)
          (<! (async/timeout period))
          (recur)))))

(defn agg [sf
           ef ef-period size-ch-input
           af
           ff ff-period size-ch-output
           n]
  (let [{:keys [offset] :as init} (sf)
        chi (chan size-ch-input)
        cho (chan size-ch-output)]
    (do-agg chi af init cho n)
    (sample-iterate ef offset ef-period chi)
    (sample (fn [] {:action :flush}) ff-period chi)
    (subscribe ff ff-period cho)
    chi))
