/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *   This test is to verify invalid lambda expressions
 * @compile/fail/ref=LambdaTest1_neg1.out -XDrawDiagnostics LambdaTest1_neg1.java
 */

import java.util.Comparator;

public class LambdaTest1_neg1 {
    void method() {
        Comparator<Number> c = (Number n1, Number n2) -> { 42; } //compile error, not a statement
    }
}
