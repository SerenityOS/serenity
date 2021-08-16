/*
 * @test /nodynamiccopyright/
 * @bug 8145466
 * @summary javac: No line numbers in compilation error
 * @compile/fail/ref=DiagnosticRewriterTest.out -Xdiags:compact -XDrawDiagnostics DiagnosticRewriterTest.java
 */

class DiagnosticRewriterTest {
   void test() {
      new Object() {
         void g() {
            m(2L);
         }
      };
   }

   void m(int i) { }
}
