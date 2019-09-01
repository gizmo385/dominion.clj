(ns dominion.game
  (:require
    [clojure.spec.alpha :as s]
    [dominion.card :as c]
    [dominion.player :as p]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Game specifications
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::buys (s/and int? (complement neg?)))
(s/def ::actions (s/and int? (complement neg?)))
(s/def ::money (s/and int? (complement neg?)))
(s/def ::turn (s/keys :req-un [::buys ::actions ::money]))

(s/def ::players (s/map-of keyword? ::p/player))
(s/def ::supply (s/map-of keyword? (s/* ::c/card)))
(s/def ::trash (s/* ::c/card))
(s/def ::player-order (s/+ keyword?))

(s/def ::game-state
  (s/keys :req-un [::players ::supply ::trash ::player-order ::turn]))


(def default-turn
  "This is the default values for the turn attributes on any given new-turn"
  {:buys 1 :actions 1 :money 0})

(defn new-game-state
  "Create a new game state, supplying information about the current players, the supply, and the
  trash. Additionally, the current turn can be modified by supplying different values for the
  amount of buys/actions/money that the current player has."
  [players supply trash player-order & {:keys [buys actions money] :as turn-info}]
  (let [turn (merge default-turn turn-info)
        gs {:players players
            :supply supply
            :trash trash
            :player-order player-order
            :turn turn}]
    (if (s/valid? ::game-state gs)
      (s/conform ::game-state gs)
      (throw (ex-info "Invalid game state!"
                      {:explain (s/explain ::game-state gs)
                       :game-state gs})))))


(defn build-supply
  "Given a number of supply decks to build, a size for each supply deck, and a series
  of card sets; builds a final supply map with a randomly selected supply.

  A card set should be a map where the keys are keywords (shorthand card name) and the
  values are the cards themselves."
  [supply-size card-count & kingdom-card-sets]
  (->> kingdom-card-sets
       (apply merge)
       (into [])
       (shuffle)
       (take supply-size)
       (map (fn [[card-key card]]
              [card-key (repeat card-count card)]))
       (into {})))

(defn build-game
  "Given a list of players and a card set to use as the supply, builds an initial
  game state to build upon for the rest of the game."
  [players supply]
  (let [player-order (-> players keys shuffle)]
    (new-game-state players supply [] player-order)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Playing cards
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- evaluate-actions
  "Given a game state, the current player, and a series of actions, evaluates those actions against
  the current game state, updating the game state and returning back the new state of the game."
  [game-state player-key actions]
  (reduce (fn [gs a] (a gs player-key)) game-state actions))

(defn play-card
  "Evaluate all of the possible results of a card being played in a game"
  [game-state player-key card]
  (-> game-state
      (evaluate-actions player-key (:actions card))))

(defn buy-card
  "Attempt to purchase a card from the supply for a particular player"
  [game-state player-key card-keyword]
  (let [available-money (-> game-state :turn :money)
        available-buys (-> game-state :turn :buys)
        card-supply (-> game-state :supply (get card-keyword))
        card (first card-supply)]
    (map println [available-money available-buys card-supply card])
    (cond
      (not (pos? available-buys))
      (throw (ex-info "The player has no more buys left!" {:gs game-state}))

      (empty? card-supply)
      (throw (ex-info "No more cards of that type in the supply!" {:gs game-state}))

      (< available-money (:cost card))
      (throw (ex-info "The player cannot afford this card!" {:gs game-state}))

      :else
      (-> game-state
          (update-in [:players player-key :discard] conj card)
          (update-in [:turn :money] - (:cost card))
          (update-in [:turn :buys] dec)
          (update-in [:supply card-keyword] rest)))))
