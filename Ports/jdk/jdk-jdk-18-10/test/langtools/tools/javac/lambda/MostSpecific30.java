/*
 * @test /nodynamiccopyright/
 * @bug 8144767
 * @summary Correct most-specific test when wildcards appear in functional interface type
 * @compile/fail/ref=MostSpecific30.out -XDrawDiagnostics MostSpecific30.java
 */
class MostSpecific30 {

    interface Pred<T> { boolean test(T arg); }
    interface Fun<T,R> { R apply(T arg); }

    static void m1(Pred<? extends Integer> f) {}
    static void m1(Fun<Integer, Boolean> f) {}

    void test() {
        m1((Integer n) -> true);
    }

}
