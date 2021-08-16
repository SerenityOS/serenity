/*
 * @test /nodynamiccopyright/
 * @bug 8004101
 * @summary Add checks for method reference well-formedness
 * @compile/fail/ref=MethodReference56.out -XDrawDiagnostics MethodReference56.java
 */
class MethodReference56<X> {

    interface V {
        void m(Object o);
    }

    V v = MethodReference56<String>::m;

    void test() {
        g(MethodReference56<String>::m);
    }

    void g(V v) { }

    static void m(Object o) { };
}
