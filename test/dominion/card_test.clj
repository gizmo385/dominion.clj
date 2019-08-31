(ns dominion.card-test
  (:require [clojure.test :refer :all]
            [dominion.card :refer :all :as c]
            [dominion.player :refer :all]))

(deftest new-card-validation
  (testing "Card function validation"
    (are [c] (thrown? Exception c)
         (new-card "T" "T" 1 [::c/action] :vp "test") ;; Invalid VP function
         (new-card "T" "T" 1 [::c/not-a-real-card-type]) ;; Invalid card type
         (new-card "T" "T" -1 [::c/action]) ;; Invalid cost
         (new-card 1 "T" 1 [::c/action]) ;; Invalid name
         (new-card "T" -2 1 [::c/action]) ;; Invalid description
         (new-card "T" "T" 1 [::c/action] :actions ["test"]) ;; Invalid action
         (new-card "T" "T" 1 [::c/action] :actions [nil])))) ;; Invalid action

(deftest victory-point-test
  (testing "Empty/nil hands evaluate to 0"
    (is (zero? (count-victory-points {:hand [] :deck [] :discard []})))
    (is (zero? (count-victory-points {:hand [(new-card "T" "T" 0 [::c/treasure])]
                                      :deck []
                                      :discard []}))))
  (testing "Simple Raw Value Card"
    (is (= 9 (count-victory-points {:hand [(new-card "T" "T" 0 [::c/action] :vp (raw-vp 1))
                                           (new-card "T" "T" 0 [::c/action] :vp (raw-vp 1))]
                                    :deck [(new-card "T" "T" 0 [::c/action] :vp (raw-vp 7))]
                                    :discard []}))
        (= 14 (count-victory-points {:hand []
                                     :deck [(new-card "T" "T" 0 [::c/action] :vp (raw-vp 7))]
                                     :discard [(new-card "T" "T" 0 [::c/action] :vp (raw-vp 7))]}))))
  (testing "Card Count Value Card"
    (is (zero? (count-victory-points {:hand [(new-card "T" "T" 0 [::c/action] :vp (card-count-vp 10 1))]
                                      :deck (repeat 8 (new-card "T" "T" 0 [::c/action]))
                                      :discard []})))
    (is (= 1 (count-victory-points {:hand [(new-card "T" "T" 0 [::c/action] :vp (card-count-vp 10 1))]
                                    :deck (repeat 9 (new-card "T" "T" 0 [::c/action]))
                                    :discard []})))
    (is (= 2 (count-victory-points {:hand (repeat
                                            2
                                            (new-card "T" "T" 0 [::c/action] :vp (card-count-vp 10 1)))
                                    :deck (repeat 9 (new-card "T" "T" 0 [::c/action]))
                                    :discard []})))))
