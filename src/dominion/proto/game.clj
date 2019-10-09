(ns dominion.proto.game
  (:require
    [dominion.proto.state :as sm]
    [dominion.proto.display :as dm]))


(def game-agent
  (agent {:display nil
          :state nil}
         :error-handler (fn [a error] (restart-agent a @a))))

(defn update-game-agent
  "Convinience function that wraps `send` calls to an agent to pull out and update a
  particular key"
  [a k update-fn & args]
  (send a (fn [agent-state]
            (let [new-value (apply update-fn (get agent-state k) args)]
              (assoc agent-state k new-value)))))

(defn update-game
  [state-update-fn & args]
  (as-> game-agent ga
    (apply update-game-agent ga :state sm/update-state state-update-fn args)
    (update-game-agent ga :display dm/render-game-state)))

(defn get-state []
  (some-> @game-agent
          (get :state)
          (sm/get-state)))

(defn set-managers! [& {:keys [display state]}]
  (let [managers (cond-> []
                   display (concat [:display display])
                   state (concat [:state state]))]
    (apply send game-agent assoc managers)))
