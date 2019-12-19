;; Copyright (c) 2013-2014 Alex Petrov, Michael S. Klishin, and the ClojureWerkz Team
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns clojurewerkz.buffy.core
  (:require [clojurewerkz.buffy.types           :as t]
            [clojurewerkz.buffy.frames          :as frames]
            [clojurewerkz.buffy.util            :as util]
            [clojurewerkz.buffy.types.protocols :as p])
  (:import [java.nio ByteBuffer]
           [io.netty.buffer ByteBuf ByteBufAllocator Unpooled
            UnpooledByteBufAllocator]))

(set! *warn-on-reflection* true)

(def ^ByteBufAllocator allocator UnpooledByteBufAllocator/DEFAULT)

(defn reset-writer-index
  [^ByteBuf buf]
  (.resetWriterIndex buf)
  buf)

(defn reset-reader-index
  [^ByteBuf buf]
  (.resetReaderIndex buf)
  buf)

(def reset-indexes (comp reset-reader-index reset-writer-index))

(defn rewind-until-end
  [^ByteBuf buf]
  (.writerIndex buf (.capacity buf))
  buf)

(defn heap-buffer
  ([]
   (rewind-until-end (.heapBuffer allocator)))
  ([^long initial-capacity]
   (rewind-until-end (.heapBuffer allocator initial-capacity initial-capacity)))
  ([^long initial-capacity ^long max-capacity]
   (rewind-until-end (.heapBuffer allocator initial-capacity max-capacity))))

(defn direct-buffer
  ([]
   (rewind-until-end (.directBuffer allocator)))
  ([^long initial-capacity]
   (rewind-until-end (.directBuffer allocator initial-capacity initial-capacity)))
  ([^long initial-capacity ^long max-capacity]
   (rewind-until-end (.directBuffer allocator initial-capacity max-capacity))))

(defprotocol Wrappable
  (wrapped-buffer [this] "Returns a buffer that wraps the given byte array, `j.nio.ByteBuffer` or netty `ByteBuf`"))

(extend-protocol Wrappable
  (Class/forName "[B")
  (wrapped-buffer [this]
    (rewind-until-end (Unpooled/wrappedBuffer ^bytes this)))

  ByteBuffer
  (wrapped-buffer [this]
    (rewind-until-end (.order (Unpooled/wrappedBuffer this)
                              (.order this))))

  ByteBuf
  (wrapped-buffer [this]
    (rewind-until-end (.order (Unpooled/wrappedBuffer this)
                              (.order this)))))

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
    (let [idx      (get indexes field-name)
          _        (assert idx (format "Index for field `%s` is `%s`, please verify that field name matches mappings" field-name idx))
          type     (nth types idx)
          position (nth positions idx)]

      (p/write type
               buf
               position
               value))
    b)

  (get-field [b field-name]
    (let [idx      (get indexes field-name)
          _        (assert idx (format "Index for field `%s` is `%s`, please verify that field name matches mappings" field-name idx))
          type     (nth types idx)
          position (nth positions idx)]
      (p/read type
              buf
              position)))

  Composable
  (compose [this kvps]
    (doseq [[k v] kvps]
      (set-field this k v))
    buf)

  (decompose [b]
    (into {}
          (for [[field _] indexes]
            [field (get-field b field)]))))

(deftype DynamicBuffer [frames]
  Composable
  (compose [this values]
    (let [size   (frames/encoding-size frames values)
          buffer (direct-buffer size)]
      (p/write frames buffer 0 values)))

  (decompose [this buffer]
    (p/read frames buffer 0)))

(defn dynamic-buffer
  [& frames]
  (DynamicBuffer. (apply frames/composite-frame frames)))

(defn spec
  [& kvps]
  (partition 2 kvps))

(defn compose-buffer
  [spec & {:keys [buffer-type orig-buffer] :or {buffer-type :direct}}]
  (let [indexes    (zipmap (map first spec)
                           (iterate inc 0))
        types      (map second spec)
        total-size (reduce + (map p/size types))
        buffer     (cond
                     orig-buffer             (wrapped-buffer orig-buffer)
                     (= buffer-type :heap)   (heap-buffer total-size)
                     (= buffer-type :direct) (direct-buffer total-size)
                     :else                   (heap-buffer total-size))
        positions  (util/positions types)]
    (BuffyBuf. buffer indexes types positions)))

(def bit-type       t/bit-type)
(def bit-map-type   t/bit-map-type)
(def int32-type     t/int32-type)
(def uint32-type    t/uint32-type)
(def boolean-type   t/boolean-type)
(def byte-type      t/byte-type)
(def ubyte-type     t/ubyte-type)
(def short-type     t/short-type)
(def ushort-type    t/ushort-type)
(def medium-type    t/medium-type)
(def umedium-type   t/umedium-type)
(def float-type     t/float-type)
(def double-type    t/double-type)
(def long-type      t/long-type)
(def ulong-type     t/ulong-type)
(def string-type    t/string-type)
(def bytes-type     t/bytes-type)
(def composite-type t/composite-type)
(def repeated-type  t/repeated-type)
(def enum-type      t/enum-type)
(def uuid-type      t/uuid-type)

(defn to-bit-map
  "Converts given value to vector of `true`/`false`, which represent on
   and off set bytes."
  [type value]
  (let [length (p/size type)
        buffer (heap-buffer length)
        _      (p/write type buffer 0 value)
        bt     (bit-type length)]
    (reverse (p/read bt buffer 0))))

(defn from-bit-map
  "Converts a vector of `true`/`false`, which represent a series of on
   and off set bytes to the actual value, based on given `type`."
  [type value]
  (let [length (p/size type)
        bt     (bit-type length)
        buffer (heap-buffer length)
        _      (p/write bt buffer 0 (reverse value))]
    (p/read type buffer 0)))

(defn to-binary
  "Converts series of `true`/`false` flags to series of `1` and `0` where
   1 is represents `on` and `0` represents `off`."
  [value]
  (mapv #(if % 1 0) value))
