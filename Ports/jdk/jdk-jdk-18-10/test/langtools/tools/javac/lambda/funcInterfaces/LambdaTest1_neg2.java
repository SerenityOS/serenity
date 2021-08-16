/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *   This test is to verify mis-use of accessing "this" from within lambda expression
 * @compile/fail/ref=LambdaTest1_neg2.out -XDrawDiagnostics LambdaTest1_neg2.java
 */

public class LambdaTest1_neg2 {
    static void method() {
        ((Runnable)
            ()-> {
                Object o = this; //use "this" inside lambda expression which is inside a static method, not allowed
            }
        ).run();
    }
}
