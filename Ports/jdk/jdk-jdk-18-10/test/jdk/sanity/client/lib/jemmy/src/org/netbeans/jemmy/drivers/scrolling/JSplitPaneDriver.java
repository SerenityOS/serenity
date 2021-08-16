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

import javax.swing.JButton;
import javax.swing.JSplitPane;

import org.netbeans.jemmy.ComponentSearcher;
import org.netbeans.jemmy.drivers.ButtonDriver;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.LightSupportiveDriver;
import org.netbeans.jemmy.drivers.ScrollDriver;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.ContainerOperator;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JSplitPaneOperator;
import org.netbeans.jemmy.operators.Operator;

/**
 * ScrollDriver for javax.swing.JSplitPane component type.
 *
 * @author Alexandre Iline(alexandre.iline@oracle.com)
 */
public class JSplitPaneDriver extends LightSupportiveDriver implements ScrollDriver {

    /**
     * Constructs a JSplitPaneDriver.
     */
    public JSplitPaneDriver() {
        super(new String[]{"org.netbeans.jemmy.operators.JSplitPaneOperator"});
    }

    @Override
    public void scroll(ComponentOperator oper, ScrollAdjuster adj) {
        moveDividerTo((JSplitPaneOperator) oper, adj);
    }

    @Override
    public void scrollToMinimum(ComponentOperator oper, int orientation) {
        expandTo((JSplitPaneOperator) oper, 0);
    }

    @Override
    public void scrollToMaximum(ComponentOperator oper, int orientation) {
        expandTo((JSplitPaneOperator) oper, 1);
    }

    private void moveDividerTo(JSplitPaneOperator oper, ScrollAdjuster adj) {
        ContainerOperator<?> divOper = oper.getDivider();
        /* workaround */
        if (oper.getDividerLocation() == -1) {
            moveTo(oper, divOper, divOper.getCenterX() - 1, divOper.getCenterY() - 1);
            if (oper.getDividerLocation() == -1) {
                moveTo(oper, divOper, divOper.getCenterX() + 1, divOper.getCenterY() + 1);
            }
        }
        if (oper.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
            moveOnce(oper, divOper, adj, 0, oper.getWidth());
        } else {
            moveOnce(oper, divOper, adj, 0, oper.getHeight());
        }
    }

    private void moveOnce(JSplitPaneOperator oper,
            ContainerOperator<?> divOper,
            ScrollAdjuster adj,
            int leftPosition,
            int rightPosition) {
        int currentPosition = 0;
        if (oper.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
            currentPosition = (int) (divOper.getLocationOnScreen().getX()
                    - oper.getLocationOnScreen().getX());
        } else {
            currentPosition = (int) (divOper.getLocationOnScreen().getY()
                    - oper.getLocationOnScreen().getY());
        }
        int nextPosition = 0;
        if (adj.getScrollDirection() == ScrollAdjuster.DECREASE_SCROLL_DIRECTION) {
            nextPosition = (currentPosition + leftPosition) / 2;
            moveToPosition(oper, divOper, nextPosition - currentPosition);
            if (currentPosition
                    == ((adj.getScrollOrientation() == JSplitPane.HORIZONTAL_SPLIT)
                            ? (int) (divOper.getLocationOnScreen().getX()
                            - oper.getLocationOnScreen().getX())
                            : (int) (divOper.getLocationOnScreen().getY()
                            - oper.getLocationOnScreen().getY()))) {
                return;
            }
            moveOnce(oper, divOper, adj, leftPosition, currentPosition);
        } else if (adj.getScrollDirection() == ScrollAdjuster.INCREASE_SCROLL_DIRECTION) {
            nextPosition = (currentPosition + rightPosition) / 2;
            moveToPosition(oper, divOper, nextPosition - currentPosition);
            if (currentPosition
                    == ((adj.getScrollOrientation() == JSplitPane.HORIZONTAL_SPLIT)
                            ? (int) (divOper.getLocationOnScreen().getX()
                            - oper.getLocationOnScreen().getX())
                            : (int) (divOper.getLocationOnScreen().getY()
                            - oper.getLocationOnScreen().getY()))) {
                return;
            }
            moveOnce(oper, divOper, adj, currentPosition, rightPosition);
        } else { // (currentLocation == dividerLocation) - stop point
            return;
        }
    }

    private void moveTo(JSplitPaneOperator oper, ComponentOperator divOper, int x, int y) {
        DriverManager.getMouseDriver(divOper).
                dragNDrop(divOper, divOper.getCenterX(), divOper.getCenterY(), x, y,
                        Operator.getDefaultMouseButton(), 0,
                        oper.getTimeouts().create("ComponentOperator.BeforeDragTimeout"),
                        oper.getTimeouts().create("ComponentOperator.AfterDragTimeout"));
    }

    private void moveToPosition(JSplitPaneOperator oper, ComponentOperator divOper, int nextPosition) {
        if (System.getProperty("java.version").startsWith("1.2")) {
            oper.setDividerLocation(nextPosition);
        }
        if (oper.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
            moveTo(oper, divOper, divOper.getCenterX() + nextPosition, divOper.getCenterY());
        } else {
            moveTo(oper, divOper, divOper.getCenterX(), divOper.getCenterY() + nextPosition);
        }
    }

    private void expandTo(JSplitPaneOperator oper, int index) {
        ContainerOperator<?> divOper = oper.getDivider();
        JButtonOperator bo
                = new JButtonOperator((JButton) divOper.
                        waitSubComponent(new JButtonOperator.JButtonFinder(ComponentSearcher.
                                getTrueChooser("JButton")),
                                index));
        bo.copyEnvironment(divOper);
        ButtonDriver bdriver = DriverManager.getButtonDriver(bo);
        bdriver.push(bo);
        bdriver.push(bo);
    }
}
