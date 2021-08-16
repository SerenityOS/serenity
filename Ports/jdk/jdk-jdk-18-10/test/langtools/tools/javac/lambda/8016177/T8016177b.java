/*
 * @test /nodynamiccopyright/
 * @bug 8016177 8016178
 * @summary structural most specific and stuckness
 * @compile/fail/ref=T8016177b.out -XDrawDiagnostics T8016177b.java
 */
class T8016177b {
    interface ToIntFunction<X> {
        int m(X x);
    }

    interface Function<X, Y> {
        Y m(X x);
    }

    <U, V> Function<U, V> id(Function<U, V> arg) { return null; }

    <U, V> Function<U, V> id2(Function<U, V> arg) { return null; }
    <U> ToIntFunction<U> id2(ToIntFunction<U> arg) { return null; }


    <X,Y,Z> X f(Y arg, Function<Y, Z> f) { return null; }

    <X,Y,Z> X f2(Y arg, Function<Y, Z> f) { return null; }
    <X,Y> X f2(Y arg, ToIntFunction<Y> f) { return null; }

    <T> T g(T arg) { return null; }

    void test() {
        g(f("hi", id(x->1))); //ok
        g(f("hi", id2(x->1))); //ambiguous
        g(f2("hi", id(x->1))); //ok
    }
}
