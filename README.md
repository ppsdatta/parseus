# parseus

A Clojure library for simple monadic parser combinators. Below is an example of
an expression parser detailing someof the important ideas.

### Example - an expression parser

First we import the namespace and create some util functions. For convenience,
we will `use` instead of `require` the `parseus.core` ns.

```clojure
(ns parseus.examples
  (:use [parseus.core]))

(def whitespace
  (p-some
    (p-char \space)))

(def digits (p-some (p-digit)))

```

A whitespace is zero or more spaces. A number (or digits) is zero of more
digit characters. Here, we chose to use zero-or-more instead of one-or-more as the later
functionality is still not available.

The BNF grammar for the parser looks like this:

```

Factor := '(' Term ')' | 
          Digits       | 
          Digits [*, /] Factor 

Term := Factor |
        Factor [+, -] Term

```

The equivalent code in parseus

```clojure
(def factor (p-seq (:= d1 (p-or
                            (p-seq
                              (p-char \()
                              whitespace
                              (:= t1 term)
                              whitespace
                              (p-char \))
                              (return t1))
                            (p-seq
                              (:= d1 digits)
                              (return (-> d1 some-value num-value)))))
                   (:= part (p-some
                              (p-seq
                                whitespace
                                (:= op (p-or (p-char \*)
                                             (p-char \/)))
                                whitespace
                                (:= d2 factor)
                                (return {:op op :num2 d2}))))
                   (return (if (fail? (some-value part))
                             d1
                             {:op   (-> part first :op)
                              :args [d1
                                     (-> part first :num2)]}))))

(def term (p-seq
            (:= f1 factor)
            (:= part (p-some
                       (p-seq
                         whitespace
                         (:= op (p-or (p-char \+)
                                      (p-char \-)))
                         whitespace
                         (:= f2 term)
                         (return {:op op :f2 f2}))))
            (return (if (fail? (some-value part))
                      f1
                      {:op   (-> part first :op)
                       :args [f1 (-> part first :f2)]}))))
```

Running it on an expression:

```clojure
(parse term "(2 + 3 * 99 ) *(5 - (11 / 11) )")

;; [{:op \*, :args [{:op \+, :args [2 {:op \*, :args [3 99]}]} {:op \-, :args [5 {:op \/, :args [11 11]}]}]} ""]
```

We can make a tiny integer calculator from it.

```clojure
(def op-map {\+ +
             \- -
             \* *
             \/ /})

(defn term-eval [term]
  (if (number? term)
    term
    (let [op (:op term)
          axs (map term-eval (:args term))]
      (apply (op-map op) axs))))

(defn calc [s]
  (->> s
       (parse term)
       first
       term-eval))

(calc "(2 + 3 * 99 ) *(5 - (11 / 11) )")
;; 1196
```

Copyright © 2023 soura.jagat@gmail.com
