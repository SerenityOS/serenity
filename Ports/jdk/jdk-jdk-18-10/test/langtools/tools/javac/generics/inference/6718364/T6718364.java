/**
 * @test /nodynamiccopyright/
 * @bug 6718364
 * @summary inference fails when a generic method is invoked with raw arguments
 * @compile/ref=T6718364.out -XDrawDiagnostics -Xlint:unchecked T6718364.java
 */
class T6718364 {
    class X<T> {}

    public <T> void m(X<T> x, T t) {}

    public void test() {
        m(new X<X<Integer>>(), new X());
    }
}
