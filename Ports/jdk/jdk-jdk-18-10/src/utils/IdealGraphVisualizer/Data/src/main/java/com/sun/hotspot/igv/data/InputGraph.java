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
package com.sun.hotspot.igv.data;

import java.util.*;

/**
 *
 * @author Thomas Wuerthinger
 */
public class InputGraph extends Properties.Entity implements FolderElement {

    private Map<Integer, InputNode> nodes;
    private List<InputEdge> edges;
    private Folder parent;
    private Group parentGroup;
    private Map<String, InputBlock> blocks;
    private List<InputBlockEdge> blockEdges;
    private Map<Integer, InputBlock> nodeToBlock;

    public InputGraph(String name) {
        setName(name);
        nodes = new LinkedHashMap<>();
        edges = new ArrayList<>();
        blocks = new LinkedHashMap<>();
        blockEdges = new ArrayList<>();
        nodeToBlock = new LinkedHashMap<>();
    }

    @Override
    public void setParent(Folder parent) {
        this.parent = parent;
        if (parent instanceof Group) {
            assert this.parentGroup == null;
            this.parentGroup = (Group) parent;
        }
    }

    public InputBlockEdge addBlockEdge(InputBlock left, InputBlock right) {
        InputBlockEdge edge = new InputBlockEdge(left, right);
        blockEdges.add(edge);
        left.addSuccessor(right);
        return edge;
    }

    public List<InputNode> findRootNodes() {
        List<InputNode> result = new ArrayList<>();
        Set<Integer> nonRoot = new HashSet<>();
        for(InputEdge curEdges : getEdges()) {
            nonRoot.add(curEdges.getTo());
        }

        for(InputNode node : getNodes()) {
            if(!nonRoot.contains(node.getId())) {
                result.add(node);
            }
        }

        return result;
    }

    public Map<InputNode, List<InputEdge>> findAllOutgoingEdges() {
        Map<InputNode, List<InputEdge>> result = new HashMap<>(getNodes().size());
        for(InputNode n : this.getNodes()) {
            result.put(n, new ArrayList<InputEdge>());
        }

        for(InputEdge e : this.edges) {
            int from = e.getFrom();
            InputNode fromNode = this.getNode(from);
            List<InputEdge> fromList = result.get(fromNode);
            assert fromList != null;
            fromList.add(e);
        }

        for(InputNode n : this.getNodes()) {
            List<InputEdge> list = result.get(n);
            Collections.sort(list, InputEdge.OUTGOING_COMPARATOR);
        }

        return result;
    }

    public Map<InputNode, List<InputEdge>> findAllIngoingEdges() {
        Map<InputNode, List<InputEdge>> result = new HashMap<>(getNodes().size());
        for(InputNode n : this.getNodes()) {
            result.put(n, new ArrayList<InputEdge>());
        }

        for(InputEdge e : this.edges) {
            int to = e.getTo();
            InputNode toNode = this.getNode(to);
            List<InputEdge> toList = result.get(toNode);
            assert toList != null;
            toList.add(e);
        }

        for(InputNode n : this.getNodes()) {
            List<InputEdge> list = result.get(n);
            Collections.sort(list, InputEdge.INGOING_COMPARATOR);
        }

        return result;
    }

    public List<InputEdge> findOutgoingEdges(InputNode n) {
        List<InputEdge> result = new ArrayList<>();

        for(InputEdge e : this.edges) {
            if(e.getFrom() == n.getId()) {
                result.add(e);
            }
        }

        Collections.sort(result, InputEdge.OUTGOING_COMPARATOR);

        return result;
    }

    public void clearBlocks() {
        blocks.clear();
        nodeToBlock.clear();
    }

    public void setEdge(int fromIndex, int toIndex, int from, int to) {
        assert fromIndex == ((char)fromIndex) : "Downcast must be safe";
        assert toIndex == ((char)toIndex) : "Downcast must be safe";

        InputEdge edge = new InputEdge((char)fromIndex, (char)toIndex, from, to);
        if(!this.getEdges().contains(edge)) {
            this.addEdge(edge);
        }
    }

    public void ensureNodesInBlocks() {
        InputBlock noBlock = null;
        Set<InputNode> scheduledNodes = new HashSet<>();

        for (InputBlock b : getBlocks()) {
            for (InputNode n : b.getNodes()) {
                assert !scheduledNodes.contains(n);
                scheduledNodes.add(n);
            }
        }

        for (InputNode n : this.getNodes()) {
            assert nodes.get(n.getId()) == n;
            if (!scheduledNodes.contains(n)) {
                if (noBlock == null) {
                    noBlock = this.addBlock("(no block)");
                }
                noBlock.addNode(n.getId());
            }
            assert this.getBlock(n) != null;
        }
    }

    public void setBlock(InputNode node, InputBlock block) {
        nodeToBlock.put(node.getId(), block);
    }

    public InputBlock getBlock(int nodeId) {
        return nodeToBlock.get(nodeId);
    }

    public InputBlock getBlock(InputNode node) {
        assert nodes.containsKey(node.getId());
        assert nodes.get(node.getId()).equals(node);
        return getBlock(node.getId());
    }

    public InputGraph getNext() {
        return parentGroup.getNext(this);
    }

    public InputGraph getPrev() {
        return parentGroup.getPrev(this);
    }

    private void setName(String name) {
        this.getProperties().setProperty("name", name);
    }

    @Override
    public String getName() {
        return getProperties().get("name");
    }

    public Collection<InputNode> getNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public Set<Integer> getNodesAsSet() {
        return Collections.unmodifiableSet(nodes.keySet());
    }

    public Collection<InputBlock> getBlocks() {
        return Collections.unmodifiableCollection(blocks.values());
    }

    public void addNode(InputNode node) {
        nodes.put(node.getId(), node);
    }

    public InputNode getNode(int id) {
        return nodes.get(id);
    }

    public InputNode removeNode(int index) {
        return nodes.remove(index);
    }

    public Collection<InputEdge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public void removeEdge(InputEdge c) {
        boolean removed = edges.remove(c);
        assert removed;
    }

    public void addEdge(InputEdge c) {
        edges.add(c);
    }

    public Group getGroup() {
        return parentGroup;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Graph ").append(getName()).append(" ").append(getProperties().toString()).append("\n");
        for (InputNode n : nodes.values()) {
            sb.append(n.toString());
            sb.append("\n");
        }

        for (InputEdge c : edges) {
            sb.append(c.toString());
            sb.append("\n");
        }

        for (InputBlock b : getBlocks()) {
            sb.append(b.toString());
            sb.append("\n");
        }

        return sb.toString();
    }

    public InputBlock addBlock(String name) {
        final InputBlock b = new InputBlock(this, name);
        blocks.put(b.getName(), b);
        return b;
    }

    public InputBlock getBlock(String s) {
        return blocks.get(s);
    }

    public Collection<InputBlockEdge> getBlockEdges() {
        return Collections.unmodifiableList(blockEdges);
    }

    @Override
    public Folder getParent() {
        return parent;
    }
}
