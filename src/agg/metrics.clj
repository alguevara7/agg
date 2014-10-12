(ns agg.metrics
  (:require [clojure.core.async :refer [go-loop >! <!]]
            [metrics.core :refer [new-registry]]
            [clojure.core.async.impl.protocols :as impl]
            [metrics.counters :refer [counter inc! dec!]]
            [metrics.meters :refer [meter mark!]]
            [metrics.reporters.graphite :as graphite])
  (:import [java.util.concurrent TimeUnit]
           [com.codahale.metrics MetricFilter]))

(def registry (new-registry))

(deftype BufferWithMetrics [buf c m]
  impl/Buffer
  (full? [this]
    (.full? buf))
  (remove! [this]
    (dec! c)
    (let [v (.remove! buf)]
      (mark! m (count v));;pass in a function instead ... maybe not be always count
      v))
  (add!* [this itm]
    (inc! c)
    (.add!* buf itm)
    this)
  clojure.lang.Counted
  (count [this]
    (.count buf)))

(defn buffer-with-metrics [buf name]
  (let [c (counter registry (str name ".c"))
        m (meter registry (str name ".m"))]
    (BufferWithMetrics. buf c m)))

(def reporter (graphite/reporter registry {:host "192.168.59.103"
                                           :prefix "agg"
                                           :rate-unit TimeUnit/SECONDS
                                           :duration-unit TimeUnit/MILLISECONDS
                                           :filter MetricFilter/ALL}))

(defn start-reporter [secs]
  (graphite/start reporter secs))

