/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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

package com.sun.java.swing.plaf.windows;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JRootPane;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicMenuBarUI;

import com.sun.java.swing.plaf.windows.TMSchema.Part;
import com.sun.java.swing.plaf.windows.TMSchema.State;
import com.sun.java.swing.plaf.windows.XPStyle.Skin;

/**
 * Windows rendition of the component.
 */
public class WindowsMenuBarUI extends BasicMenuBarUI
{
    /* to be accessed on the EDT only */
    private WindowListener windowListener = null;
    private HierarchyListener hierarchyListener = null;
    private Window window = null;

    public static ComponentUI createUI(JComponent x) {
        return new WindowsMenuBarUI();
    }

    @Override
    protected void uninstallListeners() {
        uninstallWindowListener();
        if (hierarchyListener != null) {
            menuBar.removeHierarchyListener(hierarchyListener);
            hierarchyListener = null;
        }
        super.uninstallListeners();
    }
    private void installWindowListener() {
        if (windowListener == null) {
            Component component = menuBar.getTopLevelAncestor();
            if (component instanceof Window) {
                window = (Window) component;
                windowListener = new WindowAdapter() {
                    @Override
                    public void windowActivated(WindowEvent e) {
                        menuBar.repaint();
                    }
                    @Override
                    public void windowDeactivated(WindowEvent e) {
                        menuBar.repaint();
                    }
                };
                ((Window) component).addWindowListener(windowListener);
            }
        }
    }
    private void uninstallWindowListener() {
        if (windowListener != null && window != null) {
            window.removeWindowListener(windowListener);
        }
        window = null;
        windowListener = null;
    }
    @Override
    protected void installListeners() {
        if (WindowsLookAndFeel.isOnVista()) {
            installWindowListener();
            hierarchyListener =
                new HierarchyListener() {
                    public void hierarchyChanged(HierarchyEvent e) {
                        if ((e.getChangeFlags()
                                & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0) {
                            if (menuBar.isDisplayable()) {
                                installWindowListener();
                            } else {
                                uninstallWindowListener();
                            }
                        }
                    }
            };
            menuBar.addHierarchyListener(hierarchyListener);
        }
        super.installListeners();
    }

    protected void installKeyboardActions() {
        super.installKeyboardActions();
        ActionMap map = SwingUtilities.getUIActionMap(menuBar);
        if (map == null) {
            map = new ActionMapUIResource();
            SwingUtilities.replaceUIActionMap(menuBar, map);
        }
        map.put("takeFocus", new TakeFocus());
    }

    /**
     * Action that activates the menu (e.g. when F10 is pressed).
     * Unlike BasicMenuBarUI.TakeFocus, this Action will not show menu popup.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private static class TakeFocus extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            JMenuBar menuBar = (JMenuBar)e.getSource();
            JMenu menu = menuBar.getMenu(0);
            if (menu != null) {
                MenuSelectionManager msm =
                    MenuSelectionManager.defaultManager();
                MenuElement[] path = new MenuElement[2];
                path[0] = (MenuElement)menuBar;
                path[1] = (MenuElement)menu;
                msm.setSelectedPath(path);

                // show mnemonics
                WindowsLookAndFeel.setMnemonicHidden(false);
                WindowsLookAndFeel.repaintRootPane(menuBar);
            }
        }
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        XPStyle xp = XPStyle.getXP();
        if (WindowsMenuItemUI.isVistaPainting(xp)) {
            Skin skin;
            skin = xp.getSkin(c, Part.MP_BARBACKGROUND);
            int width = c.getWidth();
            int height = c.getHeight();
            State state =  isActive(c) ? State.ACTIVE : State.INACTIVE;
            skin.paintSkin(g, 0, 0, width, height, state);
        } else {
            super.paint(g, c);
        }
    }

    /**
     * Checks if component belongs to an active window.
     * @param c component to check
     * @return true if component belongs to an active window
     */
    static boolean isActive(JComponent c) {
        JRootPane rootPane = c.getRootPane();
        if (rootPane != null) {
            Component component = rootPane.getParent();
            if (component instanceof Window) {
                return ((Window) component).isActive();
            }
        }
        return true;
    }
}
