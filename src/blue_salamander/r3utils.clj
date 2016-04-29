(ns blue-salamander.r3utils)

(defmacro vec3 [x# y# z#] `(js/THREE.Vector3. ~x# ~y# ~z#))
