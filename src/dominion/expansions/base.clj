(ns dominion.expansions.base
  (:require
    [clojure.spec.alpha :as s]
    [dominion.card :as c]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Basic cards
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def estate (c/new-card "Estate" "" 2 [::c/victory] :vp (c/raw-vp 1)))
(def duchy (c/new-card "Duchy" "" 5 [::c/victory] :vp (c/raw-vp 3)))
(def province (c/new-card "Province" "" 8 [::c/victory] :vp (c/raw-vp 3)))

(def garden
  (c/new-card
    "Gardens" "Worth 1VP per 10 cards (rounded down)." 4 [::c/victory] :vp (c/card-count-vp 10 1)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Kingdom Cards
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def smithy
  (c/new-card "Smithy" "+3 cards" 4 [::c/action] :actions [(c/draw-cards-action 3)]))
