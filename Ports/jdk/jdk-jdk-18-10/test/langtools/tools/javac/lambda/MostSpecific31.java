/*
 * @test /nodynamiccopyright/
 * @bug 8144767
 * @summary Correct most-specific test when wildcards appear in functional interface type
 * @compile/fail/ref=MostSpecific31.out -XDrawDiagnostics MostSpecific31.java
 */
class MostSpecific31 {

    interface Pred<T> { boolean test(T arg); }
    interface Fun<T,R> { R apply(T arg); }

    static void m1(Pred<? super Number> f) {}
    static void m1(Fun<Integer, Boolean> f) {}

    static boolean foo(Object arg) { return false; }

    void test() {
        m1(MostSpecific31::foo);
    }

}
