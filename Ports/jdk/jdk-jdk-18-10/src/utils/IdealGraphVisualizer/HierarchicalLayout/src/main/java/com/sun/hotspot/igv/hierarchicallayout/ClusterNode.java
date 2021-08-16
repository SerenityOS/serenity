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

import com.sun.hotspot.igv.layout.Cluster;
import com.sun.hotspot.igv.layout.Link;
import com.sun.hotspot.igv.layout.Port;
import com.sun.hotspot.igv.layout.Vertex;
import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Thomas Wuerthinger
 */
public class ClusterNode implements Vertex {

    private Cluster cluster;
    private Port inputSlot;
    private Port outputSlot;
    private Set<Vertex> subNodes;
    private Dimension size;
    private Point position;
    private Set<Link> subEdges;
    private boolean dirty;
    private boolean root;
    private String name;
    public static final int BORDER = 20;

    public ClusterNode(Cluster cluster, String name) {
        this.subNodes = new HashSet<Vertex>();
        this.subEdges = new HashSet<Link>();
        this.cluster = cluster;
        position = new Point(0, 0);
        this.name = name;
    }

    public void addSubNode(Vertex v) {
        subNodes.add(v);
    }

    public void addSubEdge(Link l) {
        subEdges.add(l);
    }

    public Set<Link> getSubEdges() {
        return Collections.unmodifiableSet(subEdges);
    }

    public void updateSize() {


        calculateSize();

        final ClusterNode widget = this;
        inputSlot = new Port() {

            public Point getRelativePosition() {
                return new Point(size.width / 2, 0);
            }

            public Vertex getVertex() {
                return widget;
            }
        };

        outputSlot = new Port() {

            public Point getRelativePosition() {
                return new Point(size.width / 2, 0);//size.height);
            }

            public Vertex getVertex() {
                return widget;
            }
        };
    }

    private void calculateSize() {

        if (subNodes.size() == 0) {
            size = new Dimension(0, 0);
        }

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;


        for (Vertex n : subNodes) {
            Point p = n.getPosition();
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            maxX = Math.max(maxX, p.x + n.getSize().width);
            maxY = Math.max(maxY, p.y + n.getSize().height);
        }

        for (Link l : subEdges) {
            List<Point> points = l.getControlPoints();
            for (Point p : points) {
                if (p != null) {
                    minX = Math.min(minX, p.x);
                    maxX = Math.max(maxX, p.x);
                    minY = Math.min(minY, p.y);
                    maxY = Math.max(maxY, p.y);
                }
            }
        }

        size = new Dimension(maxX - minX, maxY - minY);

        // Normalize coordinates
        for (Vertex n : subNodes) {
            n.setPosition(new Point(n.getPosition().x - minX, n.getPosition().y - minY));
        }

        for (Link l : subEdges) {
            List<Point> points = new ArrayList<Point>(l.getControlPoints());
            for (Point p : points) {
                p.x -= minX;
                p.y -= minY;
            }
            l.setControlPoints(points);

        }

        size.width += 2 * BORDER;
        size.height += 2 * BORDER;
    }

    public Port getInputSlot() {
        return inputSlot;

    }

    public Port getOutputSlot() {
        return outputSlot;
    }

    public Dimension getSize() {
        return size;
    }

    public Point getPosition() {
        return position;
    }

    public void setPosition(Point pos) {

        this.position = pos;
        for (Vertex n : subNodes) {
            Point cur = new Point(n.getPosition());
            cur.translate(pos.x + BORDER, pos.y + BORDER);
            n.setPosition(cur);
        }

        for (Link e : subEdges) {
            List<Point> arr = e.getControlPoints();
            ArrayList<Point> newArr = new ArrayList<Point>(arr.size());
            for (Point p : arr) {
                if (p != null) {
                    Point p2 = new Point(p);
                    p2.translate(pos.x + BORDER, pos.y + BORDER);
                    newArr.add(p2);
                } else {
                    newArr.add(null);
                }
            }

            e.setControlPoints(newArr);
        }
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster c) {
        cluster = c;
    }

    public void setDirty(boolean b) {
        dirty = b;
    }

    public void setRoot(boolean b) {
        root = b;
    }

    public boolean isRoot() {
        return root;
    }

    public int compareTo(Vertex o) {
        return toString().compareTo(o.toString());
    }

    @Override
    public String toString() {
        return name;
    }

    public Set<? extends Vertex> getSubNodes() {
        return subNodes;
    }
}
