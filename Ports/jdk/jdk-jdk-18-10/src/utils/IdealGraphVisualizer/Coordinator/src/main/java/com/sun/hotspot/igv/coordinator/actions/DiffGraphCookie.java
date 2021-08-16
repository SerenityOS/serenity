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
package com.sun.hotspot.igv.coordinator.actions;

import com.sun.hotspot.igv.data.InputGraph;
import com.sun.hotspot.igv.data.services.GraphViewer;
import com.sun.hotspot.igv.data.services.InputGraphProvider;
import com.sun.hotspot.igv.difference.Difference;
import com.sun.hotspot.igv.util.LookupHistory;
import org.openide.nodes.Node;
import org.openide.util.Lookup;

/**
 *
 * @author Thomas Wuerthinger
 */
public class DiffGraphCookie implements Node.Cookie {

    private InputGraph graph;

    public DiffGraphCookie(InputGraph graph) {
        this.graph = graph;
    }

    private InputGraph getCurrentGraph() {
        InputGraphProvider graphProvider = LookupHistory.getLast(InputGraphProvider.class);
        if (graphProvider != null) {
            return graphProvider.getGraph();
        }
        return null;
    }

    public boolean isPossible() {
        return getCurrentGraph() != null;
    }

    public void openDiff() {
        InputGraph other = getCurrentGraph();
        final GraphViewer viewer = Lookup.getDefault().lookup(GraphViewer.class);
        if (viewer != null) {
            InputGraph diffGraph = Difference.createDiffGraph(other, graph);
            viewer.view(diffGraph, true);
        }
    }
}
