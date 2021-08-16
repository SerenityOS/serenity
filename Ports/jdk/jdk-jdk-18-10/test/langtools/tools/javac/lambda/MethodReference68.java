/*
 * @test /nodynamiccopyright/
 * @bug 8016175 8078024
 * @summary Add bottom-up type-checking support for unambiguous method references
 * @compile/fail/ref=MethodReference68.out -XDrawDiagnostics MethodReference68.java
 */
class MethodReference68 {
    interface F<X> {
       String m(X x);
    }

    static class Foo {
        String getName() { return ""; }
    }

    @SuppressWarnings("unchecked")
    <Z> void g(F<Z> fz, Z... zs) { }

    void test() {
         g(Foo::getName);
         g(Foo::getName, 1); //incompatible constraints, Z <: Foo, Z :> Integer
    }
}
