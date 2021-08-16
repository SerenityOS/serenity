/*
 * @test /nodynamiccopyright/
 * @bug     6638712
 * @author  mcimadamore
 * @summary Inference with wildcard types causes selection of inapplicable method
 * @compile/fail/ref=T6638712b.out -XDrawDiagnostics T6638712b.java
 */

class T6638712b<X> {

    <I extends T6638712b<T>, T> T m(I test) { return null; }

    void test(T6638712b<Integer> x) {
        String i = m(x);
    }
}
