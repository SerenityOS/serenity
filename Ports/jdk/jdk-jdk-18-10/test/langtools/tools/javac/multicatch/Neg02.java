/*
 * @test /nodynamiccopyright/
 * @bug 6943289
 *
 * @summary Project Coin: Improved Exception Handling for Java (aka 'multicatch')
 * @author mcimadamore
 * @compile/fail/ref=Neg02.out -XDrawDiagnostics Neg02.java
 *
 */

class Neg02 {
    static class A extends Exception {}
    static class B extends Exception {}

    void m() {
        try {
            if (true) {
                throw new A();
            }
            else {
                throw new B();
            }
        } catch (final A | B ex) {
            ex = new B();
        }
    }
}
