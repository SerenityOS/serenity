/*
 * @test /nodynamiccopyright/
 * @bug 6939620 7020044 8062373
 *
 * @summary  Check that diamond works where LHS is supertype of RHS (nilary constructor)
 * @author mcimadamore
 * @compile/fail/ref=Neg06.out Neg06.java -XDrawDiagnostics
 *
 */

class Neg06 {
   interface ISuperFoo<X> {}
   interface IFoo<X extends Number> extends ISuperFoo<X> {}

   static class CSuperFoo<X> {}
   static class CFoo<X extends Number> extends CSuperFoo<X> {}

   ISuperFoo<String> isf = new IFoo<>() {};
   CSuperFoo<String> csf1 = new CFoo<>();
   CSuperFoo<String> csf2 = new CFoo<>() {};
}
