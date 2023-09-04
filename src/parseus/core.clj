(ns parseus.core
  (:use [clojure.walk :as w]))

(defn parse [p s]
  (p s))

(defn fail? [p]
  (and
    (seqable? p)
    (empty? p)))

(defn p-num [ns]
  (if (empty? ns)
    0
    (Integer/parseInt (apply str ns))))

(defn p-str [ss]
  (apply str ss))

(defn p-digit
  ([]
   (fn [s]
     (if (= (.length s) 0)
       []
       (let [c (int (.charAt s 0))]
         (if (and (>= c (int \0))
                  (<= c (int \9)))
           [(- c (int \0))
            (.substring s 1)]
           [])))))
  ([n]
   (fn [s]
     (let [[d rs] ((p-digit) s)]
       (if (and d (= d n))
         [d rs]
         [])))))

(defn p-char
  ([]
   (fn [s]
     (if (= (.length s) 0)
       []
       [(.charAt s 0)
        (.substring s 1)])))
  ([c]
   (fn [s]
     (let [[c0 rs] ((p-char) s)]
       (if (and c0 (= c0 c))
         [c0 rs]
         [])))))

(defn p-or [p1 p2]
  (fn [s]
    (let [o (p1 s)]
      (if (fail? o)
        (p2 s)
        o))))

(defn p-some [p]
  (fn [s & {:keys [acm] :or {acm []}}]
    (let [r (parse p s)]
      (if (fail? r)
        [acm s]
        (recur (r 1) {:acm (conj acm (r 0))})))))

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
  `(let [[~(second (first xs)) _rs#] (~(nth (first xs) 2) ~rs)]
     (if (fail? ~(second (first xs)))
       ~(second (first xs))
       (seq-exprs _rs# ~(rest xs)))))

(defmacro non-assign-expr [rs xs]
  `(let [[_x# _rs#] (~(first xs) ~rs)]
     (if (fail? _x#)
       _x#
       (seq-exprs _rs# ~(rest xs)))))

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



(comment

  (def p1 (p-digit 9))
  (def p2 (p-char \c))
  (def p3 (p-or p1 p2))

  (parse p1 "9 islands")
  (parse p2 "clojure")
  (parse p2 "ruby")

  (parse p3 "9B")
  (parse p3 "cB")

  (def p-digits (p-some (p-digit)))

  (parse p-digits "123algol")
  (parse p-digits "clojure")

  (def p-chars (p-some (p-char)))

  (def p-spaces (p-some (p-char \space)))

  (parse p-chars "abc233as")

  (def p-line (fn [rs]
                (let [[number1 rs] ((p-some (p-digit)) rs)]
                  (if (fail? number1)
                    number1
                    (let [[_x rs] ((p-some (p-char \space)) rs)]
                      (if (fail? _x)
                        _x
                        (let [[number2 rs] ((p-some (p-digit)) rs)]
                          (if (fail? number2)
                            number2
                            [(+ (p-num number1)
                                (p-num number2))
                             rs]))))))))

  (parse p-line "1234 4321 abcd")                            ; 5555
  (parse p-line "1234 abcd")                            ; 5555

  (def p-line2 (p-seq
                 (:= number1 (p-some (p-digit)))
                 (p-some (p-char \space))
                 (:= number2 (p-some (p-digit)))
                 (return (+ (p-num number1)
                            (p-num number2)))))

  (parse p-line2 "1234 4321 abcd")
  (parse p-line2 "1234 abcd")

  (macroexpand '(seq-exprs "" ((:= x (p-some (p-char))) (return x))))
  (macroexpand '(seq-exprs "" ((:= n1 (p-some (p-digit)))
                               (p-char \,)
                               (:= n2 (p-some (p-digit)))
                               (return (+ (p-num n1)
                                          (p-num n2))))))
  (macroexpand '(assign-expr "" ((:= x (p-some (p-char))) (return x))))
  (macroexpand '(return-expr "ac" ((return x))))

  (w/macroexpand-all '(p-seq (:= n1 (p-some (p-digit)))
                             (p-char \,)
                             (:= n2 (p-some (p-digit)))
                             (return (+ (p-num n1)
                                        (p-num n2)))))
  (def p6 (p-seq
            (:= n1 (p-some (p-digit)))
            (p-some (p-char \,))
            (:= n2 (p-some (p-digit)))
            (return (+ (p-num n1)
                       (p-num n2)))))
  (parse p6 "123,321 is my data")
  (parse p6 "a123,321 is my data")

  (def p7 (p-seq
            (:= number1 p-digits)
            p-spaces
            (:= number2 p-digits)
            (return [(p-num number1)
                     (p-num number2)])))
  (parse p7 "1234   4321 and others")

  (def delim
    (p-or
      (p-char \,)
      (p-some
        (p-char \space))))

  (parse delim "   ")

  )