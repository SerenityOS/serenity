/*
 * @test /nodynamiccopyright/
 * @bug 8143852
 * @summary Test that functional interface method parameter types are equal, even for an explicit lambda
 * @compile/fail/ref=MostSpecific28.out -XDrawDiagnostics MostSpecific28.java
 */
class MostSpecific28 {

    interface Pred<T> { boolean test(T arg); }
    interface Fun<T,R> { R apply(T arg); }

    static void m1(Pred<? super Integer> f) {}
    static void m1(Fun<Number, Boolean> f) {}

    void test() {
        m1((Number n) -> true);
    }

}
