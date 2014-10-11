(ns agg.util
  (:require [overtone.live :refer :all]))

(midi-connected-devices)

(event-debug-off)

(on-event [:midi :control-change]
          (fn [{:keys [data1 data2] :as e}]
            (case data1
              18 (println "Value = " data2)
              (println e)))
          ::x-session-handler)

(remove-event-handler ::x-session-handler)
