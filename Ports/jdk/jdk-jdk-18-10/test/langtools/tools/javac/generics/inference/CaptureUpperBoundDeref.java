/*
 * @test /nodynamiccopyright/
 * @bug 8075793
 * @summary Capture variable as an inference upper bound followed by a member reference
 * @compile/fail/ref=CaptureUpperBoundDeref.out -XDrawDiagnostics CaptureUpperBoundDeref.java
 * @compile -Xlint:-options -source 7 CaptureUpperBoundDeref.java
 */

class CaptureUpperBoundDeref {

    interface Wrapper<T> {
        I<T> get();
    }

    interface I<T> {}

    <T> T m(I<? super T> arg) { return null; }

    void test(Wrapper<? super String> w) {
        m(w.get()).substring(0);
    }
}