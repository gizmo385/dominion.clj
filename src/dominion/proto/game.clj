(ns dominion.proto.game
  (:require [dominion.proto.display :as dm]))

(defprotocol GameManager
  (update-game! [this state-update-fn args])
  (current-player [this])
  (get-state [this])
  (render [this render-fn args]))

(defrecord SimpleGameManager [game-state-atom display-manager]
  GameManager
  (update-game! [this state-update-fn args]
    (let [old-state (get-state this)
          new-state (apply state-update-fn old-state args)]
      (reset! (:game-state-atom this) new-state)))
  (current-player [this]
    (some->> this :game-state-atom deref :player-order first))
  (get-state [this]
    (-> this :game-state-atom deref))
  (render [this render-fn args]
    (apply render-fn (:display-manager this) this args)))
