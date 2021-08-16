/*
 * @test    /nodynamiccopyright/
 * @bug     6306967
 * @summary Variable x is used before initialized
 * @author  Wei Tao
 * @compile/fail/ref=T6306967.out -XDrawDiagnostics T6306967.java
 */

public class T6306967 {
    public static void main(String[] args) {
        final int x;
        while(true) {
            if (true) {
                break;
            }
            x = 1;
        }
        System.out.println(x);
    }
}
