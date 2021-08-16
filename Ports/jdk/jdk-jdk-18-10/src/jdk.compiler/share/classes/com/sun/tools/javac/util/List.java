/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.AbstractCollection;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collector;

/** A class for generic linked lists. Links are supposed to be
 *  immutable, the only exception being the incremental construction of
 *  lists via ListBuffers.  List is the main container class in
 *  GJC. Most data structures and algorithms in GJC use lists rather
 *  than arrays.
 *
 *  <p>Lists are always trailed by a sentinel element, whose head and tail
 *  are both null.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class List<A> extends AbstractCollection<A> implements java.util.List<A> {

    /** The first element of the list, supposed to be immutable.
     */
    public A head;

    /** The remainder of the list except for its first element, supposed
     *  to be immutable.
     */
    //@Deprecated
    public List<A> tail;

    /** Construct a list given its head and tail.
     */
    List(A head, List<A> tail) {
        this.tail = tail;
        this.head = head;
    }

    /** Construct an empty list.
     */
    @SuppressWarnings("unchecked")
    public static <A> List<A> nil() {
        return (List<A>)EMPTY_LIST;
    }

    private static final List<?> EMPTY_LIST = new List<Object>(null,null) {
        public List<Object> setTail(List<Object> tail) {
            throw new UnsupportedOperationException();
        }
        public boolean isEmpty() {
            return true;
        }
    };

    /** Returns the list obtained from 'l' after removing all elements 'elem'
     */
    public static <A> List<A> filter(List<A> l, A elem) {
        Assert.checkNonNull(elem);
        List<A> res = List.nil();
        for (A a : l) {
            if (a != null && !a.equals(elem)) {
                res = res.prepend(a);
            }
        }
        return res.reverse();
    }

    public List<A> intersect(List<A> that) {
        ListBuffer<A> buf = new ListBuffer<>();
        for (A el : this) {
            if (that.contains(el)) {
                buf.append(el);
            }
        }
        return buf.toList();
    }

    public List<A> diff(List<A> that) {
        ListBuffer<A> buf = new ListBuffer<>();
        for (A el : this) {
            if (!that.contains(el)) {
                buf.append(el);
            }
        }
        return buf.toList();
    }

    /**
     * Create a new list from the first {@code n} elements of this list
     */
    public List<A> take(int n) {
        ListBuffer<A> buf = new ListBuffer<>();
        int count = 0;
        for (A el : this) {
            if (count++ == n) break;
            buf.append(el);
        }
        return buf.toList();
    }

    /** Construct a list consisting of given element.
     */
    public static <A> List<A> of(A x1) {
        return new List<>(x1, List.nil());
    }

    /** Construct a list consisting of given elements.
     */
    public static <A> List<A> of(A x1, A x2) {
        return new List<>(x1, of(x2));
    }

    /** Construct a list consisting of given elements.
     */
    public static <A> List<A> of(A x1, A x2, A x3) {
        return new List<>(x1, of(x2, x3));
    }

    /** Construct a list consisting of given elements.
     */
    @SuppressWarnings({"varargs", "unchecked"})
    public static <A> List<A> of(A x1, A x2, A x3, A... rest) {
        return new List<>(x1, new List<>(x2, new List<>(x3, from(rest))));
    }

    /**
     * Construct a list consisting all elements of given array.
     * @param array an array; if {@code null} return an empty list
     */
    public static <A> List<A> from(A[] array) {
        List<A> xs = nil();
        if (array != null)
            for (int i = array.length - 1; i >= 0; i--)
                xs = new List<>(array[i], xs);
        return xs;
    }

    public static <A> List<A> from(Iterable<? extends A> coll) {
        ListBuffer<A> xs = new ListBuffer<>();
        for (A a : coll) {
            xs.append(a);
        }
        return xs.toList();
    }

    /** Construct a list consisting of a given number of identical elements.
     *  @param len    The number of elements in the list.
     *  @param init   The value of each element.
     */
    @Deprecated
    public static <A> List<A> fill(int len, A init) {
        List<A> l = nil();
        for (int i = 0; i < len; i++) l = new List<>(init, l);
        return l;
    }

    /** Does list have no elements?
     */
    @Override
    public boolean isEmpty() {
        return tail == null;
    }

    /** Does list have elements?
     */
    //@Deprecated
    public boolean nonEmpty() {
        return tail != null;
    }

    /** Return the number of elements in this list.
     */
    //@Deprecated
    public int length() {
        List<A> l = this;
        int len = 0;
        while (l.tail != null) {
            l = l.tail;
            len++;
        }
        return len;
    }
    @Override
    public int size() {
        return length();
    }

    public List<A> setTail(List<A> tail) {
        this.tail = tail;
        return tail;
    }

    /** Prepend given element to front of list, forming and returning
     *  a new list.
     */
    public List<A> prepend(A x) {
        return new List<>(x, this);
    }

    /** Prepend given list of elements to front of list, forming and returning
     *  a new list.
     */
    public List<A> prependList(List<A> xs) {
        if (this.isEmpty()) return xs;
        if (xs.isEmpty()) return this;
        if (xs.tail.isEmpty()) return prepend(xs.head);
        // return this.prependList(xs.tail).prepend(xs.head);
        List<A> result = this;
        List<A> rev = xs.reverse();
        Assert.check(rev != xs);
        // since xs.reverse() returned a new list, we can reuse the
        // individual List objects, instead of allocating new ones.
        while (rev.nonEmpty()) {
            List<A> h = rev;
            rev = rev.tail;
            h.setTail(result);
            result = h;
        }
        return result;
    }

    /** Reverse list.
     * If the list is empty or a singleton, then the same list is returned.
     * Otherwise a new list is formed.
     */
    public List<A> reverse() {
        // if it is empty or a singleton, return itself
        if (isEmpty() || tail.isEmpty())
            return this;

        List<A> rev = nil();
        for (List<A> l = this; l.nonEmpty(); l = l.tail)
            rev = new List<>(l.head, rev);
        return rev;
    }

    /** Append given element at length, forming and returning
     *  a new list.
     */
    public List<A> append(A x) {
        return of(x).prependList(this);
    }

    /** Append given list at length, forming and returning
     *  a new list.
     */
    public List<A> appendList(List<A> x) {
        return x.prependList(this);
    }

    /**
     * Append given list buffer at length, forming and returning a new
     * list.
     */
    public List<A> appendList(ListBuffer<A> x) {
        return appendList(x.toList());
    }

    /** Copy successive elements of this list into given vector until
     *  list is exhausted or end of vector is reached.
     */
    @Override @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] vec) {
        int i = 0;
        List<A> l = this;
        Object[] dest = vec;
        while (l.nonEmpty() && i < vec.length) {
            dest[i] = l.head;
            l = l.tail;
            i++;
        }
        if (l.isEmpty()) {
            if (i < vec.length)
                vec[i] = null;
            return vec;
        }

        vec = (T[])Array.newInstance(vec.getClass().getComponentType(), size());
        return toArray(vec);
    }

    public Object[] toArray() {
        return toArray(new Object[size()]);
    }

    /** Form a string listing all elements with given separator character.
     */
    public String toString(String sep) {
        if (isEmpty()) {
            return "";
        } else {
            StringBuilder buf = new StringBuilder();
            buf.append(head);
            for (List<A> l = tail; l.nonEmpty(); l = l.tail) {
                buf.append(sep);
                buf.append(l.head);
            }
            return buf.toString();
        }
    }

    /** Form a string listing all elements with comma as the separator character.
     */
    @Override
    public String toString() {
        return toString(",");
    }

    /** Compute a hash code, overrides Object
     *  @see java.util.List#hashCode
     */
    @Override
    public int hashCode() {
        List<A> l = this;
        int h = 1;
        while (l.tail != null) {
            h = h * 31 + (l.head == null ? 0 : l.head.hashCode());
            l = l.tail;
        }
        return h;
    }

    /** Is this list the same as other list?
     *  @see java.util.List#equals
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof List<?> javacList)
            return equals(this, javacList);
        if (other instanceof java.util.List<?> javaUtilList) {
            List<A> t = this;
            Iterator<?> oIter = javaUtilList.iterator();
            while (t.tail != null && oIter.hasNext()) {
                Object o = oIter.next();
                if ( !(t.head == null ? o == null : t.head.equals(o)))
                    return false;
                t = t.tail;
            }
            return (t.isEmpty() && !oIter.hasNext());
        }
        return false;
    }

    /** Are the two lists the same?
     */
    public static boolean equals(List<?> xs, List<?> ys) {
        while (xs.tail != null && ys.tail != null) {
            if (xs.head == null) {
                if (ys.head != null) return false;
            } else {
                if (!xs.head.equals(ys.head)) return false;
            }
            xs = xs.tail;
            ys = ys.tail;
        }
        return xs.tail == null && ys.tail == null;
    }

    /** Does the list contain the specified element?
     */
    @Override
    public boolean contains(Object x) {
        List<A> l = this;
        while (l.tail != null) {
            if (x == null) {
                if (l.head == null) return true;
            } else {
                if (l.head.equals(x)) return true;
            }
            l = l.tail;
        }
        return false;
    }

    /** The last element in the list, if any, or null.
     */
    public A last() {
        A last = null;
        List<A> t = this;
        while (t.tail != null) {
            last = t.head;
            t = t.tail;
        }
        return last;
    }

    @SuppressWarnings("unchecked")
    public <Z> List<Z> map(Function<A, Z> mapper) {
        boolean changed = false;
        ListBuffer<Z> buf = new ListBuffer<>();
        for (A a : this) {
            Z z = mapper.apply(a);
            buf.append(z);
            changed |= (z != a);
        }
        return changed ? buf.toList() : (List<Z>)this;
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> convert(Class<T> klass, List<?> list) {
        if (list == null)
            return null;
        for (Object o : list)
            klass.cast(o);
        return (List<T>)list;
    }

    private static final Iterator<?> EMPTYITERATOR = new Iterator<Object>() {
            public boolean hasNext() {
                return false;
            }
            public Object next() {
                throw new java.util.NoSuchElementException();
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };

    @SuppressWarnings("unchecked")
    private static <A> Iterator<A> emptyIterator() {
        return (Iterator<A>)EMPTYITERATOR;
    }

    @Override
    public Iterator<A> iterator() {
        if (tail == null)
            return emptyIterator();
        return new Iterator<A>() {
            List<A> elems = List.this;
            public boolean hasNext() {
                return elems.tail != null;
            }
            public A next() {
                if (elems.tail == null)
                    throw new NoSuchElementException();
                A result = elems.head;
                elems = elems.tail;
                return result;
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public A get(int index) {
        if (index < 0)
            throw new IndexOutOfBoundsException(String.valueOf(index));

        List<A> l = this;
        for (int i = index; i-- > 0 && !l.isEmpty(); l = l.tail)
            ;

        if (l.isEmpty())
            throw new IndexOutOfBoundsException("Index: " + index + ", " +
                                                "Size: " + size());
        return l.head;
    }

    public boolean addAll(int index, Collection<? extends A> c) {
        if (c.isEmpty())
            return false;
        throw new UnsupportedOperationException();
    }

    public A set(int index, A element) {
        throw new UnsupportedOperationException();
    }

    public void add(int index, A element) {
        throw new UnsupportedOperationException();
    }

    public A remove(int index) {
        throw new UnsupportedOperationException();
    }

    public int indexOf(Object o) {
        int i = 0;
        for (List<A> l = this; l.tail != null; l = l.tail, i++) {
            if (l.head == null ? o == null : l.head.equals(o))
                return i;
        }
        return -1;
    }

    public int lastIndexOf(Object o) {
        int last = -1;
        int i = 0;
        for (List<A> l = this; l.tail != null; l = l.tail, i++) {
            if (l.head == null ? o == null : l.head.equals(o))
                last = i;
        }
        return last;
    }

    public ListIterator<A> listIterator() {
        return Collections.unmodifiableList(new ArrayList<A>(this)).listIterator();
    }

    public ListIterator<A> listIterator(int index) {
        return Collections.unmodifiableList(new ArrayList<A>(this)).listIterator(index);
    }

    public java.util.List<A> subList(int fromIndex, int toIndex) {
        if  (fromIndex < 0 || toIndex > size() || fromIndex > toIndex)
            throw new IllegalArgumentException();

        ArrayList<A> a = new ArrayList<>(toIndex - fromIndex);
        int i = 0;
        for (List<A> l = this; l.tail != null; l = l.tail, i++) {
            if (i == toIndex)
                break;
            if (i >= fromIndex)
                a.add(l.head);
        }

        return Collections.unmodifiableList(a);
    }

    /**
     * Collect elements into a new list (using a @code{ListBuffer})
     */
    public static <Z> Collector<Z, ListBuffer<Z>, List<Z>> collector() {
        return Collector.of(ListBuffer::new,
                ListBuffer::add,
                (buf1, buf2)-> { buf1.addAll(buf2); return buf1; },
                ListBuffer::toList);
    }
}
