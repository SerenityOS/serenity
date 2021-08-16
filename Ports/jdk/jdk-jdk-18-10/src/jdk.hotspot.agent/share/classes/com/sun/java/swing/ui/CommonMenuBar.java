/*
 * Copyright (c) 2000, 2008, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 *
 */


package com.sun.java.swing.ui;

import com.sun.java.swing.action.ActionManager;
import com.sun.java.swing.action.StateChangeAction;
import javax.swing.*;

// Referenced classes of package com.sun.java.swing.ui:
//            ToggleActionPropertyChangeListener, StatusBar

public abstract class CommonMenuBar extends JMenuBar
{

    protected CommonMenuBar(ActionManager manager)
    {
        this(manager, StatusBar.getInstance());
    }

    protected CommonMenuBar(ActionManager manager, StatusBar status)
    {
        this.manager = manager;
        statusBar = status;
        configureMenu();
    }

    protected abstract void configureMenu();

    protected void configureToggleMenuItem(JMenuItem menuItem, Action action)
    {
        configureMenuItem(menuItem, action);
        action.addPropertyChangeListener(new ToggleActionPropertyChangeListener(menuItem));
    }

    protected void configureMenuItem(JMenuItem menuItem, Action action)
    {
        menuItem.addMouseListener(statusBar);
    }

    protected JMenu createMenu(String name, char mnemonic)
    {
        JMenu menu = new JMenu(name);
        menu.setMnemonic(mnemonic);
        return menu;
    }

    protected void addMenuItem(JMenu menu, Action action)
    {
        JMenuItem menuItem = menu.add(action);
        configureMenuItem(menuItem, action);
    }

    protected void addCheckBoxMenuItem(JMenu menu, StateChangeAction a)
    {
        addCheckBoxMenuItem(menu, a, false);
    }

    protected void addCheckBoxMenuItem(JMenu menu, StateChangeAction a, boolean selected)
    {
        JCheckBoxMenuItem mi = new JCheckBoxMenuItem(a);
        mi.addItemListener(a);
        mi.setSelected(selected);
        menu.add(mi);
        configureToggleMenuItem(mi, a);
    }

    protected void addRadioButtonMenuItem(JMenu menu, ButtonGroup group, StateChangeAction a)
    {
        addRadioButtonMenuItem(menu, group, a, false);
    }

    protected void addRadioButtonMenuItem(JMenu menu, ButtonGroup group, StateChangeAction a, boolean selected)
    {
        JRadioButtonMenuItem mi = new JRadioButtonMenuItem(a);
        mi.addItemListener(a);
        mi.setSelected(selected);
        menu.add(mi);
        if(group != null)
            group.add(mi);
        configureToggleMenuItem(mi, a);
    }

    protected ActionManager manager;
    private StatusBar statusBar;
}
