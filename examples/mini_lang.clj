;; Small assignment language with floating-point support — parseus example
;; Evaluate in a REPL: load this file after adding parseus to your deps.
;;
;; Grammar (informal BNF):
;;   number   := digits ('.' digits)?
;;   variable := letter (letter | digit | '_')*
;;   factor   := '(' expr ')' | number | variable
;;   term     := factor (('*' | '/') factor)*
;;   expr     := term  (('+' | '-') term)*
;;   assign   := variable ':=' expr
;;   stmt     := assign | expr
;;   program  := stmt (';' stmt)*
;;
;; AST representation:
;;   number   → double
;;   variable → string
;;   binary   → {:op \op :left lhs :right rhs}
;;   assign   → {:assign "name" :value expr-ast}

(ns parseus.examples.mini-lang
  (:use [parseus.core]))

;; --- lexical primitives ---

(def digit-seq (p-many (p-digit)))

(def number
  (p-seq (:= int-part digit-seq)
         (:= frac-part (p-optional
                        (p-seq (p-char \.)
                               (:= fds digit-seq)
                               (return fds))))
         (return (Double/parseDouble
                  (if (nil? frac-part)
                    (apply str int-part)
                    (str (apply str int-part) "." (apply str frac-part)))))))

(def variable
  (p-seq (:= first-ch (p-satisfy #(Character/isLetter %)))
         (:= rest-chs (p-some (p-satisfy #(or (Character/isLetterOrDigit %) (= % \_)))))
         (return (apply str (cons first-ch (some-value rest-chs))))))

;; --- expression grammar (produces an AST) ---

(declare expr)

(def factor
  (p-or
   (p-seq (p-token (p-char \())
          (:= e (p-ref expr))
          (p-token (p-char \)))
          (return e))
   (p-token number)
   (p-token variable)))

(def term
  (p-seq (:= lhs factor)
         (:= pairs (p-some (p-seq (:= op (p-token (p-or (p-char \*) (p-char \/))))
                                  (:= rhs factor)
                                  (return [op rhs]))))
         (return (reduce (fn [acc [op rhs]] {:op op :left acc :right rhs})
                         lhs
                         (some-value pairs)))))

(def expr
  (p-seq (:= lhs term)
         (:= pairs (p-some (p-seq (:= op (p-token (p-or (p-char \+) (p-char \-))))
                                  (:= rhs term)
                                  (return [op rhs]))))
         (return (reduce (fn [acc [op rhs]] {:op op :left acc :right rhs})
                         lhs
                         (some-value pairs)))))

;; --- statements & program ---

(def assign-op (p-token (p-str ":=")))

(def statement
  (p-or
   (p-seq (:= var (p-token variable))
          assign-op
          (:= val expr)
          (return {:assign var :value val}))
   expr))

(def program
  (p-sep-by1 statement (p-token (p-char \;))))

;; --- evaluator ---

(def ^:private op-fns {\+ + \- - \* * \/ /})

(defn eval-expr [env node]
  (cond
    (number? node) node
    (string? node) (if (contains? env node)
                     (get env node)
                     (throw (ex-info (str "Undefined variable: " node) {:var node})))
    (map? node)    (let [l (eval-expr env (:left node))
                         r (eval-expr env (:right node))]
                     ((op-fns (:op node)) l r))))

(defn run-program
  "Evaluate a sequence of statements, threading env through each.
   Returns [final-env value-of-last-stmt]."
  [stmts]
  (reduce (fn [[env _] stmt]
            (if (and (map? stmt) (:assign stmt))
              (let [val (eval-expr env (:value stmt))]
                [(assoc env (:assign stmt) val) val])
              [env (eval-expr env stmt)]))
          [{} nil]
          stmts))

(defn mini-eval
  "Parse and evaluate a mini-lang program string.
   Returns [final-env value-of-last-stmt]."
  [s]
  (let [[stmts _] (parse program s)]
    (run-program stmts)))

;; --- try it ---

(comment
  (parse number "3.14")
  ;; => [3.14 ""]

  (parse number "42")
  ;; => [42.0 ""]

  (parse variable "x_1")
  ;; => ["x_1" ""]

  (parse variable "total_2nd")
  ;; => ["total_2nd" ""]

  (parse expr "1.5 + 2.5 * 3.0")
  ;; => [{:op \+, :left 1.5, :right {:op \*, :left 2.5, :right 3.0}} ""]

  (parse statement "x := 3.14")
  ;; => [{:assign "x", :value 3.14} ""]

  (parse program "x := 3.14 ; y := x * 2.0 ; x + y")

  (mini-eval "x := 3.14 ; y := x * 2.0 ; x + y")
  ;; => [{"x" 3.14, "y" 6.28} 9.42]

  (mini-eval "a := 10.0 ; b := 3.5 ; c := a - b * 2.0 ; c")
  ;; => [{"a" 10.0, "b" 3.5, "c" 3.0} 3.0]

  (mini-eval "x_1 := 100.0 ; x_2 := x_1 / 4.0 ; x_1 - x_2")
  ;; => [{"x_1" 100.0, "x_2" 25.0} 75.0]

  (mini-eval "r := 2.5 ; area := 3.14159 * r * r ; area")
  ;; => [{"r" 2.5, "area" 19.634...} 19.634...]
  )
