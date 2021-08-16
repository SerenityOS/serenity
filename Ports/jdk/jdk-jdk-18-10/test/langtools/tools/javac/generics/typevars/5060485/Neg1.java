/*
 * @test    /nodynamiccopyright/
 * @bug     5060485
 * @summary The scope of a class type parameter is too wide
 * @author  Peter von der Ah\u00e9
 * @compile/fail/ref=Neg1.out -XDrawDiagnostics  Neg1.java
 */

public class Neg1<X extends Y> {
    public static class Y {}
}
