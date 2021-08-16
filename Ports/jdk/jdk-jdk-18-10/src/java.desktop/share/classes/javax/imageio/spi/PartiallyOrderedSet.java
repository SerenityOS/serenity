/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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

package javax.imageio.spi;

import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * A set of {@code Object}s with pairwise orderings between them.
 * The {@code iterator} method provides the elements in
 * topologically sorted order.  Elements participating in a cycle
 * are not returned.
 *
 * Unlike the {@code SortedSet} and {@code SortedMap}
 * interfaces, which require their elements to implement the
 * {@code Comparable} interface, this class receives ordering
 * information via its {@code setOrdering} and
 * {@code unsetPreference} methods.  This difference is due to
 * the fact that the relevant ordering between elements is unlikely to
 * be inherent in the elements themselves; rather, it is set
 * dynamically accoring to application policy.  For example, in a
 * service provider registry situation, an application might allow the
 * user to set a preference order for service provider objects
 * supplied by a trusted vendor over those supplied by another.
 *
 */
class PartiallyOrderedSet<E> extends AbstractSet<E> {

    // The topological sort (roughly) follows the algorithm described in
    // Horowitz and Sahni, _Fundamentals of Data Structures_ (1976),
    // p. 315.

    // Maps Objects to DigraphNodes that contain them
    private Map<E, DigraphNode<E>> poNodes = new HashMap<>();

    // The set of Objects
    private Set<E> nodes = poNodes.keySet();

    /**
     * Constructs a {@code PartiallyOrderedSet}.
     */
    public PartiallyOrderedSet() {}

    public int size() {
        return nodes.size();
    }

    public boolean contains(Object o) {
        return nodes.contains(o);
    }

    /**
     * Returns an iterator over the elements contained in this
     * collection, with an ordering that respects the orderings set
     * by the {@code setOrdering} method.
     */
    public Iterator<E> iterator() {
        return new PartialOrderIterator<>(poNodes.values().iterator());
    }

    /**
     * Adds an {@code Object} to this
     * {@code PartiallyOrderedSet}.
     */
    public boolean add(E o) {
        if (nodes.contains(o)) {
            return false;
        }

        DigraphNode<E> node = new DigraphNode<>(o);
        poNodes.put(o, node);
        return true;
    }

    /**
     * Removes an {@code Object} from this
     * {@code PartiallyOrderedSet}.
     */
    public boolean remove(Object o) {
        DigraphNode<E> node = poNodes.get(o);
        if (node == null) {
            return false;
        }

        poNodes.remove(o);
        node.dispose();
        return true;
    }

    public void clear() {
        poNodes.clear();
    }

    /**
     * Sets an ordering between two nodes.  When an iterator is
     * requested, the first node will appear earlier in the
     * sequence than the second node.  If a prior ordering existed
     * between the nodes in the opposite order, it is removed.
     *
     * @return {@code true} if no prior ordering existed
     * between the nodes, {@code false} otherwise.
     */
    public boolean setOrdering(E first, E second) {
        DigraphNode<E> firstPONode = poNodes.get(first);
        DigraphNode<E> secondPONode = poNodes.get(second);

        secondPONode.removeEdge(firstPONode);
        return firstPONode.addEdge(secondPONode);
    }

    /**
     * Removes any ordering between two nodes.
     *
     * @return true if a prior prefence existed between the nodes.
     */
    public boolean unsetOrdering(E first, E second) {
        DigraphNode<E> firstPONode = poNodes.get(first);
        DigraphNode<E> secondPONode = poNodes.get(second);

        return firstPONode.removeEdge(secondPONode) ||
            secondPONode.removeEdge(firstPONode);
    }

    /**
     * Returns {@code true} if an ordering exists between two
     * nodes.
     */
    public boolean hasOrdering(E preferred, E other) {
        DigraphNode<E> preferredPONode = poNodes.get(preferred);
        DigraphNode<E> otherPONode = poNodes.get(other);

        return preferredPONode.hasEdge(otherPONode);
    }
}

class PartialOrderIterator<E> implements Iterator<E> {

    LinkedList<DigraphNode<E>> zeroList = new LinkedList<>();
    Map<DigraphNode<E>, Integer> inDegrees = new HashMap<>();

    public PartialOrderIterator(Iterator<DigraphNode<E>> iter) {
        // Initialize scratch in-degree values, zero list
        while (iter.hasNext()) {
            DigraphNode<E> node = iter.next();
            int inDegree = node.getInDegree();
            inDegrees.put(node, inDegree);

            // Add nodes with zero in-degree to the zero list
            if (inDegree == 0) {
                zeroList.add(node);
            }
        }
    }

    public boolean hasNext() {
        return !zeroList.isEmpty();
    }

    public E next() {
        DigraphNode<E> first = zeroList.removeFirst();

        // For each out node of the output node, decrement its in-degree
        Iterator<DigraphNode<E>> outNodes = first.getOutNodes();
        while (outNodes.hasNext()) {
            DigraphNode<E> node = outNodes.next();
            int inDegree = inDegrees.get(node).intValue() - 1;
            inDegrees.put(node, inDegree);

            // If the in-degree has fallen to 0, place the node on the list
            if (inDegree == 0) {
                zeroList.add(node);
            }
        }

        return first.getData();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
