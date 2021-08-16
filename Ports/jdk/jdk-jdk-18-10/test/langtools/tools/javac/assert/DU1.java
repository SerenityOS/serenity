/*
 * @test /nodynamiccopyright/
 * @bug 4468510
 * @summary Check correct DU computation for assertions.
 * @author gafter
 * @compile/fail/ref=DU1.out -XDrawDiagnostics  DU1.java
 */

class DU1 {
    void f1() {
        final int i;
        try {
            assert false : i=3;
        } catch (AssertionError ae) {
        }
        i=4; // error
    }
}
