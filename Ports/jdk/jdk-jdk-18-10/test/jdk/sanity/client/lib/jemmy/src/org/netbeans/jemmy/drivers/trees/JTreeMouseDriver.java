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
package org.netbeans.jemmy.drivers.trees;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.text.JTextComponent;

import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.Timeout;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.LightSupportiveDriver;
import org.netbeans.jemmy.drivers.MouseDriver;
import org.netbeans.jemmy.drivers.TextDriver;
import org.netbeans.jemmy.drivers.TreeDriver;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.JTextComponentOperator;
import org.netbeans.jemmy.operators.JTreeOperator;
import org.netbeans.jemmy.operators.Operator;

/**
 * TreeDriver for javax.swing.JTree component type. Uses mouse operations.
 *
 * @author Alexandre Iline(alexandre.iline@oracle.com)
 */
public class JTreeMouseDriver extends LightSupportiveDriver implements TreeDriver {

    QueueTool queueTool;

    /**
     * Constructs a JTreeMouseDriver.
     */
    public JTreeMouseDriver() {
        super(new String[]{"org.netbeans.jemmy.operators.JTreeOperator"});
        queueTool = new QueueTool();
    }

    @Override
    public void selectItem(ComponentOperator oper, int index) {
        selectItems(oper, new int[]{index});
    }

    @Override
    public void selectItems(final ComponentOperator oper, int[] indices) {
        ((JTreeOperator) oper).clearSelection();
        checkSupported(oper);
        final MouseDriver mdriver = DriverManager.getMouseDriver(oper);
        final JTreeOperator toper = (JTreeOperator) oper;
        final Timeout clickTime = oper.getTimeouts().create("ComponentOperator.MouseClickTimeout");
        for (int i = 0; i < indices.length; i++) {
            final int index = i;
            if (!QueueTool.isDispatchThread()) {
                toper.scrollToRow(indices[i]);
            }
            final Point p = toper.getPointToClick(indices[index]);
            queueTool.invokeSmoothly(new QueueTool.QueueAction<Void>("Path selecting") {
                @Override
                public Void launch() {
                    mdriver.clickMouse(oper, p.x, p.y, 1, Operator.getDefaultMouseButton(),
                            (index == 0) ? 0 : InputEvent.CTRL_MASK, clickTime);
                    return null;
                }
            });
        }
        //1.5 workaround
        if (System.getProperty("java.specification.version").compareTo("1.4") > 0) {
            if (!QueueTool.isDispatchThread()) {
                queueTool.setOutput(oper.getOutput().createErrorOutput());
                queueTool.waitEmpty(10);
                queueTool.waitEmpty(10);
                queueTool.waitEmpty(10);
            }
        }
        //end of 1.5 workaround
    }

    @Override
    public void expandItem(ComponentOperator oper, final int index) {
        checkSupported(oper);
        final JTreeOperator toper = (JTreeOperator) oper;
        final MouseDriver mdriver = DriverManager.getMouseDriver(oper);
        if (!toper.isExpanded(index)) {
            queueTool.invokeSmoothly(new QueueTool.QueueAction<Void>("Path selecting") {
                @Override
                public Void launch() {
                    Point p = toper.getPointToClick(index);
                    mdriver.clickMouse(toper, p.x, p.y, 2, Operator.getDefaultMouseButton(),
                            0, toper.getTimeouts().
                            create("ComponentOperator.MouseClickTimeout"));
                    return null;
                }
            });
        }
    }

    @Override
    public void collapseItem(ComponentOperator oper, final int index) {
        checkSupported(oper);
        final JTreeOperator toper = (JTreeOperator) oper;
        final MouseDriver mdriver = DriverManager.getMouseDriver(oper);
        if (toper.isExpanded(index)) {
            queueTool.invokeSmoothly(new QueueTool.QueueAction<Void>("Path selecting") {
                @Override
                public Void launch() {
                    Point p = toper.getPointToClick(index);
                    mdriver.clickMouse(toper, p.x, p.y, 2, Operator.getDefaultMouseButton(),
                            0, toper.getTimeouts().
                            create("ComponentOperator.MouseClickTimeout"));
                    return null;
                }
            });
        }
    }

    @Override
    public void editItem(ComponentOperator oper, int index, Object newValue, Timeout waitEditorTime) {
        JTextComponentOperator textoper = startEditingAndReturnEditor(oper, index, waitEditorTime);
        final TextDriver text = DriverManager.getTextDriver(JTextComponentOperator.class);
        text.clearText(textoper);
        text.typeText(textoper, newValue.toString(), 0);
        DriverManager.getKeyDriver(oper).
                pushKey(textoper, KeyEvent.VK_ENTER, 0,
                        oper.getTimeouts().
                        create("ComponentOperator.PushKeyTimeout"));
    }

    @Override
    public void startEditing(ComponentOperator oper, int index, Timeout waitEditorTime) {
        startEditingAndReturnEditor(oper, index, waitEditorTime);
    }

    private JTextComponentOperator startEditingAndReturnEditor(ComponentOperator oper, final int index, Timeout waitEditorTime) {
        checkSupported(oper);
        final JTreeOperator toper = (JTreeOperator) oper;
        final MouseDriver mdriver = DriverManager.getMouseDriver(oper);
        queueTool.invokeSmoothly(new QueueTool.QueueAction<Void>("Path selecting") {
            @Override
            public Void launch() {
                Point p = toper.getPointToClick(index);
                mdriver.clickMouse(toper, p.x, p.y, 1, Operator.getDefaultMouseButton(),
                        0, toper.getTimeouts().
                        create("ComponentOperator.MouseClickTimeout"));
                return null;
            }
        });
        oper.getTimeouts().sleep("JTreeOperator.BeforeEditTimeout");
        queueTool.invokeSmoothly(new QueueTool.QueueAction<Void>("Path selecting") {
            @Override
            public Void launch() {
                Point p = toper.getPointToClick(index);
                mdriver.clickMouse(toper, p.x, p.y, 1, Operator.getDefaultMouseButton(),
                        0, toper.getTimeouts().
                        create("ComponentOperator.MouseClickTimeout"));
                return null;
            }
        });
        toper.getTimeouts().
                setTimeout("ComponentOperator.WaitComponentTimeout", waitEditorTime.getValue());
        return (new JTextComponentOperator((JTextComponent) toper.
                waitSubComponent(new JTextComponentOperator.JTextComponentFinder())));
    }
}
