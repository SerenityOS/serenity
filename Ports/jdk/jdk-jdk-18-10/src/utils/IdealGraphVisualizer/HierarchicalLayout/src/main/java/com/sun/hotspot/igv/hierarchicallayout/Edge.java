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

/**
 *
 * @author Thomas Wuerthinger
 */
public class Edge<N, E> {

    private E data;
    private Node<N, E> source;
    private Node<N, E> dest;

    protected Edge(Graph<N, E> graph, Node<N, E> source, Node<N, E> dest, E data) {
        setData(data);
        this.source = source;
        this.dest = dest;
        assert source != null;
        assert dest != null;
        assert source.getGraph() == dest.getGraph();
        assert source.getGraph() != null;
        assert dest.getGraph() != null;
    }

    public Node<N, E> getSource() {
        return source;
    }

    public Node<N, E> getDest() {
        return dest;
    }

    public E getData() {
        return data;
    }

    public void setData(E e) {
        data = e;
    }

    public void remove() {
        source.getGraph().removeEdge(this, null);
    }

    public boolean isSelfLoop() {
        return source == dest;
    }

    public void reverse() {

        // Remove from current source / dest
        source.removeOutEdge(this);
        dest.removeInEdge(this);

        Node<N, E> tmp = source;
        source = dest;
        dest = tmp;

        // Add to new source / dest
        source.addOutEdge(this);
        dest.addInEdge(this);
    }

    @Override
    public String toString() {
        return "Edge (" + source + " -- " + dest + "): " + data;
    }
}
