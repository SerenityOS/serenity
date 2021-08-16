/*
 * @test /nodynamiccopyright/
 * @bug 8003280 8078024
 * @summary Add lambda tests
 *  check that diamond inference is applied when using raw constructor reference qualifier
 * @compile/fail/ref=MethodReference43.out -XDrawDiagnostics MethodReference43.java
 */

public class MethodReference43 {

    interface SAM1 {
       Foo<?> m(String s);
    }

    interface SAM2 {
       Foo<?> m(Integer s);
    }

    interface SAM3 {
       Foo<?> m(Object o);
    }

    interface SAM4 {
       Foo<Number> m(Integer o);
    }

    static class Foo<X extends Number> {
        Foo(X x) { }
    }

    static void m1(SAM1 s) { }

    static void m2(SAM2 s) { }

    static void m3(SAM3 s) { }

    static void m4(SAM4 s) { }

    static void m5(SAM1 s) { }
    static void m5(SAM2 s) { }
    static void m5(SAM3 s) { }
    static void m5(SAM4 s) { }

    public static void main(String[] args) {
        m1(Foo::new);
        m2(Foo::new);
        m3(Foo::new);
        m4(Foo::new);
        m5(Foo::new);
    }
}
