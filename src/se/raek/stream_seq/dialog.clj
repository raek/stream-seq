;; Copyright (c) Rasmus Svensson, 2010. All rights reserved.  The use
;; and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING the root of this
;; distribution.  By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license.  You must not
;; remove this notice, or any other, from this software.

(ns se.raek.stream-seq.dialog
  "Pop-up dialogs for experimenting with the stream-seq library"
  (:use se.raek.stream-seq)
  (:import (javax.swing JOptionPane)))

(defrecord DialogSource [title message]
  Source
  (take! [source]
    (JOptionPane/showInputDialog
     nil message title JOptionPane/QUESTION_MESSAGE))
  (take! [source end-value]
    (if-let [answer (JOptionPane/showInputDialog
                     nil message title JOptionPane/QUESTION_MESSAGE)]
      answer
      end-value)))

(defrecord DialogSink [title]
  Sink
  (put! [sink item]
    (JOptionPane/showMessageDialog
     nil (str item) title JOptionPane/PLAIN_MESSAGE))
  (close! [sink]
    (JOptionPane/showMessageDialog
     nil "The sink is now closed." title
     JOptionPane/INFORMATION_MESSAGE)))

(defn dialog-source
  "Makes a source that asks the user for items with pop-up dialogs."
  ([]
     (dialog-source (str *ns*) "Please enter the next item of the stream:"))
  ([message]
     (dialog-source (str *ns*) message))
  ([title message]
     (DialogSource. title message)))

(defn dialog-sink
  "Makes a sink that displays items to the user with pop-up dialogs."
  ([]
     (dialog-sink (str *ns*)))
  ([title]
     (DialogSink. title)))
