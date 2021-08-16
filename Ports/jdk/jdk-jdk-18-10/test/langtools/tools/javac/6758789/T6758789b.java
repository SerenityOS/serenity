/*
 * @test /nodynamiccopyright/
 * @bug 6758789
 * @summary 6758789: Some method resolution diagnostic should be improved
 * @author Maurizio Cimadamore
 *
 * @compile/fail/ref=T6758789b.out -Werror -XDrawDiagnostics -Xlint:unchecked T6758789b.java
 */

class T6758789a {
    class Foo<T> {}

    <X> void m(Foo<X> foo) {}

    void test() {
        m(new Foo());
    }
}
