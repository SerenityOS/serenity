/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *   This test is for identifying SAM types #5 and instantiating non-SAM types #7 through inner class,
             see Helper.java for SAM types
 * @compile/fail/ref=LambdaTest2_neg1.out -XDrawDiagnostics LambdaTest2_neg1.java Helper.java
 */

public class LambdaTest2_neg1 {

    public static void main(String[] args) {
        LambdaTest2_neg1 test = new LambdaTest2_neg1();
        //not convertible - QooRoo is not a SAM
        test.methodQooRoo((Integer i) -> { });
    }

    void methodQooRoo(QooRoo<Integer, Integer, Void> qooroo) { }
}
