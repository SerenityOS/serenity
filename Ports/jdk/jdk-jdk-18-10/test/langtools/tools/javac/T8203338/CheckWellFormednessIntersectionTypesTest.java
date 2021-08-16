/*
 * @test  /nodynamiccopyright/
 * @bug 8203338
 * @summary Unboxing in return from lambda miscompiled to throw ClassCastException
 * @compile/fail/ref=CheckWellFormednessIntersectionTypesTest.out -XDrawDiagnostics CheckWellFormednessIntersectionTypesTest.java
 */

public class CheckWellFormednessIntersectionTypesTest {
    class U1 {}
    class U3 {}

    class X1 extends U1 {}
    class X3 extends U3 {}

    interface SAM<P1 extends X1, P2 extends P1, P3 extends X3> {
        P3 m(P1 p1, P2 p2);
    }

    interface I<T> {}

    @SuppressWarnings("unchecked")
    class Tester {
        public X3 foo(X1 x1, Object x2) { return new X3(); }
        Object method(SAM<?, ?, ?> sam) {
            return sam.m(null, null);
        }

        Object foo() {
            return method((SAM<?, ?, ?> & I<?>) this::foo);
        }
    }
}
