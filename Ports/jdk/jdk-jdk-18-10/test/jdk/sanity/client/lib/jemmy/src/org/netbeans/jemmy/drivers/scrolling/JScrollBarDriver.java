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

import javax.swing.JScrollBar;

import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.Timeout;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.MouseDriver;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JScrollBarOperator;
import org.netbeans.jemmy.operators.Operator;

/**
 * ScrollDriver for javax.swing.JScrollBar component type.
 *
 * @author Alexandre Iline(alexandre.iline@oracle.com)
 */
public class JScrollBarDriver extends AbstractScrollDriver {

    private final static int SMALL_INCREMENT = 1;
    private final static int MINIMAL_DRAGGER_SIZE = 5;
    private final static int RELATIVE_DRAG_STEP_LENGTH = 20;

    private QueueTool queueTool;

    /**
     * Constructs a JScrollBarDriver.
     */
    public JScrollBarDriver() {
        super(new String[]{"org.netbeans.jemmy.operators.JScrollBarOperator"});
        queueTool = new QueueTool();
    }

    @Override
    protected int position(ComponentOperator oper, int orientation) {
        return ((JScrollBarOperator) oper).getValue();
    }

    @Override
    public void scrollToMinimum(ComponentOperator oper, int orientation) {
        startDragging(oper);
        Point pnt = new Point(0, 0);
        drag(oper, pnt);
        Timeout sleepTime = oper.getTimeouts().create("Waiter.TimeDelta");
        while (((JScrollBarOperator) oper).getValue()
                > ((JScrollBarOperator) oper).getMinimum()) {
            sleepTime.sleep();
        }
        drop(oper, pnt);
    }

    @Override
    public void scrollToMaximum(ComponentOperator oper, int orientation) {
        startDragging(oper);
        Point pnt = new Point(oper.getWidth() - 1, oper.getHeight() - 1);
        drag(oper, pnt);
        Timeout sleepTime = oper.getTimeouts().create("Waiter.TimeDelta");
        while (((JScrollBarOperator) oper).getValue()
                > (((JScrollBarOperator) oper).getMaximum()
                - ((JScrollBarOperator) oper).getVisibleAmount())) {
            sleepTime.sleep();
        }
        drop(oper, pnt);
    }

    @Override
    protected void step(ComponentOperator oper, ScrollAdjuster adj) {
        JButtonOperator boper = findAButton(oper, adj.getScrollDirection());
        DriverManager.getButtonDriver(boper).push(boper);
    }

    @Override
    protected void jump(final ComponentOperator oper, final ScrollAdjuster adj) {
        final JButtonOperator lessButton = findAButton(oper, ScrollAdjuster.DECREASE_SCROLL_DIRECTION);
        final JButtonOperator moreButton = findAButton(oper, ScrollAdjuster.INCREASE_SCROLL_DIRECTION);
        queueTool.invokeSmoothly(new QueueTool.QueueAction<Void>("Scrolling by clicking with the mouse") {
            @Override
            public Void launch() {
                if (adj.getScrollDirection() != ScrollAdjuster.DO_NOT_TOUCH_SCROLL_DIRECTION) {
                    int x, y;
                    switch (((JScrollBarOperator) oper).getOrientation()) {
                        case JScrollBar.HORIZONTAL:
                            switch (adj.getScrollDirection()) {
                                case ScrollAdjuster.INCREASE_SCROLL_DIRECTION:
                                    x = moreButton.getX() - 1;
                                    break;
                                case ScrollAdjuster.DECREASE_SCROLL_DIRECTION:
                                    x = lessButton.getX() + lessButton.getWidth();
                                    break;
                                default:
                                    return null;
                            }
                            y = lessButton.getHeight() / 2;
                            break;
                        case JScrollBar.VERTICAL:
                            switch (adj.getScrollDirection()) {
                                case ScrollAdjuster.INCREASE_SCROLL_DIRECTION:
                                    y = moreButton.getY() - 1;
                                    break;
                                case ScrollAdjuster.DECREASE_SCROLL_DIRECTION:
                                    y = lessButton.getY() + lessButton.getHeight();
                                    break;
                                default:
                                    return null;
                            }
                            x = lessButton.getWidth() / 2;
                            break;
                        default:
                            return null;
                    }
                    DriverManager.getMouseDriver(oper).
                            clickMouse(oper, x, y, 1, Operator.getDefaultMouseButton(), 0, new Timeout("", 0));
                }
                return null;
            }
        });
    }

    @Override
    protected void startPushAndWait(ComponentOperator oper, int direction, int orientation) {
        JButtonOperator boper = findAButton(oper, direction);
        DriverManager.getButtonDriver(boper).press(boper);
    }

    @Override
    protected void stopPushAndWait(ComponentOperator oper, int direction, int orientation) {
        JButtonOperator boper = findAButton(oper, direction);
        DriverManager.getButtonDriver(boper).release(boper);
    }

    @Override
    protected Point startDragging(ComponentOperator oper) {
        JButtonOperator lessButton = findAButton(oper, ScrollAdjuster.DECREASE_SCROLL_DIRECTION);
        JButtonOperator moreButton = findAButton(oper, ScrollAdjuster.INCREASE_SCROLL_DIRECTION);
        Point pnt = getClickPoint((JScrollBarOperator) oper, lessButton, moreButton, ((JScrollBarOperator) oper).getValue());
        MouseDriver mdriver = DriverManager.getMouseDriver(oper);
        mdriver.moveMouse(oper, pnt.x, pnt.y);
        mdriver.pressMouse(oper, pnt.x, pnt.y, Operator.getDefaultMouseButton(), 0);
        return pnt;
    }

    @Override
    protected void drop(ComponentOperator oper, Point pnt) {
        DriverManager.getMouseDriver(oper).
                releaseMouse(oper, pnt.x, pnt.y, Operator.getDefaultMouseButton(), 0);
    }

    @Override
    protected void drag(ComponentOperator oper, Point pnt) {
        DriverManager.getMouseDriver(oper).
                dragMouse(oper, pnt.x, pnt.y, Operator.getDefaultMouseButton(), 0);
    }

    @Override
    protected Timeout getScrollDeltaTimeout(ComponentOperator oper) {
        return (oper.getTimeouts().
                create("ScrollbarOperator.DragAndDropScrollingDelta"));
    }

    @Override
    protected boolean canDragAndDrop(ComponentOperator oper) {
        if (!isSmallIncrement((JScrollBarOperator) oper)) {
            return false;
        }
        boolean result = false;
        MouseDriver mdriver = DriverManager.getMouseDriver(oper);
        JButtonOperator less = findAButton(oper, ScrollAdjuster.DECREASE_SCROLL_DIRECTION);
        JButtonOperator more = findAButton(oper, ScrollAdjuster.INCREASE_SCROLL_DIRECTION);
        Point pnt = getClickPoint((JScrollBarOperator) oper, less, more, ((JScrollBarOperator) oper).getValue());
        mdriver.moveMouse(oper, pnt.x, pnt.y);
        mdriver.pressMouse(oper, pnt.x, pnt.y, Operator.getDefaultMouseButton(), 0);
        result = ((JScrollBarOperator) oper).getValueIsAdjusting();
        mdriver.releaseMouse(oper, pnt.x, pnt.y, Operator.getDefaultMouseButton(), 0);
        return result && isSmallIncrement((JScrollBarOperator) oper);
    }

    @Override
    protected boolean canJump(ComponentOperator oper) {
        return isSmallIncrement((JScrollBarOperator) oper);
    }

    @Override
    protected boolean canPushAndWait(ComponentOperator oper) {
        return isSmallIncrement((JScrollBarOperator) oper);
    }

    @Override
    protected int getDragAndDropStepLength(ComponentOperator oper) {
        JButtonOperator less = findAButton(oper, ScrollAdjuster.DECREASE_SCROLL_DIRECTION);
        JButtonOperator more = findAButton(oper, ScrollAdjuster.INCREASE_SCROLL_DIRECTION);
        int width = oper.getWidth() - less.getWidth() - more.getWidth();
        int height = oper.getHeight() - less.getHeight() - more.getHeight();
        int max = (width > height) ? width : height;
        if (max >= RELATIVE_DRAG_STEP_LENGTH * 2) {
            return max / RELATIVE_DRAG_STEP_LENGTH;
        } else {
            return 1;
        }
    }

    private boolean isSmallIncrement(JScrollBarOperator oper) {
        return (oper.getUnitIncrement(-1) <= SMALL_INCREMENT
                && oper.getUnitIncrement(1) <= SMALL_INCREMENT);
    }

    private Point getClickPoint(JScrollBarOperator oper, JButtonOperator lessButton, JButtonOperator moreButton, int value) {
        int lenght = (oper.getOrientation() == JScrollBar.HORIZONTAL)
                ? oper.getWidth() - lessButton.getWidth() - moreButton.getWidth()
                : oper.getHeight() - lessButton.getHeight() - moreButton.getHeight();
        int subpos = (int) (((float) lenght / (oper.getMaximum() - oper.getMinimum())) * value);
        if (oper.getOrientation() == JScrollBar.HORIZONTAL) {
            subpos = subpos + lessButton.getWidth();
        } else {
            subpos = subpos + lessButton.getHeight();
        }
        subpos = subpos + MINIMAL_DRAGGER_SIZE / 2 + 1;
        return ((oper.getOrientation() == JScrollBar.HORIZONTAL)
                ? new Point(subpos, oper.getHeight() / 2)
                : new Point(oper.getWidth() / 2, subpos));
    }

    private JButtonOperator findAButton(ComponentOperator oper, int direction) {
        return ((direction == ScrollAdjuster.DECREASE_SCROLL_DIRECTION)
                ? ((JScrollBarOperator) oper).getDecreaseButton()
                : ((JScrollBarOperator) oper).getIncreaseButton());
    }
}
