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

(ns clojurewerkz.buffy.types
  (:refer-clojure :exclude [read])
  (:require [clojurewerkz.buffy.util :refer :all]
            [clojurewerkz.buffy.types.protocols :refer :all])
  (:import [io.netty.buffer UnpooledByteBufAllocator ByteBufAllocator]))

;;
;; Primitive types
;;


(deftype Int32Type []
  BuffyType
  (size [_] 4)
  (write [bt buffer idx value]
    (.setInt buffer idx value))
  (read [by buffer idx]
    (.getInt buffer idx))

  (rewind-write [bt buffer value]
    (.writeInt buffer value))
  (rewind-read [by buffer]
    (.readInt buffer))

  Object
  (toString [_]
    "Int32Type"))

(deftype BooleanType []
  BuffyType
  (size [_] 1)
  (write [bt buffer idx value]
    (.setBoolean buffer idx value))
  (read [by buffer idx]
    (.getBoolean buffer idx))

  (rewind-write [bt buffer value]
    (.writeBoolean buffer value))
  (rewind-read [by buffer]
    (.readBoolean buffer))


  Object
  (toString [_]
    "BooleanType"))

(deftype ByteType []
  BuffyType
  (size [_] 1)
  (write [bt buffer idx value]
    (.setByte buffer idx value))
  (read [by buffer idx]
    (.getByte buffer idx))

  (rewind-write [bt buffer value]
    (.writeByte buffer value))
  (rewind-read [by buffer]
    (.readByte buffer))

  Object
  (toString [_]
    "ByteType"))

(deftype ShortType []
  BuffyType
  (size [_] 2)
  (write [bt buffer idx value]
    (.setShort buffer idx value))
  (read [by buffer idx]
    (.getShort buffer idx))

  (rewind-write [bt buffer value]
    (.writeShort buffer value))
  (rewind-read [by buffer]
    (.readShort buffer))

  Object
  (toString [_]
    "ShortType"))

(deftype MediumType []
  BuffyType
  (size [_] 3)
  (write [bt buffer idx value]
    (.setMedium buffer idx value))
  (read [by buffer idx]
    (.getMedium buffer idx))

  (rewind-write [bt buffer value]
    (.writeMedium buffer value))
  (rewind-read [by buffer]
    (.readMedium buffer))

  Object
  (toString [_]
    "MediumType"))

(deftype BitType [byte-length]
  BuffyType
  (size [_] byte-length)
  (write [bt buffer idx value]
    (assert (seqable? value) "Bit Field value should be collection")
    (assert (= (count value) (* byte-length 8)) (str "Bit Field value should be " (* byte-length 4) " bits long"))

    (doseq [byte-index (range 0 byte-length)]
      (let [idx         (+ idx (- byte-length 1 byte-index))
            changed-val (reduce (fn [acc [v index]]
                                  (if v
                                    (bit-set acc index)
                                    (bit-clear acc index)))
                                0
                                (map vector
                                     (->> value (drop (* 8 byte-index)) (take 8))
                                     (iterate inc 0)))]
        (.setByte buffer idx changed-val))))
  (read [by buffer idx]
    (mapcat identity
            (for [byte-index (range 0 byte-length)]
              (let [idx         (+ idx (- byte-length 1 byte-index))
                    current-val (.getByte buffer idx)]
                (map #(bit-test current-val %) (range 0 8))))))


  (rewind-write [bt buffer value]
    (throw (Exception. "Don't know how to rewind-write a bit type")))
  (rewind-read  [bt buffer]
    (throw (Exception. "Don't know how to rewind-read a bit type")))

  Object
  (toString [_]
    (str "Bit Type: " byte-length)))

(defn- bit-map-extract-field-value [value field-name field-length]
  (let [mask (dec (bit-shift-left (long 1) field-length))
        field-raw-value (get value field-name)]
    (case field-raw-value
      true  1
      false 0
      nil   0
      (bit-and mask field-raw-value))))

(defn- bit-map-hash-map->long-value [value fields]
  (reduce (fn [out [field-name field-length]]
            (+ (bit-map-extract-field-value value
                                            field-name
                                            field-length)
               (bit-shift-left out field-length)))
          (long 0)
          fields))

(defn- bit-map-bits->long [bits]
  (reduce (fn [accu x] (+ (bit-shift-left accu 1)
                         (if x 1 0)))
          (long 0)
          bits))

(defn- bit-map-bits->hash-map [bits fields]
  (first
   (reduce
    (fn [[out remaining] [field-name field-length]]
      [(assoc out field-name
              (bit-map-bits->long (take field-length remaining)))
       (drop field-length remaining)])
    [{} bits]
    fields)))

(deftype BitMapType [inner-bits fields]
  BuffyType
  (size [_] (size inner-bits))
  (write [bt buffer idx value]
    (let [bit-map-value (bit-map-hash-map->long-value value fields)
          bit-indexes   (reverse (range (* 8 (size inner-bits))))]
      (write inner-bits buffer idx
             (mapv #(bit-test bit-map-value %) bit-indexes))))

  (read [by buffer idx]
    (bit-map-bits->hash-map (read inner-bits buffer idx) fields))


  (rewind-write [bt buffer value]
    (throw (Exception. "Don't know how to rewind-write a bit map type")))
  (rewind-read  [bt buffer]
    (throw (Exception. "Don't know how to rewind-read a bit map type")))

  )

(deftype FloatType []
  BuffyType
  (size [_] 4)
  (write [bt buffer idx value]
    (.setFloat buffer idx value))
  (read [by buffer idx]
    (.getFloat buffer idx))

  (rewind-write [bt buffer value]
    (.writeFloat buffer value))
  (rewind-read [by buffer]
    (.readFloat buffer))

  Object
  (toString [_]
    "FloatType"))

(deftype DoubleType []
  BuffyType
  (size [_] 8)
  (write [bt buffer idx value]
    (.setDouble buffer idx value))
  (read [by buffer idx]
    (.getDouble buffer idx))

  (rewind-write [bt buffer value]
    (.writeDouble buffer value))
  (rewind-read [by buffer]
    (.readDouble buffer))

  Object
  (toString [_]
    "DoubleType"))

(deftype LongType []
  BuffyType
  (size [_] 8)
  (write [bt buffer idx value]
    (.setLong buffer idx value))
  (read [by buffer idx]
    (.getLong buffer idx))

  (rewind-write [bt buffer value]
    (.writeLong buffer value))
  (rewind-read [by buffer]
    (.readLong buffer))

  Object
  (toString [_]
    "LongType"))

(deftype BytesType [size]
  BuffyType
  (size [_] size)
  (write [bt buffer idx value]
    (.setBytes buffer idx value)
    (zero-fill-till-end buffer idx (count value) (.size bt)))
  (read [bt buffer idx]
    (read-nonempty-bytes buffer idx (.size bt)))

  (rewind-write [bt buffer value]
    (.writeBytes buffer value)
    (zero-fill-till-end buffer (count value) (.size bt)))
  (rewind-read [bt buffer]
    (read-nonempty-bytes buffer (.size bt)))

  Object
  (toString [_]
    (str "BytesType: (" size ")")))

(deftype StringType [size]
  BuffyType
  (size [_] size)
  (write [bt buffer idx value]
    ;; TODO assert
    (.setBytes buffer idx (.getBytes value))
    (zero-fill-till-end buffer idx (count value) size))
  (read [bt buffer idx]
    (String.
     (read-nonempty-bytes buffer idx (.size bt))))

  (rewind-write [bt buffer value]
    ;; TODO assert
    (.writeBytes buffer (.getBytes value))
    (zero-fill-till-end buffer (count value) size))
  (rewind-read [bt buffer]
    (String.
     (read-nonempty-bytes buffer (.size bt))))


  Object
  (toString [_]
    (str "StringType (" size ")")))

;;
;; Comples types
;;

(deftype EnumType [item-type mappings reverse-mappings]
  BuffyType
  (size [_] (size item-type))

  (write [bt buffer idx value]
    (.write item-type buffer idx (get mappings value)))
  (read [bt buffer idx]
    (let [intermediate (.read item-type buffer idx)]
      (get reverse-mappings intermediate)))

  (rewind-write [bt buffer value]
    (.rewind-write item-type buffer (get mappings value)))
  (rewind-read [bt buffer]
    (let [intermediate (.rewind-read item-type buffer)]
      (get reverse-mappings intermediate))))

(deftype CompositeType [types]
  BuffyType
  (size [_] (reduce + (map #(.size %) types)))
  (write [bt buffer idx values]
    (assert (= (count types) (count values)) "Number of values passed to composite should equal number of types")
    (doseq [[type value position] (map vector types values (positions types))]
      (.write type buffer (+ idx position) value)))
  (read [bt buffer idx]
    (into []
          (for [[type position] (map vector types (positions types))]
            (.read type buffer (+ idx position)))))

  (rewind-write [bt buffer values]
    (assert (= (count types) (count values)) "Number of values passed to composite should equal number of types")
    (doseq [[type value] (map vector types values)]
      (.rewind-write type buffer value)))
  (rewind-read [bt buffer]
    (into []
          (for [type types]
            (.rewind-read type buffer))))

  Object
  (toString [_]
    (str "CompositeType: (" (clojure.string/join ", " (mapv str types)) ")")))

(deftype RepeatedType [type times]
  BuffyType
  (size [_] (* (.size type) times))

  (write [bt buffer idx values]
    (doseq [[value position] (map vector values (positions (repeat (count values) type)))]
      (.write type buffer (+ idx position) value))
    ;; buffer idx size expected-size
    (zero-fill-till-end buffer idx (dec (* (.size type) (-> values count inc))) (.size bt)))

  (read [bt buffer idx]
    (into []
          (for [position (positions (repeat times type))]
            (.read type buffer (+ idx position)))))


  (rewind-write [bt buffer values]
    (doseq [value values]
      (.rewind-write type buffer value))
    ;; buffer idx size expected-size
    (zero-fill-till-end buffer (dec (* (.size type) (-> values count inc))) (.size bt)))

  (rewind-read [bt buffer]
    (into []
          (for [idx (range times)]
            (.rewind-read type buffer)))))

(deftype UUIDType []
  BuffyType
  (size [_] 16)

  (read [by buffer idx]
    (let [msb (.getLong buffer idx)
          lsb (.getLong buffer (+ idx 8))]
      (java.util.UUID. lsb msb)))

  (rewind-write [bt buffer value]
    (.writeLong buffer (.getMostSignificantBits value))
    (.writeLong buffer (.getLeastSignificantBits value)))

  (write [bt buffer idx value]
    (.setLong buffer idx       (.getMostSignificantBits value))
    (.setLong buffer (+ 8 idx) (.getLeastSignificantBits value)))

  Object
  (toString [_]
    "UUIDType"))


;;
;; Constructors
;;

(def bit-type     (memoize (fn [length] (BitType. length))))
(def int32-type   (memoize #(Int32Type.)))
(def boolean-type (memoize #(BooleanType.)))
(def byte-type    (memoize #(ByteType.)))
(def short-type   (memoize #(ShortType.)))
(def medium-type  (memoize #(MediumType.)))
;; TODO: Add unsigned types: byte, int, medium, short
(def float-type   (memoize #(FloatType.)))
(def double-type  (memoize #(DoubleType.)))
(def long-type    (memoize #(LongType.)))
(def string-type  (memoize (fn [length] (StringType. length))))
(def bytes-type   (memoize (fn [length] (BytesType. length))))
(def uuid-type    (memoize #(UUIDType.)))

(def bit-map-type
  (memoize
   (fn [& fields]
     (assert (even? (count fields)) "requires an even number of forms")
     (let [inner-fields (mapv vec (partition 2 fields))
           inner-bits-count (reduce + (map second inner-fields))]
       (assert (zero? (mod inner-bits-count 8))
               "sum of all bit-map field should be multiple of 8")
       (BitMapType. (bit-type (/ inner-bits-count 8)) inner-fields)))))

(defn composite-type
  [& types]
  (CompositeType. types))

(defn repeated-type
  [type times]
  (RepeatedType. type times))

(defn enum-type
  [type mappings]
  (let [reverse-mappings (zipmap (vals mappings) (keys mappings))]
    (EnumType. type mappings reverse-mappings)))
