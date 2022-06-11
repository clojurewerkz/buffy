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
  (:require [clojurewerkz.buffy.util :as util]
            [clojurewerkz.buffy.types.protocols :as p])
  (:import [java.util UUID]
           [io.netty.buffer ByteBuf UnpooledByteBufAllocator ByteBufAllocator]))

(set! *warn-on-reflection* true)

;;
;; Primitive types
;;

(deftype Int32Type []
  p/BuffyType
  (size [_] 4)
  (write [bt buffer idx value]
    (.setInt ^ByteBuf buffer idx value))
  (read [by buffer idx]
    (.getInt ^ByteBuf buffer idx))

  (rewind-write [bt buffer value]
    (.writeInt ^ByteBuf buffer value))
  (rewind-read [by buffer]
    (.readInt ^ByteBuf buffer))

  Object
  (toString [_]
    "Int32Type"))

(deftype BooleanType []
  p/BuffyType
  (size [_] 1)
  (write [bt buffer idx value]
    (.setBoolean ^ByteBuf buffer idx value))
  (read [by buffer idx]
    (.getBoolean ^ByteBuf buffer idx))

  (rewind-write [bt buffer value]
    (.writeBoolean ^ByteBuf buffer value))
  (rewind-read [by buffer]
    (.readBoolean ^ByteBuf buffer))


  Object
  (toString [_]
    "BooleanType"))

(deftype ByteType []
  p/BuffyType
  (size [_] 1)
  (write [bt buffer idx value]
    (.setByte ^ByteBuf buffer ^ByteBuf idx value))
  (read [by buffer idx]
    (.getByte ^ByteBuf buffer idx))

  (rewind-write [bt buffer value]
    (.writeByte ^ByteBuf buffer value))
  (rewind-read [by buffer]
    (.readByte ^ByteBuf buffer))

  Object
  (toString [_]
    "ByteType"))

(deftype UnsignedByteType []
  p/BuffyType
  (size [_] 1)
  (write [bt buffer idx value]
    (.setByte ^ByteBuf buffer idx (bit-and 0xFF (short value))))
  (read [by buffer idx]
    (.getUnsignedByte ^ByteBuf buffer idx))

  (rewind-write [bt buffer value]
    (.writeByte ^ByteBuf buffer (bit-and 0xFF (short value))))
  (rewind-read [by buffer]
    (.readUnsignedByte ^ByteBuf buffer))

  Object
  (toString [_]
    "UnsignedByteType"))

(deftype ShortType []
  p/BuffyType
  (size [_] 2)
  (write [bt buffer idx value]
    (.setShort ^ByteBuf buffer idx value))
  (read [by buffer idx]
    (.getShort ^ByteBuf buffer idx))

  (rewind-write [bt buffer value]
    (.writeShort ^ByteBuf buffer value))
  (rewind-read [by buffer]
    (.readShort ^ByteBuf buffer))

  Object
  (toString [_]
    "ShortType"))

(deftype UnsignedShortType []
  p/BuffyType
  (size [_] 2)
  (write [bt buffer idx value]
    (.setShort ^ByteBuf buffer idx (bit-and 0xFFFF (int value))))
  (read [by buffer idx]
    (.getUnsignedShort ^ByteBuf buffer idx))

  (rewind-write [bt buffer value]
    (.writeShort ^ByteBuf buffer (bit-and 0xFFFF (int value))))
  (rewind-read [by buffer]
    (.readUnsignedShort ^ByteBuf buffer))

  Object
  (toString [_]
    "UnsignedShortType"))

(deftype MediumType []
  p/BuffyType
  (size [_] 3)
  (write [bt buffer idx value]
    (.setMedium ^ByteBuf buffer idx value))
  (read [by buffer idx]
    (.getMedium ^ByteBuf buffer idx))

  (rewind-write [bt buffer value]
    (.writeMedium ^ByteBuf buffer value))
  (rewind-read [by buffer]
    (.readMedium ^ByteBuf buffer))

  Object
  (toString [_]
    "MediumType"))

(deftype UnsignedMediumType []
  p/BuffyType
  (size [_] 3)
  (write [bt buffer idx value]
    (.setMedium ^ByteBuf buffer idx (bit-and 0xFFFFFF (int value))))
  (read [by buffer idx]
    (.getUnsignedMedium ^ByteBuf buffer idx))

  (rewind-write [bt buffer value]
    (.writeMedium ^ByteBuf buffer (bit-and 0xFFFFFF (int value))))
  (rewind-read [by buffer]
    (.readUnsignedMedium ^ByteBuf buffer))

  Object
  (toString [_]
    "UnsignedMediumType"))

(deftype UnsignedInt32Type []
  p/BuffyType
  (size [_] 4)
  (write [bt buffer idx value]
    (.setInt ^ByteBuf buffer idx (.intValue (Long. ^long value))))
  (read [by buffer idx]
    (.getUnsignedInt ^ByteBuf buffer idx))

  (rewind-write [bt buffer value]
    (.writeInt ^ByteBuf buffer (.intValue (Long. ^long value))))
  (rewind-read [by buffer]
    (.readUnsignedInt ^ByteBuf buffer))

  Object
  (toString [_]
    "UnsignedInt32Type"))

(deftype BitType [byte-length]
  p/BuffyType
  (size [_] byte-length)
  (write [bt buffer idx value]
    (assert (util/seqable? value) "Bit Field value should be collection")
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
        (.setByte ^ByteBuf buffer idx changed-val))))
  (read [by buffer idx]
    (mapcat identity
            (for [byte-index (range 0 byte-length)]
              (let [idx         (+ idx (- byte-length 1 byte-index))
                    current-val (.getByte ^ByteBuf buffer idx)]
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
  p/BuffyType
  (size [_] (p/size inner-bits))
  (write [bt buffer idx value]
    (let [bit-map-value (bit-map-hash-map->long-value value fields)
          bit-indexes   (reverse (range (* 8 (p/size inner-bits))))]
      (p/write inner-bits buffer idx
               (mapv #(bit-test bit-map-value %) bit-indexes))))

  (read [by buffer idx]
    (bit-map-bits->hash-map (p/read inner-bits buffer idx) fields))


  (rewind-write [bt buffer value]
    (throw (Exception. "Don't know how to rewind-write a bit map type")))
  (rewind-read  [bt buffer]
    (throw (Exception. "Don't know how to rewind-read a bit map type")))

  )

(deftype FloatType []
  p/BuffyType
  (size [_] 4)
  (write [bt buffer idx value]
    (.setFloat ^ByteBuf buffer idx value))
  (read [by buffer idx]
    (.getFloat ^ByteBuf buffer idx))

  (rewind-write [bt buffer value]
    (.writeFloat ^ByteBuf buffer value))
  (rewind-read [by buffer]
    (.readFloat ^ByteBuf buffer))

  Object
  (toString [_]
    "FloatType"))

(deftype DoubleType []
  p/BuffyType
  (size [_] 8)
  (write [bt buffer idx value]
    (.setDouble ^ByteBuf buffer idx value))
  (read [by buffer idx]
    (.getDouble ^ByteBuf buffer idx))

  (rewind-write [bt buffer value]
    (.writeDouble ^ByteBuf buffer value))
  (rewind-read [by buffer]
    (.readDouble ^ByteBuf buffer))

  Object
  (toString [_]
    "DoubleType"))

(deftype LongType []
  p/BuffyType
  (size [_] 8)
  (write [bt buffer idx value]
    (.setLong ^ByteBuf buffer idx value))
  (read [by buffer idx]
    (.getLong ^ByteBuf buffer idx))

  (rewind-write [bt buffer value]
    (.writeLong ^ByteBuf buffer value))
  (rewind-read [by buffer]
    (.readLong ^ByteBuf buffer))

  Object
  (toString [_]
    "LongType"))

(deftype UnsignedLongType []
  p/BuffyType
  (size [_] 8)
  (write [bt buffer idx value]
    (.setLong ^ByteBuf buffer idx (.longValue (bigint value))))
  (read [by buffer idx]
    (let [buf (byte-array 8)]
      (.getBytes ^ByteBuf buffer ^int idx ^bytes buf)
      (bigint (.and (new java.math.BigInteger buf) (.toBigInteger 18446744073709551615N)))))

  (rewind-write [bt buffer value]
    (.writeLong ^ByteBuf buffer value))
  (rewind-read [by buffer]
    (.readLong ^ByteBuf buffer))

  Object
  (toString [_]
    "UnsignedLongType"))

(deftype BytesType [size]
  p/BuffyType
  (size [_] size)
  (write [bt buffer idx value]
    (.setBytes ^ByteBuf buffer ^int idx ^bytes value)
    (util/zero-fill-till-end buffer idx (count value) (p/size bt)))
  (read [bt buffer idx]
    (util/read-nonempty-bytes buffer idx (p/size bt)))

  (rewind-write [bt buffer value]
    (.writeBytes ^ByteBuf buffer ^bytes value)
    (util/zero-fill-till-end buffer (count value) (p/size bt)))
  (rewind-read [bt buffer]
    (util/read-nonempty-bytes buffer (p/size bt)))

  Object
  (toString [_]
    (str "BytesType: (" size ")")))

(deftype StringType [size]
  p/BuffyType
  (size [_] size)
  (write [bt buffer idx value]
    ;; TODO assert
    (.setBytes ^ByteBuf buffer ^int idx (.getBytes ^String value))
    (util/zero-fill-till-end buffer idx (count value) size))
  (read [bt buffer idx]
    (String.
     (util/read-nonempty-bytes buffer idx (p/size bt))))

  (rewind-write [bt buffer value]
    ;; TODO assert
    (.writeBytes ^ByteBuf buffer (.getBytes ^String value))
    (util/zero-fill-till-end buffer (count value) size))
  (rewind-read [bt buffer]
    (String.
     (util/read-nonempty-bytes buffer (p/size bt))))


  Object
  (toString [_]
    (str "StringType (" size ")")))

;;
;; Comples types
;;

(deftype EnumType [item-type mappings reverse-mappings]
  p/BuffyType
  (size [_] (p/size item-type))

  (write [bt buffer idx value]
    (p/write item-type buffer idx (get mappings value)))
  (read [bt buffer idx]
    (let [intermediate (p/read item-type buffer idx)]
      (get reverse-mappings intermediate)))

  (rewind-write [bt buffer value]
    (p/rewind-write item-type buffer (get mappings value)))
  (rewind-read [bt buffer]
    (let [intermediate (p/rewind-read item-type buffer)]
      (get reverse-mappings intermediate))))

(deftype CompositeType [types]
  p/BuffyType
  (size [_] (reduce + (map p/size types)))
  (write [bt buffer idx values]
    (assert (= (count types) (count values)) "Number of values passed to composite should equal number of types")
    (doseq [[type value position] (map vector types values (util/positions types))]
      (p/write type buffer (+ idx position) value)))
  (read [bt buffer idx]
    (into []
          (for [[type position] (map vector types (util/positions types))]
            (p/read type buffer (+ idx position)))))

  (rewind-write [bt buffer values]
    (assert (= (count types) (count values)) "Number of values passed to composite should equal number of types")
    (doseq [[type value] (map vector types values)]
      (p/rewind-write type buffer value)))
  (rewind-read [bt buffer]
    (into []
          (for [type types]
            (p/rewind-read type buffer))))

  Object
  (toString [_]
    (str "CompositeType: (" (clojure.string/join ", " (mapv str types)) ")")))

(deftype RepeatedType [type times]
  p/BuffyType
  (size [_] (* (p/size type) times))

  (write [bt buffer idx values]
    (doseq [[value position] (map vector values (util/positions (repeat (count values) type)))]
      (p/write type buffer (+ idx position) value))
    ;; buffer idx size expected-size
    (util/zero-fill-till-end buffer idx (dec (* (p/size type) (-> values count inc))) (p/size bt)))

  (read [bt buffer idx]
    (into []
          (for [position (util/positions (repeat times type))]
            (p/read type buffer (+ idx position)))))


  (rewind-write [bt buffer values]
    (doseq [value values]
      (p/rewind-write type buffer value))
    ;; buffer idx size expected-size
    (util/zero-fill-till-end buffer (dec (* (p/size type) (-> values count inc))) (p/size bt)))

  (rewind-read [bt buffer]
    (into []
          (for [idx (range times)]
            (p/rewind-read type buffer)))))

(deftype UUIDType []
  p/BuffyType
  (size [_] 16)

  (read [by buffer idx]
    (let [msb (.getLong ^ByteBuf buffer idx)
          lsb (.getLong ^ByteBuf buffer (+ idx 8))]
      (UUID. msb lsb)))

  (write [bt buffer idx value]
    (.setLong ^ByteBuf buffer idx       (.getMostSignificantBits ^UUID value))
    (.setLong ^ByteBuf buffer (+ 8 idx) (.getLeastSignificantBits ^UUID value)))

  (rewind-write [bt buffer value]
    (.writeLong ^ByteBuf buffer (.getMostSignificantBits ^UUID value))
    (.writeLong ^ByteBuf buffer (.getLeastSignificantBits ^UUID value)))

  (rewind-read [by buffer]
    (let [msb (.readLong ^ByteBuf buffer)
          lsb (.readLong ^ByteBuf buffer)]
      (UUID. msb lsb)))

  Object
  (toString [_]
    "UUIDType"))


;;
;; Constructors
;;

(def bit-type     (memoize (fn [length] (BitType. length))))
(def int32-type   (memoize #(Int32Type.)))
(def uint32-type  (memoize #(UnsignedInt32Type.)))
(def boolean-type (memoize #(BooleanType.)))
(def byte-type    (memoize #(ByteType.)))
(def ubyte-type   (memoize #(UnsignedByteType.)))
(def short-type   (memoize #(ShortType.)))
(def ushort-type  (memoize #(UnsignedShortType.)))
(def medium-type  (memoize #(MediumType.)))
(def umedium-type (memoize #(UnsignedMediumType.)))
(def float-type   (memoize #(FloatType.)))
(def double-type  (memoize #(DoubleType.)))
(def long-type    (memoize #(LongType.)))
(def ulong-type   (memoize #(UnsignedLongType.)))
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
