/*
 * @test    /nodynamiccopyright/
 * @bug     6886247
 * @author  Maurizio Cimadamore
 * @summary regression: javac crashes with an assertion error in Attr.java
 * @compile/fail/ref=T6886247_2.out -XDrawDiagnostics T6886247_2.java
 */

class Outer<E> {

   public void method(Outer<?>.Inner inner) {
       E entry = inner.getE();
   }

   class Inner {
       E getE() {return null;}
   }
}
