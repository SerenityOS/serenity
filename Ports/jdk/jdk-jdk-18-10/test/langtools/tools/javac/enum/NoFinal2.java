/*
 * @test /nodynamiccopyright/
 * @bug 5097250 5087624
 * @summary Finalize methods on enums must be compile time error
 * @author Peter von der Ah\u00e9
 * @compile/fail/ref=NoFinal2.out -XDrawDiagnostics  NoFinal2.java
 */

enum NoFinal2 {
    A, B, C;
    protected void finalize() {
        System.err.println("FISK");
    }
}
