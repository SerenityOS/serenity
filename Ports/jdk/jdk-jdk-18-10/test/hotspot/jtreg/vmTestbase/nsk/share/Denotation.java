/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share;

import java.util.*;

/**
 * Denotation implies a pair of algorithms for naming and
 * indexing of some objects.
 *
 * <p>No matter what kind of objects, just make sure that:
 * <ul>
 * <li><tt>indexFor(nameFor(index))</tt> equals to <tt>index</tt>
 * </li>
 * <li><tt>nameFor(indexFor(name))</tt> is equivalent to <tt>name</tt>
 * </li>
 * </ul>
 *
 * <p>The notions of indeces equality and names equivalence
 * are formalized by the methods <tt>equality()</tt> and
 * <tt>equivalence()</tt> correspondingly.
 *
 * <p>For better understanding of Denotation, you may want to
 * see the TreeNodesDenotation class as an implementation example.
 *
 * @see #equality(int[],int[])
 * @see #equivalence(String,String)
 * @see TreeNodesDenotation
 */
abstract public class Denotation {
    /**
     * Check if the <tt>name</tt> is legal, and return the
     * numeric index for that object denoted by the given
     * <tt>name</tt>.
     *
     * @throws IllegalArgumentException If the <tt>name</tt>
     * is illegal.
     */
    abstract public int[] indexFor(String name);

    /**
     * Check if the <tt>index[]</tt> is legal, and return
     * a symbolic name for the object denoted by the given
     * <tt>index[]</tt>.
     *
     * @throws IllegalArgumentException If the <tt>index[]</tt>
     * is illegal.
     */
    abstract public String nameFor(int[] index);

    /**
     * Re-call to <tt>nameFor(int[])</tt> with the 1-element
     * array <tt>{i}</tt> as the <tt>index</tt> argument.
     *
     * @see #nameFor(int[])
     */
    public String nameFor(int i) {
        return nameFor(new int[] { i });
    }

    /**
     * Re-call to <tt>nameFor(int[])</tt> with the 2-elements
     * array <tt>{i0,i1}</tt> as the <tt>index</tt> argument.
     *
     * @see #nameFor(int[])
     */
    public String nameFor(int i0, int i1) {
        return nameFor(new int[] {i0, i1});
    }

    /**
     * Re-call to <tt>nameFor(int[])</tt> with the 3-elements
     * array <tt>{i0,i1,i2}</tt> as the <tt>index</tt> argument.
     *
     * @see #nameFor(int[])
     */
    public String nameFor(int i0, int i1, int i2) {
        return nameFor(new int[] {i0, i1, i2});
    }

    /**
     * Indeces equality means equality of objects they denote.
     *
     * <p>Indeces <tt>index1[]</tt> and <tt>index2[]</tt> are
     * equal, if they are equal as <tt>int[]</tt> arrays. But,
     * there is no index equal to <tt>null</tt>; particularly,
     * <tt>null</tt> is not equal to itself.
     *
     * @see Arrays#equals(int[],int[])
     */
    public boolean equality(int[] index1, int[] index2) {
        if (index1 == null || index2 == null)
            return false;
        return Arrays.equals(index1,index2);
    }

    /**
     * Names equivalence means equality of objects they denote.
     *
     * <p>Strings <tt>name1</tt> and <tt>name2</tt> are equivalent,
     * if correspondent indeces are equal. There is no <tt>name</tt>
     * equivalent to <tt>null</tt>; particularly, <tt>null</tt> is
     * not equivalent to itself.
     *
     * @see #equality(int[],int[])
     */
    public boolean equivalence(String name1, String name2) {
        if (name1 == null || name2 == null)
            return false;
        return equality(indexFor(name1),indexFor(name2));
    }

    /**
     * Dummy constructor.
     */
    protected Denotation() {
    }
}
