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
package com.sun.hotspot.igv.graph;

import java.awt.Point;

/**
 *
 * @author Thomas Wuerthinger
 */
public class OutputSlot extends Slot {

    protected OutputSlot(Figure figure, int wantedIndex) {
        super(figure, wantedIndex);
    }

    @Override
    public int getPosition() {
        return getFigure().getOutputSlots().indexOf(this);
    }

    @Override
    public void setPosition(int position) {
        OutputSlot s = getFigure().outputSlots.remove(position);
        getFigure().outputSlots.add(position, s);
    }

    @Override
    public Point getRelativePosition() {
        int gap = getFigure().getWidth() - Figure.getSlotsWidth(getFigure().getOutputSlots());
        if(gap < 0) {
            gap = 0;
        }
        double gapRatio = (double)gap / (double)(getFigure().getOutputSlots().size() + 1);
        int gapAmount = (int)((getPosition() + 1)*gapRatio);
        return new Point(gapAmount + Figure.getSlotsWidth(Figure.getAllBefore(getFigure().getOutputSlots(), this)) + getWidth()/2, Figure.SLOT_START);
    }

    @Override
    public String toString() {
        return "OutputSlot[figure=" + this.getFigure().toString() + ", position=" + getPosition() + "]";
    }
}
