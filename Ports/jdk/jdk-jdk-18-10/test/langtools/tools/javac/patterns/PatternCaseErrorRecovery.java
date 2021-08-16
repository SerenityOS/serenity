/*
 * @test /nodynamiccopyright/
 * @bug 8268859
 * @summary Verify error recovery/disambiguation of case labels that mix expressions and patterns
 * @compile/fail/ref=PatternCaseErrorRecovery.out --enable-preview -source ${jdk.version} -XDrawDiagnostics PatternCaseErrorRecovery.java
 */

public class PatternCaseErrorRecovery {
    Object expressionLikeType(Object o1, Object o2) {
        final int a = 1;
        final int b = 2;
        return switch (o1) {
            case true t -> o2;
            case 1 + 1 e -> o2;
            case a < b ? a : b e -> o2;
            default -> null;
        };
    }
}
