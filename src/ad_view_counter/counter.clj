(ns ad-view-counter
  (require [agg.core :refer :all]
           [clojure.core.async :as async]
           [clojure.java.jdbc :as jdbc]
           [clj-time.core :as t]
           [clj-time.coerce :as c]))

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

(defn process-event [r ad-id]
  (update-in r [ad-id] #(if % (inc %) 1)))

(defn example [] (agg (fn [] {:result {} :offset 0})
                      (fn [offset]
                        {:action :process :value (rand-int 100) :offset (inc offset)}) 1
                      process-event
                      (partial flush-state 0 "view") 5000))


(def c (example))

(async/close! c)

