(ns dominion.card
  "Defines how cards are structured in Dominion, along with functions to easily define
  them. This namespace also defines a handful of base cards that are required in
  Dominion regardless of which expansion is being used:
    Copper
    Silver
    Gold
    Estate
    Duchy
    Province
    Curse"
  (:require
    [clojure.string :as string]
    [clojure.spec.alpha :as s]
    [dominion.actions :as a]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Victory point implementations
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Card specifications
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def card-types
  #{::treasure ::victory ::action ::attack ::reaction ::duration ::curse})

(s/def ::title string?)
(s/def ::description string?)
(s/def ::key keyword?)
(s/def ::cost (s/and int? (complement neg?)))
(s/def ::card-types (s/+ card-types))

(s/def ::actions (s/nilable (s/+ fn?)))
(s/def ::vp (s/nilable fn?))

(s/def ::card
  (s/keys :req-un [::title ::key ::description ::cost ::card-types ::actions ::vp]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Generic card constructor
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn new-card
  [title description cost types & {:keys [vp actions key]}]
  (let [card-key (if (keyword? key)
                   key
                   (-> title string/lower-case keyword))
        card {:title title
              :key card-key
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Builtin cards used in all games
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def estate (new-card "Estate" "" 2 [::victory] :vp (raw-vp 1)))
(def duchy (new-card "Duchy" "" 5 [::victory] :vp (raw-vp 3)))
(def province (new-card "Province" "" 8 [::victory] :vp (raw-vp 3)))
(def curse (new-card "Curse" "" 0 [::curse] :vp (raw-vp -1)))
(def copper (new-card "Copper" "" 0 [::treasure] :actions (a/build :money 1)))
(def silver (new-card "Silver" "" 3 [::treasure] :actions (a/build :money 2)))
(def gold (new-card "Gold" "" 6 [::treasure] :actions (a/build :money 3)))

(defn builtin-cards
  "Given the number of victory and curse cards to add, builds a map of all of the
  builtin cards that should be added to any game of dominion. Additionally, a value
  can be supplied for the number of treasure cards added to the game."
  [vp-count curse-count & {:keys [treasure-count] :or {treasure-count 100}}]
  {(:key estate) (repeat vp-count estate)
   (:key duchy) (repeat vp-count duchy)
   (:key province) (repeat vp-count province)
   (:key curse) (repeat curse-count curse)
   (:key copper) (repeat treasure-count copper)
   (:key silver) (repeat treasure-count silver)
   (:key gold) (repeat treasure-count gold)})
