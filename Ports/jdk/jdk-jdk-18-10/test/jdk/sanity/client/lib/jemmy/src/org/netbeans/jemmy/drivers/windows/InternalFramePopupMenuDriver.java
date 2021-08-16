/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.jemmy.drivers.windows;

import javax.swing.UIManager;

import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.JInternalFrameOperator;
import org.netbeans.jemmy.operators.JMenuItemOperator;
import org.netbeans.jemmy.operators.JPopupMenuOperator;

/**
 * InternalFrameDriver to do Close, Minimize, Maximize, and Restore actions
 * using popup menus.
 */
public class InternalFramePopupMenuDriver extends DefaultInternalFrameDriver {

    @Override
    public void requestClose(ComponentOperator oper) {
        checkSupported(oper);
        pushMenuItem(oper, UIManager.getString(
                "InternalFrameTitlePane.closeButtonText"));
    }

    @Override
    public void iconify(ComponentOperator oper) {
        checkSupported(oper);
        pushMenuItem(oper, UIManager.getString(
                "InternalFrameTitlePane.minimizeButtonText"));
    }

    @Override
    public void maximize(ComponentOperator oper) {
        checkSupported(oper);
        if (!((JInternalFrameOperator) oper).isMaximum()) {
            if (!((JInternalFrameOperator) oper).isSelected()) {
                activate(oper);
            }
            pushMenuItem(oper, UIManager.getString(
                    "InternalFrameTitlePane.maximizeButtonText"));
        }
    }

    @Override
    public void demaximize(ComponentOperator oper) {
        checkSupported(oper);
        if (((JInternalFrameOperator) oper).isMaximum()) {
            if (!((JInternalFrameOperator) oper).isSelected()) {
                activate(oper);
            }
            pushMenuItem(oper, UIManager.getString(
                    "InternalFrameTitlePane.restoreButtonText"));
        }
    }

    private void pushMenuItem(ComponentOperator oper,
            String menuText) {
        ((JInternalFrameOperator) oper).getPopupButton().push();
        JPopupMenuOperator popupMenu = new JPopupMenuOperator();
        JMenuItemOperator menuItem =
                new JMenuItemOperator(popupMenu, menuText);
        menuItem.push();
    }
}