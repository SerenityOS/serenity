/*
 * @test /nodynamiccopyright/
 * @bug 4704365
 * @summary definite assignment status within the case expression
 * @author Neal Gafter (gafter)
 *
 * @compile/fail/ref=T4704365.out -XDrawDiagnostics  T4704365.java
 */

class T4704365 {
    T4704365() {
        switch (1) {
        case 0:
            final int i = 3; // line 1
            break;
        case i: // error: i not definitely assigned
            break;
        }
    }
}
