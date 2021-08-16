/*
 * @test /nodynamiccopyright/
 * @bug 7020043 7020044
 *
 * @summary  Check that diamond is not allowed with non-generic class types
 * @author R&eacute;mi Forax
 * @compile/fail/ref=Neg08.out Neg08.java -XDrawDiagnostics
 *
 */

class Neg08 {
   public static void main(String[] args) {
     String s = new String<>("foo");
   }
}
