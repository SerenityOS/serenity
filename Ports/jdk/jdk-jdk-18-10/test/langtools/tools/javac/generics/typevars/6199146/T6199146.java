/*
 * @test    /nodynamiccopyright/
 * @bug     6199146
 * @summary Javac accepts ambiguous compound type
 * @compile/fail/ref=T6199146.out -XDrawDiagnostics  T6199146.java
 */

public class T6199146 {
    static class Test2 <T extends I1 & I2> { }

    static interface I1 {
        int getFoo();
    }

    static interface I2 {
        float getFoo();
    }
}
