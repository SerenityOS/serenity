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

import sun.lwawt.macosx.CMenuBar;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import static sun.awt.AWTAccessor.*;

@SuppressWarnings("serial") // JDK implementation class
public class ScreenMenuBar extends MenuBar
        implements ContainerListener, ScreenMenuPropertyHandler,
                   ComponentListener {

    static boolean sJMenuBarHasHelpMenus = false; //$ could check by calling getHelpMenu in a try block

    JMenuBar fSwingBar;
    Hashtable<JMenu, ScreenMenu> fSubmenus;

    ScreenMenuPropertyListener fPropertyListener;
    ScreenMenuPropertyListener fAccessibleListener;

    public ScreenMenuBar(final JMenuBar swingBar) {
        fSwingBar = swingBar;
        fSubmenus = new Hashtable<JMenu, ScreenMenu>(fSwingBar.getMenuCount());
    }

    public void addNotify() {
        super.addNotify();

        fSwingBar.addContainerListener(this);
        fPropertyListener = new ScreenMenuPropertyListener(this);
        fSwingBar.addPropertyChangeListener(fPropertyListener);
        fAccessibleListener = new ScreenMenuPropertyListener(this);
        fSwingBar.getAccessibleContext().addPropertyChangeListener(fAccessibleListener);

        // We disable component events when the menu bar is not parented.  So now we need to
        // sync back up with the current state of the JMenuBar.  We first add the menus we
        // don't have and then remove the items that are no longer on the JMenuBar.
        final int count = fSwingBar.getMenuCount();
        for(int i = 0; i < count ; i++) {
            final JMenu m = fSwingBar.getMenu(i);
            if (m != null) {
                addSubmenu(m);
            }
        }

        final Enumeration<JMenu> e = fSubmenus.keys();
        while (e.hasMoreElements()) {
            final JMenu m = e.nextElement();
            if (fSwingBar.getComponentIndex(m) == -1) {
                removeSubmenu(m);
            }
        }
    }

    public void removeNotify() {
        // KCH - 3974930 - We do null checks for fSwingBar and fSubmenus because some people are using
        // reflection to muck about with our ivars
        if (fSwingBar != null) {
            fSwingBar.removePropertyChangeListener(fPropertyListener);
            fSwingBar.getAccessibleContext().removePropertyChangeListener(fAccessibleListener);
            fSwingBar.removeContainerListener(this);
        }

        fPropertyListener = null;
        fAccessibleListener = null;

        if (fSubmenus != null) {
            // We don't listen to events when the menu bar is not parented.
            // Remove all the component listeners.
            final Enumeration<JMenu> e = fSubmenus.keys();
            while (e.hasMoreElements()) {
                final JMenu m = e.nextElement();
                m.removeComponentListener(this);
            }
        }

        super.removeNotify();
    }

    /**
     * Invoked when a component has been added to the container.
     */
    public void componentAdded(final ContainerEvent e) {
        final Component child = e.getChild();
        if (!(child instanceof JMenu)) return;
            addSubmenu((JMenu)child);
     }

    /**
     * Invoked when a component has been removed from the container.
     */
    public void componentRemoved(final ContainerEvent e) {
          final Component child = e.getChild();
          if (!(child instanceof JMenu)) return;
            removeSubmenu((JMenu)child);
        }

    /**
        * Invoked when the component's size changes.
     */
    public void componentResized(final ComponentEvent e)  {}

    /**
        * Invoked when the component's position changes.
     */
    public void componentMoved(final ComponentEvent e)  {}

    /**
        * Invoked when the component has been made visible.
     * See componentHidden - we should still have a MenuItem
     * it just isn't inserted
     */
    public void componentShown(final ComponentEvent e) {
        final Object source = e.getSource();
        if (!(source instanceof JMenuItem)) return;
        setChildVisible((JMenuItem)source, true);
    }

    /**
        * Invoked when the component has been made invisible.
     * MenuComponent.setVisible does nothing,
     * so we remove the ScreenMenuItem from the ScreenMenu
     * but leave it in fItems
     */
    public void componentHidden(final ComponentEvent e)  {
        final Object source = e.getSource();
        if (!(source instanceof JMenuItem)) return;
        setChildVisible((JMenuItem)source, false);
    }

    /*
     * MenuComponent.setVisible does nothing,
     * so we just add or remove the child from the ScreenMenuBar
     * but leave it in the list
     */
    public void setChildVisible(final JMenuItem child, final boolean b) {
        if (child instanceof JMenu) {
            if (b) {
                addSubmenu((JMenu)child);
            } else {
                final ScreenMenu sm = fSubmenus.get(child);
                if (sm != null)
                    remove(sm);
            }
        }
    }

    public void removeAll() {
        synchronized (getTreeLock()) {
            final int nitems = getMenuCount();
            for (int i = nitems-1 ; i >= 0 ; i--) {
                remove(i);
            }
        }
    }

    public void setIcon(final Icon i) {}
    public void setLabel(final String s) {}

    public void setEnabled(final boolean b) {
        final int count = fSwingBar.getMenuCount();
        for (int i = 0; i < count; i++) {
            fSwingBar.getMenu(i).setEnabled(b);
        }
    }

    public void setAccelerator(final KeyStroke ks) {}
    public void setToolTipText(final String tooltip) {}

    // only check and radio items can be indeterminate
    public void setIndeterminate(boolean indeterminate) { }

    ScreenMenu addSubmenu(final JMenu m) {
        ScreenMenu sm = fSubmenus.get(m);

        if (sm == null) {
            sm = new ScreenMenu(m);
            m.addComponentListener(this);
            fSubmenus.put(m, sm);
        }

        sm.setEnabled(m.isEnabled());

        // MenuComponents don't support setVisible, so we just don't add it to the menubar
        if (m.isVisible() && sm.getParent() == null) {
            int newIndex = 0, currVisibleIndex = 0;
            JMenu menu = null;
            final int menuCount = fSwingBar.getMenuCount();
            for (int i = 0; i < menuCount; i++) {
                menu = fSwingBar.getMenu(i);
                if (menu == m) {
                    newIndex = currVisibleIndex;
                    break;
                }
                if (menu != null && menu.isVisible()) {
                    currVisibleIndex++;
                }
            }
            add(sm, newIndex);
        }

        return sm;
    }

    /**
     * Remove the screen menu associated with the specifiec menu.  This
     * also removes any associated component listener on the screen menu
     * and removes the key/value (menu/screen menu) from the fSubmenus cache.
     *
     * @param menu The swing menu we want to remove the screen menu for.
     */
    private void removeSubmenu(final JMenu menu) {
        final ScreenMenu screenMenu = fSubmenus.get(menu);
        if (screenMenu == null) return;

            menu.removeComponentListener(this);
            remove(screenMenu);
            fSubmenus.remove(menu);
    }

    public Menu add(final Menu m, final int index) {
        synchronized (getTreeLock()) {
            if (m.getParent() != null) {
                m.getParent().remove(m);
            }

            final Vector<Menu> menus = getMenuBarAccessor().getMenus(this);
            menus.insertElementAt(m, index);
            final MenuComponentAccessor acc = getMenuComponentAccessor();
            acc.setParent(m, this);

            final CMenuBar peer = acc.getPeer(this);
            if (peer == null) return m;

            peer.setNextInsertionIndex(index);
            final CMenuBar mPeer = acc.getPeer(m);
            if (mPeer == null) {
                m.addNotify();
            }

            peer.setNextInsertionIndex(-1);
            return m;
        }
    }
}
