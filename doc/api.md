# parseus API Reference

All symbols are in `parseus.core`.

Result contract: every parser is a function `String → [value rest]` on success, `[]` on failure.

---

## Running parsers

### `parse`
```clojure
(parse p s) → result
```
Applies parser `p` to string `s`. Returns `[value remaining]` or `[]`.

---

## Primitives

### `p-char`
```clojure
(p-char)    ; matches any single character
(p-char c)  ; matches the specific character c
```
Returns the matched `Character` and the remaining string.

```clojure
(parse (p-char \a) "abc") ;=> [\a "bc"]
(parse (p-char \a) "xyz") ;=> []
(parse (p-char)    "xyz") ;=> [\x "yz"]
```

### `p-digit`
```clojure
(p-digit)    ; matches any digit 0–9
(p-digit n)  ; matches only the digit n (Integer)
```
Returns the matched digit as an `Integer`.

```clojure
(parse (p-digit)  "9 foo") ;=> [9 " foo"]
(parse (p-digit 3) "3 foo") ;=> [3 " foo"]
(parse (p-digit 3) "9 foo") ;=> []
```

---

## Combinators

### `p-or`
```clojure
(p-or p1 p2)
```
Ordered alternation. Tries `p1`; if it fails, tries `p2` on the same input (implicit backtrack).

```clojure
(parse (p-or (p-digit) (p-char \x)) "x!") ;=> [\x "!"]
(parse (p-or (p-digit) (p-char \x)) "5!") ;=> [5 "!"]
(parse (p-or (p-digit) (p-char \x)) "y!") ;=> []
```

### `p-some`
```clojure
(p-some p)
```
Zero-or-more repetition (EBNF `{ }`). Always succeeds. Returns `[[:none] rest]` when zero matches occur; returns `[[v1 v2 ...] rest]` otherwise.

Use `some-value` to normalize the result before further processing.

```clojure
(parse (p-some (p-digit)) "123x") ;=> [[1 2 3] "x"]
(parse (p-some (p-digit)) "x")    ;=> [[:none] "x"]
```

### `p-seq`
```clojure
(p-seq expr* (return value))
```
Sequences multiple parsers left-to-right. Short-circuits on the first failure. Bind intermediate results with `:=`:

```clojure
(:= name parser)   ; bind result of parser to name
(return expr)      ; produce expr as the result of the whole p-seq
```

A step without `:=` runs the parser for its effect only (result discarded).

```clojure
(def p-two-digits
  (p-seq (:= a (p-digit))
         (:= b (p-digit))
         (return [a b])))

(parse p-two-digits "42rest") ;=> [[4 2] "rest"]
(parse p-two-digits "4rest")  ;=> []
```

---

## Predicates & conversions

### `fail?`
```clojure
(fail? result) → boolean
```
Returns `true` when `result` is an empty sequence (i.e. a failed parse).

### `some-value`
```clojure
(some-value sm) → seq-or-[]
```
Unwraps a `p-some` result. Converts the zero-match sentinel `[:none]` to `[]` so downstream code can treat all results as plain sequences.

```clojure
(some-value [1 2 3]) ;=> [1 2 3]
(some-value [:none]) ;=> []
```

### `num-value`
```clojure
(num-value digit-list) → Integer
```
Converts a list of digit integers (as returned by `p-digit`) into a single integer.

```clojure
(num-value [1 2 3]) ;=> 123
(num-value [])      ;=> 0
```

### `str-value`
```clojure
(str-value char-list) → String
```
Converts a list of characters (as returned by `p-char`) into a string.

```clojure
(str-value [\h \i]) ;=> "hi"
(str-value [])      ;=> ""
```

---

## Composition patterns

### Building a number parser
```clojure
(def p-number
  (p-seq (:= ds (p-some (p-digit)))
         (return (num-value (some-value ds)))))
```

### Building a word parser
```clojure
(def p-word
  (p-seq (:= cs (p-some (p-char)))
         (return (str-value (some-value cs)))))
```

### Optional with `p-or`
Until `p-optional` is available, emulate it:
```clojure
(p-or p (fn [s] [nil s]))
```
