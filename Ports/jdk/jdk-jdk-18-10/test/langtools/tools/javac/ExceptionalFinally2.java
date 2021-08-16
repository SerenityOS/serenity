/*
 * @test /nodynamiccopyright/
 * @bug 4630634
 * @summary missing warn about exception not thrown in try block if finally can't complete
 * @author gafter
 *
 * @compile/fail/ref=ExceptionalFinally2.out -XDrawDiagnostics ExceptionalFinally2.java
 */

class ExceptionalFinally2 {
    static class E extends Exception {}

    public void t() throws E {}

    void f() {
        try {
            try {
                t();
            } finally {
                return;
            }
        } catch (E x) { // error: E can't be thrown in try block
        }
    }
}
