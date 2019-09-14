(ns dominion.game-test
  (:require [clojure.test :refer :all]
            [dominion.card :as c]
            [dominion.player :as p]
            [dominion.game :refer :all]))

(deftest buy-card-testing
  (let [test-card (c/new-card "T" "T" 1 [::c/treasure])
        test-gs (new-game-state {:p1 p/base-player}
                                {:c1 (repeat 2 test-card)}
                                []
                                [:p1]
                                :money 2)]
    (testing "Can buy cards that are available"
      (let [post-buy-gs (buy-card test-gs :p1 :c1)]
        (is (= 1 (-> post-buy-gs :supply :c1 count)))
        (is (= 1 (-> post-buy-gs :players :p1 :discard count)))
        (is (= (-> post-buy-gs :turn :money)
               (-> test-gs :turn :money (- (:cost test-card)))))
        (is (= (-> post-buy-gs :turn :buys)
               (-> test-gs :turn :buys dec)))))
    (testing "Cannot buy cards without enough money"
      (let [poor-gs (assoc-in test-gs [:turn :money] 0)]
        (is (thrown? Exception (buy-card poor-gs :p1 :c1)))))
    (testing "Cannot buy cards without supply"
      (let [empty-supply-gs (assoc-in test-gs [:supply :c1] [])]
        (is (thrown? Exception (buy-card empty-supply-gs :p1 :c1)))))
    (testing "Cannot buy cards without any buys"
      (let [no-buys-gs (assoc-in test-gs [:turn :buys] 0)]
        (is (thrown? Exception (buy-card no-buys-gs :p1 :c1)))))))

(deftest game-over-conditions
  (let [test-gs (build-game {:p1 p/base-player :p2 p/base-player} {})]
    (testing "game-over? returns false for games which have not ended"
      (is (not (game-over? test-gs)))
      (is (not (-> test-gs (assoc-in [:supply (:key c/estate)] '()) game-over?)))
      (is (not (-> test-gs
                   (assoc-in [:supply (:key c/estate)] '())
                   (assoc-in [:supply (:key c/duchy)] '())
                   game-over?))))

    (testing "Running out of provinces in the supply ends the game"
      (is (-> test-gs (assoc-in [:supply (:key c/province)] '()) game-over?)))

    (testing "Running out of three non-province cards in the supply ends the game"
      (is (-> test-gs
              (assoc-in [:supply (:key c/copper)] '())
              (assoc-in [:supply (:key c/silver)] '())
              (assoc-in [:supply (:key c/gold)] '())
              game-over?)))))

