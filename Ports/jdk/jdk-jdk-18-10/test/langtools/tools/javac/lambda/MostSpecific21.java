/*
 * @test /nodynamiccopyright/
 * @bug 8143852
 * @summary Most specific inference constraints derived from both functional interface method parameters and tparam bounds
 * @compile/fail/ref=MostSpecific21.out -XDrawDiagnostics MostSpecific21.java
 */
class MostSpecific21 {
    interface F1<T> { <X extends T> Object apply(T arg); }
    interface F2 { <Y extends Number> String apply(Integer arg); }

    static <T> T m1(F1<T> f) { return null; }
    static Object m1(F2 f) { return null; }

    static String foo(Object in) { return "a"; }

    void test() {
        m1(MostSpecific21::foo);
    }

}