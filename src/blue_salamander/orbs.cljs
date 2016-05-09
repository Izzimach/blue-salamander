(ns blue-salamander.orbs
  (:require [thi.ng.geom.core :as g]
            [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]))

(def orb-bling-sound (js/Audio. "assets/Pickup_Coin2.wav"))

(defn gen-orb [space-coord]
  {:position space-coord
   :rotation 0
   :radius 0.7
   :mesh "assets/salamander.json"})

(defn gen-orbs [state space-coords]
  (assoc state :orbs (map gen-orb space-coords)))

(defn rotate-orb [orb]
  (assoc orb :rotation (+ (:rotation orb) 0.1)))

(defn update-orbs [state]
  (update-in state [:orbs] #(map rotate-orb %)))

(defn collide-player-with-orbs [state]
  (let [playerpos (:player/position state)
        orb-get-distance 1
        get-orb #(< (g/dist playerpos %) orb-get-distance)]
    ;; remove any orbs close enough to get
    (assoc state :orbs (into [] (remove #(get-orb (:position %)) (:orbs state))))))

(defn gather-orbs-with-audio [state]
  (let [new-state (collide-player-with-orbs state)
        old-orb-count (count (:orbs state))
        new-orb-count (count (:orbs new-state))]
    ;; if the orb count changed, play the "picked up an orb" sound
    (if (not= old-orb-count new-orb-count)
      (.play orb-bling-sound))
    ;; if no orbs are left, switch to the completion menu
    (if (and (= 0 new-orb-count)
             (= (:gamemode state) :playing))
      (assoc new-state :gamemode :victorymenu)
      new-state)))
