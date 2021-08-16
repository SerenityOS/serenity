/* @test /nodynamiccopyright/
 * @bug 7196163
 * @summary Verify that an improper combination of modifiers and variable is rejected
 *          in an operand to try-with-resources
 * @compile/fail/ref=TwrForVariable2.out -XDrawDiagnostics -Xlint:-options TwrForVariable2.java
 */
public class TwrForVariable2 implements AutoCloseable {
    public static void main(String... args) {
        TwrForVariable2 v = new TwrForVariable2();
        TwrForVariable3[] v2 = new TwrForVariable3[1];
        TwrForVariable3[][] v3 = new TwrForVariable3[1][1];

        try (final v) {
            fail("no modifiers before variables");
        }
        try (@Deprecated v) {
            fail("no annotations before variables");
        }
        try (v;;) {
            fail("illegal double semicolon");
        }
        try ((v)) {
            fail("parentheses not allowed");
        }
        try (v2[0]) {
            fail("array access not allowed");
        }
        try (v3[0][0]) {
            fail("array access not allowed");
        }
        try (args.length == 0 ? v : v) {
            fail("general expressions not allowed");
        }
        try ((TwrForVariable2)null) {
            fail("null as variable is not allowed");
        }
    }

    static void fail(String reason) {
        throw new RuntimeException(reason);
    }

    public void close() {
    }

}
