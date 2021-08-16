/*
 * @test /nodynamiccopyright/
 * @bug 5088429
 *
 * @summary varargs overloading problem
 * @author mcimadamore
 * @compile/fail/ref=T5088429Neg01.out -XDrawDiagnostics T5088429Neg01.java
 *
 */

class T5088429Neg01 {
    interface A {}
    interface B extends A {}

    T5088429Neg01(A... args) {}
    T5088429Neg01(A a, A... args) {}

    void test(B b) {
        new T5088429Neg01(b);
    }
}
