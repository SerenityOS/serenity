/*
 * @test /nodynamiccopyright/
 * @bug 8206986
 * @summary Verify cases with multiple labels work properly.
 * @compile/fail/ref=MultipleLabelsStatement-old.out -source 9 -Xlint:-options -XDrawDiagnostics MultipleLabelsStatement.java
 * @compile MultipleLabelsStatement.java
 * @run main MultipleLabelsStatement
 */

import java.util.Objects;
import java.util.function.Function;

public class MultipleLabelsStatement {
    public static void main(String... args) {
        new MultipleLabelsStatement().run();
    }

    private void run() {
        runTest(this::statement1);
    }

    private void runTest(Function<T, String> print) {
        check(T.A,  print, "A");
        check(T.B,  print, "B-C");
        check(T.C,  print, "B-C");
        check(T.D,  print, "D");
        check(T.E,  print, "other");
    }

    private String statement1(T t) {
        String res;

        switch (t) {
            case A: res = "A"; break;
            case B, C: res = "B-C"; break;
            case D: res = "D"; break;
            default: res = "other"; break;
        }

        return res;
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
