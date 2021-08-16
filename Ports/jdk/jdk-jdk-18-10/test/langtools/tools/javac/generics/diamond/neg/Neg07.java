/*
 * @test /nodynamiccopyright/
 * @bug 6939620 7020044 8062373 8078024
 *
 * @summary  Check that diamond works where LHS is supertype of RHS (1-ary constructor)
 * @author mcimadamore
 * @compile/fail/ref=Neg07.out Neg07.java -XDrawDiagnostics
 *
 */

class Neg07 {
   static class SuperFoo<X> {}
   static class Foo<X extends Number> extends SuperFoo<X> {
       Foo(X x) {}
   }

   SuperFoo<String> sf1 = new Foo<>("");
   SuperFoo<String> sf2 = new Foo<>("") {};
}
