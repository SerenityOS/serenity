/* @test /nodynamiccopyright/
 * @bug 7192246
 * @summary check that default overrides are properly type-checked
 * @compile/fail/ref=Neg10.out -Werror -Xlint:unchecked -XDrawDiagnostics Neg10.java
 */
class Neg10 {
    interface I<X extends Exception> {
        default void m() throws X { }
    }

    static class C1 {
        public void m() throws Exception { } //unchecked (throws) override
    }

    static class C2<Z extends Exception> extends C1 implements I<Z> { }

    static class C3<Z extends Exception> implements I<Z> {
        public void m() throws Exception { } //unchecked (throws) override
    }
}
