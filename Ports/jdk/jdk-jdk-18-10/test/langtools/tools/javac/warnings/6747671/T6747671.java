/**
 * @test /nodynamiccopyright/
 * @bug 6747671 8022567
 * @summary -Xlint:rawtypes
 * @compile/ref=T6747671.out -XDrawDiagnostics -Xlint:rawtypes T6747671.java
 */


class T6747671<E> {

    static class B<X> {}

    class A<X> {
        class X {}
        class Z<Y> {}
    }


    A.X x1;//raw warning
    A.Z z1;//raw warning

    T6747671.B<Integer> b1;//ok
    T6747671.B b2;//raw warning

    A<String>.X x2;//ok
    A<String>.Z<Integer> z2;//ok
    A<B>.Z<A<B>> z3;//raw warning (2)

    void test(Object arg1, B arg2) {//raw warning
        boolean b = arg1 instanceof A;//ok
        Object a = (A)arg1;//ok
        A a2 = new A() {};//raw warning (2)
        a2.new Z() {};//raw warning
    }

    @TA B @TA[] arr = new @TA B @TA [0];//JDK-8022567: raw warning (2)
    //todo: 8057688 type annotations in type argument position are lost
    Class<B[]> classes1;//no warning
    Class<B>[] classes2;//no warning

    @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE)
    @interface TA { }
}
