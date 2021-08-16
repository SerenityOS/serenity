/* @test  /nodynamiccopyright/
   @bug 4087127 4785453
   @author dps
   @summary class: instance access through types is not allowed

   @compile/fail/ref=NonStaticFieldExpr3.out -XDrawDiagnostics NonStaticFieldExpr3.java
*/

class NonStaticFieldExpr3 {
  public int x;
}

class Subclass extends NonStaticFieldExpr3 {
  int a = NonStaticFieldExpr3.x;      // SHOULD BE ERROR
}
