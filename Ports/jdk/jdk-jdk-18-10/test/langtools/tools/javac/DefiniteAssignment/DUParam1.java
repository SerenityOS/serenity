/*
 * @test /nodynamiccopyright/
 * @bug 4533915
 * @summary javac should not analyze final parameters for definite assignment status
 * @author Neal Gafter (gafter)
 *
 * @compile/fail/ref=DUParam1.out -XDrawDiagnostics  DUParam1.java
 */

public class DUParam1 {
    public static void main(final String[] args) {
        // 8.4.1 makes it illegal to assign to a final parameter.
        if (false) args = null;
    }
}
