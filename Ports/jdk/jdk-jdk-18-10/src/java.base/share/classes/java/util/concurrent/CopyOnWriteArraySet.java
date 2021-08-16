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

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A {@link Set} that uses an internal {@link CopyOnWriteArrayList}
 * for all of its operations.  Thus, it shares the same basic properties:
 * <ul>
 *  <li>It is best suited for applications in which set sizes generally
 *       stay small, read-only operations
 *       vastly outnumber mutative operations, and you need
 *       to prevent interference among threads during traversal.
 *  <li>It is thread-safe.
 *  <li>Mutative operations ({@code add}, {@code set}, {@code remove}, etc.)
 *      are expensive since they usually entail copying the entire underlying
 *      array.
 *  <li>Iterators do not support the mutative {@code remove} operation.
 *  <li>Traversal via iterators is fast and cannot encounter
 *      interference from other threads. Iterators rely on
 *      unchanging snapshots of the array at the time the iterators were
 *      constructed.
 * </ul>
 *
 * <p><b>Sample Usage.</b> The following code sketch uses a
 * copy-on-write set to maintain a set of Handler objects that
 * perform some action upon state updates.
 *
 * <pre> {@code
 * class Handler { void handle() { ... } }
 *
 * class X {
 *   private final CopyOnWriteArraySet<Handler> handlers
 *     = new CopyOnWriteArraySet<>();
 *   public void addHandler(Handler h) { handlers.add(h); }
 *
 *   private long internalState;
 *   private synchronized void changeState() { internalState = ...; }
 *
 *   public void update() {
 *     changeState();
 *     for (Handler handler : handlers)
 *       handler.handle();
 *   }
 * }}</pre>
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 * Java Collections Framework</a>.
 *
 * @see CopyOnWriteArrayList
 * @since 1.5
 * @author Doug Lea
 * @param <E> the type of elements held in this set
 */
public class CopyOnWriteArraySet<E> extends AbstractSet<E>
        implements java.io.Serializable {
    private static final long serialVersionUID = 5457747651344034263L;

    private final CopyOnWriteArrayList<E> al;

    /**
     * Creates an empty set.
     */
    public CopyOnWriteArraySet() {
        al = new CopyOnWriteArrayList<E>();
    }

    /**
     * Creates a set containing all of the elements of the specified
     * collection.
     *
     * @param c the collection of elements to initially contain
     * @throws NullPointerException if the specified collection is null
     */
    public CopyOnWriteArraySet(Collection<? extends E> c) {
        if (c.getClass() == CopyOnWriteArraySet.class) {
            @SuppressWarnings("unchecked") CopyOnWriteArraySet<E> cc =
                (CopyOnWriteArraySet<E>)c;
            al = new CopyOnWriteArrayList<E>(cc.al);
        }
        else {
            al = new CopyOnWriteArrayList<E>();
            al.addAllAbsent(c);
        }
    }

    /**
     * Returns the number of elements in this set.
     *
     * @return the number of elements in this set
     */
    public int size() {
        return al.size();
    }

    /**
     * Returns {@code true} if this set contains no elements.
     *
     * @return {@code true} if this set contains no elements
     */
    public boolean isEmpty() {
        return al.isEmpty();
    }

    /**
     * Returns {@code true} if this set contains the specified element.
     * More formally, returns {@code true} if and only if this set
     * contains an element {@code e} such that {@code Objects.equals(o, e)}.
     *
     * @param o element whose presence in this set is to be tested
     * @return {@code true} if this set contains the specified element
     */
    public boolean contains(Object o) {
        return al.contains(o);
    }

    /**
     * Returns an array containing all of the elements in this set.
     * If this set makes any guarantees as to what order its elements
     * are returned by its iterator, this method must return the
     * elements in the same order.
     *
     * <p>The returned array will be "safe" in that no references to it
     * are maintained by this set.  (In other words, this method must
     * allocate a new array even if this set is backed by an array).
     * The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all the elements in this set
     */
    public Object[] toArray() {
        return al.toArray();
    }

    /**
     * Returns an array containing all of the elements in this set; the
     * runtime type of the returned array is that of the specified array.
     * If the set fits in the specified array, it is returned therein.
     * Otherwise, a new array is allocated with the runtime type of the
     * specified array and the size of this set.
     *
     * <p>If this set fits in the specified array with room to spare
     * (i.e., the array has more elements than this set), the element in
     * the array immediately following the end of the set is set to
     * {@code null}.  (This is useful in determining the length of this
     * set <i>only</i> if the caller knows that this set does not contain
     * any null elements.)
     *
     * <p>If this set makes any guarantees as to what order its elements
     * are returned by its iterator, this method must return the elements
     * in the same order.
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *
     * <p>Suppose {@code x} is a set known to contain only strings.
     * The following code can be used to dump the set into a newly allocated
     * array of {@code String}:
     *
     * <pre> {@code String[] y = x.toArray(new String[0]);}</pre>
     *
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param a the array into which the elements of this set are to be
     *        stored, if it is big enough; otherwise, a new array of the same
     *        runtime type is allocated for this purpose.
     * @return an array containing all the elements in this set
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in this
     *         set
     * @throws NullPointerException if the specified array is null
     */
    public <T> T[] toArray(T[] a) {
        return al.toArray(a);
    }

    /**
     * Removes all of the elements from this set.
     * The set will be empty after this call returns.
     */
    public void clear() {
        al.clear();
    }

    /**
     * Removes the specified element from this set if it is present.
     * More formally, removes an element {@code e} such that
     * {@code Objects.equals(o, e)}, if this set contains such an element.
     * Returns {@code true} if this set contained the element (or
     * equivalently, if this set changed as a result of the call).
     * (This set will not contain the element once the call returns.)
     *
     * @param o object to be removed from this set, if present
     * @return {@code true} if this set contained the specified element
     */
    public boolean remove(Object o) {
        return al.remove(o);
    }

    /**
     * Adds the specified element to this set if it is not already present.
     * More formally, adds the specified element {@code e} to this set if
     * the set contains no element {@code e2} such that
     * {@code Objects.equals(e, e2)}.
     * If this set already contains the element, the call leaves the set
     * unchanged and returns {@code false}.
     *
     * @param e element to be added to this set
     * @return {@code true} if this set did not already contain the specified
     *         element
     */
    public boolean add(E e) {
        return al.addIfAbsent(e);
    }

    /**
     * Returns {@code true} if this set contains all of the elements of the
     * specified collection.  If the specified collection is also a set, this
     * method returns {@code true} if it is a <i>subset</i> of this set.
     *
     * @param  c collection to be checked for containment in this set
     * @return {@code true} if this set contains all of the elements of the
     *         specified collection
     * @throws NullPointerException if the specified collection is null
     * @see #contains(Object)
     */
    public boolean containsAll(Collection<?> c) {
        return (c instanceof Set)
            ? compareSets(al.getArray(), (Set<?>) c) >= 0
            : al.containsAll(c);
    }

    /**
     * Tells whether the objects in snapshot (regarded as a set) are a
     * superset of the given set.
     *
     * @return -1 if snapshot is not a superset, 0 if the two sets
     * contain precisely the same elements, and 1 if snapshot is a
     * proper superset of the given set
     */
    private static int compareSets(Object[] snapshot, Set<?> set) {
        // Uses O(n^2) algorithm, that is only appropriate for small
        // sets, which CopyOnWriteArraySets should be.
        //
        // Optimize up to O(n) if the two sets share a long common prefix,
        // as might happen if one set was created as a copy of the other set.

        final int len = snapshot.length;
        // Mark matched elements to avoid re-checking
        final boolean[] matched = new boolean[len];

        // j is the largest int with matched[i] true for { i | 0 <= i < j }
        int j = 0;
        outer: for (Object x : set) {
            for (int i = j; i < len; i++) {
                if (!matched[i] && Objects.equals(x, snapshot[i])) {
                    matched[i] = true;
                    if (i == j)
                        do { j++; } while (j < len && matched[j]);
                    continue outer;
                }
            }
            return -1;
        }
        return (j == len) ? 0 : 1;
    }

    /**
     * Adds all of the elements in the specified collection to this set if
     * they're not already present.  If the specified collection is also a
     * set, the {@code addAll} operation effectively modifies this set so
     * that its value is the <i>union</i> of the two sets.  The behavior of
     * this operation is undefined if the specified collection is modified
     * while the operation is in progress.
     *
     * @param  c collection containing elements to be added to this set
     * @return {@code true} if this set changed as a result of the call
     * @throws NullPointerException if the specified collection is null
     * @see #add(Object)
     */
    public boolean addAll(Collection<? extends E> c) {
        return al.addAllAbsent(c) > 0;
    }

    /**
     * Removes from this set all of its elements that are contained in the
     * specified collection.  If the specified collection is also a set,
     * this operation effectively modifies this set so that its value is the
     * <i>asymmetric set difference</i> of the two sets.
     *
     * @param  c collection containing elements to be removed from this set
     * @return {@code true} if this set changed as a result of the call
     * @throws ClassCastException if the class of an element of this set
     *         is incompatible with the specified collection
     * (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if this set contains a null element and the
     *         specified collection does not permit null elements
     * (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>),
     *         or if the specified collection is null
     * @see #remove(Object)
     */
    public boolean removeAll(Collection<?> c) {
        return al.removeAll(c);
    }

    /**
     * Retains only the elements in this set that are contained in the
     * specified collection.  In other words, removes from this set all of
     * its elements that are not contained in the specified collection.  If
     * the specified collection is also a set, this operation effectively
     * modifies this set so that its value is the <i>intersection</i> of the
     * two sets.
     *
     * @param  c collection containing elements to be retained in this set
     * @return {@code true} if this set changed as a result of the call
     * @throws ClassCastException if the class of an element of this set
     *         is incompatible with the specified collection
     * (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if this set contains a null element and the
     *         specified collection does not permit null elements
     * (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>),
     *         or if the specified collection is null
     * @see #remove(Object)
     */
    public boolean retainAll(Collection<?> c) {
        return al.retainAll(c);
    }

    /**
     * Returns an iterator over the elements contained in this set
     * in the order in which these elements were added.
     *
     * <p>The returned iterator provides a snapshot of the state of the set
     * when the iterator was constructed. No synchronization is needed while
     * traversing the iterator. The iterator does <em>NOT</em> support the
     * {@code remove} method.
     *
     * @return an iterator over the elements in this set
     */
    public Iterator<E> iterator() {
        return al.iterator();
    }

    /**
     * Compares the specified object with this set for equality.
     * Returns {@code true} if the specified object is the same object
     * as this object, or if it is also a {@link Set} and the elements
     * returned by an {@linkplain Set#iterator() iterator} over the
     * specified set are the same as the elements returned by an
     * iterator over this set.  More formally, the two iterators are
     * considered to return the same elements if they return the same
     * number of elements and for every element {@code e1} returned by
     * the iterator over the specified set, there is an element
     * {@code e2} returned by the iterator over this set such that
     * {@code Objects.equals(e1, e2)}.
     *
     * @param o object to be compared for equality with this set
     * @return {@code true} if the specified object is equal to this set
     */
    public boolean equals(Object o) {
        return (o == this)
            || ((o instanceof Set)
                && compareSets(al.getArray(), (Set<?>) o) == 0);
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean removeIf(Predicate<? super E> filter) {
        return al.removeIf(filter);
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     */
    public void forEach(Consumer<? super E> action) {
        al.forEach(action);
    }

    /**
     * Returns a {@link Spliterator} over the elements in this set in the order
     * in which these elements were added.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#IMMUTABLE},
     * {@link Spliterator#DISTINCT}, {@link Spliterator#SIZED}, and
     * {@link Spliterator#SUBSIZED}.
     *
     * <p>The spliterator provides a snapshot of the state of the set
     * when the spliterator was constructed. No synchronization is needed while
     * operating on the spliterator.
     *
     * @return a {@code Spliterator} over the elements in this set
     * @since 1.8
     */
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator
            (al.getArray(), Spliterator.IMMUTABLE | Spliterator.DISTINCT);
    }
}
