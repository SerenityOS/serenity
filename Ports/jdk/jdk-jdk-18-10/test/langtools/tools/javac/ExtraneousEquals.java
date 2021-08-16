/*
 * @test /nodynamiccopyright/
 * @bug 5019614 8057647
 * @summary variance prototype syntax leftover
 *
 * @compile/fail/ref=ExtraneousEquals.out -XDrawDiagnostics ExtraneousEquals.java
 */

public class ExtraneousEquals {
  int[] foo = new int[=] { 1, 2, 3 };
}
