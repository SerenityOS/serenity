/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.apple.laf;

import java.awt.*;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicMenuUI;

public class AquaMenuUI extends BasicMenuUI implements AquaMenuPainter.Client {
    public static ComponentUI createUI(final JComponent x) {
        return new AquaMenuUI();
    }

    protected ChangeListener createChangeListener(final JComponent c) {
        return new ChangeHandler((JMenu)c, this);
    }

    protected void installDefaults() {
        super.installDefaults();

        // [3361625]
        // In Aqua, the menu delay is 8 ticks, according to Eric Schlegel.
        // That makes the millisecond delay 8 ticks * 1 second / 60 ticks * 1000 milliseconds/second
        ((JMenu)menuItem).setDelay(8 * 1000 / 60);
    }

    protected void paintMenuItem(final Graphics g, final JComponent c, final Icon localCheckIcon, final Icon localArrowIcon, final Color background, final Color foreground, final int localDefaultTextIconGap) {
        AquaMenuPainter.instance().paintMenuItem(this, g, c, localCheckIcon, localArrowIcon, background, foreground, disabledForeground, selectionForeground, localDefaultTextIconGap, acceleratorFont);
    }

    protected Dimension getPreferredMenuItemSize(final JComponent c, final Icon localCheckIcon, final Icon localArrowIcon, final int localDefaultTextIconGap) {
        final Dimension d = AquaMenuPainter.instance().getPreferredMenuItemSize(c, localCheckIcon, localArrowIcon, localDefaultTextIconGap, acceleratorFont);
        if (c.getParent() instanceof JMenuBar) d.height = Math.max(d.height, 21);
        return d;
    }

    public void paintBackground(final Graphics g, final JComponent c, final int menuWidth, final int menuHeight) {
        final Container parent = c.getParent();
        final boolean parentIsMenuBar = parent instanceof JMenuBar;

        final ButtonModel model = ((JMenuItem)c).getModel();
        if (model.isArmed() || model.isSelected()) {
            if (parentIsMenuBar) {
                AquaMenuPainter.instance().paintSelectedMenuTitleBackground(g, menuWidth, menuHeight);
            } else {
                AquaMenuPainter.instance().paintSelectedMenuItemBackground(g, menuWidth, menuHeight);
            }
        } else {
            if (parentIsMenuBar) {
                AquaMenuPainter.instance().paintMenuBarBackground(g, menuWidth, menuHeight, c);
            } else {
                g.setColor(c.getBackground());
                g.fillRect(0, 0, menuWidth, menuHeight);
            }
        }
    }

    protected MouseInputListener createMouseInputListener(final JComponent c) {
        return new AquaMouseInputHandler();
    }

    protected MenuDragMouseListener createMenuDragMouseListener(final JComponent c) {
        //return super.createMenuDragMouseListener(c);
        return new MenuDragMouseHandler();
    }

    class MenuDragMouseHandler implements MenuDragMouseListener {
        public void menuDragMouseDragged(final MenuDragMouseEvent e) {
            if (menuItem.isEnabled() == false) return;

            final MenuSelectionManager manager = e.getMenuSelectionManager();
            final MenuElement[] path = e.getPath();

            // In Aqua, we always respect the menu's delay, if one is set.
            // Doesn't matter how the menu is clicked on or otherwise moused over.
            final Point p = e.getPoint();
            if (p.x >= 0 && p.x < menuItem.getWidth() && p.y >= 0 && p.y < menuItem.getHeight()) {
                final JMenu menu = (JMenu)menuItem;
                final MenuElement[] selectedPath = manager.getSelectedPath();
                if (!(selectedPath.length > 0 && selectedPath[selectedPath.length - 1] == menu.getPopupMenu())) {
                    if (menu.getDelay() == 0) {
                        appendPath(path, menu.getPopupMenu());
                    } else {
                        manager.setSelectedPath(path);
                        setupPostTimer(menu);
                    }
                }
            } else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
                final Component comp = manager.componentForPoint(e.getComponent(), e.getPoint());
                if (comp == null) manager.clearSelectedPath();
            }
        }

        public void menuDragMouseEntered(final MenuDragMouseEvent e) { }
        public void menuDragMouseExited(final MenuDragMouseEvent e) { }
        public void menuDragMouseReleased(final MenuDragMouseEvent e) { }
    }

    static void appendPath(final MenuElement[] path, final MenuElement elem) {
        final MenuElement[] newPath = new MenuElement[path.length + 1];
        System.arraycopy(path, 0, newPath, 0, path.length);
        newPath[path.length] = elem;
        MenuSelectionManager.defaultManager().setSelectedPath(newPath);
    }

    protected class AquaMouseInputHandler extends MouseInputHandler {
        /**
         * Invoked when the cursor enters the menu. This method sets the selected
         * path for the MenuSelectionManager and handles the case
         * in which a menu item is used to pop up an additional menu, as in a
         * hierarchical menu system.
         *
         * @param e the mouse event; not used
         */
        public void mouseEntered(final MouseEvent e) {
            final JMenu menu = (JMenu)menuItem;
            if (!menu.isEnabled()) return;

            final MenuSelectionManager manager = MenuSelectionManager.defaultManager();
            final MenuElement[] selectedPath = manager.getSelectedPath();

            // In Aqua, we always have a menu delay, regardless of where the menu is.
            if (!(selectedPath.length > 0 && selectedPath[selectedPath.length - 1] == menu.getPopupMenu())) {
                // the condition below prevents from activating menu in other frame
                if (!menu.isTopLevelMenu() || (selectedPath.length > 0 &&
                        selectedPath[0] == menu.getParent())) {
                    if (menu.getDelay() == 0) {
                        appendPath(getPath(), menu.getPopupMenu());
                    } else {
                        manager.setSelectedPath(getPath());
                        setupPostTimer(menu);
                    }
                }
            }
        }
    }
}
