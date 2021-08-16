/*
 * @test /nodynamiccopyright/
 * @bug 5009574
 * @summary verify java.lang.Enum can't be directly subclassed
 * @author Joseph D. Darcy
 *
 * @compile/fail/ref=FauxEnum1.out -XDrawDiagnostics  FauxEnum1.java
 */

public class FauxEnum1 extends java.lang.Enum {
    private FauxEnum1() {
        // super("", 0);
    }
}
