/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.event.*;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;

import sun.awt.AWTAccessor;
import sun.lwawt.macosx.CMenuItem;

@SuppressWarnings("serial") // JDK implementation class
final class ScreenMenuItem extends MenuItem
        implements ActionListener, ComponentListener,
                   ScreenMenuPropertyHandler {

    ScreenMenuPropertyListener fListener;
    JMenuItem fMenuItem;

    ScreenMenuItem(final JMenuItem mi) {
        super(mi.getText());
        fMenuItem = mi;
        setEnabled(fMenuItem.isEnabled());
        final ComponentUI ui = fMenuItem.getUI();

        if (ui instanceof ScreenMenuItemUI) {
            ((ScreenMenuItemUI)ui).updateListenersForScreenMenuItem();
            // SAK:  Not calling this means that mouse and mouse motion listeners don't get
            // installed.  Not a problem because the menu manager handles tracking for us.
    }
    }

    public void addNotify() {
        super.addNotify();

        fMenuItem.addComponentListener(this);
        fListener = new ScreenMenuPropertyListener(this);
        fMenuItem.addPropertyChangeListener(fListener);
        addActionListener(this);

        setEnabled(fMenuItem.isEnabled());

        // can't setState or setAccelerator or setIcon till we have a peer
        setAccelerator(fMenuItem.getAccelerator());

        final String label = fMenuItem.getText();
        if (label != null) {
            setLabel(label);
        }

        final Icon icon = fMenuItem.getIcon();
        if (icon != null) {
            this.setIcon(icon);
        }

        final String tooltipText = fMenuItem.getToolTipText();
        if (tooltipText != null) {
            this.setToolTipText(tooltipText);
        }

        if (fMenuItem instanceof JRadioButtonMenuItem) {
            final ComponentUI ui = fMenuItem.getUI();

            if (ui instanceof ScreenMenuItemUI) {
                ((ScreenMenuItemUI)ui).updateListenersForScreenMenuItem();
            }
        }
    }

    public void removeNotify() {
        super.removeNotify();
        removeActionListener(this);
        fMenuItem.removePropertyChangeListener(fListener);
        fListener = null;
        fMenuItem.removeComponentListener(this);
    }

    static void syncLabelAndKS(MenuItem menuItem, String label, KeyStroke ks) {
        Object peer = AWTAccessor.getMenuComponentAccessor().getPeer(menuItem);
        if (!(peer instanceof CMenuItem)) {
            //Is it possible?
            return;
        }
        final CMenuItem cmi = (CMenuItem) peer;
        if (ks == null) {
            cmi.setLabel(label);
        } else {
            cmi.setLabel(label, ks.getKeyChar(), ks.getKeyCode(),
                         ks.getModifiers());
        }
    }

    @Override
    public synchronized void setLabel(final String label) {
        syncLabelAndKS(this, label, fMenuItem.getAccelerator());
    }

    @Override
    public void setAccelerator(final KeyStroke ks) {
        syncLabelAndKS(this, fMenuItem.getText(), ks);
    }

    public void actionPerformed(final ActionEvent e) {
        fMenuItem.doClick(0); // This takes care of all the different events
    }

    /**
     * Invoked when the component's size changes.
     */
    public void componentResized(final ComponentEvent e) {}

    /**
     * Invoked when the component's position changes.
     */
    public void componentMoved(final ComponentEvent e) {}

    /**
     * Invoked when the component has been made visible.
     * See componentHidden - we should still have a MenuItem
     * it just isn't inserted
     */
    public void componentShown(final ComponentEvent e) {
        setVisible(true);
    }

    /**
     * Invoked when the component has been made invisible.
     * MenuComponent.setVisible does nothing,
     * so we remove the ScreenMenuItem from the ScreenMenu
     * but leave it in fItems
     */
    public void componentHidden(final ComponentEvent e) {
        setVisible(false);
    }

    public void setVisible(final boolean b) {
        // Tell our parent to add/remove us -- parent may be nil if we aren't set up yet.
        // Hang on to our parent
        final MenuContainer parent = getParent();

        if (parent != null) {
            ((ScreenMenuPropertyHandler)parent).setChildVisible(fMenuItem, b);
        }
    }

    public void setToolTipText(final String text) {
        Object peer = AWTAccessor.getMenuComponentAccessor().getPeer(this);
        if (!(peer instanceof CMenuItem)) return;

        final CMenuItem cmi = (CMenuItem)peer;
        cmi.setToolTipText(text);
    }

    public void setIcon(final Icon i) {
        Object peer = AWTAccessor.getMenuComponentAccessor().getPeer(this);
        if (!(peer instanceof CMenuItem)) return;

        final CMenuItem cmi = (CMenuItem)peer;
            Image img = null;

        if (i != null) {
            if (i.getIconWidth() > 0 && i.getIconHeight() > 0) {
                    img = AquaIcon.getImageForIcon(i);
                }
        }
            cmi.setImage(img);
        }

    // we have no children
    public void setChildVisible(final JMenuItem child, final boolean b) {}

    // only check and radio items can be indeterminate
    public void setIndeterminate(boolean indeterminate) { }
}
