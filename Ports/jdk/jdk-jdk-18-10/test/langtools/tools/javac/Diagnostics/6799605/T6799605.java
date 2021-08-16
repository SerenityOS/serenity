/**
 * @test /nodynamiccopyright/
 * @bug     6799605 8078024
 * @summary Basic/Raw formatters should use type/symbol printer instead of toString()
 * @author  mcimadamore
 * @compile/fail/ref=T6799605.out -XDrawDiagnostics  T6799605.java
 * @compile/fail/ref=T6799605.out -XDoldDiags -XDrawDiagnostics  T6799605.java
 */

class T6799605<X> {

    <T extends T6799605<T>> void m(T6799605<T> x1) {}
    <T> void m(T6799605<T> x1, T6799605<T> x2) {}
    <T> void m(T6799605<T> x1, T6799605<T> x2, T6799605<T> x3) {}

    void test(T6799605<?> t) {
        m(t);
        m(t, t);
        m(t, t, t);
    }
}
