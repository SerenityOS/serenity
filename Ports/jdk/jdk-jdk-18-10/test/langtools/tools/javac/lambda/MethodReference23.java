/*
 * @test /nodynamiccopyright/
 * @bug 8003280 8075184 8081271
 * @summary Add lambda tests
 *  check that pair of bound/non-bound constructor references is flagged as ambiguous
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=MethodReference23.out -XDrawDiagnostics MethodReference23.java
 */

class MethodReference23 {

    class Inner1 {
        Inner1(MethodReference23 outer) {};
        Inner1() {};
    }

    static class Inner2 {
        Inner2(MethodReference23 outer) {};
        Inner2() {};
    }

    interface SAM11 {
        Inner1 m(MethodReference23 rec);
    }

    interface SAM12 {
        Inner1 m();
    }

    interface SAM21 {
        Inner2 m(MethodReference23 rec);
    }

    interface SAM22 {
        Inner2 m();
    }

    static void call11(SAM11 s) {   }

    static void call12(SAM12 s) {   }

    static void call21(SAM21 s) {   }

    static void call22(SAM22 s) {   }

    static void call3(SAM11 s) {   }
    static void call3(SAM12 s) {   }
    static void call3(SAM21 s) {   }
    static void call3(SAM22 s) {   }

    static void test11() {
        SAM11 s = MethodReference23.Inner1::new; // fail.
        call11(MethodReference23.Inner1::new); // fail.
    }

    static void test12() {
        SAM12 s = MethodReference23.Inner1::new; //fail
        call12(MethodReference23.Inner1::new); //fail
    }

    static void test21() {
        SAM21 s = MethodReference23.Inner2::new; //ok
        call21(MethodReference23.Inner2::new); //ok
    }

    static void test22() {
        SAM22 s = MethodReference23.Inner2::new; //ok
        call22(MethodReference23.Inner2::new); //ok
    }

    static void test3() {
        call3(MethodReference23.Inner2::new); //ambiguous
    }
}
