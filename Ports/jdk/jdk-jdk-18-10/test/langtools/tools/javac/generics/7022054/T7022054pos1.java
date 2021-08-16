/*
 * @test /nodynamiccopyright/
 * @bug 7022054
 *
 * @summary  Invalid compiler error on covariant overriding methods with the same erasure
 * @compile -source 7 T7022054pos1.java
 * @compile/fail/ref=T7022054pos1.out -XDrawDiagnostics T7022054pos1.java
 *
 */

class T7022054pos1 {
    static class A {
        A m(String s) { return null; }
    }
    static class B extends A {
        <X extends B> X m(String s) { return null; }
    }
}
