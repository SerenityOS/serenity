/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *   This test is to verify mis-use of capturing local variable within lambda expression
 * @compile/fail/ref=LambdaTest1_neg3.out -XDrawDiagnostics LambdaTest1_neg3.java
 */

public class LambdaTest1_neg3 {
    void method() {
        int n = 2; //effectively final variable
        ((Runnable)
            ()-> {
                int n2 = n; //inside lambda accessing effectively final variable;
            }
        ).run();
        n++; //compile error if n is modified
    }
}
