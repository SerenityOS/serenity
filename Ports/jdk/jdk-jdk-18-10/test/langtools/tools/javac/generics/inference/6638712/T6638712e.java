/*
 * @test /nodynamiccopyright/
 * @bug     6638712 6795689
 * @author  mcimadamore
 * @summary Inference with wildcard types causes selection of inapplicable method
 * @compile/fail/ref=T6638712e.out -XDrawDiagnostics T6638712e.java
 */

class T6638712e {

    static class Foo<A, B> {
        <X> Foo<X, B> m(Foo<? super X, ? extends A> foo) { return null;}
    }

    static class Test {
        Foo<Object, String> test(Foo<Boolean, String> foo1, Foo<Boolean, Boolean> foo2) {
             return foo1.m(foo2);
        }
    }
}
