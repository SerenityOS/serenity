/* @test   /nodynamiccopyright/
 * @bug    8071453
 * @author sadayapalam
 * @summary Test that a lone private interface method cannot supply the SAM.
 * @compile/fail/ref=Private06.out -XDrawDiagnostics Private06.java
 */

public class Private06 {
    @FunctionalInterface
    interface NAFI {
        private void foo() {
        }
    }

    @FunctionalInterface
    interface FI {
        void foo(NAFI nafi);
    }

    public static void main(String [] args) {
        Private06.NAFI nafi = () -> {};
        Private06.FI fi = Private06.NAFI::foo; // OK.
    }
}

class Private06_01 {
    public static void main(String [] args) {
        Private06.FI fi = Private06.NAFI::foo; // NOT OK.
    }
}
