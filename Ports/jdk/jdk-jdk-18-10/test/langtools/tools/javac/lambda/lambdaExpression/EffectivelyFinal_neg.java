/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *   Negative test of capture of "effectively final" local variable in lambda expressions
 * @compile/fail/ref=EffectivelyFinal_neg.out -XDrawDiagnostics EffectivelyFinal_neg.java
 */

public class EffectivelyFinal_neg {

    void test() {
        String s = "a";
        String s2 = "a";
        int n = 1;
        ((Runnable)
            ()-> {
                s2 = "b"; //re-assign illegal here
                System.out.println(n);
                System.out.println(s);
                s = "b"; // not effectively final
            }
        ).run();
        n = 2; // not effectively final
    }
}
