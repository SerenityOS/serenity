/*
 * @test  /nodynamiccopyright/
 * @bug 8058408
 * @summary Test that illegal.parenthesized.expression error key is used for parenthesized expression used in cast
 *
 * @compile/fail/ref=IllegalParenthesis.out -XDrawDiagnostics IllegalParenthesis.java
 */

import java.time.LocalDate;

class IllegalParenthesis {
    String s = (Integer).toString(123);
    void f(){
        LocalDate date = (LocalDate).now();
    }
}
