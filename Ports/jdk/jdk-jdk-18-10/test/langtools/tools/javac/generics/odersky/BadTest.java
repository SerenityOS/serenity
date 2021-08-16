/*
 * @test /nodynamiccopyright/
 * @summary Negative regression test from odersky
 * @author odersky
 *
 * @compile/fail/ref=BadTest-old.out -XDrawDiagnostics -source 15 -Xlint:-options BadTest.java
 * @compile/fail/ref=BadTest.out -XDrawDiagnostics  BadTest.java
 */

class BadTest {
    static class Main {

        static <B> List<B> nil() { return new List<B>(); }
        static <A> List<A> cons(A x, List<A> xs) { return xs.prepend(x); }
        static <A> Cell<A> makeCell(A x) { return new Cell<A>(x); }
        static <A> A id(A x) { return x; }

        public static void main(String[] args) {
            List<Cell<String>> as = nil().prepend(makeCell(null));
            List<Cell<String>> bs = cons(makeCell(null), nil());
            List<String> xs = id(nil());
            List<String> ys = cons("abc", id(nil()));
            List<String> zs = id(nil()).prepend("abc");
            List<Cell<String>> us = id(nil()).prepend(makeCell(null));
            List<Cell<String>> vs = cons(makeCell(null), id(nil()));
            System.out.println(nil() instanceof List<String>);
            nil();
        }

    }
}
