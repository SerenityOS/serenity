/*
 * @test /nodynamiccopyright/
 * @bug 5097250 5087624
 * @summary Finalize methods on enums must be compile time error
 * @author Peter von der Ah\u00e9
 * @compile/fail/ref=NoFinal4.out -XDrawDiagnostics  NoFinal4.java
 */

enum NoFinal4 {
    A, B, C;
    private void finalize() {
        System.err.println("FISK");
    }
}
