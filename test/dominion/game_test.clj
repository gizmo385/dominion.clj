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
      (let [post-buy-gs (buy-card test-gs :c1)]
        (is (= 1 (-> post-buy-gs :supply :c1 count)))
        (is (= 1 (-> post-buy-gs :players :p1 :discard count)))
        (is (= (-> post-buy-gs :turn :money)
               (-> test-gs :turn :money (- (:cost test-card)))))
        (is (= (-> post-buy-gs :turn :buys)
               (-> test-gs :turn :buys dec)))))
    (testing "Cannot buy cards without enough money"
      (let [poor-gs (assoc-in test-gs [:turn :money] 0)]
        (is (thrown? Exception (buy-card poor-gs :c1)))))
    (testing "Cannot buy cards without supply"
      (let [empty-supply-gs (assoc-in test-gs [:supply :c1] [])]
        (is (thrown? Exception (buy-card empty-supply-gs :c1)))))
    (testing "Cannot buy cards without any buys"
      (let [no-buys-gs (assoc-in test-gs [:turn :buys] 0)]
        (is (thrown? Exception (buy-card no-buys-gs :c1)))))))

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

(deftest turn-over-conditions
  (let [test-gs (build-game {:p1 p/base-player} {})]
    (testing "Game is not over at start of turn"
      (is (not (turn-over? test-gs))))

    (testing "Test mid-turn zero states"
      (is (not (turn-over? (assoc-in test-gs [:turn :buys] 0))))
      (is (not (turn-over? (assoc-in test-gs [:turn :actions] 0))))
      (is (not (turn-over? (-> test-gs
                               (assoc-in [:turn :buys] 0)
                               (assoc-in [:turn :actions] 0)
                               (assoc-in [:turn :staged-card] c/estate))))))

    (testing "Correctly identifies turn-end state"
      (is (turn-over? (-> test-gs
                          (assoc-in [:turn :buys] 0)
                          (assoc-in [:turn :actions] 0)))))))

(deftest stage-card-testing
  (let [test-gs (build-game {:p1 p/base-player} {})
        staged-card-gs (stage-card test-gs c/copper)]
    (testing "Staging a card works"
      (is (-> test-gs :players :p1 :hand count (= 5)))
      (is (-> test-gs :turn :staged-card nil?))
      (is (-> staged-card-gs :players :p1 :hand count (= 4)))
      (is (-> staged-card-gs :turn :staged-card some?)))

    (testing "Unstaging a card inverts staging a card")))

(deftest select-card-testing
  (let [test-gs (build-game {:p1 p/base-player} {})
        select-one-card-gs (-> test-gs
                               (select-card c/copper))
        select-two-card-gs (-> test-gs
                               (select-card c/copper)
                               (select-card c/copper))]

    (testing "Selecting cards works"
      (is (-> test-gs :players :p1 :hand count (= 5)))
      (is (-> test-gs :turn :selected empty?))
      (is (-> select-one-card-gs :players :p1 :hand count (= 4)))
      (is (-> select-one-card-gs :turn :selected count (= 1)))
      (is (-> select-two-card-gs :players :p1 :hand count (= 3)))
      (is (-> select-two-card-gs :turn :selected count (= 2))))

    (testing "Unselected a card inverts selecting a card")))

(deftest play-card-testing)
