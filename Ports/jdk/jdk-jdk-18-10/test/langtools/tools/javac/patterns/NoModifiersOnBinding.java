/* @test /nodynamiccopyright/
 * @bug 8256266
 * @summary Binding variables cannot have (non-annotation) modifiers.
 * @compile/fail/ref=NoModifiersOnBinding.out -XDrawDiagnostics NoModifiersOnBinding.java
 */

public class NoModifiersOnBinding {

    private static void test(Object o) {
        if (o instanceof final String) {
            System.err.println(s);
        }
        if (o instanceof /**@deprecated*/ String) {
            System.err.println(s);
        }
        if (o instanceof static String s) {
            System.err.println(s);
        }
        if (o instanceof /**@deprecated*/ String s) {
            System.err.println(s);
        }
    }

}
