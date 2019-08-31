(ns dominion.card
  (:require [clojure.spec.alpha :as s]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Victory point implementations
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn raw-vp [value]
  (fn [player] value))

(defn card-count-vp [required-cards multiplier]
  (fn [player]
    (let [card-count (+ (-> player :hand count)
                        (-> player :deck count)
                        (-> player :discard count))]
      (int (* multiplier (Math/floor (/ card-count required-cards)))))))

(defn count-victory-points [player]
  (->> (concat (:hand player)
               (:deck player)
               (:discard player))
       (map :vp)
       (filter some?)
       (map #(% player))
       (apply +)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Defining card actions, which allow for things such as draw, discard, and the like
; to be handled.
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Card specifications
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def card-types
  #{::treasure ::victory ::action ::attack ::reaction})

(s/def ::title string?)
(s/def ::description string?)
(s/def ::cost (s/and int? (complement neg?)))
(s/def ::card-types (s/+ card-types))

(s/def ::actions (s/nilable (s/+ fn?)))
(s/def ::vp (s/nilable fn?))

(s/def ::card (s/keys :req-un [::title ::description ::cost ::card-types ::actions ::vp]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Generic card constructor
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn new-card
  [title description cost types & {:keys [vp actions]}]
  (let [card {:title title
              :description description
              :cost cost
              :card-types types
              :vp vp
              :actions actions}]
    (if (s/valid? ::card card)
      (s/conform ::card card)
      (throw (ex-info "Invalid card created!"
                      {:explain (s/explain ::card card)
                       :card card})))))

(comment
  (new-card "Test" "Test" -2 [::treasure])
    )
