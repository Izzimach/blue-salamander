(ns blue-salamander.blockmaze
  (:require [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]))


;;
;; generate a standard maze made up of n * n blocks
;;

;;
;; tiles are maps that contain keys for each direction (n,e,s,w) and
;; values that are true if you can travel in the direction specified
;; in the key. for example:
;;
;; {:n true :e true :s false :w false :used true}
;;
;; is a tile with openings to the north and the east.
;;
;; The 'used' key is true if the tile is attached to the maze. All tiles
;; start out unused except for the start tile. The maze is built by
;; iteratively connecting used tiles to adjacent unused tiles.
;;

(defn find-used-tile [mazetiles]
  (rand-nth (filter (fn [[[x y] data]] (:used data)) mazetiles)))

(defn mark-tile-used [mazetiles tile-coord]
  {:pre [(vector? tile-coord)
         (not (nil? (get mazetiles tile-coord nil)))]}
  (update-in mazetiles [tile-coord] #(assoc % :used true)))

(defn pick-adjacent-tile [[tx ty] maze-size]
  (let [candidate-tiles [[:w [(- tx 1) ty]]
                         [:e [(+ tx 1) ty]]
                         [:n [tx (- ty 1)]]
                         [:s [tx (+ ty 1)]]]
        is-legal-tile (fn [[direction-keyword [x y]]]
                        (and (>= x 0)
                             (>= y 0)
                             (< x maze-size)
                             (< y maze-size)))
        legal-tiles (filter is-legal-tile candidate-tiles)]
    (rand-nth legal-tiles)))

(defn opposite-dir [dir]
  (get {:w :e
        :e :w
        :n :s
        :s :n} dir))

(defn gen-empty-maze [maze-size]
  (let [initial-tile-state {:used false :n false :s false :w false :e false}
        maze-range (range maze-size)]
    (into {} (for [x maze-range
                   y maze-range]
               [[x y] initial-tile-state]))))

(defn try-to-add-branch [maze-tiles maze-size]
  (let [;; pick a tile that's already part of the maze to branch from
        branch-tile (find-used-tile maze-tiles)
        [branch-coord branch-data] branch-tile
        ;; try to branch off by picking an adjacent tile random
        [adjacent-dir adjacent-coord] (pick-adjacent-tile branch-coord maze-size)
        adjacent-tile (get maze-tiles adjacent-coord)]
    ;; we need an unused adjacent tile to branch into. If the adjacent tile is
    ;; used, do nothing
    (if (:used adjacent-tile)
        maze-tiles
        (-> maze-tiles
            ;; connect the tiles together. We mark the relvant directions on each
            ;; tile true.
            (assoc-in [branch-coord adjacent-dir] true)
            (assoc-in [adjacent-coord (opposite-dir adjacent-dir)] true)
            ;; the adjacent tile is now connected to the maze, so mark it used
            (assoc-in [adjacent-coord :used] true)))))

(defn gen-maze [maze-size]
  ;; each tile is a map describing which directions are open to adjacent tiles
  ;; and a flag indicating if it has been used or not
  (let [empty-tiles (gen-empty-maze maze-size)
        start-coord [0 0]
        initial-tiles (mark-tile-used empty-tiles start-coord)
        branch-attempts (* maze-size maze-size 20)]
    ;; just keep trying to add tiles to the maze by repeatedly branching off of tiles
    ;; already in the maze.
    (nth
     (iterate #(try-to-add-branch % maze-size) initial-tiles)
     branch-attempts)))

;;
;; convert the maze data into geometry descriptions for rendering and collision
;;

(def unity (vec3 1 1 1))

(defrecord Block [geometry material])

(defn gen-block [x y blocksize]
  (Block. {:center (vec3 x 0 y) :size (vec3 blocksize 1 blocksize)} :lavarock))

(defn tilecoord->spacecoord
  [[tile-x tile-y] maze-size blocksize]
  (let [maze-offset (* (- maze-size 1) blocksize)
        base-x (- (* tile-x blocksize 2) maze-offset)
        base-y (- (* tile-y blocksize 2) maze-offset)]
    (vec3 base-x 0 base-y)))

(defn mazetile->blocks
  [[tile-coord tile-data] maze-size blocksize]
  (let [;;maze-offset (* (- maze-size 1) blocksize)
        ;;base-x (- (* tile-x blocksize 2) maze-offset)
        ;;base-y (- (* tile-y blocksize 2) maze-offset)
        [base-x _ base-y] (tilecoord->spacecoord tile-coord maze-size blocksize)
        east-x (+ base-x blocksize)
        south-y (+ base-y blocksize)
        east-open? (:e tile-data)
        south-open? (:s tile-data)
        se-block (gen-block east-x south-y blocksize)
        east-block (if (:e tile-data)
                               []
                               [(gen-block east-x base-y blocksize)])
        south-block (if (:s tile-data)
                      []
                      [(gen-block base-x south-y blocksize)])]
    (concat [se-block] east-block south-block)))

(defn mazedata->blocks [mazedata maze-size blocksize]
  (mapcat #(mazetile->blocks % maze-size blocksize) mazedata))

;;
;; mazedata->blocks generates some of the edge walls but not the
;; ones in the north & west sides. Instead of adding special-case code
;; into mazetile->blocks we generate them manually here.
;;

(defn gen-edge-walls [maze-size blocksize]
  (let [scalevec (vec3 blocksize blocksize blocksize)
        maze-offset (* maze-size blocksize)
        maze-min (- maze-offset)
        maze-max maze-offset
        block-at (fn [x y] (gen-block x y blocksize))]
    (mapcat (fn [x] (let [bx (- (* x blocksize) maze-max)]
                      [(block-at bx maze-min)
                       #_(block-at bx maze-max)
                       (block-at maze-min bx)
                       #_(block-at maze-max bx)]))
            (range (+ 1 (* maze-size 2))))))

;;
;; returns lazy seq of randomly chosen spots in the maze
;;

(defn placement-spots-seq [maze maze-size blocksize place-count]
  (as-> maze $
    (keys $)
    (shuffle $)
    (map #(tilecoord->spacecoord % maze-size blocksize) $)))

;;
;; build up a maze of blocks
;;


(defn blocks-for-maze [mazedata size blocksize]
  (let [totalsize (* size blocksize 2)
        floor    (Block. {:center (vec3 0 -2 0) :size (vec3 totalsize 3 totalsize)} :lavarock)
        edges (gen-edge-walls size blocksize)
        inner-blocks (mazedata->blocks mazedata size blocksize)]
    (concat [floor]
            edges
            inner-blocks)))
