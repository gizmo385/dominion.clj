(ns dominion.proto.state)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; UI State Context variables
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol StateManager
  (get-state* [this])
  (update-state* [this update-fn & args]))

(defrecord AtomGameStateManager [game-state]
  StateManager
  (get-state* [this]
    @(:game-state this))
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

(defn update-state
  "Applies a particular update function "
  [update-fn args]
  (apply update-state* *game-state-manager* update-fn args))
