/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import jdk.internal.access.JavaUtilCollectionAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.misc.CDS;
import jdk.internal.vm.annotation.Stable;

/**
 * Container class for immutable collections. Not part of the public API.
 * Mainly for namespace management and shared infrastructure.
 *
 * Serial warnings are suppressed throughout because all implementation
 * classes use a serial proxy and thus have no need to declare serialVersionUID.
 */
@SuppressWarnings("serial")
class ImmutableCollections {
    /**
     * A "salt" value used for randomizing iteration order. This is initialized once
     * and stays constant for the lifetime of the JVM. It need not be truly random, but
     * it needs to vary sufficiently from one run to the next so that iteration order
     * will vary between JVM runs.
     */
    private static final long SALT32L;

    /**
     * For set and map iteration, we will iterate in "reverse" stochastically,
     * decided at bootstrap time.
     */
    private static final boolean REVERSE;
    static {
        // to generate a reasonably random and well-mixed SALT, use an arbitrary
        // value (a slice of pi), multiply with a random seed, then pick
        // the mid 32-bits from the product. By picking a SALT value in the
        // [0 ... 0xFFFF_FFFFL == 2^32-1] range, we ensure that for any positive
        // int N, (SALT32L * N) >> 32 is a number in the [0 ... N-1] range. This
        // property will be used to avoid more expensive modulo-based
        // calculations.
        long color = 0x243F_6A88_85A3_08D3L; // slice of pi

        // When running with -Xshare:dump, the VM will supply a "random" seed that's
        // derived from the JVM build/version, so can we generate the exact same
        // CDS archive for the same JDK build. This makes it possible to verify the
        // consistency of the JDK build.
        long seed = CDS.getRandomSeedForDumping();
        if (seed == 0) {
          seed = System.nanoTime();
        }
        SALT32L = (int)((color * seed) >> 16) & 0xFFFF_FFFFL;
        // use the lowest bit to determine if we should reverse iteration
        REVERSE = (SALT32L & 1) == 0;
    }

    /**
     * Constants following this might be initialized from the CDS archive via
     * this array.
     */
    private static Object[] archivedObjects;

    private static final Object EMPTY;
    static final ListN<?> EMPTY_LIST;
    static final ListN<?> EMPTY_LIST_NULLS;
    static final SetN<?> EMPTY_SET;
    static final MapN<?,?> EMPTY_MAP;

    static {
        CDS.initializeFromArchive(ImmutableCollections.class);
        if (archivedObjects == null) {
            EMPTY = new Object();
            EMPTY_LIST = new ListN<>(new Object[0], false);
            EMPTY_LIST_NULLS = new ListN<>(new Object[0], true);
            EMPTY_SET = new SetN<>();
            EMPTY_MAP = new MapN<>();
            archivedObjects =
                new Object[] { EMPTY, EMPTY_LIST, EMPTY_LIST_NULLS, EMPTY_SET, EMPTY_MAP };
        } else {
            EMPTY = archivedObjects[0];
            EMPTY_LIST = (ListN)archivedObjects[1];
            EMPTY_LIST_NULLS = (ListN)archivedObjects[2];
            EMPTY_SET = (SetN)archivedObjects[3];
            EMPTY_MAP = (MapN)archivedObjects[4];
        }
    }

    static class Access {
        static {
            SharedSecrets.setJavaUtilCollectionAccess(new JavaUtilCollectionAccess() {
                public <E> List<E> listFromTrustedArray(Object[] array) {
                    return ImmutableCollections.listFromTrustedArray(array);
                }
                public <E> List<E> listFromTrustedArrayNullsAllowed(Object[] array) {
                    return ImmutableCollections.listFromTrustedArrayNullsAllowed(array);
                }
            });
        }
    }

    /** No instances. */
    private ImmutableCollections() { }

    /**
     * The reciprocal of load factor. Given a number of elements
     * to store, multiply by this factor to get the table size.
     */
    static final int EXPAND_FACTOR = 2;

    static UnsupportedOperationException uoe() { return new UnsupportedOperationException(); }

    @jdk.internal.ValueBased
    static abstract class AbstractImmutableCollection<E> extends AbstractCollection<E> {
        // all mutating methods throw UnsupportedOperationException
        @Override public boolean add(E e) { throw uoe(); }
        @Override public boolean addAll(Collection<? extends E> c) { throw uoe(); }
        @Override public void    clear() { throw uoe(); }
        @Override public boolean remove(Object o) { throw uoe(); }
        @Override public boolean removeAll(Collection<?> c) { throw uoe(); }
        @Override public boolean removeIf(Predicate<? super E> filter) { throw uoe(); }
        @Override public boolean retainAll(Collection<?> c) { throw uoe(); }
    }

    // ---------- List Static Factory Methods ----------

    /**
     * Copies a collection into a new List, unless the arg is already a safe,
     * null-prohibiting unmodifiable list, in which case the arg itself is returned.
     * Null argument or null elements in the argument will result in NPE.
     *
     * @param <E> the List's element type
     * @param input the input array
     * @return the new list
     */
    @SuppressWarnings("unchecked")
    static <E> List<E> listCopy(Collection<? extends E> coll) {
        if (coll instanceof List12 || (coll instanceof ListN && ! ((ListN<?>)coll).allowNulls)) {
            return (List<E>)coll;
        } else {
            return (List<E>)List.of(coll.toArray()); // implicit nullcheck of coll
        }
    }

    /**
     * Creates a new List from an untrusted array, creating a new array for internal
     * storage, and checking for and rejecting null elements.
     *
     * @param <E> the List's element type
     * @param input the input array
     * @return the new list
     */
    @SafeVarargs
    static <E> List<E> listFromArray(E... input) {
        // copy and check manually to avoid TOCTOU
        @SuppressWarnings("unchecked")
        E[] tmp = (E[])new Object[input.length]; // implicit nullcheck of input
        for (int i = 0; i < input.length; i++) {
            tmp[i] = Objects.requireNonNull(input[i]);
        }
        return new ListN<>(tmp, false);
    }

    /**
     * Creates a new List from a trusted array, checking for and rejecting null
     * elements.
     *
     * <p>A trusted array has no references retained by the caller. It can therefore be
     * safely reused as the List's internal storage, avoiding a defensive copy. The array's
     * class must be Object[].class. This method is declared with a parameter type of
     * Object... instead of E... so that a varargs call doesn't accidentally create an array
     * of some class other than Object[].class.
     *
     * @param <E> the List's element type
     * @param input the input array
     * @return the new list
     */
    @SuppressWarnings("unchecked")
    static <E> List<E> listFromTrustedArray(Object... input) {
        assert input.getClass() == Object[].class;
        for (Object o : input) { // implicit null check of 'input' array
            Objects.requireNonNull(o);
        }

        return switch (input.length) {
            case 0  -> (List<E>) ImmutableCollections.EMPTY_LIST;
            case 1  -> (List<E>) new List12<>(input[0]);
            case 2  -> (List<E>) new List12<>(input[0], input[1]);
            default -> (List<E>) new ListN<>(input, false);
        };
    }

    /**
     * Creates a new List from a trusted array, allowing null elements.
     *
     * <p>A trusted array has no references retained by the caller. It can therefore be
     * safely reused as the List's internal storage, avoiding a defensive copy. The array's
     * class must be Object[].class. This method is declared with a parameter type of
     * Object... instead of E... so that a varargs call doesn't accidentally create an array
     * of some class other than Object[].class.
     *
     * <p>Avoids creating a List12 instance, as it cannot accommodate null elements.
     *
     * @param <E> the List's element type
     * @param input the input array
     * @return the new list
     */
    @SuppressWarnings("unchecked")
    static <E> List<E> listFromTrustedArrayNullsAllowed(Object... input) {
        assert input.getClass() == Object[].class;
        if (input.length == 0) {
            return (List<E>) EMPTY_LIST_NULLS;
        } else {
            return new ListN<>((E[])input, true);
        }
    }

    // ---------- List Implementations ----------

    @jdk.internal.ValueBased
    static abstract class AbstractImmutableList<E> extends AbstractImmutableCollection<E>
            implements List<E>, RandomAccess {

        // all mutating methods throw UnsupportedOperationException
        @Override public void    add(int index, E element) { throw uoe(); }
        @Override public boolean addAll(int index, Collection<? extends E> c) { throw uoe(); }
        @Override public E       remove(int index) { throw uoe(); }
        @Override public void    replaceAll(UnaryOperator<E> operator) { throw uoe(); }
        @Override public E       set(int index, E element) { throw uoe(); }
        @Override public void    sort(Comparator<? super E> c) { throw uoe(); }

        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            int size = size();
            subListRangeCheck(fromIndex, toIndex, size);
            return SubList.fromList(this, fromIndex, toIndex);
        }

        static void subListRangeCheck(int fromIndex, int toIndex, int size) {
            if (fromIndex < 0)
                throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
            if (toIndex > size)
                throw new IndexOutOfBoundsException("toIndex = " + toIndex);
            if (fromIndex > toIndex)
                throw new IllegalArgumentException("fromIndex(" + fromIndex +
                        ") > toIndex(" + toIndex + ")");
        }

        @Override
        public Iterator<E> iterator() {
            return new ListItr<E>(this, size());
        }

        @Override
        public ListIterator<E> listIterator() {
            return listIterator(0);
        }

        @Override
        public ListIterator<E> listIterator(final int index) {
            int size = size();
            if (index < 0 || index > size) {
                throw outOfBounds(index);
            }
            return new ListItr<E>(this, size, index);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }

            if (!(o instanceof List)) {
                return false;
            }

            Iterator<?> oit = ((List<?>) o).iterator();
            for (int i = 0, s = size(); i < s; i++) {
                if (!oit.hasNext() || !Objects.equals(get(i), oit.next())) {
                    return false;
                }
            }
            return !oit.hasNext();
        }

        @Override
        public int hashCode() {
            int hash = 1;
            for (int i = 0, s = size(); i < s; i++) {
                hash = 31 * hash + Objects.hashCode(get(i));
            }
            return hash;
        }

        @Override
        public boolean contains(Object o) {
            return indexOf(o) >= 0;
        }

        IndexOutOfBoundsException outOfBounds(int index) {
            return new IndexOutOfBoundsException("Index: " + index + " Size: " + size());
        }
    }

    static final class ListItr<E> implements ListIterator<E> {

        @Stable
        private final List<E> list;

        @Stable
        private final int size;

        @Stable
        private final boolean isListIterator;

        private int cursor;

        ListItr(List<E> list, int size) {
            this.list = list;
            this.size = size;
            this.cursor = 0;
            isListIterator = false;
        }

        ListItr(List<E> list, int size, int index) {
            this.list = list;
            this.size = size;
            this.cursor = index;
            isListIterator = true;
        }

        public boolean hasNext() {
            return cursor != size;
        }

        public E next() {
            try {
                int i = cursor;
                E next = list.get(i);
                cursor = i + 1;
                return next;
            } catch (IndexOutOfBoundsException e) {
                throw new NoSuchElementException();
            }
        }

        public void remove() {
            throw uoe();
        }

        public boolean hasPrevious() {
            if (!isListIterator) {
                throw uoe();
            }
            return cursor != 0;
        }

        public E previous() {
            if (!isListIterator) {
                throw uoe();
            }
            try {
                int i = cursor - 1;
                E previous = list.get(i);
                cursor = i;
                return previous;
            } catch (IndexOutOfBoundsException e) {
                throw new NoSuchElementException();
            }
        }

        public int nextIndex() {
            if (!isListIterator) {
                throw uoe();
            }
            return cursor;
        }

        public int previousIndex() {
            if (!isListIterator) {
                throw uoe();
            }
            return cursor - 1;
        }

        public void set(E e) {
            throw uoe();
        }

        public void add(E e) {
            throw uoe();
        }
    }

    static final class SubList<E> extends AbstractImmutableList<E>
            implements RandomAccess {

        @Stable
        private final AbstractImmutableList<E> root;

        @Stable
        private final int offset;

        @Stable
        private final int size;

        private SubList(AbstractImmutableList<E> root, int offset, int size) {
            assert root instanceof List12 || root instanceof ListN;
            this.root = root;
            this.offset = offset;
            this.size = size;
        }

        /**
         * Constructs a sublist of another SubList.
         */
        static <E> SubList<E> fromSubList(SubList<E> parent, int fromIndex, int toIndex) {
            return new SubList<>(parent.root, parent.offset + fromIndex, toIndex - fromIndex);
        }

        /**
         * Constructs a sublist of an arbitrary AbstractImmutableList, which is
         * not a SubList itself.
         */
        static <E> SubList<E> fromList(AbstractImmutableList<E> list, int fromIndex, int toIndex) {
            return new SubList<>(list, fromIndex, toIndex - fromIndex);
        }

        public E get(int index) {
            Objects.checkIndex(index, size);
            return root.get(offset + index);
        }

        public int size() {
            return size;
        }

        public Iterator<E> iterator() {
            return new ListItr<>(this, size());
        }

        public ListIterator<E> listIterator(int index) {
            rangeCheck(index);
            return new ListItr<>(this, size(), index);
        }

        public List<E> subList(int fromIndex, int toIndex) {
            subListRangeCheck(fromIndex, toIndex, size);
            return SubList.fromSubList(this, fromIndex, toIndex);
        }

        private void rangeCheck(int index) {
            if (index < 0 || index > size) {
                throw outOfBounds(index);
            }
        }

        private boolean allowNulls() {
            return root instanceof ListN && ((ListN<?>)root).allowNulls;
        }

        @Override
        public int indexOf(Object o) {
            if (!allowNulls() && o == null) {
                throw new NullPointerException();
            }
            for (int i = 0, s = size(); i < s; i++) {
                if (Objects.equals(o, get(i))) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public int lastIndexOf(Object o) {
            if (!allowNulls() && o == null) {
                throw new NullPointerException();
            }
            for (int i = size() - 1; i >= 0; i--) {
                if (Objects.equals(o, get(i))) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public Object[] toArray() {
            Object[] array = new Object[size];
            for (int i = 0; i < size; i++) {
                array[i] = get(i);
            }
            return array;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            T[] array = a.length >= size ? a :
                    (T[])java.lang.reflect.Array
                            .newInstance(a.getClass().getComponentType(), size);
            for (int i = 0; i < size; i++) {
                array[i] = (T)get(i);
            }
            if (array.length > size) {
                array[size] = null; // null-terminate
            }
            return array;
        }
    }

    @jdk.internal.ValueBased
    static final class List12<E> extends AbstractImmutableList<E>
            implements Serializable {

        @Stable
        private final E e0;

        @Stable
        private final Object e1;

        List12(E e0) {
            this.e0 = Objects.requireNonNull(e0);
            // Use EMPTY as a sentinel for an unused element: not using null
            // enables constant folding optimizations over single-element lists
            this.e1 = EMPTY;
        }

        List12(E e0, E e1) {
            this.e0 = Objects.requireNonNull(e0);
            this.e1 = Objects.requireNonNull(e1);
        }

        @Override
        public int size() {
            return e1 != EMPTY ? 2 : 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        @SuppressWarnings("unchecked")
        public E get(int index) {
            if (index == 0) {
                return e0;
            } else if (index == 1 && e1 != EMPTY) {
                return (E)e1;
            }
            throw outOfBounds(index);
        }

        @Override
        public int indexOf(Object o) {
            Objects.requireNonNull(o);
            if (o.equals(e0)) {
                return 0;
            } else if (e1 != EMPTY && o.equals(e1)) {
                return 1;
            } else {
                return -1;
            }
        }

        @Override
        public int lastIndexOf(Object o) {
            Objects.requireNonNull(o);
            if (e1 != EMPTY && o.equals(e1)) {
                return 1;
            } else if (o.equals(e0)) {
                return 0;
            } else {
                return -1;
            }
        }

        @java.io.Serial
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw new InvalidObjectException("not serial proxy");
        }

        @java.io.Serial
        private Object writeReplace() {
            if (e1 == EMPTY) {
                return new CollSer(CollSer.IMM_LIST, e0);
            } else {
                return new CollSer(CollSer.IMM_LIST, e0, e1);
            }
        }

        @Override
        public Object[] toArray() {
            if (e1 == EMPTY) {
                return new Object[] { e0 };
            } else {
                return new Object[] { e0, e1 };
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            int size = size();
            T[] array = a.length >= size ? a :
                    (T[])Array.newInstance(a.getClass().getComponentType(), size);
            array[0] = (T)e0;
            if (size == 2) {
                array[1] = (T)e1;
            }
            if (array.length > size) {
                array[size] = null; // null-terminate
            }
            return array;
        }
    }

    @jdk.internal.ValueBased
    static final class ListN<E> extends AbstractImmutableList<E>
            implements Serializable {

        @Stable
        private final E[] elements;

        @Stable
        private final boolean allowNulls;

        // caller must ensure that elements has no nulls if allowNulls is false
        private ListN(E[] elements, boolean allowNulls) {
            this.elements = elements;
            this.allowNulls = allowNulls;
        }

        @Override
        public boolean isEmpty() {
            return elements.length == 0;
        }

        @Override
        public int size() {
            return elements.length;
        }

        @Override
        public E get(int index) {
            return elements[index];
        }

        @java.io.Serial
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw new InvalidObjectException("not serial proxy");
        }

        @java.io.Serial
        private Object writeReplace() {
            return new CollSer(allowNulls ? CollSer.IMM_LIST_NULLS : CollSer.IMM_LIST, elements);
        }

        @Override
        public Object[] toArray() {
            return Arrays.copyOf(elements, elements.length);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            int size = elements.length;
            if (a.length < size) {
                // Make a new array of a's runtime type, but my contents:
                return (T[]) Arrays.copyOf(elements, size, a.getClass());
            }
            System.arraycopy(elements, 0, a, 0, size);
            if (a.length > size) {
                a[size] = null; // null-terminate
            }
            return a;
        }

        @Override
        public int indexOf(Object o) {
            if (!allowNulls && o == null) {
                throw new NullPointerException();
            }
            Object[] es = elements;
            for (int i = 0; i < es.length; i++) {
                if (Objects.equals(o, es[i])) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public int lastIndexOf(Object o) {
            if (!allowNulls && o == null) {
                throw new NullPointerException();
            }
            Object[] es = elements;
            for (int i = es.length - 1; i >= 0; i--) {
                if (Objects.equals(o, es[i])) {
                    return i;
                }
            }
            return -1;
        }
    }

    // ---------- Set Implementations ----------

    @jdk.internal.ValueBased
    static abstract class AbstractImmutableSet<E> extends AbstractImmutableCollection<E>
            implements Set<E> {

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (!(o instanceof Set)) {
                return false;
            }

            Collection<?> c = (Collection<?>) o;
            if (c.size() != size()) {
                return false;
            }
            for (Object e : c) {
                if (e == null || !contains(e)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public abstract int hashCode();
    }

    @jdk.internal.ValueBased
    static final class Set12<E> extends AbstractImmutableSet<E>
            implements Serializable {

        @Stable
        private final E e0;

        @Stable
        private final Object e1;

        Set12(E e0) {
            this.e0 = Objects.requireNonNull(e0);
            // Use EMPTY as a sentinel for an unused element: not using null
            // enable constant folding optimizations over single-element sets
            this.e1 = EMPTY;
        }

        Set12(E e0, E e1) {
            if (e0.equals(Objects.requireNonNull(e1))) { // implicit nullcheck of e0
                throw new IllegalArgumentException("duplicate element: " + e0);
            }

            this.e0 = e0;
            this.e1 = e1;
        }

        @Override
        public int size() {
            return (e1 == EMPTY) ? 1 : 2;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean contains(Object o) {
            return o.equals(e0) || e1.equals(o); // implicit nullcheck of o
        }

        @Override
        public int hashCode() {
            return e0.hashCode() + (e1 == EMPTY ? 0 : e1.hashCode());
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<>() {
                private int idx = (e1 == EMPTY) ? 1 : 2;

                @Override
                public boolean hasNext() {
                    return idx > 0;
                }

                @Override
                @SuppressWarnings("unchecked")
                public E next() {
                    if (idx == 1) {
                        idx = 0;
                        return (REVERSE || e1 == EMPTY) ? e0 : (E)e1;
                    } else if (idx == 2) {
                        idx = 1;
                        return REVERSE ? (E)e1 : e0;
                    } else {
                        throw new NoSuchElementException();
                    }
                }
            };
        }

        @java.io.Serial
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw new InvalidObjectException("not serial proxy");
        }

        @java.io.Serial
        private Object writeReplace() {
            if (e1 == EMPTY) {
                return new CollSer(CollSer.IMM_SET, e0);
            } else {
                return new CollSer(CollSer.IMM_SET, e0, e1);
            }
        }

        @Override
        public Object[] toArray() {
            if (e1 == EMPTY) {
                return new Object[] { e0 };
            } else if (REVERSE) {
                return new Object[] { e1, e0 };
            } else {
                return new Object[] { e0, e1 };
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            int size = size();
            T[] array = a.length >= size ? a :
                    (T[])Array.newInstance(a.getClass().getComponentType(), size);
            if (size == 1) {
                array[0] = (T)e0;
            } else if (REVERSE) {
                array[0] = (T)e1;
                array[1] = (T)e0;
            } else {
                array[0] = (T)e0;
                array[1] = (T)e1;
            }
            if (array.length > size) {
                array[size] = null; // null-terminate
            }
            return array;
        }
    }


    /**
     * An array-based Set implementation. The element array must be strictly
     * larger than the size (the number of contained elements) so that at
     * least one null is always present.
     * @param <E> the element type
     */
    @jdk.internal.ValueBased
    static final class SetN<E> extends AbstractImmutableSet<E>
            implements Serializable {

        @Stable
        final E[] elements;

        @Stable
        final int size;

        @SafeVarargs
        @SuppressWarnings("unchecked")
        SetN(E... input) {
            size = input.length; // implicit nullcheck of input

            elements = (E[])new Object[EXPAND_FACTOR * input.length];
            for (int i = 0; i < input.length; i++) {
                E e = input[i];
                int idx = probe(e); // implicit nullcheck of e
                if (idx >= 0) {
                    throw new IllegalArgumentException("duplicate element: " + e);
                } else {
                    elements[-(idx + 1)] = e;
                }
            }
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public boolean isEmpty() {
            return size == 0;
        }

        @Override
        public boolean contains(Object o) {
            Objects.requireNonNull(o);
            return size > 0 && probe(o) >= 0;
        }

        private final class SetNIterator implements Iterator<E> {

            private int remaining;

            private int idx;

            SetNIterator() {
                remaining = size;
                // pick a starting index in the [0 .. element.length-1] range
                // randomly based on SALT32L
                idx = (int) ((SALT32L * elements.length) >>> 32);
            }

            @Override
            public boolean hasNext() {
                return remaining > 0;
            }

            @Override
            public E next() {
                if (remaining > 0) {
                    E element;
                    int idx = this.idx;
                    int len = elements.length;
                    // step to the next element; skip null elements
                    do {
                        if (REVERSE) {
                            if (++idx >= len) {
                                idx = 0;
                            }
                        } else {
                            if (--idx < 0) {
                                idx = len - 1;
                            }
                        }
                    } while ((element = elements[idx]) == null);
                    this.idx = idx;
                    remaining--;
                    return element;
                } else {
                    throw new NoSuchElementException();
                }
            }
        }

        @Override
        public Iterator<E> iterator() {
            return new SetNIterator();
        }

        @Override
        public int hashCode() {
            int h = 0;
            for (E e : elements) {
                if (e != null) {
                    h += e.hashCode();
                }
            }
            return h;
        }

        // returns index at which element is present; or if absent,
        // (-i - 1) where i is location where element should be inserted.
        // Callers are relying on this method to perform an implicit nullcheck
        // of pe
        private int probe(Object pe) {
            int idx = Math.floorMod(pe.hashCode(), elements.length);
            while (true) {
                E ee = elements[idx];
                if (ee == null) {
                    return -idx - 1;
                } else if (pe.equals(ee)) {
                    return idx;
                } else if (++idx == elements.length) {
                    idx = 0;
                }
            }
        }

        @java.io.Serial
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw new InvalidObjectException("not serial proxy");
        }

        @java.io.Serial
        private Object writeReplace() {
            Object[] array = new Object[size];
            int dest = 0;
            for (Object o : elements) {
                if (o != null) {
                    array[dest++] = o;
                }
            }
            return new CollSer(CollSer.IMM_SET, array);
        }

        @Override
        public Object[] toArray() {
            Object[] array = new Object[size];
            Iterator<E> it = iterator();
            for (int i = 0; i < size; i++) {
                array[i] = it.next();
            }
            return array;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            T[] array = a.length >= size ? a :
                    (T[])Array.newInstance(a.getClass().getComponentType(), size);
            Iterator<E> it = iterator();
            for (int i = 0; i < size; i++) {
                array[i] = (T)it.next();
            }
            if (array.length > size) {
                array[size] = null; // null-terminate
            }
            return array;
        }
    }

    // ---------- Map Implementations ----------

    @jdk.internal.ValueBased
    abstract static class AbstractImmutableMap<K,V> extends AbstractMap<K,V> implements Serializable {
        @Override public void clear() { throw uoe(); }
        @Override public V compute(K key, BiFunction<? super K,? super V,? extends V> rf) { throw uoe(); }
        @Override public V computeIfAbsent(K key, Function<? super K,? extends V> mf) { throw uoe(); }
        @Override public V computeIfPresent(K key, BiFunction<? super K,? super V,? extends V> rf) { throw uoe(); }
        @Override public V merge(K key, V value, BiFunction<? super V,? super V,? extends V> rf) { throw uoe(); }
        @Override public V put(K key, V value) { throw uoe(); }
        @Override public void putAll(Map<? extends K,? extends V> m) { throw uoe(); }
        @Override public V putIfAbsent(K key, V value) { throw uoe(); }
        @Override public V remove(Object key) { throw uoe(); }
        @Override public boolean remove(Object key, Object value) { throw uoe(); }
        @Override public V replace(K key, V value) { throw uoe(); }
        @Override public boolean replace(K key, V oldValue, V newValue) { throw uoe(); }
        @Override public void replaceAll(BiFunction<? super K,? super V,? extends V> f) { throw uoe(); }

        /**
         * @implNote {@code null} values are disallowed in these immutable maps,
         * so we can improve upon the default implementation since a
         * {@code null} return from {@code get(key)} always means the default
         * value should be returned.
         */
        @Override
        public V getOrDefault(Object key, V defaultValue) {
            V v;
            return ((v = get(key)) != null)
                    ? v
                    : defaultValue;
        }
    }

    @jdk.internal.ValueBased
    static final class Map1<K,V> extends AbstractImmutableMap<K,V> {
        @Stable
        private final K k0;
        @Stable
        private final V v0;

        Map1(K k0, V v0) {
            this.k0 = Objects.requireNonNull(k0);
            this.v0 = Objects.requireNonNull(v0);
        }

        @Override
        public Set<Map.Entry<K,V>> entrySet() {
            return Set.of(new KeyValueHolder<>(k0, v0));
        }

        @Override
        public V get(Object o) {
            return o.equals(k0) ? v0 : null; // implicit nullcheck of o
        }

        @Override
        public boolean containsKey(Object o) {
            return o.equals(k0); // implicit nullcheck of o
        }

        @Override
        public boolean containsValue(Object o) {
            return o.equals(v0); // implicit nullcheck of o
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @java.io.Serial
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw new InvalidObjectException("not serial proxy");
        }

        @java.io.Serial
        private Object writeReplace() {
            return new CollSer(CollSer.IMM_MAP, k0, v0);
        }

        @Override
        public int hashCode() {
            return k0.hashCode() ^ v0.hashCode();
        }
    }

    /**
     * An array-based Map implementation. There is a single array "table" that
     * contains keys and values interleaved: table[0] is kA, table[1] is vA,
     * table[2] is kB, table[3] is vB, etc. The table size must be even. It must
     * also be strictly larger than the size (the number of key-value pairs contained
     * in the map) so that at least one null key is always present.
     * @param <K> the key type
     * @param <V> the value type
     */
    @jdk.internal.ValueBased
    static final class MapN<K,V> extends AbstractImmutableMap<K,V> {

        @Stable
        final Object[] table; // pairs of key, value

        @Stable
        final int size; // number of pairs

        MapN(Object... input) {
            if ((input.length & 1) != 0) { // implicit nullcheck of input
                throw new InternalError("length is odd");
            }
            size = input.length >> 1;

            int len = EXPAND_FACTOR * input.length;
            len = (len + 1) & ~1; // ensure table is even length
            table = new Object[len];

            for (int i = 0; i < input.length; i += 2) {
                @SuppressWarnings("unchecked")
                    K k = Objects.requireNonNull((K)input[i]);
                @SuppressWarnings("unchecked")
                    V v = Objects.requireNonNull((V)input[i+1]);
                int idx = probe(k);
                if (idx >= 0) {
                    throw new IllegalArgumentException("duplicate key: " + k);
                } else {
                    int dest = -(idx + 1);
                    table[dest] = k;
                    table[dest+1] = v;
                }
            }
        }

        @Override
        public boolean containsKey(Object o) {
            Objects.requireNonNull(o);
            return size > 0 && probe(o) >= 0;
        }

        @Override
        public boolean containsValue(Object o) {
            Objects.requireNonNull(o);
            for (int i = 1; i < table.length; i += 2) {
                Object v = table[i];
                if (v != null && o.equals(v)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            for (int i = 0; i < table.length; i += 2) {
                Object k = table[i];
                if (k != null) {
                    hash += k.hashCode() ^ table[i + 1].hashCode();
                }
            }
            return hash;
        }

        @Override
        @SuppressWarnings("unchecked")
        public V get(Object o) {
            if (size == 0) {
                Objects.requireNonNull(o);
                return null;
            }
            int i = probe(o);
            if (i >= 0) {
                return (V)table[i+1];
            } else {
                return null;
            }
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public boolean isEmpty() {
            return size == 0;
        }

        class MapNIterator implements Iterator<Map.Entry<K,V>> {

            private int remaining;

            private int idx;

            MapNIterator() {
                remaining = size;
                // pick an even starting index in the [0 .. table.length-1]
                // range randomly based on SALT32L
                idx = (int) ((SALT32L * (table.length >> 1)) >>> 32) << 1;
            }

            @Override
            public boolean hasNext() {
                return remaining > 0;
            }

            private int nextIndex() {
                int idx = this.idx;
                if (REVERSE) {
                    if ((idx += 2) >= table.length) {
                        idx = 0;
                    }
                } else {
                    if ((idx -= 2) < 0) {
                        idx = table.length - 2;
                    }
                }
                return this.idx = idx;
            }

            @Override
            public Map.Entry<K,V> next() {
                if (remaining > 0) {
                    int idx;
                    while (table[idx = nextIndex()] == null) {}
                    @SuppressWarnings("unchecked")
                    Map.Entry<K,V> e =
                            new KeyValueHolder<>((K)table[idx], (V)table[idx+1]);
                    remaining--;
                    return e;
                } else {
                    throw new NoSuchElementException();
                }
            }
        }

        @Override
        public Set<Map.Entry<K,V>> entrySet() {
            return new AbstractSet<>() {
                @Override
                public int size() {
                    return MapN.this.size;
                }

                @Override
                public Iterator<Map.Entry<K,V>> iterator() {
                    return new MapNIterator();
                }
            };
        }

        // returns index at which the probe key is present; or if absent,
        // (-i - 1) where i is location where element should be inserted.
        // Callers are relying on this method to perform an implicit nullcheck
        // of pk.
        private int probe(Object pk) {
            int idx = Math.floorMod(pk.hashCode(), table.length >> 1) << 1;
            while (true) {
                @SuppressWarnings("unchecked")
                K ek = (K)table[idx];
                if (ek == null) {
                    return -idx - 1;
                } else if (pk.equals(ek)) {
                    return idx;
                } else if ((idx += 2) == table.length) {
                    idx = 0;
                }
            }
        }

        @java.io.Serial
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw new InvalidObjectException("not serial proxy");
        }

        @java.io.Serial
        private Object writeReplace() {
            Object[] array = new Object[2 * size];
            int len = table.length;
            int dest = 0;
            for (int i = 0; i < len; i += 2) {
                if (table[i] != null) {
                    array[dest++] = table[i];
                    array[dest++] = table[i+1];
                }
            }
            return new CollSer(CollSer.IMM_MAP, array);
        }
    }
}

// ---------- Serialization Proxy ----------

/**
 * A unified serialization proxy class for the immutable collections.
 *
 * @serial
 * @since 9
 */
final class CollSer implements Serializable {
    @java.io.Serial
    private static final long serialVersionUID = 6309168927139932177L;

    static final int IMM_LIST       = 1;
    static final int IMM_SET        = 2;
    static final int IMM_MAP        = 3;
    static final int IMM_LIST_NULLS = 4;

    /**
     * Indicates the type of collection that is serialized.
     * The low order 8 bits have the value 1 for an immutable
     * {@code List}, 2 for an immutable {@code Set}, 3 for
     * an immutable {@code Map}, and 4 for an immutable
     * {@code List} that allows null elements.
     *
     * Any other value causes an
     * {@link InvalidObjectException} to be thrown. The high
     * order 24 bits are zero when an instance is serialized,
     * and they are ignored when an instance is deserialized.
     * They can thus be used by future implementations without
     * causing compatibility issues.
     *
     * <p>The tag value also determines the interpretation of the
     * transient {@code Object[] array} field.
     * For {@code List} and {@code Set}, the array's length is the size
     * of the collection, and the array contains the elements of the collection.
     * Null elements are not allowed. For {@code Set}, duplicate elements
     * are not allowed.
     *
     * <p>For {@code Map}, the array's length is twice the number of mappings
     * present in the map. The array length is necessarily even.
     * The array contains a succession of key and value pairs:
     * {@code k1, v1, k2, v2, ..., kN, vN.} Nulls are not allowed,
     * and duplicate keys are not allowed.
     *
     * @serial
     * @since 9
     */
    private final int tag;

    /**
     * @serial
     * @since 9
     */
    private transient Object[] array;

    CollSer(int t, Object... a) {
        tag = t;
        array = a;
    }

    /**
     * Reads objects from the stream and stores them
     * in the transient {@code Object[] array} field.
     *
     * @serialData
     * A nonnegative int, indicating the count of objects,
     * followed by that many objects.
     *
     * @param ois the ObjectInputStream from which data is read
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a serialized class cannot be loaded
     * @throws InvalidObjectException if the count is negative
     * @since 9
     */
    @java.io.Serial
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        int len = ois.readInt();

        if (len < 0) {
            throw new InvalidObjectException("negative length " + len);
        }

        SharedSecrets.getJavaObjectInputStreamAccess().checkArray(ois, Object[].class, len);
        Object[] a = new Object[len];
        for (int i = 0; i < len; i++) {
            a[i] = ois.readObject();
        }

        array = a;
    }

    /**
     * Writes objects to the stream from
     * the transient {@code Object[] array} field.
     *
     * @serialData
     * A nonnegative int, indicating the count of objects,
     * followed by that many objects.
     *
     * @param oos the ObjectOutputStream to which data is written
     * @throws IOException if an I/O error occurs
     * @since 9
     */
    @java.io.Serial
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        oos.writeInt(array.length);
        for (int i = 0; i < array.length; i++) {
            oos.writeObject(array[i]);
        }
    }

    /**
     * Creates and returns an immutable collection from this proxy class.
     * The instance returned is created as if by calling one of the
     * static factory methods for
     * <a href="List.html#unmodifiable">List</a>,
     * <a href="Map.html#unmodifiable">Map</a>, or
     * <a href="Set.html#unmodifiable">Set</a>.
     * This proxy class is the serial form for all immutable collection instances,
     * regardless of implementation type. This is necessary to ensure that the
     * existence of any particular implementation type is kept out of the
     * serialized form.
     *
     * @return a collection created from this proxy object
     * @throws InvalidObjectException if the tag value is illegal or if an exception
     *         is thrown during creation of the collection
     * @throws ObjectStreamException if another serialization error has occurred
     * @since 9
     */
    @java.io.Serial
    private Object readResolve() throws ObjectStreamException {
        try {
            if (array == null) {
                throw new InvalidObjectException("null array");
            }

            // use low order 8 bits to indicate "kind"
            // ignore high order 24 bits
            switch (tag & 0xff) {
                case IMM_LIST:
                    return List.of(array);
                case IMM_LIST_NULLS:
                    return ImmutableCollections.listFromTrustedArrayNullsAllowed(
                            Arrays.copyOf(array, array.length, Object[].class));
                case IMM_SET:
                    return Set.of(array);
                case IMM_MAP:
                    if (array.length == 0) {
                        return ImmutableCollections.EMPTY_MAP;
                    } else if (array.length == 2) {
                        return new ImmutableCollections.Map1<>(array[0], array[1]);
                    } else {
                        return new ImmutableCollections.MapN<>(array);
                    }
                default:
                    throw new InvalidObjectException(String.format("invalid flags 0x%x", tag));
            }
        } catch (NullPointerException|IllegalArgumentException ex) {
            InvalidObjectException ioe = new InvalidObjectException("invalid object");
            ioe.initCause(ex);
            throw ioe;
        }
    }
}
