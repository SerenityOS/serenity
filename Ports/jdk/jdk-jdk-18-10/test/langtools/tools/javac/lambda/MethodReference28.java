/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that non-compatible method references are rejected
 * @compile/fail/ref=MethodReference28.out -XDrawDiagnostics MethodReference28.java
 */

class MethodReference28 {

    interface SAM1 {
        void m(int i);
    }

    interface SAM2 {
        void m(MethodReference28 rec, int i);
    }

    static void static_m1(Integer i) { } //ok - boxing
    static void static_m2(Integer i1, Integer i2) { } //wrong arity
    static void static_m3(String s) { } //type mismatch
    static void static_m4(String... ss) { } //type mismatch - varargs

    void m1(Integer i) { } //ok - boxing
    void m2(Integer i1, Integer i2) { } //wrong arity
    void m3(String s) { } //type mismatch
    void m4(String... ss) { } //type mismatch - varargs

    static void testStatic() {
        SAM1 s1 = MethodReference28::static_m1;
        SAM1 s2 = MethodReference28::static_m2;
        SAM1 s3 = MethodReference28::static_m3;
        SAM1 s4 = MethodReference28::static_m4;
    }

    void testBadMember() {
        SAM1 s1 = MethodReference28::m1;
        SAM1 s2 = MethodReference28::m2;
        SAM1 s3 = MethodReference28::m3;
        SAM1 s4 = MethodReference28::m4;
    }

    void testMember() {
        SAM1 s1 = this::m1;
        SAM1 s2 = this::m2;
        SAM1 s3 = this::m3;
        SAM1 s4 = this::m4;
    }

    static void testUnbound() {
        SAM2 s1 = MethodReference28::m1;
        SAM2 s2 = MethodReference28::m2;
        SAM2 s3 = MethodReference28::m3;
        SAM2 s4 = MethodReference28::m4;
    }
}
