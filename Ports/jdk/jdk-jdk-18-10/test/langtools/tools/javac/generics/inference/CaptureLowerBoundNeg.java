/*
 * @test /nodynamiccopyright/
 * @bug 8039214
 * @summary Capture variable as an inference variable's lower bound
 * @compile/fail/ref=CaptureLowerBoundNeg.out -XDrawDiagnostics CaptureLowerBoundNeg.java
 * @compile -Xlint:-options -source 7 CaptureLowerBoundNeg.java
 */

public class CaptureLowerBoundNeg {

    static class D<T> {
        void take(T arg) {}
        static <T> D<T> make(Class<? extends T> c) { return new D<T>(); }
    }

    void test(Object o) {
        D.make(o.getClass()).take(o);
    }

}
