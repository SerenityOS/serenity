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
package org.netbeans.jemmy.drivers.lists;

import java.awt.Point;
import java.awt.event.InputEvent;

import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.LightSupportiveDriver;
import org.netbeans.jemmy.drivers.OrderedListDriver;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.JTableHeaderOperator;
import org.netbeans.jemmy.operators.Operator;

/**
 * List driver for javax.swing.table.JTableHeader component type.
 *
 * @author Alexandre Iline(alexandre.iline@oracle.com)
 */
public class JTableHeaderDriver extends LightSupportiveDriver implements OrderedListDriver {

    private QueueTool queueTool;

    /**
     * Constructs a JTableHeaderDriver.
     */
    public JTableHeaderDriver() {
        super(new String[]{"org.netbeans.jemmy.operators.JTableHeaderOperator"});
        queueTool = new QueueTool();
    }

    @Override
    public void selectItem(ComponentOperator oper, int index) {
        clickOnHeader((JTableHeaderOperator) oper, index);
    }

    @Override
    public void selectItems(ComponentOperator oper, int[] indices) {
        clickOnHeader((JTableHeaderOperator) oper, indices[0]);
        for (int i = 1; i < indices.length; i++) {
            clickOnHeader((JTableHeaderOperator) oper, indices[i], InputEvent.CTRL_MASK);
        }
    }

    @Override
    public void moveItem(ComponentOperator oper, int moveColumn, int moveTo) {
        Point start = ((JTableHeaderOperator) oper).getPointToClick(moveColumn);
        Point end = ((JTableHeaderOperator) oper).getPointToClick(moveTo);
        oper.dragNDrop(start.x, start.y, end.x, end.y);
    }

    /**
     * Clicks on a column header.
     *
     * @param oper an operator to click on.
     * @param index column index.
     */
    protected void clickOnHeader(JTableHeaderOperator oper, int index) {
        clickOnHeader(oper, index, 0);
    }

    /**
     * Clicks on a column header.
     *
     * @param oper an operator to click on.
     * @param index column index.
     * @param modifiers a combination of {@code InputEvent.*_MASK} fields.
     */
    protected void clickOnHeader(final JTableHeaderOperator oper, final int index, final int modifiers) {
        queueTool.invokeSmoothly(new QueueTool.QueueAction<Void>("Column selecting") {
            @Override
            public Void launch() {
                Point toClick = oper.getPointToClick(index);
                DriverManager.getMouseDriver(oper).
                        clickMouse(oper,
                                toClick.x,
                                toClick.y,
                                1, Operator.getDefaultMouseButton(), modifiers,
                                oper.getTimeouts().create("ComponentOperator.MouseClickTimeout"));
                return null;
            }
        });
    }
}
