/*
 * @test /nodynamiccopyright/
 * @bug 4533915
 * @summary javac should not analyze final parameters for definite assignment status
 * @author Neal Gafter (gafter)
 *
 * @compile/fail/ref=DUParam2.out -XDrawDiagnostics  DUParam2.java
 */

public class DUParam2 {
    public static void main(String[] args) {
        try {
        } catch (final Exception e) {
            // 14.19 makes it illegal to assign to a final exception parameter.
            if (false) e = null;
        }
    }
}
