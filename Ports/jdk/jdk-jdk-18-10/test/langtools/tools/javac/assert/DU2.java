/*
 * @test /nodynamiccopyright/
 * @bug 4468510
 * @summary Check correct DU computation for assertions.
 * @author gafter
 * @compile/fail/ref=DU2.out -XDrawDiagnostics  DU2.java
 */

class DU2 {
    void f1() {
        final int i;
        i=5;
        assert false;
        i=6; // error
    }
}
