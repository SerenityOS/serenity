/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that erroneous method references are flagged with errors as expected
 * @compile/fail/ref=MethodReference50.out -XDrawDiagnostics MethodReference50.java
 */

class MethodReference50 {

    interface SAM1 {
        void m();
    }

    interface SAM2 {
        void m();
    }

    void call(SAM1 s) {}
    void call(SAM2 s) {}

    {
        call(NonExistentType::m);
    }
}
