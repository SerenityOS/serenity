/*
 * @test   /nodynamiccopyright/
 * @bug    8080726
 * @summary Redundant error message on private abstract interface method with body.
 * @compile/fail/ref=Private10.out -XDrawDiagnostics Private10.java
 */


public class Private10 {
    interface I {
        private abstract void foo() {}
    }
    class C {
        private abstract void foo() {}
    }
}
