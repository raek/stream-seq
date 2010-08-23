;; Copyright (c) Rasmus Svensson, 2010. All rights reserved.  The use
;; and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING the root of this
;; distribution.  By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license.  You must not
;; remove this notice, or any other, from this software.

(ns se.raek.stream-seq.line
  "Line-oriented streams built on Java's BufferReader and BufferedWriter"
  {:author "Rasmus Svensson"}
  (:use se.raek.stream-seq)
  (:require [clojure.java.io :as io])
  (:import (java.io EOFException)))

(defrecord LineSource [reader]
  Source
  (take! [source]
         (.readLine reader))
  (take! [source end-value]
         (if-let [line (.readLine reader)]
           line
           end-value)))

(defrecord LineSink [writer]
  Sink
  (put! [sink item]
        (try
          (doto writer
            (.write (str item))
            (.newLine)
            (.flush))
          (catch EOFException _
            (throw IllegalStateException "The writer is closed."))))
  (close! [sink]
    (.close writer)))

(defn line-source
  "Makes a source that reads lines from in. The arguments are passed to
   clojure.java.io/reader to make a reader f in, if it isn't one."
  [in & options]
  (LineSource. (apply io/reader in options)))

(defn line-sink
  "Makes a source that writes lines to out. The arguments are passed to
   clojure.java.io/writer to make a writer of out, if it isn't one."
  [out & options]
  (LineSink. (apply io/writer out options)))
