/*
 * @test  /nodynamiccopyright/
 * @bug     6330920
 * @summary Verify that javac doesn't duplicate method error on method with error
 * @author  Peter von der Ah\u00e9
 * @compile/fail/ref=T6330920.out -XDrawDiagnostics T6330920.java
 */

public class T6330920 {
    public void test(T6330920 x) {}
    public void test(T6330920Missing x) {}
}
