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
import java.util.List;

/**
 *
 * @author Thomas Wuerthinger
 */
public class InputSlot extends Slot {

    protected InputSlot(Figure figure, int wantedIndex) {
        super(figure, wantedIndex);
    }

    @Override
    public int getPosition() {
        return getFigure().getInputSlots().indexOf(this);
    }

    @Override
    public void setPosition(int position) {
        List<InputSlot> inputSlots = getFigure().inputSlots;
        InputSlot s = inputSlots.remove(position);
        inputSlots.add(position, s);
    }
    @Override
    public Point getRelativePosition() {
        int gap = getFigure().getWidth() - Figure.getSlotsWidth(getFigure().getInputSlots());
        double gapRatio = (double)gap / (double)(getFigure().getInputSlots().size() + 1);
        int gapAmount = (int)((getPosition() + 1)*gapRatio);
        return new Point(gapAmount + Figure.getSlotsWidth(Figure.getAllBefore(getFigure().getInputSlots(), this)) + getWidth()/2, -Figure.SLOT_START);
        //return new Point((getFigure().getWidth() / (getFigure().getInputSlots().size() * 2)) * (getPosition() * 2 + 1), -Figure.SLOT_START);
    }

    @Override
    public String toString() {
        return "InputSlot[figure=" + this.getFigure().toString() + ", position=" + getPosition() + "]";
    }
}
