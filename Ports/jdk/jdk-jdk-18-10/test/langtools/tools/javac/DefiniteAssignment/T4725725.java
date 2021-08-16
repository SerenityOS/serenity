/*
 * @test /nodynamiccopyright/
 * @bug 4725725
 * @summary missing DA error in anonymous ctor
 * @author Neal Gafter (gafter)
 *
 * @compile/fail/ref=T4725725.out -XDrawDiagnostics  T4725725.java
 */

class T4725725 {
    final int x;
    final Object o = new Object() {
            int y = x; // error: x not DA
        };
    {
        x = 12;
    }
}
