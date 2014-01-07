(ns clojurewerkz.buffy.frames
  (:refer-clojure :exclude [read])
  (:require [clojurewerkz.buffy.types :refer :all]
            [clojurewerkz.buffy.util :refer :all]
            [clojurewerkz.buffy.types.protocols :refer :all]))

(declare composite-frame)
(declare encoding-size*)
(declare decoding-size*)

(defn- make-encoder-bindings
  [triplets]
  (reduce (fn [acc [a1 a2 _]] (-> acc (conj a1) (conj a2)))
          []
          triplets))

(defn- types
  [triplets]
  (map second triplets))

(defn third [[_ _ x]] x)

(defmacro frame-encoder
  [binding & triplets]
  (let [triplets (partition 3 triplets)]
    `(fn* ~binding
          (let ~(make-encoder-bindings triplets)
            [(apply composite-frame [~@(types triplets)])
             ~(mapv third triplets)]))))


(defn make-decoder-bindings
  ([tuples]
     (make-decoder-bindings tuples (map first tuples)))
  ([tuples types]
     (let [[[f s] & more] tuples]
       (if (not (empty? more))
         (list 'let [f s] (make-decoder-bindings more types))
         (list 'let [f s] (cons 'composite-frame types))))))

(defmacro frame-decoder
  [binding & tuples]
  (let [buffer (first binding)
        tuples (partition 2 tuples)]
    `(fn* ~binding
          ~(make-decoder-bindings tuples))))

(defprotocol Frame
  (encoder-for [this value])
  (decoder-for [this buffer idx])

  (encoding-size [this value])
  (decoding-size [this buffer idx]))

(deftype FrameType [encoder decoder value-formatter]
  BuffyType
  (size [_] (throw (RuntimeException. "Can't determine size of the frame")))

  (encoder-for [this value]
    (encoder value))

  (decoder-for [this buffer idx]
    (decoder buffer idx))

  (write [bt buffer idx value]
    (let [[type values] (encoder value)]
      (.write type
              buffer
              idx
              values)))
  (read [by buffer idx]
    (value-formatter
     (.read (decoder buffer idx)
            buffer
            idx)))

  Frame
  (encoding-size [_ value]
    (->> value
        encoder
        (partition 2)
        (map (fn [[a b]] (encoding-size* a b)))
        (reduce +)))

  (decoding-size [_ buffer idx]
    (decoding-size* (decoder buffer idx) buffer idx))

  Object
  (toString [_]
    "Frame"))

(defn composite-frame-sizes
  [subframes values]
  (->> values
       (interleave subframes)
       (partition 2)
       (map (fn [[t v]] (encoding-size* t v)))))

(deftype CompositeFrame [subframes]
  BuffyType
  (size [_] (throw (RuntimeException. "Can't determine size of composite frame")))

  (write [_ buffer idx values]
    (loop [[[frame value] & more] (partition 2 (interleave subframes values))
           [size & more-sizes]    (composite-frame-sizes subframes values)
           idx                    idx]
      (write frame buffer idx value)
      (when (not (empty? more))
        (recur more more-sizes (+ idx size))))
    buffer)

  (read [_ buffer idx]
    (loop [[frame & more] subframes
           idx            idx
           acc            []]
      (if (nil? frame)
        acc
        (let [size  (decoding-size* frame buffer idx)
              value (read frame buffer idx)]
          (recur more (+ idx size) (conj acc value))))))

  Frame
  (encoding-size [_ values]
    (reduce + (composite-frame-sizes subframes values)))

  (decoding-size [_ buffer idx]
    (loop [size 0
           idx  idx
           [frame & more] subframes]
      (if (nil? frame)
        size
        (let [frame-size (decoding-size* frame buffer idx)]
          (recur (+ size frame-size) (+ idx frame-size) more)))))

  Object
  (toString [_]
    (str "Composite Frame of:" (vec subframes))))

(defn encoding-size*
  [frame-or-type value]
  (if (instance? (:on-interface Frame) frame-or-type)
    (encoding-size frame-or-type value)
    (size frame-or-type)))

(defn decoding-size*
  [frame-or-type buffer idx]
  (if (instance? (:on-interface Frame) frame-or-type)
    (decoding-size frame-or-type buffer idx)
    (size frame-or-type)))

(defn frame-type
  ([encoder decoder]
     (FrameType. encoder decoder identity))
  ([encoder decoder value-formatter]
     (FrameType. encoder decoder value-formatter)))

(defn composite-frame
  [& subframes]
  (CompositeFrame. subframes))

(defn repeated-frame
  [subframe count]
  (CompositeFrame. (repeat count subframe)))
