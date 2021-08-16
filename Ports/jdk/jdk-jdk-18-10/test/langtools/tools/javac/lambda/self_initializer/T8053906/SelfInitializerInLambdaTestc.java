/*
 * @test /nodynamiccopyright/
 * @bug 8053906
 * @summary javac, some lambda programs are rejected by flow analysis
 * @compile/fail/ref=SelfInitializerInLambdaTestc.out -XDrawDiagnostics SelfInitializerInLambdaTestc.java
 */

public class SelfInitializerInLambdaTestc {
    interface SAM {
        void foo();
    }

    final SAM notInitialized = ()-> {
        SAM simpleVariable = () -> notInitialized.foo();
    };

    final SAM notInitialized2 = ()-> {
        SAM simpleVariable1 = () -> {
            SAM simpleVariable2 = () -> {
                SAM simpleVariable3 = () -> {
                    SAM simpleVariable4 = () -> notInitialized2.foo();
                };
            };
        };
    };
}
