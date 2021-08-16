/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  certain cases of erroneous member reference lookup are not handled by Attr.visitReference
 * @compile/fail/ref=MethodReference51.out -XDrawDiagnostics MethodReference51.java
 */
class MethodReference51 {

    private static class Foo {
        static int j(int i) { return i; }
    }

    static Foo foo = new Foo();

    static void m(String s) { }
    static void m(Integer i) { }

    static int f(String s) { return 1; }

    static int g(Integer i, Number n) { return 1; }
    static int g(Number n, Integer i) { return 1; }

    int h(int i) { return i; }
}

class TestMethodReference51 {

    interface IntSam {
        int m(int i);
    }

    interface IntegerIntegerSam {
        int m(Integer i1, Integer i2);
    }


    static void test() {
        IntSam s1 = MethodReference51::unknown; //fail
        IntSam s2 = MethodReference51::f; //fail
        IntSam s3 = MethodReference51::g; //fail
        IntegerIntegerSam s4 = MethodReference51::g; //fail
        IntSam s5 = MethodReference51::h; //fail
        IntSam s6 = MethodReference51.foo::j; //fail
    }
}
