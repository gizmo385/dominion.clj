(ns dominion.proto.game
  (:require
    [dominion.proto.state :as sm]
    [dominion.proto.display :as dm]))

(defprotocol GameManager
  "The Game Manager protocol essentially acts as a bridge between the State Manager
  protocol and the "
  (update-game* [update-fn & args])
  (get-state* [this]))

(def ^:dynamic *game-manager*
  nil)

(defn update-game
  [update-fn & args]
  (apply update-game* *game-manager* update-fn args))

(defn get-state []
  (get-state* *game-manager*))

(defrecord SimpleGameManager []
  GameManager
  (update-game* [this update-fn args]
    (try
      (apply sm/update-state update-fn args)
      (dm/render-game-state)
      (catch Exception e
        (dm/render-error  e))))
  (get-state* [this]
    (sm/get-state)))
