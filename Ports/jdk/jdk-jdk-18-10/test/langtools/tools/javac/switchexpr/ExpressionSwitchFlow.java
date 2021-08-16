/*
 * @test /nodynamiccopyright/
 * @bug 8212982
 * @summary Verify a compile-time error is produced if switch expression does not provide a value
 * @compile/fail/ref=ExpressionSwitchFlow.out -XDrawDiagnostics ExpressionSwitchFlow.java
 */

public class ExpressionSwitchFlow {
    private String test1(int i) {
        return switch (i) {
            case 0 -> {}
            default -> { yield "other"; }
        };
    }
    private String test2(int i) {
        return switch (i) {
            case 0 -> {
            }
            default -> "other";
        };
    }
    private String test3(int i) {
        return switch (i) {
            case 0 -> {}
            case 1 -> "";
            default -> throw new IllegalStateException();
        };
    }
    private String test4(int i) {
        return switch (i) {
            case 0 -> { yield "other"; }
            default -> {}
        };
    }
    private String test5(int i) {
        return switch (i) {
            case 0 -> "other";
            default -> {}
        };
    }
    private String test6(int i) {
        return switch (i) {
            case 0 -> throw new IllegalStateException();
            case 1 -> "";
            default -> {}
        };
    }
    private String test7(int i) {
        return switch (i) {
            case 0: throw new IllegalStateException();
            case 1: yield "";
            default:
        };
    }
    private String test8(int i) {
        return switch (i) {
            case 1: yield "";
            case 0: i++;
            default: {
            }
        };
    }
    private String test9(int i) {
        return switch (i) {
            case 1: yield "";
            case 0:
            default:
                System.err.println();
        };
    }
}
