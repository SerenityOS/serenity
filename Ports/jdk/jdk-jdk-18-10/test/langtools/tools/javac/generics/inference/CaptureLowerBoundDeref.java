/*
 * @test /nodynamiccopyright/
 * @bug 8075793
 * @summary Capture variable as an inference lower bound followed by a member reference
 * @compile/fail/ref=CaptureLowerBoundDeref.out -XDrawDiagnostics CaptureLowerBoundDeref.java
 * @compile -Xlint:-options -source 7 CaptureLowerBoundDeref.java
 */

class CaptureLowerBoundDeref {

    interface Wrapper<T> {
        I<T> get();
    }

    interface I<T> {}

    interface K<T> { void take(T arg); }

    <T> K<T> m(I<? extends T> arg) { return null; }

    void test(Wrapper<?> w) {
        m(w.get()).take(new Object());
    }
}