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
