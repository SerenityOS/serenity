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

import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.Timeout;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.MouseDriver;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.Operator;

/**
 * ScrollDriver for awt components.
 *
 * @author Alexandre Iline(alexandre.iline@oracle.com)
 */
public abstract class AWTScrollDriver extends AbstractScrollDriver {

    private QueueTool queueTool;

    /**
     * Constructs a ChoiceDriver.
     *
     * @param supported an array of supported class names
     */
    public AWTScrollDriver(String[] supported) {
        super(supported);
        queueTool = new QueueTool();
    }

    @Override
    protected void step(final ComponentOperator oper, final ScrollAdjuster adj) {
        if (adj.getScrollDirection() != ScrollAdjuster.DO_NOT_TOUCH_SCROLL_DIRECTION) {
            queueTool.invokeSmoothly(new QueueTool.QueueAction<Void>("Scrolling by clicking with mouse") {
                @Override
                public Void launch() {
                    Point clickPoint = getClickPoint(oper, adj.getScrollDirection(), adj.getScrollOrientation());
                    if (clickPoint != null) {
                        DriverManager.getMouseDriver(oper).
                                clickMouse(oper, clickPoint.x, clickPoint.y, 1,
                                        Operator.getDefaultMouseButton(),
                                        0,
                                        oper.getTimeouts().
                                        create("ComponentOperator.MouseClickTimeout"));
                    }
                    return null;
                }
            });
        }
    }

    @Override
    protected void jump(ComponentOperator oper, ScrollAdjuster adj) {
    }

    @Override
    protected void startPushAndWait(final ComponentOperator oper, final int direction, final int orientation) {
        queueTool.invokeSmoothly(new QueueTool.QueueAction<Void>("Scrolling: pressing the mouse button and holding") {
            @Override
            public Void launch() {
                Point clickPoint = getClickPoint(oper, direction, orientation);
                if (clickPoint != null) {
                    MouseDriver mdriver = DriverManager.getMouseDriver(oper);
                    mdriver.moveMouse(oper, clickPoint.x, clickPoint.y);
                    mdriver.pressMouse(oper, clickPoint.x, clickPoint.y,
                            Operator.getDefaultMouseButton(),
                            0);
                }
                return null;
            }
        });
    }

    @Override
    protected void stopPushAndWait(final ComponentOperator oper, final int direction, final int orientation) {
        queueTool.invokeSmoothly(new QueueTool.QueueAction<Void>("Scrolling: releasing the mouse button") {
            @Override
            public Void launch() {
                Point clickPoint = getClickPoint(oper, direction, orientation);
                if (clickPoint != null) {
                    MouseDriver mdriver = DriverManager.getMouseDriver(oper);
                    mdriver.releaseMouse(oper, clickPoint.x, clickPoint.y,
                            Operator.getDefaultMouseButton(),
                            0);
                }
                return null;
            }
        });
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
    protected Timeout getScrollDeltaTimeout(ComponentOperator oper) {
        return (oper.getTimeouts().
                create("ScrollbarOperator.DragAndDropScrollingDelta"));
    }

    @Override
    protected boolean canDragAndDrop(ComponentOperator oper) {
        return false;
    }

    @Override
    protected boolean canJump(ComponentOperator oper) {
        return false;
    }

    @Override
    protected boolean canPushAndWait(ComponentOperator oper) {
        return true;
    }

    @Override
    protected int getDragAndDropStepLength(ComponentOperator oper) {
        return 1;
    }

    /**
     * Defines a click point which needs to be used in order to
     * increase/decrease scroller value.
     *
     * @param oper an operator.
     * @param direction - one of the ScrollAdjister.INCREASE_SCROLL_DIRECTION,
     * ScrollAdjister.DECREASE_SCROLL_DIRECTION,
     * ScrollAdjister.DO_NOT_TOUCH_SCROLL_DIRECTION values.
     * @param orientation one of the Adjustable.HORIZONTAL or
     * Adjustable.VERTICAL values.
     * @return a point to click.
     */
    protected abstract Point getClickPoint(ComponentOperator oper, int direction, int orientation);
}
