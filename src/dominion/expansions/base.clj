(ns dominion.expansions.base
  (:require
    [clojure.spec.alpha :as s]
    [dominion.actions :as a]
    [dominion.card :as c]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Basic cards
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def estate (c/new-card "Estate" "" 2 [::c/victory] :vp (c/raw-vp 1)))
(def duchy (c/new-card "Duchy" "" 5 [::c/victory] :vp (c/raw-vp 3)))
(def province (c/new-card "Province" "" 8 [::c/victory] :vp (c/raw-vp 3)))

(def garden
  (c/new-card
    "Gardens" "Worth 1VP per 10 cards (rounded down)."
    4
    [::c/victory]
    :vp (c/card-count-vp 10 1)))

(def copper (c/new-card "Copper" "" 0 [::c/treasure] :actions (a/build :money 1)))
(def silver (c/new-card "Silver" "" 3 [::c/treasure] :actions (a/build :money 2)))
(def gold (c/new-card "Gold" "" 6 [::c/treasure] :actions (a/build :money 3)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Kingdom Cards
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def smithy
  (c/new-card "Smithy" "+3 cards" 4 [::c/action] :actions (a/build :actions 3)))

(def laboratory
  (c/new-card "Laboratory" "+2 cards, +1 action"
              5
              [::c/action]
              :actions (a/build :draw 2 :actions 1)))

(def village
  (c/new-card "Village" "+1 card, +2 actions"
              3
              [::c/action]
              :actions (a/build :draw 1 :actions 2)))

(def market
  (c/new-card
    "Market"
    "+1 card, +1 action, +1 buy, +1 money"
    5
    [::c/action]
    :actions (a/build :draw 1 :buys 1 :money 1 :actions 1)))

(def festival
  (c/new-card
    "Festival"
    "+2 actions, +1 buy, +2 money"
    5
    [::c/action]
    :actions (a/build :actions 2 :buys 1 :money 2)))


(def available-cards
  [smithy laboratory village market festival])
