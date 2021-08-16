/*
 * @test /nodynamiccopyright/
 * @bug 4408036
 * @summary Compiler accepted "(i=2);" as a valid expession statement.
 * @author gafter
 *
 * @compile/fail/ref=Parens2.out -XDrawDiagnostics Parens2.java
 */

class Parens2 {
    void f() {
        int i;
        (i = 2);
    }
}
