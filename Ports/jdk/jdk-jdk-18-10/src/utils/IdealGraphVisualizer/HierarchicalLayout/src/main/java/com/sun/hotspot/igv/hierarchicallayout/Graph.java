/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
 *
 */
package com.sun.hotspot.igv.hierarchicallayout;

import java.util.*;

/**
 *
 * @author Thomas Wuerthinger
 */
public class Graph<N, E> {

    private HashMap<Object, Node<N, E>> nodes;
    private HashMap<Object, Edge<N, E>> edges;
    private List<Node<N, E>> nodeList;

    public Graph() {
        nodes = new HashMap<>();
        edges = new HashMap<>();
        nodeList = new ArrayList<>();
    }

    public Node<N, E> createNode(N data, Object key) {
        Node<N, E> n = new Node<>(this, data);
        assert key == null || !nodes.containsKey(key);
        if (key != null) {
            nodes.put(key, n);
        }
        nodeList.add(n);
        return n;
    }

    public Edge<N, E> createEdge(Node<N, E> source, Node<N, E> dest, E data, Object key) {
        Edge<N, E> e = new Edge<>(this, source, dest, data);
        source.addOutEdge(e);
        dest.addInEdge(e);
        if (key != null) {
            edges.put(key, e);
        }
        return e;
    }

    public Node<N, E> getNode(Object key) {
        return nodes.get(key);
    }

    public Edge<N, E> getEdge(Object key) {
        return edges.get(key);
    }

    public Collection<Edge<N, E>> getEdges() {
        return Collections.unmodifiableCollection(edges.values());
    }

    public Collection<Node<N, E>> getNodes() {
        return Collections.unmodifiableList(nodeList);
    }

    public void removeEdge(Edge<N, E> e, Object key) {
        assert key == null || edges.containsKey(key);
        if (key != null) {
            edges.remove(key);
        }
        e.getSource().removeOutEdge(e);
        e.getDest().removeInEdge(e);
    }

    public class DFSTraversalVisitor {

        public void visitNode(Node<N, E> n) {
        }

        public boolean visitEdge(Edge<N, E> e, boolean backEdge) {
            return true;
        }
    }

    public class BFSTraversalVisitor {

        public void visitNode(Node<N, E> n, int depth) {
        }
    }

    public List<Node<N, E>> getNodesWithInDegree(int x) {
        return getNodesWithInDegree(x, true);
    }

    public List<Node<N, E>> getNodesWithInDegree(int x, boolean countSelfLoops) {

        List<Node<N, E>> result = new ArrayList<>();
        for (Node<N, E> n : getNodes()) {
            if (n.getInDegree(countSelfLoops) == x) {
                result.add(n);
            }
        }

        return result;

    }

    private void markReachable(Node<N, E> startingNode) {
        ArrayList<Node<N, E>> arr = new ArrayList<>();
        arr.add(startingNode);
        for (Node<N, E> n : getNodes()) {
            n.setReachable(false);
        }
        traverseDFS(arr, new DFSTraversalVisitor() {

            @Override
            public void visitNode(Node<N, E> n) {
                n.setReachable(true);
            }
        });
    }

    public void traverseBFS(Node<N, E> startingNode, BFSTraversalVisitor tv, boolean longestPath) {

        if (longestPath) {
            markReachable(startingNode);
        }

        for (Node<N, E> n : getNodes()) {
            n.setVisited(false);
            n.setActive(false);
        }

        Queue<Node<N, E>> queue = new LinkedList<>();
        queue.add(startingNode);
        startingNode.setVisited(true);
        int layer = 0;
        Node<N, E> lastOfLayer = startingNode;
        Node<N, E> lastAdded = null;

        while (!queue.isEmpty()) {

            Node<N, E> current = queue.poll();
            tv.visitNode(current, layer);
            current.setActive(false);


            for (Edge<N, E> e : current.getOutEdges()) {
                if (!e.getDest().isVisited()) {

                    boolean allow = true;
                    if (longestPath) {
                        for (Node<N, E> pred : e.getDest().getPredecessors()) {
                            if ((!pred.isVisited() || pred.isActive()) && pred.isReachable()) {
                                allow = false;
                                break;
                            }
                        }
                    }

                    if (allow) {
                        queue.offer(e.getDest());
                        lastAdded = e.getDest();
                        e.getDest().setVisited(true);
                        e.getDest().setActive(true);
                    }
                }
            }

            if (current == lastOfLayer && !queue.isEmpty()) {
                lastOfLayer = lastAdded;
                layer++;
            }
        }
    }

    public void traverseDFS(DFSTraversalVisitor tv) {
        traverseDFS(getNodes(), tv);
    }

    public void traverseDFS(Collection<Node<N, E>> startingNodes, DFSTraversalVisitor tv) {

        for (Node<N, E> n : getNodes()) {
            n.setVisited(false);
            n.setActive(false);
        }

        boolean result = false;
        for (Node<N, E> n : startingNodes) {
            traverse(tv, n);
        }
    }

    private void traverse(DFSTraversalVisitor tv, Node<N, E> n) {

        if (!n.isVisited()) {
            n.setVisited(true);
            n.setActive(true);
            tv.visitNode(n);

            for (Edge<N, E> e : n.getOutEdges()) {

                Node<N, E> next = e.getDest();
                if (next.isActive()) {
                    tv.visitEdge(e, true);
                } else {
                    if (tv.visitEdge(e, false)) {
                        traverse(tv, next);
                    }
                }
            }

            n.setActive(false);
        }

    }

    public boolean hasCycles() {

        for (Node<N, E> n : getNodes()) {
            n.setVisited(false);
            n.setActive(false);
        }

        boolean result = false;
        for (Node<N, E> n : getNodes()) {
            result |= checkCycles(n);
            if (result) {
                break;
            }
        }
        return result;
    }

    private boolean checkCycles(Node<N, E> n) {

        if (n.isActive()) {
            return true;
        }

        if (!n.isVisited()) {

            n.setVisited(true);
            n.setActive(true);

            for (Node<N, E> succ : n.getSuccessors()) {
                if (checkCycles(succ)) {
                    return true;
                }
            }

            n.setActive(false);

        }

        return false;
    }

    @Override
    public String toString() {

        StringBuilder s = new StringBuilder();
        s.append("Nodes: ");
        for (Node<N, E> n : getNodes()) {
            s.append(n.toString());
            s.append("\n");
        }

        s.append("Edges: ");

        for (Edge<N, E> e : getEdges()) {
            s.append(e.toString());
            s.append("\n");
        }

        return s.toString();
    }
}
