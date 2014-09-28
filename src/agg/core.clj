(ns agg.core
  (:require [clojure.core.async :as async :refer [chan go <! >! >!! <!! alt!! alts!!]]))

;; TODO: graphite!

;; {:action :boot, {...}}

;; {:action :process, {:offset 1 :value 1}}

;; {:action :flush, {...}}


(defn agg-chan [af init-state ff]
  "
  'af' function that aggregates the value of event into result
  'ff' function that writes result and event offset to db

  returns a channel this aggregator will process messages from
  "
  (let [c (chan)]
    (go
     (loop [state init-state
            {:keys [action value offset] :as event} {:action :boot}]
       (when event ;;exit when channel is closed
         (case action
           :boot (recur init-state (<! c))
           :process (recur {:result (af (:result state) value) :offset offset}
                           (<! c))
           :flush (do (ff state)
                      (recur state (<! c)))
           (recur state (<! c))))))
    c))

(defn sample-iterate [f init-offset period c]
  "'period' in milliseconds
  'f' function to sample
  'c' channel to write sampled value to

  TODO: stop sampling when output channel is closed
  "
  (go (loop [offset init-offset]
        (when (>! c (f offset))
          (<! (async/timeout period))
          (recur (inc offset))))))

(defn sample [f period c]
  (go (loop []
        (when (>! c (f))
          (<! (async/timeout period))
          (recur)))))

;; ef: [long] -> seq[[offset message]]
(defn agg [sf
           ef ef-period
           af
           ff ff-period]
  (let [{:keys [offset] :as init-state} (sf)
        c (agg-chan af init-state ff)]
    (sample-iterate ef offset ef-period c)
    (sample (fn [] {:action :flush}) ff-period c)
    c))
