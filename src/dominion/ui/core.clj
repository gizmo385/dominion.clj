(ns dominion.ui.core
  (:require [seesaw.core :as ss]
            [seesaw.border :as b]
            [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Building panels for cards
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn build-border
  [& {:keys [size color] :or {size 4 color :black}}]
  (b/compound-border size (b/line-border :color color :thickness size)))

(defn build-card-panel
  [card & {:keys [tip]}]
  (ss/vertical-panel
    :border (build-border)
    :tip tip
    :items [(->> card :title ss/label)
            (->> card :description ss/label)
            (->> card :card-types (map name) (string/join ", ") ss/label)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Building panels for the player's displays
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn build-multi-cards-panel [cards]
  (->> cards
       (map build-card-panel)
       (ss/grid-panel :columns 5 :hgap 10 :vgap 20 :items)))

(defn build-player-discard-panel [player]
  (let [discard-panel (->> player :discard build-multi-cards-panel)]
    (ss/vertical-panel :items [(ss/label "Hand") discard-panel])))

(defn build-player-hand-panel [player]
  (let [hand-panel (->> player :hand build-multi-cards-panel)]
    (ss/vertical-panel :items [(ss/label "Hand") hand-panel])))

(defn build-player-panel [player]
  (let [hand (build-player-hand-panel player)
        discard (build-player-discard-panel player)
        separator (ss/separator :orientation :vertical)]
    (ss/horizontal-panel :items [hand separator discard])))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Building the panels for the kingdom and the stashes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn build-supply-stack-panel
  "Given a single card stack in the supply, builds the panel representing that card
  stack in the user interface"
  [cards]
  (let [tooltip (format "%d cards available..." (count cards))]
    (build-card-panel (first cards) :tip tooltip)))

(defn build-supply-panel
  "Given a game state, builds the panel for the game states current supply."
  [game-state]
  (some->> game-state
           :supply
           vals
           (map build-supply-stack-panel)
           (ss/grid-panel :rows 3 :columns 7 :hgap 20 :vgap 20 :items)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Building the menus for the game
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Building the overall game frame
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn build-game-panel
  [game-state pov-player]
  (let [supply-panel (build-supply-panel game-state)
        player-panel (build-player-panel (get-in game-state [:players pov-player]))]
    (ss/border-panel
      :hgap 30
      :vgap 30
      :center supply-panel
      :south player-panel)))

(defn build-frame [game-state pov-player]
  (->> (build-game-panel game-state pov-player)
       (ss/frame :title "Dominion" :content)))

(comment
  (require '[dominion.game :as g])
  (require '[dominion.player :as p])
  (require '[dominion.card :as c])
  (require '[dominion.expansions.base :as base])
  (-> (g/build-game {:p1 p/base-player
                     :p2 p/base-player}
                    {})
      (update-in [:players :p1 :discard] conj c/province)
      (build-frame :p1)
      ss/pack!
      ss/show!)

  )
