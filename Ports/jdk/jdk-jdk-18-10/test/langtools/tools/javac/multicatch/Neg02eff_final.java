/*
 * @test /nodynamiccopyright/
 * @bug 6943289 6993963
 *
 * @summary Project Coin: Improved Exception Handling for Java (aka 'multicatch')
 * @author mcimadamore
 * @compile/fail/ref=Neg02eff_final.out -XDrawDiagnostics Neg02eff_final.java
 *
 */

class Neg02eff_final {
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
        } catch (A | B ex) {
            ex = new B();
        }
    }
}
