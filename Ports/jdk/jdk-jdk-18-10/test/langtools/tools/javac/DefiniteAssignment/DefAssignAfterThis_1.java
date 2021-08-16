/*
 * @test /nodynamiccopyright/
 * @bug 4087865 4277291
 * @summary Verify definite assignment of blank finals after 'this(...)'
 * @author William Maddox (maddox)
 *
 * @compile/fail/ref=DefAssignAfterThis_1.out -XDrawDiagnostics  DefAssignAfterThis_1.java
 */

public class DefAssignAfterThis_1 {

    final int x;

    DefAssignAfterThis_1() {
        this(0);
        x = 1;          // ERROR -- duplicate assignment to blank final
    }

    DefAssignAfterThis_1(int i) {
        x = 1;
    }
}
