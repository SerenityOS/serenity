/*
 * @test /nodynamiccopyright/
 * @bug 8016175
 * @summary Add bottom-up type-checking support for unambiguous method references
 * @compile/fail/ref=MethodReference70.out -XDrawDiagnostics MethodReference70.java
 */
class MethodReference70 {
    interface F<X> {
        void m(X x);
    }

    interface G<X> {
        Integer m(X x);
    }

    void m1(Integer i) { }

    void m2(Integer i) { }
    void m2(String i) { }

    <Z> void g(F<Z> fz) { }
    <Z> void g(G<Z> gz) { }

    void test() {
         g(this::m1); //ok
         g(this::m2); //ambiguous (stuck!)
    }
}
