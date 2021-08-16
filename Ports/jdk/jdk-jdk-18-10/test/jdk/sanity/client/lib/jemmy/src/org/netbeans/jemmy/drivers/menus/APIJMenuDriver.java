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
package org.netbeans.jemmy.drivers.menus;

import java.awt.Component;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.JemmyException;
import org.netbeans.jemmy.drivers.MenuDriver;
import org.netbeans.jemmy.drivers.PathChooser;
import org.netbeans.jemmy.operators.AbstractButtonOperator;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.JMenuItemOperator;
import org.netbeans.jemmy.operators.JMenuOperator;

public class APIJMenuDriver extends DefaultJMenuDriver implements MenuDriver {

    public APIJMenuDriver() {
        super();
    }

    protected Object push(ComponentOperator oper, JMenuBar menuBar,
            PathChooser chooser, int depth, boolean pressMouse) {
        try {
            oper.waitComponentVisible(true);
            oper.waitComponentEnabled();
        } catch (InterruptedException e) {
            throw (new JemmyException("Interrupted!", e));
        }
        if (depth > chooser.getDepth() - 1) {
            if (oper instanceof JMenuOperator) {
                if (((JMenuOperator) oper).isPopupMenuVisible()) {
                    ((JMenuOperator) oper).setPopupMenuVisible(false);
                }
                ((JMenuOperator) oper).setPopupMenuVisible(true);
                waitPopupMenu(oper);
            }
            ((AbstractButtonOperator) oper).doClick();
            return oper.getSource();
        } else {
            if (((JMenuOperator) oper).isPopupMenuVisible()) {
                ((JMenuOperator) oper).setPopupMenuVisible(false);
            }
            ((JMenuOperator) oper).setPopupMenuVisible(true);
            waitPopupMenu(oper);
        }
        oper.getTimeouts().sleep("JMenuOperator.WaitBeforePopupTimeout");
        JMenuItem item = waitItem(oper, waitPopupMenu(oper), chooser, depth);
        if (item instanceof JMenu) {
            JMenuOperator mo = new JMenuOperator((JMenu) item);
            mo.copyEnvironment(oper);
            Object result = push(mo, null, chooser, depth + 1, false);
            if (result instanceof JMenu) {
                org.netbeans.jemmy.JemmyProperties.getCurrentOutput().printLine("IN HERE" + ((JMenu) result).getText());
                org.netbeans.jemmy.JemmyProperties.getCurrentOutput().printLine("IN HERE" + Boolean.toString(((JMenu) result).isPopupMenuVisible()));
                if (!((JMenu) result).isPopupMenuVisible()) {
                    ((JMenuOperator) oper).setPopupMenuVisible(false);
                }
            } else {
                ((JMenuOperator) oper).setPopupMenuVisible(false);
                waitNoPopupMenu(oper);
            }
            return result;
        } else {
            JMenuItemOperator mio = new JMenuItemOperator(item);
            mio.copyEnvironment(oper);
            try {
                mio.waitComponentEnabled();
            } catch (InterruptedException e) {
                throw (new JemmyException("Interrupted!", e));
            }
            mio.doClick();
            ((JMenuOperator) oper).setPopupMenuVisible(false);
            waitNoPopupMenu(oper);
            return item;
        }
    }

    protected void waitNoPopupMenu(final ComponentOperator oper) {
        oper.waitState(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                return !((JMenuOperator) oper).isPopupMenuVisible();
            }

            @Override
            public String getDescription() {
                return ((JMenuOperator) oper).getText() + "'s popup";
            }

            @Override
            public String toString() {
                return "waitNoPopupMenu.ComponentChooser{description = " + getDescription() + '}';
            }
        });
    }

}
