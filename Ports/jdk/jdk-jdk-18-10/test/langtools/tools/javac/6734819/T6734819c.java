/*
 * @test /nodynamiccopyright/
 * @bug 6734819
 * @summary Javac performs flows analysis on already translated classes
 * @author Maurizio Cimadamore
 *
 * @compile/fail/ref=T6734819c.out -XDrawDiagnostics -Xlint:all -XDverboseCompilePolicy T6734819c.java
 */
class Y extends W {}
class W extends Z {}

class Z {
    void m(Z z) {
        return;
        W w = (W)z;
    }
}
