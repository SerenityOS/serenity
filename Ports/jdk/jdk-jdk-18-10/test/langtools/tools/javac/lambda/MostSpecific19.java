/*
 * @test /nodynamiccopyright/
 * @bug 8143852
 * @summary Test that generic function interface method bounds are the same
 * @compile/fail/ref=MostSpecific19.out -XDrawDiagnostics MostSpecific19.java
 */
class MostSpecific19 {
    interface F1 { <X extends Number> Object apply(X arg); }
    interface F2 { <Y extends Integer> String apply(Y arg); }

    static void m1(F1 f) {}
    static void m1(F2 f) {}

    static String foo(Object in) { return "a"; }

    void test() {
        m1(MostSpecific19::foo);
    }

}