/* @test /nodynamiccopyright/
 * @bug 7192246
 * @summary check that default overrides are properly type-checked
 * @compile/fail/ref=Neg11.out -XDrawDiagnostics Neg11.java
 */
class Neg11 {
    interface I {
        default void m() { }
    }

    static class C1 {
        public void m() throws Exception { } //bad throws
    }

    static class C2 extends C1 implements I { }

    static class C3 implements I {
        public void m() throws Exception { } //bad throws
    }
}
