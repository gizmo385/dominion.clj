(ns dominion.expansions.intrigue
  (:require
    [dominion.actions :as a]
    [dominion.card :as c]))

(def duke
  (c/new-card
    "Duke"
    "Worth 1 VP per Duchy you have."
    5
    [::c/victory]
    :vp (fn [player]
          (->> (concat (:hand player)
                       (:deck player)
                       (:discard player))
              (filter (partial = c/duchy))
              count))))

(def harem
  (c/new-card
    "Harem"
    "+2 money, +2 VP"
    6
    [::c/treasure ::c/victory]
    :action (a/build :money 2)
    :vp (c/raw-vp 2)))
