/*
 * @test /nodynamiccopyright/
 * @bug 8206986
 * @summary Verify behavior of not exhaustive switch expressions.
 * @compile/fail/ref=ExpressionSwitchNotExhaustive.out -XDrawDiagnostics ExpressionSwitchNotExhaustive.java
 */

public class ExpressionSwitchNotExhaustive {
    private String print(int i) {
        return switch (i) {
            case 42 -> "42";
            case 43 -> "43";
        };
    }
    private String e(E e) {
        return switch (e) {
            case A -> "42";
        };
    }
    private String f(int i, E e) {
        return switch (i) {
            case 0:
                String s;
                switch (e) {
                    case A:
                        s = "42";
                        break;
                }
                yield s;
            default:
                yield "43";
        };
    }
    enum E {
        A, B;
    }
}
