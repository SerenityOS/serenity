/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  The qualifier type of a constructor reference must be a concrete class
 * @compile/fail/ref=MethodReference38.out -XDrawDiagnostics MethodReference38.java
 */

class MethodReference38 {

    interface SAM<R> {
        R invoke();
    }

    @interface A { }

    interface I { }

    static abstract class AC { }

    enum E { }

    void test() {
        SAM s1 = A::new;
        SAM s2 = I::new;
        SAM s3 = AC::new;
        SAM s4 = E::new;
    }
}
