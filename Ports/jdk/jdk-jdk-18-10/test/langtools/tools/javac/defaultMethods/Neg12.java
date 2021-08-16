/* @test /nodynamiccopyright/
 * @bug 7192246
 * @summary check that abstract methods are discarded in overload resolution diags
 * @compile/fail/ref=Neg12.out -XDrawDiagnostics Neg12.java
 */
class Neg12 {

    interface I1 {
        default void m(String s) {};
    }

    interface I2 {
        void m(String s);
    }

    static class B {
        void m(Integer i) { }
    }

    static class C extends B implements I1 { }
    static class D extends B implements I2 { }

    void test(C c, D d) {
        c.m();
        d.m();
    }
}
