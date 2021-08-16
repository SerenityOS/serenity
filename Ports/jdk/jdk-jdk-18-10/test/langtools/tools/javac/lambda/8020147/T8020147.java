/*
 * @test /nodynamiccopyright/
 * @bug 8020147
 * @summary Spurious errors when compiling nested stuck lambdas
 * @compile/fail/ref=T8020147.out -Werror -Xlint:cast -XDrawDiagnostics T8020147.java
 */
class T8020147 {
    interface Function<X, Y> {
        Y apply(X x);
    }

    <T> void g(Function<String, T> f) { }
    <U> String m(U u, Function<U, U> fuu) { return null; }

    void test() {
        g(x->m("", i->(String)i));
        g(x->m("", i->(String)x));
    }
}
