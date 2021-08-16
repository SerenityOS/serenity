/*
 * @test /nodynamiccopyright/
 * @bug 6910550
 *
 * @summary javac 1.5.0_17 fails with incorrect error message
 * @compile/fail/ref=T6910550c.out -XDrawDiagnostics T6910550c.java
 *
 */

class T6910550c {
    void m(Object[] x) {}
    void m(Object... x) {}

    { m(); }
    { m(null); }
    { m(null, null); }
    { m(null, null, null); }
}
