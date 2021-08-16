/*
 * @test /nodynamiccopyright/
 * @bug 8016175
 * @summary Add bottom-up type-checking support for unambiguous method references
 * @compile/fail/ref=MethodReference71.out -XDrawDiagnostics MethodReference71.java
 */
class MethodReference71 {
    interface F<X> {
        void m(X x);
    }

    interface G<X> {
        Integer m(X x);
    }

    void m1(Integer i) { }
    void m2(Integer... i) { }

    <Z> void g(F<Z> f) { }
    <Z> void g(G<Z> g) { }

    void test() {
         g(this::m1); //ok
         g(this::m2); //ambiguous (stuck!)
    }
}
