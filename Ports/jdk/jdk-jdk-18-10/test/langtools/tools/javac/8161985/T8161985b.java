/*
 * @test /nodynamiccopyright/
 * @bug 8161985
 * @summary Spurious override of Object.getClass leads to NPE
 * @compile/fail/ref=T8161985b.out -XDrawDiagnostics T8161985b.java
 */

class T8161985b {
   public String getClass() { return ""; }

   void test() {
      this.getClass().getSimpleName();
   }
}
