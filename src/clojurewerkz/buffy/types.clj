(ns clojurewerkz.buffy.types
  (:refer-clojure :exclude [read])
  (:use clojurewerkz.buffy.types.protocols)
  (:require [clojurewerkz.buffy.util :refer :all])
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
    (.getInt buffer idx)))

(deftype BooleanType []
  BuffyType
  (size [_] 1)
  (write [bt buffer idx value]
    (.setBoolean buffer idx value))
  (read [by buffer idx]
    (.getBoolean buffer idx)))

(deftype ByteType []
  BuffyType
  (size [_] 1)
  (write [bt buffer idx value]
    (.setByte buffer idx value))
  (read [by buffer idx]
    (.getByte buffer idx)))

(deftype ShortType []
  BuffyType
  (size [_] 2)
  (write [bt buffer idx value]
    (.setShort buffer idx value))
  (read [by buffer idx]
    (.getShort buffer idx)))

(deftype MediumType []
  BuffyType
  (size [_] 3)
  (write [bt buffer idx value]
    (.setMedium buffer idx value))
  (read [by buffer idx]
    (.getMedium buffer idx)))

(deftype FloatType []
  BuffyType
  (size [_] 4)
  (write [bt buffer idx value]
    (.setFloat buffer idx value))
  (read [by buffer idx]
    (.getFloat buffer idx)))

(deftype LongType []
  BuffyType
  (size [_] 8)
  (write [bt buffer idx value]
    (.setLong buffer idx value))
  (read [by buffer idx]
    (.getLong buffer idx)))

(deftype BytesType [size]
  BuffyType
  (size [_] size)
  (write [bt buffer idx value]
    (.setBytes buffer idx value)
    (zero-fill-till-end buffer idx (count value) (.size bt)))
  (read [bt buffer idx]
    (read-nonempty-bytes buffer idx (.size bt))))

(deftype StringType [size]
  BuffyType
  (size [_] size)
  (write [bt buffer idx value]
    ;; TODO assert
    (.setBytes buffer idx (.getBytes value))
    (zero-fill-till-end buffer idx (count value) size))
  (read [bt buffer idx]
    (String.
     (read-nonempty-bytes buffer idx (.size bt)))))

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
            (.read type buffer (+ idx position))))))

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
            (.read type buffer (+ idx position))))))

;;
;; Constructors
;;


(def int32-type   (memoize #(Int32Type.)))
(def boolean-type (memoize #(BooleanType.)))
(def byte-type    (memoize #(ByteType.)))
(def short-type   (memoize #(ShortType.)))
(def medium-type  (memoize #(MediumType.)))
;; TODO: Add unsigned types: byte, int, medium, short
(def float-type   (memoize #(FloatType.)))
(def long-type    (memoize #(LongType.)))
(def string-type  (memoize (fn [length] (StringType. length))))
(def bytes-type   (memoize (fn [length] (BytesType. length))))

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
