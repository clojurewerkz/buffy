(ns clojurewerkz.buffy.core
  (:refer-clojure :exclude [read])
  (:require [clojurewerkz.buffy.types :as t]
            [clojurewerkz.buffy.frames :refer :all]
            [clojurewerkz.buffy.util :refer :all]
            [clojurewerkz.buffy.types.protocols :refer :all])
  (:import [io.netty.buffer UnpooledByteBufAllocator ByteBufAllocator]))

(def ^ByteBufAllocator allocator UnpooledByteBufAllocator/DEFAULT)

(defn- set-writer-index
  [buf]
  (.writerIndex buf (.capacity buf))
  buf)

(defn heap-buffer
  ([]
     (set-writer-index (.heapBuffer allocator)))
  ([^long initial-capacity]
     (set-writer-index (.heapBuffer allocator initial-capacity initial-capacity)))
  ([^long initial-capacity ^long max-capacity]
     (set-writer-index (.heapBuffer allocator initial-capacity max-capacity))))

(defn direct-buffer
  ([]
     (set-writer-index (.directBuffer allocator)))
  ([^long initial-capacity]
     (set-writer-index (.directBuffer allocator initial-capacity initial-capacity)))
  ([^long initial-capacity ^long max-capacity]
     (set-writer-index (.directBuffer allocator initial-capacity max-capacity))))

(defn wrapped-buffer
  "Returns a buffer that wraps the given byte array, `j.nio.ByteBuffer` or netty `ByteBuf`"
  [orig-buffer]
  (set-writer-index (.wrappedBuffer orig-buffer)))

(defprotocol Composable
  (decompose [this] [this buffer])
  (compose [this kvps]))

(defprotocol IBuffyBuf
  (buffer [this])
  (slices [this])

  (get-field [this field-name])
  (get-field-idx [this field-idx])

  (set-field [this field-name value])
  (set-field-idx [this field-name value]))

(deftype BuffyBuf [buf indexes types positions]
  IBuffyBuf
  (buffer [b] buf)

  (set-field [b field-name value]
    (let [idx      (get (.indexes b) field-name)
          _        (assert idx (format "Index for field `%s` is `%s`, please verify that field name matches mappings" field-name idx))
          type     (nth (.types b) idx)
          position (nth (.positions b) idx)]

      (write type
             (.buf b)
             position
             value))
    b)

  (get-field [b field-name]
    (let [idx      (get (.indexes b) field-name)
          _        (assert idx (format "Index for field `%s` is `%s`, please verify that field name matches mappings" field-name idx))
          type     (nth (.types b) idx)
          position (nth (.positions b) idx)]
      (read type
            (.buf b)
            position)))

  Composable
  (compose [this kvps]
    (doseq [[k v] kvps]
      (set-field this k v))
    buf)

  (decompose [b]
    (into {}
          (for [[field _] indexes]
            [field (.get-field b field)]))))

(deftype DynamicBuffer [frames]
  Composable
  (compose [this values]
    (let [size   (encoding-size frames values)
          buffer (direct-buffer size)]
      (write frames buffer 0 values)))

  (decompose [this buffer]
    (read frames buffer 0)))

(defn dynamic-buffer
  [& frames]
  (DynamicBuffer. (apply composite-frame frames)))

(defn spec
  [& kvps]
  (partition 2 kvps))

(defn compose-buffer
  [spec & {:keys [buffer-type orig-buffer] :or {buffer-type :direct}}]
  (let [indexes    (zipmap (map first spec)
                           (iterate inc 0))
        types      (map second spec)
        total-size (reduce + (map size types))
        buffer     (cond
                    orig-buffer             (wrapped-buffer orig-buffer)
                    (= buffer-type :heap)   (heap-buffer total-size)
                    (= buffer-type :direct) (direct-buffer total-size)
                    :else                   (heap-buffer total-size))
        positions  (positions types)]
    (BuffyBuf. buffer indexes types positions)))

(def bit-type     t/bit-type)
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
(def enum-type  t/enum-type)

(defn to-bit-map
  "Converts given value to vector of `true`/`false`, which represent on
   and off set bytes."
  [type value]
  (let [length (size type)
        buffer (heap-buffer length)
        _      (write type buffer 0 value)
        bt     (bit-type length)]
    (reverse (read bt buffer 0))))

(defn from-bit-map
  "Converts a vector of `true`/`false`, which represent a series of on
   and off set bytes to the actual value, based on given `type`."
  [type value]
  (let [length (size type)
        bt     (bit-type length)
        buffer (heap-buffer length)
        _      (write bt buffer 0 (reverse value))]
    (read type buffer 0)))

(defn to-binary
  "Converts series of `true`/`false` flags to series of `1` and `0` where
   1 is represents `on` and `0` represents `off`."
  [value]
  (mapv #(if % 1 0) value))
