/*
 * @test /nodynamiccopyright/
 * @bug 5097250 5087624
 * @summary Finalize methods on enums must be compile time error
 * @author Peter von der Ah\u00e9
 * @compile/fail/ref=NoFinal3.out -XDrawDiagnostics  NoFinal3.java
 */

enum NoFinal3 {
    A, B, C;
    public void finalize() {
        System.err.println("FISK");
    }
}
