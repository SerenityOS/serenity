/*
 * Copyright (c) 2000, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.dns;


import java.util.Hashtable;


/**
 * A NameNode represents a node in the DNS namespace.  Each node
 * has a label, which is its name relative to its parent (so the
 * node at Sun.COM has label "Sun").  Each node has a hashtable of
 * children indexed by their labels converted to lower-case.
 *
 * <p> A node may be addressed from another by giving a DnsName
 * consisting of the sequence of labels from one node to the other.
 *
 * <p> Each node also has an {@code isZoneCut} flag, used to indicate
 * if the node is a zone cut.  A zone cut is a node with an NS record
 * that is contained in one zone, but that actually belongs to a child zone.
 *
 * <p> All access is unsynchronized.
 *
 * @author Scott Seligman
 */


class NameNode {

    private String label;               // name of this node relative to its
                                        // parent, or null for root of a tree
    private Hashtable<String,NameNode> children = null;  // child nodes
    private boolean isZoneCut = false;  // true if this node is a zone cut
    private int depth = 0;              // depth in tree (0 for root)

    NameNode(String label) {
        this.label = label;
    }

    /*
     * Returns a newly-allocated NameNode.  Used to allocate new nodes
     * in a tree.  Should be overridden in a subclass to return an object
     * of the subclass's type.
     */
    protected NameNode newNameNode(String label) {
        return new NameNode(label);
    }

    /*
     * Returns the name of this node relative to its parent, or null for
     * the root of a tree.
     */
    String getLabel() {
        return label;
    }

    /*
     * Returns the depth of this node in the tree.  The depth of the root
     * is 0.
     */
    int depth() {
        return depth;
    }

    boolean isZoneCut() {
        return isZoneCut;
    }

    void setZoneCut(boolean isZoneCut) {
        this.isZoneCut = isZoneCut;
    }

    /*
     * Returns the children of this node, or null if there are none.
     * The caller must not modify the Hashtable returned.
     */
    Hashtable<String,NameNode> getChildren() {
        return children;
    }

    /*
     * Returns the child node given the hash key (the down-cased label)
     * for its name relative to this node, or null if there is no such
     * child.
     */
    NameNode get(String key) {
        return (children != null)
            ? children.get(key)
            : null;
    }

    /*
     * Returns the node at the end of a path, or null if the
     * node does not exist.
     * The path is specified by the labels of {@code name}, beginning
     * at index idx.
     */
    NameNode get(DnsName name, int idx) {
        NameNode node = this;
        for (int i = idx; i < name.size() && node != null; i++) {
            node = node.get(name.getKey(i));
        }
        return node;
    }

    /*
     * Returns the node at the end of a path, creating it and any
     * intermediate nodes as needed.
     * The path is specified by the labels of {@code name}, beginning
     * at index idx.
     */
    NameNode add(DnsName name, int idx) {
        NameNode node = this;
        for (int i = idx; i < name.size(); i++) {
            String label = name.get(i);
            String key = name.getKey(i);

            NameNode child = null;
            if (node.children == null) {
                node.children = new Hashtable<>();
            } else {
                child = node.children.get(key);
            }
            if (child == null) {
                child = newNameNode(label);
                child.depth = node.depth + 1;
                node.children.put(key, child);
            }
            node = child;
        }
        return node;
    }
}
