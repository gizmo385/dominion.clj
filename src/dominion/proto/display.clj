(ns dominion.proto.display)

(defprotocol DisplayManager
  (render-game-state [this game-manager])
  (render-user-prompt [this game-manager user-prompt responses callback])
  (render-error [this game-manager error]))
