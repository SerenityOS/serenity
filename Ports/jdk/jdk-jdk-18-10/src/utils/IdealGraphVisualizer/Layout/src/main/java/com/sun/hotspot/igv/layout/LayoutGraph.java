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
package com.sun.hotspot.igv.layout;

import java.util.*;

/**
 *
 * @author Thomas Wuerthinger
 */
public class LayoutGraph {

    private Set<? extends Link> links;
    private SortedSet<Vertex> vertices;
    private HashMap<Vertex, Set<Port>> inputPorts;
    private HashMap<Vertex, Set<Port>> outputPorts;
    private HashMap<Port, Set<Link>> portLinks;

    public LayoutGraph(Set<? extends Link> links) {
        this(links, new HashSet<Vertex>());
    }

    public LayoutGraph(Set<? extends Link> links, Set<? extends Vertex> additionalVertices) {
        this.links = links;
        assert verify();

        vertices = new TreeSet<>();
        portLinks = new HashMap<>(links.size());
        inputPorts = new HashMap<>(links.size());
        outputPorts = new HashMap<>(links.size());

        for (Link l : links) {
            Port p = l.getFrom();
            Port p2 = l.getTo();
            Vertex v1 = p.getVertex();
            Vertex v2 = p2.getVertex();

            if (!vertices.contains(v1)) {

                outputPorts.put(v1, new HashSet<Port>(1));
                inputPorts.put(v1, new HashSet<Port>(3));
                vertices.add(v1);
                assert vertices.contains(v1);
            }

            if (!vertices.contains(v2)) {
                vertices.add(v2);
                assert vertices.contains(v2);
                outputPorts.put(v2, new HashSet<Port>(1));
                inputPorts.put(v2, new HashSet<Port>(3));
            }

            if (!portLinks.containsKey(p)) {
                HashSet<Link> hashSet = new HashSet<>(3);
                portLinks.put(p, hashSet);
            }

            if (!portLinks.containsKey(p2)) {
                portLinks.put(p2, new HashSet<Link>(3));
            }

            outputPorts.get(v1).add(p);
            inputPorts.get(v2).add(p2);

            portLinks.get(p).add(l);
            portLinks.get(p2).add(l);
        }

        for (Vertex v : additionalVertices) {
            if (!vertices.contains(v)) {
                outputPorts.put(v, new HashSet<Port>(1));
                inputPorts.put(v, new HashSet<Port>(3));
                vertices.add(v);
                vertices.contains(v);
            }
        }
    }

    public Set<Port> getInputPorts(Vertex v) {
        return this.inputPorts.get(v);
    }

    public Set<Port> getOutputPorts(Vertex v) {
        return this.outputPorts.get(v);
    }

    public Set<Link> getPortLinks(Port p) {
        return portLinks.get(p);
    }

    public Set<? extends Link> getLinks() {
        return links;
    }

    public boolean verify() {
        return true;
    }

    public SortedSet<Vertex> getVertices() {
        return vertices;
    }

    private void markNotRoot(Set<Vertex> notRootSet, Vertex v, Vertex startingVertex) {

        if (notRootSet.contains(v)) {
            return;
        }
        if (v != startingVertex) {
            notRootSet.add(v);
        }
        Set<Port> outPorts = getOutputPorts(v);
        for (Port p : outPorts) {
            Set<Link> portLinks = getPortLinks(p);
            for (Link l : portLinks) {
                Port other = l.getTo();
                Vertex otherVertex = other.getVertex();
                if (otherVertex != startingVertex) {
                    markNotRoot(notRootSet, otherVertex, startingVertex);
                }
            }
        }
    }

    // Returns a set of vertices with the following properties:
    // - All Vertices in the set startingRoots are elements of the set.
    // - When starting a DFS at every vertex in the set, every vertex of the
    //   whole graph is visited.
    public Set<Vertex> findRootVertices(Set<Vertex> startingRoots) {

        Set<Vertex> notRootSet = new HashSet<>();
        for (Vertex v : startingRoots) {
            if (!notRootSet.contains(v)) {
                markNotRoot(notRootSet, v, v);
            }
        }

        Set<Vertex> tmpVertices = getVertices();
        for (Vertex v : tmpVertices) {
            if (!notRootSet.contains(v)) {
                if (this.getInputPorts(v).size() == 0) {
                    markNotRoot(notRootSet, v, v);
                }
            }
        }

        for (Vertex v : tmpVertices) {
            if (!notRootSet.contains(v)) {
                markNotRoot(notRootSet, v, v);
            }
        }

        Set<Vertex> result = new HashSet<>();
        for (Vertex v : tmpVertices) {
            if (!notRootSet.contains(v)) {
                result.add(v);
            }
        }
        assert tmpVertices.size() == 0 || result.size() > 0;
        return result;
    }

    public Set<Vertex> findRootVertices() {
        return findRootVertices(new HashSet<Vertex>());
    }

    public SortedSet<Cluster> getClusters() {

        SortedSet<Cluster> clusters = new TreeSet<Cluster>();
        for (Vertex v : getVertices()) {
            if (v.getCluster() != null) {
                clusters.add(v.getCluster());
            }
        }

        return clusters;
    }
}
