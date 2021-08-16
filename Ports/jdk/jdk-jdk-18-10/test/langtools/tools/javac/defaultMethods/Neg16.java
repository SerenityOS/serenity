/* @test /nodynamiccopyright/
 * @bug 7192246
 * @summary check that level skipping in default super calls is correctly rejected
 * @compile/fail/ref=Neg16.out -XDrawDiagnostics Neg16.java
 */
class Neg16 {
    interface I { default void m() {  } }
    interface J extends I { default void m() {  } }

    static class C implements I, J {
        void foo() { I.super.m(); }
    }
}
