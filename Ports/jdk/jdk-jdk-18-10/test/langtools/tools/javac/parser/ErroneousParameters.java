/*
 * @test /nodynamiccopyright/
 * @bug 8030091 8031383
 * @summary Producing reasonable errors for unexpected tokens in method parameters
 * @compile/fail/ref=ErroneousParameters.out -XDrawDiagnostics ErroneousParameters.java
 */

public class ErroneousParameters {

    public static void test(int... extraVarArg, int additionalParam) { }
    public static void test(byte param...) { }
    public static void test(char param,) { }
    public static void test(short param[) { }
    public static void test(int param=) { }

}
