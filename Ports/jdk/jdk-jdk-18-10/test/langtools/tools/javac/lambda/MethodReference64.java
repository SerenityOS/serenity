/*
 * @test /nodynamiccopyright/
 * @bug 8008540 8008539 8008538
 * @summary Constructor reference to non-reifiable array should be rejected
 * @compile/fail/ref=MethodReference64.out -XDrawDiagnostics MethodReference64.java
 */
class MethodReference64 {
    interface ClassFactory {
        Object m();
    }

    interface ArrayFactory {
        Object m(int i);
    }

    @interface Anno { }

    enum E { }

    interface I { }

    static class Foo<X> { }

    void m(ClassFactory cf) { }
    void m(ArrayFactory cf) { }

    void testAssign() {
        ClassFactory c1 = Anno::new; //error
        ClassFactory c2 = E::new; //error
        ClassFactory c3 = I::new; //error
        ClassFactory c4 = Foo<?>::new; //error
        ClassFactory c5 = 1::new; //error
        ArrayFactory a1 = Foo<?>[]::new; //ok
        ArrayFactory a2 = Foo<? extends String>[]::new; //error
    }

    void testMethod() {
        m(Anno::new); //error
        m(E::new); //error
        m(I::new); //error
        m(Foo<?>::new); //error
        m(1::new); //error
        m(Foo<?>[]::new); //ok - resolves to m(ArrayFactory)
        m(Foo<? extends String>[]::new); //error
    }
}
