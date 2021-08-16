/*
 * @test /nodynamiccopyright/
 * @bug 7022054
 *
 * @summary  Invalid compiler error on covariant overriding methods with the same erasure
 * @compile/fail/ref=T7022054neg1.out -XDrawDiagnostics T7022054neg1.java
 *
 */

class T7022054neg1 {
    static class A {
        A m(String s) { return null; }
    }
    static class B extends A {
        <X extends String> A m(X s) { return null; }
    }
}
