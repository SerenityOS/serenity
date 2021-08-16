/*
 * @test /nodynamiccopyright/
 * @bug 4721062
 * @summary DA treatment of return statements in constructors
 * @author Neal Gafter (gafter)
 *
 * @compile/fail/ref=T4721062b.out -XDrawDiagnostics  T4721062b.java
 */

class T4721062b {
    final int i;
    T4721062b(boolean b) {
        if (b)
            return;
        i = 1;
    }
}
