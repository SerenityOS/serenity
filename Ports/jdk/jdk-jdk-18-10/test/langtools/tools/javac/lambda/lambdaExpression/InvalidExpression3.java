/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *   Test invalid lambda expressions
 * @compile/fail/ref=InvalidExpression3.out -XDrawDiagnostics InvalidExpression3.java
 */

import java.util.Comparator;

public class InvalidExpression3 {

    void test() {
        Comparator<Integer> c2 = (Integer i1, Integer i2) -> { return "0"; }; //return type need to match
    }
}
