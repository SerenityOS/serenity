/*
 * @test /nodynamiccopyright/
 * @bug 8143852
 * @summary Rename functional interface method type parameters during most specific test
 * @compile/fail/ref=MostSpecific16.out -XDrawDiagnostics MostSpecific16.java
 */
class MostSpecific16 {
    interface F1 { <X> Object apply(Object arg); }
    interface F2 { String apply(Object arg); }

    static void m1(F1 f) {}
    static void m1(F2 f) {}

    static String foo(Object in) { return "a"; }

    void test() {
        m1(MostSpecific16::foo);
    }

}