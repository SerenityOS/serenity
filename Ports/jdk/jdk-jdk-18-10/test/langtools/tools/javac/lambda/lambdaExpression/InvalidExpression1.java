/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *   Test invalid lambda expressions
 * @compile/fail/ref=InvalidExpression1.out -XDrawDiagnostics InvalidExpression1.java
 */

import java.util.Comparator;

public class InvalidExpression1 {

    void test() {
        Comparator<Number> c = (Number n1, Number n2)-> { 42; }; //not a statement
        Comparator<Number> c = (Number n1, Number n2)-> { return 42 }; //";" expected
    }
}
