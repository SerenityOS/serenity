/*
 * @test /nodynamiccopyright/
 * @bug 6943289
 *
 * @summary Project Coin: Improved Exception Handling for Java (aka 'multicatch')
 * @author mcimadamore
 * @compile/fail/ref=Neg03.out -XDrawDiagnostics Neg03.java
 *
 */

class Neg03 {

    static class A extends Exception { public void m() {}; public Object f;}
    static class B1 extends A {}
    static class B2 extends A {}

    void m() throws B1, B2 {
        try {
            if (true) {
                throw new B1();
            }
            else {
                throw new B2();
            }
        } catch (Exception ex) {
            ex = new B2(); //effectively final analysis disabled!
            throw ex;
        }
    }
}
