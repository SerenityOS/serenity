/*
 * @test /nodynamiccopyright/
 * @bug 8144767
 * @summary Correct most-specific test when wildcards appear in functional interface type
 * @compile/fail/ref=MostSpecific32.out -XDrawDiagnostics MostSpecific32.java
 */
class MostSpecific32 {

    interface A<T> {}
    interface B<T> extends A<T> {}

    interface F1<S> { A<S> apply(); }
    interface F2<S> { B<S> apply(); }

    static void m1(F1<? extends Number> f1) {}
    static void m1(F2<? extends Number> f2) {}

    void test() {
        m1(() -> null); // B<CAP ext Number> </: A<Number>
    }

}
