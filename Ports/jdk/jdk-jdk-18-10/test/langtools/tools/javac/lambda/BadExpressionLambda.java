/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that a conditonal can't be void
 * @compile/fail/ref=BadExpressionLambda.out -XDrawDiagnostics BadExpressionLambda.java
 */

class BadExpressionLambda {

    interface SAM {
        void invoke();
    }

    public static void m() {}

    void test() {
        SAM sam1 = () -> m(); //ok
        SAM sam2 = () -> true ? m() : m(); //not ok
    }
}
