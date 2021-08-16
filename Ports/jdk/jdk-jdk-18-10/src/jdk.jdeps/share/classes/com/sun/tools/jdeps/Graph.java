/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tools.jdeps;

import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Graph<T> {
    private final Set<T> nodes;
    private final Map<T, Set<T>> edges;

    public Graph(Set<T> nodes, Map<T, Set<T>> edges) {
        this.nodes = Collections.unmodifiableSet(nodes);
        this.edges = Collections.unmodifiableMap(edges);
    }

    public Set<T> nodes() {
        return nodes;
    }

    public Map<T, Set<T>> edges() {
        return edges;
    }

    public Set<T> adjacentNodes(T u) {
        return edges.get(u);
    }

    public boolean contains(T u) {
        return nodes.contains(u);
    }

    public Set<Edge<T>> edgesFrom(T u) {
        return edges.get(u).stream()
                    .map(v -> new Edge<T>(u, v))
                    .collect(Collectors.toSet());
    }

    /**
     * Returns a new Graph after transitive reduction
     */
    public Graph<T> reduce() {
        Builder<T> builder = new Builder<>();
        nodes.stream()
                .forEach(u -> {
                    builder.addNode(u);
                    edges.get(u).stream()
                         .filter(v -> !pathExists(u, v, false))
                         .forEach(v -> builder.addEdge(u, v));
                });
        return builder.build();
    }

    /**
     * Returns a new Graph after transitive reduction.  All edges in
     * the given g takes precedence over this graph.
     *
     * @throw IllegalArgumentException g must be a subgraph this graph
     */
    public Graph<T> reduce(Graph<T> g) {
        boolean subgraph = nodes.containsAll(g.nodes) &&
                g.edges.keySet().stream()
                       .allMatch(u -> adjacentNodes(u).containsAll(g.adjacentNodes(u)));
        if (!subgraph) {
            throw new IllegalArgumentException(g + " is not a subgraph of " + this);
        }

        Builder<T> builder = new Builder<>();
        nodes.stream()
                .forEach(u -> {
                    builder.addNode(u);
                    // filter the edge if there exists a path from u to v in the given g
                    // or there exists another path from u to v in this graph
                    edges.get(u).stream()
                         .filter(v -> !g.pathExists(u, v) && !pathExists(u, v, false))
                         .forEach(v -> builder.addEdge(u, v));
                });

        // add the overlapped edges from this graph and the given g
        g.edges().keySet().stream()
                .forEach(u -> g.adjacentNodes(u).stream()
                                .filter(v -> isAdjacent(u, v))
                                .forEach(v -> builder.addEdge(u, v)));
        return builder.build().reduce();
    }

    /**
     * Returns nodes sorted in topological order.
     */
    public Stream<T> orderedNodes() {
        TopoSorter<T> sorter = new TopoSorter<>(this);
        return sorter.result.stream();
    }

    /**
     * Traverse this graph and performs the given action in topological order
     */
    public void ordered(Consumer<T> action) {
        TopoSorter<T> sorter = new TopoSorter<>(this);
        sorter.ordered(action);
    }

    /**
     * Traverses this graph and performs the given action in reverse topological order
     */
    public void reverse(Consumer<T> action) {
        TopoSorter<T> sorter = new TopoSorter<>(this);
        sorter.reverse(action);
    }

    /**
     * Returns a transposed graph from this graph
     */
    public Graph<T> transpose() {
        Builder<T> builder = new Builder<>();
        builder.addNodes(nodes);
        // reverse edges
        edges.keySet().forEach(u -> {
            edges.get(u).stream()
                .forEach(v -> builder.addEdge(v, u));
        });
        return builder.build();
    }

    /**
     * Returns all nodes reachable from the given set of roots.
     */
    public Set<T> dfs(Set<T> roots) {
        Deque<T> deque = new ArrayDeque<>(roots);
        Set<T> visited = new HashSet<>();
        while (!deque.isEmpty()) {
            T u = deque.pop();
            if (!visited.contains(u)) {
                visited.add(u);
                if (contains(u)) {
                    adjacentNodes(u).stream()
                        .filter(v -> !visited.contains(v))
                        .forEach(deque::push);
                }
            }
        }
        return visited;
    }

    private boolean isAdjacent(T u, T v) {
        return edges.containsKey(u) && edges.get(u).contains(v);
    }

    private boolean pathExists(T u, T v) {
        return pathExists(u, v, true);
    }

    /**
     * Returns true if there exists a path from u to v in this graph.
     * If includeAdjacent is false, it returns true if there exists
     * another path from u to v of distance > 1
     */
    private boolean pathExists(T u, T v, boolean includeAdjacent) {
        if (!nodes.contains(u) || !nodes.contains(v)) {
            return false;
        }
        if (includeAdjacent && isAdjacent(u, v)) {
            return true;
        }
        Deque<T> stack = new ArrayDeque<>();
        Set<T> visited = new HashSet<>();
        stack.push(u);
        while (!stack.isEmpty()) {
            T node = stack.pop();
            if (node.equals(v)) {
                return true;
            }
            if (!visited.contains(node)) {
                visited.add(node);
                edges.get(node).stream()
                     .filter(e -> includeAdjacent || !node.equals(u) || !e.equals(v))
                     .forEach(stack::push);
            }
        }
        assert !visited.contains(v);
        return false;
    }

    public void printGraph(PrintWriter out) {
        out.println("graph for " + nodes);
        nodes.stream()
             .forEach(u -> adjacentNodes(u).stream()
                               .forEach(v -> out.format("  %s -> %s%n", u, v)));
    }

    @Override
    public String toString() {
        return nodes.toString();
    }

    static class Edge<T> {
        final T u;
        final T v;
        Edge(T u, T v) {
            this.u = u;
            this.v = v;
        }

        @Override
        public String toString() {
            return String.format("%s -> %s", u, v);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || !(o instanceof Edge))
                return false;

            @SuppressWarnings("unchecked")
            Edge<T> edge = (Edge<T>) o;

            return u.equals(edge.u) && v.equals(edge.v);
        }

        @Override
        public int hashCode() {
            int result = u.hashCode();
            result = 31 * result + v.hashCode();
            return result;
        }
    }

    static class Builder<T> {
        final Set<T> nodes = new HashSet<>();
        final Map<T, Set<T>> edges = new HashMap<>();

        public void addNode(T node) {
            if (nodes.contains(node)) {
                return;
            }
            nodes.add(node);
            edges.computeIfAbsent(node, _e -> new HashSet<>());
        }

        public void addNodes(Set<T> nodes) {
            this.nodes.addAll(nodes);
        }

        public void addEdge(T u, T v) {
            addNode(u);
            addNode(v);
            edges.get(u).add(v);
        }

        public Graph<T> build() {
            return new Graph<T>(nodes, edges);
        }
    }

    /**
     * Topological sort
     */
    static class TopoSorter<T> {
        final Deque<T> result = new ArrayDeque<>();
        final Graph<T> graph;
        TopoSorter(Graph<T> graph) {
            this.graph = graph;
            sort();
        }

        public void ordered(Consumer<T> action) {
            result.iterator().forEachRemaining(action);
        }

        public void reverse(Consumer<T> action) {
            result.descendingIterator().forEachRemaining(action);
        }

        private void sort() {
            Set<T> visited = new HashSet<>();
            Set<T> done = new HashSet<>();
            for (T node : graph.nodes()) {
                if (!visited.contains(node)) {
                    visit(node, visited, done);
                }
            }
        }

        private void visit(T node, Set<T> visited, Set<T> done) {
            if (visited.contains(node)) {
                if (!done.contains(node)) {
                    throw new IllegalArgumentException("Cyclic detected: " +
                        node + " " + graph.edges().get(node));
                }
                return;
            }
            visited.add(node);
            graph.edges().get(node).stream()
                 .forEach(x -> visit(x, visited, done));
            done.add(node);
            result.addLast(node);
        }
    }
}
