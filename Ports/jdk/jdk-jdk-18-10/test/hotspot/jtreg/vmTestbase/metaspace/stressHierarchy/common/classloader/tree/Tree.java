/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
package metaspace.stressHierarchy.common.classloader.tree;

import java.util.*;

import metaspace.stressHierarchy.common.classloader.StressClassloader;
import metaspace.stressHierarchy.common.generateHierarchy.NodeDescriptor;


/**
 * Tree of hierarchy.
 *
 * The tree consists of {@link Nodes}s. Each node contains classloader {@link metaspace.stressHierarchy.common.classloader.StressClassloader} that is
 * associated with one class (or interface) and can load only it. Edge in this
 * tree correspond to parent relation between corresponding classloaders.
 * Each classloader delegates to parent only after failed attempt to load class itself.
 *
 */
public class Tree {

    private final List<Node> allNodes = new LinkedList<Node>(); //order matters

    private int maxLevel;

    private Node root;

    public void cleanupLevel(int level) {
        for (Node node : getNodesInLevel(level)) {
            node.cleanup();
        }
    }

    public int getMaxLevel() { return maxLevel; }

    public List<Node> getNodes() {
        return allNodes;
    }

    public List<Node> getNodesInLevel(int level) {
        List<Node> result = new LinkedList<Node>();
        for (Iterator<Node> iter = allNodes.iterator(); iter.hasNext();) {
            Node current = iter.next();
            if (current.getLevel() == level) {
                result.add(current);
            }
        }
        return result;
    }

    private Node findParent(Node node) {
        for (Iterator<Node> iter = allNodes.iterator(); iter.hasNext();) {
            Node current = iter.next();
            if (current.equals(node)) {
                return current;
            }
        }
        return null;
    }

    public void addNode(NodeDescriptor nodeDescriptor) {
        if (nodeDescriptor.level == 0) {
            root = new Node(0, 0);
            root.setClassLoader(new StressClassloader(nodeDescriptor, null));
            allNodes.add(root);
            return;
        }
        Node newOne = new Node(nodeDescriptor.level, nodeDescriptor.index);
        Node parent = findParent(new Node(nodeDescriptor.level - 1, nodeDescriptor.parentIndex));

        //add a payload to new node
        newOne.setClassLoader(new StressClassloader(nodeDescriptor, parent.getClassLoader()));

        newOne.setParent(parent);
        allNodes.add(newOne);
        maxLevel = maxLevel < newOne.getLevel() ? newOne.getLevel() : maxLevel;
    }

}
