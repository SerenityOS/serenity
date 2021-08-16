/*
 * @test /nodynamiccopyright/
 * @bug 8016175
 * @summary Add bottom-up type-checking support for unambiguous method references
 * @compile/fail/ref=MethodReference72.out -XDrawDiagnostics MethodReference72.java
 */
class MethodReference72 {
    interface F<X> {
        @SuppressWarnings("unchecked")
        void m(X... x);
    }

    void m1(Integer i) { }

    <Z> void g(F<Z> f) { }

    void test() {
        g(this::m1); //bad method reference argument type
    }
}
