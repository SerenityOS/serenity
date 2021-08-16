/*
 * @test /nodynamiccopyright/
 * @bug 6910550
 *
 * @summary javac 1.5.0_17 fails with incorrect error message
 * @compile/fail/ref=T6910550d.out -XDrawDiagnostics T6910550d.java
 *
 */

class T6910550d {
    <X> void m(X x) {}
    <Y> void m(Y y) {}

    { m(null); }
}
