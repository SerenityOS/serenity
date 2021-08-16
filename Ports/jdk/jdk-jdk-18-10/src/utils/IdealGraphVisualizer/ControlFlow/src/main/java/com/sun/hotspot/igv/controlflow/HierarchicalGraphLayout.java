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
package com.sun.hotspot.igv.controlflow;

import com.sun.hotspot.igv.hierarchicallayout.HierarchicalLayoutManager;
import com.sun.hotspot.igv.layout.Cluster;
import com.sun.hotspot.igv.layout.LayoutGraph;
import com.sun.hotspot.igv.layout.Link;
import com.sun.hotspot.igv.layout.Port;
import com.sun.hotspot.igv.layout.Vertex;
import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.netbeans.api.visual.graph.layout.GraphLayout;
import org.netbeans.api.visual.graph.layout.UniversalGraph;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Thomas Wuerthinger
 */
public class HierarchicalGraphLayout<N, E> extends GraphLayout<N, E> {

    public HierarchicalGraphLayout() {
    }

    private class LinkWrapper implements Link {

        private VertexWrapper from;
        private VertexWrapper to;

        public LinkWrapper(VertexWrapper from, VertexWrapper to) {
            this.from = from;
            this.to = to;
        }

        public Port getFrom() {
            return from.getSlot();
        }

        public Port getTo() {
            return to.getSlot();
        }

        public List<Point> getControlPoints() {
            return new ArrayList<Point>();
        }

        public void setControlPoints(List<Point> list) {
        // Do nothing for now
        }

        public boolean isVIP() {
            return false;
        }
    }

    private class VertexWrapper implements Vertex {

        private N node;
        private UniversalGraph<N, E> graph;
        private Port slot;
        private Point position;

        public VertexWrapper(N node, UniversalGraph<N, E> graph) {
            this.node = node;
            this.graph = graph;
            final VertexWrapper vertex = this;
            this.slot = new Port() {

                public Vertex getVertex() {
                    return vertex;
                }

                public Point getRelativePosition() {
                    return new Point((int) (vertex.getSize().getWidth() / 2), (int) (vertex.getSize().getHeight() / 2));
                }
            };

            Widget w = graph.getScene().findWidget(node);
            this.position = w.getPreferredLocation();
        }

        public Cluster getCluster() {
            return null;
        }

        public Dimension getSize() {
            Widget w = graph.getScene().findWidget(node);
            return w.getBounds().getSize();
        }

        public Point getPosition() {
            return position;
        }

        public void setPosition(Point p) {
            HierarchicalGraphLayout.this.setResolvedNodeLocation(graph, node, p);
            position = p;
        }

        public boolean isRoot() {
            return false;
        }

        public int compareTo(Vertex o) {
            @SuppressWarnings("unchecked")
            VertexWrapper vw = (VertexWrapper) o;
            return node.toString().compareTo(vw.node.toString());
        }

        public Port getSlot() {
            return slot;
        }
    }

    protected void performGraphLayout(UniversalGraph<N, E> graph) {

        Set<LinkWrapper> links = new LinkedHashSet<LinkWrapper>();
        Set<VertexWrapper> vertices = new LinkedHashSet<VertexWrapper>();
        Map<N, VertexWrapper> vertexMap = new HashMap<N, VertexWrapper>();

        for (N node : graph.getNodes()) {
            VertexWrapper v = new VertexWrapper(node, graph);
            vertexMap.put(node, v);
            vertices.add(v);
        }

        for (E edge : graph.getEdges()) {
            N source = graph.getEdgeSource(edge);
            N target = graph.getEdgeTarget(edge);
            LinkWrapper l = new LinkWrapper(vertexMap.get(source), vertexMap.get(target));
            links.add(l);
        }

        HierarchicalLayoutManager m = new HierarchicalLayoutManager(HierarchicalLayoutManager.Combine.NONE);

        LayoutGraph layoutGraph = new LayoutGraph(links, vertices);
        m.doLayout(layoutGraph);
    }

    protected void performNodesLayout(UniversalGraph<N, E> graph, Collection<N> nodes) {
        throw new UnsupportedOperationException();
    }
}
