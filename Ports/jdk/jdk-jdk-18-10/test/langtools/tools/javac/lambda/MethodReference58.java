/*
 * @test /nodynamiccopyright/
 * @bug 8004102 8078024
 * @summary Add support for generic functional descriptors
 * @compile/fail/ref=MethodReference58.out -XDrawDiagnostics MethodReference58.java
 */
class MethodReference58 {

    interface F_Object {
        <X> void m(X x);
    }

    interface F_Integer {
        <X extends Integer> void m(X x);
    }

    void test() {
        F_Object f1 = this::g; //incompatible bounds
        F_Integer f2 = this::g; //ok
    }

    <Z extends Number> void g(Z z) { }
}
