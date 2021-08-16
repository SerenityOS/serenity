/*
 * @test /nodynamiccopyright/
 * @bug 8016175
 * @summary Add bottom-up type-checking support for unambiguous method references
 * @compile/fail/ref=MethodReference69.out -XDrawDiagnostics MethodReference69.java
 */
class MethodReference69 {
    interface F<X> {
        String m(Integer x1, X x2);
    }

    static class Foo {
        String getNameAt(Integer i) { return ""; }
    }

    <Z> void g(F<Z> fz) { }

    void test() {
         g(Foo::getName);
    }
}
