/*
 * @test /nodynamiccopyright/
 * @bug 4394546
 * @summary get no err msg if label wrapped in parentheses
 * @author gafter
 *
 * @compile/fail/ref=Parens3.out -XDrawDiagnostics Parens3.java
 */

class Parens3 {
    void f() {
    (foo): ;
    }
}
