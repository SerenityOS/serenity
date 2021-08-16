/*
 * @test /nodynamiccopyright/
 * @bug 8222035
 * @summary minimal inference context optimization is forcing resolution with incomplete constraints
 * @compile/fail/ref=MinContextOpTest_A.out -XDrawDiagnostics -source 15 MinContextOpTest.java
 * @compile/fail/ref=MinContextOpTest_B.out -XDrawDiagnostics MinContextOpTest.java
 */

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

public class MinContextOpTest {
    abstract class A {
        abstract static class T<K> {
            abstract String f();
        }

        abstract <E> Function<E, E> id();

        abstract static class ImmutableMap<K, V> implements Map<K, V> {}

        abstract <T, K, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(
                Function<? super T, ? extends K> k, Function<? super T, ? extends V> v);

        ImmutableMap<String, T<?>> test(Stream<T> stream) {
            return stream.collect(toImmutableMap(T::f, id()));
        }
    }
}
