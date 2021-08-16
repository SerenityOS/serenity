/*
 * @test /nodynamiccopyright/
 * @bug 6910550
 *
 * @summary javac 1.5.0_17 fails with incorrect error message
 * @compile/fail/ref=T6910550e.out -XDrawDiagnostics T6910550e.java
 *
 */

class T6910550e {
    static class Pair<X,Y> {}

    <X> void m(Pair<X,X> x) {}
    <X,Y> void m(Pair<X,Y> y) {}

   { m(new Pair<String,String>());
     m(new Pair<String,Integer>()); }
}
