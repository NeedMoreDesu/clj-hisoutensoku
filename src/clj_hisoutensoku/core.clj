(ns clj-hisoutensoku.core
 (:gen-class)
 (:use clj-hisoutensoku.tables)
 (:require clojure.set)
 (:require clojure.java.io))

(def ^:dynamic *delay* 20)

(declare ^:dynamic key-a)
(declare ^:dynamic key-b)
(declare ^:dynamic key-c)
(declare ^:dynamic key-d)
(declare ^:dynamic key-1)
(declare ^:dynamic key-2)
(declare ^:dynamic key-3)
(declare ^:dynamic key-4)
(declare ^:dynamic key-6)
(declare ^:dynamic key-7)
(declare ^:dynamic key-8)
(declare ^:dynamic key-9)
(declare ^:dynamic key-power)
(declare ^:dynamic key-card-choose)
(declare ^:dynamic key-card-use)

(declare ^:dynamic key-old-a)
(declare ^:dynamic key-old-b)
(declare ^:dynamic key-old-c)
(declare ^:dynamic key-old-d)
(declare ^:dynamic key-old-card-choose)
(declare ^:dynamic key-old-card-use)
(declare ^:dynamic key-old-2)
(declare ^:dynamic key-old-6)
(declare ^:dynamic key-old-8)
(declare ^:dynamic key-old-4)


(defmacro jna-call [lib func ret & args]
 "Thanks nakkaya!"
 `(let [library#  (name ~lib)
        function# (com.sun.jna.Function/getFunction library# ~func)]
   (.invoke function# ~ret (to-array [~@args]))))
(defmacro jna-malloc [size]
 "Thanks nakkaya!"
 `(let [buffer# (java.nio.ByteBuffer/allocateDirect ~size)
        pointer# (com.sun.jna.Native/getDirectBufferPointer buffer#)]
   (.order buffer# java.nio.ByteOrder/LITTLE_ENDIAN)
   {:pointer pointer# :buffer buffer#}))

(defn get-current-window-title []
 (let [str-pointer (jna-malloc 512)
       size (jna-call :User32 "GetWindowTextA" Integer
             (jna-call :User32 "GetForegroundWindow" Integer)
             (:pointer str-pointer)
             512)]
  (apply str (map
              (fn [arg] (char (if (< arg 0) (+ arg 256) arg)))
              (repeatedly size #(.get (:buffer str-pointer)))))))

(declare ^:dynamic window-pattern)
(defn touhou-window? []
 (re-find
  (re-pattern window-pattern)
  (get-current-window-title)))

(defn key-down? [key & [before-latest-check]]
 (let [gcs (jna-call :User32 "GetAsyncKeyState" Integer (get vk-key-table key))
       current (not= 0 (bit-and gcs 0x8000))
       before (not= 0 (bit-and gcs 0x1))]
  (if before-latest-check
   (or current before)
   current)))
(defn keys-down? [& keys]
 (every? key-down? keys))

(def robot (java.awt.Robot.))
(def sleep (fn
            ([] (Thread/sleep *delay*))
            ([arg] (Thread/sleep arg))))

(letfn
 [(send-key-press [key]
   (.keyPress robot (robot-key-table key)))]
 (defn press
  ([keys]
   (doall (map send-key-press keys)))
  ([delay keys]
   (let [key (first keys)
         keys (next keys)]
    (if key
     (do
      (send-key-press key)
      (doall
       (map
        (fn [arg]
         (sleep delay)
         (send-key-press arg))
        keys))))))))
(letfn [(send-key-release [key]
         (.keyRelease robot (robot-key-table key)))]
 (defn release
  ([keys]
   (doall (map send-key-release keys)))
  ([delay keys]
   (let [key (first keys)
         keys (next keys)]
    (if key
     (do
      (send-key-release key)
      (doall
       (map
        (fn [arg]
         (sleep delay)
         (send-key-release arg))
        keys))))))))
(defn press-release
 ([keys]
  (press keys)
  (sleep)
  (release keys))
 ([delay keys]
  (press delay keys)
  (sleep delay)
  (release delay keys))
 ([delay1 delay2 keys]
  (press delay1 keys)
  (sleep delay2)
  (release delay1 keys)))

(defn fns
 [& {:keys [key-pressed key-released key-hold]
     :or {key-pressed (fn [])
          key-released (fn [])
          key-hold (fn [])}}]
 (fn [pressed-keys last-sequences sequence & [sequence-failed]]
  (if (not sequence-failed)
   {:last-sequences
    (if (some #(= % sequence) last-sequences)
     (do
      (key-hold)
      last-sequences)
     (do
      (key-pressed)
      (conj last-sequences sequence)))
    :succeed? true}
   {:last-sequences
    (if (some #(= % sequence) last-sequences)
     (do
      (key-released)
      (disj last-sequences sequence))
     last-sequences)
    :succeed?
    false})))
(def ^:dynamic *reset* true)
(defn key-sequence
 [sequence & [delay]]
 (fns
  :key-pressed
  (let [sequence (map (fn [arg] (if (keyword? arg) arg (first arg))) sequence)]
   (if *reset*
    #(do
      (release sequence)
      (sleep *delay*)
      (if delay
       (press delay sequence)
       (press sequence)))
    #(press sequence)))
  :key-released
  #(doall
    (map
     (fn [arg]
      (if (keyword? arg)
       (release [arg])
       (if (some (fn [arg]
                  (if (coll? arg)
                   (and
                    (some keys-down? (first arg))
                    (not (some keys-down? (second arg))))
                   (keys-down? arg)))
            (second arg)) ; the only fn that directly checks key state.
                          ; Hope that this wont make bugs.
        nil
        (release [(first arg)]))))
     sequence))))
(defn all-matches
 [& matches]
 (fn [pressed-keys last-sequences sequence & [sequence-failed]]
  (reduce
   (fn [{:keys [last-sequences succeed?]}
        [keys function]]
    (let [new-sequence
          (vec (concat sequence keys))
          seq-failed?
          (not (clojure.set/subset? (set keys) pressed-keys))
          {new-last-sequences :last-sequences
           new-succeed? :succeed?}
          (function
           pressed-keys
           last-sequences
           new-sequence
           (or sequence-failed seq-failed?))]
     (zipmap
      [:last-sequences
       :succeed?]
      [new-last-sequences
       (or succeed? new-succeed?)])))
   {:last-sequences last-sequences
    :succeed? false}
   (partition 2 matches))))

(defn first-match
 [& matches]
 (fn [pressed-keys last-sequences sequence & [sequence-failed]]
  (reduce
   (fn [{:keys [last-sequences matched succeed?]}
        [keys function]]
    (let [new-sequence
          (vec (concat sequence keys))
          seq-failed?
          (not (clojure.set/subset? (set keys) pressed-keys))
          {new-last-sequences :last-sequences
           new-succeed? :succeed?}
          (function
           pressed-keys
           last-sequences
           new-sequence
           (or matched seq-failed?))]
     (zipmap
      [:last-sequences
       :matched
       :succeed?]
      [new-last-sequences
       (or matched new-succeed?)
       (or succeed? new-succeed?)])))
   {:last-sequences last-sequences
    :matched sequence-failed
    :succeed? false}
   (partition 2 matches))))

(def fns-623b (fns
               :key-pressed
               (fn []
                (release [key-old-8 key-old-4 key-old-2 key-old-6 key-old-a key-old-b key-old-c key-old-d])
                (sleep)
                (press-release *delay* [key-old-6])
                (press-release *delay* [key-old-2 key-old-6])
                (press [key-old-b])
                (sleep))
               :key-released
               #(release [key-old-b])))
(def fns-623c (fns
               :key-pressed
               (fn []
                (release [key-old-8 key-old-4 key-old-2 key-old-6 key-old-a key-old-b key-old-c key-old-d])
                (sleep)
                (press-release *delay* [key-old-6])
                (press-release *delay* [key-old-2 key-old-6])
                (press [key-old-c])
                (sleep))
               :key-released
               #(release [key-old-c])))
(def fns-421b (fns
               :key-pressed
               (fn []
                (release [key-old-8 key-old-4 key-old-2 key-old-6 key-old-a key-old-b key-old-c key-old-d])
                (sleep)
                (press-release *delay* [key-old-4])
                (press-release *delay* [key-old-2 key-old-4])
                (press [key-old-b])
                (sleep))
               :key-released
               #(release [key-old-b])))
(def fns-421c (fns
               :key-pressed
               (fn []
                (release [key-old-8 key-old-4 key-old-2 key-old-6 key-old-a key-old-b key-old-c key-old-d])
                (sleep)
                (press-release *delay* [key-old-4])
                (press-release *delay* [key-old-2 key-old-4])
                (press [key-old-c])
                (sleep))
               :key-released
               #(release [key-old-c])))
(def fns-236b (fns
               :key-pressed
               (fn []
                (release [key-old-8 key-old-4 key-old-2 key-old-6 key-old-a key-old-b key-old-c key-old-d])
                (sleep)
                (press-release *delay* [key-old-2 key-old-6])
                (press [key-old-b])
                (sleep))
               :key-released
               #(release [key-old-b])))
(def fns-236c (fns
               :key-pressed
               (fn []
                (release [key-old-8 key-old-4 key-old-2 key-old-6 key-old-a key-old-b key-old-c key-old-d])
                (sleep)
                (press-release *delay* [key-old-2 key-old-6])
                (press [key-old-c])
                (sleep))
               :key-released
               #(release [key-old-c])))
(def fns-214b (fns
               :key-pressed
               (fn []
                (release [key-old-8 key-old-4 key-old-2 key-old-6 key-old-a key-old-b key-old-c key-old-d])
                (sleep)
                (press-release *delay* [key-old-2 key-old-4])
                (press [key-old-b])
                (sleep))
               :key-released
               #(release [key-old-b])))
(def fns-214c (fns
               :key-pressed
               (fn []
                (release [key-old-8 key-old-4 key-old-2 key-old-6 key-old-a key-old-b key-old-c key-old-d])
                (sleep)
                (press-release *delay* [key-old-2 key-old-4])
                (press [key-old-c])
                (sleep))
               :key-released
               #(release [key-old-c])))
(def fns-22b (fns
              :key-pressed
              (fn []
               (release [key-old-8 key-old-4 key-old-2 key-old-6 key-old-a key-old-b key-old-c key-old-d])
               (sleep)
               (press-release [key-old-2])
               (sleep)
               (press-release [key-old-2])
               (press [key-old-b])
               (sleep))
              :key-released
              #(release [key-old-b])))
(def fns-22c (fns
              :key-pressed
              (fn []
               (release [key-old-8 key-old-4 key-old-2 key-old-6 key-old-a key-old-b key-old-c key-old-d])
               (sleep)
               (press-release [key-old-2])
               (sleep)
               (press-release [key-old-2])
               (press [key-old-c])
               (sleep))
              :key-released
              #(release [key-old-c])))
(defn tk8 [f]
 (fns
  :key-pressed
  #(press [key-old-8])
  :key-hold
  #(do
    (press-release 15 [key-old-8])
    (sleep 15)
    (f)
    (sleep 15))
  :key-released
  #(release [key-old-8])))
(defn tk9 [f]
 (fns
  :key-pressed
  #(press [key-old-6 key-old-8])
  :key-hold
  #(do
    (press-release 0 15 [key-old-6 key-old-8])
    (sleep 15)
    (f)
    (sleep 15))
  :key-released
  #(release [key-old-6 key-old-8])))
(defn tk7 [f]
 (fns
  :key-pressed
  #(press [key-old-4 key-old-8])
  :key-hold
  #(do
    (press-release 0 15 [key-old-4 key-old-8])
    (sleep 15)
    (f)
    (sleep 15))
  :key-released
  #(release [key-old-4 key-old-8])))
(defn tk [f]
 (first-match
  [key-9]
  (tk9 f)
  [key-8]
  (tk8 f)
  [key-7]
  (tk7 f)))
(defn tk-cancel-tree [key]
 (first-match
  [key-6]
  (tk #(press-release 0 15 [key-old-6 key]))
  [key-4]
  (tk #(press-release 0 15 [key-old-4 key]))
  [key-3]
  (tk #(press-release 0 15 [key-old-2 key]))
  [key-2]
  (tk #(press-release 0 15 [key-old-2 key]))
  [key-1]
  (tk #(press-release 0 15 [key-old-2 key]))
  [key-d]
  (tk #(press-release 0 15 [key]))
  [key-8]
  (first-match
   [key-7]
   (tk7 #(press-release 0 15 [key-old-8 key]))
   [key-9]
   (tk9 #(press-release 0 15 [key-old-8 key]))
   []
   (tk8 #(press-release 0 15 [key-old-8 key])))
  [key-7]
  (tk7 #(press-release 0 15 [key]))
  [key-9]
  (tk9 #(press-release 0 15 [key]))
  []
  (key-sequence [key])))

(defn fn29 []
 (release [key-old-4 key-old-2 key-old-6 key-old-8 key-old-d])
 (sleep 15)
 (press [key-old-2 key-old-6])
 (sleep 15)
 (press [key-old-8])
 (sleep 15)
 (release [key-old-8 key-old-2 key-old-6]))
(defn fn28 []
 (release [key-old-4 key-old-2 key-old-6 key-old-8 key-old-d])
 (sleep 15)
 (press [key-old-2])
 (sleep 15)
 (press [key-old-8])
 (sleep 15)
 (release [key-old-8 key-old-2]))
(defn fn27 []
 (release [key-old-4 key-old-2 key-old-6 key-old-8 key-old-d])
 (sleep 15)
 (press [key-old-2 key-old-4])
 (sleep 15)
 (press [key-old-8])
 (sleep 15)
 (release [key-old-8 key-old-2 key-old-4]))
(defn triangle [hj-fn directions]
 (fns
  :key-pressed
  (fn []
   (hj-fn)
   (sleep 15)
   (press directions)
   (sleep 15)
   (press [key-old-d]))
  :key-released
  (fn []
   (hj-fn)
   (hj-fn))))

(def fns-66
 (first-match
  [key-a]
  (fns
   :key-hold
   #(do
     (press [key-old-6]) (sleep 15)
     (release [key-old-6]) (sleep 15)
     (press [key-old-6]) (sleep 15)
     (release [key-old-6]) (sleep 15)
     (press [key-old-a]) (sleep 15)
     (release [key-old-a]) (sleep 15)))
  [key-c]
  (fns
   :key-hold
   #(do
     (press [key-old-6]) (sleep 15)
     (release [key-old-6]) (sleep 15)
     (press [key-old-6]) (sleep 15)
     (release [key-old-6]) (sleep 15)
     (press [key-old-c]) (sleep 15)
     (release [key-old-c]) (sleep 15)))
  []
  (fns
   :key-hold
   #(do
     (press [key-old-6]) (sleep 10)
     (release [key-old-6]) (sleep 10)))))

(def fns-44
 (first-match
  [key-a]
  (fns
   :key-hold
   #(do
     (press [key-old-4]) (sleep 15)
     (release [key-old-4]) (sleep 15)
     (press [key-old-4]) (sleep 15)
     (release [key-old-4]) (sleep 15)
     (press [key-old-a]) (sleep 15)
     (release [key-old-a]) (sleep 15)))
  [key-c]
  (fns
   :key-hold
   #(do
     (press [key-old-4]) (sleep 15)
     (release [key-old-4]) (sleep 15)
     (press [key-old-4]) (sleep 15)
     (release [key-old-4]) (sleep 15)
     (press [key-old-c]) (sleep 15)
     (release [key-old-c]) (sleep 15)))
  []
  (fns
   :key-hold
   #(do
     (press [key-old-4]) (sleep 10)
     (release [key-old-4]) (sleep 10)))))

(declare ^:dynamic input-list)

(defn this-jar
  "utility function to get the name of jar in which this function is invoked"
  [& [ns]]
  (-> (or ns (class *ns*))
      .getProtectionDomain .getCodeSource .getLocation .getPath))

(defn -main [& args]
 (with-open
  [reader
   (java.io.PushbackReader.
    (clojure.java.io/reader
     (clojure.java.io/file
      "d:/end06/clj-hisoutensoku/target/clj-hisoutensoku-config.clj")))]
  (let [window-pattern2 (read reader)
        new-keys (read reader)
        old-keys (read reader)
        transformer (read reader)
        [key2-a
         key2-b
         key2-c
         key2-d
         key2-1
         key2-2
         key2-3
         key2-4
         key2-6
         key2-7
         key2-8
         key2-9
         key2-power
         key2-card-choose
         key2-card-use]
        new-keys
        [key2-old-a
         key2-old-b
         key2-old-c
         key2-old-d
         key2-old-card-choose
         key2-old-card-use
         key2-old-2
         key2-old-6
         key2-old-8
         key2-old-4]
        old-keys]
   (binding [window-pattern window-pattern2
             key-a key2-a
             key-b key2-b
             key-c key2-c
             key-d key2-d
             key-1 key2-1
             key-2 key2-2
             key-3 key2-3
             key-4 key2-4
             key-6 key2-6
             key-7 key2-7
             key-8 key2-8
             key-9 key2-9
             key-power key2-power
             key-card-choose key2-card-choose
             key-card-use key2-card-use
             key-old-a key2-old-a
             key-old-b key2-old-b
             key-old-c key2-old-c
             key-old-d key2-old-d
             key-old-card-choose key2-old-card-choose
             key-old-card-use key2-old-card-use
             key-old-2 key2-old-2
             key-old-6 key2-old-6
             key-old-8 key2-old-8
             key-old-4 key2-old-4
             *ns* (create-ns 'clj-hisoutensoku.core)]
    (binding [input-list
              (eval transformer)]
     (loop [prev #{}]
      (if (touhou-window?)
       (do
        (Thread/sleep 1)
        (recur (:last-sequences (input-list
                                 (set (keep (fn[[name _]] (if (key-down? name) name)) vk-key-table))
                                 prev
                                 []))))
       (do
        (Thread/sleep 700)
        (recur prev)))))))))
