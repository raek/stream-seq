;; Copyright (c) Rasmus Svensson, 2010. All rights reserved.  The use
;; and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING the root of this
;; distribution.  By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license.  You must not
;; remove this notice, or any other, from this software.

(ns se.raek.stream-seq
  "Stream-oriented programming with sequences"
  {:author "Rasmus Svensson"}
  (:require [clojure.java.io :as io])
  (:use (clojure.contrib def json))
  (:import (clojure.lang Seqable)
           (java.util.concurrent LinkedBlockingQueue)))



;;; Sources

(defprotocol Source
  "Protocol for things that provide a way of taking items from a stream."
  (take! [source] [source end-value]
    "Retrieves and removes the next object from the source. If the source
     is closed the end-value sentinel object -- or nil if it is not given
     -- is returned instead."))

(defonce- end-of-stream (Object.))

(defn source-seq
  "Returns a lazy sequence that takes items from source. The source-seq will
   only contain all items emanating from the source if it is the only process
   taking from that source."
  [source]
  (lazy-seq
   (let [item (take! source end-of-stream)]
     (if-not (= item end-of-stream)
       (cons item (source-seq source))))))



;;; Sinks

(defprotocol Sink
  "Protocol for things that provide a way of putting items into a stream."
  (put! [sink item]
    "Puts the item into the sink. Throws an IllegalStateException if the
     sink is closed.")
  (close! [sink]
    "Closes the sink so that no more objects can be put into it."))

(defn put
  "Like put!, but is safe to use in transactions and requires the sink to be
   wrapped in an agent. The actual put! call will be held until the
   transaction succeeds."
  [sink-agent item]
  (send-off sink-agent put! item))

(defn close 
  "Like close!, but is safe to use in transactions and requires the sink to be
   wrapped in an agent. The actual close! call will be held until the
   transaction succeeds."
  [sink-agent item]
  (send-off sink-agent close!))

(extend-type clojure.lang.Atom
  Sink
  (put! [sink item]
    (put sink item))
  (close! [sink]
    (close sink)))



;;; Processes

(defn feeder
  "Starts a separate thread that puts every element of coll into sink. coll
   is presumably a lazy sequence that blocks during its realization. The
   return value is a map with the key :future associated with the future
   object that traverses the sequence."
  {:arglists '([sink coll :close-when-done false])}
  [sink coll & {:keys [close-when-done], :or {close-when-done false}}]
  {:future (future
             (doseq [item coll]
               (put! sink item)))})

(defn drainer
  "Takes items from source and puts them into sink. A separate thread is
   started, which takes items from source and puts them into sink until the
   source is closed. The return value is a map with the key :future associated
   with the future object that traverses the sequence."
  {:arglists '([source sink :close-when-done false])}
  [source sink & {:keys [close-when-done], :or {close-when-done false}} ]
  (feeder sink (source-seq source)))

(defn walker
  "Starts a separate thread that walks through coll -- presumably a lazy
   sequence that blocks during its realization -- and records the last part of
   its tail that has been realized. This is intended to provide a way to
   \"jump in\" into the last known part of a sequence without having to retain
   its head. The return value is a map with the key :future associated
   with the future object that traverses the sequence, and :tail with the last
   seen realized part of the sequence inside an atom."
  [coll]
  (let [tail (atom coll)
        ftr (future
              (loop [s coll]
                (reset! tail s)
                (if (seq s)
                  (recur (rest s)))))]
    {:tail tail
     :future ftr}))



;;; Pipes

(defonce- nil-substitute (Object.))

(defonce- plug (Object.))

(defrecord Pipe [queue source-closed source-lock sink-closed sink-lock]
  Source
  (take! [source]
    (take! source nil))
  (take! [source end-value]
    (locking source-lock
      (if @source-closed
        end-value
        (let [item (.take queue)]
          (condp = item
              nil-substitute nil
              plug (do (compare-and-set! source-closed false true)
                       end-value)
              item)))))
  Sink
  (put! [sink item]
    (locking sink-lock
      (if @sink-closed
        (throw (IllegalStateException. "The pipe is closed."))
        (if (nil? item)
          (.put queue nil-substitute)
          (.put queue item)))))
  (close! [sink]
    (locking sink-lock
      (if (compare-and-set! sink-closed false true)
        (.put queue plug)))))

(defn pipe
  "Makes a pipe object, which can be used both as a Source and a Sink. Objects
  put into the sink will be stored in a queue until they are taken from the
  source."
  []
  (Pipe. (new LinkedBlockingQueue)
         (atom false) (Object.)
         (atom false) (Object.)))
