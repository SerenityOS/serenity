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

import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A {@link Deque} that additionally supports blocking operations that wait
 * for the deque to become non-empty when retrieving an element, and wait for
 * space to become available in the deque when storing an element.
 *
 * <p>{@code BlockingDeque} methods come in four forms, with different ways
 * of handling operations that cannot be satisfied immediately, but may be
 * satisfied at some point in the future:
 * one throws an exception, the second returns a special value (either
 * {@code null} or {@code false}, depending on the operation), the third
 * blocks the current thread indefinitely until the operation can succeed,
 * and the fourth blocks for only a given maximum time limit before giving
 * up.  These methods are summarized in the following table:
 *
 * <table class="plain">
 * <caption>Summary of BlockingDeque methods</caption>
 *  <tr>
 *    <th id="First" colspan="5"> First Element (Head)</th>
 *  </tr>
 *  <tr>
 *    <td></td>
 *    <th id="FThrow" style="font-weight:normal; font-style: italic">Throws exception</th>
 *    <th id="FValue" style="font-weight:normal; font-style: italic">Special value</th>
 *    <th id="FBlock" style="font-weight:normal; font-style: italic">Blocks</th>
 *    <th id="FTimes" style="font-weight:normal; font-style: italic">Times out</th>
 *  </tr>
 *  <tr>
 *    <th id="FInsert" style="text-align:left">Insert</th>
 *    <td headers="First FInsert FThrow">{@link #addFirst(Object) addFirst(e)}</td>
 *    <td headers="First FInsert FValue">{@link #offerFirst(Object) offerFirst(e)}</td>
 *    <td headers="First FInsert FBlock">{@link #putFirst(Object) putFirst(e)}</td>
 *    <td headers="First FInsert FTimes">{@link #offerFirst(Object, long, TimeUnit) offerFirst(e, time, unit)}</td>
 *  </tr>
 *  <tr>
 *    <th id="FRemove" style="text-align:left">Remove</th>
 *    <td headers="First FRemove FThrow">{@link #removeFirst() removeFirst()}</td>
 *    <td headers="First FRemove FValue">{@link #pollFirst() pollFirst()}</td>
 *    <td headers="First FRemove FBlock">{@link #takeFirst() takeFirst()}</td>
 *    <td headers="First FRemove FTimes">{@link #pollFirst(long, TimeUnit) pollFirst(time, unit)}</td>
 *  </tr>
 *  <tr>
 *    <th id="FExamine" style="text-align:left">Examine</th>
 *    <td headers="First FExamine FThrow">{@link #getFirst() getFirst()}</td>
 *    <td headers="First FExamine FValue">{@link #peekFirst() peekFirst()}</td>
 *    <td headers="First FExamine FBlock" style="font-style:italic">not applicable</td>
 *    <td headers="First FExamine FTimes" style="font-style:italic">not applicable</td>
 *  </tr>
 *  <tr>
 *    <th id="Last" colspan="5"> Last Element (Tail)</th>
 *  </tr>
 *  <tr>
 *    <td></td>
 *    <th id="LThrow" style="font-weight:normal; font-style: italic">Throws exception</th>
 *    <th id="LValue" style="font-weight:normal; font-style: italic">Special value</th>
 *    <th id="LBlock" style="font-weight:normal; font-style: italic">Blocks</th>
 *    <th id="LTimes" style="font-weight:normal; font-style: italic">Times out</th>
 *  </tr>
 *  <tr>
 *    <th id="LInsert" style="text-align:left">Insert</th>
 *    <td headers="Last LInsert LThrow">{@link #addLast(Object) addLast(e)}</td>
 *    <td headers="Last LInsert LValue">{@link #offerLast(Object) offerLast(e)}</td>
 *    <td headers="Last LInsert LBlock">{@link #putLast(Object) putLast(e)}</td>
 *    <td headers="Last LInsert LTimes">{@link #offerLast(Object, long, TimeUnit) offerLast(e, time, unit)}</td>
 *  </tr>
 *  <tr>
 *    <th id="LRemove" style="text-align:left">Remove</th>
 *    <td headers="Last LRemove LThrow">{@link #removeLast() removeLast()}</td>
 *    <td headers="Last LRemove LValue">{@link #pollLast() pollLast()}</td>
 *    <td headers="Last LRemove LBlock">{@link #takeLast() takeLast()}</td>
 *    <td headers="Last LRemove LTimes">{@link #pollLast(long, TimeUnit) pollLast(time, unit)}</td>
 *  </tr>
 *  <tr>
 *    <th id="LExamine" style="text-align:left">Examine</th>
 *    <td headers="Last LExamine LThrow">{@link #getLast() getLast()}</td>
 *    <td headers="Last LExamine LValue">{@link #peekLast() peekLast()}</td>
 *    <td headers="Last LExamine LBlock" style="font-style:italic">not applicable</td>
 *    <td headers="Last LExamine LTimes" style="font-style:italic">not applicable</td>
 *  </tr>
 * </table>
 *
 * <p>Like any {@link BlockingQueue}, a {@code BlockingDeque} is thread safe,
 * does not permit null elements, and may (or may not) be
 * capacity-constrained.
 *
 * <p>A {@code BlockingDeque} implementation may be used directly as a FIFO
 * {@code BlockingQueue}. The methods inherited from the
 * {@code BlockingQueue} interface are precisely equivalent to
 * {@code BlockingDeque} methods as indicated in the following table:
 *
 * <table class="plain">
 * <caption>Comparison of BlockingQueue and BlockingDeque methods</caption>
 *  <tr>
 *    <td></td>
 *    <th id="BQueue"> {@code BlockingQueue} Method</th>
 *    <th id="BDeque"> Equivalent {@code BlockingDeque} Method</th>
 *  </tr>
 *  <tr>
 *    <th id="Insert" rowspan="4" style="text-align:left; vertical-align:top">Insert</th>
 *    <th id="add" style="font-weight:normal; text-align:left">{@link #add(Object) add(e)}</th>
 *    <td headers="Insert BDeque add">{@link #addLast(Object) addLast(e)}</td>
 *  </tr>
 *  <tr>
 *    <th id="offer1" style="font-weight:normal; text-align:left">{@link #offer(Object) offer(e)}</th>
 *    <td headers="Insert BDeque offer1">{@link #offerLast(Object) offerLast(e)}</td>
 *  </tr>
 *  <tr>
 *    <th id="put" style="font-weight:normal; text-align:left">{@link #put(Object) put(e)}</th>
 *    <td headers="Insert BDeque put">{@link #putLast(Object) putLast(e)}</td>
 *  </tr>
 *  <tr>
 *    <th id="offer2" style="font-weight:normal; text-align:left">{@link #offer(Object, long, TimeUnit) offer(e, time, unit)}</th>
 *    <td headers="Insert BDeque offer2">{@link #offerLast(Object, long, TimeUnit) offerLast(e, time, unit)}</td>
 *  </tr>
 *  <tr>
 *    <th id="Remove" rowspan="4" style="text-align:left; vertical-align:top">Remove</th>
 *    <th id="remove" style="font-weight:normal; text-align:left">{@link #remove() remove()}</th>
 *    <td headers="Remove BDeque remove">{@link #removeFirst() removeFirst()}</td>
 *  </tr>
 *  <tr>
 *    <th id="poll1" style="font-weight:normal; text-align:left">{@link #poll() poll()}</th>
 *    <td headers="Remove BDeque poll1">{@link #pollFirst() pollFirst()}</td>
 *  </tr>
 *  <tr>
 *    <th id="take" style="font-weight:normal; text-align:left">{@link #take() take()}</th>
 *    <td headers="Remove BDeque take">{@link #takeFirst() takeFirst()}</td>
 *  </tr>
 *  <tr>
 *    <th id="poll2" style="font-weight:normal; text-align:left">{@link #poll(long, TimeUnit) poll(time, unit)}</th>
 *    <td headers="Remove BDeque poll2">{@link #pollFirst(long, TimeUnit) pollFirst(time, unit)}</td>
 *  </tr>
 *  <tr>
 *    <th id="Examine" rowspan="2" style="text-align:left; vertical-align:top">Examine</th>
 *    <th id="element" style="font-weight:normal; text-align:left">{@link #element() element()}</th>
 *    <td headers="Examine BDeque element">{@link #getFirst() getFirst()}</td>
 *  </tr>
 *  <tr>
 *    <th id="peek" style="font-weight:normal; text-align:left">{@link #peek() peek()}</th>
 *    <td headers="Examine BDeque peek">{@link #peekFirst() peekFirst()}</td>
 *  </tr>
 * </table>
 *
 * <p>Memory consistency effects: As with other concurrent
 * collections, actions in a thread prior to placing an object into a
 * {@code BlockingDeque}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions subsequent to the access or removal of that element from
 * the {@code BlockingDeque} in another thread.
 *
 * <p>This interface is a member of the
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 * Java Collections Framework</a>.
 *
 * @since 1.6
 * @author Doug Lea
 * @param <E> the type of elements held in this deque
 */
public interface BlockingDeque<E> extends BlockingQueue<E>, Deque<E> {
    /*
     * We have "diamond" multiple interface inheritance here, and that
     * introduces ambiguities.  Methods might end up with different
     * specs depending on the branch chosen by javadoc.  Thus a lot of
     * methods specs here are copied from superinterfaces.
     */

    /**
     * Inserts the specified element at the front of this deque if it is
     * possible to do so immediately without violating capacity restrictions,
     * throwing an {@code IllegalStateException} if no space is currently
     * available.  When using a capacity-restricted deque, it is generally
     * preferable to use {@link #offerFirst(Object) offerFirst}.
     *
     * @param e the element to add
     * @throws IllegalStateException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException {@inheritDoc}
     */
    void addFirst(E e);

    /**
     * Inserts the specified element at the end of this deque if it is
     * possible to do so immediately without violating capacity restrictions,
     * throwing an {@code IllegalStateException} if no space is currently
     * available.  When using a capacity-restricted deque, it is generally
     * preferable to use {@link #offerLast(Object) offerLast}.
     *
     * @param e the element to add
     * @throws IllegalStateException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException {@inheritDoc}
     */
    void addLast(E e);

    /**
     * Inserts the specified element at the front of this deque if it is
     * possible to do so immediately without violating capacity restrictions,
     * returning {@code true} upon success and {@code false} if no space is
     * currently available.
     * When using a capacity-restricted deque, this method is generally
     * preferable to the {@link #addFirst(Object) addFirst} method, which can
     * fail to insert an element only by throwing an exception.
     *
     * @param e the element to add
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException {@inheritDoc}
     */
    boolean offerFirst(E e);

    /**
     * Inserts the specified element at the end of this deque if it is
     * possible to do so immediately without violating capacity restrictions,
     * returning {@code true} upon success and {@code false} if no space is
     * currently available.
     * When using a capacity-restricted deque, this method is generally
     * preferable to the {@link #addLast(Object) addLast} method, which can
     * fail to insert an element only by throwing an exception.
     *
     * @param e the element to add
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException {@inheritDoc}
     */
    boolean offerLast(E e);

    /**
     * Inserts the specified element at the front of this deque,
     * waiting if necessary for space to become available.
     *
     * @param e the element to add
     * @throws InterruptedException if interrupted while waiting
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    void putFirst(E e) throws InterruptedException;

    /**
     * Inserts the specified element at the end of this deque,
     * waiting if necessary for space to become available.
     *
     * @param e the element to add
     * @throws InterruptedException if interrupted while waiting
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    void putLast(E e) throws InterruptedException;

    /**
     * Inserts the specified element at the front of this deque,
     * waiting up to the specified wait time if necessary for space to
     * become available.
     *
     * @param e the element to add
     * @param timeout how long to wait before giving up, in units of
     *        {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code timeout} parameter
     * @return {@code true} if successful, or {@code false} if
     *         the specified waiting time elapses before space is available
     * @throws InterruptedException if interrupted while waiting
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    boolean offerFirst(E e, long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * Inserts the specified element at the end of this deque,
     * waiting up to the specified wait time if necessary for space to
     * become available.
     *
     * @param e the element to add
     * @param timeout how long to wait before giving up, in units of
     *        {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code timeout} parameter
     * @return {@code true} if successful, or {@code false} if
     *         the specified waiting time elapses before space is available
     * @throws InterruptedException if interrupted while waiting
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    boolean offerLast(E e, long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * Retrieves and removes the first element of this deque, waiting
     * if necessary until an element becomes available.
     *
     * @return the head of this deque
     * @throws InterruptedException if interrupted while waiting
     */
    E takeFirst() throws InterruptedException;

    /**
     * Retrieves and removes the last element of this deque, waiting
     * if necessary until an element becomes available.
     *
     * @return the tail of this deque
     * @throws InterruptedException if interrupted while waiting
     */
    E takeLast() throws InterruptedException;

    /**
     * Retrieves and removes the first element of this deque, waiting
     * up to the specified wait time if necessary for an element to
     * become available.
     *
     * @param timeout how long to wait before giving up, in units of
     *        {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code timeout} parameter
     * @return the head of this deque, or {@code null} if the specified
     *         waiting time elapses before an element is available
     * @throws InterruptedException if interrupted while waiting
     */
    E pollFirst(long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * Retrieves and removes the last element of this deque, waiting
     * up to the specified wait time if necessary for an element to
     * become available.
     *
     * @param timeout how long to wait before giving up, in units of
     *        {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code timeout} parameter
     * @return the tail of this deque, or {@code null} if the specified
     *         waiting time elapses before an element is available
     * @throws InterruptedException if interrupted while waiting
     */
    E pollLast(long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * Removes the first occurrence of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the first element {@code e} such that
     * {@code o.equals(e)} (if such an element exists).
     * Returns {@code true} if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if an element was removed as a result of this call
     * @throws ClassCastException if the class of the specified element
     *         is incompatible with this deque
     * (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null
     * (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     */
    boolean removeFirstOccurrence(Object o);

    /**
     * Removes the last occurrence of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the last element {@code e} such that
     * {@code o.equals(e)} (if such an element exists).
     * Returns {@code true} if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if an element was removed as a result of this call
     * @throws ClassCastException if the class of the specified element
     *         is incompatible with this deque
     * (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null
     * (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     */
    boolean removeLastOccurrence(Object o);

    // *** BlockingQueue methods ***

    /**
     * Inserts the specified element into the queue represented by this deque
     * (in other words, at the tail of this deque) if it is possible to do so
     * immediately without violating capacity restrictions, returning
     * {@code true} upon success and throwing an
     * {@code IllegalStateException} if no space is currently available.
     * When using a capacity-restricted deque, it is generally preferable to
     * use {@link #offer(Object) offer}.
     *
     * <p>This method is equivalent to {@link #addLast(Object) addLast}.
     *
     * @param e the element to add
     * @throws IllegalStateException {@inheritDoc}
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    boolean add(E e);

    /**
     * Inserts the specified element into the queue represented by this deque
     * (in other words, at the tail of this deque) if it is possible to do so
     * immediately without violating capacity restrictions, returning
     * {@code true} upon success and {@code false} if no space is currently
     * available.  When using a capacity-restricted deque, this method is
     * generally preferable to the {@link #add} method, which can fail to
     * insert an element only by throwing an exception.
     *
     * <p>This method is equivalent to {@link #offerLast(Object) offerLast}.
     *
     * @param e the element to add
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    boolean offer(E e);

    /**
     * Inserts the specified element into the queue represented by this deque
     * (in other words, at the tail of this deque), waiting if necessary for
     * space to become available.
     *
     * <p>This method is equivalent to {@link #putLast(Object) putLast}.
     *
     * @param e the element to add
     * @throws InterruptedException {@inheritDoc}
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    void put(E e) throws InterruptedException;

    /**
     * Inserts the specified element into the queue represented by this deque
     * (in other words, at the tail of this deque), waiting up to the
     * specified wait time if necessary for space to become available.
     *
     * <p>This method is equivalent to
     * {@link #offerLast(Object,long,TimeUnit) offerLast}.
     *
     * @param e the element to add
     * @return {@code true} if the element was added to this deque, else
     *         {@code false}
     * @throws InterruptedException {@inheritDoc}
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    boolean offer(E e, long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * Retrieves and removes the head of the queue represented by this deque
     * (in other words, the first element of this deque).
     * This method differs from {@link #poll() poll()} only in that it
     * throws an exception if this deque is empty.
     *
     * <p>This method is equivalent to {@link #removeFirst() removeFirst}.
     *
     * @return the head of the queue represented by this deque
     * @throws NoSuchElementException if this deque is empty
     */
    E remove();

    /**
     * Retrieves and removes the head of the queue represented by this deque
     * (in other words, the first element of this deque), or returns
     * {@code null} if this deque is empty.
     *
     * <p>This method is equivalent to {@link #pollFirst()}.
     *
     * @return the head of this deque, or {@code null} if this deque is empty
     */
    E poll();

    /**
     * Retrieves and removes the head of the queue represented by this deque
     * (in other words, the first element of this deque), waiting if
     * necessary until an element becomes available.
     *
     * <p>This method is equivalent to {@link #takeFirst() takeFirst}.
     *
     * @return the head of this deque
     * @throws InterruptedException if interrupted while waiting
     */
    E take() throws InterruptedException;

    /**
     * Retrieves and removes the head of the queue represented by this deque
     * (in other words, the first element of this deque), waiting up to the
     * specified wait time if necessary for an element to become available.
     *
     * <p>This method is equivalent to
     * {@link #pollFirst(long,TimeUnit) pollFirst}.
     *
     * @return the head of this deque, or {@code null} if the
     *         specified waiting time elapses before an element is available
     * @throws InterruptedException if interrupted while waiting
     */
    E poll(long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * Retrieves, but does not remove, the head of the queue represented by
     * this deque (in other words, the first element of this deque).
     * This method differs from {@link #peek() peek} only in that it throws an
     * exception if this deque is empty.
     *
     * <p>This method is equivalent to {@link #getFirst() getFirst}.
     *
     * @return the head of this deque
     * @throws NoSuchElementException if this deque is empty
     */
    E element();

    /**
     * Retrieves, but does not remove, the head of the queue represented by
     * this deque (in other words, the first element of this deque), or
     * returns {@code null} if this deque is empty.
     *
     * <p>This method is equivalent to {@link #peekFirst() peekFirst}.
     *
     * @return the head of this deque, or {@code null} if this deque is empty
     */
    E peek();

    /**
     * Removes the first occurrence of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the first element {@code e} such that
     * {@code o.equals(e)} (if such an element exists).
     * Returns {@code true} if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     * <p>This method is equivalent to
     * {@link #removeFirstOccurrence(Object) removeFirstOccurrence}.
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if this deque changed as a result of the call
     * @throws ClassCastException if the class of the specified element
     *         is incompatible with this deque
     * (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null
     * (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     */
    boolean remove(Object o);

    /**
     * Returns {@code true} if this deque contains the specified element.
     * More formally, returns {@code true} if and only if this deque contains
     * at least one element {@code e} such that {@code o.equals(e)}.
     *
     * @param o object to be checked for containment in this deque
     * @return {@code true} if this deque contains the specified element
     * @throws ClassCastException if the class of the specified element
     *         is incompatible with this deque
     * (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null
     * (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     */
    boolean contains(Object o);

    /**
     * Returns the number of elements in this deque.
     *
     * @return the number of elements in this deque
     */
    int size();

    /**
     * Returns an iterator over the elements in this deque in proper sequence.
     * The elements will be returned in order from first (head) to last (tail).
     *
     * @return an iterator over the elements in this deque in proper sequence
     */
    Iterator<E> iterator();

    // *** Stack methods ***

    /**
     * Pushes an element onto the stack represented by this deque (in other
     * words, at the head of this deque) if it is possible to do so
     * immediately without violating capacity restrictions, throwing an
     * {@code IllegalStateException} if no space is currently available.
     *
     * <p>This method is equivalent to {@link #addFirst(Object) addFirst}.
     *
     * @throws IllegalStateException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException {@inheritDoc}
     */
    void push(E e);
}
