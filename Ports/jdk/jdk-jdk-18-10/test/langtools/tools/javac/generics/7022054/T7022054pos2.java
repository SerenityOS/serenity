/*
 * @test /nodynamiccopyright/
 * @bug 7022054
 *
 * @summary  Invalid compiler error on covariant overriding methods with the same erasure
 * @compile -source 7 T7022054pos2.java
 * @compile/fail/ref=T7022054pos2.out -XDrawDiagnostics T7022054pos2.java
 */

class T7022054pos2 {
    static class A {
        static A m(String s) { return null; }
    }
    static class B extends A {
        static <X extends B> X m(String s) { return null; }
    }
}
