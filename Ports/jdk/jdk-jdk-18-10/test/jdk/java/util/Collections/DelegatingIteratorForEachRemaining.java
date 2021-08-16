/*
 * Copyright (c) 2018 Google Inc. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * @test
 * @run junit DelegatingIteratorForEachRemaining
 */

import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class DelegatingIteratorForEachRemaining {

    static abstract class ForwardingIterator<E> implements Iterator<E> {
        private final Iterator<E> delegate;

        protected ForwardingIterator(Iterator<E> delegate) {
            this.delegate = Objects.requireNonNull(delegate);
        }

        @Override public boolean hasNext() { return delegate.hasNext(); }
        @Override public E next() { return delegate.next(); }
        @Override public void remove() { delegate.remove(); }
        @Override public void forEachRemaining(Consumer<? super E> action) {
            delegate.forEachRemaining(action);
        }
    }

    static final class ThrowingIterator<E> extends ForwardingIterator<E> {
        public ThrowingIterator(Iterator<E> delegate) {
            super(delegate);
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            throw new UnsupportedOperationException();
        }
    }

    static abstract class ForwardingSet<E> implements Set<E> {
        private final Set<E> delegate;

        protected ForwardingSet(Set<E> delegate) {
            this.delegate = Objects.requireNonNull(delegate);
        }

        @Override public int size() { return delegate.size(); }
        @Override public boolean isEmpty() { return delegate.isEmpty(); }
        @Override public boolean contains(Object o) { return delegate.contains(o); }
        @Override public Iterator<E> iterator() { return delegate.iterator(); }
        @Override public Object[] toArray() { return delegate.toArray(); }
        @Override public <T> T[] toArray( T[] ts) { return delegate.toArray(ts); }
        @Override public boolean add(E e) { return delegate.add(e); }
        @Override public boolean remove(Object o) { return delegate.remove(o); }
        @Override public boolean containsAll( Collection<?> c) { return delegate.containsAll(c); }
        @Override public boolean addAll( Collection<? extends E> c) { return delegate.addAll(c); }
        @Override public boolean retainAll( Collection<?> c) { return delegate.retainAll(c); }
        @Override public boolean removeAll( Collection<?> c) { return delegate.removeAll(c); }
        @Override public void clear() { delegate.clear(); }
        @Override public boolean equals(Object o) { return delegate.equals(o); }
        @Override public int hashCode() { return delegate.hashCode(); }
        @Override public Spliterator<E> spliterator() { return delegate.spliterator(); }
        @Override public boolean removeIf(Predicate<? super E> filter) { return delegate.removeIf(filter); }
        @Override public Stream<E> stream() { return delegate.stream(); }
        @Override public Stream<E> parallelStream() { return delegate.parallelStream(); }
        @Override public void forEach(Consumer<? super E> action) { delegate.forEach(action); }
    }

    static class ThrowingSet<E> extends ForwardingSet<E> {
        public ThrowingSet(Set<E> delegate) {
            super(delegate);
        }

        @Override
        public ThrowingIterator<E> iterator() {
            return new ThrowingIterator<>(super.iterator());
        }
    }

    static abstract class ForwardingMap<K, V> implements Map<K, V> {
        private final Map<K, V> delegate;

        public ForwardingMap(Map<K, V> delegate) {
            this.delegate = delegate;
        }

        @Override public int size() { return delegate.size(); }
        @Override public boolean isEmpty() { return delegate.isEmpty(); }
        @Override public boolean containsKey(Object o) { return delegate.containsKey(o); }
        @Override public boolean containsValue(Object o) { return delegate.containsValue(o); }
        @Override public V get(Object o) { return delegate.get(o); }
        @Override public V put(K k, V v) { return delegate.put(k, v); }
        @Override public V remove(Object o) { return delegate.remove(o); }
        @Override public void putAll(Map<? extends K, ? extends V> map) { delegate.putAll(map); }
        @Override public void clear() { delegate.clear(); }
        @Override public Set<K> keySet() { return delegate.keySet(); }
        @Override public Collection<V> values() { return delegate.values(); }
        @Override public Set<Entry<K, V>> entrySet() { return delegate.entrySet(); }
        @Override public boolean equals(Object o) { return delegate.equals(o); }
        @Override public int hashCode() { return delegate.hashCode(); }
        @Override public V getOrDefault(Object key, V defaultValue) { return delegate.getOrDefault(key, defaultValue); }
        @Override public void forEach(BiConsumer<? super K, ? super V> action) { delegate.forEach(action); }
        @Override public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) { delegate.replaceAll(function); }
        @Override public V putIfAbsent(K key, V value) { return delegate.putIfAbsent(key, value); }
        @Override public boolean remove(Object key, Object value) { return delegate.remove(key, value); }
        @Override public boolean replace(K key, V oldValue, V newValue) { return delegate.replace(key, oldValue, newValue); }
        @Override public V replace(K key, V value) { return delegate.replace(key, value); }
        @Override public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) { return delegate.computeIfAbsent(key, mappingFunction); }
        @Override public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return delegate.computeIfPresent(key, remappingFunction); }
        @Override public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return delegate.compute(key, remappingFunction); }
        @Override public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) { return delegate.merge(key, value, remappingFunction); }
    }

    static class ThrowingMap<K, V> extends ForwardingMap<K, V> {
        public ThrowingMap(Map<K, V> delegate) {
            super(delegate);
        }

        @Override
        public ThrowingSet<Entry<K, V>> entrySet() {
            return new ThrowingSet<>(super.entrySet());
        }

        @Override
        public Set<K> keySet() {
            return new ThrowingSet(super.keySet());
        }
    }

    static<E> void assertThrowingIterator(Iterator<E> iterator) {
        try {
            iterator.forEachRemaining((entry) -> {});
            Assert.fail();
        } catch (UnsupportedOperationException expected) {
        }
    }

    private static Map<String, Object> map() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Bill");
        map.put("age", 23);
        return new ThrowingMap<>(map);
    }

    @Test public void testUnwrapped() {
        assertThrowingIterator(map().entrySet().iterator());
        assertThrowingIterator(map().keySet().iterator());
    }

    @Test public void test_unmodifiableMap_entrySet() {
        assertThrowingIterator(Collections.unmodifiableMap(map()).entrySet().iterator());
    }

    @Test public void test_checkedMap_entrySet() {
        assertThrowingIterator(Collections.checkedMap(map(), String.class, Object.class).entrySet().iterator());
    }

    @Test public void test_entrySet_checkedSet() {
        Set<Map.Entry<String, Object>> entrySet = map().entrySet();
        Class clazz = entrySet.iterator().next().getClass();
        assertThrowingIterator(Collections.checkedSet(entrySet, clazz).iterator());
    }
}
