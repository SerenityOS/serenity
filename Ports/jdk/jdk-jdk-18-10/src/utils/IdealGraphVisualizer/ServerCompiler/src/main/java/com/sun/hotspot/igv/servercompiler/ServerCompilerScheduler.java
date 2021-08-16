/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.hotspot.igv.servercompiler;

import com.sun.hotspot.igv.data.InputBlock;
import com.sun.hotspot.igv.data.InputEdge;
import com.sun.hotspot.igv.data.InputGraph;
import com.sun.hotspot.igv.data.InputNode;
import com.sun.hotspot.igv.data.services.Scheduler;
import java.util.*;
import org.openide.ErrorManager;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Thomas Wuerthinger
 */
@ServiceProvider(service=Scheduler.class)
public class ServerCompilerScheduler implements Scheduler {

    private static class Node {

        public InputNode inputNode;
        public Set<Node> succs = new HashSet<>();
        public List<Node> preds = new ArrayList<>();
        public InputBlock block;
        public boolean isBlockProjection;
        public boolean isBlockStart;
        public boolean isCFG;
    }
    private InputGraph graph;
    private Collection<Node> nodes;
    private Map<InputNode, Node> inputNodeToNode;
    private Vector<InputBlock> blocks;
    private Map<InputBlock, InputBlock> dominatorMap;
    private Map<InputBlock, Integer> blockIndex;
    private InputBlock[][] commonDominator;
    private static final Comparator<InputEdge> edgeComparator = new Comparator<InputEdge>() {

        @Override
        public int compare(InputEdge o1, InputEdge o2) {
            return o1.getToIndex() - o2.getToIndex();
        }
    };

    public void buildBlocks() {

        // Initialize data structures.
        blocks = new Vector<>();
        Node root = findRoot();
        if (root == null) {
            return;
        }
        Stack<Node> stack = new Stack<>();
        Set<Node> visited = new HashSet<>();
        Map<InputBlock, Set<Node>> terminators = new HashMap<>();
        // Pre-compute control successors of each node, excluding self edges.
        Map<Node, Set<Node>> controlSuccs = new HashMap<>();
        for (Node n : nodes) {
            if (n.isCFG) {
                Set<Node> nControlSuccs = new HashSet<>();
                for (Node s : n.succs) {
                    if (s.isCFG && s != n) {
                        nControlSuccs.add(s);
                    }
                }
                controlSuccs.put(n, nControlSuccs);
            }
        }
        stack.add(root);
        // Start from 1 to follow the style of compiler-generated CFGs.
        int blockCount = 1;
        InputBlock rootBlock = null;

        // Traverse the control-flow subgraph forwards, starting from the root.
        while (!stack.isEmpty()) {
            // Pop a node, mark it as visited, and create a new block.
            Node n = stack.pop();
            if (visited.contains(n)) {
                continue;
            }
            visited.add(n);
            InputBlock block = graph.addBlock(Integer.toString(blockCount));
            blocks.add(block);
            if (n == root) {
                rootBlock = block;
            }
            blockCount++;
            Set<Node> blockTerminators = new HashSet<Node>();
            // Move forwards until a terminator node is found, assigning all
            // visited nodes to the current block.
            while (true) {
                // Assign n to current block.
                n.block = block;
                if (controlSuccs.get(n).size() == 0) {
                    // No successors: end the block.
                    blockTerminators.add(n);
                    break;
                } else if (controlSuccs.get(n).size() == 1) {
                    // One successor: end the block if it is a block start node.
                    Node s = controlSuccs.get(n).iterator().next();
                    if (s.isBlockStart) {
                        // Block start: end the block.
                        blockTerminators.add(n);
                        stack.push(s);
                        break;
                    } else {
                        // Not a block start: keep filling the current block.
                        n = s;
                    }
                } else {
                    // Multiple successors: end the block.
                    for (Node s : controlSuccs.get(n)) {
                        if (s.isBlockProjection && s != root) {
                            // Assign block projections to the current block,
                            // and push their successors to the stack. In the
                            // normal case, we would expect control projections
                            // to have only one successor, but there are some
                            // intermediate graphs (e.g. 'Before RemoveUseless')
                            // where 'IfX' nodes flow both to 'Region' and
                            // (dead) 'Safepoint' nodes.
                            s.block = block;
                            blockTerminators.add(s);
                            for (Node ps : controlSuccs.get(s)) {
                                stack.push(ps);
                            }
                        } else {
                            blockTerminators.add(n);
                            stack.push(s);
                        }
                    }
                    break;
                }
            }
            terminators.put(block, blockTerminators);
        }

        // Add block edges based on terminator successors. Note that a block
        // might have multiple terminators preceding the same successor block.
        for (Map.Entry<InputBlock, Set<Node>> terms : terminators.entrySet()) {
            // Unique set of terminator successors.
            Set<Node> uniqueSuccs = new HashSet<>();
            for (Node t : terms.getValue()) {
                for (Node s : controlSuccs.get(t)) {
                    if (s.block != rootBlock) {
                        uniqueSuccs.add(s);
                    }
                }
            }
            for (Node s : uniqueSuccs) {
                graph.addBlockEdge(terms.getKey(), s.block);
            }
        }

        // Fill the blocks.
        for (Node n : nodes) {
            InputBlock block = n.block;
            if (block != null) {
                block.addNode(n.inputNode.getId());
            }
        }

        // Compute block index map for dominator computation.
        int z = 0;
        blockIndex = new HashMap<>(blocks.size());
        for (InputBlock b : blocks) {
            blockIndex.put(b, z);
            z++;
        }
    }

    private String getBlockName(InputNode n) {
        return n.getProperties().get("block");
    }

    @Override
    public Collection<InputBlock> schedule(InputGraph graph) {
        if (graph.getNodes().isEmpty()) {
            return Collections.emptyList();
        }

        if (graph.getBlocks().size() > 0) {
            Collection<InputNode> tmpNodes = new ArrayList<>(graph.getNodes());
            for (InputNode n : tmpNodes) {
                String block = getBlockName(n);
                if (graph.getBlock(n) == null) {
                    graph.getBlock(block).addNode(n.getId());
                    assert graph.getBlock(n) != null;
                }
            }
            return graph.getBlocks();
        } else {
            nodes = new ArrayList<>();
            inputNodeToNode = new HashMap<>(graph.getNodes().size());

            this.graph = graph;
            if (!hasCategoryInformation()) {
                ErrorManager.getDefault().log(ErrorManager.WARNING,
                    "Cannot find node category information in the input graph. " +
                    "The control-flow graph will not be approximated.");
                return null;
            }
            buildUpGraph();
            markCFGNodes();
            connectOrphansAndWidows();
            buildBlocks();
            buildDominators();
            buildCommonDominators();
            scheduleLatest();

            InputBlock noBlock = null;
            for (InputNode n : graph.getNodes()) {
                if (graph.getBlock(n) == null) {
                    if (noBlock == null) {
                        noBlock = graph.addBlock("(no block)");
                        blocks.add(noBlock);
                    }

                    graph.setBlock(n, noBlock);
                }
                assert graph.getBlock(n) != null;
            }

            return blocks;
        }
    }

    private void scheduleLatest() {
        Node root = findRoot();
        if(root == null) {
            assert false : "No root found!";
            return;
        }

        // Mark all nodes reachable in backward traversal from root
        Set<Node> reachable = new HashSet<>();
        reachable.add(root);
        Stack<Node> stack = new Stack<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Node cur = stack.pop();
            for (Node n : cur.preds) {
                if (!reachable.contains(n)) {
                    reachable.add(n);
                    stack.push(n);
                }
            }
        }

        Set<Node> unscheduled = new HashSet<>();
        for (Node n : this.nodes) {
            if (n.block == null && reachable.contains(n)) {
                unscheduled.add(n);
            }
        }

        while (unscheduled.size() > 0) {
            boolean progress = false;

            Set<Node> newUnscheduled = new HashSet<>();
            for (Node n : unscheduled) {

                InputBlock block = null;
                if (this.isPhi(n) && n.preds.get(0) != null) {
                    // Phi nodes in same block as region nodes
                    block = n.preds.get(0).block;
                } else {
                    for (Node s : n.succs) {
                        if (reachable.contains(s)) {
                            if (s.block == null) {
                                block = null;
                                break;
                            } else {
                                if (block == null) {
                                    block = s.block;
                                } else {
                                    block = commonDominator[this.blockIndex.get(block)][blockIndex.get(s.block)];
                                }
                            }
                        }
                    }
                }

                if (block != null) {
                    n.block = block;
                    block.addNode(n.inputNode.getId());
                    progress = true;
                } else {
                    newUnscheduled.add(n);
                }
            }

            unscheduled = newUnscheduled;

            if (!progress) {
                break;
            }
        }

        Set<Node> curReachable = new HashSet<>(reachable);
        for (Node n : curReachable) {
            if (n.block != null) {
                for (Node s : n.succs) {
                    if (!reachable.contains(s)) {
                        markWithBlock(s, n.block, reachable);
                    }
                }
            }
        }

    }

    private void markWithBlock(Node n, InputBlock b, Set<Node> reachable) {
        assert !reachable.contains(n);
        Stack<Node> stack = new Stack<>();
        stack.push(n);
        n.block = b;
        b.addNode(n.inputNode.getId());
        reachable.add(n);

        while (!stack.isEmpty()) {
            Node cur = stack.pop();
            for (Node s : cur.succs) {
                if (!reachable.contains(s)) {
                    reachable.add(s);
                    s.block = b;
                    b.addNode(s.inputNode.getId());
                    stack.push(s);
                }
            }

            for (Node s : cur.preds) {
                if (!reachable.contains(s)) {
                    reachable.add(s);
                    s.block = b;
                    b.addNode(s.inputNode.getId());
                    stack.push(s);
                }
            }
        }
    }

    private class BlockIntermediate {

        InputBlock block;
        int index;
        int dominator;
        int semi;
        int parent;
        int label;
        int ancestor;
        List<Integer> pred;
        List<Integer> bucket;
    }

    public void buildCommonDominators() {
        commonDominator = new InputBlock[this.blocks.size()][this.blocks.size()];
        for (int i = 0; i < blocks.size(); i++) {
            for (int j = 0; j < blocks.size(); j++) {
                commonDominator[i][j] = getCommonDominator(i, j);
            }
        }
    }

    public InputBlock getCommonDominator(int a, int b) {
        InputBlock ba = blocks.get(a);
        InputBlock bb = blocks.get(b);
        if (ba == bb) {
            return ba;
        }
        Set<InputBlock> visited = new HashSet<>();
        while (ba != null) {
            visited.add(ba);
            ba = dominatorMap.get(ba);
        }

        while (bb != null) {
            if (visited.contains(bb)) {
                return bb;
            }
            bb = dominatorMap.get(bb);
        }

        assert false;
        return null;
    }

    public void buildDominators() {
        dominatorMap = new HashMap<>(graph.getBlocks().size());
        if (blocks.size() == 0) {
            return;
        }
        Vector<BlockIntermediate> intermediate = new Vector<>(graph.getBlocks().size());
        Map<InputBlock, BlockIntermediate> map = new HashMap<>(graph.getBlocks().size());
        int z = 0;
        for (InputBlock b : blocks) {
            BlockIntermediate bi = new BlockIntermediate();
            bi.block = b;
            bi.index = z;
            bi.dominator = -1;
            bi.semi = -1;
            bi.parent = -1;
            bi.label = z;
            bi.ancestor = -1;
            bi.pred = new ArrayList<>();
            bi.bucket = new ArrayList<>();
            intermediate.add(bi);
            map.put(b, bi);
            z++;
        }
        Stack<Integer> stack = new Stack<>();
        stack.add(0);

        Vector<BlockIntermediate> array = new Vector<>();
        intermediate.get(0).dominator = 0;

        int n = 0;
        while (!stack.isEmpty()) {
            int index = stack.pop();
            BlockIntermediate ib = intermediate.get(index);
            ib.semi = n;
            array.add(ib);
            n = n + 1;
            for (InputBlock b : ib.block.getSuccessors()) {
                BlockIntermediate succ = map.get(b);
                if (succ.semi == -1) {
                    succ.parent = index;
                    stack.push(succ.index); // TODO: check if same node could be pushed twice
                }
                succ.pred.add(index);
            }
        }

        for (int i = n - 1; i > 0; i--) {
            BlockIntermediate block = array.get(i);
            int block_index = block.index;
            for (int predIndex : block.pred) {
                int curIndex = eval(predIndex, intermediate);
                BlockIntermediate curBlock = intermediate.get(curIndex);
                if (curBlock.semi < block.semi) {
                    block.semi = curBlock.semi;
                }
            }


            int semiIndex = block.semi;
            BlockIntermediate semiBlock = array.get(semiIndex);
            semiBlock.bucket.add(block_index);

            link(block.parent, block_index, intermediate);
            BlockIntermediate parentBlock = intermediate.get(block.parent);

            for (int j = 0; j < parentBlock.bucket.size(); j++) {
                for (int curIndex : parentBlock.bucket) {
                    int newIndex = eval(curIndex, intermediate);
                    BlockIntermediate curBlock = intermediate.get(curIndex);
                    BlockIntermediate newBlock = intermediate.get(newIndex);
                    int dom = block.parent;
                    if (newBlock.semi < curBlock.semi) {
                        dom = newIndex;
                    }

                    curBlock.dominator = dom;
                }
            }


            parentBlock.bucket.clear();
        }

        for (int i = 1; i < n; i++) {

            BlockIntermediate block = array.get(i);
            int block_index = block.index;

            int semi_index = block.semi;
            BlockIntermediate semi_block = array.get(semi_index);

            if (block.dominator != semi_block.index) {
                int new_dom = intermediate.get(block.dominator).dominator;
                block.dominator = new_dom;
            }
        }

        for (BlockIntermediate ib : intermediate) {
            if (ib.dominator == -1) {
                ib.dominator = 0;
            }
        }

        for (BlockIntermediate bi : intermediate) {
            InputBlock b = bi.block;
            int dominator = bi.dominator;
            InputBlock dominatorBlock = null;
            if (dominator != -1) {
                dominatorBlock = intermediate.get(dominator).block;
            }

            if (dominatorBlock == b) {
                dominatorBlock = null;
            }
            this.dominatorMap.put(b, dominatorBlock);
        }
    }

    private void compress(int index, Vector<BlockIntermediate> blocks) {
        BlockIntermediate block = blocks.get(index);

        int ancestor = block.ancestor;
        assert ancestor != -1;

        BlockIntermediate ancestor_block = blocks.get(ancestor);
        if (ancestor_block.ancestor != -1) {
            compress(ancestor, blocks);

            int label = block.label;
            BlockIntermediate label_block = blocks.get(label);

            int ancestor_label = ancestor_block.label;
            BlockIntermediate ancestor_label_block = blocks.get(label);
            if (ancestor_label_block.semi < label_block.semi) {
                block.label = ancestor_label;
            }

            block.ancestor = ancestor_block.ancestor;
        }
    }

    private int eval(int index, Vector<BlockIntermediate> blocks) {
        BlockIntermediate block = blocks.get(index);
        if (block.ancestor == -1) {
            return index;
        } else {
            compress(index, blocks);
            return block.label;
        }
    }

    private void link(int index1, int index2, Vector<BlockIntermediate> blocks) {
        BlockIntermediate block2 = blocks.get(index2);
        block2.ancestor = index1;
    }

    private boolean isRegion(Node n) {
        return n.inputNode.getProperties().get("name").equals("Region");
    }

    private boolean isPhi(Node n) {
        return n.inputNode.getProperties().get("name").equals("Phi");
    }

    private Node findRoot() {
        Node minNode = null;
        Node alternativeRoot = null;

        for (Node node : nodes) {
            InputNode inputNode = node.inputNode;
            String s = inputNode.getProperties().get("name");
            if (s != null && s.equals("Root")) {
                return node;
            }

            if (alternativeRoot == null && node.preds.isEmpty()) {
                alternativeRoot = node;
            }

            if (minNode == null || node.inputNode.getId() < minNode.inputNode.getId()) {
                minNode = node;
            }
        }

        if (alternativeRoot != null) {
            return alternativeRoot;
        } else {
            return minNode;
        }
    }

    public boolean hasCategoryInformation() {
        for (InputNode n : graph.getNodes()) {
            if (n.getProperties().get("category") == null) {
                return false;
            }
        }
        return true;
    }

    public void buildUpGraph() {

        for (InputNode n : graph.getNodes()) {
            Node node = new Node();
            node.inputNode = n;
            nodes.add(node);
            String p = n.getProperties().get("is_block_proj");
            node.isBlockProjection = (p != null && p.equals("true"));
            p = n.getProperties().get("is_block_start");
            node.isBlockStart = (p != null && p.equals("true"));
            inputNodeToNode.put(n, node);
        }

        Map<Integer, List<InputEdge>> edgeMap = new HashMap<>(graph.getEdges().size());
        for (InputEdge e : graph.getEdges()) {

            int to = e.getTo();
            if (!edgeMap.containsKey(to)) {
                edgeMap.put(to, new ArrayList<InputEdge>());
            }


            List<InputEdge> list = edgeMap.get(to);
            list.add(e);
        }


        for (Integer i : edgeMap.keySet()) {

            List<InputEdge> list = edgeMap.get(i);
            Collections.sort(list, edgeComparator);

            int to = i;
            InputNode toInputNode = graph.getNode(to);
            Node toNode = inputNodeToNode.get(toInputNode);
            for (InputEdge e : list) {
                assert to == e.getTo();
                int from = e.getFrom();
                InputNode fromInputNode = graph.getNode(from);
                Node fromNode = inputNodeToNode.get(fromInputNode);
                fromNode.succs.add(toNode);
                toNode.preds.add(fromNode);
            }
        }
    }

    // Mark nodes that form the CFG (same as shown by the 'Show control flow
    // only' filter, plus the Root node).
    public void markCFGNodes() {
        for (Node n : nodes) {
            String category = n.inputNode.getProperties().get("category");
            if (category.equals("control") || category.equals("mixed")) {
                // Example: If, IfTrue, CallStaticJava.
                n.isCFG = true;
            } else if (n.inputNode.getProperties().get("type").equals("bottom")
                       && n.preds.size() > 0 &&
                       n.preds.get(0) != null &&
                       n.preds.get(0).inputNode.getProperties()
                       .get("category").equals("control")) {
                // Example: Halt, Return, Rethrow.
                n.isCFG = true;
            } else if (n.isBlockStart || n.isBlockProjection) {
                // Example: Root.
                n.isCFG = true;
            } else {
                n.isCFG = false;
            }
        }
    }

    // Fix ill-formed graphs with orphan/widow control-flow nodes by adding
    // edges from/to the Root node. Such edges are assumed by different parts of
    // the scheduling algorithm, but are not always present, e.g. for certain
    // 'Safepoint' nodes in the 'Before RemoveUseless' phase.
    public void connectOrphansAndWidows() {
        Node root = findRoot();
        if (root == null) {
            return;
        }
        for (Node n : nodes) {
            if (n.isCFG) {
                boolean orphan = true;
                for (Node p : n.preds) {
                    if (p != n && p.isCFG) {
                        orphan = false;
                    }
                }
                if (orphan) {
                    // Add edge from root to this node.
                    root.succs.add(n);
                    n.preds.add(0, root);
                }
                boolean widow = true;
                for (Node s : n.succs) {
                    if (s != n && s.isCFG) {
                        widow = false;
                    }
                }
                if (widow) {
                    // Add edge from this node to root.
                    root.preds.add(n);
                    n.succs.add(root);
                }
            }
        }
    }
}
