/*
 * @test /nodynamiccopyright/
 * @bug 8017618
 * @summary NullPointerException in RichDiagnosticFormatter for bad input program
 * @compile/fail/ref=BadNestedLambda.out -XDrawDiagnostics BadNestedLambda.java
 */
class BadNestedLambda {
    void test() {
        Runnable add = (int x) -> (int y) -> x + y;
    }
}
