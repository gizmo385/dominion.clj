(ns dominion.proto.state)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; UI State Context variables
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol StateManager
  (get-state [this])
  (update-state [this update-fn & args]))

(defrecord AtomGameStateManager [game-state]
  StateManager
  (get-state [this]
    @(:game-state this))
  (update-state [this update-fn & args]
    (let [current-state (get-state this)]
      (printf "From %s: Calling %s against %s\n" this update-fn args)
      (apply swap! (:game-state this) update-fn current-state args))))
