/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that lambda features are not enabled with source < 8
 * @compile/fail/ref=SourceLevelTest.out -XDrawDiagnostics -source 7 -Xlint:-options SourceLevelTest.java
 */

class SourceLevelTest {
    interface I {
        default void m() { SourceLevelTest.impl(this); }
    }

    interface SAM {
        void m();
    }

    SAM s1 = () -> { };
    SAM s2 = this::m;

    static void impl(I i) {}
    void m() {}
}
