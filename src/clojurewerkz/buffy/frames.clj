(ns clojurewerkz.buffy.frames
  (:refer-clojure :exclude [read])
  (:require [clojurewerkz.buffy.types :refer :all]
            [clojurewerkz.buffy.util :refer :all]
            [clojurewerkz.buffy.types.protocols :refer :all]))

(defn- make-encoder-bindings
  [triplets]
  (reduce (fn [acc [a1 a2 _]] (-> acc (conj a1) (conj a2)))
          []
          triplets))

(defn- types
  [triplets]
  (map second triplets))

(defn third [[_ _ x]] x)

(defmacro defframeencoder
  [binding & triplets]
  (let [triplets (partition 3 triplets)]
    `(fn* ~binding
         (let ~(make-encoder-bindings triplets)
           [(apply composite-type [~@(types triplets)])
            ~(mapv third triplets)]))))


(defn make-decoder-bindings
  ([tuples]
     (make-decoder-bindings tuples (map first tuples)))
  ([tuples types]
     (let [[[f s] & more] tuples]
       (if (not (empty? more))
         (list 'let [f s] (make-decoder-bindings more types))
         (list 'let [f s] (cons 'composite-type types))))))

(defmacro defframedecoder
  [binding & tuples]
  (let [buffer (first binding)
        tuples (partition 2 tuples)]
    `(fn* ~binding
          ~(make-decoder-bindings tuples))))

(defprotocol Frame
  (encoder [this value])
  (decoder [this value])
  (encoding-size [this value])
  (decoding-size [this buffer idx]))

(deftype FrameType [encoder decoder value-formatter]
  BuffyType
  (size [_] (throw (RuntimeException. "Can't determine size of the frame")))

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
    (-> value encoder first size))

  (decoding-size [_ buffer idx]
    (size (decoder buffer idx)))

  Object
  (toString [_]
    "Frame"))

(deftype CompositeFrame [subframes]
  BuffyType
  (size [_] (throw (RuntimeException. "Can't determine size of composite frame")))

  (write [_ buffer idx values]
    (let [sizes  (->> (interleave subframes values)
                      (partition 2)
                      (map (fn [[t v]] (encoding-size t v)))
                      vec)]
      (loop [[[frame value] & more] (partition 2 (interleave subframes values))
             [size & more-sizes] sizes
             idx           0]
        (write frame buffer idx value)
        (when (not (empty? more))
          (recur more sizes (+ idx size))))
      buffer)
    )
  (read [_ buffer idx]
    (loop [[frame & more] subframes
           idx            0
           acc            []]
      (if (nil? frame)
        acc
        (let [size  (decoding-size frame buffer idx)
              value (read frame buffer idx)]
          (recur more (+ idx size) (conj acc value)))))

    )

  Frame
  (encoding-size [_ values]
    (reduce + (map (fn [[a b]] (encoding-size a b)) (partition 2 (interleave subframes (map identity values))))))

  (decoding-size [_ buffer idx]
    (loop [size 0
           idx  idx
           [frame & more] subframes]
      (if (nil? frame)
        size
        (let [frame-size (decoding-size frame buffer idx)]
          (recur (+ size frame-size) (+ idx frame-size) more))))))

(defn frame-type
  ([encoder decoder]
     (FrameType. encoder decoder identity))
  ([encoder decoder value-formatter]
     (FrameType. encoder decoder value-formatter)))

(defn composite-frame
  [& subframes]
  (CompositeFrame. subframes))
