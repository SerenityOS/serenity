/*
 * @test /nodynamiccopyright/
 * @bug 8023549
 * @summary Compiler emitting spurious errors when constructor reference type is inferred and explicit type arguments are supplied
 * @compile/fail/ref=T8023549.out -XDrawDiagnostics T8023549.java
 */

public class T8023549 {
    static class Foo<X> { }

    interface Supplier<X> {
        X make();
    }

    interface ExtSupplier<X> extends Supplier<X> { }

    void m1(Supplier<Foo<String>> sfs) { }

    void m2(Supplier<Foo<String>> sfs) { }
    void m2(ExtSupplier<Foo<Integer>> sfs) { }

    void test() {
        Supplier<Foo<String>> sfs = Foo::<Number>new;
        m1(Foo::<Number>new);
        m2(Foo::<Number>new);
    }
}
