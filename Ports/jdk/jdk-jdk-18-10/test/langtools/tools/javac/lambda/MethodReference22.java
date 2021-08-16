/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that pair of bound/non-bound method references checked correctly
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=MethodReference22.out -XDrawDiagnostics MethodReference22.java
 */

class MethodReference22 {

    void m1(String x) { }
    void m1(MethodReference22 rec, String x) { }

    static void m2(String x) { }
    static void m2(MethodReference22 rec, String x) { }

    static void m3(String x) { }
    void m3(MethodReference22 rec, String x) { }

    void m4(String x) { }
    static void m4(MethodReference22 rec, String x) { }

    interface SAM1 {
        void m(String x);
    }

    interface SAM2 {
        void m(MethodReference22 rec, String x);
    }

    static void call1(SAM1 s) {   }

    static void call2(SAM2 s) {   }

    static void call3(SAM1 s) {   }
    static void call3(SAM2 s) {   }

    static void test1() {
        SAM1 s1 = MethodReference22::m1; //fail
        call1(MethodReference22::m1); //fail
        SAM1 s2 = MethodReference22::m2; //ok
        call1(MethodReference22::m2); //ok
        SAM1 s3 = MethodReference22::m3; //ok
        call1(MethodReference22::m3); //ok
        SAM1 s4 = MethodReference22::m4; //fail
        call1(MethodReference22::m4); //fail
    }

    static void test2() {
        SAM2 s1 = MethodReference22::m1; //ok
        call2(MethodReference22::m1); //ok
        SAM2 s2 = MethodReference22::m2; //ok
        call2(MethodReference22::m2); //ok
        SAM2 s3 = MethodReference22::m3; //fail
        call2(MethodReference22::m3); //fail
        SAM2 s4 = MethodReference22::m4; //fail
        call2(MethodReference22::m4); //fail
    }

    static void test3() {
        call3(MethodReference22::m1); //ok
        call3(MethodReference22::m2); //ambiguous
        call3(MethodReference22::m3); //ok
        call3(MethodReference22::m4); //fail
    }
}
