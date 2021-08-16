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

package javax.swing.event;

import java.util.EventListener;

/**
 * Defines the interface for an object that listens
 * to changes in a TreeModel.
 * For further information and examples see
 * <a
 href="https://docs.oracle.com/javase/tutorial/uiswing/events/treemodellistener.html">How to Write a Tree Model Listener</a>,
 * a section in <em>The Java Tutorial.</em>
 *
 * @author Rob Davis
 * @author Ray Ryan
 */
public interface TreeModelListener extends EventListener {

    /**
     * <p>Invoked after a node (or a set of siblings) has changed in some
     * way. The node(s) have not changed locations in the tree or
     * altered their children arrays, but other attributes have
     * changed and may affect presentation. Example: the name of a
     * file has changed, but it is in the same location in the file
     * system.
     *
     * <p>To indicate the root has changed, childIndices and children
     * will be null.
     *
     * <p>Use {@code e.getPath()} to get the parent of the changed node(s).
     * {@code e.getChildIndices()} returns the index(es) of the changed node(s).
     *
     * @param e a {@code TreeModelEvent} describing changes to a tree model
     */
    void treeNodesChanged(TreeModelEvent e);

    /**
     * <p>Invoked after nodes have been inserted into the tree.</p>
     *
     * <p>Use {@code e.getPath()} to get the parent of the new node(s).
     * {@code e.getChildIndices()} returns the index(es) of the new node(s)
     * in ascending order.
     *
     * @param e a {@code TreeModelEvent} describing changes to a tree model
     */
    void treeNodesInserted(TreeModelEvent e);

    /**
     * <p>Invoked after nodes have been removed from the tree.  Note that
     * if a subtree is removed from the tree, this method may only be
     * invoked once for the root of the removed subtree, not once for
     * each individual set of siblings removed.</p>
     *
     * <p>Use {@code e.getPath()} to get the former parent of the deleted
     * node(s). {@code e.getChildIndices()} returns, in ascending order, the
     * index(es) the node(s) had before being deleted.
     *
     * @param e a {@code TreeModelEvent} describing changes to a tree model
     */
    void treeNodesRemoved(TreeModelEvent e);

    /**
     * <p>Invoked after the tree has drastically changed structure from a
     * given node down.  If the path returned by e.getPath() is of length
     * one and the first element does not identify the current root node
     * the first element should become the new root of the tree.
     *
     * <p>Use {@code e.getPath()} to get the path to the node.
     * {@code e.getChildIndices()} returns null.
     *
     * @param e a {@code TreeModelEvent} describing changes to a tree model
     */
    void treeStructureChanged(TreeModelEvent e);

}
