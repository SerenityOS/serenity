/*
 * @test /nodynamiccopyright/
 * @bug 8003280 8064365 8183126
 * @summary Add lambda tests
 *  simple test for lambda candidate check
 * @compile/fail/ref=LambdaConv18.out -XDrawDiagnostics LambdaConv18.java
 */

class LambdaConv18 {

    interface NonSAM {
        void m1();
        void m2();
    }

    NonSAM s1 = new NonSAM() { public void m1() {}
                              public void m2() {} };
    NonExistent s2 = new NonExistent() { public void m() {} };
}
