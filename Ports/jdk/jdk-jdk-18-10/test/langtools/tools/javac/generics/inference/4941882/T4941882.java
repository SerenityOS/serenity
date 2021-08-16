/*
 * @test    /nodynamiccopyright/
 * @bug     4941882 8078024
 * @summary incorrect inference for result of lub(int[], float[])
 * @compile/fail/ref=T4941882.out -XDrawDiagnostics  T4941882.java
 */

public class T4941882 {
    static <T> T f(T a, T b) {
        return a;
    }
    static Object[] main(int[] a, float[] b) {
        return f(a, b);
    }
}
