/*
 * @test /nodynamiccopyright/
 * @bug 4736963
 * @summary Negative regression test from odersky
 * @author odersky
 *
 * @compile/fail/ref=BadTest4.out -XDrawDiagnostics -source 7 -Xlint:-options BadTest4.java
 * @compile BadTest4.java
 */

class BadTest4 {

    interface I {}
    interface J {}
    static class C implements I, J {}
    static class D implements I, J {}

    interface Ord {}

    static class Main {

        static C c = new C();
        static D d = new D();

        static <B> List<B> nil() { return new List<B>(); }
        static <A, B extends A> A f(A x, B y) { return x; }
        static <A, B extends A> B g(List<A> x, List<B> y) { return y.head; }

        static <A> List<A> cons(A x, List<A> xs) { return xs.prepend(x); }
        static <A> Cell<A> makeCell(A x) { return new Cell<A>(x); }
        static <A> A id(A x) { return x; }

        static Integer i = Integer.valueOf(1);
        static Number n = i;

        public static void main(String[] args) {
            Number x = f(n, i);
            x = f(i, n);
            f(cons("abc", nil()), nil());
            f(nil(), cons("abc", nil()));
        }
    }
}
