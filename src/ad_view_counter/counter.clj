(ns ad-view-counter
  (require [agg.core :refer :all]
           [clojure.core.async :as async]
           [clojure.java.jdbc :as jdbc]
           [clj-time.core :as t]
           [clj-time.coerce :as c]))

;; TODO:
;; - graphite!
;; - write changed counters only

(def db {:subprotocol "mysql"
         :subname "//192.168.59.103:3306/agg"
         :user "agg"
         :password "agg"})


;; only write counters that changed
(defn flush-state [partition-id counter-type {:keys [result offset] :as state}]
  (let [now (c/to-date (t/now))]
    (jdbc/with-db-transaction [conn db :isolation :read-committed]
        (doseq [[ad-id value] result] ;; one per transaction for now, support batching later
          (jdbc/execute! conn [(str "INSERT INTO ad_counter(ad_id, type, value, modification_date, creation_date)"
                                    " VALUES (?,?,?,?,?)"
                                    " ON DUPLICATE KEY UPDATE value = ?, modification_date = ?")
                               ad-id counter-type value now now value now]))
        (jdbc/execute! conn [(str "INSERT INTO ad_counter_offset(partition_id, offset) VALUES (?, ?)"
                                  " ON DUPLICATE KEY UPDATE offset = ?")
                             partition-id offset offset]))))

(def counter (atom 0))

(defn process-event [r ad-id]
  (swap! counter dec)
  (update-in r [ad-id] #(if % (inc %) 1)))


(defn fetch-event [partition-id offset]
  (let [n 100000]
    (swap! counter inc)
    {:action :process :value (+ (* partition-id n) (rand-int n)) :offset (inc offset)}))

;; TODO
;;
;; - need to publish aggregation batches of a certain size  (the batch size is how many keys there are in state)
;;   that controls how much data will be written to the db in one transaction
;;
;; - allow events to be fetched in batches
(defn start-counting [counter-type partition-id]
  (agg (fn [] {:result {} :offset 0})
       (partial fetch-event partition-id) 1 100
       process-event
       (partial flush-state partition-id counter-type) 1000 100
       50))



(def c1 (start-counting "view" 0))
(def c2 (start-counting "view" 1))
(def c3 (start-counting "view" 3))
(def c4 (start-counting "view" 4))
(def c5 (start-counting "view" 5))
(def c6 (start-counting "view" 6))
(def c7 (start-counting "view" 7))
(def c8 (start-counting "view" 8))
(def c9 (start-counting "view" 9))
(def c10 (start-counting "view" 10))
(def c11 (start-counting "view" 11))
(def c12 (start-counting "view" 12))
(def c13 (start-counting "view" 13))
(def c13 (start-counting "view" 14))
(def c14 (start-counting "view" 15))



(async/close! c1)

@counter



