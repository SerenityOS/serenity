/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.awt;

import java.awt.peer.PopupMenuPeer;
import java.io.Serial;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

import sun.awt.AWTAccessor;

/**
 * A class that implements a menu which can be dynamically popped up
 * at a specified position within a component.
 * <p>
 * As the inheritance hierarchy implies, a {@code PopupMenu}
 *  can be used anywhere a {@code Menu} can be used.
 * However, if you use a {@code PopupMenu} like a {@code Menu}
 * (e.g., you add it to a {@code MenuBar}), then you <b>cannot</b>
 * call {@code show} on that {@code PopupMenu}.
 *
 * @author      Amy Fowler
 */
public class PopupMenu extends Menu {

    private static final String base = "popup";
    static int nameCounter = 0;

    transient volatile boolean isTrayIconPopup;

    static {
        AWTAccessor.setPopupMenuAccessor(
            new AWTAccessor.PopupMenuAccessor() {
                public boolean isTrayIconPopup(PopupMenu popupMenu) {
                    return popupMenu.isTrayIconPopup;
                }
            });
    }

    /**
     * Use serialVersionUID from JDK 1.1 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -4620452533522760060L;

    /**
     * Creates a new popup menu with an empty name.
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     * returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public PopupMenu() throws HeadlessException {
        this("");
    }

    /**
     * Creates a new popup menu with the specified name.
     *
     * @param label a non-{@code null} string specifying
     *                the popup menu's label
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     * returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public PopupMenu(String label) throws HeadlessException {
        super(label);
    }

    /**
     * {@inheritDoc}
     */
    public MenuContainer getParent() {
        if (isTrayIconPopup) {
            return null;
        }
        return super.getParent();
    }

    /**
     * Constructs a name for this {@code MenuComponent}.
     * Called by {@code getName} when the name is {@code null}.
     */
    String constructComponentName() {
        synchronized (PopupMenu.class) {
            return base + nameCounter++;
        }
    }

    /**
     * Creates the popup menu's peer.
     * The peer allows us to change the appearance of the popup menu without
     * changing any of the popup menu's functionality.
     */
    public void addNotify() {
        synchronized (getTreeLock()) {
            // If our parent is not a Component, then this PopupMenu is
            // really just a plain, old Menu.
            if (parent != null && !(parent instanceof Component)) {
                super.addNotify();
            }
            else {
                if (peer == null)
                    peer = getComponentFactory().createPopupMenu(this);
                int nitems = getItemCount();
                for (int i = 0 ; i < nitems ; i++) {
                    MenuItem mi = getItem(i);
                    mi.parent = this;
                    mi.addNotify();
                }
            }
        }
    }

   /**
     * Shows the popup menu at the x, y position relative to an origin
     * component.
     * The origin component must be contained within the component
     * hierarchy of the popup menu's parent.  Both the origin and the parent
     * must be showing on the screen for this method to be valid.
     * <p>
     * If this {@code PopupMenu} is being used as a {@code Menu}
     * (i.e., it has a non-{@code Component} parent),
     * then you cannot call this method on the {@code PopupMenu}.
     *
     * @param origin the component which defines the coordinate space
     * @param x the x coordinate position to popup the menu
     * @param y the y coordinate position to popup the menu
     * @exception NullPointerException  if the parent is {@code null}
     * @exception IllegalArgumentException  if this {@code PopupMenu}
     *                has a non-{@code Component} parent
     * @exception IllegalArgumentException if the origin is not in the
     *                parent's hierarchy
     * @exception RuntimeException if the parent is not showing on screen
     */
    @SuppressWarnings("deprecation")
    public void show(Component origin, int x, int y) {
        // Use localParent for thread safety.
        MenuContainer localParent = parent;
        if (localParent == null) {
            throw new NullPointerException("parent is null");
        }
        if (!(localParent instanceof Component)) {
            throw new IllegalArgumentException(
                "PopupMenus with non-Component parents cannot be shown");
        }
        Component compParent = (Component)localParent;
        //Fixed 6278745: Incorrect exception throwing in PopupMenu.show() method
        //Exception was not thrown if compParent was not equal to origin and
        //was not Container
        if (compParent != origin) {
            if (compParent instanceof Container) {
                if (!((Container)compParent).isAncestorOf(origin)) {
                    throw new IllegalArgumentException("origin not in parent's hierarchy");
                }
            } else {
                throw new IllegalArgumentException("origin not in parent's hierarchy");
            }
        }
        if (compParent.peer == null || !compParent.isShowing()) {
            throw new RuntimeException("parent not showing on screen");
        }
        if (peer == null) {
            addNotify();
        }
        synchronized (getTreeLock()) {
            if (peer != null) {
                ((PopupMenuPeer)peer).show(
                    new Event(origin, 0, Event.MOUSE_DOWN, x, y, 0, 0));
            }
        }
    }


/////////////////
// Accessibility support
////////////////

    /**
     * Gets the {@code AccessibleContext} associated with this
     * {@code PopupMenu}.
     *
     * @return the {@code AccessibleContext} of this
     *                {@code PopupMenu}
     * @since 1.3
     */
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleAWTPopupMenu();
        }
        return accessibleContext;
    }

    /**
     * Inner class of PopupMenu used to provide default support for
     * accessibility.  This class is not meant to be used directly by
     * application developers, but is instead meant only to be
     * subclassed by menu component developers.
     * <p>
     * The class used to obtain the accessible role for this object.
     * @since 1.3
     */
    protected class AccessibleAWTPopupMenu extends AccessibleAWTMenu
    {
        /**
         * Use serialVersionUID from JDK 1.3 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = -4282044795947239955L;

        /**
         * Constructs an {@code AccessibleAWTPopupMenu}.
         */
        protected AccessibleAWTPopupMenu() {}

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         * object
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.POPUP_MENU;
        }

    } // class AccessibleAWTPopupMenu

}
