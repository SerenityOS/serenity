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
import org.netbeans.jemmy.JemmyException;
import org.netbeans.jemmy.JemmyProperties;

import org.netbeans.jemmy.Timeout;
import org.netbeans.jemmy.drivers.LightSupportiveDriver;
import org.netbeans.jemmy.drivers.ScrollDriver;
import org.netbeans.jemmy.operators.ComponentOperator;

/**
 * Superclass for all scroll drivers. Contains all the logic of scrolling. Tries
 * allowed operations in this order: "jump", "drag'n'drop", "push'n'wait",
 * "step". Repeats "step" scrolling while scroller value is not equal to the
 * necessary value, but no more than {@code ADJUST_CLICK_COUNT}.
 *
 * @author Alexandre Iline(alexandre.iline@oracle.com)
 */
public abstract class AbstractScrollDriver extends LightSupportiveDriver implements ScrollDriver {

    /**
     * Maximal number of attempts to reach required position by minimal
     * scrolling operation.
     */
    public static final int ADJUST_CLICK_COUNT = 10;
    public static final String SCROLL_FREEZE_TIMEOUT = AbstractScrollDriver.class.getName() + ".freeze.timeout";

    static {
        JemmyProperties.getProperties().initTimeout(SCROLL_FREEZE_TIMEOUT, 1000);
    }

    /**
     * Constructs an AbstractScrollDriver.
     *
     * @param supported an array of supported class names
     */
    public AbstractScrollDriver(String[] supported) {
        super(supported);
    }

    @Override
    public void scroll(ComponentOperator oper, ScrollAdjuster adj) {
        if (canJump(oper)) {
            doJumps(oper, adj);
        }
        if (canDragAndDrop(oper)) {
            doDragAndDrop(oper, adj);
        }
        if (canPushAndWait(oper)) {
            if (!doPushAndWait(oper, adj, oper.getTimeouts().getTimeout(SCROLL_FREEZE_TIMEOUT))) {
                throw new JemmyException("Scrolling stuck for more than "
                        + oper.getTimeouts().getTimeout(SCROLL_FREEZE_TIMEOUT)
                        + " on " + oper);
            }
        }
        for (int i = 0; i < ADJUST_CLICK_COUNT; i++) {
            doSteps(oper, adj);
        }
    }

    /**
     * Performs minimal scrolling step.
     *
     * @param oper an operator.
     * @param adj a scroll adjuster
     */
    protected abstract void step(ComponentOperator oper, ScrollAdjuster adj);

    /**
     * Performs maximal scroll step.
     *
     * @param oper an operator.
     * @param adj a scroll adjuster
     */
    protected abstract void jump(ComponentOperator oper, ScrollAdjuster adj);

    /**
     * Presses something like a scroll button.
     *
     * @param oper an operator.
     * @param direction - one of the ScrollAdjister.INCREASE_SCROLL_DIRECTION,
     * ScrollAdjister.DECREASE_SCROLL_DIRECTION,
     * ScrollAdjister.DO_NOT_TOUCH_SCROLL_DIRECTION values.
     * @param orientation one of the Adjustable.HORIZONTAL or
     * Adjustable.VERTICAL values.
     */
    protected abstract void startPushAndWait(ComponentOperator oper, int direction, int orientation);

    /**
     * Releases something like a scroll button.
     *
     * @param oper an operator.
     * @param direction - one of the ScrollAdjister.INCREASE_SCROLL_DIRECTION,
     * ScrollAdjister.DECREASE_SCROLL_DIRECTION,
     * ScrollAdjister.DO_NOT_TOUCH_SCROLL_DIRECTION values.
     * @param orientation one of the Adjustable.HORIZONTAL or
     * Adjustable.VERTICAL values.
     */
    protected abstract void stopPushAndWait(ComponentOperator oper, int direction, int orientation);

    /**
     * Starts drag'n'drop scrolling.
     *
     * @param oper an operator.
     * @return start drugging point.
     */
    protected abstract Point startDragging(ComponentOperator oper);

    /**
     * Drop at a specified point.
     *
     * @param oper an operator.
     * @param pnt the point to drop.
     */
    protected abstract void drop(ComponentOperator oper, Point pnt);

    /**
     * Drag to a specified point.
     *
     * @param oper an operator.
     * @param pnt the point to drag to.
     */
    protected abstract void drag(ComponentOperator oper, Point pnt);

    /**
     * Returns a timeout for sleeping between verifications during "push and
     * wait" scrolling.
     *
     * @param oper an operator.
     * @return a timeout
     */
    protected abstract Timeout getScrollDeltaTimeout(ComponentOperator oper);

    /**
     * Tells if this driver allows to perform drag'n'drop scrolling.
     *
     * @param oper an operator.
     * @return true if this driver allows to drag'n'drop.
     */
    protected abstract boolean canDragAndDrop(ComponentOperator oper);

    /**
     * Tells if this driver allows to perform jumps.
     *
     * @param oper an operator.
     * @return true if this driver allows to jump.
     */
    protected abstract boolean canJump(ComponentOperator oper);

    /**
     * Tells if this driver allows to perform "push and wait" scrolling.
     *
     * @param oper an operator.
     * @return true if this driver allows to "push and wait".
     */
    protected abstract boolean canPushAndWait(ComponentOperator oper);

    /**
     * Returns a number of pixels in one drag and drop scrolling.
     *
     * @param oper an operator.
     * @return drag'n'drop step length.
     */
    protected abstract int getDragAndDropStepLength(ComponentOperator oper);

    /**
     * Performs drag'n'drop scrolling till scroller's value does not cross
     * required value.
     *
     * @param oper an operator.
     * @param adj a scroll adjuster
     */
    protected void doDragAndDrop(ComponentOperator oper, ScrollAdjuster adj) {
        int direction = adj.getScrollDirection();
        if (direction != ScrollAdjuster.DO_NOT_TOUCH_SCROLL_DIRECTION) {
            Point pnt = startDragging(oper);
            while (adj.getScrollDirection() == direction) {
                drag(oper, pnt = increasePoint(oper, pnt, adj, direction));
            }
            drop(oper, pnt);
        }
    }

    /**
     * Performs jump scrolling till scroller's value does not cross required
     * value.
     *
     * @param oper an operator.
     * @param adj a scroll adjuster
     */
    protected void doJumps(ComponentOperator oper, ScrollAdjuster adj) {
        int direction = adj.getScrollDirection();
        if (direction != ScrollAdjuster.DO_NOT_TOUCH_SCROLL_DIRECTION) {
            while (adj.getScrollDirection() == direction) {
                jump(oper, adj);
            }
        }
    }

    protected abstract int position(ComponentOperator oper, int orientation);

    /**
     * Performs "push and wait" scrolling till scroller's value does not cross
     * required value.
     *
     * @param oper an operator.
     * @param adj a scroll adjuster
     */
    protected boolean doPushAndWait(ComponentOperator oper, ScrollAdjuster adj, long freezeTimeout) {
        int direction = adj.getScrollDirection();
        int orientation = adj.getScrollOrientation();
        int position = position(oper, orientation);
        long lastChanded = System.currentTimeMillis();
        if (direction != ScrollAdjuster.DO_NOT_TOUCH_SCROLL_DIRECTION) {
            Timeout delta = getScrollDeltaTimeout(oper);
            startPushAndWait(oper, direction, orientation);
            while (adj.getScrollDirection() == direction) {
                delta.sleep();
                int curPosition = position(oper, orientation);
                if (curPosition != position) {
                    position = curPosition;
                    lastChanded = System.currentTimeMillis();
                } else if ((System.currentTimeMillis() - lastChanded) > freezeTimeout) {
                    return false;
                }
            }
            stopPushAndWait(oper, direction, orientation);
        }
        return true;
    }

    /**
     * Performs minimal scrollings till scroller's value does not cross required
     * value.
     *
     * @param oper an operator.
     * @param adj a scroll adjuster
     */
    protected void doSteps(ComponentOperator oper, ScrollAdjuster adj) {
        int direction = adj.getScrollDirection();
        if (direction != ScrollAdjuster.DO_NOT_TOUCH_SCROLL_DIRECTION) {
            while (adj.getScrollDirection() == direction) {
                step(oper, adj);
            }
        }
    }

    private Point increasePoint(ComponentOperator oper, Point pnt, ScrollAdjuster adj, int direction) {
        return ((adj.getScrollOrientation() == Adjustable.HORIZONTAL)
                ? new Point(pnt.x + ((direction == 1) ? 1 : -1) * getDragAndDropStepLength(oper), pnt.y)
                : new Point(pnt.x, pnt.y + ((direction == 1) ? 1 : -1) * getDragAndDropStepLength(oper)));
    }
}
