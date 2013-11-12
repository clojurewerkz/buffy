(ns clojurewerkz.buffy.types.protocols)

;;
;; Protocol
;;

(defprotocol BuffyType
  (size [bt])
  (write [bt buffer idx value])
  (read [bt buffer idx]))
