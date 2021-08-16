/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

class SpliteratorLateBindingFailFastHelper {

    interface Source<T> {
        Spliterator<T> spliterator();

        void update();

        default boolean bindOnCharacteristics() {
            return false;
        }
    }

    static class IntSource<T> implements Source<Integer> {
        final T b;
        final Function<? super T, Spliterator.OfInt> toSpliterator;
        final Consumer<T> updater;
        final boolean bindOnCharacteristics;

        public IntSource(T b, Function<? super T, Spliterator.OfInt> toSpliterator,
                         Consumer<T> updater) {
            this(b, toSpliterator, updater, false);
        }

        public IntSource(T b, Function<? super T, Spliterator.OfInt> toSpliterator,
                         Consumer<T> updater, boolean bindOnCharacteristics) {
            this.b = b;
            this.toSpliterator = toSpliterator;
            this.updater = updater;
            this.bindOnCharacteristics = bindOnCharacteristics;
        }

        @Override
        public Spliterator.OfInt spliterator() {
            return toSpliterator.apply(b);
        }

        @Override
        public void update() {
            updater.accept(b);
        }

        @Override
        public boolean bindOnCharacteristics() {
            return bindOnCharacteristics;
        }
    }

    static class SpliteratorDataBuilder<T> {
        final List<Object[]> data;

        final T newValue;

        final List<T> exp;

        final Map<T, T> mExp;

        SpliteratorDataBuilder(List<Object[]> data, T newValue, List<T> exp) {
            this.data = data;
            this.newValue = newValue;
            this.exp = exp;
            this.mExp = createMap(exp);
        }

        Map<T, T> createMap(List<T> l) {
            Map<T, T> m = new LinkedHashMap<>();
            for (T t : l) {
                m.put(t, t);
            }
            return m;
        }

        void add(String description, Supplier<Source<?>> s) {
            data.add(new Object[]{description, s});
        }

        void addCollection(Function<Collection<T>, ? extends Collection<T>> f) {
            class CollectionSource implements Source<T> {
                final Collection<T> c = f.apply(exp);

                final Consumer<Collection<T>> updater;

                CollectionSource(Consumer<Collection<T>> updater) {
                    this.updater = updater;
                }

                @Override
                public Spliterator<T> spliterator() {
                    return c.spliterator();
                }

                @Override
                public void update() {
                    updater.accept(c);
                }
            }

            String description = "new " + f.apply(Collections.<T>emptyList()).getClass().getName() + ".spliterator() ";
            add(description + "ADD", () -> new CollectionSource(c -> c.add(newValue)));
            add(description + "REMOVE", () -> new CollectionSource(c -> c.remove(c.iterator().next())));
        }

        void addList(Function<Collection<T>, ? extends List<T>> l) {
            addCollection(l);
            addCollection(l.andThen(list -> list.subList(0, list.size())));
        }

        void addMap(Function<Map<T, T>, ? extends Map<T, T>> mapConstructor) {
            class MapSource<U> implements Source<U> {
                final Map<T, T> m = mapConstructor.apply(mExp);

                final Collection<U> c;

                final Consumer<Map<T, T>> updater;

                MapSource(Function<Map<T, T>, Collection<U>> f, Consumer<Map<T, T>> updater) {
                    this.c = f.apply(m);
                    this.updater = updater;
                }

                @Override
                public Spliterator<U> spliterator() {
                    return c.spliterator();
                }

                @Override
                public void update() {
                    updater.accept(m);
                }
            }

            Map<String, Consumer<Map<T, T>>> actions = new HashMap<>();
            actions.put("ADD", m -> m.put(newValue, newValue));
            actions.put("REMOVE", m -> m.remove(m.keySet().iterator().next()));

            String description = "new " + mapConstructor.apply(Collections.<T, T>emptyMap()).getClass().getName();
            for (Map.Entry<String, Consumer<Map<T, T>>> e : actions.entrySet()) {
                add(description + ".keySet().spliterator() " + e.getKey(),
                    () -> new MapSource<>(m -> m.keySet(), e.getValue()));
                add(description + ".values().spliterator() " + e.getKey(),
                    () -> new MapSource<>(m -> m.values(), e.getValue()));
                add(description + ".entrySet().spliterator() " + e.getKey(),
                    () -> new MapSource<>(m -> m.entrySet(), e.getValue()));
            }
        }
    }

    static class AbstractRandomAccessListImpl extends AbstractList<Integer> implements RandomAccess {
        List<Integer> l;

        AbstractRandomAccessListImpl(Collection<Integer> c) {
            this.l = new ArrayList<>(c);
        }

        @Override
        public boolean add(Integer integer) {
            modCount++;
            return l.add(integer);
        }

        @Override
        public Iterator<Integer> iterator() {
            return l.iterator();
        }

        @Override
        public Integer get(int index) {
            return l.get(index);
        }

        @Override
        public boolean remove(Object o) {
            modCount++;
            return l.remove(o);
        }

        @Override
        public int size() {
            return l.size();
        }

        @Override
        public List<Integer> subList(int fromIndex, int toIndex) {
            return l.subList(fromIndex, toIndex);
        }
    }
}
