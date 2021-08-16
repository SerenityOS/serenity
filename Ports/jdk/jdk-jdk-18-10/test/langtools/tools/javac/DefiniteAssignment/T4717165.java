/*
 * @test /nodynamiccopyright/
 * @bug 4717165
 * @summary when can a statement complete normally? (break&continue versus finally)
 * @author Neal Gafter (gafter)
 *
 * @compile/fail/ref=T4717165.out -XDrawDiagnostics  T4717165.java
 */

class T4717165 {
    void f() {
        int i;
        a: try {
            break a;
        } finally {
            return;
        }
        i = 12; // unreachable
    }
}
