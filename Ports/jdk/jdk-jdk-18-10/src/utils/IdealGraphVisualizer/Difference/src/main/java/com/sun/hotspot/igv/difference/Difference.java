/*
 * Copyright (c) 1998, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.hotspot.igv.difference;

import com.sun.hotspot.igv.data.Properties;
import com.sun.hotspot.igv.data.*;
import com.sun.hotspot.igv.data.services.Scheduler;
import java.util.*;
import org.openide.util.Lookup;

/**
 *
 * @author Thomas Wuerthinger
 */
public class Difference {

    public static final String PROPERTY_STATE = "state";
    public static final String VALUE_NEW = "new";
    public static final String VALUE_CHANGED = "changed";
    public static final String VALUE_SAME = "same";
    public static final String VALUE_DELETED = "deleted";
    public static final String NEW_PREFIX = "NEW_";
    public static final String MAIN_PROPERTY = "name";
    public static final double LIMIT = 100.0;
    public static final String[] IGNORE_PROPERTIES = new String[]{"idx", "debug_idx"};

    public static InputGraph createDiffGraph(InputGraph a, InputGraph b) {
        if (a.getGroup() == b.getGroup()) {
            return createDiffSameGroup(a, b);
        } else {
            return createDiff(a, b);
        }
    }

    private static InputGraph createDiffSameGroup(InputGraph a, InputGraph b) {
        Map<Integer, InputNode> keyMapB = new HashMap<>(b.getNodes().size());
        for (InputNode n : b.getNodes()) {
            Integer key = n.getId();
            assert !keyMapB.containsKey(key);
            keyMapB.put(key, n);
        }

        Set<NodePair> pairs = new HashSet<>();

        for (InputNode n : a.getNodes()) {
            Integer key = n.getId();


            if (keyMapB.containsKey(key)) {
                InputNode nB = keyMapB.get(key);
                pairs.add(new NodePair(n, nB));
            }
        }

        return createDiff(a, b, pairs);
    }

    private static void ensureScheduled(InputGraph a) {
        if (a.getBlocks().isEmpty()) {
            Scheduler s = Lookup.getDefault().lookup(Scheduler.class);
            a.clearBlocks();
            s.schedule(a);
            a.ensureNodesInBlocks();
        }
    }

    private static InputGraph createDiff(InputGraph a, InputGraph b, Set<NodePair> pairs) {
        ensureScheduled(a);
        ensureScheduled(b);

        Group g = new Group(null);
        g.setMethod(a.getGroup().getMethod());
        if (a.getGroup() == b.getGroup()) {
            g.getProperties().add(a.getGroup().getProperties());
        } else {
            // copy properties that have the same value in both groups
            Properties bps = b.getGroup().getProperties();
            for (Property p : a.getGroup().getProperties()) {
                String value = p.getValue();
                if (value != null && value.equals(bps.get(p.getName()))) {
                    g.getProperties().setProperty(p.getName(), value);
                }
            }
        }
        g.getProperties().setProperty("name", "Difference");
        InputGraph graph = new InputGraph(a.getName() + ", " + b.getName());
        g.addElement(graph);

        Map<InputBlock, InputBlock> blocksMap = new HashMap<>();
        for (InputBlock blk : a.getBlocks()) {
            InputBlock diffblk = graph.addBlock(blk.getName());
            blocksMap.put(blk, diffblk);
        }
        for (InputBlock blk : b.getBlocks()) {
            InputBlock diffblk = graph.getBlock(blk.getName());
            if (diffblk == null) {
                diffblk = graph.addBlock(blk.getName());
            }
            blocksMap.put(blk, diffblk);
        }

        // Difference between block edges
        Set<Pair<String, String>> aEdges = new HashSet<>();
        for (InputBlockEdge edge : a.getBlockEdges()) {
            aEdges.add(new Pair<>(edge.getFrom().getName(), edge.getTo().getName()));
        }
        for (InputBlockEdge bEdge : b.getBlockEdges()) {
            InputBlock from = bEdge.getFrom();
            InputBlock to = bEdge.getTo();
            Pair<String, String> pair = new Pair<>(from.getName(), to.getName());
            if (aEdges.contains(pair)) {
                // same
                graph.addBlockEdge(blocksMap.get(from), blocksMap.get(to));
                aEdges.remove(pair);
            } else {
                // added
                InputBlockEdge edge = graph.addBlockEdge(blocksMap.get(from), blocksMap.get(to));
                edge.setState(InputBlockEdge.State.NEW);
            }
        }
        for (Pair<String, String> deleted : aEdges) {
            // removed
            InputBlock from = graph.getBlock(deleted.getLeft());
            InputBlock to = graph.getBlock(deleted.getRight());
            InputBlockEdge edge = graph.addBlockEdge(from, to);
            edge.setState(InputBlockEdge.State.DELETED);
        }

        Set<InputNode> nodesA = new HashSet<>(a.getNodes());
        Set<InputNode> nodesB = new HashSet<>(b.getNodes());

        Map<InputNode, InputNode> inputNodeMap = new HashMap<>(pairs.size());
        for (NodePair p : pairs) {
            InputNode n = p.getLeft();
            assert nodesA.contains(n);
            InputNode nB = p.getRight();
            assert nodesB.contains(nB);

            nodesA.remove(n);
            nodesB.remove(nB);
            InputNode n2 = new InputNode(n);
            inputNodeMap.put(n, n2);
            inputNodeMap.put(nB, n2);
            graph.addNode(n2);
            InputBlock block = blocksMap.get(a.getBlock(n));
            block.addNode(n2.getId());
            markAsChanged(n2, n, nB);
        }

        for (InputNode n : nodesA) {
            InputNode n2 = new InputNode(n);
            graph.addNode(n2);
            InputBlock block = blocksMap.get(a.getBlock(n));
            block.addNode(n2.getId());
            markAsDeleted(n2);
            inputNodeMap.put(n, n2);
        }

        int curIndex = 0;
        for (InputNode n : nodesB) {
            InputNode n2 = new InputNode(n);

            // Find new ID for node of b, does not change the id property
            while (graph.getNode(curIndex) != null) {
                curIndex++;
            }

            n2.setId(curIndex);
            graph.addNode(n2);
            InputBlock block = blocksMap.get(b.getBlock(n));
            block.addNode(n2.getId());
            markAsNew(n2);
            inputNodeMap.put(n, n2);
        }

        Collection<InputEdge> edgesA = a.getEdges();
        Collection<InputEdge> edgesB = b.getEdges();

        Set<InputEdge> newEdges = new HashSet<>();

        for (InputEdge e : edgesA) {
            int from = e.getFrom();
            int to = e.getTo();
            InputNode nodeFrom = inputNodeMap.get(a.getNode(from));
            InputNode nodeTo = inputNodeMap.get(a.getNode(to));
            char fromIndex = e.getFromIndex();
            char toIndex = e.getToIndex();

            if (nodeFrom == null || nodeTo == null) {
                System.out.println("Unexpected edge : " + from + " -> " + to);
            } else {
                InputEdge newEdge = new InputEdge(fromIndex, toIndex, nodeFrom.getId(), nodeTo.getId(), e.getLabel(), e.getType());
                if (!newEdges.contains(newEdge)) {
                    markAsDeleted(newEdge);
                    newEdges.add(newEdge);
                    graph.addEdge(newEdge);
                }
            }
        }

        for (InputEdge e : edgesB) {
            int from = e.getFrom();
            int to = e.getTo();
            InputNode nodeFrom = inputNodeMap.get(b.getNode(from));
            InputNode nodeTo = inputNodeMap.get(b.getNode(to));
            char fromIndex = e.getFromIndex();
            char toIndex = e.getToIndex();

            if (nodeFrom == null || nodeTo == null) {
                System.out.println("Unexpected edge : " + from + " -> " + to);
            } else {
                InputEdge newEdge = new InputEdge(fromIndex, toIndex, nodeFrom.getId(), nodeTo.getId(), e.getLabel(), e.getType());
                if (!newEdges.contains(newEdge)) {
                    markAsNew(newEdge);
                    newEdges.add(newEdge);
                    graph.addEdge(newEdge);
                } else {
                    newEdges.remove(newEdge);
                    graph.removeEdge(newEdge);
                    markAsSame(newEdge);
                    newEdges.add(newEdge);
                    graph.addEdge(newEdge);
                }
            }
        }

        return graph;
    }

    private static class NodePair extends Pair<InputNode, InputNode> {


        public NodePair(InputNode n1, InputNode n2) {
            super(n1, n2);
        }

        public double getValue() {

            double result = 0.0;
            for (Property p : getLeft().getProperties()) {
                double faktor = 1.0;
                for (String forbidden : IGNORE_PROPERTIES) {
                    if (p.getName().equals(forbidden)) {
                        faktor = 0.1;
                        break;
                    }
                }
                String p2 = getRight().getProperties().get(p.getName());
                result += evaluate(p.getValue(), p2) * faktor;
            }

            return result;
        }

        private double evaluate(String p, String p2) {
            if (p2 == null) {
                return 1.0;
            }
            if (p.equals(p2)) {
                return 0.0;
            } else {
                return (double) (Math.abs(p.length() - p2.length())) / p.length() + 0.5;
            }
        }
    }

    private static InputGraph createDiff(InputGraph a, InputGraph b) {

        Set<InputNode> matched = new HashSet<>();

        Set<NodePair> pairs = new HashSet<>();
        for (InputNode n : a.getNodes()) {
            String s = n.getProperties().get(MAIN_PROPERTY);
            if (s == null) {
                s = "";
            }
            for (InputNode n2 : b.getNodes()) {
                String s2 = n2.getProperties().get(MAIN_PROPERTY);
                if (s2 == null) {
                    s2 = "";
                }

                if (s.equals(s2)) {
                    NodePair p = new NodePair(n, n2);
                    pairs.add(p);
                }
            }
        }

        Set<NodePair> selectedPairs = new HashSet<>();
        while (pairs.size() > 0) {

            double min = Double.MAX_VALUE;
            NodePair minPair = null;
            for (NodePair p : pairs) {
                double cur = p.getValue();
                if (cur < min) {
                    minPair = p;
                    min = cur;
                }
            }

            if (min > LIMIT) {
                break;
            } else {
                selectedPairs.add(minPair);

                Set<NodePair> toRemove = new HashSet<>();
                for (NodePair p : pairs) {
                    if (p.getLeft() == minPair.getLeft() || p.getRight() == minPair.getRight()) {
                        toRemove.add(p);
                    }
                }
                pairs.removeAll(toRemove);
            }
        }

        return createDiff(a, b, selectedPairs);
    }

    private static void markAsNew(InputEdge e) {
        e.setState(InputEdge.State.NEW);
    }

    private static void markAsDeleted(InputEdge e) {
        e.setState(InputEdge.State.DELETED);

    }

    private static void markAsSame(InputEdge e) {
        e.setState(InputEdge.State.SAME);
    }

    private static void markAsChanged(InputNode n, InputNode firstNode, InputNode otherNode) {

        boolean difference = false;
        for (Property p : otherNode.getProperties()) {
            String s = firstNode.getProperties().get(p.getName());
            if (!p.getValue().equals(s)) {
                difference = true;
                n.getProperties().setProperty(NEW_PREFIX + p.getName(), p.getValue());
            }
        }

        for (Property p : firstNode.getProperties()) {
            String s = otherNode.getProperties().get(p.getName());
            if (s == null && p.getValue().length() > 0) {
                difference = true;
                n.getProperties().setProperty(NEW_PREFIX + p.getName(), "");
            }
        }

        if (difference) {
            n.getProperties().setProperty(PROPERTY_STATE, VALUE_CHANGED);
        } else {
            n.getProperties().setProperty(PROPERTY_STATE, VALUE_SAME);
        }
    }

    private static void markAsDeleted(InputNode n) {
        n.getProperties().setProperty(PROPERTY_STATE, VALUE_DELETED);
    }

    private static void markAsNew(InputNode n) {
        n.getProperties().setProperty(PROPERTY_STATE, VALUE_NEW);
    }
}
