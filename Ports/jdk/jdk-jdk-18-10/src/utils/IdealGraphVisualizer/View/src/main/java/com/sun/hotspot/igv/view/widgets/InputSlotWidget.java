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
package com.sun.hotspot.igv.view.widgets;

import com.sun.hotspot.igv.graph.Figure;
import com.sun.hotspot.igv.graph.InputSlot;
import com.sun.hotspot.igv.view.DiagramScene;
import java.awt.Point;
import java.util.List;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Thomas Wuerthinger
 */
public class InputSlotWidget extends SlotWidget {

    private InputSlot inputSlot;

    public InputSlotWidget(InputSlot slot, DiagramScene scene, Widget parent, FigureWidget fw) {
        super(slot, scene, parent, fw);
        inputSlot = slot;
        //init();
        //getFigureWidget().getLeftWidget().addChild(this);
        Point p = inputSlot.getRelativePosition();
        p.x -= this.calculateClientArea().width / 2;
        p.y += Figure.SLOT_START;
        this.setPreferredLocation(p);
    }

    public InputSlot getInputSlot() {
        return inputSlot;
    }

    @Override
    protected int calculateSlotWidth() {
        List<InputSlot> slots = getSlot().getFigure().getInputSlots();
        assert slots.contains(getSlot());
        return calculateWidth(slots.size());
    }
/*
    protected Point calculateRelativeLocation() {
        if (getFigureWidget().getBounds() == null) {
            return new Point(0, 0);
        }

        double x = 0;
        List<InputSlot> slots = inputSlot.getFigure().getInputSlots();
        assert slots.contains(inputSlot);
        return new Point((int) x, (int) (calculateRelativeY(slots.size(), slots.indexOf(inputSlot))));
    }*/
}
