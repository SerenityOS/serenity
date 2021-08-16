/* @test /nodynamiccopyright/
 * @bug 7192246
 * @summary check that level skipping in default super calls is correctly rejected
 * @compile/fail/ref=Neg15.out -XDrawDiagnostics Neg15.java
 */
class Neg15 {
    interface I { default void m() {  } }
    interface J extends I { default void m() {  } }
    interface K extends I {}

    static class C implements J, K {
        void foo() { K.super.m(); }
    }
}
