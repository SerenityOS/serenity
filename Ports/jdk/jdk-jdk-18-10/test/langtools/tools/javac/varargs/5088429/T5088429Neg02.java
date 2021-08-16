/*
 * @test /nodynamiccopyright/
 * @bug 5088429
 *
 * @summary varargs overloading problem
 * @author mcimadamore
 * @compile/fail/ref=T5088429Neg02.out -XDrawDiagnostics T5088429Neg02.java
 *
 */

class T5088429Neg02 {
    interface A {}
    interface B extends A {}

    void m(A... args) {}
    void m(A a, A... args) {}

    void test(B b) {
        m(b);
    }
}
