/*
 * @test /nodynamiccopyright/
 * @bug 8206986
 * @summary Verify cases with multiple labels work properly.
 * @compile/fail/ref=MultipleLabelsExpression-old.out -source 9 -Xlint:-options -XDrawDiagnostics MultipleLabelsExpression.java
 * @compile MultipleLabelsExpression.java
 * @run main MultipleLabelsExpression
 */

import java.util.Objects;
import java.util.function.Function;

public class MultipleLabelsExpression {
    public static void main(String... args) {
        new MultipleLabelsExpression().run();
    }

    private void run() {
        runTest(this::expression1);
    }

    private void runTest(Function<T, String> print) {
        check(T.A,  print, "A");
        check(T.B,  print, "B-C");
        check(T.C,  print, "B-C");
        check(T.D,  print, "D");
        check(T.E,  print, "other");
    }

    private String expression1(T t) {
        return switch (t) {
            case A -> "A";
            case B, C -> { yield "B-C"; }
            case D -> "D";
            default -> "other";
        };
    }

    private void check(T t, Function<T, String> print, String expected) {
        String result = print.apply(t);
        if (!Objects.equals(result, expected)) {
            throw new AssertionError("Unexpected result: " + result);
        }
    }

    enum T {
        A, B, C, D, E;
    }
}
