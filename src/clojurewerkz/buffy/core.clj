(ns clojurewerkz.buffy.core
  (:refer-clojure :exclude [read])
  (:use clojurewerkz.buffy.types.protocols)
  (:require [clojurewerkz.buffy.types :as t]
            [clojurewerkz.buffy.util :refer :all])
  (:import [io.netty.buffer UnpooledByteBufAllocator ByteBufAllocator]))

(def ^ByteBufAllocator allocator UnpooledByteBufAllocator/DEFAULT)

(defn heap-buffer
  ([]
     (.heapBuffer allocator))
  ([^long initial-capacity]
     (.heapBuffer allocator initial-capacity initial-capacity))
  ([^long initial-capacity ^long max-capacity]
     (.heapBuffer allocator initial-capacity max-capacity)))

(defn direct-buffer
  ([]
     (.directBuffer allocator))
  ([^long initial-capacity]
     (.directBuffer allocator initial-capacity initial-capacity))
  ([^long initial-capacity ^long max-capacity]
     (.directBuffer allocator initial-capacity max-capacity)))

(defprotocol IBuffyBuf
  (buffer [this])
  (slices [this])
  (decompose [this])
  (set-fields [this kvps])

  (get-field [this field-name])
  (get-field-idx [this field-idx])

  (set-field [this field-name value])
  (set-field-idx [this field-name value]))

(deftype BuffyBuf [buf indexes types positions]
  IBuffyBuf
  (buffer [b] buf)

  (set-fields [this kvps]
    (doseq [[k v] kvps]
      (set-field this k v)))

  (set-field [b field-name value]
    (let [idx (get (.indexes b) field-name)]
      (write (nth (.types b) idx)
             (.buf b)
             (nth (.positions b) idx)
             value))
    b)

  (get-field [b field-name]
    (let [idx (get (.indexes b) field-name)]
      (read (nth (.types b) idx)
            (.buf b)
            (nth (.positions b) idx))))

  (decompose [b]
    (into {}
          (for [[field _] indexes]
            [field (.get-field b field)]))))

(defn compose-buffer
  [spec & {:keys [buffer-type] :or {buffer-type :direct}}]
  (let [indexes    (zipmap (map first spec)
                           (iterate inc 0))
        types      (map second spec)
        total-size (reduce + (map size types))
        buffer     (cond
                    (= buffer-type :heap)   (heap-buffer total-size)
                    (= buffer-type :direct) (direct-buffer total-size)
                    :else                   (heap-buffer total-size))
        positions  (positions types)]
    (BuffyBuf. buffer indexes types positions)))

(def int32-type   t/int32-type)
(def boolean-type t/boolean-type)
(def byte-type    t/byte-type)
(def short-type   t/short-type)
(def medium-type  t/medium-type)
;; TODO: Add unsigned types: byte, int, medium, short
(def float-type   t/float-type)
(def long-type    t/long-type)
(def string-type  t/string-type)
(def bytes-type   t/bytes-type)
(def composite-type t/composite-type)
(def repeated-type  t/repeated-type)
