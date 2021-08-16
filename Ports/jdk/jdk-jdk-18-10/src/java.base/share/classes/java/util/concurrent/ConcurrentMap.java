/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A {@link Map} providing thread safety and atomicity guarantees.
 *
 * <p>To maintain the specified guarantees, default implementations of
 * methods including {@link #putIfAbsent} inherited from {@link Map}
 * must be overridden by implementations of this interface. Similarly,
 * implementations of the collections returned by methods {@link
 * #keySet}, {@link #values}, and {@link #entrySet} must override
 * methods such as {@code removeIf} when necessary to
 * preserve atomicity guarantees.
 *
 * <p>Memory consistency effects: As with other concurrent
 * collections, actions in a thread prior to placing an object into a
 * {@code ConcurrentMap} as a key or value
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions subsequent to the access or removal of that object from
 * the {@code ConcurrentMap} in another thread.
 *
 * <p>This interface is a member of the
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 * Java Collections Framework</a>.
 *
 * @since 1.5
 * @author Doug Lea
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public interface ConcurrentMap<K,V> extends Map<K,V> {

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation assumes that the ConcurrentMap cannot
     * contain null values and {@code get()} returning null unambiguously means
     * the key is absent. Implementations which support null values
     * <strong>must</strong> override this default implementation.
     *
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default V getOrDefault(Object key, V defaultValue) {
        V v;
        return ((v = get(key)) != null) ? v : defaultValue;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec The default implementation is equivalent to, for this
     * {@code map}:
     * <pre> {@code
     * for (Map.Entry<K,V> entry : map.entrySet()) {
     *   action.accept(entry.getKey(), entry.getValue());
     * }}</pre>
     *
     * @implNote The default implementation assumes that
     * {@code IllegalStateException} thrown by {@code getKey()} or
     * {@code getValue()} indicates that the entry has been removed and cannot
     * be processed. Operation continues for subsequent entries.
     *
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        for (Map.Entry<K,V> entry : entrySet()) {
            K k;
            V v;
            try {
                k = entry.getKey();
                v = entry.getValue();
            } catch (IllegalStateException ise) {
                // this usually means the entry is no longer in the map.
                continue;
            }
            action.accept(k, v);
        }
    }

    /**
     * If the specified key is not already associated
     * with a value, associates it with the given value.
     * This is equivalent to, for this {@code map}:
     * <pre> {@code
     * if (!map.containsKey(key))
     *   return map.put(key, value);
     * else
     *   return map.get(key);}</pre>
     *
     * except that the action is performed atomically.
     *
     * @implNote This implementation intentionally re-abstracts the
     * inappropriate default provided in {@code Map}.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with the key,
     *         if the implementation supports null values.)
     * @throws UnsupportedOperationException if the {@code put} operation
     *         is not supported by this map
     * @throws ClassCastException if the class of the specified key or value
     *         prevents it from being stored in this map
     * @throws NullPointerException if the specified key or value is null,
     *         and this map does not permit null keys or values
     * @throws IllegalArgumentException if some property of the specified key
     *         or value prevents it from being stored in this map
     */
    V putIfAbsent(K key, V value);

    /**
     * Removes the entry for a key only if currently mapped to a given value.
     * This is equivalent to, for this {@code map}:
     * <pre> {@code
     * if (map.containsKey(key)
     *     && Objects.equals(map.get(key), value)) {
     *   map.remove(key);
     *   return true;
     * } else {
     *   return false;
     * }}</pre>
     *
     * except that the action is performed atomically.
     *
     * @implNote This implementation intentionally re-abstracts the
     * inappropriate default provided in {@code Map}.
     *
     * @param key key with which the specified value is associated
     * @param value value expected to be associated with the specified key
     * @return {@code true} if the value was removed
     * @throws UnsupportedOperationException if the {@code remove} operation
     *         is not supported by this map
     * @throws ClassCastException if the key or value is of an inappropriate
     *         type for this map
     * (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified key or value is null,
     *         and this map does not permit null keys or values
     * (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     */
    boolean remove(Object key, Object value);

    /**
     * Replaces the entry for a key only if currently mapped to a given value.
     * This is equivalent to, for this {@code map}:
     * <pre> {@code
     * if (map.containsKey(key)
     *     && Objects.equals(map.get(key), oldValue)) {
     *   map.put(key, newValue);
     *   return true;
     * } else {
     *   return false;
     * }}</pre>
     *
     * except that the action is performed atomically.
     *
     * @implNote This implementation intentionally re-abstracts the
     * inappropriate default provided in {@code Map}.
     *
     * @param key key with which the specified value is associated
     * @param oldValue value expected to be associated with the specified key
     * @param newValue value to be associated with the specified key
     * @return {@code true} if the value was replaced
     * @throws UnsupportedOperationException if the {@code put} operation
     *         is not supported by this map
     * @throws ClassCastException if the class of a specified key or value
     *         prevents it from being stored in this map
     * @throws NullPointerException if a specified key or value is null,
     *         and this map does not permit null keys or values
     * @throws IllegalArgumentException if some property of a specified key
     *         or value prevents it from being stored in this map
     */
    boolean replace(K key, V oldValue, V newValue);

    /**
     * Replaces the entry for a key only if currently mapped to some value.
     * This is equivalent to, for this {@code map}:
     * <pre> {@code
     * if (map.containsKey(key))
     *   return map.put(key, value);
     * else
     *   return null;}</pre>
     *
     * except that the action is performed atomically.
     *
     * @implNote This implementation intentionally re-abstracts the
     * inappropriate default provided in {@code Map}.
     *
     * @param key key with which the specified value is associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with the key,
     *         if the implementation supports null values.)
     * @throws UnsupportedOperationException if the {@code put} operation
     *         is not supported by this map
     * @throws ClassCastException if the class of the specified key or value
     *         prevents it from being stored in this map
     * @throws NullPointerException if the specified key or value is null,
     *         and this map does not permit null keys or values
     * @throws IllegalArgumentException if some property of the specified key
     *         or value prevents it from being stored in this map
     */
    V replace(K key, V value);

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * <p>The default implementation is equivalent to, for this {@code map}:
     * <pre> {@code
     * for (Map.Entry<K,V> entry : map.entrySet()) {
     *   K k;
     *   V v;
     *   do {
     *     k = entry.getKey();
     *     v = entry.getValue();
     *   } while (!map.replace(k, v, function.apply(k, v)));
     * }}</pre>
     *
     * The default implementation may retry these steps when multiple
     * threads attempt updates including potentially calling the function
     * repeatedly for a given key.
     *
     * <p>This implementation assumes that the ConcurrentMap cannot contain null
     * values and {@code get()} returning null unambiguously means the key is
     * absent. Implementations which support null values <strong>must</strong>
     * override this default implementation.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        forEach((k,v) -> {
            while (!replace(k, v, function.apply(k, v))) {
                // v changed or k is gone
                if ( (v = get(k)) == null) {
                    // k is no longer in the map.
                    break;
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * The default implementation is equivalent to the following steps for this
     * {@code map}:
     *
     * <pre> {@code
     * V oldValue, newValue;
     * return ((oldValue = map.get(key)) == null
     *         && (newValue = mappingFunction.apply(key)) != null
     *         && (oldValue = map.putIfAbsent(key, newValue)) == null)
     *   ? newValue
     *   : oldValue;}</pre>
     *
     * <p>This implementation assumes that the ConcurrentMap cannot contain null
     * values and {@code get()} returning null unambiguously means the key is
     * absent. Implementations which support null values <strong>must</strong>
     * override this default implementation.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default V computeIfAbsent(K key,
            Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        V oldValue, newValue;
        return ((oldValue = get(key)) == null
                && (newValue = mappingFunction.apply(key)) != null
                && (oldValue = putIfAbsent(key, newValue)) == null)
            ? newValue
            : oldValue;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * The default implementation is equivalent to performing the following
     * steps for this {@code map}:
     *
     * <pre> {@code
     * for (V oldValue; (oldValue = map.get(key)) != null; ) {
     *   V newValue = remappingFunction.apply(key, oldValue);
     *   if ((newValue == null)
     *       ? map.remove(key, oldValue)
     *       : map.replace(key, oldValue, newValue))
     *     return newValue;
     * }
     * return null;}</pre>
     * When multiple threads attempt updates, map operations and the
     * remapping function may be called multiple times.
     *
     * <p>This implementation assumes that the ConcurrentMap cannot contain null
     * values and {@code get()} returning null unambiguously means the key is
     * absent. Implementations which support null values <strong>must</strong>
     * override this default implementation.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default V computeIfPresent(K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        for (V oldValue; (oldValue = get(key)) != null; ) {
            V newValue = remappingFunction.apply(key, oldValue);
            if ((newValue == null)
                ? remove(key, oldValue)
                : replace(key, oldValue, newValue))
                return newValue;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * The default implementation is equivalent to performing the following
     * steps for this {@code map}:
     *
     * <pre> {@code
     * for (;;) {
     *   V oldValue = map.get(key);
     *   V newValue = remappingFunction.apply(key, oldValue);
     *   if (newValue != null) {
     *     if ((oldValue != null)
     *       ? map.replace(key, oldValue, newValue)
     *       : map.putIfAbsent(key, newValue) == null)
     *       return newValue;
     *   } else if (oldValue == null || map.remove(key, oldValue)) {
     *     return null;
     *   }
     * }}</pre>
     * When multiple threads attempt updates, map operations and the
     * remapping function may be called multiple times.
     *
     * <p>This implementation assumes that the ConcurrentMap cannot contain null
     * values and {@code get()} returning null unambiguously means the key is
     * absent. Implementations which support null values <strong>must</strong>
     * override this default implementation.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default V compute(K key,
                      BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        retry: for (;;) {
            V oldValue = get(key);
            // if putIfAbsent fails, opportunistically use its return value
            haveOldValue: for (;;) {
                V newValue = remappingFunction.apply(key, oldValue);
                if (newValue != null) {
                    if (oldValue != null) {
                        if (replace(key, oldValue, newValue))
                            return newValue;
                    }
                    else if ((oldValue = putIfAbsent(key, newValue)) == null)
                        return newValue;
                    else continue haveOldValue;
                } else if (oldValue == null || remove(key, oldValue)) {
                    return null;
                }
                continue retry;
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * The default implementation is equivalent to performing the following
     * steps for this {@code map}:
     *
     * <pre> {@code
     * for (;;) {
     *   V oldValue = map.get(key);
     *   if (oldValue != null) {
     *     V newValue = remappingFunction.apply(oldValue, value);
     *     if (newValue != null) {
     *       if (map.replace(key, oldValue, newValue))
     *         return newValue;
     *     } else if (map.remove(key, oldValue)) {
     *       return null;
     *     }
     *   } else if (map.putIfAbsent(key, value) == null) {
     *     return value;
     *   }
     * }}</pre>
     * When multiple threads attempt updates, map operations and the
     * remapping function may be called multiple times.
     *
     * <p>This implementation assumes that the ConcurrentMap cannot contain null
     * values and {@code get()} returning null unambiguously means the key is
     * absent. Implementations which support null values <strong>must</strong>
     * override this default implementation.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default V merge(K key, V value,
            BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        Objects.requireNonNull(value);
        retry: for (;;) {
            V oldValue = get(key);
            // if putIfAbsent fails, opportunistically use its return value
            haveOldValue: for (;;) {
                if (oldValue != null) {
                    V newValue = remappingFunction.apply(oldValue, value);
                    if (newValue != null) {
                        if (replace(key, oldValue, newValue))
                            return newValue;
                    } else if (remove(key, oldValue)) {
                        return null;
                    }
                    continue retry;
                } else {
                    if ((oldValue = putIfAbsent(key, value)) == null)
                        return value;
                    continue haveOldValue;
                }
            }
        }
    }
}
