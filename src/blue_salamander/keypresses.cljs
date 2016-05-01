(ns blue-salamander.keypresses)

;;
;; All this code does is track which keys are currently pressed by listening
;; for keyup/keydown events and putting the currently pressed keys into a
;; set. You pass in an atom that contains the (initially empty set) and
;; as keys are pressed/released their keycodes are swap!'d into and out of the set

;; record keys as something more readable than numbers

(def keypress-map
  {37 :left-arrow
   38 :up-arrow
   39 :right-arrow
   40 :down-arrow})

;; KeyboardEvent.keyCode is deprecated but .key doesn't seem
;; to be supported everywhere

(defn process-key-down [keys-pressed-atom key-event]
  (let [key (.-keyCode key-event)]
    (swap! keys-pressed-atom #(conj % (get keypress-map key)))
    #_(js/console.log (str "key down: " key))))

(defn process-key-up [keys-pressed-atom key-event]
  (let [key (.-keyCode key-event)]
    (swap! keys-pressed-atom #(disj % (get keypress-map key)))
    #_(js/console.log (str "key up: " key))))

(defn track-keypresses-in-atom
  [keytrack-atom]
  (let []
    (aset js/window "onkeydown" #(process-key-down keytrack-atom %))
    (aset js/window "onkeyup" #(process-key-up keytrack-atom %))))

