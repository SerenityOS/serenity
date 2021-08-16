/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.hotspot.igv.graph;

import com.sun.hotspot.igv.data.Source;
import com.sun.hotspot.igv.layout.Link;
import com.sun.hotspot.igv.layout.Port;
import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Thomas Wuerthinger
 */
public class Connection implements Source.Provider, Link {

    @Override
    public boolean isVIP() {
        return style == ConnectionStyle.BOLD;
    }

    public enum ConnectionStyle {

        NORMAL,
        DASHED,
        BOLD,
        INVISIBLE
    }
    private InputSlot inputSlot;
    private OutputSlot outputSlot;
    private Source source;
    private Color color;
    private ConnectionStyle style;
    private List<Point> controlPoints;
    private String label;
    private String type;

    protected Connection(InputSlot inputSlot, OutputSlot outputSlot, String label, String type) {
        this.inputSlot = inputSlot;
        this.outputSlot = outputSlot;
        this.label = label;
        this.type = type;
        this.inputSlot.connections.add(this);
        this.outputSlot.connections.add(this);
        controlPoints = new ArrayList<>();
        Figure sourceFigure = this.outputSlot.getFigure();
        Figure destFigure = this.inputSlot.getFigure();
        sourceFigure.addSuccessor(destFigure);
        destFigure.addPredecessor(sourceFigure);
        source = new Source();

        this.color = Color.BLACK;
        this.style = ConnectionStyle.NORMAL;
    }

    public InputSlot getInputSlot() {
        return inputSlot;
    }

    public OutputSlot getOutputSlot() {
        return outputSlot;
    }

    public Color getColor() {
        return color;
    }

    public ConnectionStyle getStyle() {
        return style;
    }

    public void setColor(Color c) {
        color = c;
    }

    public void setStyle(ConnectionStyle s) {
        style = s;
    }

    @Override
    public Source getSource() {
        return source;
    }

    public String getLabel() {
        return label;
    }

    public String getType() {
        return type;
    }

    public void remove() {
        inputSlot.getFigure().removePredecessor(outputSlot.getFigure());
        inputSlot.connections.remove(this);
        outputSlot.getFigure().removeSuccessor(inputSlot.getFigure());
        outputSlot.connections.remove(this);
    }

    public String getToolTipText() {
        StringBuilder builder = new StringBuilder();
        if (label != null) {
            builder.append(label).append(": ");
        }
        if (type != null) {
            builder.append(type).append(" ");
        }
        // Resolve strings lazily every time the tooltip is shown, instead of
        // eagerly as for node labels, for efficiency.
        String shortNodeText = getInputSlot().getFigure().getDiagram().getShortNodeText();
        builder.append(getOutputSlot().getFigure().getProperties().resolveString(shortNodeText));
        builder.append(" → ");
        builder.append(getInputSlot().getFigure().getProperties().resolveString(shortNodeText));
        return builder.toString();
    }

    @Override
    public String toString() {
        return "Connection('" + label + "', " + getFrom().getVertex() + " to " + getTo().getVertex() + ")";
    }

    @Override
    public Port getFrom() {
        return outputSlot;
    }

    @Override
    public Port getTo() {
        return inputSlot;
    }

    @Override
    public List<Point> getControlPoints() {
        return controlPoints;
    }

    @Override
    public void setControlPoints(List<Point> list) {
        controlPoints = list;
    }
}

