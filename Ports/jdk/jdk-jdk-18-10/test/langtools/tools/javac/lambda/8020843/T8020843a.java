/*
 * @test /nodynamiccopyright/
 * @bug 8020843
 * @summary javac crashes on accessibility check with method reference with typevar receiver
 * @compile/fail/ref=T8020843a.out -XDrawDiagnostics T8020843a.java
 */

class T8020843a {
    interface Function<X, Y> {
        Y m(X x);
    }

    <T> void test(T t) {
        Function<T, Object> ss = T::clone;
    }
}
