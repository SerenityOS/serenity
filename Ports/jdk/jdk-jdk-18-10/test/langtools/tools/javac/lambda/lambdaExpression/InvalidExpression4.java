/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *   Test invalid lambda expressions
 * @compile/fail/ref=InvalidExpression4.out -XDrawDiagnostics InvalidExpression4.java
 */

public class InvalidExpression4 {

    interface SAM {
        void m(int i);
    }

    void test() {
        SAM s = (Integer i) -> { }; //parameters not match, boxing not allowed here
    }
}
