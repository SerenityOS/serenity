/*
 * @test    /nodynamiccopyright/
 * @bug     5082929
 * @summary Comparing Float and Integer
 * @author  Peter von der Ah\u00e9
 * @compile/fail/ref=T5082929.out -XDrawDiagnostics  T5082929.java
 */

public class T5082929 {
    void test(Float f, Integer i) {
        boolean b = f == i;
    }
}
