# parseus

A simple parser combinator library in Clojure

## Usage

```clojure
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
```

## License

Copyright © 2023 soura.jagat@gmail.com

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.

