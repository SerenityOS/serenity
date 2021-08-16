/*
 * Copyright (c) 2002, 2003, Oracle and/or its affiliates. All rights reserved.
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

class List<A> {

    /** The first element of the list, supposed to be immutable.
     */
    public A head;

    /** The remainder of the list except for its first element, supposed
     *  to be immutable.
     */
    public List<A> tail;

    /** Construct a list given its head and tail.
     */
    public List(A head, List<A> tail) {
        this.tail = tail;
        this.head = head;
    }

    /** Construct an empty list.
     */
    public List() {
        this(null, null);
    }

    /** Construct a list consisting of given element.
     */
    public static <A> List<A> make(A x1) {
        return new List<A>(x1, new List<A>());
    }

    /** Construct a list consisting of given elements.
     */
    public static <A> List<A> make(A x1, A x2) {
        return new List<A>(x1, new List<A>(x2, new List<A>()));
    }

    /** Construct a list consisting of given elements.
     */
    public static <A> List<A> make(A x1, A x2, A x3) {
        return new List<A>(x1, new List<A>(x2, new List<A>(x3, new List<A>())));
    }

    /** Construct a list consisting all elements of given array.
     */
    public static <A> List<A> make(A[] vec) {
        List<A> xs = new List<A>();
        for (int i = vec.length - 1; i >= 0; i--)
            xs = new List<A>(vec[i], xs);
        return xs;
    }

    /** Construct a list consisting of a given number of identical elements.
     *  @param len    The number of elements in the list.
     *  @param init   The value of each element.
     */
    public static <A> List<A> make(int len, A init) {
        List<A> l = new List<A>();
        for (int i = 0; i < len; i++) l = new List<A>(init, l);
        return l;
    }

    /** Does list have no elements?
     */
    public boolean isEmpty() {
        return tail == null;
    }

    /** Does list have elements?
     */
    public boolean nonEmpty() {
        return tail != null;
    }

    /** Return the number of elements in this list.
     */
    public int length() {
        List<A> l = this;
        int len = 0;
        while (l.tail != null) {
            l = l.tail;
            len++;
        }
        return len;
    }

    /** Prepend given element to front of list, forming and returning
     *  a new list.
     */
    public List<A> prepend(A x) {
        return new List<A>(x, this);
    }

    /** Prepend given list of elements to front of list, forming and returning
     *  a new list.
     */
    public List<A> prependList(List<A> xs) {
        if (this.isEmpty()) return xs;
        else if (xs.isEmpty()) return this;
        else return this.prependList(xs.tail).prepend(xs.head);
    }

    /** Reverse list, forming and returning a new list.
     */
    public List<A> reverse() {
        List<A> rev = new List<A>();
        for (List<A> l = this; l.nonEmpty(); l = l.tail)
            rev = new List<A>(l.head, rev);
        return rev;
    }

    /** Append given element at length, forming and returning
     *  a new list.
     */
    public List<A> append(A x) {
        return make(x).prependList(this);
    }

    /** Append given list at length, forming and returning
     *  a new list.
     */
    public List<A> appendList(List<A> x) {
        return x.prependList(this);
    }

    /** Copy successive elements of this list into given vector until
     *  list is exhausted or end of vector is reached.
     */
    public A[] toArray(A[] vec) {
        int i = 0;
        List<A> l = this;
        while (l.nonEmpty() && i < vec.length) {
            vec[i] = l.head;
            l = l.tail;
            i++;
        }
        return vec;
    }

    /** Form a string listing all elements with given separator character.
     */
    public String toString(String sep) {
        if (isEmpty()) {
            return "";
        } else {
            StringBuffer buf = new StringBuffer();
            buf.append(((Object)head).toString());
            for (List<A> l = tail; l.nonEmpty(); l = l.tail) {
                buf.append(sep);
                buf.append(((Object)l.head).toString());
            }
            return buf.toString();
        }
    }

    /** Form a string listing all elements with comma as the separator character.
     */
    public String toString() {
        return toString(",");
    }

    /** Compute a hash code, overrides Object
     */
    public int hashCode() {
        List<A> l = this;
        int h = 0;
        while (l.tail != null) {
            h = h * 41 + (head != null ? head.hashCode() : 0);
            l = l.tail;
        }
        return h;
    }

    /** Is this list the same as other list?
     */
    public boolean equals(Object other) {
        return other instanceof List && equals(this, (List)other);
    }

    /** Are the two lists the same?
     */
    public static boolean equals(List xs, List ys) {
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
    public boolean contains(A x) {
        List<A> l = this;
        while (l.tail != null) {
            if (x == null) {
                if (l.head == null) return true;
            } else {
                if (x.equals(l.head)) return true;
            }
            l = l.tail;
        }
        return false;
    }

}
