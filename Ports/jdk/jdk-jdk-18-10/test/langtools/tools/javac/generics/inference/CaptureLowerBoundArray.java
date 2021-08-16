/*
 * @test /nodynamiccopyright/
 * @bug 8075793
 * @summary Capture variable as an inference lower bound followed by an array write
 * @compile/fail/ref=CaptureLowerBoundArray.out -XDrawDiagnostics CaptureLowerBoundArray.java
 * @compile -Xlint:-options -source 7 CaptureLowerBoundArray.java
 */

class CaptureLowerBoundArray {

    interface I<T> {
        T[] getArray();
    }

    <T> T[] m(T[] arg) { return null; }

    void test(I<? extends Exception> i) {
        m(i.getArray())[0] = new Exception();
    }


}
