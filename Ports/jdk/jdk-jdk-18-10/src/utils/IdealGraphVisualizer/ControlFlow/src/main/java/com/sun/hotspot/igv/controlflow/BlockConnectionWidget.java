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

import com.sun.hotspot.igv.data.InputBlockEdge;
import com.sun.hotspot.igv.layout.Link;
import com.sun.hotspot.igv.layout.Port;
import java.awt.BasicStroke;
import java.awt.Point;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;
import org.netbeans.api.visual.widget.ConnectionWidget;

/**
 *
 * @author Thomas Wuerthinger
 */
public class BlockConnectionWidget extends ConnectionWidget implements Link {

    private static final Stroke NORMAL_STROKE = new BasicStroke(1.0f);
    private static final Stroke BOLD_STROKE = new BasicStroke(2.5f);
    private static final Stroke DASHED_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{5, 5}, 0);
    private static final Stroke BOLD_DASHED_STROKE = new BasicStroke(2.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{5, 5}, 0);

    private BlockWidget from;
    private BlockWidget to;
    private Port inputSlot;
    private Port outputSlot;
    private List<Point> points;
    private InputBlockEdge edge;
    private boolean isDashed = false;
    private boolean isBold = false;

    public BlockConnectionWidget(ControlFlowScene scene, InputBlockEdge edge) {
        super(scene);

        this.edge = edge;
        this.from = (BlockWidget) scene.findWidget(edge.getFrom());
        this.to = (BlockWidget) scene.findWidget(edge.getTo());
        inputSlot = to.getInputSlot();
        outputSlot = from.getOutputSlot();
        points = new ArrayList<Point>();
    }

    public InputBlockEdge getEdge() {
        return edge;
    }

    public Port getTo() {
        return inputSlot;
    }

    public Port getFrom() {
        return outputSlot;
    }

    public void setBold(boolean bold) {
        this.isBold = bold;
        updateStroke();
    }

    public void setDashed(boolean dashed) {
        this.isDashed = dashed;
        updateStroke();
    }

    private void updateStroke() {
        Stroke stroke = NORMAL_STROKE;
        if (isBold) {
            if (isDashed) {
                stroke = BOLD_DASHED_STROKE;
            } else {
                stroke = BOLD_STROKE;
            }
        } else if (isDashed) {
            stroke = DASHED_STROKE;
        }
        setStroke(stroke);
    }

    public void setControlPoints(List<Point> p) {
        this.points = p;
    }

    @Override
    public List<Point> getControlPoints() {
        return points;
    }

    @Override
    public String toString() {
        return "Connection[ " + from.toString() + " - " + to.toString() + "]";
    }

    @Override
    public boolean isVIP() {
        return isBold;
    }
}
