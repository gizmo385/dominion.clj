(ns dominion.game
  (:require
    [clojure.spec.alpha :as s]
    [dominion.card :as c]
    [dominion.player :as p]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Player specifications
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(s/def ::players (s/map-of keyword? ::p/player))
(s/def ::kingdom (s/map-of keyword? (s/* ::c/card)))
(s/def ::trash (s/* ::c/card))

(s/def ::game-state (s/keys :req-un [::players ::kingdom ::trash]))

(defn new-game-state [players kingdom trash]
  (let [gs {:players players
            :kingdom kingdom
            :trash trash}]
    (if (s/valid? ::game-state gs)
      (s/conform ::game-state gs)
      (throw (ex-info "Invalid game state!"
                      {:explain (s/explain ::game-state gs)
                       :game-state gs})))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Player specifications
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- evaluate-actions
  [game-state player-key actions]
  (reduce (fn [gs a] (a gs player-key)) game-state actions))

(defn play-card [game-state player-key card]
  (-> game-state
      (evaluate-actions player-key (:actions card))))
