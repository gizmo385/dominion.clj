(ns dominion.actions
  "Helper functions for defining actions on cards in Dominion, such as:

    1. Card draw actions
    2. + Money/Buys/Actions cards
    3. A keyword based builder function for the above action types."
  (:require [dominion.proto.game :as gp]
            [dominion.proto.display :as dm]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Modeling different kinds of actions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti perform-action
  (fn perform-action-dispatch [action game-manager]
    (::action-type action)))

(defmethod perform-action ::simple-action
  [{::keys [game-state-modifier]} game-manager]
  (game-state-modifier))

(defmethod perform-action ::conditional-action
  [{::keys [game-state-predicate game-state-modifier]} game-manager]
  (when (game-state-predicate (gp/get-state game-manager))
    (game-state-modifier game-manager)))

(defmethod perform-action ::compound-action
  [{::keys [actions-to-perform]} game-manager]
  (doseq [a actions-to-perform]
    (perform-action a game-manager)))

(defmethod perform-action ::user-prompt-action
  [{::keys [user-prompt responses-fn responses-list callback]} game-manager]
  (let [responses (if (some? responses-fn)
                    (responses-fn game-manager)
                    responses-list)]
    (gp/render
      game-manager
      dm/render-user-prompt
      [user-prompt
       responses
       callback])))

(defmethod perform-action ::optional-action
  [{::keys [action-description yes-action]} game-manager]
  (gp/render
    game-manager
    dm/render-user-prompt
    [action-description
     ["Yes" "No"]
     (fn [r] (when (= r "Yes") (yes-action)))]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Card draw actions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn draw-cards-for-player [number-to-draw {:keys [hand deck discard] :as player}]
  (if (> number-to-draw (count deck))
    (let [drawable-now (count deck)
          drawable-later (- number-to-draw drawable-now)
          shuffled-discard (shuffle discard)]
      (assoc player
             :hand (concat hand deck (take drawable-later shuffled-discard))
             :deck (drop drawable-later shuffled-discard)
             :discard []))
    (assoc player
           :hand (concat hand (take number-to-draw deck))
           :deck (drop number-to-draw deck))))

(defn draw-cards [number-to-draw game-state player-key]
  (let [player (get-in game-state [:players player-key])
        updated-player (draw-cards-for-player number-to-draw player)]
    (assoc-in game-state [:players player-key] updated-player)))

(defn draw-cards-action [number-to-draw]
  (partial draw-cards number-to-draw))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Turn modification actions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn plus-buys-action [additional-buys]
  (fn [game-state player-key]
    (update-in game-state [:turn  :buys] + additional-buys)))

(defn plus-money-action [additional-money]
  (fn [game-state player-key]
    (update-in game-state [:turn :money] + additional-money)))

(defn plus-actions-action [additional-actions]
  (fn [game-state player-key]
    (update-in game-state [:turn :actions] + additional-actions)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Utility helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn card-in-hand-by-name [gs player card-name]
  (->> gs
       :players
       player
       :hand
       (filter (fn card-name-filter [card]
                 (-> card :title (= card-name))))
       first))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Action building helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn choose-from-hand-action [action]
  {::action-type ::user-prompt-action
   ::user-prompt "Choose a card from your hand"
   ::responses-fn (fn response-fn [game-manager]
                    (let [current-player (gp/current-player game-manager)]
                      (->> (gp/get-state game-manager)
                           :players
                           current-player
                           :hand
                           (map :title))))
   ::callback (fn callback-wrapper [card-name game-manager]
                (let [card (card-in-hand-by-name
                             (gp/get-state game-manager)
                             (gp/current-player game-manager)
                             card-name)]
                  (action game-manager card)))})

(defn attack-opponents [attack-fn]
  (fn [game-state attack-player]
    (->> game-state
         :players
         keys
         (filter (partial not= attack-player))
         (reduce attack-fn game-state))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Generic helper function for building quick action definitions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn build
  "Helper function for easily defining multiple card actions"
  [& {:keys [buys money actions draw]}]
  (cond->   '()
    draw    (conj (draw-cards-action draw))
    money   (conj (plus-money-action money))
    buys    (conj (plus-buys-action buys))
    actions (conj (plus-actions-action actions))))
