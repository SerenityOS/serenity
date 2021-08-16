/*
 * @test /nodynamiccopyright/
 * @bug 8143852
 * @summary Most specific failure if ivar can be bounded by functional interface method tparam
 * @compile/fail/ref=MostSpecific23.out -XDrawDiagnostics MostSpecific23.java
 */
class MostSpecific23 {
    interface F1<T> { <X extends T> Object apply(Integer arg); }
    interface F2 { <Y extends Class<Y>> String apply(Integer arg); }

    static <T> T m1(F1<T> f) { return null; }
    static Object m1(F2 f) { return null; }

    static String foo(Object in) { return "a"; }

    void test() {
        m1(MostSpecific23::foo);
    }

}