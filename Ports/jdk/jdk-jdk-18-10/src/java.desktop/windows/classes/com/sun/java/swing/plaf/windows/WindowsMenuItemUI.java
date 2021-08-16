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
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicMenuItemUI;

import com.sun.java.swing.plaf.windows.TMSchema.Part;
import com.sun.java.swing.plaf.windows.TMSchema.State;
import com.sun.java.swing.plaf.windows.XPStyle.Skin;
import sun.swing.MenuItemCheckIconFactory;
import sun.swing.MenuItemLayoutHelper;
import sun.swing.SwingUtilities2;

/**
 * Windows rendition of the component.
 *
 * @author Igor Kushnirskiy
 */
public class WindowsMenuItemUI extends BasicMenuItemUI {
    /**
     * The instance of {@code PropertyChangeListener}.
     */
    private PropertyChangeListener changeListener;

    final WindowsMenuItemUIAccessor accessor =
        new  WindowsMenuItemUIAccessor() {

            public JMenuItem getMenuItem() {
                return menuItem;
            }

            public State getState(JMenuItem menuItem) {
                return WindowsMenuItemUI.getState(this, menuItem);
            }

            public Part getPart(JMenuItem menuItem) {
                return WindowsMenuItemUI.getPart(this, menuItem);
            }
    };
    public static ComponentUI createUI(JComponent c) {
        return new WindowsMenuItemUI();
    }

    private void updateCheckIcon() {
        String prefix = getPropertyPrefix();

        if (checkIcon == null ||
                checkIcon instanceof UIResource) {
            checkIcon = UIManager.getIcon(prefix + ".checkIcon");
            //In case of column layout, .checkIconFactory is defined for this UI,
            //the icon is compatible with it and useCheckAndArrow() is true,
            //then the icon is handled by the checkIcon.
            boolean isColumnLayout = MenuItemLayoutHelper.isColumnLayout(
                    menuItem.getComponentOrientation().isLeftToRight(), menuItem);
            if (isColumnLayout) {
                MenuItemCheckIconFactory iconFactory =
                        (MenuItemCheckIconFactory) UIManager.get(prefix
                                + ".checkIconFactory");
                if (iconFactory != null
                        && MenuItemLayoutHelper.useCheckAndArrow(menuItem)
                        && iconFactory.isCompatible(checkIcon, prefix)) {
                    checkIcon = iconFactory.getIcon(menuItem);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installListeners() {
        super.installListeners();
        changeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                String name = e.getPropertyName();
                if (name == "horizontalTextPosition") {
                    updateCheckIcon();
                }
            }
        };
        menuItem.addPropertyChangeListener(changeListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void uninstallListeners() {
        super.uninstallListeners();
        if (changeListener != null) {
            menuItem.removePropertyChangeListener(changeListener);
        }
        changeListener = null;
    }

    /**
     * Method which renders the text of the current menu item.
     *
     * @param g Graphics context
     * @param menuItem Current menu item to render
     * @param textRect Bounding rectangle to render the text.
     * @param text String to render
     */
    protected void paintText(Graphics g, JMenuItem menuItem,
                             Rectangle textRect, String text) {
        if (WindowsMenuItemUI.isVistaPainting()) {
            WindowsMenuItemUI.paintText(accessor, g, menuItem, textRect, text);
            return;
        }
        ButtonModel model = menuItem.getModel();
        Color oldColor = g.getColor();

        if(model.isEnabled() &&
            (model.isArmed() || (menuItem instanceof JMenu &&
             model.isSelected()))) {
            g.setColor(selectionForeground); // Uses protected field.
        }

        WindowsGraphicsUtils.paintText(g, menuItem, textRect, text, 0);

        g.setColor(oldColor);
    }

    @Override
    protected void paintBackground(Graphics g, JMenuItem menuItem,
            Color bgColor) {
        if (WindowsMenuItemUI.isVistaPainting()) {
            WindowsMenuItemUI.paintBackground(accessor, g, menuItem, bgColor);
            return;
        }
        super.paintBackground(g, menuItem, bgColor);
    }

    static void paintBackground(WindowsMenuItemUIAccessor menuItemUI,
            Graphics g, JMenuItem menuItem, Color bgColor) {
        XPStyle xp = XPStyle.getXP();
        assert isVistaPainting(xp);
        if (isVistaPainting(xp)) {
            int menuWidth = menuItem.getWidth();
            int menuHeight = menuItem.getHeight();
            if (menuItem.isOpaque()) {
                Color oldColor = g.getColor();
                g.setColor(menuItem.getBackground());
                g.fillRect(0,0, menuWidth, menuHeight);
                g.setColor(oldColor);
            }
            Part part = menuItemUI.getPart(menuItem);
            Skin skin = xp.getSkin(menuItem, part);
            skin.paintSkin(g, 0 , 0,
                menuWidth,
                menuHeight,
                menuItemUI.getState(menuItem));
        }
    }

    static void paintText(WindowsMenuItemUIAccessor menuItemUI, Graphics g,
                                JMenuItem menuItem, Rectangle textRect,
                                String text) {
        assert isVistaPainting();
        if (isVistaPainting()) {
            State state = menuItemUI.getState(menuItem);

            /* part of it copied from WindowsGraphicsUtils.java */
            FontMetrics fm = SwingUtilities2.getFontMetrics(menuItem, g);
            int mnemIndex = menuItem.getDisplayedMnemonicIndex();
            // W2K Feature: Check to see if the Underscore should be rendered.
            if (WindowsLookAndFeel.isMnemonicHidden() == true) {
                mnemIndex = -1;
            }
            WindowsGraphicsUtils.paintXPText(menuItem,
                menuItemUI.getPart(menuItem), state,
                g, textRect.x,
                textRect.y + fm.getAscent(),
                text, mnemIndex);
        }
    }

    static State getState(WindowsMenuItemUIAccessor menuItemUI, JMenuItem menuItem) {
        State state;
        ButtonModel model = menuItem.getModel();
        if (model.isArmed()) {
            state = (model.isEnabled()) ? State.HOT : State.DISABLEDHOT;
        } else {
            state = (model.isEnabled()) ? State.NORMAL : State.DISABLED;
        }
        return state;
    }

    static Part getPart(WindowsMenuItemUIAccessor menuItemUI, JMenuItem menuItem) {
        return Part.MP_POPUPITEM;
    }

    /*
     * TODO idk can we use XPStyle.isVista?
     * is it possible that in some theme some Vista parts are not defined while
     * others are?
     */
    static boolean isVistaPainting(final XPStyle xp) {
        return xp != null && xp.isSkinDefined(null, Part.MP_POPUPITEM);
    }

    static boolean isVistaPainting() {
        return isVistaPainting(XPStyle.getXP());
    }
}
