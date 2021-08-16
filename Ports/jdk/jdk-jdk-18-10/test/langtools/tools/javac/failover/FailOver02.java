/*
 * @test /nodynamiccopyright/
 * @bug 6970584
 * @summary Flow.java should be more error-friendly
 * @author mcimadamore
 *
 * @compile/fail/ref=FailOver02.out -XDrawDiagnostics --should-stop=at=FLOW -XDdev FailOver02.java
 */

class Test implements AutoCloseable {
    void test() {
        try(Test t = null) {}
    }
}
