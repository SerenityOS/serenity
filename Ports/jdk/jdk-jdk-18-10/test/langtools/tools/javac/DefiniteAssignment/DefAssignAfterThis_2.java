/*
 * @test /nodynamiccopyright/
 * @bug 4087865
 * @summary Verify definite assignment of blank finals after 'this(...)'
 * @author William Maddox (maddox)
 *
 * @compile DefAssignAfterThis_2.java
 */

/*
 * This program should compile without errors.
 */

public class DefAssignAfterThis_2 {

    final int x;

    DefAssignAfterThis_2() {
        this(0);
        // 'x' should be definitely assigned here
    }

    DefAssignAfterThis_2(int i) {
        x = 1;
    }
}
