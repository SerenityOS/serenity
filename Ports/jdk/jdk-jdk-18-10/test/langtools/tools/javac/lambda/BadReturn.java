/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that incompatible return types in lambdas are flagged with error
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=BadReturn.out -XDrawDiagnostics BadReturn.java
 */

class BadReturn {

    interface SAM {
        Comparable<?> m();
    }

    static void testNeg1() {
        SAM s = ()-> {
            if (true) {
                return "";
            } else {
                return System.out.println("");
            }};
    }

    static void testNeg2() {
        SAM s = ()-> { return System.out.println(""); };
    }

    static void testPos() {
        SAM s = ()-> {
            if (false) {
                return 10;
            }
            else {
                return true;
            }};
    }
}
