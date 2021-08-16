/*
 * @test /nodynamiccopyright/
 * @bug 8039214
 * @summary Capture variable as an inference variable's lower bound
 * @compile CaptureLowerBound.java
 * @compile/fail/ref=CaptureLowerBound7.out -Xlint:-options -source 7 -XDrawDiagnostics CaptureLowerBound.java
 */

public class CaptureLowerBound {

    interface I<X1,X2> {}
    static class C<T> implements I<T,T> {}

    <X> void m(I<? extends X, X> arg) {}

    void test(C<?> arg) {
      m(arg);
    }

}
