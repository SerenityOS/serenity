/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1.humongousObjects.objectGraphTest;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ObjectGraph {

    private ObjectGraph() {
    }

    public enum ReferenceType {
        NONE,
        WEAK,
        SOFT,
        STRONG;
    }

    /**
     * Performs operation on all nodes that are reachable from initial ones
     *
     * @param nodes     initial nodes
     * @param operation operation
     */
    public static void propagateTransitiveProperty(Set<Object[]> nodes, Consumer<Object[]> operation) {
        Deque<Object[]> roots = new ArrayDeque<>();
        nodes.stream().forEach(roots::push);
        ObjectGraph.enumerateAndMark(roots, operation);
    }

    /**
     * Connects graph's vertexes with single-directed (vertex -> neighbour) link
     *
     * @param vertex    who is connected
     * @param neighbour connected to whom
     */
    private static void connectVertexes(Object[] vertex, Object[] neighbour) {

        // check if vertex array is full
        if (vertex[vertex.length - 1] != null) {
            throw new Error("Array is full and no connections could be added");
        }
        int i = 0;
        while (vertex[i] != null) {
            ++i;
        }
        vertex[i] = neighbour;
    }


    /**
     * Builds object graph using description from list of parsed nodes. Graph uses Object[] as nodes, first n elements
     * of array are links to connected nodes, others are null. Then runs visitors on generated graph
     *
     * @param parsedNodes             list of nodes' description
     * @param visitors                visitors that will visit each node of generated graph
     * @param humongousAllocationSize size of humongous node
     * @param simpleAllocationSize    size of simple (non-humongous) node
     * @return root reference to generated graph
     */
    public static Object[] generateObjectNodes(List<TestcaseData.FinalParsedNode> parsedNodes,
                                               Map<Predicate<TestcaseData.FinalParsedNode>,
                                                       BiConsumer<TestcaseData.FinalParsedNode, Object[][]>> visitors,
                                               int humongousAllocationSize, int simpleAllocationSize) {

        Object[][] objectNodes = new Object[parsedNodes.size()][];

        // Allocating nodes on Object[]
        for (int i = 0; i < parsedNodes.size(); ++i) {
            objectNodes[i] = new Object[(parsedNodes.get(i).isHumongous ?
                    humongousAllocationSize : simpleAllocationSize)];
        }

        // Connecting nodes on allocated on Object[]
        for (int i = 0; i < parsedNodes.size(); ++i) {
            for (int j = 0; j < parsedNodes.get(i).getConnectedTo().size(); ++j) {
                connectVertexes(objectNodes[i], objectNodes[parsedNodes.get(i).getConnectedTo().get(j)]);
            }
        }

        // Calling visitors
        visitors.entrySet()
                .stream()
                .forEach(
                        entry -> parsedNodes.stream()
                                .filter(parsedNode -> entry.getKey().test(parsedNode))
                                .forEach(node -> entry.getValue().accept(node, objectNodes))
                );

        return objectNodes[0];
    }

    /**
     * Enumerates graph starting with provided vertexes. All vertexes that are reachable from the provided ones are
     * marked
     *
     * @param markedParents provided vertexes
     * @param markVertex    lambda which marks vertexes
     */
    public static void enumerateAndMark(Deque<Object[]> markedParents,
                                        Consumer<Object[]> markVertex) {
        Map<Object[], Boolean> isVisited = new HashMap<>();
        while (!markedParents.isEmpty()) {
            Object[] vertex = markedParents.pop();
            if (vertex == null || isVisited.containsKey(vertex)) {
                continue;
            }
            isVisited.put(vertex, true);
            markVertex.accept(vertex);

            for (int i = 0; i < vertex.length; ++i) {
                if (vertex[i] == null) {
                    break;
                }
                markedParents.add((Object[]) vertex[i]);
            }
        }
    }

}
