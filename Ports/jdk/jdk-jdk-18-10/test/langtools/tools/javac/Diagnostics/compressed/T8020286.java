/**
 * @test /nodynamiccopyright/
 * @bug     8020286
 * @summary Wrong diagnostic after compaction
 * @compile/fail/ref=T8020286.out -XDrawDiagnostics -Xdiags:compact T8020286.java
 */

class T8020286 {
   void m(String s) { }
   void m(Integer i, String s) { }
   void test() {
       m(1, 1);
       m(1);
   }
}
