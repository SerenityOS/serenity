/* @test /nodynamiccopyright/
 * @bug 7196163
 * @summary Verify that variable used as an operand to try-with-resources is rejected if it is not
 *          definitelly assigned before use and or not final or effectivelly final.
 * @compile/fail/ref=TwrForVariable4.out -XDrawDiagnostics -Xlint:-options TwrForVariable4.java
 */
public class TwrForVariable4 implements AutoCloseable {
    public static void main(String... args) {
        TwrForVariable4 uninitialized;

        try (uninitialized) {
            fail("must be initialized before use");
        }
        uninitialized = new TwrForVariable4();

        TwrForVariable4 notEffectivellyFinal1 = new TwrForVariable4();

        notEffectivellyFinal1 = new TwrForVariable4();

        try (notEffectivellyFinal1) {
            fail("not effectivelly final");
        }

        TwrForVariable4 notEffectivellyFinal2 = new TwrForVariable4();

        try (notEffectivellyFinal2) {
            notEffectivellyFinal2 = new TwrForVariable4();
            fail("not effectivelly final");
        }

        try (notFinal) {
            fail("not final");
        }
    }

    static TwrForVariable4 notFinal = new TwrForVariable4();

    static void fail(String reason) {
        throw new RuntimeException(reason);
    }

    public void close() {
    }

}
