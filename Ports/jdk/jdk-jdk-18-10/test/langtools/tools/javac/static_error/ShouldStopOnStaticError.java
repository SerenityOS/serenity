/**
 * @test /nodynamiccopyright/
 * @bug 8238213
 * @summary Method resolution should stop on static error
 * @compile/fail/ref=ShouldStopOnStaticError.out -XDrawDiagnostics ShouldStopOnStaticError.java
 */

public class ShouldStopOnStaticError {
    static void foo() {
        test1(5.0);
        test2((Double)5.0);
    }

    void test1(double d) {}
    void test1(Double d) {}

    void test2(Number n) {}
    static void test2(Double... d) {}
}
