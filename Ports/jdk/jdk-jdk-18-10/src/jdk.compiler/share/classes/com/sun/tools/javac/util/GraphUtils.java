/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

/** <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class GraphUtils {

    /**
     * Basic interface for defining various dependency kinds.
     */
    public interface DependencyKind { }

    /**
     * Common superinterfaces to all graph nodes.
     */
    public interface Node<D, N extends Node<D, N>> {
        /**
         * visitor method.
         */
        <A> void accept(NodeVisitor<D, N, A> visitor, A arg);
    }

    /**
     * Visitor for graph nodes.
     */
    static abstract class NodeVisitor<D, N extends Node<D, N>, A> {
        /**
         * Visitor action for nodes.
         */
        public abstract void visitNode(N node, A arg);
        /**
         * Visitor action for a dependency between 'from' and 'to' with given kind.
         */
        public abstract void visitDependency(DependencyKind dk, N from, N to, A arg);

        /**
         * Visitor entry point.
         */
        public void visit(Collection<? extends N> nodes, A arg) {
            for (N n : new ArrayList<>(nodes)) {
                n.accept(this, arg);
            }
        }
    }

    /**
     * Optional interface for nodes supporting dot-based representation.
     */
    public interface DottableNode<D, N extends DottableNode<D, N>> extends Node<D, N> {
        /**
         * Retrieves the set of dot attributes associated with the node.
         */
        Properties nodeAttributes();
        /**
         * Retrieves the set of dot attributes associated with a given dependency.
         */
        Properties dependencyAttributes(N to, DependencyKind dk);
    }

    /**
     * This class is a basic abstract class for representing a node.
     * A node is associated with a given data.
     */
    public static abstract class AbstractNode<D, N extends AbstractNode<D, N>> implements Node<D, N> {
        public final D data;

        public AbstractNode(D data) {
            this.data = data;
        }

        /**
         * Get an array of the dependency kinds supported by this node.
         */
        public abstract DependencyKind[] getSupportedDependencyKinds();

        /**
         * Get all dependencies of a given kind
         */
        public abstract Collection<? extends N> getDependenciesByKind(DependencyKind dk);

        @Override
        public String toString() {
            return data.toString();
        }

        @SuppressWarnings("unchecked")
        public <A> void accept(NodeVisitor<D, N, A> visitor, A arg) {
            visitor.visitNode((N)this, arg);
            for (DependencyKind dk : getSupportedDependencyKinds()) {
                for (N dep : new ArrayList<>(getDependenciesByKind(dk))) {
                    visitor.visitDependency(dk, (N)this, dep, arg);
                }
            }
        }
    }

    /**
     * This class specialized Node, by adding elements that are required in order
     * to perform Tarjan computation of strongly connected components.
     */
    public static abstract class TarjanNode<D, N extends TarjanNode<D, N>> extends AbstractNode<D, N>
            implements Comparable<N> {
        int index = -1;
        int lowlink;
        boolean active;

        public TarjanNode(D data) {
            super(data);
        }

        public abstract Iterable<? extends N> getAllDependencies();

        public int compareTo(N o) {
            return (index < o.index) ? -1 : (index == o.index) ? 0 : 1;
        }
    }

    /**
     * Tarjan's algorithm to determine strongly connected components of a
     * directed graph in linear time. Works on TarjanNode.
     */
    public static <D, N extends TarjanNode<D, N>> List<? extends List<? extends N>> tarjan(Iterable<? extends N> nodes) {
        Tarjan<D, N> tarjan = new Tarjan<>();
        return tarjan.findSCC(nodes);
    }
    //where
    private static class Tarjan<D, N extends TarjanNode<D, N>> {

        /** Unique node identifier. */
        int index = 0;

        /** List of SCCs found fso far. */
        ListBuffer<List<N>> sccs = new ListBuffer<>();

        /** Stack of all reacheable nodes from given root. */
        ListBuffer<N> stack = new ListBuffer<>();

        private List<? extends List<? extends N>> findSCC(Iterable<? extends N> nodes) {
            for (N node : nodes) {
                if (node.index == -1) {
                    findSCC(node);
                }
            }
            return sccs.toList();
        }

        private void findSCC(N v) {
            visitNode(v);
            for (N n: v.getAllDependencies()) {
                if (n.index == -1) {
                    //it's the first time we see this node
                    findSCC(n);
                    v.lowlink = Math.min(v.lowlink, n.lowlink);
                } else if (stack.contains(n)) {
                    //this node is already reachable from current root
                    v.lowlink = Math.min(v.lowlink, n.index);
                }
            }
            if (v.lowlink == v.index) {
                //v is the root of a SCC
                addSCC(v);
            }
        }

        private void visitNode(N n) {
            n.index = index;
            n.lowlink = index;
            index++;
            stack.prepend(n);
            n.active = true;
        }

        private void addSCC(N v) {
            N n;
            ListBuffer<N> cycle = new ListBuffer<>();
            do {
                n = stack.remove();
                n.active = false;
                cycle.add(n);
            } while (n != v);
            sccs.add(cycle.toList());
        }
    }

    /**
     * Debugging: dot representation of a set of connected nodes. The resulting
     * dot representation will use {@code Node.toString} to display node labels
     * and {@code Node.printDependency} to display edge labels. The resulting
     * representation is also customizable with a graph name and a header.
     */
    public static <D, N extends DottableNode<D, N>> String toDot(Collection<? extends N> nodes, String name, String header) {
        StringBuilder buf = new StringBuilder();
        buf.append(String.format("digraph %s {\n", name));
        buf.append(String.format("label = %s;\n", DotVisitor.wrap(header)));
        DotVisitor<D, N> dotVisitor = new DotVisitor<>();
        dotVisitor.visit(nodes, buf);
        buf.append("}\n");
        return buf.toString();
    }

    /**
     * This visitor is used to dump the contents of a set of nodes of type {@link DottableNode}
     * onto a string builder.
     */
    public static class DotVisitor<D, N extends DottableNode<D, N>> extends NodeVisitor<D, N, StringBuilder> {

        @Override
        public void visitDependency(DependencyKind dk, N from, N to, StringBuilder buf) {
            buf.append(String.format("%s -> %s", from.hashCode(), to.hashCode()));
            buf.append(formatProperties(from.dependencyAttributes(to, dk)));
            buf.append('\n');
        }

        @Override
        public void visitNode(N node, StringBuilder buf) {
            buf.append(String.format("%s ", node.hashCode()));
            buf.append(formatProperties(node.nodeAttributes()));
            buf.append('\n');
        }

        protected String formatProperties(Properties p) {
            return p.toString().replaceAll(",", " ")
                .replaceAll("\\{", "[")
                .replaceAll("\\}", "]");
        }

        protected static String wrap(String s) {
            String res = "\"" + s + "\"";
            return res.replaceAll("\n", "");
        }
    }
}
