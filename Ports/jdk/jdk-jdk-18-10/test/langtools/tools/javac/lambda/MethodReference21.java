/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that erroneous method references are flagged with errors as expected
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=MethodReference21.out -XDrawDiagnostics MethodReference21.java
 */

class MethodReference21 {

    interface SAM {
        void m();
    }

    void call(SAM s) {}

    SAM s = NonExistentType::m;

    {
        call(NonExistentType::m);
    }
}
