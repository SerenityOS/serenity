/*
 * @test /nodynamiccopyright/
 * @bug 4718708
 * @summary bug in DU analysis of while loop
 * @author Neal Gafter (gafter)
 *
 * @compile/fail/ref=T4718708.out -XDrawDiagnostics  T4718708.java
 */

class T4718708 {
    void f() {
        final int i;
        while (true) {
            i = 3;
            continue;
        }
    }
}
