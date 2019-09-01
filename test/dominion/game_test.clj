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
                                :p1
                                :money 2)]
    (testing "Can buy cards that are available"
      (let [post-buy-gs (buy-card test-gs :p1 :c1)]
        (is (= 1 (-> post-buy-gs :supply :c1 count)))
        (is (= 1 (-> post-buy-gs :players :p1 :discard count)))))
    (testing "Cannot buy cards without enough money"
      (let [poor-gs (assoc-in test-gs [:turn :money] 0)]
        (is (thrown? Exception (buy-card poor-gs :p1 :c1)))))
    (testing "Cannot buy cards without supply"
      (let [empty-supply-gs (assoc-in test-gs [:supply :c1] [])]
        (is (thrown? Exception (buy-card empty-supply-gs :p1 :c1)))))
    (testing "Cannot buy cards without any buys"
      (let [no-buys-gs (assoc-in test-gs [:turn :buys] 0)]
        (is (thrown? Exception (buy-card no-buys-gs :p1 :c1)))))))
