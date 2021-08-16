/*
 * @test /nodynamiccopyright/
 * @bug 6967002 8006775
 * @summary JDK7 b99 javac compilation error (java.lang.AssertionError)
 * @author Maurizio Cimadamore
 * @compile/fail/ref=T6967002.out -XDrawDiagnostics T6967002.java
 */
class Test {
   private static void m(byte[] octets) {
      return m(octets..., ?);
   }
}
