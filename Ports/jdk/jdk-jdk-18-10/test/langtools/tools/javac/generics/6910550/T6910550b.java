/*
 * @test /nodynamiccopyright/
 * @bug 6910550
 *
 * @summary javac 1.5.0_17 fails with incorrect error message
 * @compile/fail/ref=T6910550b.out -XDrawDiagnostics T6910550b.java
 *
 */

class T6910550b<X, Y, Z> {
    void m(X x) {}
    void m(Y y) {}
    void m(Z y) {}

    { m(null); }
}
