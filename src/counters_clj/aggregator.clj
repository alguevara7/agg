(ns counters-clj.aggregator
  (:require [clojure.core.async :as async :refer [chan go <! >! >!!]]))

; message source

; message offset


; coordinator

;; aggregator


;; {:action :boot, {...}}

;; {:action :process, {:offset 1 :value 1}}

;; {:action :flush, {...}}

(defn aggregate [c a f]
  "'c' channel of events
  'a' function that aggregates the value of event into result
  'f' function that writes result and event offset to db"
  (let [c (chan)]
    (go
     (loop [action :boot state {} event nil]
       (println action)
       (case action
         :boot (recur :process {:result 0 :offset 0} (<! c))
         :process (recur :process
                         {:result (a (:result state) (:value event))
                          :offset (:offset event)}
                         (<! c))
         :flush (f state))))
    c))



(def c (aggregate c + (fn [{:keys [result offset] :as state}]
                 (println (str "result:" result "offset" offset)))))

(>!! c {:action :process :event {:value 100 :offset 1}})

(>!! c {:action :flush :event {:value 100 :offset 1}})
