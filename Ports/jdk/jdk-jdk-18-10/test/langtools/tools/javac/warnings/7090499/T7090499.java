/**
 * @test /nodynamiccopyright/
 * @bug 7094099
 * @summary -Xlint:rawtypes
 * @compile/fail/ref=T7090499.out -XDrawDiagnostics -Xlint:rawtypes T7090499.java
 */


class T7090499<E> {

    static class B<X> {}

    class A<X> {
        class X {}
        class Z<Y> {}
    }

    T7090499 t = new T7090499() { //raw warning (2)

        A.X x1;//raw warning
        A.Z z1;//raw warning

        T7090499.B<Integer> b1;//ok
        T7090499.B b2;//raw warning

        A<String>.X x2;//ok
        A<String>.Z<Integer> z2;//ok
        A<B>.Z<A<B>> z3;//raw warning (2)

        void test(Object arg1, B arg2) {//raw warning
            boolean b = arg1 instanceof A;//ok
            Object a = (A)arg1;//ok
            A a2 = new A() {};//raw warning (2)
            a2.new Z() {};//raw warning
        }
    };
}
