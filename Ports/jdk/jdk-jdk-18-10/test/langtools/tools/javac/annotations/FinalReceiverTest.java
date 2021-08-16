/*
 * @test /nodynamiccopyright/
 * @bug 8027886
 * @summary Receiver parameters must not be final
 * @compile/fail/ref=FinalReceiverTest.out  -XDrawDiagnostics FinalReceiverTest.java
 */

class FinalReceiverTest {
    void m() {
        class Inner {
            Inner(final FinalReceiverTest FinalReceiverTest.this) {}
        }
    }
}
