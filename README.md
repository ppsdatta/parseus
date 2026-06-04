# parseus

A Clojure parser combinator library. Parsers are plain functions `String → [value rest]` on success, `[]` on failure, composed with a small set of combinators.

See [doc/api.md](doc/api.md) for the full API reference.

---

## Quick example — an expression calculator

```clojure
(ns parseus.examples.calculator
  (:use [parseus.core]))

(def whitespace (p-some (p-char \space)))
(def digits     (p-some (p-digit)))

(declare term)

(def factor
  (p-seq (:= d1 (p-or
                  (p-seq (p-char \()
                         whitespace
                         (:= t1 term)
                         whitespace
                         (p-char \))
                         (return t1))
                  (p-seq (:= d1 digits)
                         (return (-> d1 some-value num-value)))))
         (:= part (p-some
                    (p-seq whitespace
                           (:= op (p-or (p-char \*) (p-char \/)))
                           whitespace
                           (:= d2 factor)
                           (return {:op op :num2 d2}))))
         (return (if (fail? (some-value part))
                   d1
                   {:op (-> part first :op)
                    :args [d1 (-> part first :num2)]}))))

(def term
  (p-seq (:= f1 factor)
         (:= part (p-some
                    (p-seq whitespace
                           (:= op (p-or (p-char \+) (p-char \-)))
                           whitespace
                           (:= f2 term)
                           (return {:op op :f2 f2}))))
         (return (if (fail? (some-value part))
                   f1
                   {:op (-> part first :op)
                    :args [f1 (-> part first :f2)]}))))

(def op-map {\+ + \- - \* * \/ /})

(defn term-eval [t]
  (if (number? t)
    t
    (apply (op-map (:op t)) (map term-eval (:args t)))))

(defn calc [s]
  (->> s (parse term) first term-eval))
```

```clojure
(calc "2 + 3 * 99")
;; => 299

(calc "(2 + 3 * 99) * (5 - (11 / 11))")
;; => 1196
```

---

Copyright © 2023 soura.jagat@gmail.com
