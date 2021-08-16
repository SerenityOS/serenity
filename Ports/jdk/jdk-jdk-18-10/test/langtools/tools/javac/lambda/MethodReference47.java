/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that generic method reference is inferred when type parameters are omitted
 * @compile/fail/ref=MethodReference47.out -XDrawDiagnostics MethodReference47.java
 */
public class MethodReference47 {

    interface SAM1 {
       void m(Integer s);
    }

    interface SAM2 {
       void m(Integer s);
    }

    static class Foo<X extends Number> {
        Foo(X x) { }
    }

    static <X extends Number> void m(X fx) { }

    static void g1(SAM1 s) { }
    static void g2(SAM1 s) { }
    static void g2(SAM2 s) { }

    public static void main(String[] args) {
        g1(MethodReference47::m);
        g2(MethodReference47::m);
    }
}
