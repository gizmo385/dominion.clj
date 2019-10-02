(ns dominion.proto.display)

(defprotocol DisplayManager
  (render-game-state* [this])
  (render-error* [this error]))

(def ^:dynamic *display-manager*
  nil)

(defn render-game-state []
  (render-game-state* *display-manager*))

(defn render-error [error]
  (render-error* *display-manager* error))
