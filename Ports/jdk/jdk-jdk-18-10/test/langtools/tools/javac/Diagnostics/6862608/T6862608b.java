/**
 * @test /nodynamiccopyright/
 * @bug     6862608
 * @summary rich diagnostic sometimes contain wrong type variable numbering
 * @author  mcimadamore
 * @compile/fail/ref=T6862608b.out -XDrawDiagnostics --diags=formatterOptions=disambiguateTvars,where T6862608b.java
 */

class T66862608b<T extends String, S> {
   <S, T extends S> void foo(T t) {
      test(t);
   }

   void test(T t) {}
}
