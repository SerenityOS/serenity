/*
 * @test /nodynamiccopyright/
 * @bug 8230105
 * @summary Verify the analyzers work reasonably for stuck lambdas
 * @compile/ref=StuckLambdas.out -XDfind=local -XDrawDiagnostics StuckLambdas.java
 */

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.*;
import java.util.stream.*;

abstract class X {
    public interface N<K, V> {
        Stream<V> getValues();
    }

    abstract <K, V> N<K, V> c();

    abstract <T, K, V, M extends N<K, V>> Collector<T, ?, M> f(
            Function<? super T, ? extends K> k,
            Function<? super T, ? extends Stream<? extends V>> v,
            Supplier<M> multimapSupplier);

    void m(Map<String, N<?, ?>> c, ExecutorService s) {
        s.submit(() -> {
            String s1 = "";
            return c.entrySet()
                    .parallelStream()
                    .collect(f(Map.Entry::getKey, e -> {String s2 = ""; return e.getValue().getValues();}, this::c));
        });
    }
}

