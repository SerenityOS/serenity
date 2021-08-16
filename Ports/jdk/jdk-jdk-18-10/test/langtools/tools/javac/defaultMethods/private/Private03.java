/*
 * @test   /nodynamiccopyright/
 * @bug    8071453
 * @author sadayapalam
 * @summary Various tests for private methods in interfaces.
 * @compile/fail/ref=Private03.out -XDrawDiagnostics Private03.java
 */


public class Private03 {
    interface I {
        private void foo(int x) {}
        private void goo(int x) {}
    }

    interface J extends I {
        // Verify that we are able to declare a public abstract method with the same signature as a private method in super type.
        void foo(int x);
        // Verify that we are able to declare a public default method with the same signature as a private method in super type.
        default void goo(int x) {}
    }

    interface K extends J {
        private void foo(int x) {} // Error, cannot reduce visibility
        private void goo(int x) {} // Ditto.
    }
}
