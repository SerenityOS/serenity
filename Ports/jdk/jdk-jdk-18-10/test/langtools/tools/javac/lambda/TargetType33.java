/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  crash when incompatible method reference is found in conditional expression
 * @compile/fail/ref=TargetType33.out -XDrawDiagnostics TargetType33.java
 */

class TargetType33 {

    interface A<X> {
        X m();
    }

    void m(A<Integer> a) { }
    <Z> void m2(A<Z> a) { }

    int intRes(Object o) { return 42; }

    void testMethodRef(boolean flag) {
        A<Integer> c = flag ? this::intRes : this::intRes;
        m(flag ? this::intRes : this::intRes);
        m2(flag ? this::intRes : this::intRes);
    }
}
