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

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;
import com.sun.hotspot.igv.layout.Cluster;
import com.sun.hotspot.igv.layout.LayoutGraph;
import com.sun.hotspot.igv.layout.LayoutManager;
import com.sun.hotspot.igv.layout.Link;
import com.sun.hotspot.igv.layout.Port;
import com.sun.hotspot.igv.layout.Vertex;

/**
 *
 * @author Thomas Wuerthinger
 */
public class HierarchicalClusterLayoutManager implements LayoutManager {

    private HierarchicalLayoutManager.Combine combine;
    private LayoutManager subManager = new HierarchicalLayoutManager(combine);
    private LayoutManager manager = new HierarchicalLayoutManager(combine);
    private static final boolean TRACE = false;

    public HierarchicalClusterLayoutManager(HierarchicalLayoutManager.Combine combine) {
        this.combine = combine;
    }

    public void doLayout(LayoutGraph graph) {
        doLayout(graph, new HashSet<Vertex>(), new HashSet<Vertex>(), new HashSet<Link>());
    }

    public void doLayout(LayoutGraph graph, Set<? extends Link> importantLinks) {
        doLayout(graph);
    }

    public void setSubManager(LayoutManager manager) {
        this.subManager = manager;
    }

    public void setManager(LayoutManager manager) {
        this.manager = manager;
    }

    public void doLayout(LayoutGraph graph, Set<? extends Vertex> firstLayerHint, Set<? extends Vertex> lastLayerHint, Set<? extends Link> importantLinks) {

        assert graph.verify();

        HashMap<Cluster, List<Vertex>> lists = new HashMap<Cluster, List<Vertex>>();
        HashMap<Cluster, List<Link>> listsConnection = new HashMap<Cluster, List<Link>>();
        HashMap<Cluster, HashMap<Port, ClusterInputSlotNode>> clusterInputSlotHash = new HashMap<Cluster, HashMap<Port, ClusterInputSlotNode>>();
        HashMap<Cluster, HashMap<Port, ClusterOutputSlotNode>> clusterOutputSlotHash = new HashMap<Cluster, HashMap<Port, ClusterOutputSlotNode>>();

        HashMap<Cluster, ClusterNode> clusterNodes = new HashMap<Cluster, ClusterNode>();
        HashMap<Cluster, Set<ClusterInputSlotNode>> clusterInputSlotSet = new HashMap<Cluster, Set<ClusterInputSlotNode>>();
        HashMap<Cluster, Set<ClusterOutputSlotNode>> clusterOutputSlotSet = new HashMap<Cluster, Set<ClusterOutputSlotNode>>();
        Set<Link> clusterEdges = new HashSet<Link>();
        Set<Link> interClusterEdges = new HashSet<Link>();
        HashMap<Link, ClusterOutgoingConnection> linkClusterOutgoingConnection = new HashMap<Link, ClusterOutgoingConnection>();
        HashMap<Link, InterClusterConnection> linkInterClusterConnection = new HashMap<Link, InterClusterConnection>();
        HashMap<Link, ClusterIngoingConnection> linkClusterIngoingConnection = new HashMap<Link, ClusterIngoingConnection>();
        Set<ClusterNode> clusterNodeSet = new HashSet<ClusterNode>();

        Set<Cluster> cluster = graph.getClusters();
        int z = 0;
        for (Cluster c : cluster) {
            lists.put(c, new ArrayList<Vertex>());
            listsConnection.put(c, new ArrayList<Link>());
            clusterInputSlotHash.put(c, new HashMap<Port, ClusterInputSlotNode>());
            clusterOutputSlotHash.put(c, new HashMap<Port, ClusterOutputSlotNode>());
            clusterOutputSlotSet.put(c, new TreeSet<ClusterOutputSlotNode>());
            clusterInputSlotSet.put(c, new TreeSet<ClusterInputSlotNode>());
            ClusterNode cn = new ClusterNode(c, "" + z);
            clusterNodes.put(c, cn);
            clusterNodeSet.add(cn);
            z++;
        }

        // Add cluster edges
        for (Cluster c : cluster) {

            ClusterNode start = clusterNodes.get(c);

            for (Cluster succ : c.getSuccessors()) {
                ClusterNode end = clusterNodes.get(succ);
                if (end != null && start != end) {
                    ClusterEdge e = new ClusterEdge(start, end);
                    clusterEdges.add(e);
                    interClusterEdges.add(e);
                }
            }
        }

        for (Vertex v : graph.getVertices()) {
            Cluster c = v.getCluster();
            assert c != null : "Cluster of vertex " + v + " is null!";
            clusterNodes.get(c).addSubNode(v);
        }

        for (Link l : graph.getLinks()) {

            Port fromPort = l.getFrom();
            Port toPort = l.getTo();
            Vertex fromVertex = fromPort.getVertex();
            Vertex toVertex = toPort.getVertex();
            Cluster fromCluster = fromVertex.getCluster();
            Cluster toCluster = toVertex.getCluster();

            Port samePort = null;
            if (combine == HierarchicalLayoutManager.Combine.SAME_INPUTS) {
                samePort = toPort;
            } else if (combine == HierarchicalLayoutManager.Combine.SAME_OUTPUTS) {
                samePort = fromPort;
            }

            assert listsConnection.containsKey(fromCluster);
            assert listsConnection.containsKey(toCluster);

            if (fromCluster == toCluster) {
                listsConnection.get(fromCluster).add(l);
                clusterNodes.get(fromCluster).addSubEdge(l);
            } else {
                ClusterInputSlotNode inputSlotNode = null;
                ClusterOutputSlotNode outputSlotNode = null;

                if (samePort != null) {
                    outputSlotNode = clusterOutputSlotHash.get(fromCluster).get(samePort);
                    inputSlotNode = clusterInputSlotHash.get(toCluster).get(samePort);
                }

                if (outputSlotNode == null) {
                    outputSlotNode = new ClusterOutputSlotNode(clusterNodes.get(fromCluster), "Out " + fromCluster.toString() + " " + samePort.toString());
                    clusterOutputSlotSet.get(fromCluster).add(outputSlotNode);
                    ClusterOutgoingConnection conn = new ClusterOutgoingConnection(outputSlotNode, l);
                    outputSlotNode.setOutgoingConnection(conn);
                    clusterNodes.get(fromCluster).addSubEdge(conn);
                    if (samePort != null) {
                        clusterOutputSlotHash.get(fromCluster).put(samePort, outputSlotNode);
                    }

                    linkClusterOutgoingConnection.put(l, conn);
                } else {
                    linkClusterOutgoingConnection.put(l, outputSlotNode.getOutgoingConnection());
                }

                if (inputSlotNode == null) {
                    inputSlotNode = new ClusterInputSlotNode(clusterNodes.get(toCluster), "In " + toCluster.toString() + " " + samePort.toString());
                    clusterInputSlotSet.get(toCluster).add(inputSlotNode);
                }

                ClusterIngoingConnection conn = new ClusterIngoingConnection(inputSlotNode, l);
                inputSlotNode.setIngoingConnection(conn);
                clusterNodes.get(toCluster).addSubEdge(conn);
                if (samePort != null) {
                    clusterInputSlotHash.get(toCluster).put(samePort, inputSlotNode);
                }

                linkClusterIngoingConnection.put(l, conn);


                InterClusterConnection interConn = new InterClusterConnection(outputSlotNode, inputSlotNode);
                linkInterClusterConnection.put(l, interConn);
                clusterEdges.add(interConn);
            }
        }

        Timing t = null;

        if (TRACE) {
            new Timing("Child timing");
            t.start();
        }

        for (Cluster c : cluster) {
            ClusterNode n = clusterNodes.get(c);
            subManager.doLayout(new LayoutGraph(n.getSubEdges(), n.getSubNodes()), new HashSet<Link>());
            n.updateSize();
        }

        Set<Vertex> roots = new LayoutGraph(interClusterEdges).findRootVertices();
        for (Vertex v : roots) {
            assert v instanceof ClusterNode;
            ((ClusterNode) v).setRoot(true);
        }

        manager.doLayout(new LayoutGraph(clusterEdges, clusterNodeSet), interClusterEdges);

        for (Cluster c : cluster) {
            ClusterNode n = clusterNodes.get(c);
            c.setBounds(new Rectangle(n.getPosition(), n.getSize()));
        }

        // TODO: handle case where blocks are not fully connected

        if (TRACE) {
            t.stop();
            t.print();
        }

        for (Link l : graph.getLinks()) {

            if (linkInterClusterConnection.containsKey(l)) {
                ClusterOutgoingConnection conn1 = linkClusterOutgoingConnection.get(l);
                InterClusterConnection conn2 = linkInterClusterConnection.get(l);
                ClusterIngoingConnection conn3 = linkClusterIngoingConnection.get(l);

                assert conn1 != null;
                assert conn2 != null;
                assert conn3 != null;

                List<Point> points = new ArrayList<Point>();

                points.addAll(conn1.getControlPoints());
                points.addAll(conn2.getControlPoints());
                points.addAll(conn3.getControlPoints());

                l.setControlPoints(points);
            }
        }
    }

    public void doRouting(LayoutGraph graph) {
    }
}
