/*
 * Copyright (c) 1995, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.event.KeyEvent;
import java.awt.peer.MenuPeer;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Vector;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

import sun.awt.AWTAccessor;

/**
 * A {@code Menu} object is a pull-down menu component
 * that is deployed from a menu bar.
 * <p>
 * A menu can optionally be a <i>tear-off</i> menu. A tear-off menu
 * can be opened and dragged away from its parent menu bar or menu.
 * It remains on the screen after the mouse button has been released.
 * The mechanism for tearing off a menu is platform dependent, since
 * the look and feel of the tear-off menu is determined by its peer.
 * On platforms that do not support tear-off menus, the tear-off
 * property is ignored.
 * <p>
 * Each item in a menu must belong to the {@code MenuItem}
 * class. It can be an instance of {@code MenuItem}, a submenu
 * (an instance of {@code Menu}), or a check box (an instance of
 * {@code CheckboxMenuItem}).
 *
 * @author Sami Shaio
 * @see     java.awt.MenuItem
 * @see     java.awt.CheckboxMenuItem
 * @since   1.0
 */
public class Menu extends MenuItem implements MenuContainer, Accessible {

    static {
        /* ensure that the necessary native libraries are loaded */
        Toolkit.loadLibraries();
        if (!GraphicsEnvironment.isHeadless()) {
            initIDs();
        }

        AWTAccessor.setMenuAccessor(
            new AWTAccessor.MenuAccessor() {
                public Vector<MenuItem> getItems(Menu menu) {
                    return menu.items;
                }
            });
    }

    /**
     * A vector of the items that will be part of the Menu.
     *
     * @serial
     * @see #countItems()
     */
    private final Vector<MenuItem> items = new Vector<>();

    /**
     * This field indicates whether the menu has the
     * tear of property or not.  It will be set to
     * {@code true} if the menu has the tear off
     * property and it will be set to {@code false}
     * if it does not.
     * A torn off menu can be deleted by a user when
     * it is no longer needed.
     *
     * @serial
     * @see #isTearOff()
     */
    private final boolean tearOff;

    /**
     * This field will be set to {@code true}
     * if the Menu in question is actually a help
     * menu.  Otherwise it will be set to
     * {@code false}.
     *
     * @serial
     */
    volatile boolean isHelpMenu;

    private static final String base = "menu";
    private static int nameCounter = 0;

    /**
     * Use serialVersionUID from JDK 1.1 for interoperability.
     */
     @Serial
     private static final long serialVersionUID = -8809584163345499784L;

    /**
     * Constructs a new menu with an empty label. This menu is not
     * a tear-off menu.
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     * returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @since      1.1
     */
    public Menu() throws HeadlessException {
        this("", false);
    }

    /**
     * Constructs a new menu with the specified label. This menu is not
     * a tear-off menu.
     * @param       label the menu's label in the menu bar, or in
     *                   another menu of which this menu is a submenu.
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     * returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public Menu(String label) throws HeadlessException {
        this(label, false);
    }

    /**
     * Constructs a new menu with the specified label,
     * indicating whether the menu can be torn off.
     * <p>
     * Tear-off functionality may not be supported by all
     * implementations of AWT.  If a particular implementation doesn't
     * support tear-off menus, this value is silently ignored.
     * @param       label the menu's label in the menu bar, or in
     *                   another menu of which this menu is a submenu.
     * @param       tearOff   if {@code true}, the menu
     *                   is a tear-off menu.
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     * returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public Menu(String label, boolean tearOff) throws HeadlessException {
        super(label);
        this.tearOff = tearOff;
    }

    /**
     * Construct a name for this MenuComponent.  Called by getName() when
     * the name is null.
     */
    String constructComponentName() {
        synchronized (Menu.class) {
            return base + nameCounter++;
        }
    }

    /**
     * Creates the menu's peer.  The peer allows us to modify the
     * appearance of the menu without changing its functionality.
     */
    public void addNotify() {
        synchronized (getTreeLock()) {
            if (peer == null)
                peer = getComponentFactory().createMenu(this);
            int nitems = getItemCount();
            for (int i = 0 ; i < nitems ; i++) {
                MenuItem mi = getItem(i);
                mi.parent = this;
                mi.addNotify();
            }
        }
    }

    /**
     * Removes the menu's peer.  The peer allows us to modify the appearance
     * of the menu without changing its functionality.
     */
    public void removeNotify() {
        synchronized (getTreeLock()) {
            int nitems = getItemCount();
            for (int i = 0 ; i < nitems ; i++) {
                getItem(i).removeNotify();
            }
            super.removeNotify();
        }
    }

    /**
     * Indicates whether this menu is a tear-off menu.
     * <p>
     * Tear-off functionality may not be supported by all
     * implementations of AWT.  If a particular implementation doesn't
     * support tear-off menus, this value is silently ignored.
     * @return      {@code true} if this is a tear-off menu;
     *                         {@code false} otherwise.
     */
    public boolean isTearOff() {
        return tearOff;
    }

    /**
      * Get the number of items in this menu.
      * @return the number of items in this menu
      * @since      1.1
      */
    public int getItemCount() {
        return countItems();
    }

    /**
     * Returns the number of items in this menu.
     *
     * @return the number of items in this menu
     * @deprecated As of JDK version 1.1,
     * replaced by {@code getItemCount()}.
     */
    @Deprecated
    public int countItems() {
        return countItemsImpl();
    }

    /*
     * This is called by the native code, so client code can't
     * be called on the toolkit thread.
     */
    final int countItemsImpl() {
        return items.size();
    }

    /**
     * Gets the item located at the specified index of this menu.
     * @param     index the position of the item to be returned.
     * @return    the item located at the specified index.
     */
    public MenuItem getItem(int index) {
        return getItemImpl(index);
    }

    /*
     * This is called by the native code, so client code can't
     * be called on the toolkit thread.
     */
    final MenuItem getItemImpl(int index) {
        return items.elementAt(index);
    }

    /**
     * Adds the specified menu item to this menu. If the
     * menu item has been part of another menu, removes it
     * from that menu.
     *
     * @param       mi   the menu item to be added
     * @return      the menu item added
     * @see         java.awt.Menu#insert(java.lang.String, int)
     * @see         java.awt.Menu#insert(java.awt.MenuItem, int)
     */
    public MenuItem add(MenuItem mi) {
        synchronized (getTreeLock()) {
            if (mi.parent != null) {
                mi.parent.remove(mi);
            }
            items.addElement(mi);
            mi.parent = this;
            MenuPeer peer = (MenuPeer)this.peer;
            if (peer != null) {
                mi.addNotify();
                peer.addItem(mi);
            }
            return mi;
        }
    }

    /**
     * Adds an item with the specified label to this menu.
     *
     * @param       label   the text on the item
     * @see         java.awt.Menu#insert(java.lang.String, int)
     * @see         java.awt.Menu#insert(java.awt.MenuItem, int)
     */
    public void add(String label) {
        add(new MenuItem(label));
    }

    /**
     * Inserts a menu item into this menu
     * at the specified position.
     *
     * @param         menuitem  the menu item to be inserted.
     * @param         index     the position at which the menu
     *                          item should be inserted.
     * @see           java.awt.Menu#add(java.lang.String)
     * @see           java.awt.Menu#add(java.awt.MenuItem)
     * @exception     IllegalArgumentException if the value of
     *                    {@code index} is less than zero
     * @since         1.1
     */

    public void insert(MenuItem menuitem, int index) {
        synchronized (getTreeLock()) {
            if (index < 0) {
                throw new IllegalArgumentException("index less than zero.");
            }

            int nitems = getItemCount();
            Vector<MenuItem> tempItems = new Vector<>();

            /* Remove the item at index, nitems-index times
               storing them in a temporary vector in the
               order they appear on the menu.
            */
            for (int i = index ; i < nitems; i++) {
                tempItems.addElement(getItem(index));
                remove(index);
            }

            add(menuitem);

            /* Add the removed items back to the menu, they are
               already in the correct order in the temp vector.
            */
            for (int i = 0; i < tempItems.size()  ; i++) {
                add(tempItems.elementAt(i));
            }
        }
    }

    /**
     * Inserts a menu item with the specified label into this menu
     * at the specified position.  This is a convenience method for
     * {@code insert(menuItem, index)}.
     *
     * @param       label the text on the item
     * @param       index the position at which the menu item
     *                      should be inserted
     * @see         java.awt.Menu#add(java.lang.String)
     * @see         java.awt.Menu#add(java.awt.MenuItem)
     * @exception     IllegalArgumentException if the value of
     *                    {@code index} is less than zero
     * @since       1.1
     */

    public void insert(String label, int index) {
        insert(new MenuItem(label), index);
    }

    /**
     * Adds a separator line, or a hypen, to the menu at the current position.
     * @see         java.awt.Menu#insertSeparator(int)
     */
    public void addSeparator() {
        add("-");
    }

    /**
     * Inserts a separator at the specified position.
     * @param       index the position at which the
     *                       menu separator should be inserted.
     * @exception   IllegalArgumentException if the value of
     *                       {@code index} is less than 0.
     * @see         java.awt.Menu#addSeparator
     * @since       1.1
     */

    public void insertSeparator(int index) {
        synchronized (getTreeLock()) {
            if (index < 0) {
                throw new IllegalArgumentException("index less than zero.");
            }

            int nitems = getItemCount();
            Vector<MenuItem> tempItems = new Vector<>();

            /* Remove the item at index, nitems-index times
               storing them in a temporary vector in the
               order they appear on the menu.
            */
            for (int i = index ; i < nitems; i++) {
                tempItems.addElement(getItem(index));
                remove(index);
            }

            addSeparator();

            /* Add the removed items back to the menu, they are
               already in the correct order in the temp vector.
            */
            for (int i = 0; i < tempItems.size()  ; i++) {
                add(tempItems.elementAt(i));
            }
        }
    }

    /**
     * Removes the menu item at the specified index from this menu.
     * @param       index the position of the item to be removed.
     */
    public void remove(int index) {
        synchronized (getTreeLock()) {
            MenuItem mi = getItem(index);
            items.removeElementAt(index);
            MenuPeer peer = (MenuPeer)this.peer;
            if (peer != null) {
                peer.delItem(index);
                mi.removeNotify();
            }
            mi.parent = null;
        }
    }

    /**
     * Removes the specified menu item from this menu.
     * @param  item the item to be removed from the menu.
     *         If {@code item} is {@code null}
     *         or is not in this menu, this method does
     *         nothing.
     */
    public void remove(MenuComponent item) {
        synchronized (getTreeLock()) {
            int index = items.indexOf(item);
            if (index >= 0) {
                remove(index);
            }
        }
    }

    /**
     * Removes all items from this menu.
     * @since       1.1
     */
    public void removeAll() {
        synchronized (getTreeLock()) {
            int nitems = getItemCount();
            for (int i = nitems-1 ; i >= 0 ; i--) {
                remove(i);
            }
        }
    }

    /*
     * Post an ActionEvent to the target of the MenuPeer
     * associated with the specified keyboard event (on
     * keydown).  Returns true if there is an associated
     * keyboard event.
     */
    boolean handleShortcut(KeyEvent e) {
        int nitems = getItemCount();
        for (int i = 0 ; i < nitems ; i++) {
            MenuItem mi = getItem(i);
            if (mi.handleShortcut(e)) {
                return true;
            }
        }
        return false;
    }

    MenuItem getShortcutMenuItem(MenuShortcut s) {
        int nitems = getItemCount();
        for (int i = 0 ; i < nitems ; i++) {
            MenuItem mi = getItem(i).getShortcutMenuItem(s);
            if (mi != null) {
                return mi;
            }
        }
        return null;
    }

    synchronized Enumeration<MenuShortcut> shortcuts() {
        Vector<MenuShortcut> shortcuts = new Vector<>();
        int nitems = getItemCount();
        for (int i = 0 ; i < nitems ; i++) {
            MenuItem mi = getItem(i);
            if (mi instanceof Menu) {
                Enumeration<MenuShortcut> e = ((Menu)mi).shortcuts();
                while (e.hasMoreElements()) {
                    shortcuts.addElement(e.nextElement());
                }
            } else {
                MenuShortcut ms = mi.getShortcut();
                if (ms != null) {
                    shortcuts.addElement(ms);
                }
            }
        }
        return shortcuts.elements();
    }

    void deleteShortcut(MenuShortcut s) {
        int nitems = getItemCount();
        for (int i = 0 ; i < nitems ; i++) {
            getItem(i).deleteShortcut(s);
        }
    }


    /* Serialization support.  A MenuContainer is responsible for
     * restoring the parent fields of its children.
     */

    /**
     * The menu serialized Data Version.
     *
     * @serial
     */
    private int menuSerializedDataVersion = 1;

    /**
     * Writes default serializable fields to stream.
     *
     * @param  s the {@code ObjectOutputStream} to write
     * @throws IOException if an I/O error occurs
     * @see AWTEventMulticaster#save(ObjectOutputStream, String, EventListener)
     * @see #readObject(ObjectInputStream)
     */
    @Serial
    private void writeObject(java.io.ObjectOutputStream s)
      throws java.io.IOException
    {
      s.defaultWriteObject();
    }

    /**
     * Reads the {@code ObjectInputStream}.
     * Unrecognized keys or values will be ignored.
     *
     * @param  s the {@code ObjectInputStream} to read
     * @throws ClassNotFoundException if the class of a serialized object could
     *         not be found
     * @throws IOException if an I/O error occurs
     * @throws HeadlessException if {@code GraphicsEnvironment.isHeadless()}
     *         returns {@code true}
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see #writeObject(ObjectOutputStream)
     */
    @Serial
    private void readObject(ObjectInputStream s)
      throws IOException, ClassNotFoundException, HeadlessException
    {
      // HeadlessException will be thrown from MenuComponent's readObject
      s.defaultReadObject();
      for(int i = 0; i < items.size(); i++) {
        MenuItem item = items.elementAt(i);
        item.parent = this;
      }
    }

    /**
     * Returns a string representing the state of this {@code Menu}.
     * This method is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not be
     * {@code null}.
     *
     * @return the parameter string of this menu
     */
    public String paramString() {
        String str = ",tearOff=" + tearOff+",isHelpMenu=" + isHelpMenu;
        return super.paramString() + str;
    }

    /**
     * Initialize JNI field and method IDs
     */
    private static native void initIDs();


/////////////////
// Accessibility support
////////////////

    /**
     * Gets the AccessibleContext associated with this Menu.
     * For menus, the AccessibleContext takes the form of an
     * AccessibleAWTMenu.
     * A new AccessibleAWTMenu instance is created if necessary.
     *
     * @return an AccessibleAWTMenu that serves as the
     *         AccessibleContext of this Menu
     * @since 1.3
     */
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleAWTMenu();
        }
        return accessibleContext;
    }

    /**
     * Defined in MenuComponent. Overridden here.
     */
    int getAccessibleChildIndex(MenuComponent child) {
        return items.indexOf(child);
    }

    /**
     * Inner class of Menu used to provide default support for
     * accessibility.  This class is not meant to be used directly by
     * application developers, but is instead meant only to be
     * subclassed by menu component developers.
     * <p>
     * This class implements accessibility support for the
     * {@code Menu} class.  It provides an implementation of the
     * Java Accessibility API appropriate to menu user-interface elements.
     * @since 1.3
     */
    protected class AccessibleAWTMenu extends AccessibleAWTMenuItem
    {
        /**
         * Use serialVersionUID from JDK 1.3 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = 5228160894980069094L;

        /**
         * Constructs an {@code AccessibleAWTMenu}.
         */
        protected AccessibleAWTMenu() {}

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         * object
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.MENU;
        }

    } // class AccessibleAWTMenu

}
