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
(defn read-user-line [prompt]
  (print (str prompt " "))
  (flush)
  (read-line))

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
  (fn [game-manager command args] command))

(defmethod handle-command :pass [game-manager command args]
  (gm/update-game! game-manager g/next-turn [])
  true)

(defmethod handle-command :exit [& _]
  true)

(defmethod handle-command :default
  [game-manager command args]
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
  [game-manager]
  (let [game-state (gm/get-state game-manager)
        current-player (-> game-state :player-order first)
        player (get-in game-state [:players current-player])]
    (str
      "Current Supply\n"
      "=============="
      (render-supply (:supply game-state))
      \newline
      \newline
      (render-player player))))

(defn available-actions
  [game-manager pov-player]
  (if (= pov-player (gm/current-player game-manager))
    {:buy    "Buy [Card Name]"
     :claim  "Claim [Card Name]"
     :play   "Play [Card Name]"
     :select "Select [Card Name]"
     :stage  "Stage [Card Name]"
     :pass   "Pass"
     :exit   "Exit"}
    {:reveal "Reveal [Card Name]"
     :exit   "Exit"}))

(defn get-command-input []
  (let [split-input (s/split (read-line) #"\s+")]
    (cons
      (-> split-input first s/lower-case keyword)
      (rest split-input))))

(defn present-ui
  [game-manager pov-player]
  (print (game-state->terminal game-manager))
  (let [actions (available-actions game-manager pov-player)]
    (println "Available actions:")
    (println "------------------")
    (->> actions vals (s/join \newline) println)
    (println "------------------")
    (loop []
      (let [[command args] (get-command-input)]
        (if (get actions command)
          (handle-command game-manager command args)
          (do
            (println "Invalid command!")
            (recur))))
      )
    true))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Display manager implementation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defrecord TerminalDisplayManager [pov-player]
  DisplayManager
  (render-game-state [this game-manager]
    (present-ui game-manager (:pov-player this)))

  (render-user-prompt [this game-manager user-prompt responses callback]
    (let [enumerated-options (zipmap (map (comp str inc) (range)) responses)
          options-str (->> enumerated-options
                          (map (partial s/join ": "))
                          (s/join "\n"))]
      (println options-str)
      (loop [user-response (read-user-line user-prompt)]
        (cond
          ;; Is the response EOL? If so, exit
          (nil? user-response) nil

          ;; Is it a valid response? If so, let's hit the callback
          (get enumerated-options user-response)
          (callback (get enumerated-options user-response) game-manager)

          ;; Otherwise, we'll prompt again
          :else (recur (read-user-line "Please select a valid option: "))))))

  (render-error [this game-manager error]
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
