/*
 * @test /nodynamiccopyright/
 * @summary Negative regression test from odersky
 * @author odersky
 *
 * @compile/fail/ref=BadTest3.out -XDrawDiagnostics  BadTest3.java
 */

class BadTest3 {

    interface I {}
    interface J {}
    static class C implements I, J {}
    static class D implements I, J {}

    interface Ord {}

    static class Main {

        static C c = new C();
        static D d = new D();

        static <B extends Ord> List<B> nil() { return new List<B>(); }
        static <B extends I & J> B f(B x) { return x; }

        static <A> List<A> cons(A x, List<A> xs) { return xs.prepend(x); }
        static <A> Cell<A> makeCell(A x) { return new Cell<A>(x); }
        static <A> A id(A x) { return x; }

        public static void main(String[] args) {
            List<String> xs = nil();
            f(null);
            f(nil());
            I i = f(null);
            J j = f(nil());
        }
    }
}
