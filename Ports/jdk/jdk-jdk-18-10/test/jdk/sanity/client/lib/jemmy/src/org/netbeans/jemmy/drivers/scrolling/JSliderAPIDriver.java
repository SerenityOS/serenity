/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package org.netbeans.jemmy.drivers.scrolling;

import java.awt.Point;

import org.netbeans.jemmy.Timeout;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.JSliderOperator;

/**
 * A scroll driver serving JSlider component.
 *
 * @author Alexandre Iline(alexandre.iline@oracle.com)
 */
public class JSliderAPIDriver extends AbstractScrollDriver {

    private final static int SMALL_INCREMENT = 1;

    /**
     * Constructs a JSliderDriver object.
     */
    public JSliderAPIDriver() {
        super(new String[]{"org.netbeans.jemmy.operators.JSliderOperator"});
    }

    @Override
    protected int position(ComponentOperator oper, int orientation) {
        return ((JSliderOperator) oper).getValue();
    }

    @Override
    public void scrollToMinimum(final ComponentOperator oper, int orientation) {
        ((JSliderOperator) oper).setValue(((JSliderOperator) oper).getMinimum());
    }

    @Override
    public void scrollToMaximum(final ComponentOperator oper, int orientation) {
        ((JSliderOperator) oper).setValue(((JSliderOperator) oper).getMaximum());
    }

    @Override
    protected void step(ComponentOperator oper, ScrollAdjuster adj) {
        JSliderOperator scroll = (JSliderOperator) oper;
        int newValue = -1;
        if (adj.getScrollDirection() == ScrollAdjuster.DECREASE_SCROLL_DIRECTION) {
            newValue = (scroll.getValue() > scroll.getMinimum()
                    + getUnitIncrement(scroll))
                            ? scroll.getValue() - getUnitIncrement(scroll)
                            : scroll.getMinimum();
            scroll.setValue(newValue);
        } else if (adj.getScrollDirection() == ScrollAdjuster.INCREASE_SCROLL_DIRECTION) {
            newValue = (scroll.getValue() < scroll.getMaximum()
                    - getUnitIncrement(scroll))
                            ? scroll.getValue() + getUnitIncrement(scroll)
                            : scroll.getMaximum();
            scroll.setValue(newValue);
        }
    }

    @Override
    protected Timeout getScrollDeltaTimeout(ComponentOperator oper) {
        return (oper.getTimeouts().
                create("JSliderOperator.ScrollingDelta"));
    }

    @Override
    protected void jump(final ComponentOperator oper, final ScrollAdjuster adj) {
        JSliderOperator scroll = (JSliderOperator) oper;
        int newValue = -1;
        if (adj.getScrollDirection() == ScrollAdjuster.DECREASE_SCROLL_DIRECTION) {
            newValue = (scroll.getValue() > scroll.getMinimum()
                    + getBlockIncrement(scroll))
                            ? scroll.getValue() - getBlockIncrement(scroll)
                            : scroll.getMinimum();
            scroll.setValue(newValue);
        } else if (adj.getScrollDirection() == ScrollAdjuster.INCREASE_SCROLL_DIRECTION) {
            newValue = (scroll.getValue() < scroll.getMaximum()
                    - getBlockIncrement(scroll))
                            ? scroll.getValue() + getBlockIncrement(scroll)
                            : scroll.getMaximum();
            scroll.setValue(newValue);
        }
    }

    @Override
    protected void startPushAndWait(ComponentOperator oper, int direction, int orientation) {
    }

    @Override
    protected void stopPushAndWait(ComponentOperator oper, int direction, int orientation) {
    }

    @Override
    protected Point startDragging(ComponentOperator oper) {
        return null;
    }

    @Override
    protected void drop(ComponentOperator oper, Point pnt) {
    }

    @Override
    protected void drag(ComponentOperator oper, Point pnt) {
    }

    @Override
    protected boolean canDragAndDrop(ComponentOperator oper) {
        return false;
    }

    @Override
    protected boolean canJump(ComponentOperator oper) {
        return isSmallIncrement((JSliderOperator) oper);
    }

    @Override
    protected boolean canPushAndWait(ComponentOperator oper) {
        return false;
    }

    @Override
    protected int getDragAndDropStepLength(ComponentOperator oper) {
        return 1;
    }

    private int getUnitIncrement(JSliderOperator oper) {
        return ((oper.getMinorTickSpacing() == 0)
                ? 1
                : oper.getMinorTickSpacing());
    }

    private int getBlockIncrement(JSliderOperator oper) {
        return ((oper.getMajorTickSpacing() == 0)
                ? 1
                : oper.getMajorTickSpacing());
    }

    private boolean isSmallIncrement(JSliderOperator oper) {
        return (oper.getMajorTickSpacing() <= SMALL_INCREMENT
                && oper.getMajorTickSpacing() <= SMALL_INCREMENT);
    }

}
