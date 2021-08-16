/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jit.misctests.fpustack;

import java.util.*;
import java.awt.*;
import java.applet.Applet;
import nsk.share.TestFailure;

public class GraphPanel extends Panel {
    private Panel graph;                                                        // the container

    private ilayout layout = null;  // the strategy


    private int nodesN;                                                         // number of nodes
    private Node nodes[] = new Node[200];                       // nodes container

    /**
    ** constructor
    **
    ** @param      Panel                   the container
    ** @param      layout  a strategy to layout the nodes
    **
    **/
    GraphPanel(Panel graph, layout ls ) {
        this.graph = graph;
        layout = ls;
    }




    /**
    ** add a node via label text.
    **
    ** @param      lbl             the label
    ** @return     the index of the node in array nodes[]
    **
    **/

    public int addNode(String lbl) {
        Node n = new Node();
        if (nodesN > 0) {
            n.x = nodes[nodesN-1].x + 30;
            n.y = nodes[nodesN-1].y + 30;
        }
        n.lbl = lbl;

        nodes[nodesN] = n;
        return nodesN++;
    }



    /**
    ** add a node via label text.
    **
    ** @param      lbl             the label
    ** @return     the index of the node in array nodes[]
    **
    **/

    public void addNodes(String lb1, String lb2, String lb3) {
        addNode(lb1);
        addNode(lb2);
        addNode(lb3);
    }



    /**
    ** layout the nodes on the panel. the layout is used
    **
    **
    **/
    public synchronized void formatNodes( ) {

        // format nodes
        FontMetrics fm = getFontMetrics(getFont());
        Dimension d = getSize();

        Node[] ns = new Node[ nodesN ];
        System.arraycopy(nodes, 0, ns, 0, nodesN);
        layout.formatNodes( ns, d, fm );

    }



}
