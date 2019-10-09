(ns dominion.expansions.base
  "Defines cards that were present in the base Dominion game."
  (:require
    [dominion.actions :as a]
    [dominion.card :as c]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Kingdom Cards
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def smithy
  (c/new-card "Smithy" "+3 cards" 4 [::c/action] :actions (a/build :actions 3)))

(def laboratory
  (c/new-card
    "Laboratory"
    "+2 cards, +1 action"
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

(def garden
  (c/new-card
    "Gardens" "Worth 1VP per 10 cards (rounded down)."
    4
    [::c/victory]
    :vp (c/card-count-vp 10 1)))

(def cellar
  (c/new-card
    "Cellar"
    "+1 Action. Discard any number of cards, then draw that many."
    2
    [::c/action]
    :actions [(a/plus-actions-action 1)
              (fn [game-state player-key]
                (let [selected-cards (-> game-state :turn :selected)]
                  (as-> game-state gs
                    (update-in gs [:players player-key :discard] concat selected-cards)
                    (assoc-in gs [:turn :selected] '())
                    (a/draw-cards (count selected-cards) game-state player-key))))]))

(def woodcutter
  (c/new-card
    "Woodcutter"
    "+1 buy, +2 money"
    3
    [::c/action]
    :actions (a/build :money 2 :buy 1)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; TODO: Think about how to generalize 'perform action on every other player' in the
;;; game.clj file as this is a common motif amongst cards in the game.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def witch
  (c/new-card
    "Witch"
    "+2 cards, each other player gains a Curse"
    5
    [::c/action ::c/attack]
    :actions [(a/draw-cards-action 2)
              (a/attack-opponents
                (fn [game-state player]
                  (if (-> game-state :supply :curse count pos?)
                    (-> game-state
                        (update-in [:supply :curse] rest)
                        (update-in [:players player :deck] conj c/curse))
                    game-state)))]))

(def available-cards
  "Cards that are defined in the base game of dominion"
  [smithy laboratory village market festival garden cellar])
