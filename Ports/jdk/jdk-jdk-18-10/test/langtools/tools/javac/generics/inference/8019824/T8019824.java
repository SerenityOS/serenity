/**
 * @test /nodynamiccopyright/
 * @bug 8019824
 * @summary very long error messages on inference error
 * @compile/fail/ref=T8019824.out -XDrawDiagnostics T8019824.java
 */
class T8019824 {
    void test(Class<? extends Foo<?, ?>> cls) {
        Foo<?, ?> foo = make(cls);
    }

    <A, B, C extends Foo<A, B>> Foo<A, B> make(Class<C> cls) { return null; }

    interface Foo<A, B> {}
}
