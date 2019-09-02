(ns dominion.utils)

(defn rotate-list
  "Given a list `l`, rotates that list such that the first `delta` elements of `l`
  become the last `delta` elements of `l`. Rotating a list by with a delta equal to it
  length is an identity operation."
  [l & {:keys [delta] :or {delta 1}}]
  (concat (drop delta l) (take delta l)))

(defn map-values
  "Given a function `f` and a map `m`, maps `f` over the values of `m` and returns the
  updated map."
  [f m]
  (into {} (for [[k v] m] [k (f v)])))
