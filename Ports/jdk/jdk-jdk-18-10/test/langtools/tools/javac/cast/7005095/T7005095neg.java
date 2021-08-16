/*
 * @test /nodynamiccopyright/
 * @bug     7005095
 * @summary Cast: compile reject sensible cast from final class to interface
 * @compile/fail/ref=T7005095neg.out -XDrawDiagnostics T7005095neg.java
 */

class T7005095pos<T extends Integer> {
    interface Foo<T> {}

    static final class FooImpl implements Foo<String> {}

    Object o = (Foo<T>) new FooImpl();
}
