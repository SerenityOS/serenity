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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicMenuUI;

import com.sun.java.swing.plaf.windows.TMSchema.Part;
import com.sun.java.swing.plaf.windows.TMSchema.State;

/**
 * Windows rendition of the component.
 */
public class WindowsMenuUI extends BasicMenuUI {
    protected Integer menuBarHeight;
    protected boolean hotTrackingOn;

    final WindowsMenuItemUIAccessor accessor =
        new WindowsMenuItemUIAccessor() {

            public JMenuItem getMenuItem() {
                return menuItem;
            }

            public State getState(JMenuItem menu) {
                State state = menu.isEnabled() ? State.NORMAL
                        : State.DISABLED;
                ButtonModel model = menu.getModel();
                if (model.isArmed() || model.isSelected()) {
                    state = (menu.isEnabled()) ? State.PUSHED
                            : State.DISABLEDPUSHED;
                } else if (model.isRollover()
                           && ((JMenu) menu).isTopLevelMenu()) {
                    /*
                     * Only paint rollover if no other menu on menubar is
                     * selected
                     */
                    State stateTmp = state;
                    state = (menu.isEnabled()) ? State.HOT
                            : State.DISABLEDHOT;
                    for (MenuElement menuElement :
                        ((JMenuBar) menu.getParent()).getSubElements()) {
                        if (((JMenuItem) menuElement).isSelected()) {
                            state = stateTmp;
                            break;
                        }
                    }
                }

                //non top level menus have HOT state instead of PUSHED
                if (!((JMenu) menu).isTopLevelMenu()) {
                    if (state == State.PUSHED) {
                        state = State.HOT;
                    } else if (state == State.DISABLEDPUSHED) {
                        state = State.DISABLEDHOT;
                    }
                }

                /*
                 * on Vista top level menu for non active frame looks disabled
                 */
                if (((JMenu) menu).isTopLevelMenu() && WindowsMenuItemUI.isVistaPainting()) {
                    if (! WindowsMenuBarUI.isActive(menu)) {
                        state = State.DISABLED;
                    }
                }
                return state;
            }

            public Part getPart(JMenuItem menuItem) {
                return ((JMenu) menuItem).isTopLevelMenu() ? Part.MP_BARITEM
                        : Part.MP_POPUPITEM;
            }
    };
    public static ComponentUI createUI(JComponent x) {
        return new WindowsMenuUI();
    }

    protected void installDefaults() {
        super.installDefaults();
        if (!WindowsLookAndFeel.isClassicWindows()) {
            menuItem.setRolloverEnabled(true);
        }

        menuBarHeight = (Integer)UIManager.getInt("MenuBar.height");

        Object obj      = UIManager.get("MenuBar.rolloverEnabled");
        hotTrackingOn = (obj instanceof Boolean) ? (Boolean)obj : true;
    }

    /**
     * Draws the background of the menu.
     * @since 1.4
     */
    protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
        if (WindowsMenuItemUI.isVistaPainting()) {
            WindowsMenuItemUI.paintBackground(accessor, g, menuItem, bgColor);
            return;
        }

        JMenu menu = (JMenu)menuItem;
        ButtonModel model = menu.getModel();

        // Use superclass method for the old Windows LAF,
        // for submenus, and for XP toplevel if selected or pressed
        if (WindowsLookAndFeel.isClassicWindows() ||
            !menu.isTopLevelMenu() ||
            (XPStyle.getXP() != null && (model.isArmed() || model.isSelected()))) {

            super.paintBackground(g, menu, bgColor);
            return;
        }

        Color oldColor = g.getColor();
        int menuWidth = menu.getWidth();
        int menuHeight = menu.getHeight();

        UIDefaults table = UIManager.getLookAndFeelDefaults();
        Color highlight = table.getColor("controlLtHighlight");
        Color shadow = table.getColor("controlShadow");

        g.setColor(menu.getBackground());
        g.fillRect(0,0, menuWidth, menuHeight);

        if (menu.isOpaque()) {
            if (model.isArmed() || model.isSelected()) {
                // Draw a lowered bevel border
                g.setColor(shadow);
                g.drawLine(0,0, menuWidth - 1,0);
                g.drawLine(0,0, 0,menuHeight - 2);

                g.setColor(highlight);
                g.drawLine(menuWidth - 1,0, menuWidth - 1,menuHeight - 2);
                g.drawLine(0,menuHeight - 2, menuWidth - 1,menuHeight - 2);
            } else if (model.isRollover() && model.isEnabled()) {
                // Only paint rollover if no other menu on menubar is selected
                boolean otherMenuSelected = false;
                MenuElement[] menus = ((JMenuBar)menu.getParent()).getSubElements();
                for (int i = 0; i < menus.length; i++) {
                    if (((JMenuItem)menus[i]).isSelected()) {
                        otherMenuSelected = true;
                        break;
                    }
                }
                if (!otherMenuSelected) {
                    if (XPStyle.getXP() != null) {
                        g.setColor(selectionBackground); // Uses protected field.
                        g.fillRect(0, 0, menuWidth, menuHeight);
                    } else {
                        // Draw a raised bevel border
                        g.setColor(highlight);
                        g.drawLine(0,0, menuWidth - 1,0);
                        g.drawLine(0,0, 0,menuHeight - 2);

                        g.setColor(shadow);
                        g.drawLine(menuWidth - 1,0, menuWidth - 1,menuHeight - 2);
                        g.drawLine(0,menuHeight - 2, menuWidth - 1,menuHeight - 2);
                    }
                }
            }
        }
        g.setColor(oldColor);
    }

    /**
     * Method which renders the text of the current menu item.
     *
     * @param g Graphics context
     * @param menuItem Current menu item to render
     * @param textRect Bounding rectangle to render the text.
     * @param text String to render
     * @since 1.4
     */
    protected void paintText(Graphics g, JMenuItem menuItem,
                             Rectangle textRect, String text) {
        if (WindowsMenuItemUI.isVistaPainting()) {
            WindowsMenuItemUI.paintText(accessor, g, menuItem, textRect, text);
            return;
        }
        JMenu menu = (JMenu)menuItem;
        ButtonModel model = menuItem.getModel();
        Color oldColor = g.getColor();

        // Only paint rollover if no other menu on menubar is selected
        boolean paintRollover = model.isRollover();
        if (paintRollover && menu.isTopLevelMenu()) {
            MenuElement[] menus = ((JMenuBar)menu.getParent()).getSubElements();
            for (int i = 0; i < menus.length; i++) {
                if (((JMenuItem)menus[i]).isSelected()) {
                    paintRollover = false;
                    break;
                }
            }
        }

        if ((model.isSelected() && (WindowsLookAndFeel.isClassicWindows() ||
                                    !menu.isTopLevelMenu())) ||
            (XPStyle.getXP() != null && (paintRollover ||
                                         model.isArmed() ||
                                         model.isSelected()))) {
            g.setColor(selectionForeground); // Uses protected field.
        }

        WindowsGraphicsUtils.paintText(g, menuItem, textRect, text, 0);

        g.setColor(oldColor);
    }

    protected MouseInputListener createMouseInputListener(JComponent c) {
        return new WindowsMouseInputHandler();
    }

    /**
     * This class implements a mouse handler that sets the rollover flag to
     * true when the mouse enters the menu and false when it exits.
     * @since 1.4
     */
    protected class WindowsMouseInputHandler extends BasicMenuUI.MouseInputHandler {
        public void mouseEntered(MouseEvent evt) {
            super.mouseEntered(evt);

            JMenu menu = (JMenu)evt.getSource();
            if (hotTrackingOn && menu.isTopLevelMenu() && menu.isRolloverEnabled()) {
                menu.getModel().setRollover(true);
                menuItem.repaint();
            }
        }

        public void mouseExited(MouseEvent evt) {
            super.mouseExited(evt);

            JMenu menu = (JMenu)evt.getSource();
            ButtonModel model = menu.getModel();
            if (menu.isRolloverEnabled()) {
                model.setRollover(false);
                menuItem.repaint();
            }
        }
    }

    protected Dimension getPreferredMenuItemSize(JComponent c,
                                                     Icon checkIcon,
                                                     Icon arrowIcon,
                                                     int defaultTextIconGap) {

        Dimension d = super.getPreferredMenuItemSize(c, checkIcon, arrowIcon,
                                                     defaultTextIconGap);

        // Note: When toolbar containers (rebars) are implemented, only do
        // this if the JMenuBar is not in a rebar (i.e. ignore the desktop
        // property win.menu.height if in a rebar.)
        if (c instanceof JMenu && ((JMenu)c).isTopLevelMenu() &&
            menuBarHeight != null && d.height < menuBarHeight) {

            d.height = menuBarHeight;
        }

        return d;
    }
}
