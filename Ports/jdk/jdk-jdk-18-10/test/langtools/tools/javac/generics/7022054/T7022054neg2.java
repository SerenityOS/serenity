/*
 * @test /nodynamiccopyright/
 * @bug 7022054
 *
 * @summary  Invalid compiler error on covariant overriding methods with the same erasure
 * @compile/fail/ref=T7022054neg2.out -XDrawDiagnostics T7022054neg2.java
 *
 */

class T7022054neg2 {
    static class A {
        static A m(String s) { return null; }
    }
    static class B extends A {
        static <X extends String> A m(X s) { return null; }
    }
}
