/*
 * @test /nodynamiccopyright/
 * @bug 6939780 7020044 8009459 8021338 8064365 8062373
 *
 * @summary  add a warning to detect diamond sites (including anonymous class instance creation at source >= 9)
 * @author mcimadamore
 * @compile/ref=T6939780_7.out -Xlint:-options -source 7 T6939780.java -XDrawDiagnostics -XDfind=diamond
 * @compile/ref=T6939780_8.out -Xlint:-options -source 8 T6939780.java -XDrawDiagnostics -XDfind=diamond
 * @compile/ref=T6939780_9.out -Xlint:-options -source 9 T6939780.java -XDrawDiagnostics -XDfind=diamond
 *
 */

class T6939780 {

    static class Foo<X extends Number> {
        Foo() {}
        Foo(X x) {}
    }

    void testAssign() {
        Foo<Number> f1 = new Foo<Number>(1);
        Foo<?> f2 = new Foo<Number>();
        Foo<?> f3 = new Foo<Integer>();
        Foo<Number> f4 = new Foo<Number>(1) {};
        Foo<?> f5 = new Foo<Number>() {};
        Foo<?> f6 = new Foo<Integer>() {};
    }

    void testMethod() {
        gn(new Foo<Number>(1));
        gw(new Foo<Number>());
        gw(new Foo<Integer>());
        gn(new Foo<Number>(1) {});
        gw(new Foo<Number>() {});
        gw(new Foo<Integer>() {});
    }

    void gw(Foo<?> fw) { }
    void gn(Foo<Number> fn) { }

    static class Foo2<X> {
        X copy(X t) {
            return t;
        }
    }

    void testReciever() {
        Number s = new Foo2<Number>().copy(0);
    }

}
