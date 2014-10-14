(ns ad-view-counter
  (require [agg.core :refer :all]
           [agg.metrics :refer :all]
           [clojure.core.async :refer [chan close! buffer go-loop <! timeout go <!!]]
           [clojure.java.jdbc :as jdbc]
           [clj-time.core :as t]
           [clj-time.coerce :as c]))

(def db {:subprotocol "mysql"
         :subname "//192.168.59.103:3306/agg"
         :user "agg"
         :password "agg"})

(defn flush-state [partition-id counter-type {:keys [delta offset] :as state}]
  (let [now (c/to-date (t/now))]
    (jdbc/with-db-transaction [conn db :isolation :read-committed]
        (doseq [[ad-id value] delta] ;; one per transaction for now, support batching later
          (jdbc/execute! conn [(str "INSERT INTO ad_counter(ad_id, type, value, modification_date, creation_date)"
                                    " VALUES (?,?,?,?,?)"
                                    " ON DUPLICATE KEY UPDATE value = ?, modification_date = ?")
                               ad-id counter-type value now now value now]))
        (jdbc/execute! conn [(str "INSERT INTO ad_counter_offset(partition_id, offset) VALUES (?, ?)"
                                  " ON DUPLICATE KEY UPDATE offset = ?")
                             partition-id offset offset]))))

(defn increment-counter [[result delta] ad-id]
  (let [new-result (update-in result [ad-id] #(if % (inc %) 1))
        new-delta (assoc delta ad-id (get new-result ad-id))]
    [new-result new-delta]))


(defn fetch-event [partition-id offset]
  (let [n 100000]
    {:value (+ (* partition-id n) (rand-int n)) :offset (inc offset)}))

;;(defn agg [in f init out n msecs]


;;- change message-chunk-size, flush n limit, flush msecs limit, input channel size, output channel size with Midi device
;;  - close current input
;;  - create new agg :) with new params
(defn start-counting [counter-type partition-id in-size out-size chunk-size]
  (let [in (chan (buffer-with-metrics (buffer in-size) (str "input." partition-id)))
        out-buf (buffer-with-metrics (buffer out-size) (str "output." partition-id))
        out (chan out-buf)]
    (agg in increment-counter {} out chunk-size (* 1 1000))
    (sample-from-offset (partial fetch-event partition-id) 0 1 in)
    (subscribe out (partial flush-state partition-id counter-type))
    (fn []
      (close! in)
      (close! out)
      (go-loop []
               (println "waiting ...")
               (<! (timeout 1000))
               (when-not (zero? (count out-buf))
                 (recur)))))
  )

(start-reporter 1)

#_(def c (start-counting "view" 1))
#_(close! c)


(def a
(let [in-size (atom 16)
      out-size (atom 16)
      chunk-size (atom 16)
      ch (atom nil)]
  (reset! ch (start-counting "view" 0 @in-size @out-size @chunk-size))
  (add-watch chunk-size ::chunk-size (fn [_ _ _ value]
                                       (<!! (@ch))
                                       (reset! ch (start-counting "view" 0 @in-size @out-size @chunk-size))))
  (add-watch in-size ::in-size (fn [_ _ _ value]
                                       (<!! (@ch))
                                       (reset! ch (start-counting "view" 0 @in-size @out-size @chunk-size))))
  (add-watch out-size ::out-size (fn [_ _ _ value]
                                       (<!! (@ch))
                                       (reset! ch (start-counting "view" 0 @in-size @out-size @chunk-size))))
  [in-size out-size chunk-size]))


(let [[in-size out-size chunk-size] a]
  (reset! out-size 16))

a





