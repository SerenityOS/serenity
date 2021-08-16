/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  the case in which no member reference is found is now treated as a normal error (not dependent on target-type)
 * @compile/fail/ref=MethodReference53.out -XDrawDiagnostics MethodReference53.java
 */
class MethodReference53 {

    interface SAM1 {
        void m(int i);
    }

    interface SAM2 {
        void m(long i);
    }

    void m(SAM1 s1) { }
    void m(SAM2 s1) { }

    void test() {
        m(this::unknown); //should not generate outer resolution diagnostic
    }
}
