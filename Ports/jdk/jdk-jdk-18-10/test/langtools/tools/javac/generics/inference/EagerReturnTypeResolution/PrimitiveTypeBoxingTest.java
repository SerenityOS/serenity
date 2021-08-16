/*
 * @test /nodynamiccopyright/
 * @bug 8030741 8078024
 * @summary Inference: implement eager resolution of return types, consistent with JDK-8028800
 * @compile/fail/ref=PrimitiveTypeBoxingTest.out -XDrawDiagnostics PrimitiveTypeBoxingTest.java
 */

public class PrimitiveTypeBoxingTest {

    static void foo(long arg) {}
    static void bar(int arg) {}

    interface F<X> { void get(X arg); }

    <Z> void m1(F<Z> f, Z arg) {}
    <Z> void m2(Z arg, F<Z> f) {}

    void test() {
        m1(PrimitiveTypeBoxingTest::foo, 23); // expected: error
        m2(23, PrimitiveTypeBoxingTest::foo); // expected: error

        m1(PrimitiveTypeBoxingTest::bar, 23); // expected: success
        m2(23, PrimitiveTypeBoxingTest::bar); // expected: success
    }
}
