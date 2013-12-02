(ns clojurewerkz.buffy.types.protocols
  (:refer-clojure :exclude [read]))

;;
;; Protocol
;;

(defprotocol BuffyType
  (size [bt])
  (write [bt buffer idx value])
  (read [bt buffer idx]))
