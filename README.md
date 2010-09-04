# stream-seq

Stream-oriented programming with sequences

## What is this?

This is a library to make it easier to program applications doing blocking I/O using clojure sequences. Its primary usage is in conjunction with sockets.

## Alright, so what does it look like?

Imagine that you have two threads: a producer and a consumer. One way of letting them communicate is to use java.util.concurrent.BlockingQueue directly using Clojure's interop features. Another way would be to wrap that into something that looks more Clojurian:

    (use '[se.raek.stream-seq :only [pipe put! close! take!]])
    (def p (pipe))
    (def end (Object.))
    
    ;; Producer thread
    (future
      (doseq [item (range 10)]
        (put! p item))
      (close! p))
    
    ;; Consumer thread
    (future
      (loop [item (take! p end)]
        (when-not (= item end)
          (println item)
          (recur (take! p end)))))

The consumer code uses manual iteration. Wouldn't it be nice to to do this in a more *sequential* manner? Exposing input as lazy sequences is one of the central ideas behind stream-seq. The source-seq function returns a lazy sequence that takes items from the source until it is closed:

    (use '[se.raek.stream-seq :only [source-seq]])
    
    ;; Consumer thread - sequence version
    (future
      (doseq [item (source-seq p)]
        (println item)))

## API

See http://raek.github.com/stream-seq/

## Installation

Clone this repo.

## License

EPL v.10
