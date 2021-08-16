/*
 * @test /nodynamiccopyright/
 * @bug 4391330
 * @summary compiler accepted (Integer).toString(123)
 * @author gafter
 *
 * @compile/fail/ref=Parens1.out -XDrawDiagnostics Parens1.java
 */

class Parens1 {
    void f() {
        String s = (Integer).toString(123);
    }
}
