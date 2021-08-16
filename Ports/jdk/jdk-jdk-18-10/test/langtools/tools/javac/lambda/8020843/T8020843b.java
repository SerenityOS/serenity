/*
 * @test /nodynamiccopyright/
 * @bug 8020843
 * @summary javac crashes on accessibility check with method reference with typevar receiver
 * @compile/fail/ref=T8020843b.out -XDrawDiagnostics T8020843b.java
 */

class T8020843b {
    interface Function<X, Y> {
        Y m(X x);
    }

    interface BiFunction<X, Y, Z> {
        Z m(X x, Y y);
    }

    Object m(int i) { return null; }
    static Object m(String t) { return null; }

    Object m2(int i) { return null; }
    static Object m2(long t) { return null; }

    static void test() {
        Function<T8020843b, Object> f1 = T8020843b::m; //show bound case diag
        BiFunction<T8020843b, String, Object> f2 = T8020843b::m2; //show unbound case diag
    }
}
