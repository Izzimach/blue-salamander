(ns blue-salamander.player-movement
  (:require  [blue-salamander.collision :as coll]
             [thi.ng.geom.core :as g]
             [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]))

(defn- revise-sphere-position
  [{:keys [center radius] :as sphere} collisionpos]
  ;; push sphere away from the collision point
  (let [coll-vector (g/- collisionpos center)
        coll-distance (g/mag coll-vector)
        ;; push in the opposite direction of the collision
        ;; by the amount equal to the penetration amount
        push-direction (g/invert (g/normalize coll-vector))
        push-amount (- radius coll-distance)
        new-center (g/+ center (g/* push-direction push-amount))]
    (coll/Sphere. new-center radius)))

(defn- collision-reducer
  [sphere blockdata]
  (let [boxgeom (:geometry blockdata)
        collresult (coll/collide-sphere-cube sphere boxgeom)]
    ;; if no collision, use the current sphere position
    (if (nil? collresult)
      sphere
      ;; if collision, return a revised sphere collision
      (revise-sphere-position sphere collresult))))

(defn collide-sphere-with-boxes
  [old-sphere-center
   new-sphere-center
   sphere-radius
   blocks]
  (let [new-sphere (coll/Sphere. new-sphere-center sphere-radius)]
    ;; check against boxes and revise the sphere position when collisions happen
    (reduce collision-reducer new-sphere blocks)))
