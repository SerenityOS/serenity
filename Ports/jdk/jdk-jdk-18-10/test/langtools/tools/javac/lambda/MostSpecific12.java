/*
 * @test /nodynamiccopyright/
 * @bug 8034223
 * @summary Most-specific testing with inference variables in function parameter types
 * @compile/fail/ref=MostSpecific12.out -XDrawDiagnostics MostSpecific12.java
 */
class MostSpecific12 {

    interface I<T> { void take(T arg1, String arg2); }
    interface J<T> { void take(String arg1, T arg2); }
    interface K { void take(String arg1, String arg2); }

    <T> void m1(I<T> arg) {}
    void m1(K arg) {}

    <T> void m2(J<T> arg) {}
    <T> void m2(K arg) {}

    <T> void m3(I<T> arg) {}
    <T> void m3(J<T> arg) {}

    void test() {
        m1((String s1, String s2) -> {}); // ok
        m2((String s1, String s2) -> {}); // ok
        m3((String s1, String s2) -> {}); // error

        m1(this::referencedMethod); // ok
        m2(this::referencedMethod); // ok
        m3(this::referencedMethod); // error

        m1(String::compareTo); // ok
        m2(String::compareTo); // ok
        m3(String::compareTo); // error
    }

    void referencedMethod(String s1, String s2) {}

}
