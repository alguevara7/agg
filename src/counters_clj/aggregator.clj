(ns counters-clj.aggregator
  (:require [clojure.core.async :as async :refer [chan go <! >! >!!]]))

; message source

; message offset


; coordinator

;; aggregator


;; {:action :boot, {...}}

;; {:action :process, {:offset 1 :value 1}}

;; {:action :flush, {...}}


(defn aggregate [a v f]
  "
  'a' function that aggregates the value of event into result
  'f' function that writes result and event offset to db
  "
  (let [c (chan)]
    (go
     (loop [state {}
            {:keys [action value offset] :as event} {:action :boot}]
       (case action
         :boot (recur {:result v :offset 0} (<! c))
         :process (recur {:result (a (:result state) value) :offset offset}
                         (<! c))
         :flush (do (f state)
                  (recur state
                         (<! c))))))
    c))



(def c (aggregate (fn [r v]
                    (println (str "AGREGATE -> r:" r " v:" v))
                    (+ r v))
                  0
                  (fn [{:keys [result offset] :as state}]
                    (println (str "FLUSH: -> " "result:" result " offset: " offset)))))



(>!! c {:action :process :value 1001 :offset 4})

(>!! c {:action :flush})

