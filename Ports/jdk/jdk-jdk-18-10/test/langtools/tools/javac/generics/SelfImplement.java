/*
 * @test /nodynamiccopyright/
 * @bug 4725668
 * @summary generics: reject implementation with incorrect return type
 * @author gafter
 *
 * @compile/fail/ref=SelfImplement.out -XDrawDiagnostics   SelfImplement.java
 */

class SelfImplement {
    static abstract class A<T> {
        abstract void f(T t);
        public int f(Integer t) { return 3; }
    }
    static abstract class B extends A<Integer> {
        // error: A<Integer>.f(Integer) returning int can't implement
        //        A<Integer>.f(Integer) returning void.
    }
}
