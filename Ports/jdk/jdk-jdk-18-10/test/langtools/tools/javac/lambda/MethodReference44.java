/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that generic method reference is inferred when type parameters are omitted
 * @compile/fail/ref=MethodReference44.out -XDrawDiagnostics MethodReference44.java
 */

public class MethodReference44 {

    static class SuperFoo<X> { }

    static class Foo<X extends Number> extends SuperFoo<X> { }

    interface SAM1 {
        SuperFoo<String> m();
    }

    interface SAM2 {
        SuperFoo<Integer> m();
    }

    interface SAM3 {
        SuperFoo<Object> m();
    }

    static <X extends Number> Foo<X> m() { return null; }

    static void g1(SAM1 s) { }

    static void g2(SAM2 s) { }

    static void g3(SAM3 s) { }

    static void g4(SAM1 s) { }
    static void g4(SAM2 s) { }
    static void g4(SAM3 s) { }

    public static void main(String[] args) {
        g1(MethodReference44::m);
        g2(MethodReference44::m);
        g3(MethodReference44::m);
        g4(MethodReference44::m);
    }
}
