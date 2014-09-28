(ns agg.core
  (:require [clojure.core.async :as async :refer [chan go <! >! >!! <!! alt!! alts!!]]))

;; TODO: graphite!

;; retrieve events in batches

;; figure out creation of channels ... agg should handle it ...

(defn do-agg [ch af init-state ff]
  "
  'ch' the channel events will be read from
  'af' function that aggregates the value of event into result
  'ff' function that writes result and event offset to db
  "
  (go (loop [state init-state
             {:keys [action value offset] :as event} {:action :boot}]
        (when event ;;exit when channel is closed
          (case action
            :boot (recur init-state (<! ch))
            :process (recur {:result (af (:result state) value) :offset offset}
                            (<! ch))
            :flush (do (ff state)
                     (recur state (<! ch)))
            (recur state (<! ch)))))))

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

;; ef: [long] -> seq[[offset message]]
(defn agg [sf
           ef ef-period
           af
           ff ff-period
           n]
  (let [{:keys [offset] :as init-state} (sf)
        ch (chan n)]
    (do-agg ch af init-state ff)
    (sample-iterate ef offset ef-period ch)
    (sample (fn [] {:action :flush}) ff-period ch)
    ch))
