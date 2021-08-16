/*
 * @test /nodynamiccopyright/
 * @bug 4717181
 * @summary javac treats inherited abstract method as an overrider
 * @author gafter
 *
 * @compile/fail/ref=T4717181c.out -XDrawDiagnostics   T4717181c.java
 */

class T4717181c {
    static abstract class A<T> {
        abstract void f(T t);
        abstract int f(Integer t);
    }
    static abstract class B extends A<Integer> {}
}
