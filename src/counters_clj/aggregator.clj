(ns counters-clj.aggregator
  (:require [clojure.core.async :as async :refer [chan go <! >! >!! <!! alt!! alts!!]]))

;; TODO: graphite!

; coordinator

;; aggregator


;; {:action :boot, {...}}

;; {:action :process, {:offset 1 :value 1}}

;; {:action :flush, {...}}


(defn aggregator [af init-state ff]
  "
  'af' function that aggregates the value of event into result
  'ff' function that writes result and event offset to db

  returns a channel this aggregator will process messages from
  "
  (let [c (chan)]
    (go
     (loop [state init-state
            {:keys [action value offset] :as event} {:action :boot}]
       (when event
         (case action
           :boot (recur init-state (<! c))
           :process (recur {:result (af (:result state) value) :offset offset}
                           (<! c))
           :flush (do (ff state)
                      (recur state (<! c)))
           (recur state (<! c))))))
    c))

(defn fetch [f init-offset period c]
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

(defn start-aggregator [sf
                        ef ef-period
                        af
                        ff ff-period]
  (let [{:keys [offset] :as init-state} (sf)
        c (aggregator af init-state ff)]
    (fetch ef offset ef-period c)
    (sample (fn [] {:action :flush}) ff-period c)
    c))


(defn lala [i] (start-aggregator (fn [] {:result 0 :offset 0})
                         (fn [offset] {:action :process :value 3 :offset (inc offset)}) 10
                         (fn [r v] (+ r v))
                         (fn [{:keys [result offset] :as state}]
                           (println (str "FLUSH: -> " "result:" result " offset: " offset))) 5000))


(def cs (map #(lala %) (range 50)))

(doall cs)

(doseq [c cs] (async/close! c))
