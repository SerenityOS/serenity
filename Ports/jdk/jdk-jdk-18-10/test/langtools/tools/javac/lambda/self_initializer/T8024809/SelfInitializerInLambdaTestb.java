/*
 * @test /nodynamiccopyright/
 * @bug 8024809
 * @summary javac, some lambda programs are rejected by flow analysis
 * @compile/fail/ref=SelfInitializerInLambdaTestb.out -XDrawDiagnostics SelfInitializerInLambdaTestb.java
 */

public class SelfInitializerInLambdaTestb {

    final Runnable r1;

    final Runnable r2 = ()-> System.out.println(r1);

    SelfInitializerInLambdaTestb() {
        r1 = ()->System.out.println(r1);
    }
}
