/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.tree;

import java.util.Enumeration;

/**
 * Defines the requirements for an object that can be used as a
 * tree node in a JTree.
 * <p>
 * Implementations of <code>TreeNode</code> that override <code>equals</code>
 * will typically need to override <code>hashCode</code> as well.  Refer
 * to {@link javax.swing.tree.TreeModel} for more information.
 *
 * For further information and examples of using tree nodes,
 * see <a
 href="https://docs.oracle.com/javase/tutorial/uiswing/components/tree.html">How to Use Tree Nodes</a>
 * in <em>The Java Tutorial.</em>
 *
 * @author Rob Davis
 * @author Scott Violet
 */

public interface TreeNode
{
    /**
     * Returns the child <code>TreeNode</code> at index
     * <code>childIndex</code>.
     *
     * @param   childIndex  index of child
     * @return              the child node at given index
     */
    TreeNode getChildAt(int childIndex);

    /**
     * Returns the number of children <code>TreeNode</code>s the receiver
     * contains.
     *
     * @return              the number of children the receiver contains
     */
    int getChildCount();

    /**
     * Returns the parent <code>TreeNode</code> of the receiver.
     *
     * @return              the parent of the receiver
     */
    TreeNode getParent();

    /**
     * Returns the index of <code>node</code> in the receivers children.
     * If the receiver does not contain <code>node</code>, -1 will be
     * returned.
     *
     * @param   node        node to be loked for
     * @return              index of specified node
     */
    int getIndex(TreeNode node);

    /**
     * Returns true if the receiver allows children.
     *
     * @return              whether the receiver allows children
     */
    boolean getAllowsChildren();

    /**
     * Returns true if the receiver is a leaf.
     *
     * @return              whether the receiver is a leaf
     */
    boolean isLeaf();

    /**
     * Returns the children of the receiver as an <code>Enumeration</code>.
     *
     * @return              the children of the receiver as an {@code Enumeration}
     */
    Enumeration<? extends TreeNode> children();
}
