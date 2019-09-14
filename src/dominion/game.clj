(ns dominion.game
  (:require
    [clojure.spec.alpha :as s]
    [dominion.actions :as a]
    [dominion.card :as c]
    [dominion.utils :as u]
    [dominion.player :as p]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Game specifications
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(s/def ::buys (s/and int? (complement neg?)))
(s/def ::actions (s/and int? (complement neg?)))
(s/def ::money (s/and int? (complement neg?)))
(s/def ::played (s/* ::c/card))
(s/def ::selected (s/* ::c/card))
(s/def ::staged-card (s/nilable ::c/card))
(s/def ::turn (s/keys :req-un [::buys ::actions ::money ::played ::selected]))

(s/def ::players (s/map-of keyword? ::p/player))
(s/def ::supply (s/map-of keyword? (s/* ::c/card)))
(s/def ::trash (s/* ::c/card))
(s/def ::player-order (s/+ keyword?))

(s/def ::game-state
  (s/keys :req-un [::players ::supply ::trash ::player-order ::turn]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Useful constants for starting games and progressing through turns
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def default-turn
  "This is the default values for the turn attributes on any given new-turn."
  {:buys 1 :actions 1 :money 0 :played '() :selected '() :staged-card nil})


(def player-starting-deck
  "Starting deck for a player, which consists of 3 estates and 7 copper."
  (concat (repeat 3 c/estate) (repeat 7 c/copper)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Helper/utility functions used in defining and updating game states
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- add-builtin-cards
  "Given a supply and a player count, adds the correct number of builtin treasure,
  curse, and victory cards to the supply."
  [supply player-count]
  (let [required-cards (case player-count
                         1 (c/builtin-cards 1 1 :treasure-count 1)
                         2 (c/builtin-cards 8 10)
                         3 (c/builtin-cards 12 20)
                         4 (c/builtin-cards 12 30))]
    (merge supply required-cards)))

(defn- setup-player-cards
  "Given a player, gives them the player starting deck, shuffles it, and then draws 5
  cards out of that deck to start the game with"
  [{:keys [hand deck discard] :as player}]
  (as-> player p
    (assoc p :deck (shuffle player-starting-deck))
    (a/draw-cards-for-player 5 p)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Helper/utility functions used in defining and updating game states
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn new-game-state
  "Create a new game state, supplying information about the current players, the
  supply, and the trash. Additionally, the current turn can be modified by supplying
  different values for the amount of buys/actions/money that the current player has."
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
  [supply-size card-count kingdom-cards]
  (->> kingdom-cards
       (map (partial repeat card-count))
       (take supply-size)
       (zipmap (map :key kingdom-cards))))

(defn build-game
  "Given a list of players and a card set to use as the supply, builds an initial
  game state to build upon for the rest of the game."
  [players supply]
  (let [player-order (-> players keys shuffle)
        players-with-decks (u/map-values setup-player-cards players)
        supply-with-builtin-cards (add-builtin-cards supply (count players))]
    (new-game-state players-with-decks supply-with-builtin-cards [] player-order)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Turn progression
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn next-turn [gs]
  (let [players (:players gs)
        current-player (->> gs :player-order first (get players))
        turn (:turn gs)]
    (as-> gs gs
      ;; Rotate the player order
      (update gs :player-order u/rotate-list)
      ;; Move the played cards to the discard pile
      (update-in gs [:players current-player :discard] concat (:played turn))
      ;; Reset the turn counters so the next player has the correct number of buys,
      ;; actions, and money.
      (assoc gs :turn default-turn)
      ;; Move the current player's hand into their discard
      (update-in gs [:players current-player :discard] concat (:hand current-player))
      ;; Empty out the current player's hand
      (assoc-in gs [:players current-player :hand] '())
      ;; Draw 5 new cards for the current player
      (a/draw-cards 5 gs current-player))))

(defn game-over?
  "Given a game state, returns whether or not the game is over. In Dominion, a game
  ends for one of two reasons:
    1: There are zero provinces left in the supply
    2: There are three separate supply stacks with zero cards left."
  [game-state]
  (let [supply-counts (->> game-state :supply (u/map-values count))
        province-count (get supply-counts (:key c/province))
        empty-supplies (filter (fn [[card card-count]] (zero? card-count))
                               supply-counts)]
    (or (zero? province-count)
        (->> empty-supplies count (= 3)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Game state modification functions for actions taken during a game
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- evaluate-actions
  "Given a game state, the current player, and a series of actions, evaluates those
  actions against the current game state, updating the game state and returning back
  the new state of the game."
  [game-state player-key actions]
  (reduce (fn [gs a] (a gs player-key)) game-state actions))

(defn deselect-card
  "Deselects a card, moving it from the 'selected' state of the turn back to the
  player's hand"
  [game-state player-key card]
  (-> game-state
      (update-in [:players player-key :hand] conj card)
      (update-in [:turn :selected] #(u/remove-once #{card} %1))))

(defn select-card
   "Selects a card, moving it from the players hand to the selected state in the
   current turn. Card selection is leveraged by cards that have a pending state."
  [game-state player-key card]
  (-> game-state
      (update-in [:players player-key :hand] #(u/remove-once #{card} %1))
      (update-in [:turn :selected] conj card)))

(defn play-card
  "Evaluate all of the possible results of a card being played in a game."
  [game-state player-key card]
  (-> game-state
      ;; Evaluate any actions present on the card
      (evaluate-actions player-key (:actions card))
      ;; Remove the card from the player's hand
      (update-in [:players player-key :hand] #(u/remove-once #{card} %1))
      ;; Move the card to the :played section of the turn
      (update-in [:turn :played] conj card)))

(defn unstage-card
  "'Unstage' the currently staged card, if present."
  [game-state player-key]
  (if-let [staged-card (-> game-state :turn :staged-card)]
    (-> game-state
        (update-in [:players player-key :hand] conj staged-card)
        (assoc-in [:turn :staged-card] nil))
    game-state))

(defn stage-card
  "'Stage' a card allowing it to be later evaluated against yet-to-be-selected cards
  from the players hand."
  [game-state player-key card]
  (-> game-state
      (update-in [:players player-key :hand] #(u/remove-once #{card} %1))
      (assoc-in [:turn :staged-card] card)))

(defn evaluate-staged-card
  "Evaluates the currently staged card for the turn, if one is present."
  [game-state player-key]
  (if-let [staged-card (-> game-state :turn :staged-card)]
    (-> game-state
        (assoc-in [:turn :staged-card] nil)
        (play-card player-key staged-card))
    game-state))

(defn buy-card
  "Attempt to purchase a card from the supply for a particular player"
  [game-state player-key card-keyword]
  (let [available-money (-> game-state :turn :money)
        available-buys (-> game-state :turn :buys)
        card-supply (-> game-state :supply (get card-keyword))
        card (first card-supply)]
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
