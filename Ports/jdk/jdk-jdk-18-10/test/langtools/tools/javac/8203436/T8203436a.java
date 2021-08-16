/*
 * @test /nodynamiccopyright/
 * @bug 8203436
 * @summary javac should fail early when emitting illegal signature attributes
 * @compile/fail/ref=T8203436a.out -XDrawDiagnostics T8203436a.java
 */

class T8203436a<X> {
   class Inner { }

   void test(T8203436a<?> outer) {
      outer.new Inner() { };
   }
}
