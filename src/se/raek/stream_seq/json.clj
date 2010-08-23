;; Copyright (c) Rasmus Svensson, 2010. All rights reserved.  The use
;; and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING the root of this
;; distribution.  By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license.  You must not
;; remove this notice, or any other, from this software.

(ns se.raek.stream-seq.json
  "Read and write JSON data using the stream-seq library"
  (:use se.raek.stream-seq
        clojure.contrib.json)
  (:require [clojure.java.io :as io])
  (:import (java.io EOFException
                    StringReader
                    PrintWriter)))

;; End-of-stream handling in clojure.contrib.json/read-json is currenty broken.

;; (defrecord JSONSource [reader keywordize]
;;   Source
;;   (take! [source]
;;          (read-json reader keywordize false nil))
;;   (take! [source end-value]
;;          (read-json reader keywordize false end-value)))

;; Workaround:

(defrecord JSONSource [reader keywordize]
  Source
  (take! [source]
    (try (read-json reader keywordize false nil)
         (catch IllegalArgumentException _
           nil)))
  (take! [source end-value]
    (try (read-json reader keywordize false end-value)
         (catch IllegalArgumentException _
           end-value))))

(defrecord JSONSink [writer]
  Sink
  (put! [sink item]
    (try (do (write-json item writer)
             (.println writer)
             (.flush writer))
         (catch EOFException _
           (throw (IllegalStateException. "The writer is closed.")))))
  (close! [sink]
    (.close writer)))

(defn json-source
  "Makes a source that reads a sequence of json scalars, arrays and objects
   from in. If in is a string, the json data will be read from its contents.
   Otherwise, the arguments are passed to clojure.java.io/reader to make a
   reader of in, if it isn't one."
  [in & options]
  (let [opts (apply hash-map options)
        keywordize (get options :keywordize true)
        reader (if (string? in)
                 (StringReader. in)
                 (apply io/reader in options))]
    (JSONSource. reader keywordize)))

(defn json-sink
  "Makes a sink that writes json scalars, arrays and objects to out. The
   arguments are passed to clojure.java.io/writer to make a writer of out, if
   it isn't one."
  [out & options]
  (JSONSink. (if (instance? PrintWriter out)
               out
               (PrintWriter. (apply io/writer out options)))))
