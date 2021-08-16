/*
 * @test /nodynamiccopyright/
 * @bug 6943289
 *
 * @summary Project Coin: Improved Exception Handling for Java (aka 'multicatch')
 * @author darcy
 * @compile/fail/ref=Neg01eff_final.out -XDrawDiagnostics Neg01eff_final.java
 */

class Neg01eff_final {
    static class A extends Exception {}
    static class B1 extends A {}
    static class B2 extends A {}

    class Test {
        void m() throws A {
            try {
                throw new B1();
            } catch (A ex1) {
                try {
                    throw ex1; // used to throw A, now throws B1!
                } catch (B2 ex2) { }//unreachable
            }
        }
    }
}
