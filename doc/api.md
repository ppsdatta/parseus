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

### `p-satisfy`
```clojure
(p-satisfy pred)
```
Matches a single character satisfying `pred`. Fails on empty input or when the predicate returns false. Foundation for all character-level parsers.

```clojure
(parse (p-satisfy #(Character/isUpperCase %)) "Hello") ;=> [\H "ello"]
(parse (p-satisfy #(Character/isUpperCase %)) "hello") ;=> []
```

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
(parse (p-digit)   "9 foo") ;=> [9 " foo"]
(parse (p-digit 3) "3 foo") ;=> [3 " foo"]
(parse (p-digit 3) "9 foo") ;=> []
```

### `p-any-of`
```clojure
(p-any-of chars)
```
Matches a single character that is a member of `chars`.

```clojure
(parse (p-any-of [\a \e \i \o \u]) "apricot") ;=> [\a "pricot"]
(parse (p-any-of [\a \e \i \o \u]) "fruit")   ;=> []
```

### `p-none-of`
```clojure
(p-none-of chars)
```
Matches a single character that is **not** in `chars`.

```clojure
(parse (p-none-of [\, \;]) "abc,") ;=> [\a "bc,"]
(parse (p-none-of [\, \;]) ",abc") ;=> []
```

### `p-eof`
```clojure
p-eof
```
Succeeds only at end of input, returning `[nil ""]`. Use to assert a complete parse.

```clojure
(parse p-eof "")    ;=> [nil ""]
(parse p-eof "abc") ;=> []
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
Zero-or-more repetition (EBNF `{ }`). Always succeeds. Returns `[[:none] rest]` on zero matches; `[[v1 v2 ...] rest]` otherwise. Use `some-value` to normalise.

```clojure
(parse (p-some (p-digit)) "123x") ;=> [[1 2 3] "x"]
(parse (p-some (p-digit)) "x")    ;=> [[:none] "x"]
```

### `p-many`
```clojure
(p-many p)
```
One-or-more repetition. Like `p-some` but fails when zero matches occur.

```clojure
(parse (p-many (p-digit)) "123x") ;=> [(1 2 3) "x"]
(parse (p-many (p-digit)) "x")    ;=> []
```

### `p-optional`
```clojure
(p-optional p)
```
Runs `p`; if it fails returns `[nil rest]` instead of `[]`. Never fails.

```clojure
(parse (p-optional (p-char \-)) "-42") ;=> [\- "42"]
(parse (p-optional (p-char \-)) "42")  ;=> [nil "42"]
```

### `p-seq`
```clojure
(p-seq expr* (return value))
```
Sequences multiple parsers left-to-right. Short-circuits on the first failure. Bind intermediate results with `:=`; a step without `:=` runs for effect only.

```clojure
(:= name parser)  ; bind result of parser to name
(return expr)     ; produce expr as the result of the whole p-seq
```

```clojure
(parse (p-seq (:= a (p-digit))
              (p-char \-)
              (:= b (p-digit))
              (return (+ a b)))
       "3-7done")
;=> [10 "done"]
```

### `p-collect`
```clojure
(p-collect p1 p2 ...)
```
Runs each parser in sequence and collects all results into a vector. Fails if any parser fails.

```clojure
(parse (p-collect (p-char \c) (p-char \+) (p-char \+)) "c++ rocks")
;=> [[\c \+ \+] " rocks"]
```

### `p-str`
```clojure
(p-str s)
```
Matches the literal string `s` character by character, returning a vector of matched characters. Use `str-value` to convert back to a string.

```clojure
(parse (p-str "if") "if (x)") ;=> [[\i \f] " (x)"]
(str-value (first *1))        ;=> "if"
```

### `p-keyword`
```clojure
(p-keyword kw)
```
Like `p-str` but asserts the next character is not a letter or digit. Prevents `"if"` from matching the prefix of `"iffy"`.

```clojure
(parse (p-keyword "if") "if (x)") ;=> ["if" " (x)"]
(parse (p-keyword "if") "iffy")   ;=> []
```

### `p-between`
```clojure
(p-between open close p)
```
Runs `open`, then `p`, then `close`; returns the result of `p`.

```clojure
(parse (p-between (p-char \() (p-char \)) (p-many (p-digit))) "(123)rest")
;=> [(1 2 3) "rest"]
```

### `p-sep-by1`
```clojure
(p-sep-by1 p sep)
```
Parses one-or-more occurrences of `p` separated by `sep`. Returns a seq of parsed values.

```clojure
(parse (p-sep-by1 (p-many (p-digit)) (p-char \,)) "1,22,333end")
;=> [((1) (2 2) (3 3 3)) "end"]
```

### `p-sep-by`
```clojure
(p-sep-by p sep)
```
Like `p-sep-by1` but zero-or-more. Returns `[[] rest]` when nothing matches.

```clojure
(parse (p-sep-by (p-digit) (p-char \,)) "1,2,3rest") ;=> [(1 2 3) "rest"]
(parse (p-sep-by (p-digit) (p-char \,)) "rest")      ;=> [[] "rest"]
```

### `p-chain-left`
```clojure
(p-chain-left p op)
```
Parses `p (op p)*` and folds results **left-associatively**. `op` must return a two-argument function.

```clojure
; 10 - 3 - 2 = (10-3)-2 = 5
(parse (p-chain-left p-num (p-fmap {\- -} (p-char \-))) "10-3-2")
;=> [5 ""]
```

### `p-chain-right`
```clojure
(p-chain-right p op)
```
Like `p-chain-left` but folds **right-associatively**.

```clojure
; 1 - 2 - 3 = 1-(2-3) = 2
(parse (p-chain-right p-num (p-fmap {\- -} (p-char \-))) "1-2-3")
;=> [2 ""]
```

---

## Lookahead

### `p-look-ahead`
```clojure
(p-look-ahead p)
```
Runs `p` and returns its result but does **not** consume any input.

```clojure
(parse (p-look-ahead (p-digit)) "3abc") ;=> [3 "3abc"]
```

### `p-not-followed-by`
```clojure
(p-not-followed-by p)
```
Succeeds (returning `[nil rest]`) if `p` would **fail** at the current position; consumes nothing. Used to assert a boundary.

```clojure
(parse (p-not-followed-by (p-digit)) "abc") ;=> [nil "abc"]
(parse (p-not-followed-by (p-digit)) "3abc") ;=> []
```

---

## Skipping & whitespace

### `p-skip`
```clojure
(p-skip p)
```
Runs `p` and discards its value, returning `[nil rest]`. Propagates failure.

```clojure
(parse (p-seq (p-skip (p-char \())
              (:= n (p-digit))
              (p-skip (p-char \)))
              (return n))
       "(7)rest")
;=> [7 "rest"]
```

### `p-whitespace`
```clojure
p-whitespace
```
Skips zero-or-more whitespace characters (space, tab, newline). Always succeeds.

```clojure
(parse p-whitespace "   abc") ;=> [... "abc"]
(parse p-whitespace "abc")    ;=> [... "abc"]
```

### `p-token`
```clojure
(p-token p)
```
Skips leading whitespace then runs `p`. Use to wrap every terminal in a whitespace-insensitive grammar.

```clojure
(parse (p-token (p-many (p-digit))) "   42rest") ;=> [[4 2] "rest"]
```

### `p-comment`
```clojure
p-comment
```
Skips a `(* ... *)` comment, including nested comments. Returns `[nil rest]`.

```clojure
(parse p-comment "(* hello (* nested *) world *)rest")
;=> [nil "rest"]
```

---

## Transformation

### `p-fmap`
```clojure
(p-fmap f p)
```
`fmap` for parsers. Applies `f` to the parsed value on success; propagates failure unchanged.

```clojure
(parse (p-fmap #(* % 10) (p-digit)) "3abc") ;=> [30 "abc"]
(parse (p-fmap str-value (p-many (p-char))) "hi!") ;=> ["hi!" ""]
```

---

## Recursion

### `p-ref`
```clojure
(p-ref v)
```
Macro. Wraps a `declare`d var so it is resolved lazily at parse time rather than at definition time. Required for mutually recursive parsers.

```clojure
(declare p-expr)

(def p-atom
  (p-or (p-between (p-char \() (p-char \)) (p-ref p-expr))
        (p-fmap num-value (p-many (p-digit)))))

(def p-expr
  (p-chain-left p-atom (p-fmap {\+ +} (p-char \+))))
```

---

## Predicates & conversions

### `fail?`
```clojure
(fail? result) → boolean
```
Returns `true` when `result` is `[]` (a failed parse).

### `some-value`
```clojure
(some-value sm) → seq-or-[]
```
Unwraps a `p-some` result. Converts the zero-match sentinel `[:none]` to `[]`.

```clojure
(some-value [1 2 3]) ;=> [1 2 3]
(some-value [:none]) ;=> []
```

### `num-value`
```clojure
(num-value digit-list) → Integer
```
Converts a list of digit integers (as returned by `p-digit` or `p-many`/`p-some` of `p-digit`) into a single integer.

```clojure
(num-value [1 2 3]) ;=> 123
(num-value [])      ;=> 0
```

### `str-value`
```clojure
(str-value char-list) → String
```
Converts a list of characters into a string.

```clojure
(str-value [\h \i]) ;=> "hi"
(str-value [])      ;=> ""
```

---

## Common patterns

### Number parser
```clojure
(def p-number
  (p-fmap num-value (p-many (p-digit))))
```

### Word parser (letters only)
```clojure
(def p-word
  (p-fmap str-value
          (p-many (p-satisfy #(Character/isLetter %)))))
```

### Identifier parser
```clojure
(def p-ident
  (p-seq (:= first-char (p-satisfy #(Character/isLetter %)))
         (:= rest       (p-some (p-satisfy #(Character/isLetterOrDigit %))))
         (return (str-value (cons first-char (some-value rest))))))
```

### Comma-separated list
```clojure
(p-sep-by1 p-number (p-token (p-char \,)))
```

### Parenthesised expression
```clojure
(p-between (p-token (p-char \())
           (p-token (p-char \)))
           p-expr)
```
