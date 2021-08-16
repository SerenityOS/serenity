/*
 * @test    /nodynamiccopyright/
 * @bug     4848619
 * @summary static final variable declared after use and self initialized
 * @author  Peter von der Ah\u00e9
 * @compile/fail/ref=T4848619b.out -XDrawDiagnostics T4848619b.java
 */

public class T4848619b {
    int getX() { return y; }
    int y = y;
}
