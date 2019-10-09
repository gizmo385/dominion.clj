(ns dominion.ui.cli
  (:require [dominion.game :as g]
            [dominion.proto.game :as gm]
            [clojure.string :as s]
            [clojure.pprint :as pp]
            [clojure.set :refer [rename-keys]])
  (:import [dominion.proto.display DisplayManager]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Output helper functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn interleave-lines [lines spacer]
  (lazy-seq
    (let [first-lines (map first lines)]
      (when (some identity first-lines)
        (cons (str (s/join spacer first-lines) \newline)
              (interleave-lines
                (map rest lines) spacer))))))

(defn interleave-multiline-strings [strings spacer]
  (let [lines (map #(s/split %1 #"\n") strings)]
    (s/join (interleave-lines lines spacer))))

(defn map->table-string [m]
  (with-out-str
    (pp/print-table m)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Terminal command implementation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti handle-command
  (fn [command args] command))

(defmethod handle-command :pass [& _]
  (gm/update-game g/next-turn)
  true)

(defmethod handle-command :exit [& _]
  true)

(defmethod handle-command :default
  [command args]
  (if-let [command-name (first command)]
    (printf "Unknown command, please try again: %s\n" command-name)
    (printf "Invalid input!\n")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Per-section render functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn render-supply [supply]
  (->> (for [[card-key cards] supply
             :let [card (first cards)]]
         {"Card Name" (:title card)
          "Description" (:description card)
          "Cost" (:cost card)
          "Card Types" (s/join " * " (map name (:card-types card)))
          "Number Available" (count cards)})
       (sort-by #(get % "Cost"))
        map->table-string))

(defn render-player-hand [player]
  (->> (for [card (:hand player)]
         {"Card Name" (:title card)
          "Description" (:description card)
          "Card Types" (->> card
                            :card-types
                            (map name)
                            (map s/capitalize)
                            (s/join " * "))})
       (sort-by #(get % "Card Name"))
       (map->table-string)))

(defn render-player-discard [player]
  (->> (for [card (:discard player)]
         {"Card Name" (:title card)
          "Description" (:description card)
          "Card Types" (->> card
                            :card-types
                            (map name)
                            (map s/capitalize)
                            (s/join " * "))})
       (sort-by #(get % "Card Name"))
       (map->table-string)))

(defn render-player [player]
  (let [discard (render-player-discard player)
        hand (render-player-hand player)]
    (interleave-multiline-strings [hand discard] \tab)))

(defn game-state->terminal
  ([] (game-state->terminal (gm/get-state)))
  ([game-state]
   (let [current-player (-> game-state :player-order first)
         player (get-in game-state [:players current-player])]
     (str
       "Current Supply"
       (render-supply (:supply game-state))
       \newline
       \newline
       (render-player player)))))

(defn available-actions
  ([pov-player] (available-actions pov-player (gm/get-state)))
  ([pov-player game-state]
   (if (= pov-player (-> game-state :player-order first))
     {:buy    "Buy [Card Name]"
      :claim  "Claim [Card Name]"
      :play   "Play [Card Name]"
      :select "Select [Card Name]"
      :stage  "Stage [Card Name]"
      :pass   "Pass"
      :exit   "Exit"}
     {:reveal "Reveal [Card Name]"
      :exit   "Exit"})))

(defn get-command-input []
  (let [split-input (s/split (read-line) #"\s+")]
    (cons
      (-> split-input first s/lower-case keyword)
      (rest split-input))))

(defn present-ui
  ([pov-player]
   (present-ui pov-player (gm/get-state)))

  ([pov-player game-state]
   (print (game-state->terminal game-state))
   (let [actions (available-actions pov-player game-state)]
     (println "Available actions:")
     (println "------------------")
     (->> actions vals (s/join \newline) println)
     (println "------------------")
     (loop []
       (let [[command args] (get-command-input)]
         (if (get actions command)
           (handle-command command args)
           (do
             (println "Invalid command!")
             (recur))))
       )
     true)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Display manager implementation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defrecord TerminalDisplayManager [pov-player]
  DisplayManager
  (render-game-state [this]
    (present-ui (:pov-player this)))
  (render-error [this error]
    (binding [*out* *err*]
      (println (.getMessage error)))))

(comment
  (require '[dominion.player :as p])
  (require '[dominion.card :as c])
  (require '[dominion.expansions.base :as base])
  (require '[dominion.proto.state :as sm])

  (let [supply (g/build-supply 10 10 base/available-cards)
        gs (g/build-game {:p1 p/base-player :p2 p/base-player} supply)
        gsm (-> gs atom sm/->AtomGameStateManager)]
    (await (gm/set-managers! :state gsm))
    ))
