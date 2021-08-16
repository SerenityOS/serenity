/*
 * @test    /nodynamiccopyright/
 * @bug     4848619
 * @summary static final variable declared after use and self initialized
 * @author  Peter von der Ah\u00e9
 * @compile/fail/ref=T4848619a.out -XDrawDiagnostics T4848619a.java
 */

public class T4848619a {
    static void m() { System.err.println(X); };
    private static final String X = X;
}
