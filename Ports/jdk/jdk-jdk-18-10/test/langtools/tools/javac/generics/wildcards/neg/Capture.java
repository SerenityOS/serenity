/*
 * @test /nodynamiccopyright/
 * @bug 4916634
 * @summary Wildcard capture
 * @author gafter
 *
 * @compile/fail/ref=Capture.out -XDrawDiagnostics  Capture.java
 */

class X<T> {}

class Capture {
    void f(X<X<? extends Number>> x) {
        f4(x);
    }

    <T> void f4(X<X<T>> x) {}
}
