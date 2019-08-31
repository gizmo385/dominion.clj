(ns dominion.player
  (:require
    [clojure.spec.alpha :as s]
    [dominion.card :as c]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Player specifications
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(s/def ::hand (s/* ::c/card))
(s/def ::discard (s/* ::c/card))
(s/def ::deck (s/* ::c/card))

(s/def ::player
  (s/keys :req-un [::hand ::discard ::deck]))

(def base-player
  {:hand []
   :deck []
   :discard []})
