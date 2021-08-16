/*
 * @test /nodynamiccopyright/
 * @bug 4721062
 * @summary DA treatment of return statements in constructors
 * @author Neal Gafter (gafter)
 *
 * @compile/fail/ref=T4721062a.out -XDrawDiagnostics  T4721062a.java
 */

class T4721062a {
    final int i;
    T4721062a() {
        if (true)
            return;
        i = 1;
    }
}
