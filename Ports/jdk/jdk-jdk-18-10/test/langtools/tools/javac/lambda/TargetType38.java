/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that cast conversion context does not affect compatibility of lambda
 * @compile/fail/ref=TargetType38.out -XDrawDiagnostics TargetType38.java
 */
class TargetType38 {

    interface I { }

    interface SAM {
        I m();
    }

    static Object m() { return null; }

    void test() {
        Object o1 = (SAM)()->new Object();
        Object o2 = (SAM)TargetType38::m;
    }
}
