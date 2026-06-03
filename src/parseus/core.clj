(ns parseus.core
  (:use [clojure.walk :as w]))

(defn parse [p s]
  (p s))

(defn fail? [p]
  (and
    (seqable? p)
    (empty? p)))

(defn num-value [ns]
  (if (empty? ns)
    0
    (Integer/parseInt (apply str ns))))

(defn str-value [ss]
  (apply str ss))

(defn some-value [sm]
  (if (and (seqable? sm)
           (= (count sm) 1)
           (= (first sm) :none))
    []
    sm))

(defn p-satisfy [pred]
  (fn [s]
    (if (empty? s)
      []
      (let [c (.charAt s 0)]
        (if (pred c) [c (.substring s 1)] [])))))

(defn p-digit
  ([]
   (fn [s]
     (let [r ((p-satisfy #(Character/isDigit %)) s)]
       (if (fail? r)
         []
         [(- (int (first r)) (int \0)) (second r)]))))
  ([n]
   (fn [s]
     (let [[d rs] ((p-digit) s)]
       (if (and d (= d n))
         [d rs]
         [])))))

(defn p-char
  ([]
   (p-satisfy (constantly true)))
  ([c]
   (p-satisfy #(= % c))))

(defn p-any-of [chars]
  (p-satisfy (set chars)))

(defn p-none-of [chars]
  (p-satisfy (complement (set chars))))

(def p-eof
  (fn [s]
    (if (empty? s) [nil s] [])))

(defn p-or [p1 p2]
  (fn [s]
    (let [o (p1 s)]
      (if (fail? o)
        (p2 s)
        o))))

(defn p-fmap [f p]
  (fn [s]
    (let [r (p s)]
      (if (fail? r)
        []
        [(f (first r)) (second r)]))))

(defn p-collect [& parsers]
  (fn [s]
    (loop [parsers parsers, s s, acm []]
      (if (empty? parsers)
        [acm s]
        (let [r ((first parsers) s)]
          (if (fail? r)
            []
            (recur (rest parsers) (second r) (conj acm (first r)))))))))

(defn p-str [s]
  (apply p-collect (map p-char s)))

(defn p-some [p]
  (fn [s]
    (loop [s s, acm []]
      (let [r (parse p s)]
        (if (or (fail? r) (= (r 1) s))
          [(if (empty? acm) [:none] acm) s]
          (recur (r 1) (conj acm (r 0))))))))

(defn p-optional [p]
  (p-or p (fn [s] [nil s])))

;(p-seq
;  (:= number1 (p-some (p-digit)))
;  (p-some (p-char \space))
;  (:= number2 (p-some (p-digit)))
;  (return (+ (p-num number1)
;             (p-num number2)))

;(fn [rs]
;  (let [[number1 rs] ((p-some (p-digit)) rs)]
;    (if (fail? number1)
;      number1
;      (let [[_x rs] ((p-some (p-char \space)) rs)]
;        (if (fail? _x)
;          _x
;          (let [[number2 rs] ((p-some (p-digit)) rs)]
;            (if (fail? number2)
;              number2
;              [(+ (p-num number1)
;                  (p-num number2))
;                rs]))))))))

(defmacro return-expr [rs xs]
  (let [x (first xs)]
    `[~(second x)
      ~rs]))

(defmacro assign-expr [rs xs]
  `(let [_r# (~(nth (first xs) 2) ~rs)]
     (if (fail? _r#)
       _r#
       (let [[~(second (first xs)) _rs#] _r#]
         (seq-exprs _rs# ~(rest xs))))))

(defmacro non-assign-expr [rs xs]
  `(let [_r# (~(first xs) ~rs)]
     (if (fail? _r#)
       _r#
       (let [[_x# _rs#] _r#]
         (seq-exprs _rs# ~(rest xs))))))

(defmacro seq-exprs [rs xs]
  (cond
    (empty? xs) `[nil ~rs]
    (and (seqable? (first xs))
         (= (first (first xs)) 'return)) `(return-expr ~rs ~xs)
    (and (seqable? (first xs))
         (= (first (first xs)) ':=)) `(assign-expr ~rs ~xs)
    :else `(non-assign-expr ~rs ~xs)))

(defmacro p-seq [& xs]
  `(fn [s#]
     (seq-exprs s# ~xs)))

(defn p-many [p]
  (p-seq (:= h p)
         (:= t (p-some p))
         (return (cons h (some-value t)))))

(defn p-skip [p]
  (p-fmap (constantly nil) p))

(def p-whitespace
  (p-some (p-satisfy #(Character/isWhitespace %))))

(defn p-token [p]
  (p-seq p-whitespace
         (:= v p)
         (return v)))

(defn p-look-ahead [p]
  (fn [s]
    (let [r (p s)]
      (if (fail? r)
        []
        [(first r) s]))))

(defn p-not-followed-by [p]
  (fn [s]
    (if (fail? (p s))
      [nil s]
      [])))

(defn p-keyword [kw]
  (p-seq (:= _ (p-str kw))
         (p-not-followed-by (p-satisfy #(Character/isLetterOrDigit %)))
         (return kw)))

(defn p-between [open close p]
  (p-seq open (:= v p) close (return v)))

(defn p-sep-by1 [p sep]
  (p-seq (:= h p)
         (:= t (p-some (p-seq sep (:= v p) (return v))))
         (return (cons h (some-value t)))))

(defn p-sep-by [p sep]
  (p-or (p-sep-by1 p sep)
        (fn [s] [[] s])))



(comment


  )