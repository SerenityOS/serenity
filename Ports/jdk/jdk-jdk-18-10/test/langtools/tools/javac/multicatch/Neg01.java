/*
 * @test /nodynamiccopyright/
 * @bug 6943289
 *
 * @summary Project Coin: Improved Exception Handling for Java (aka 'multicatch')
 * @author darcy
 * @compile/fail/ref=Neg01.out -XDrawDiagnostics Neg01.java
 */

class Neg01 {
    static class A extends Exception {}
    static class B1 extends A {}
    static class B2 extends A {}

    class Test {
        void m() throws A {
            try {
                throw new B1();
            } catch (final A ex1) {
                try {
                    throw ex1; // used to throw A, now throws B1!
                } catch (B2 ex2) { }//unreachable
            }
        }
    }
}
