/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that generic method reference is inferred when type parameters are omitted
 * @compile/fail/ref=MethodReference45.out -XDrawDiagnostics MethodReference45.java
 */
public class MethodReference45 {

    static int assertionCount = 0;

    static void assertTrue(boolean cond) {
        assertionCount++;
        if (!cond)
            throw new AssertionError();
    }

    static class SuperFoo<X> { }

    static class Foo<X extends Number> extends SuperFoo<X> { }

    interface SAM1 {
        void m();
    }

    interface SAM2 {
        void m();
    }

    static <X extends Number> Foo<X> m() { return null; }

    static void g1(SAM1 s) { }
    static void g2(SAM1 s) { }
    static void g2(SAM2 s) { }

    void test() {
        g1(MethodReference45::m);
        g2(MethodReference45::m);
    }
}
