/*
 * @test /nodynamiccopyright/
 * @bug 6943289
 *
 * @summary Project Coin: Improved Exception Handling for Java (aka 'multicatch')
 * @author mcimadamore
 * @compile/fail/ref=Neg04.out -XDrawDiagnostics Neg04.java
 *
 */

class Neg04 {
    static class A extends Exception {}
    static class B extends Exception {}

    void test() throws B {
        try {
            if (true) {
                throw new A();
            } else if (false) {
                throw new B();
            } else {
                throw (Throwable)new Exception();
            }
        }
        catch (A e) {}
        catch (final Exception e) {
            throw e;
        }
        catch (Throwable t) {}
    }
}
