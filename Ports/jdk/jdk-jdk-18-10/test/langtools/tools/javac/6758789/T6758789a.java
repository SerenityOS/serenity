/*
 * @test /nodynamiccopyright/
 * @bug 6758789
 * @summary 6758789: Some method resolution diagnostic should be improved
 * @author Maurizio Cimadamore
 *
 * @compile/fail/ref=T6758789a.out -XDrawDiagnostics T6758789a.java
 */

class T6758789a {
    void m1() {}
    void m2(int i) {}
    void test() {
        m1(1);
        m2();
    }
}
