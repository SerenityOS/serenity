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
import javax.swing.plaf.ButtonUI;

import com.apple.laf.AquaMenuItemUI.IndeterminateListener;

import sun.awt.AWTAccessor;
import sun.lwawt.macosx.*;

@SuppressWarnings("serial") // JDK implementation class
final class ScreenMenuItemCheckbox extends CheckboxMenuItem
        implements ActionListener, ComponentListener, ScreenMenuPropertyHandler,
                   ItemListener {

    JMenuItem fMenuItem;
    MenuContainer fParent;

    ScreenMenuItemCheckbox(final JCheckBoxMenuItem mi) {
        super(mi.getText(), mi.getState());
        init(mi);
    }

    ScreenMenuItemCheckbox(final JRadioButtonMenuItem mi) {
        super(mi.getText(), mi.getModel().isSelected());
        init(mi);
    }

    public void init(final JMenuItem mi) {
        fMenuItem = mi;
        setEnabled(fMenuItem.isEnabled());
    }

    ScreenMenuPropertyListener fPropertyListener;

    public void addNotify() {
        super.addNotify();

        // Avoid the Auto toggle behavior of AWT CheckBoxMenuItem
        CCheckboxMenuItem ccb = AWTAccessor.getMenuComponentAccessor().getPeer(this);
        ccb.setAutoToggle(false);

        fMenuItem.addComponentListener(this);
        fPropertyListener = new ScreenMenuPropertyListener(this);
        fMenuItem.addPropertyChangeListener(fPropertyListener);
        addActionListener(this);
        addItemListener(this);
        fMenuItem.addItemListener(this);
        setIndeterminate(IndeterminateListener.isIndeterminate(fMenuItem));

        // can't setState or setAccelerator or setIcon till we have a peer
        setAccelerator(fMenuItem.getAccelerator());

        final Icon icon = fMenuItem.getIcon();
        if (icon != null) {
            this.setIcon(icon);
        }

        final String tooltipText = fMenuItem.getToolTipText();
        if (tooltipText != null) {
            this.setToolTipText(tooltipText);
        }

        // sja fix is this needed?
        fMenuItem.addItemListener(this);

        final ButtonUI ui = fMenuItem.getUI();
        if (ui instanceof ScreenMenuItemUI) {
            ((ScreenMenuItemUI)ui).updateListenersForScreenMenuItem();
        }

        if (fMenuItem instanceof JCheckBoxMenuItem) {
            forceSetState(fMenuItem.isSelected());
        } else {
            forceSetState(fMenuItem.getModel().isSelected());
        }
    }

    public void removeNotify() {
        fMenuItem.removeComponentListener(this);
        fMenuItem.removePropertyChangeListener(fPropertyListener);
        fPropertyListener = null;
        removeActionListener(this);
        removeItemListener(this);
        fMenuItem.removeItemListener(this);

        super.removeNotify();
    }

    @Override
    public synchronized void setLabel(final String label) {
        ScreenMenuItem.syncLabelAndKS(this, label, fMenuItem.getAccelerator());
    }

    @Override
    public void setAccelerator(final KeyStroke ks) {
        ScreenMenuItem.syncLabelAndKS(this, fMenuItem.getText(), ks);
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

    public void setToolTipText(final String text) {
        Object peer = AWTAccessor.getMenuComponentAccessor().getPeer(this);
        if (!(peer instanceof CMenuItem)) return;

        ((CMenuItem)peer).setToolTipText(text);
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

    public void setVisible(final boolean b) {
        // Tell our parent to add/remove us
        // Hang on to our parent
        if (fParent == null) fParent = getParent();
        ((ScreenMenuPropertyHandler)fParent).setChildVisible(fMenuItem, b);
    }

    // we have no children
    public void setChildVisible(final JMenuItem child, final boolean b) {}

    /**
     * Invoked when an item's state has been changed.
     */
    public void itemStateChanged(final ItemEvent e) {
        if (e.getSource() == this) {
            fMenuItem.doClick(0);
            return;
        }

            switch (e.getStateChange()) {
                case ItemEvent.SELECTED:
                    forceSetState(true);
                    break;
                case ItemEvent.DESELECTED:
                    forceSetState(false);
                    break;
            }
        }

    public void setIndeterminate(final boolean indeterminate) {
        Object peer = AWTAccessor.getMenuComponentAccessor().getPeer(this);
        if (peer instanceof CCheckboxMenuItem) {
            ((CCheckboxMenuItem)peer).setIsIndeterminate(indeterminate);
        }
    }

    /*
     * The CCheckboxMenuItem peer is calling setState unconditionally every time user clicks the menu
     * However for Swing controls in the screen menu bar it is wrong - the state should be changed only
     * in response to the ITEM_STATE_CHANGED event. So the setState is overridden to no-op and all the
     * correct state changes are made with forceSetState
     */

    @Override
    public synchronized void setState(boolean b) {
        // No Op
    }

    private void forceSetState(boolean b) {
        super.setState(b);
    }
}
