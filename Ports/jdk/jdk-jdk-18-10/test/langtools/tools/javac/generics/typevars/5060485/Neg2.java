/*
 * @test    /nodynamiccopyright/
 * @bug     5060485
 * @summary The scope of a class type parameter is too wide
 * @author  Peter von der Ah\u00e9
 * @compile/fail/ref=Neg2.out -XDrawDiagnostics  Neg2.java
 */

public class Neg2<X extends X> {
}
