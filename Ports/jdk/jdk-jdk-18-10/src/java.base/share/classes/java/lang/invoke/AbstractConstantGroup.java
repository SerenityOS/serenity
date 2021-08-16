/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.invoke;

import java.util.*;
import jdk.internal.vm.annotation.Stable;

/** Utility class for implementing ConstantGroup. */
/*non-public*/
abstract class AbstractConstantGroup implements ConstantGroup {
    /** The size of this constant group, set permanently by the constructor. */
    protected final int size;

    /** The constructor requires the size of the constant group being represented.
     * @param size the size of this constant group, set permanently by the constructor
     */
    AbstractConstantGroup(int size) {
        this.size = size;
    }

    @Override public final int size() {
        return size;
    }

    public abstract Object get(int index) throws LinkageError;

    public abstract Object get(int index, Object ifNotPresent);

    public abstract boolean isPresent(int index);

    // Do not override equals or hashCode, since this type is stateful.

    /**
     * Produce a string using the non-resolving list view,
     * where unresolved elements are presented as asterisks.
     * @return {@code this.asList("*").toString()}
     */
    @Override public String toString() {
        return asList("*").toString();
    }

    static class AsIterator implements Iterator<Object> {
        private final ConstantGroup self;
        private final int end;
        private final boolean resolving;
        private final Object ifNotPresent;

        // Mutable state:
        private int index;

        private AsIterator(ConstantGroup self, int start, int end,
                         boolean resolving, Object ifNotPresent) {
            this.self = self;
            this.end = end;
            this.index = start;
            this.resolving = resolving;
            this.ifNotPresent = ifNotPresent;
        }
        AsIterator(ConstantGroup self, int start, int end) {
            this(self, start, end, true, null);
        }
        AsIterator(ConstantGroup self, int start, int end,
                 Object ifNotPresent) {
            this(self, start, end, false, ifNotPresent);
        }

        @Override
        public boolean hasNext() {
            return index < end;
        }

        @Override
        public Object next() {
            int i = bumpIndex();
            if (resolving)
                return self.get(i);
            else
                return self.get(i, ifNotPresent);
        }

        private int bumpIndex() {
            int i = index;
            if (i >= end)  throw new NoSuchElementException();
            index = i+1;
            return i;
        }
    }

    static class SubGroup extends AbstractConstantGroup {
        private final ConstantGroup self;  // the real CG
        private final int offset;  // offset within myself
        SubGroup(ConstantGroup self, int start, int end) {
            super(end - start);
            this.self = self;
            this.offset = start;
            Objects.checkFromToIndex(start, end, size);
        }

        private int mapIndex(int index) {
            return Objects.checkIndex(index, size) + offset;
        }

        @Override
        public Object get(int index) {
            return self.get(mapIndex(index));
        }

        @Override
        public Object get(int index, Object ifNotPresent) {
            return self.get(mapIndex(index), ifNotPresent);
        }

        @Override
        public boolean isPresent(int index) {
            return self.isPresent(mapIndex(index));
        }

        @Override
        public ConstantGroup subGroup(int start, int end) {
            Objects.checkFromToIndex(start, end, size);
            return new SubGroup(self, offset + start, offset + end);
        }

        @Override
        public List<Object> asList() {
            return new AsList(self, offset, offset + size);
        }

        @Override
        public List<Object> asList(Object ifNotPresent) {
            return new AsList(self, offset, offset + size, ifNotPresent);
        }

        @Override
        public int copyConstants(int start, int end,
                                  Object[] buf, int pos) throws LinkageError {
            Objects.checkFromToIndex(start, end, size);
            return self.copyConstants(offset + start, offset + end,
                                      buf, pos);
        }

        @Override
        public int copyConstants(int start, int end,
                                  Object[] buf, int pos,
                                  Object ifNotPresent) {
            Objects.checkFromToIndex(start, end, size);
            return self.copyConstants(offset + start, offset + end,
                                      buf, pos, ifNotPresent);
        }
    }

    static class AsList extends AbstractList<Object> {
        private final ConstantGroup self;
        private final int size;
        private final int offset;
        private final boolean resolving;
        private final Object ifNotPresent;

        private AsList(ConstantGroup self, int start, int end,
                       boolean resolving, Object ifNotPresent) {
            this.self = self;
            this.size = end - start;
            this.offset = start;
            this.resolving = resolving;
            this.ifNotPresent = ifNotPresent;
            Objects.checkFromToIndex(start, end, self.size());
        }
        AsList(ConstantGroup self, int start, int end) {
            this(self, start, end, true, null);
        }
        AsList(ConstantGroup self, int start, int end,
               Object ifNotPresent) {
            this(self, start, end, false, ifNotPresent);
        }

        private int mapIndex(int index) {
            return Objects.checkIndex(index, size) + offset;
        }

        @Override public final int size() {
            return size;
        }

        @Override public Object get(int index) {
            if (resolving)
                return self.get(mapIndex(index));
            else
                return self.get(mapIndex(index), ifNotPresent);
        }

        @Override
        public Iterator<Object> iterator() {
            if (resolving)
                return new AsIterator(self, offset, offset + size);
            else
                return new AsIterator(self, offset, offset + size, ifNotPresent);
        }

        @Override public List<Object> subList(int start, int end) {
            Objects.checkFromToIndex(start, end, size);
            return new AsList(self, offset + start, offset + end,
                              resolving, ifNotPresent);
        }

        @Override public Object[] toArray() {
            return toArray(new Object[size]);
        }
        @Override public <T> T[] toArray(T[] a) {
            int pad = a.length - size;
            if (pad < 0) {
                pad = 0;
                a = Arrays.copyOf(a, size);
            }
            if (resolving)
                self.copyConstants(offset, offset + size, a, 0);
            else
                self.copyConstants(offset, offset + size, a, 0,
                                   ifNotPresent);
            if (pad > 0)  a[size] = null;
            return a;
        }
    }

    static abstract
    class WithCache extends AbstractConstantGroup {
        @Stable final Object[] cache;

        WithCache(int size) {
            super(size);
            // It is caller's responsibility to initialize the cache.
            // Initial contents are all-null, which means nothing is present.
            cache = new Object[size];
        }

        void initializeCache(List<Object> cacheContents, Object ifNotPresent) {
            // Replace ifNotPresent with NOT_PRESENT,
            // and null with RESOLVED_TO_NULL.
            // Then forget about the user-provided ifNotPresent.
            for (int i = 0; i < cache.length; i++) {
                Object x = cacheContents.get(i);
                if (x == ifNotPresent)
                    continue;  // leave the null in place
                if (x == null)
                    x = RESOLVED_TO_NULL;
                cache[i] = x;
            }
        }

        @Override public Object get(int i) {
            Object x = cache[i];
            // @Stable array must use null for sentinel
            if (x == null)  x = fillCache(i);
            return unwrapNull(x);
        }

        @Override public Object get(int i, Object ifNotAvailable) {
            Object x = cache[i];
            // @Stable array must use null for sentinel
            if (x == null)  return ifNotAvailable;
            return unwrapNull(x);
        }

        @Override
        public boolean isPresent(int i) {
            return cache[i] != null;
        }

        /** hook for local subclasses */
        Object fillCache(int i) {
            throw new NoSuchElementException("constant group does not contain element #"+i);
        }

        /// routines for mapping between null sentinel and true resolved null

        static Object wrapNull(Object x) {
            return x == null ? RESOLVED_TO_NULL : x;
        }

        static Object unwrapNull(Object x) {
            assert(x != null);
            return x == RESOLVED_TO_NULL ? null : x;
        }

        // secret sentinel for an actual null resolved value, in the cache
        static final Object RESOLVED_TO_NULL = new Object();

        // secret sentinel for a "hole" in the cache:
        static final Object NOT_PRESENT = new Object();

    }

    /** Skeleton implementation of BootstrapCallInfo. */
    static class BSCIWithCache<T> extends WithCache implements BootstrapCallInfo<T> {
        private final MethodHandle bsm;
        private final String name;
        private final T type;

        @Override public String toString() {
            return bsm+"/"+name+":"+type+super.toString();
        }

        BSCIWithCache(MethodHandle bsm, String name, T type, int size) {
            super(size);
            this.type = type;
            this.bsm = bsm;
            this.name = name;
            assert(type instanceof Class || type instanceof MethodType);
        }

        @Override public MethodHandle bootstrapMethod() { return bsm; }
        @Override public String invocationName() { return name; }
        @Override public T invocationType() { return type; }
    }
}
