/*
 * @test  /nodynamiccopyright/
 * @bug 8201281
 * @summary Truncated error message with Incompatible : null
 * @compile/fail/ref=NullInErrorMessageTest.out -XDrawDiagnostics NullInErrorMessageTest.java
 */

import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

public class NullInErrorMessageTest {
    private CompletionStage<String> test() {
        return null;
    }

    private CompletionStage<Integer> test2() {
        return null;
    }

    public static <T> Function<Throwable, T> test3() {
        return null;
    }

    private Supplier<CompletionStage<Integer>> createSupplier() {
        return () -> test().thenCompose(value -> {
            return () -> test2().exceptionally(test3());
        });
    }
}
