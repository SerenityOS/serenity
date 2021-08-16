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
 * Written by Doug Lea and Martin Buchholz with assistance from members of
 * JCP JSR-166 Expert Group and released to the public domain, as explained
 * at http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * An unbounded concurrent {@linkplain Deque deque} based on linked nodes.
 * Concurrent insertion, removal, and access operations execute safely
 * across multiple threads.
 * A {@code ConcurrentLinkedDeque} is an appropriate choice when
 * many threads will share access to a common collection.
 * Like most other concurrent collection implementations, this class
 * does not permit the use of {@code null} elements.
 *
 * <p>Iterators and spliterators are
 * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
 *
 * <p>Beware that, unlike in most collections, the {@code size} method
 * is <em>NOT</em> a constant-time operation. Because of the
 * asynchronous nature of these deques, determining the current number
 * of elements requires a traversal of the elements, and so may report
 * inaccurate results if this collection is modified during traversal.
 *
 * <p>Bulk operations that add, remove, or examine multiple elements,
 * such as {@link #addAll}, {@link #removeIf} or {@link #forEach},
 * are <em>not</em> guaranteed to be performed atomically.
 * For example, a {@code forEach} traversal concurrent with an {@code
 * addAll} operation might observe only some of the added elements.
 *
 * <p>This class and its iterator implement all of the <em>optional</em>
 * methods of the {@link Deque} and {@link Iterator} interfaces.
 *
 * <p>Memory consistency effects: As with other concurrent collections,
 * actions in a thread prior to placing an object into a
 * {@code ConcurrentLinkedDeque}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions subsequent to the access or removal of that element from
 * the {@code ConcurrentLinkedDeque} in another thread.
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 * Java Collections Framework</a>.
 *
 * @since 1.7
 * @author Doug Lea
 * @author Martin Buchholz
 * @param <E> the type of elements held in this deque
 */
public class ConcurrentLinkedDeque<E>
    extends AbstractCollection<E>
    implements Deque<E>, java.io.Serializable {

    /*
     * This is an implementation of a concurrent lock-free deque
     * supporting interior removes but not interior insertions, as
     * required to support the entire Deque interface.
     *
     * We extend the techniques developed for ConcurrentLinkedQueue and
     * LinkedTransferQueue (see the internal docs for those classes).
     * Understanding the ConcurrentLinkedQueue implementation is a
     * prerequisite for understanding the implementation of this class.
     *
     * The data structure is a symmetrical doubly-linked "GC-robust"
     * linked list of nodes.  We minimize the number of volatile writes
     * using two techniques: advancing multiple hops with a single CAS
     * and mixing volatile and non-volatile writes of the same memory
     * locations.
     *
     * A node contains the expected E ("item") and links to predecessor
     * ("prev") and successor ("next") nodes:
     *
     * class Node<E> { volatile Node<E> prev, next; volatile E item; }
     *
     * A node p is considered "live" if it contains a non-null item
     * (p.item != null).  When an item is CASed to null, the item is
     * atomically logically deleted from the collection.
     *
     * At any time, there is precisely one "first" node with a null
     * prev reference that terminates any chain of prev references
     * starting at a live node.  Similarly there is precisely one
     * "last" node terminating any chain of next references starting at
     * a live node.  The "first" and "last" nodes may or may not be live.
     * The "first" and "last" nodes are always mutually reachable.
     *
     * A new element is added atomically by CASing the null prev or
     * next reference in the first or last node to a fresh node
     * containing the element.  The element's node atomically becomes
     * "live" at that point.
     *
     * A node is considered "active" if it is a live node, or the
     * first or last node.  Active nodes cannot be unlinked.
     *
     * A "self-link" is a next or prev reference that is the same node:
     *   p.prev == p  or  p.next == p
     * Self-links are used in the node unlinking process.  Active nodes
     * never have self-links.
     *
     * A node p is active if and only if:
     *
     * p.item != null ||
     * (p.prev == null && p.next != p) ||
     * (p.next == null && p.prev != p)
     *
     * The deque object has two node references, "head" and "tail".
     * The head and tail are only approximations to the first and last
     * nodes of the deque.  The first node can always be found by
     * following prev pointers from head; likewise for tail.  However,
     * it is permissible for head and tail to be referring to deleted
     * nodes that have been unlinked and so may not be reachable from
     * any live node.
     *
     * There are 3 stages of node deletion;
     * "logical deletion", "unlinking", and "gc-unlinking".
     *
     * 1. "logical deletion" by CASing item to null atomically removes
     * the element from the collection, and makes the containing node
     * eligible for unlinking.
     *
     * 2. "unlinking" makes a deleted node unreachable from active
     * nodes, and thus eventually reclaimable by GC.  Unlinked nodes
     * may remain reachable indefinitely from an iterator.
     *
     * Physical node unlinking is merely an optimization (albeit a
     * critical one), and so can be performed at our convenience.  At
     * any time, the set of live nodes maintained by prev and next
     * links are identical, that is, the live nodes found via next
     * links from the first node is equal to the elements found via
     * prev links from the last node.  However, this is not true for
     * nodes that have already been logically deleted - such nodes may
     * be reachable in one direction only.
     *
     * 3. "gc-unlinking" takes unlinking further by making active
     * nodes unreachable from deleted nodes, making it easier for the
     * GC to reclaim future deleted nodes.  This step makes the data
     * structure "gc-robust", as first described in detail by Boehm
     * (http://portal.acm.org/citation.cfm?doid=503272.503282).
     *
     * GC-unlinked nodes may remain reachable indefinitely from an
     * iterator, but unlike unlinked nodes, are never reachable from
     * head or tail.
     *
     * Making the data structure GC-robust will eliminate the risk of
     * unbounded memory retention with conservative GCs and is likely
     * to improve performance with generational GCs.
     *
     * When a node is dequeued at either end, e.g. via poll(), we would
     * like to break any references from the node to active nodes.  We
     * develop further the use of self-links that was very effective in
     * other concurrent collection classes.  The idea is to replace
     * prev and next pointers with special values that are interpreted
     * to mean off-the-list-at-one-end.  These are approximations, but
     * good enough to preserve the properties we want in our
     * traversals, e.g. we guarantee that a traversal will never visit
     * the same element twice, but we don't guarantee whether a
     * traversal that runs out of elements will be able to see more
     * elements later after enqueues at that end.  Doing gc-unlinking
     * safely is particularly tricky, since any node can be in use
     * indefinitely (for example by an iterator).  We must ensure that
     * the nodes pointed at by head/tail never get gc-unlinked, since
     * head/tail are needed to get "back on track" by other nodes that
     * are gc-unlinked.  gc-unlinking accounts for much of the
     * implementation complexity.
     *
     * Since neither unlinking nor gc-unlinking are necessary for
     * correctness, there are many implementation choices regarding
     * frequency (eagerness) of these operations.  Since volatile
     * reads are likely to be much cheaper than CASes, saving CASes by
     * unlinking multiple adjacent nodes at a time may be a win.
     * gc-unlinking can be performed rarely and still be effective,
     * since it is most important that long chains of deleted nodes
     * are occasionally broken.
     *
     * The actual representation we use is that p.next == p means to
     * goto the first node (which in turn is reached by following prev
     * pointers from head), and p.next == null && p.prev == p means
     * that the iteration is at an end and that p is a (static final)
     * dummy node, NEXT_TERMINATOR, and not the last active node.
     * Finishing the iteration when encountering such a TERMINATOR is
     * good enough for read-only traversals, so such traversals can use
     * p.next == null as the termination condition.  When we need to
     * find the last (active) node, for enqueueing a new node, we need
     * to check whether we have reached a TERMINATOR node; if so,
     * restart traversal from tail.
     *
     * The implementation is completely directionally symmetrical,
     * except that most public methods that iterate through the list
     * follow next pointers, in the "forward" direction.
     *
     * We believe (without full proof) that all single-element Deque
     * operations that operate directly at the two ends of the Deque
     * (e.g., addFirst, peekLast, pollLast) are linearizable (see
     * Herlihy and Shavit's book).  However, some combinations of
     * operations are known not to be linearizable.  In particular,
     * when an addFirst(A) is racing with pollFirst() removing B, it
     * is possible for an observer iterating over the elements to
     * observe first [A B C] and then [A C], even though no interior
     * removes are ever performed.  Nevertheless, iterators behave
     * reasonably, providing the "weakly consistent" guarantees.
     *
     * Empirically, microbenchmarks suggest that this class adds about
     * 40% overhead relative to ConcurrentLinkedQueue, which feels as
     * good as we can hope for.
     */

    private static final long serialVersionUID = 876323262645176354L;

    /**
     * A node from which the first node on list (that is, the unique node p
     * with p.prev == null && p.next != p) can be reached in O(1) time.
     * Invariants:
     * - the first node is always O(1) reachable from head via prev links
     * - all live nodes are reachable from the first node via succ()
     * - head != null
     * - (tmp = head).next != tmp || tmp != head
     * - head is never gc-unlinked (but may be unlinked)
     * Non-invariants:
     * - head.item may or may not be null
     * - head may not be reachable from the first or last node, or from tail
     */
    private transient volatile Node<E> head;

    /**
     * A node from which the last node on list (that is, the unique node p
     * with p.next == null && p.prev != p) can be reached in O(1) time.
     * Invariants:
     * - the last node is always O(1) reachable from tail via next links
     * - all live nodes are reachable from the last node via pred()
     * - tail != null
     * - tail is never gc-unlinked (but may be unlinked)
     * Non-invariants:
     * - tail.item may or may not be null
     * - tail may not be reachable from the first or last node, or from head
     */
    private transient volatile Node<E> tail;

    private static final Node<Object> PREV_TERMINATOR, NEXT_TERMINATOR;

    @SuppressWarnings("unchecked")
    Node<E> prevTerminator() {
        return (Node<E>) PREV_TERMINATOR;
    }

    @SuppressWarnings("unchecked")
    Node<E> nextTerminator() {
        return (Node<E>) NEXT_TERMINATOR;
    }

    static final class Node<E> {
        volatile Node<E> prev;
        volatile E item;
        volatile Node<E> next;
    }

    /**
     * Returns a new node holding item.  Uses relaxed write because item
     * can only be seen after piggy-backing publication via CAS.
     */
    static <E> Node<E> newNode(E item) {
        Node<E> node = new Node<E>();
        ITEM.set(node, item);
        return node;
    }

    /**
     * Links e as first element.
     */
    private void linkFirst(E e) {
        final Node<E> newNode = newNode(Objects.requireNonNull(e));

        restartFromHead:
        for (;;)
            for (Node<E> h = head, p = h, q;;) {
                if ((q = p.prev) != null &&
                    (q = (p = q).prev) != null)
                    // Check for head updates every other hop.
                    // If p == q, we are sure to follow head instead.
                    p = (h != (h = head)) ? h : q;
                else if (p.next == p) // PREV_TERMINATOR
                    continue restartFromHead;
                else {
                    // p is first node
                    NEXT.set(newNode, p); // CAS piggyback
                    if (PREV.compareAndSet(p, null, newNode)) {
                        // Successful CAS is the linearization point
                        // for e to become an element of this deque,
                        // and for newNode to become "live".
                        if (p != h) // hop two nodes at a time; failure is OK
                            HEAD.weakCompareAndSet(this, h, newNode);
                        return;
                    }
                    // Lost CAS race to another thread; re-read prev
                }
            }
    }

    /**
     * Links e as last element.
     */
    private void linkLast(E e) {
        final Node<E> newNode = newNode(Objects.requireNonNull(e));

        restartFromTail:
        for (;;)
            for (Node<E> t = tail, p = t, q;;) {
                if ((q = p.next) != null &&
                    (q = (p = q).next) != null)
                    // Check for tail updates every other hop.
                    // If p == q, we are sure to follow tail instead.
                    p = (t != (t = tail)) ? t : q;
                else if (p.prev == p) // NEXT_TERMINATOR
                    continue restartFromTail;
                else {
                    // p is last node
                    PREV.set(newNode, p); // CAS piggyback
                    if (NEXT.compareAndSet(p, null, newNode)) {
                        // Successful CAS is the linearization point
                        // for e to become an element of this deque,
                        // and for newNode to become "live".
                        if (p != t) // hop two nodes at a time; failure is OK
                            TAIL.weakCompareAndSet(this, t, newNode);
                        return;
                    }
                    // Lost CAS race to another thread; re-read next
                }
            }
    }

    private static final int HOPS = 2;

    /**
     * Unlinks non-null node x.
     */
    void unlink(Node<E> x) {
        // assert x != null;
        // assert x.item == null;
        // assert x != PREV_TERMINATOR;
        // assert x != NEXT_TERMINATOR;

        final Node<E> prev = x.prev;
        final Node<E> next = x.next;
        if (prev == null) {
            unlinkFirst(x, next);
        } else if (next == null) {
            unlinkLast(x, prev);
        } else {
            // Unlink interior node.
            //
            // This is the common case, since a series of polls at the
            // same end will be "interior" removes, except perhaps for
            // the first one, since end nodes cannot be unlinked.
            //
            // At any time, all active nodes are mutually reachable by
            // following a sequence of either next or prev pointers.
            //
            // Our strategy is to find the unique active predecessor
            // and successor of x.  Try to fix up their links so that
            // they point to each other, leaving x unreachable from
            // active nodes.  If successful, and if x has no live
            // predecessor/successor, we additionally try to gc-unlink,
            // leaving active nodes unreachable from x, by rechecking
            // that the status of predecessor and successor are
            // unchanged and ensuring that x is not reachable from
            // tail/head, before setting x's prev/next links to their
            // logical approximate replacements, self/TERMINATOR.
            Node<E> activePred, activeSucc;
            boolean isFirst, isLast;
            int hops = 1;

            // Find active predecessor
            for (Node<E> p = prev; ; ++hops) {
                if (p.item != null) {
                    activePred = p;
                    isFirst = false;
                    break;
                }
                Node<E> q = p.prev;
                if (q == null) {
                    if (p.next == p)
                        return;
                    activePred = p;
                    isFirst = true;
                    break;
                }
                else if (p == q)
                    return;
                else
                    p = q;
            }

            // Find active successor
            for (Node<E> p = next; ; ++hops) {
                if (p.item != null) {
                    activeSucc = p;
                    isLast = false;
                    break;
                }
                Node<E> q = p.next;
                if (q == null) {
                    if (p.prev == p)
                        return;
                    activeSucc = p;
                    isLast = true;
                    break;
                }
                else if (p == q)
                    return;
                else
                    p = q;
            }

            // TODO: better HOP heuristics
            if (hops < HOPS
                // always squeeze out interior deleted nodes
                && (isFirst | isLast))
                return;

            // Squeeze out deleted nodes between activePred and
            // activeSucc, including x.
            skipDeletedSuccessors(activePred);
            skipDeletedPredecessors(activeSucc);

            // Try to gc-unlink, if possible
            if ((isFirst | isLast) &&

                // Recheck expected state of predecessor and successor
                (activePred.next == activeSucc) &&
                (activeSucc.prev == activePred) &&
                (isFirst ? activePred.prev == null : activePred.item != null) &&
                (isLast  ? activeSucc.next == null : activeSucc.item != null)) {

                updateHead(); // Ensure x is not reachable from head
                updateTail(); // Ensure x is not reachable from tail

                // Finally, actually gc-unlink
                PREV.setRelease(x, isFirst ? prevTerminator() : x);
                NEXT.setRelease(x, isLast  ? nextTerminator() : x);
            }
        }
    }

    /**
     * Unlinks non-null first node.
     */
    private void unlinkFirst(Node<E> first, Node<E> next) {
        // assert first != null;
        // assert next != null;
        // assert first.item == null;
        for (Node<E> o = null, p = next, q;;) {
            if (p.item != null || (q = p.next) == null) {
                if (o != null && p.prev != p &&
                    NEXT.compareAndSet(first, next, p)) {
                    skipDeletedPredecessors(p);
                    if (first.prev == null &&
                        (p.next == null || p.item != null) &&
                        p.prev == first) {

                        updateHead(); // Ensure o is not reachable from head
                        updateTail(); // Ensure o is not reachable from tail

                        // Finally, actually gc-unlink
                        NEXT.setRelease(o, o);
                        PREV.setRelease(o, prevTerminator());
                    }
                }
                return;
            }
            else if (p == q)
                return;
            else {
                o = p;
                p = q;
            }
        }
    }

    /**
     * Unlinks non-null last node.
     */
    private void unlinkLast(Node<E> last, Node<E> prev) {
        // assert last != null;
        // assert prev != null;
        // assert last.item == null;
        for (Node<E> o = null, p = prev, q;;) {
            if (p.item != null || (q = p.prev) == null) {
                if (o != null && p.next != p &&
                    PREV.compareAndSet(last, prev, p)) {
                    skipDeletedSuccessors(p);
                    if (last.next == null &&
                        (p.prev == null || p.item != null) &&
                        p.next == last) {

                        updateHead(); // Ensure o is not reachable from head
                        updateTail(); // Ensure o is not reachable from tail

                        // Finally, actually gc-unlink
                        PREV.setRelease(o, o);
                        NEXT.setRelease(o, nextTerminator());
                    }
                }
                return;
            }
            else if (p == q)
                return;
            else {
                o = p;
                p = q;
            }
        }
    }

    /**
     * Guarantees that any node which was unlinked before a call to
     * this method will be unreachable from head after it returns.
     * Does not guarantee to eliminate slack, only that head will
     * point to a node that was active while this method was running.
     */
    private final void updateHead() {
        // Either head already points to an active node, or we keep
        // trying to cas it to the first node until it does.
        Node<E> h, p, q;
        restartFromHead:
        while ((h = head).item == null && (p = h.prev) != null) {
            for (;;) {
                if ((q = p.prev) == null ||
                    (q = (p = q).prev) == null) {
                    // It is possible that p is PREV_TERMINATOR,
                    // but if so, the CAS is guaranteed to fail.
                    if (HEAD.compareAndSet(this, h, p))
                        return;
                    else
                        continue restartFromHead;
                }
                else if (h != head)
                    continue restartFromHead;
                else
                    p = q;
            }
        }
    }

    /**
     * Guarantees that any node which was unlinked before a call to
     * this method will be unreachable from tail after it returns.
     * Does not guarantee to eliminate slack, only that tail will
     * point to a node that was active while this method was running.
     */
    private final void updateTail() {
        // Either tail already points to an active node, or we keep
        // trying to cas it to the last node until it does.
        Node<E> t, p, q;
        restartFromTail:
        while ((t = tail).item == null && (p = t.next) != null) {
            for (;;) {
                if ((q = p.next) == null ||
                    (q = (p = q).next) == null) {
                    // It is possible that p is NEXT_TERMINATOR,
                    // but if so, the CAS is guaranteed to fail.
                    if (TAIL.compareAndSet(this, t, p))
                        return;
                    else
                        continue restartFromTail;
                }
                else if (t != tail)
                    continue restartFromTail;
                else
                    p = q;
            }
        }
    }

    private void skipDeletedPredecessors(Node<E> x) {
        whileActive:
        do {
            Node<E> prev = x.prev;
            // assert prev != null;
            // assert x != NEXT_TERMINATOR;
            // assert x != PREV_TERMINATOR;
            Node<E> p = prev;
            findActive:
            for (;;) {
                if (p.item != null)
                    break findActive;
                Node<E> q = p.prev;
                if (q == null) {
                    if (p.next == p)
                        continue whileActive;
                    break findActive;
                }
                else if (p == q)
                    continue whileActive;
                else
                    p = q;
            }

            // found active CAS target
            if (prev == p || PREV.compareAndSet(x, prev, p))
                return;

        } while (x.item != null || x.next == null);
    }

    private void skipDeletedSuccessors(Node<E> x) {
        whileActive:
        do {
            Node<E> next = x.next;
            // assert next != null;
            // assert x != NEXT_TERMINATOR;
            // assert x != PREV_TERMINATOR;
            Node<E> p = next;
            findActive:
            for (;;) {
                if (p.item != null)
                    break findActive;
                Node<E> q = p.next;
                if (q == null) {
                    if (p.prev == p)
                        continue whileActive;
                    break findActive;
                }
                else if (p == q)
                    continue whileActive;
                else
                    p = q;
            }

            // found active CAS target
            if (next == p || NEXT.compareAndSet(x, next, p))
                return;

        } while (x.item != null || x.prev == null);
    }

    /**
     * Returns the successor of p, or the first node if p.next has been
     * linked to self, which will only be true if traversing with a
     * stale pointer that is now off the list.
     */
    final Node<E> succ(Node<E> p) {
        // TODO: should we skip deleted nodes here?
        if (p == (p = p.next))
            p = first();
        return p;
    }

    /**
     * Returns the predecessor of p, or the last node if p.prev has been
     * linked to self, which will only be true if traversing with a
     * stale pointer that is now off the list.
     */
    final Node<E> pred(Node<E> p) {
        if (p == (p = p.prev))
            p = last();
        return p;
    }

    /**
     * Returns the first node, the unique node p for which:
     *     p.prev == null && p.next != p
     * The returned node may or may not be logically deleted.
     * Guarantees that head is set to the returned node.
     */
    Node<E> first() {
        restartFromHead:
        for (;;)
            for (Node<E> h = head, p = h, q;;) {
                if ((q = p.prev) != null &&
                    (q = (p = q).prev) != null)
                    // Check for head updates every other hop.
                    // If p == q, we are sure to follow head instead.
                    p = (h != (h = head)) ? h : q;
                else if (p == h
                         // It is possible that p is PREV_TERMINATOR,
                         // but if so, the CAS is guaranteed to fail.
                         || HEAD.compareAndSet(this, h, p))
                    return p;
                else
                    continue restartFromHead;
            }
    }

    /**
     * Returns the last node, the unique node p for which:
     *     p.next == null && p.prev != p
     * The returned node may or may not be logically deleted.
     * Guarantees that tail is set to the returned node.
     */
    Node<E> last() {
        restartFromTail:
        for (;;)
            for (Node<E> t = tail, p = t, q;;) {
                if ((q = p.next) != null &&
                    (q = (p = q).next) != null)
                    // Check for tail updates every other hop.
                    // If p == q, we are sure to follow tail instead.
                    p = (t != (t = tail)) ? t : q;
                else if (p == t
                         // It is possible that p is NEXT_TERMINATOR,
                         // but if so, the CAS is guaranteed to fail.
                         || TAIL.compareAndSet(this, t, p))
                    return p;
                else
                    continue restartFromTail;
            }
    }

    // Minor convenience utilities

    /**
     * Returns element unless it is null, in which case throws
     * NoSuchElementException.
     *
     * @param v the element
     * @return the element
     */
    private E screenNullResult(E v) {
        if (v == null)
            throw new NoSuchElementException();
        return v;
    }

    /**
     * Constructs an empty deque.
     */
    public ConcurrentLinkedDeque() {
        head = tail = new Node<E>();
    }

    /**
     * Constructs a deque initially containing the elements of
     * the given collection, added in traversal order of the
     * collection's iterator.
     *
     * @param c the collection of elements to initially contain
     * @throws NullPointerException if the specified collection or any
     *         of its elements are null
     */
    public ConcurrentLinkedDeque(Collection<? extends E> c) {
        // Copy c into a private chain of Nodes
        Node<E> h = null, t = null;
        for (E e : c) {
            Node<E> newNode = newNode(Objects.requireNonNull(e));
            if (h == null)
                h = t = newNode;
            else {
                NEXT.set(t, newNode);
                PREV.set(newNode, t);
                t = newNode;
            }
        }
        initHeadTail(h, t);
    }

    /**
     * Initializes head and tail, ensuring invariants hold.
     */
    private void initHeadTail(Node<E> h, Node<E> t) {
        if (h == t) {
            if (h == null)
                h = t = new Node<E>();
            else {
                // Avoid edge case of a single Node with non-null item.
                Node<E> newNode = new Node<E>();
                NEXT.set(t, newNode);
                PREV.set(newNode, t);
                t = newNode;
            }
        }
        head = h;
        tail = t;
    }

    /**
     * Inserts the specified element at the front of this deque.
     * As the deque is unbounded, this method will never throw
     * {@link IllegalStateException}.
     *
     * @throws NullPointerException if the specified element is null
     */
    public void addFirst(E e) {
        linkFirst(e);
    }

    /**
     * Inserts the specified element at the end of this deque.
     * As the deque is unbounded, this method will never throw
     * {@link IllegalStateException}.
     *
     * <p>This method is equivalent to {@link #add}.
     *
     * @throws NullPointerException if the specified element is null
     */
    public void addLast(E e) {
        linkLast(e);
    }

    /**
     * Inserts the specified element at the front of this deque.
     * As the deque is unbounded, this method will never return {@code false}.
     *
     * @return {@code true} (as specified by {@link Deque#offerFirst})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offerFirst(E e) {
        linkFirst(e);
        return true;
    }

    /**
     * Inserts the specified element at the end of this deque.
     * As the deque is unbounded, this method will never return {@code false}.
     *
     * <p>This method is equivalent to {@link #add}.
     *
     * @return {@code true} (as specified by {@link Deque#offerLast})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offerLast(E e) {
        linkLast(e);
        return true;
    }

    public E peekFirst() {
        restart: for (;;) {
            E item;
            Node<E> first = first(), p = first;
            while ((item = p.item) == null) {
                if (p == (p = p.next)) continue restart;
                if (p == null)
                    break;
            }
            // recheck for linearizability
            if (first.prev != null) continue restart;
            return item;
        }
    }

    public E peekLast() {
        restart: for (;;) {
            E item;
            Node<E> last = last(), p = last;
            while ((item = p.item) == null) {
                if (p == (p = p.prev)) continue restart;
                if (p == null)
                    break;
            }
            // recheck for linearizability
            if (last.next != null) continue restart;
            return item;
        }
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E getFirst() {
        return screenNullResult(peekFirst());
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E getLast() {
        return screenNullResult(peekLast());
    }

    public E pollFirst() {
        restart: for (;;) {
            for (Node<E> first = first(), p = first;;) {
                final E item;
                if ((item = p.item) != null) {
                    // recheck for linearizability
                    if (first.prev != null) continue restart;
                    if (ITEM.compareAndSet(p, item, null)) {
                        unlink(p);
                        return item;
                    }
                }
                if (p == (p = p.next)) continue restart;
                if (p == null) {
                    if (first.prev != null) continue restart;
                    return null;
                }
            }
        }
    }

    public E pollLast() {
        restart: for (;;) {
            for (Node<E> last = last(), p = last;;) {
                final E item;
                if ((item = p.item) != null) {
                    // recheck for linearizability
                    if (last.next != null) continue restart;
                    if (ITEM.compareAndSet(p, item, null)) {
                        unlink(p);
                        return item;
                    }
                }
                if (p == (p = p.prev)) continue restart;
                if (p == null) {
                    if (last.next != null) continue restart;
                    return null;
                }
            }
        }
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E removeFirst() {
        return screenNullResult(pollFirst());
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E removeLast() {
        return screenNullResult(pollLast());
    }

    // *** Queue and stack methods ***

    /**
     * Inserts the specified element at the tail of this deque.
     * As the deque is unbounded, this method will never return {@code false}.
     *
     * @return {@code true} (as specified by {@link Queue#offer})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        return offerLast(e);
    }

    /**
     * Inserts the specified element at the tail of this deque.
     * As the deque is unbounded, this method will never throw
     * {@link IllegalStateException} or return {@code false}.
     *
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws NullPointerException if the specified element is null
     */
    public boolean add(E e) {
        return offerLast(e);
    }

    public E poll()           { return pollFirst(); }
    public E peek()           { return peekFirst(); }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E remove()         { return removeFirst(); }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E pop()            { return removeFirst(); }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E element()        { return getFirst(); }

    /**
     * @throws NullPointerException {@inheritDoc}
     */
    public void push(E e)     { addFirst(e); }

    /**
     * Removes the first occurrence of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the first element {@code e} such that
     * {@code o.equals(e)} (if such an element exists).
     * Returns {@code true} if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if the deque contained the specified element
     * @throws NullPointerException if the specified element is null
     */
    public boolean removeFirstOccurrence(Object o) {
        Objects.requireNonNull(o);
        for (Node<E> p = first(); p != null; p = succ(p)) {
            final E item;
            if ((item = p.item) != null
                && o.equals(item)
                && ITEM.compareAndSet(p, item, null)) {
                unlink(p);
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the last occurrence of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the last element {@code e} such that
     * {@code o.equals(e)} (if such an element exists).
     * Returns {@code true} if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if the deque contained the specified element
     * @throws NullPointerException if the specified element is null
     */
    public boolean removeLastOccurrence(Object o) {
        Objects.requireNonNull(o);
        for (Node<E> p = last(); p != null; p = pred(p)) {
            final E item;
            if ((item = p.item) != null
                && o.equals(item)
                && ITEM.compareAndSet(p, item, null)) {
                unlink(p);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if this deque contains the specified element.
     * More formally, returns {@code true} if and only if this deque contains
     * at least one element {@code e} such that {@code o.equals(e)}.
     *
     * @param o element whose presence in this deque is to be tested
     * @return {@code true} if this deque contains the specified element
     */
    public boolean contains(Object o) {
        if (o != null) {
            for (Node<E> p = first(); p != null; p = succ(p)) {
                final E item;
                if ((item = p.item) != null && o.equals(item))
                    return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if this collection contains no elements.
     *
     * @return {@code true} if this collection contains no elements
     */
    public boolean isEmpty() {
        return peekFirst() == null;
    }

    /**
     * Returns the number of elements in this deque.  If this deque
     * contains more than {@code Integer.MAX_VALUE} elements, it
     * returns {@code Integer.MAX_VALUE}.
     *
     * <p>Beware that, unlike in most collections, this method is
     * <em>NOT</em> a constant-time operation. Because of the
     * asynchronous nature of these deques, determining the current
     * number of elements requires traversing them all to count them.
     * Additionally, it is possible for the size to change during
     * execution of this method, in which case the returned result
     * will be inaccurate. Thus, this method is typically not very
     * useful in concurrent applications.
     *
     * @return the number of elements in this deque
     */
    public int size() {
        restart: for (;;) {
            int count = 0;
            for (Node<E> p = first(); p != null;) {
                if (p.item != null)
                    if (++count == Integer.MAX_VALUE)
                        break;  // @see Collection.size()
                if (p == (p = p.next))
                    continue restart;
            }
            return count;
        }
    }

    /**
     * Removes the first occurrence of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the first element {@code e} such that
     * {@code o.equals(e)} (if such an element exists).
     * Returns {@code true} if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     * <p>This method is equivalent to {@link #removeFirstOccurrence(Object)}.
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if the deque contained the specified element
     * @throws NullPointerException if the specified element is null
     */
    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }

    /**
     * Appends all of the elements in the specified collection to the end of
     * this deque, in the order that they are returned by the specified
     * collection's iterator.  Attempts to {@code addAll} of a deque to
     * itself result in {@code IllegalArgumentException}.
     *
     * @param c the elements to be inserted into this deque
     * @return {@code true} if this deque changed as a result of the call
     * @throws NullPointerException if the specified collection or any
     *         of its elements are null
     * @throws IllegalArgumentException if the collection is this deque
     */
    public boolean addAll(Collection<? extends E> c) {
        if (c == this)
            // As historically specified in AbstractQueue#addAll
            throw new IllegalArgumentException();

        // Copy c into a private chain of Nodes
        Node<E> beginningOfTheEnd = null, last = null;
        for (E e : c) {
            Node<E> newNode = newNode(Objects.requireNonNull(e));
            if (beginningOfTheEnd == null)
                beginningOfTheEnd = last = newNode;
            else {
                NEXT.set(last, newNode);
                PREV.set(newNode, last);
                last = newNode;
            }
        }
        if (beginningOfTheEnd == null)
            return false;

        // Atomically append the chain at the tail of this collection
        restartFromTail:
        for (;;)
            for (Node<E> t = tail, p = t, q;;) {
                if ((q = p.next) != null &&
                    (q = (p = q).next) != null)
                    // Check for tail updates every other hop.
                    // If p == q, we are sure to follow tail instead.
                    p = (t != (t = tail)) ? t : q;
                else if (p.prev == p) // NEXT_TERMINATOR
                    continue restartFromTail;
                else {
                    // p is last node
                    PREV.set(beginningOfTheEnd, p); // CAS piggyback
                    if (NEXT.compareAndSet(p, null, beginningOfTheEnd)) {
                        // Successful CAS is the linearization point
                        // for all elements to be added to this deque.
                        if (!TAIL.weakCompareAndSet(this, t, last)) {
                            // Try a little harder to update tail,
                            // since we may be adding many elements.
                            t = tail;
                            if (last.next == null)
                                TAIL.weakCompareAndSet(this, t, last);
                        }
                        return true;
                    }
                    // Lost CAS race to another thread; re-read next
                }
            }
    }

    /**
     * Removes all of the elements from this deque.
     */
    public void clear() {
        while (pollFirst() != null)
            ;
    }

    public String toString() {
        String[] a = null;
        restart: for (;;) {
            int charLength = 0;
            int size = 0;
            for (Node<E> p = first(); p != null;) {
                final E item;
                if ((item = p.item) != null) {
                    if (a == null)
                        a = new String[4];
                    else if (size == a.length)
                        a = Arrays.copyOf(a, 2 * size);
                    String s = item.toString();
                    a[size++] = s;
                    charLength += s.length();
                }
                if (p == (p = p.next))
                    continue restart;
            }

            if (size == 0)
                return "[]";

            return Helpers.toString(a, size, charLength);
        }
    }

    private Object[] toArrayInternal(Object[] a) {
        Object[] x = a;
        restart: for (;;) {
            int size = 0;
            for (Node<E> p = first(); p != null;) {
                final E item;
                if ((item = p.item) != null) {
                    if (x == null)
                        x = new Object[4];
                    else if (size == x.length)
                        x = Arrays.copyOf(x, 2 * (size + 4));
                    x[size++] = item;
                }
                if (p == (p = p.next))
                    continue restart;
            }
            if (x == null)
                return new Object[0];
            else if (a != null && size <= a.length) {
                if (a != x)
                    System.arraycopy(x, 0, a, 0, size);
                if (size < a.length)
                    a[size] = null;
                return a;
            }
            return (size == x.length) ? x : Arrays.copyOf(x, size);
        }
    }

    /**
     * Returns an array containing all of the elements in this deque, in
     * proper sequence (from first to last element).
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this deque.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this deque
     */
    public Object[] toArray() {
        return toArrayInternal(null);
    }

    /**
     * Returns an array containing all of the elements in this deque,
     * in proper sequence (from first to last element); the runtime
     * type of the returned array is that of the specified array.  If
     * the deque fits in the specified array, it is returned therein.
     * Otherwise, a new array is allocated with the runtime type of
     * the specified array and the size of this deque.
     *
     * <p>If this deque fits in the specified array with room to spare
     * (i.e., the array has more elements than this deque), the element in
     * the array immediately following the end of the deque is set to
     * {@code null}.
     *
     * <p>Like the {@link #toArray()} method, this method acts as
     * bridge between array-based and collection-based APIs.  Further,
     * this method allows precise control over the runtime type of the
     * output array, and may, under certain circumstances, be used to
     * save allocation costs.
     *
     * <p>Suppose {@code x} is a deque known to contain only strings.
     * The following code can be used to dump the deque into a newly
     * allocated array of {@code String}:
     *
     * <pre> {@code String[] y = x.toArray(new String[0]);}</pre>
     *
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param a the array into which the elements of the deque are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose
     * @return an array containing all of the elements in this deque
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in
     *         this deque
     * @throws NullPointerException if the specified array is null
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a == null) throw new NullPointerException();
        return (T[]) toArrayInternal(a);
    }

    /**
     * Returns an iterator over the elements in this deque in proper sequence.
     * The elements will be returned in order from first (head) to last (tail).
     *
     * <p>The returned iterator is
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * @return an iterator over the elements in this deque in proper sequence
     */
    public Iterator<E> iterator() {
        return new Itr();
    }

    /**
     * Returns an iterator over the elements in this deque in reverse
     * sequential order.  The elements will be returned in order from
     * last (tail) to first (head).
     *
     * <p>The returned iterator is
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * @return an iterator over the elements in this deque in reverse order
     */
    public Iterator<E> descendingIterator() {
        return new DescendingItr();
    }

    private abstract class AbstractItr implements Iterator<E> {
        /**
         * Next node to return item for.
         */
        private Node<E> nextNode;

        /**
         * nextItem holds on to item fields because once we claim
         * that an element exists in hasNext(), we must return it in
         * the following next() call even if it was in the process of
         * being removed when hasNext() was called.
         */
        private E nextItem;

        /**
         * Node returned by most recent call to next. Needed by remove.
         * Reset to null if this element is deleted by a call to remove.
         */
        private Node<E> lastRet;

        abstract Node<E> startNode();
        abstract Node<E> nextNode(Node<E> p);

        AbstractItr() {
            advance();
        }

        /**
         * Sets nextNode and nextItem to next valid node, or to null
         * if no such.
         */
        private void advance() {
            lastRet = nextNode;

            Node<E> p = (nextNode == null) ? startNode() : nextNode(nextNode);
            for (;; p = nextNode(p)) {
                if (p == null) {
                    // might be at active end or TERMINATOR node; both are OK
                    nextNode = null;
                    nextItem = null;
                    break;
                }
                final E item;
                if ((item = p.item) != null) {
                    nextNode = p;
                    nextItem = item;
                    break;
                }
            }
        }

        public boolean hasNext() {
            return nextItem != null;
        }

        public E next() {
            E item = nextItem;
            if (item == null) throw new NoSuchElementException();
            advance();
            return item;
        }

        public void remove() {
            Node<E> l = lastRet;
            if (l == null) throw new IllegalStateException();
            l.item = null;
            unlink(l);
            lastRet = null;
        }
    }

    /** Forward iterator */
    private class Itr extends AbstractItr {
        Itr() {}                        // prevent access constructor creation
        Node<E> startNode() { return first(); }
        Node<E> nextNode(Node<E> p) { return succ(p); }
    }

    /** Descending iterator */
    private class DescendingItr extends AbstractItr {
        DescendingItr() {}              // prevent access constructor creation
        Node<E> startNode() { return last(); }
        Node<E> nextNode(Node<E> p) { return pred(p); }
    }

    /** A customized variant of Spliterators.IteratorSpliterator */
    final class CLDSpliterator implements Spliterator<E> {
        static final int MAX_BATCH = 1 << 25;  // max batch array size;
        Node<E> current;    // current node; null until initialized
        int batch;          // batch size for splits
        boolean exhausted;  // true when no more nodes

        public Spliterator<E> trySplit() {
            Node<E> p, q;
            if ((p = current()) == null || (q = p.next) == null)
                return null;
            int i = 0, n = batch = Math.min(batch + 1, MAX_BATCH);
            Object[] a = null;
            do {
                final E e;
                if ((e = p.item) != null) {
                    if (a == null)
                        a = new Object[n];
                    a[i++] = e;
                }
                if (p == (p = q))
                    p = first();
            } while (p != null && (q = p.next) != null && i < n);
            setCurrent(p);
            return (i == 0) ? null :
                Spliterators.spliterator(a, 0, i, (Spliterator.ORDERED |
                                                   Spliterator.NONNULL |
                                                   Spliterator.CONCURRENT));
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            Node<E> p;
            if ((p = current()) != null) {
                current = null;
                exhausted = true;
                do {
                    final E e;
                    if ((e = p.item) != null)
                        action.accept(e);
                    if (p == (p = p.next))
                        p = first();
                } while (p != null);
            }
        }

        public boolean tryAdvance(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            Node<E> p;
            if ((p = current()) != null) {
                E e;
                do {
                    e = p.item;
                    if (p == (p = p.next))
                        p = first();
                } while (e == null && p != null);
                setCurrent(p);
                if (e != null) {
                    action.accept(e);
                    return true;
                }
            }
            return false;
        }

        private void setCurrent(Node<E> p) {
            if ((current = p) == null)
                exhausted = true;
        }

        private Node<E> current() {
            Node<E> p;
            if ((p = current) == null && !exhausted)
                setCurrent(p = first());
            return p;
        }

        public long estimateSize() { return Long.MAX_VALUE; }

        public int characteristics() {
            return (Spliterator.ORDERED |
                    Spliterator.NONNULL |
                    Spliterator.CONCURRENT);
        }
    }

    /**
     * Returns a {@link Spliterator} over the elements in this deque.
     *
     * <p>The returned spliterator is
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#CONCURRENT},
     * {@link Spliterator#ORDERED}, and {@link Spliterator#NONNULL}.
     *
     * @implNote
     * The {@code Spliterator} implements {@code trySplit} to permit limited
     * parallelism.
     *
     * @return a {@code Spliterator} over the elements in this deque
     * @since 1.8
     */
    public Spliterator<E> spliterator() {
        return new CLDSpliterator();
    }

    /**
     * Saves this deque to a stream (that is, serializes it).
     *
     * @param s the stream
     * @throws java.io.IOException if an I/O error occurs
     * @serialData All of the elements (each an {@code E}) in
     * the proper order, followed by a null
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {

        // Write out any hidden stuff
        s.defaultWriteObject();

        // Write out all elements in the proper order.
        for (Node<E> p = first(); p != null; p = succ(p)) {
            final E item;
            if ((item = p.item) != null)
                s.writeObject(item);
        }

        // Use trailing null as sentinel
        s.writeObject(null);
    }

    /**
     * Reconstitutes this deque from a stream (that is, deserializes it).
     * @param s the stream
     * @throws ClassNotFoundException if the class of a serialized object
     *         could not be found
     * @throws java.io.IOException if an I/O error occurs
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();

        // Read in elements until trailing null sentinel found
        Node<E> h = null, t = null;
        for (Object item; (item = s.readObject()) != null; ) {
            @SuppressWarnings("unchecked")
            Node<E> newNode = newNode((E) item);
            if (h == null)
                h = t = newNode;
            else {
                NEXT.set(t, newNode);
                PREV.set(newNode, t);
                t = newNode;
            }
        }
        initHeadTail(h, t);
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        return bulkRemove(filter);
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return bulkRemove(e -> c.contains(e));
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return bulkRemove(e -> !c.contains(e));
    }

    /** Implementation of bulk remove methods. */
    private boolean bulkRemove(Predicate<? super E> filter) {
        boolean removed = false;
        for (Node<E> p = first(), succ; p != null; p = succ) {
            succ = succ(p);
            final E item;
            if ((item = p.item) != null
                && filter.test(item)
                && ITEM.compareAndSet(p, item, null)) {
                unlink(p);
                removed = true;
            }
        }
        return removed;
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     */
    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        E item;
        for (Node<E> p = first(); p != null; p = succ(p))
            if ((item = p.item) != null)
                action.accept(item);
    }

    // VarHandle mechanics
    private static final VarHandle HEAD;
    private static final VarHandle TAIL;
    private static final VarHandle PREV;
    private static final VarHandle NEXT;
    private static final VarHandle ITEM;
    static {
        PREV_TERMINATOR = new Node<Object>();
        PREV_TERMINATOR.next = PREV_TERMINATOR;
        NEXT_TERMINATOR = new Node<Object>();
        NEXT_TERMINATOR.prev = NEXT_TERMINATOR;
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            HEAD = l.findVarHandle(ConcurrentLinkedDeque.class, "head",
                                   Node.class);
            TAIL = l.findVarHandle(ConcurrentLinkedDeque.class, "tail",
                                   Node.class);
            PREV = l.findVarHandle(Node.class, "prev", Node.class);
            NEXT = l.findVarHandle(Node.class, "next", Node.class);
            ITEM = l.findVarHandle(Node.class, "item", Object.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
