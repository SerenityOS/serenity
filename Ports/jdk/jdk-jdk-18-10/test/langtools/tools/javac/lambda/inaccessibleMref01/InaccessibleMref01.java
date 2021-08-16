/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that classfiles with member ref CP entries are read correctly
 * @compile/fail/ref=InaccessibleMref01.out -XDrawDiagnostics InaccessibleMref01.java
 */
class InaccessibleMref01 {
    interface SAM {
        void m();
    }

    void test(p1.C c) {
        SAM s = c::m;
    }
}
