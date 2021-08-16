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

import java.awt.Adjustable;
import java.awt.Point;
import java.awt.Scrollbar;

import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.ScrollPaneOperator;

/**
 * ScrollDriver for java.awt.ScrollPane component type.
 *
 * @author Alexandre Iline(alexandre.iline@oracle.com)
 */
public class ScrollPaneDriver extends AWTScrollDriver {

    private static final int CLICK_OFFSET = 5;

    /**
     * Constructs a ScrollPaneDriver.
     */
    public ScrollPaneDriver() {
        super(new String[]{"org.netbeans.jemmy.operators.ScrollPaneOperator"});
    }

    @Override
    protected int position(ComponentOperator oper, int orientation) {
        return (orientation == Adjustable.HORIZONTAL)
                ? ((ScrollPaneOperator) oper).getScrollPosition().x
                : ((ScrollPaneOperator) oper).getScrollPosition().y;
    }

    @Override
    public void scrollToMinimum(ComponentOperator oper, final int orientation) {
        final Adjustable adj
                = (orientation == Scrollbar.HORIZONTAL)
                        ? ((ScrollPaneOperator) oper).getHAdjustable()
                        : ((ScrollPaneOperator) oper).getVAdjustable();
        scroll(oper,
                new ScrollAdjuster() {
            @Override
            public int getScrollDirection() {
                return ((adj.getMinimum() < adj.getValue())
                        ? DECREASE_SCROLL_DIRECTION
                        : DO_NOT_TOUCH_SCROLL_DIRECTION);
            }

            @Override
            public int getScrollOrientation() {
                return orientation;
            }

            @Override
            public String getDescription() {
                return "Scroll to minimum";
            }

            @Override
            public String toString() {
                return "scrollToMinimum.ScrollAdjuster{description = " + getDescription() + '}';
            }
        });
    }

    @Override
    public void scrollToMaximum(ComponentOperator oper, final int orientation) {
        final Adjustable adj
                = (orientation == Scrollbar.HORIZONTAL)
                        ? ((ScrollPaneOperator) oper).getHAdjustable()
                        : ((ScrollPaneOperator) oper).getVAdjustable();
        scroll(oper,
                new ScrollAdjuster() {
            @Override
            public int getScrollDirection() {
                return (((adj.getMaximum() - adj.getVisibleAmount()) > adj.getValue())
                        ? INCREASE_SCROLL_DIRECTION
                        : DO_NOT_TOUCH_SCROLL_DIRECTION);
            }

            @Override
            public int getScrollOrientation() {
                return orientation;
            }

            @Override
            public String getDescription() {
                return "Scroll to maximum";
            }

            @Override
            public String toString() {
                return "scrollToMaximum.ScrollAdjuster{description = " + getDescription() + '}';
            }
        });
    }

    @Override
    protected Point getClickPoint(ComponentOperator oper, int direction, int orientation) {
        int x, y;
        if (orientation == Scrollbar.HORIZONTAL) {
            int offset = ((ScrollPaneOperator) oper).
                    isScrollbarVisible(Scrollbar.VERTICAL)
                    ? ((ScrollPaneOperator) oper).getVScrollbarWidth() : 0;
            if (direction == ScrollAdjuster.INCREASE_SCROLL_DIRECTION) {
                x = oper.getWidth() - 1 - CLICK_OFFSET - offset;
            } else if (direction == ScrollAdjuster.DECREASE_SCROLL_DIRECTION) {
                x = CLICK_OFFSET;
            } else {
                return null;
            }
            y = oper.getHeight() - ((ScrollPaneOperator) oper).getHScrollbarHeight() / 2;
        } else if (orientation == Scrollbar.VERTICAL) {
            int offset = ((ScrollPaneOperator) oper).
                    isScrollbarVisible(Scrollbar.HORIZONTAL)
                    ? ((ScrollPaneOperator) oper).getHScrollbarHeight() : 0;
            if (direction == ScrollAdjuster.INCREASE_SCROLL_DIRECTION) {
                y = oper.getHeight() - 1 - CLICK_OFFSET - offset;
            } else if (direction == ScrollAdjuster.DECREASE_SCROLL_DIRECTION) {
                y = CLICK_OFFSET;
            } else {
                return null;
            }
            x = oper.getWidth() - ((ScrollPaneOperator) oper).getVScrollbarWidth() / 2;
        } else {
            return null;
        }
        return new Point(x, y);
    }
}
