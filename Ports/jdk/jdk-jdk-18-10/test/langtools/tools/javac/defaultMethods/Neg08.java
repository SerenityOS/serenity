/* @test /nodynamiccopyright/
 * @bug 7192246
 * @summary check that default overrides are properly type-checked
 * @compile/fail/ref=Neg08.out -XDrawDiagnostics Neg08.java
 */
class Neg08 {
    interface I {
        default void m() { }
    }

    static class C1 {
        void m() { } //weaker modifier
    }

    static class C2 extends C1 implements I { }

    static class C3 implements I {
        void m() { } //weaker modifier
    }
}
