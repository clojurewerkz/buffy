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
    "Int32Type"))


(defn frame-type
  ([encoder decoder]
     (FrameType. encoder decoder identity))
  ([encoder decoder value-formatter]
     (FrameType. encoder decoder value-formatter)))
