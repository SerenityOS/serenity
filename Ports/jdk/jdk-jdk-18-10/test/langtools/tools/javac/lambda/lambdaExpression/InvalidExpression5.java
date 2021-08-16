/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *   Test invalid lambda expressions
 * @compile/fail/ref=InvalidExpression5.out -XDrawDiagnostics InvalidExpression5.java
 */

public class InvalidExpression5 {

    void test() {
        Object o = (int n) -> { }; // Invalid target type
    }
}
