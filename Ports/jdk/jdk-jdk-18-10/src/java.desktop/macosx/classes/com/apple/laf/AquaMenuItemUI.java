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
import java.beans.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicMenuItemUI;

import apple.laf.JRSUIConstants.Size;

// TODO: no screen menu bar for now
public class AquaMenuItemUI extends BasicMenuItemUI implements AquaMenuPainter.Client/*, ScreenMenuItemUI*/ {
    static final int kPlain = 0, kCheckBox = 1, kRadioButton = 2;
    static final String[] sPropertyPrefixes = { "MenuItem", "CheckBoxMenuItem", "RadioButtonMenuItem" };

    boolean fIsScreenMenuItem = false;
    boolean fIsIndeterminate = false;
    int fType;

    AquaMenuItemUI(final int type) {
        super();
        fType = type;
    }

    public static ComponentUI createUI(final JComponent c) {
        int type = kPlain;
        if (c instanceof JCheckBoxMenuItem) type = kCheckBox;
        if (c instanceof JRadioButtonMenuItem) type = kRadioButton;
        return new AquaMenuItemUI(type);
    }

    // The only real difference between the three is which property prefix it returns
    // and therefore which icons!
    protected String getPropertyPrefix() {
        return sPropertyPrefixes[fType];
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        IndeterminateListener.install(menuItem);
    }

    @Override
    protected void uninstallListeners() {
        IndeterminateListener.uninstall(menuItem);
        super.uninstallListeners();
    }

    public void updateListenersForScreenMenuItem() {
        setIsScreenMenu(true);
    }

    // Users can dynamically change the kind of menu we're on by calling JPopupMenu.setInvoker
    // so we need to be prepared to put the listeners back on
    protected void setIsScreenMenu(final boolean isScreenMenuItem) {
        if (fIsScreenMenuItem != isScreenMenuItem) {
            fIsScreenMenuItem = isScreenMenuItem;
            if (fIsScreenMenuItem) removeListeners();
            else addListeners();
        }
    }

    protected void removeListeners() {
        menuItem.removeMouseListener(mouseInputListener);
        menuItem.removeMouseMotionListener(mouseInputListener);
        menuItem.removeMenuDragMouseListener(menuDragMouseListener);
    }

    protected void addListeners() {
        menuItem.addMouseListener(mouseInputListener);
        menuItem.addMouseMotionListener(mouseInputListener);
        menuItem.addMenuDragMouseListener(menuDragMouseListener);
    }

    protected void paintMenuItem(final Graphics g, final JComponent c, final Icon localCheckIcon, final Icon localArrowIcon, final Color background, final Color foreground, final int localDefaultTextIconGap) {
        AquaMenuPainter.instance().paintMenuItem(this, g, c, localCheckIcon, localArrowIcon, background, foreground, disabledForeground, selectionForeground, localDefaultTextIconGap, acceleratorFont);
    }

    protected Dimension getPreferredMenuItemSize(final JComponent c, final Icon localCheckIcon, final Icon localArrowIcon, final int localDefaultTextIconGap) {
        return AquaMenuPainter.instance().getPreferredMenuItemSize(c, localCheckIcon, localArrowIcon, localDefaultTextIconGap, acceleratorFont);
    }

    public void update(final Graphics g, final JComponent c) {
        if (c.isOpaque()) {
            // sja fix ((PenGraphics)g).alphaClearRect(0,0,c.getWidth(),c.getHeight());
            final Color oldColor = g.getColor();
            g.setColor(c.getBackground());
            g.fillRect(0, 0, c.getWidth(), c.getHeight());
            g.setColor(oldColor);
        }

        paint(g, c);
    }

    public void paintBackground(final Graphics g, final JComponent c, final int menuWidth, final int menuHeight) {
        if ((c.getParent() instanceof JMenuBar)) return;
        final Color oldColor = g.getColor();

        g.setColor(c.getBackground());
        g.fillRect(0, 0, menuWidth, menuHeight);
        if (((JMenuItem)c).isBorderPainted()) {
            if (((JMenuItem)c).getModel().isArmed()) {
                AquaMenuPainter.instance().paintSelectedMenuItemBackground(g, menuWidth, menuHeight);
            }
            //getTheme().drawMenuItem(c, g, 0, 0, menuWidth, menuHeight);
        } else {
            // If selected, use black (see AquaLookAndFeel "Menu.selectionBackground")
            if (((JMenuItem)c).getModel().isArmed()) {
                final Color holdc = g.getColor();
                g.setColor(Color.black);
                g.fillRect(0, 0, menuWidth, menuHeight);
                g.setColor(holdc);
            } else {
                g.setColor(Color.green);
                g.fillRect(0, 0, menuWidth, menuHeight);
                //super.paintBackground(g,c,menuWidth, menuHeight); //getTheme().drawMenuBackground((Component)c, g, (short)1, 0, 0, menuWidth, menuHeight);
            }
        }
        g.setColor(oldColor);
    }

    protected void doClick(final MenuSelectionManager msm) {
        final Dimension size = menuItem.getSize();
        AquaUtils.blinkMenu(new AquaUtils.Selectable() {
            public void paintSelected(final boolean selected) {
                menuItem.setArmed(selected);
                menuItem.paintImmediately(0, 0, size.width, size.height);
            }
        });
        super.doClick(msm);
    }

    static final IndeterminateListener INDETERMINATE_LISTENER = new IndeterminateListener();
    static class IndeterminateListener implements PropertyChangeListener {
        static final String CLIENT_PROPERTY_KEY = "JMenuItem.selectedState";

        static void install(final JMenuItem menuItem) {
            menuItem.addPropertyChangeListener(CLIENT_PROPERTY_KEY, INDETERMINATE_LISTENER);
            apply(menuItem, menuItem.getClientProperty(CLIENT_PROPERTY_KEY));
        }

        static void uninstall(final JMenuItem menuItem) {
            menuItem.removePropertyChangeListener(CLIENT_PROPERTY_KEY, INDETERMINATE_LISTENER);
        }

        public void propertyChange(final PropertyChangeEvent evt) {
            final String key = evt.getPropertyName();
            if (!CLIENT_PROPERTY_KEY.equalsIgnoreCase(key)) return;

            final Object source = evt.getSource();
            if (!(source instanceof JMenuItem)) return;

            final JMenuItem c = (JMenuItem)source;
            apply(c, evt.getNewValue());
        }

        static void apply(final JMenuItem menuItem, final Object value) {
            final ButtonUI ui = menuItem.getUI();
            if (!(ui instanceof AquaMenuItemUI)) return;

            final AquaMenuItemUI aquaUI = (AquaMenuItemUI)ui;

            if (aquaUI.fIsIndeterminate = "indeterminate".equals(value)) {
                aquaUI.checkIcon = UIManager.getIcon(aquaUI.getPropertyPrefix() + ".dashIcon");
            } else {
                aquaUI.checkIcon = UIManager.getIcon(aquaUI.getPropertyPrefix() + ".checkIcon");
            }
        }

        public static boolean isIndeterminate(final JMenuItem menuItem) {
            return "indeterminate".equals(menuItem.getClientProperty(CLIENT_PROPERTY_KEY));
        }
    }
}
