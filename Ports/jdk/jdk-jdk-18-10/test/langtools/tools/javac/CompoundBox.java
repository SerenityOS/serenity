/*
 * @test /nodynamiccopyright/
 * @bug 4960369
 * @summary drop compound boxing operations
 * @author gafter
 *
 * @compile/fail/ref=CompoundBox.out -XDrawDiagnostics  CompoundBox.java
 */

class CompoundBox {
    {
        Float f = 3;
    }
}
