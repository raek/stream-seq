;; Copyright (c) Rasmus Svensson, 2010. All rights reserved.  The use
;; and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING the root of this
;; distribution.  By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license.  You must not
;; remove this notice, or any other, from this software.

(ns examples
  (:use se.raek.stream-seq
        se.raek.stream-seq.line
        clojure.contrib.server-socket
        clojure.test))

(with-test
  (defn upper-responses [seq]
    (->> seq
         (filter #(.startsWith % "to-upper: "))
         (map #(.toUpperCase %))))
  
  (is (= ["TO-UPPER: FOO"]
           (upper-responses ["ignore this..."
                             "to-upper: foo"
                             "...and this"]))))

(with-test
  (defn lower-responses [seq]
    (->> seq
         (filter #(.startsWith % "to-lower: "))
         (map #(.toLowerCase %))))
  
  (is (= ["to-lower: foo"]
           (lower-responses ["ignore this..."
                             "to-lower: FOO"
                             "...and this"]))))

(defn handle-connection [is os]
  (let [in-seq (source-seq (line-source is))
        out-sink (line-sink os)
        upper-feeder (feeder out-sink (upper-responses in-seq))
        lower-feeder (feeder out-sink (lower-responses in-seq))]
    @(:future upper-feeder)
    @(:future lower-feeder)))

(def server (create-server 9001 #'handle-connection))
