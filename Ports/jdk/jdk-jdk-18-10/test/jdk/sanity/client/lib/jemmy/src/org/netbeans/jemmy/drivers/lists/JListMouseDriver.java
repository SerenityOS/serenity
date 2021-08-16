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

import java.awt.Rectangle;
import java.awt.event.InputEvent;

import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.LightSupportiveDriver;
import org.netbeans.jemmy.drivers.MultiSelListDriver;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.JListOperator;
import org.netbeans.jemmy.operators.Operator;

/**
 * List driver for javax.swing.JList component type.
 *
 * @author Alexandre Iline(alexandre.iline@oracle.com)
 */
public class JListMouseDriver extends LightSupportiveDriver implements MultiSelListDriver {

    QueueTool queueTool;

    /**
     * Constructs a JListMouseDriver.
     */
    public JListMouseDriver() {
        super(new String[]{"org.netbeans.jemmy.operators.JListOperator"});
        queueTool = new QueueTool();
    }

    @Override
    public void selectItem(ComponentOperator oper, int index) {
        clickOnItem((JListOperator) oper, index);
    }

    @Override
    public void selectItems(ComponentOperator oper, int[] indices) {
        clickOnItem((JListOperator) oper, indices[0]);
        for (int i = 1; i < indices.length; i++) {
            clickOnItem((JListOperator) oper, indices[i], InputEvent.CTRL_MASK);
        }
    }

    /**
     * Clicks on a list item.
     *
     * @param oper an operator to click on.
     * @param index item index.
     */
    protected void clickOnItem(JListOperator oper, int index) {
        clickOnItem(oper, index, 0);
    }

    /**
     * Clicks on a list item.
     *
     * @param oper an operator to click on.
     * @param index item index.
     * @param modifiers a combination of {@code InputEvent.*_MASK} fields.
     */
    protected void clickOnItem(final JListOperator oper, final int index, final int modifiers) {
        if (!QueueTool.isDispatchThread()) {
            oper.scrollToItem(index);
        }
        queueTool.invokeSmoothly(new QueueTool.QueueAction<Void>("Path selecting") {
            @Override
            public Void launch() {
                Rectangle rect = oper.getCellBounds(index, index);
                DriverManager.getMouseDriver(oper).
                        clickMouse(oper,
                                rect.x + rect.width / 2,
                                rect.y + rect.height / 2,
                                1, Operator.getDefaultMouseButton(), modifiers,
                                oper.getTimeouts().create("ComponentOperator.MouseClickTimeout"));
                return null;
            }
        });
    }
}
