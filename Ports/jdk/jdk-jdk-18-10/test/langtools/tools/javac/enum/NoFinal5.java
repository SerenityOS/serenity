/*
 * @test /nodynamiccopyright/
 * @bug 5097250 5087624
 * @summary Finalize methods on enums must be compile time error
 * @author Peter von der Ah\u00e9
 * @compile/fail/ref=NoFinal5.out -XDrawDiagnostics  NoFinal5.java
 */

enum NoFinal5 {
    A, B, C;
    void finalize() {
        System.err.println("FISK");
    }
}
