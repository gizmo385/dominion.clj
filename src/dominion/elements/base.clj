(ns dominion.elements.base
  (:require
    [clojure.spec.alpha :as s]
    [dominion.card :as c]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Basic cards
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def estate (c/new-card "Estate" "" 2 :victory :vp (c/raw-vp 1)))
(def duchy (c/new-card "Duchy" "" 5 :victory :vp (c/raw-vp 3)))
(def province (c/new-card "Province" "" 8 :victory :vp (c/raw-vp 3)))
(def copper (c/new-card "Copper" "" 0 :treasure :actions [(c/plus-money-action 1)]))
(def silver (c/new-card "Silver" "" 3 :treasure :actions [(c/plus-money-action 2)]))
(def gold (c/new-card "Gold" "" 6 :treasure :actions [(c/plus-money-action 3)]))

(def garden
  (c/new-card
    "Gardens" "Worth 1VP per 10 cards (rounded down)." 4 :victory :vp (c/card-count-vp 10 1)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Kingdom Cards
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def smithy
  (c/new-card "Smithy" "+3 cards" 4 :action :actions [(c/draw-cards-action 3)]))

(def laboratory
  (c/new-card "Laboratory" "+2 cards, +1 action" 5 :action :actions [(c/draw-cards-action 2)
                                                           (c/plus-actions-action 1)]))

(def village
  (c/new-card "Village" "+1 card, +2 actions" 3 :action :actions [(c/draw-cards-action 1)
                                                                  (c/plus-actions-action 2)]))

(def market
  (c/new-card
    "Market"
    "+1 card, +1 action, +1 buy, +1 money"
    5
    :action
    :actions [(c/draw-cards-action 1)
              (c/plus-buys-action 1)
              (c/plus-money-action 1)
              (c/plus-actions-action 1)]))

(def festival
  (c/new-card
    "Festival"
    "+2 actions, +1 buy, +2 money"
    5
    :action
    :actions [(c/plus-actions-action 2)
              (c/plus-buy-action 1)
              (c/plus-money-action 2)]))


(def kingdom-cards
  [smithy laboratory village market festival])
