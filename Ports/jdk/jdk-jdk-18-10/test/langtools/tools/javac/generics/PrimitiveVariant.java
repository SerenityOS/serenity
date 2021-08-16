/*
 * @test /nodynamiccopyright/
 * @bug 4471667
 * @summary compiler allows overriding with different primitive return type
 * @author gafter
 *
 * @compile/fail/ref=PrimitiveVariant.out -XDrawDiagnostics  PrimitiveVariant.java
 */

package PrimitiveVariant;

interface I {
    double m();
}

abstract class J {
    int m() { return 2; }
}

class Main extends J implements I {
    public short m() { return 1; }
}
