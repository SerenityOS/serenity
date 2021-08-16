/**
 * @test /nodynamiccopyright/
 * @bug 8041704
 * @summary wrong error message when mixing lambda expression and inner class
 * @compile/fail/ref=ErrorMessageTest.out -XDrawDiagnostics ErrorMessageTest.java
 */

public class ErrorMessageTest {
    void f(Runnable r) {
        f(() -> { f(new MISSING() { public void run() {} }); });
    }
}
