(ns dominion.ui.core
  (:require
    [dominion.game :as g]
    [seesaw.core :as ss]
    [seesaw.border :as b]
    [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Open Questions/Todo List
;
; TODO:
;   * Styling of the card UI
;   * Implementing action selection
;   * Hooking the UI up to the model in the background
;   * Display other players in the game
;   * Display the currently played section (regardless of who is playing)
;
; Questions:
;   1. How to handle managing the game state throughout the UI? Should the UI be
;     created and managed inside some sort of context object?
;
;   2. How to handle accessing the game state throughout the UI? When the UI needs to
;     perform some sort of game state update, where should it get it from?
;
;   3. A dynamically bound context atomic variable could potentially solve 1/2, but
;     where would that be initially set?
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; UI State Context variables
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol GameStateManager
  (get-state* [this])
  (update-state* [this update-fn & args]))

(defrecord AtomGameStateManager [game-state]
  GameStateManager
  (get-state* [this] @(:game-state this))
  (update-state* [this update-fn & args]
    (let [current-state (get-state* this)]
      (apply swap! (:game-state this) update-fn current-state args))))


(def ^:dynamic *game-state-manager*
  "The game state manager is the way that any updates to the game state will be
  performed in the context of the UI. It is used to abstract away the state management
  from the UI code. Essentially, the UI will dispatch out state updates to the state
  manager, and it will be the responsibility of the state manager to actually perform
  those updates.

  This means that you could easily have different state implementations for different
  purposes. For example, state could be managed as a local atom for the purposes of
  single-player testing, but instead be managed by some sort of synchronized network
  state manager for multiplayer games connected to a centralized server."
  nil)

 (defn get-state []
  (get-state* *game-state-manager*))

(defn update-state [update-fn & args]
  (apply update-state* *game-state-manager* update-fn args))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; UI Helper functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn dimension [h w]
  (java.awt.Dimension. h w))

(defn build-border
  [& {:keys [size color] :or {size 4 color :black}}]
  (b/compound-border size (b/line-border :color color :thickness size)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Building panels for cards
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn build-card-panel
  [card & {:keys [tip on-click]}]
  (let [card-title (->> card :title ss/label)
        card-description (->> card :description ss/label)
        card-types (->> card
                        :card-types
                        (map name)
                        (string/join " * ")
                        ss/label)
        opts {:border (build-border)
              :tip tip
              :size (dimension 50 125)
              :north card-title
              :center card-description
              :south card-types}
        opts (cond-> opts
               tip (assoc :tip tip)
               on-click (assoc :listen [:mouse-clicked on-click]))]
    (apply ss/border-panel (interleave (keys opts) (vals opts)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Building panels for the player's displays
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
 (defn hand-selection-dialog
  [card]
  (let [options [(ss/button
                   :text "Play"
                   :listen [:action #(ss/return-from-dialog % :ok)])
                 (ss/button
                   :text "Select"
                   #_#_:listen [:action (update-state g/buy-card nil card)])
                 (ss/button
                   :text "Stage"
                   #_#_:listen [:action (update-state g/buy-card nil card)])]]
    (ss/dialog :content "What would you like to do?"
               :options options)))

(defn build-player-discard-panel [player]
  (let [discard-panel (->> player
                           :discard
                           (map build-card-panel)
                           (ss/grid-panel :columns 5 :hgap 10 :vgap 20 :items))]
    (ss/vertical-panel :items [(ss/label "Discard") discard-panel])))

(defn build-player-hand-panel [player]
  (let [hand-cards (:hand player)
        hand-panel  (->> (for [card hand-cards]
                           (let [action (fn [e]
                                          (-> (hand-selection-dialog (first card))
                                              ss/pack!
                                              ss/show!))]
                             (build-card-panel card :on-click action)))
                         (ss/grid-panel :columns 5 :hgap 10 :vgap 20 :items))]
    (ss/vertical-panel :items [(ss/label "Hand") hand-panel])))

(defn build-player-panel [player]
  (let [hand (build-player-hand-panel player)
        discard (build-player-discard-panel player)
        separator (ss/separator :orientation :vertical)]
    (ss/horizontal-panel :items [hand separator discard])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Building the panels for the kingdom and the stashes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn supply-selection-dialog
  [card]
  (let [options [(ss/button
                   :text "Cancel"
                   :listen [:action #(ss/return-from-dialog % :ok)])
                 (ss/button
                   :text "Buy"
                   #_#_:listen [:action (update-state g/buy-card nil card)])
                 (ss/button
                   :text "Claim"
                   #_#_:listen [:action (update-state g/buy-card nil card)])]]
    (ss/dialog :content "What would you like to do?"
               :options options)))

(defn build-supply-stack-panel
  "Given a single card stack in the supply, builds the panel representing that card
  stack in the user interface"
  [cards]
  (let [show-dialog (fn [e]
                      (-> (supply-selection-dialog (first cards))
                          ss/pack!
                          ss/show!))
        tooltip (format "%d cards available..." (count cards))]
    (build-card-panel (first cards)
                      :tip tooltip
                      :on-click show-dialog)))

(defn build-supply-panel
  "Given a game state, builds the panel for the game states current supply."
  []
  (let [supply-cards (some->> (get-state) :supply vals)
        supply-stacks (map build-supply-stack-panel supply-cards)]
    (ss/grid-panel :rows 3 :columns 7 :hgap 20 :vgap 20 :items supply-stacks)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Building the menus for the game
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Building the overall game frame
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn build-game-panel
  [pov-player]
  (let [supply-panel (build-supply-panel)
        player-panel (-> (get-state)
                         (get-in [:players pov-player])
                         build-player-panel)]
    (ss/border-panel
      :hgap 30
      :vgap 30
      :center supply-panel
      :south player-panel)))

(defn build-frame [game-state-manager pov-player]
  (binding [*game-state-manager* game-state-manager]
    (let [game-panel (build-game-panel pov-player)]
      (ss/frame :title "Dominion" :content game-panel))))

(comment
  (require '[dominion.game :as g])
  (require '[dominion.player :as p])
  (require '[dominion.card :as c])
  (require '[dominion.expansions.base :as base])
  (let [supply (g/build-supply 10 10 base/available-cards)
        gs (g/build-game {:p1 p/base-player :p2 p/base-player} supply)]
    (-> gs
        (update-in [:players :p1 :discard] conj c/province)
        atom
        ->AtomGameStateManager
        (build-frame :p1)
        ss/pack!
        ss/show!)))
