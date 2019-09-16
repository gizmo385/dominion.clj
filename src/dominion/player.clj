(ns dominion.player
  (:require
    [clojure.spec.alpha :as s]
    [dominion.card :as c]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Player specifications
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(s/def ::hand (s/* ::c/card))
(s/def ::discard (s/* ::c/card))
(s/def ::deck (s/* ::c/card))

(s/def ::player
  (s/keys :req-un [::hand ::discard ::deck]))

(def base-player
  {:hand []
   :deck []
   :discard []})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; TODO: How to handle turn-blocking actions, such as an attack from another player that
; requires all other players to discard down to 3 cards in their hand.
;
; Can all of these cards be satisfied by having the action mark the player with a
; predicate that determines whether or not the player's turn can proceed?
;
; How to handle attacks where each attack requires the player to discard a set number of
; cards N? Predicates would have to be combined in some way so that 3 different "player
; has discarded 2 cards" predicates would result in a check that a total of 6 cards
; had been discarded.
;
; Perhaps a protocol that defines 3 different functions?
;   (1) A function which runs a predicate determining if the condition has been met
;   (2) A function to temporarily put cards in escrow the were involved in the check
;   (3) A function that undoes the function in (2)
;
; Is a protocol like that sustainable? Would all of player turn blockers be checkable in
; this kind of way?
;
; There is also the possibility of simply blocking the play of any attack cards until
; all players in the game have resolved any currently pending attacks. That has the
; downside of interrupting turn flow and giving a single player the ability to
; potentially delay the game indefinitely, which is unfun.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
