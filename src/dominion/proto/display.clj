(ns dominion.proto.display)

(defprotocol DisplayManager
  (render-game-state [this])
  (render-error [this error]))
