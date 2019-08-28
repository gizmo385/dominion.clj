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
(s/def ::money int?)
(s/def ::actions int?)
(s/def ::buys int?)

(s/def ::player
  (s/keys :req-un [::hand ::discard ::deck ::buys ::actions ::money]))

(def base-player
  {:hand []
   :deck []
   :discard []
   :money 0
   :buys 1
   :actions 1})
