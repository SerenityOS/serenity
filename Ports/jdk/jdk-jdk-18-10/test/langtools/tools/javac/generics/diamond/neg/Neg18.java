/*
 * @test /nodynamiccopyright/
 * @bug 8062373
 * @summary Test that inaccessible vararg element type triggers an error during diamond inferred anonymous class instance creation.
 * @compile/fail/ref=Neg18.out Neg18.java -XDrawDiagnostics
 */

import java.util.Collections;
import pkg.Neg18_01;

class Neg18 {

   public static void main(String[] args) {
        new Neg18_01<>() {};
   }
}
