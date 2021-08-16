/*
 * @test /nodynamiccopyright/
 * @bug 8211102
 * @summary Ensure javac does not crash for (invalid) lambda in standalone conditional expression.
 * @compile/fail/ref=NoTargetLambda.out -XDrawDiagnostics NoTargetLambda.java
 */

public class NoTargetLambda {
    private void t(boolean b) {
        (b ? "" : () -> { return null; }).toString();
    }
}
