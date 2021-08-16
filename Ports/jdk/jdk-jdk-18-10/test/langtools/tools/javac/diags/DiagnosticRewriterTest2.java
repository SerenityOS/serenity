/*
 * @test /nodynamiccopyright/
 * @bug 8145466
 * @summary javac: No line numbers in compilation error
 * @compile/fail/ref=DiagnosticRewriterTest2.out -Xdiags:compact -XDrawDiagnostics DiagnosticRewriterTest2.java
 */

class DiagnosticRewriterTest2 {
   class Bar {
       Bar(Object o) { }
   }
   void test() {
      new Bar(null) {
         void g() {
            m(2L);
            m();
         }
      };
   }

   void m(int i) { }
}
