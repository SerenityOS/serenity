/*
 * @test /nodynamiccopyright/
 * @bug 4695463
 * @summary DA versus switch: javac allows reference to uninitialized variable
 * @author Neal Gafter (gafter)
 *
 * @compile/fail/ref=DASwitch.out -XDrawDiagnostics  DASwitch.java
 */

public class DASwitch {
    public static void main(final String[] args) {
        int t = 1;
        {
            final int x;
            x = 1;
        }
        switch(t) {
        case 0:
            Integer b;
            break;
        case 1:
            System.out.println(b.toString());
        }
    }
}
