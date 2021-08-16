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
import java.awt.event.KeyEvent;

import org.netbeans.jemmy.Timeout;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.KeyDriver;
import org.netbeans.jemmy.drivers.LightSupportiveDriver;
import org.netbeans.jemmy.drivers.ListDriver;
import org.netbeans.jemmy.operators.ChoiceOperator;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.Operator;

/**
 * List driver for java.awt.Choice component type.
 *
 * @author Alexandre Iline(alexandre.iline@oracle.com)
 */
public class ChoiceDriver extends LightSupportiveDriver implements ListDriver {

    private final static int RIGHT_INDENT = 10;

    /**
     * Constructs a ChoiceDriver.
     */
    public ChoiceDriver() {
        super(new String[]{"org.netbeans.jemmy.operators.ChoiceOperator"});
    }

    @Override
    public void selectItem(ComponentOperator oper, int index) {
        ChoiceOperator coper = (ChoiceOperator) oper;
        Point pointToClick = getClickPoint(oper);
        DriverManager.getMouseDriver(oper).
                clickMouse(oper, pointToClick.x, pointToClick.y,
                        1, Operator.getDefaultMouseButton(), 0,
                        oper.getTimeouts().create("ComponentOperator.MouseClickTimeout"));
        KeyDriver kdriver = DriverManager.getKeyDriver(oper);
        Timeout pushTimeout = oper.getTimeouts().create("ComponentOperator.PushKeyTimeout");
        if (System.getProperty("java.specification.version").compareTo("1.3") > 0) {
            while (coper.getSelectedIndex() != index) {
                kdriver.pushKey(oper, (index > coper.getSelectedIndex()) ? KeyEvent.VK_DOWN : KeyEvent.VK_UP, 0, pushTimeout);
            }
        } else {
            int current = ((ChoiceOperator) oper).getSelectedIndex();
            int diff = 0;
            int key = 0;
            if (index > current) {
                diff = index - current;
                key = KeyEvent.VK_DOWN;
            } else {
                diff = current - index;
                key = KeyEvent.VK_UP;
            }
            for (int i = 0; i < diff; i++) {
                kdriver.pushKey(oper, key, 0, pushTimeout);
            }
        }
        kdriver.pushKey(oper, KeyEvent.VK_ENTER, 0, pushTimeout);
    }

    private Point getClickPoint(ComponentOperator oper) {
        return new Point(oper.getWidth() - RIGHT_INDENT, oper.getHeight() / 2);
    }
}
