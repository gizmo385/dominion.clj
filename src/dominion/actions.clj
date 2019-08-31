(ns dominion.actions)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Card draw actions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
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
; Generic helper function for building quick action definitions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn build
  "Helper function for easily defining multiple card actions"
  [& {:keys [buys money actions draw]}]
  (->> [(if buys (plus-buys-action buys))
        (if money (plus-money-action money))
        (if draw (draw-cards-action draw))
        (if actions (plus-actions-action actions))]
       (filter some?)))
