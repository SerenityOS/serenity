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

import java.awt.event.KeyEvent;

import org.netbeans.jemmy.Timeout;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.KeyDriver;
import org.netbeans.jemmy.drivers.MultiSelListDriver;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.ListOperator;

/**
 * List driver for java.awt.List component type. Uses keyboard and mouse.
 *
 * @author Alexandre Iline(alexandre.iline@oracle.com)
 */
public class ListKeyboardDriver extends ListAPIDriver implements MultiSelListDriver {

    /**
     * Constructs a ListKeyboardDriver.
     */
    public ListKeyboardDriver() {
        super();
    }

    @Override
    public void selectItem(ComponentOperator oper, int index) {
        ListOperator loper = (ListOperator) oper;
        if (loper.isMultipleMode()) {
            super.selectItem(loper, index);
        }
        DriverManager.getFocusDriver(oper).giveFocus(oper);
        KeyDriver kDriver = DriverManager.getKeyDriver(oper);
        int current = loper.getSelectedIndex();
        int diff = 0;
        int key = 0;
        if (index > current) {
            diff = index - current;
            key = KeyEvent.VK_DOWN;
        } else {
            diff = current - index;
            key = KeyEvent.VK_UP;
        }
        Timeout pushTime = oper.getTimeouts().create("ComponentOperator.PushKeyTimeout");
        for (int i = 0; i < diff; i++) {
            kDriver.pushKey(oper, key, 0, pushTime);
        }
        kDriver.pushKey(oper, KeyEvent.VK_ENTER, 0, pushTime);
    }
}
