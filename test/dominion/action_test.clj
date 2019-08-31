 (ns dominion.action-test
   (:require [clojure.test :refer :all]
             [dominion.card :refer :all :as c]
             [dominion.actions :refer :all]))

 (deftest turn-modifying-actions-test
  (testing "Card actions which modify turn attributes"
    (let [game-state {:turn {:buys 1 :money 0 :actions 1}}
          plus-buys (plus-buys-action 1)
          plus-actions (plus-actions-action 1)
          plus-money (plus-money-action 1)]
      (is (= (-> game-state (plus-buys :test))
             (update-in game-state [:turn :buys] inc)))
      (is (= (-> game-state (plus-actions :test))
             (update-in game-state [:turn :actions] inc)))
      (is (= (-> game-state (plus-money :test))
             (update-in game-state [:turn :money] inc))))))

 (deftest draw-cards-test
  (testing "Drawing cards smaller than the deck size"
    (let [player {:hand [1 2 3]
                  :deck [4 5]
                  :discard [6 7]}
          drew-one (draw-cards-for-player 1 player)
          drew-two (draw-cards-for-player 2 player)]
      ;; Check resulting deck sizes
      (is (= 1 (-> drew-one :deck count)))
      (is (empty? (:deck drew-two)))

      ;; Check resulting discard sizes
      (is (= (:discard player) (:discard drew-one)))
      (is (= (:discard player) (:discard drew-two)))

      ;; Check resulting hand sizes
      (is (= (-> drew-one :hand count)
             (-> player :hand count inc)))
      (is (= (-> drew-two :hand count)
             (-> player :hand count (+ 2))))))

  (testing "Drawing cards larger than the deck size"
    (let [player {:hand [1 2 3]
                  :deck [1]
                  :discard [6 7]}
          drew-two (draw-cards-for-player 2 player)
          drew-three (draw-cards-for-player 3 player)]
      ;; Ensure the discards are now empty
      (is (-> drew-two :discard empty?))
      (is (-> drew-three :discard empty?))

      ;; Verify resulting deck sizes
      (is (= 1 (count (:deck drew-two))))
      (is (empty? (:deck drew-three))))))
