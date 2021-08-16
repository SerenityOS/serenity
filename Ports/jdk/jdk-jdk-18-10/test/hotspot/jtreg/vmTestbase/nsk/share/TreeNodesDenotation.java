/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * This denotation provides naming and indexing for nodes
 * of a binary, or ternary, or <tt>n</tt>-ary tree.
 *
 * <p>Here, <tt>n</tt> would be the length of a symbols
 * string used as an alphabeth for nodes naming. For a
 * binary tree, <tt>n=2</tt>, and an aplhabeth could be
 * the <tt>"LR"</tt> string. This implies the following
 * naming for tree nodes:
 * <pre>
 *              (empty)
 *             /       \
 *            L         R
 *          /   \     /   \
 *         LL   LR   RL   RR
 *        /  \ /  \ /  \ /  \
 * </pre>
 *
 * <p>Anyway, the tree root node is named with the empty
 * string <tt>""</tt> and is indexed with 2-zeroes array
 * <tt>{0,0}</tt>.
 *
 * <p>Index for a tree node is 2-elements <tt>int[]</tt>
 * array. The 1st element is the node's level in a tree.
 * The 2nd element is the item's number among all nodes of
 * that level; provided that node items are enumerated from
 * <tt>0</tt> to <tt>n</tt><sup>level</sup><tt>-1</tt>.
 * Given a level, lexicographic order is assumed for the
 * nodes of the same level.
 *
 * <p>For example: given the above sample tree, the node
 * <tt>"L"</tt> has the index <tt>{1,0}</tt>, while the
 * node <tt>"RL"</tt> has the index <tt>{2,2}</tt>.
 *
 * <p>In general case, ordering of characters used for nodes
 * naming is implied by the given alphabeth. This may differ
 * from the ``natural'' ordering. For example, if alphabeth
 * is <tt>"ZYX...CBA"</tt>, then ordering for nodes would be
 * opposite to ``natural''.
 */
public class TreeNodesDenotation extends Denotation {
    /**
     * Symbols to denote tree nodes.
     *
     * @see #TreeNodeDenotation(String)
     */
    private String alphabeth;

    /**
     * Standard denotation for a binary tree; alphabeth
     * is <tt>"LR"</tt>.
     *
     * @see #TreeNodesDenotation(String)
     */
    public TreeNodesDenotation() {
        this("LR");
    }

    /**
     * Denotation for nodes of a tree.
     *
     * <p>Each tree node is marked with a string of symbols
     * from the given <tt>alphabeth</tt>. A string length
     * equals to the node's level. The root node is always
     * denoted with the empty string.
     *
     * <p>For example, an <tt>alphabeth</tt> for a binary
     * tree could be <tt>"LR"</tt>, or <tt>"01"</tt>, or
     * any 2-symbols string. However, <tt>"lL"</tt> or
     * <tt>"rR"</tt> would be illegal because of collision
     * between upper- and lower- case letters.
     *
     * <p>In general case, it is illegal for <tt>alphabeth</tt>
     * to contain two or several copies of the same symbol.
     * This constructor deems lower- and upper-case variants
     * of the same letter are the same symbol.
     *
     * @throws IllegalArgumentException If the <tt>alphabeth</tt>
     * looks illegal.
     */
    public TreeNodesDenotation(String alphabeth) {
        if (alphabeth.length() == 0)
            throw new IllegalArgumentException("empty alphabeth");
        // Check for lower- to upper- case collision:
        this.alphabeth = alphabeth.toUpperCase();
        int length = this.alphabeth.length();
        Set<Character> pool = new HashSet<Character>(); // still empty
        for (int i=0; i<length; i++)
            pool.add(Character.valueOf(this.alphabeth.charAt(i)));
        if (pool.size() != length)
            throw new IllegalArgumentException("collision: " + alphabeth);
    }

    /**
     * Check if the <tt>name</tt> is legal, and return the
     * numeric index for the tree node denoted by the given
     * <tt>name</tt>.
     *
     * @throws IllegalArgumentException If the <tt>name</tt>
     * is illegal.
     */
    public int[] indexFor(String name) {
        int level = name.length();
        int factor = alphabeth.length();
        long item = 0;
        for (int i=0; i<level; i++) {
            char symbol = Character.toUpperCase(name.charAt(i));
            int position = alphabeth.indexOf(symbol);
            if (position < 0)
                throw new IllegalArgumentException("unknown symbol: " + name);
            item = item*factor + position;
            if (item < 0 || item > Integer.MAX_VALUE)
                throw new IllegalArgumentException("too long name: " + name);
        };
        int[] index = new int [] { level, (int)item };
        return index;
    }

    /**
     * Check if the <tt>index[]</tt> is legal, and return
     * a symbolic name for the tree node denoted by the
     * given <tt>index[]</tt>.
     *
     * @throws IllegalArgumentException If the <tt>index[]</tt>
     * is illegal.
     */
    public String nameFor(int[] index) {
        if (index.length != 2)
            throw new IllegalArgumentException(
                "index dimention: " + index.length);
        StringBuffer name = new StringBuffer(); // still empty
        int level = index[0];
        int item  = index[1];
        if (level < 0 || item < 0)
            throw new IllegalArgumentException(
                "negative index: " + level + ", " + item);
        int factor = alphabeth.length();
        for (int i=0; i<level; i++) {
            int k = item % factor;
            name.append(alphabeth.charAt(k));
            item = item / factor;
        };
        if (item != 0)
            throw new IllegalArgumentException(
                "out of range: {"+ index[0] + "," + index[1] + "}");
        return new String(name.reverse());
    }
}
