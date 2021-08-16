/*
 * @test /nodynamiccopyright/
 * @bug 8004101 8072445
 * @summary Add checks for method reference well-formedness
 * @compile/fail/ref=MethodReference55.out -XDrawDiagnostics MethodReference55.java
 */
class MethodReference55<X> {

    interface V {
        void m(Object o);
    }

    V v = new MethodReference55<String>()::m;

    void test() {
        g(new MethodReference55<String>()::m);
    }

    void g(V v) { }

    static void m(Object o) { };
}
