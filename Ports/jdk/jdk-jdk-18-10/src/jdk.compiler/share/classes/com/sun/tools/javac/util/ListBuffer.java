/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.util;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/** A class for constructing lists by appending elements. Modelled after
 *  java.lang.StringBuffer.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ListBuffer<A> extends AbstractQueue<A> {

    public static <T> ListBuffer<T> of(T x) {
        ListBuffer<T> lb = new ListBuffer<>();
        lb.add(x);
        return lb;
    }

    /** The list of elements of this buffer.
     */
    private List<A> elems;

    /** A pointer pointing to the last element of 'elems' containing data,
     *  or null if the list is empty.
     */
    private List<A> last;

    /** The number of element in this buffer.
     */
    private int count;

    /** Has a list been created from this buffer yet?
     */
    private boolean shared;

    /** Create a new initially empty list buffer.
     */
    public ListBuffer() {
        clear();
    }

    public final void clear() {
        this.elems = List.nil();
        this.last = null;
        count = 0;
        shared = false;
    }

    /** Return the number of elements in this buffer.
     */
    public int length() {
        return count;
    }
    public int size() {
        return count;
    }

    /** Is buffer empty?
     */
    public boolean isEmpty() {
        return count == 0;
    }

    /** Is buffer not empty?
     */
    public boolean nonEmpty() {
        return count != 0;
    }

    /** Copy list and sets last.
     */
    private void copy() {
        if (elems.nonEmpty()) {
            List<A> orig = elems;

            elems = last = List.of(orig.head);

            while ((orig = orig.tail).nonEmpty()) {
                last.tail = List.of(orig.head);
                last = last.tail;
            }
        }
    }

    /** Prepend an element to buffer.
     */
    public ListBuffer<A> prepend(A x) {
        elems = elems.prepend(x);
        if (last == null) last = elems;
        count++;
        return this;
    }

    /** Append an element to buffer.
     */
    public ListBuffer<A> append(A x) {
        Assert.checkNonNull(x);
        if (shared) copy();
        List<A> newLast = List.of(x);
        if (last != null) {
            last.tail = newLast;
            last = newLast;
        } else {
            elems = last = newLast;
        }
        count++;
        return this;
    }

    /** Append all elements in a list to buffer.
     */
    public ListBuffer<A> appendList(List<A> xs) {
        while (xs.nonEmpty()) {
            append(xs.head);
            xs = xs.tail;
        }
        return this;
    }

    /** Append all elements in a list to buffer.
     */
    public ListBuffer<A> appendList(ListBuffer<A> xs) {
        return appendList(xs.toList());
    }

    /** Append all elements in an array to buffer.
     */
    public ListBuffer<A> appendArray(A[] xs) {
        for (A x : xs) {
            append(x);
        }
        return this;
    }

    /** Convert buffer to a list of all its elements.
     */
    public List<A> toList() {
        shared = true;
        return elems;
    }

    /** Does the list contain the specified element?
     */
    public boolean contains(Object x) {
        return elems.contains(x);
    }

    /** Convert buffer to an array
     */
    public <T> T[] toArray(T[] vec) {
        return elems.toArray(vec);
    }
    public Object[] toArray() {
        return toArray(new Object[size()]);
    }

    /** The first element in this buffer.
     */
    public A first() {
        return elems.head;
    }

    /** Return first element in this buffer and remove
     */
    public A next() {
        A x = elems.head;
        if (!elems.isEmpty()) {
            elems = elems.tail;
            if (elems.isEmpty()) last = null;
            count--;
        }
        return x;
    }

    /** An enumeration of all elements in this buffer.
     */
    public Iterator<A> iterator() {
        return new Iterator<A>() {
            List<A> elems = ListBuffer.this.elems;
            public boolean hasNext() {
                return !elems.isEmpty();
            }
            public A next() {
                if (elems.isEmpty())
                    throw new NoSuchElementException();
                A elem = elems.head;
                elems = elems.tail;
                return elem;
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public boolean add(A a) {
        append(a);
        return true;
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean containsAll(Collection<?> c) {
        for (Object x: c) {
            if (!contains(x))
                return false;
        }
        return true;
    }

    public boolean addAll(Collection<? extends A> c) {
        for (A a: c)
            append(a);
        return true;
    }

    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean offer(A a) {
        append(a);
        return true;
    }

    public A poll() {
        return next();
    }

    public A peek() {
        return first();
    }

    public A last() {
        return last != null ? last.head : null;
    }
}
