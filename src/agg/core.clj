(ns agg.core
  (:require [clojure.core.async :as async :refer [chan go <! >! go-loop alts! timeout]]))

(defn do-agg [chi f init cho n]
  "
  'chi' input channel, events will be read from it
  'f' function that aggregates the value of events into a result
  'cho' output channel, aggregate results will be writen to it
  "
  (go (loop [state init
             {:keys [action value offset] :as message} {:action :boot}
             i 0]
        (when message  ;; TODO recur from then end, build event before hand
          (case action
            :boot (recur state (<! chi) i)
            :process (let [new-state {:result (f (:result state) value) :offset offset}
                           new-i (inc i)]
                       (if (>= new-i n)
                         (recur new-state {:action :flush} i)
                         (recur new-state (<! chi) new-i)))
            :flush (do (>! cho state)
                       (recur state (<! chi) 0))
            (recur state (<! chi) i))))))

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

(defn when-fn [x f g]
  (if (f x) (g x) x))

(defn stage-flush [in out n msecs]
  (go (loop [state {:i 0}]
        (println (str "state = " state))
        (let [t (timeout msecs)
              [v ch] (alts! [in t])]
          (if-not (nil? v)
            (recur (-> state
                       (assoc :message v)
                       (update-in [:i] inc)
                       (when-fn (fn [{:keys [i]}] (and (> i 0) (zero? (rem i n))))
                                (fn [{:keys [i message] :as s}]
                                  (async/put! out message)
                                  (assoc s :flushed-at i)))))
            (let [{:keys [i message]} state]
              (when (> i (get state :flushed-at 0))
                (>! out (:message state)))
              (when (= ch t)
                (recur (assoc state :flushed-at i)))))))))

;;        (when
;;         (>! out state))

;; (def in1 (chan 100))
;; (def out1 (chan 100))
;; (stage-agg in1 + {:result 0} out1)

;; (async/>!! in1 {:value 1110 :offset 12})

;; (def out2 (chan 100))

;; (stage-flush out1 out2 5 10000)

;; (go (loop [v nil]
;;       (println (str "flush!"))
;;       (recur (<! out2))))



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

;; (def c1 (chan))
;; (def out (chan 1))

;; (let [a (->> c1
;;              (async/reduce + 0)
;;              (vector)
;;              (async/map #({:v % :k "awesome"}))
;;              )]
;;   (async/pipe a out false))

;; (async/>!! c1 14)
;; (close! c1)

;; (async/<!! out)




