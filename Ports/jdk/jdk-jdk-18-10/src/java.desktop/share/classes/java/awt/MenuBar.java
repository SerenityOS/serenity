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
import java.awt.peer.MenuBarPeer;
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
 * The {@code MenuBar} class encapsulates the platform's
 * concept of a menu bar bound to a frame. In order to associate
 * the menu bar with a {@code Frame} object, call the
 * frame's {@code setMenuBar} method.
 * <p>
 * <a id="mbexample"></a><!-- target for cross references -->
 * This is what a menu bar might look like:
 * <p>
 * <img src="doc-files/MenuBar-1.gif"
 * alt="Diagram of MenuBar containing 2 menus: Examples and Options. Examples
 * menu is expanded showing items: Basic, Simple, Check, and More Examples."
 * style="margin: 7px 10px;">
 * <p>
 * A menu bar handles keyboard shortcuts for menu items, passing them
 * along to its child menus.
 * (Keyboard shortcuts, which are optional, provide the user with
 * an alternative to the mouse for invoking a menu item and the
 * action that is associated with it.)
 * Each menu item can maintain an instance of {@code MenuShortcut}.
 * The {@code MenuBar} class defines several methods,
 * {@link MenuBar#shortcuts} and
 * {@link MenuBar#getShortcutMenuItem}
 * that retrieve information about the shortcuts a given
 * menu bar is managing.
 *
 * @author Sami Shaio
 * @see        java.awt.Frame
 * @see        java.awt.Frame#setMenuBar(java.awt.MenuBar)
 * @see        java.awt.Menu
 * @see        java.awt.MenuItem
 * @see        java.awt.MenuShortcut
 * @since      1.0
 */
public class MenuBar extends MenuComponent implements MenuContainer, Accessible {

    static {
        /* ensure that the necessary native libraries are loaded */
        Toolkit.loadLibraries();
        if (!GraphicsEnvironment.isHeadless()) {
            initIDs();
        }
        AWTAccessor.setMenuBarAccessor(
            new AWTAccessor.MenuBarAccessor() {
                public Menu getHelpMenu(MenuBar menuBar) {
                    return menuBar.helpMenu;
                }

                public Vector<Menu> getMenus(MenuBar menuBar) {
                    return menuBar.menus;
                }
            });
    }

    /**
     * This field represents a vector of the
     * actual menus that will be part of the MenuBar.
     *
     * @serial
     * @see #countMenus()
     */
    private final Vector<Menu> menus = new Vector<>();

    /**
     * This menu is a special menu dedicated to
     * help.  The one thing to note about this menu
     * is that on some platforms it appears at the
     * right edge of the menubar.
     *
     * @serial
     * @see #getHelpMenu()
     * @see #setHelpMenu(Menu)
     */
    private volatile Menu helpMenu;

    private static final String base = "menubar";
    private static int nameCounter = 0;

    /**
     * Use serialVersionUID from JDK 1.1 for interoperability.
     */
     @Serial
     private static final long serialVersionUID = -4930327919388951260L;

    /**
     * Creates a new menu bar.
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     * returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public MenuBar() throws HeadlessException {
    }

    /**
     * Construct a name for this MenuComponent.  Called by getName() when
     * the name is null.
     */
    String constructComponentName() {
        synchronized (MenuBar.class) {
            return base + nameCounter++;
        }
    }

    /**
     * Creates the menu bar's peer.  The peer allows us to change the
     * appearance of the menu bar without changing any of the menu bar's
     * functionality.
     */
    public void addNotify() {
        synchronized (getTreeLock()) {
            if (peer == null)
                peer = getComponentFactory().createMenuBar(this);

            int nmenus = getMenuCount();
            for (int i = 0 ; i < nmenus ; i++) {
                getMenu(i).addNotify();
            }
        }
    }

    /**
     * Removes the menu bar's peer.  The peer allows us to change the
     * appearance of the menu bar without changing any of the menu bar's
     * functionality.
     */
    public void removeNotify() {
        synchronized (getTreeLock()) {
            int nmenus = getMenuCount();
            for (int i = 0 ; i < nmenus ; i++) {
                getMenu(i).removeNotify();
            }
            super.removeNotify();
        }
    }

    /**
     * Gets the help menu on the menu bar.
     * @return    the help menu on this menu bar.
     */
    public Menu getHelpMenu() {
        return helpMenu;
    }

    /**
     * Sets the specified menu to be this menu bar's help menu.
     * If this menu bar has an existing help menu, the old help menu is
     * removed from the menu bar, and replaced with the specified menu.
     * @param m    the menu to be set as the help menu
     */
    public void setHelpMenu(final Menu m) {
        synchronized (getTreeLock()) {
            if (helpMenu == m) {
                return;
            }
            if (helpMenu != null) {
                remove(helpMenu);
            }
            helpMenu = m;
            if (m != null) {
                if (m.parent != this) {
                    add(m);
                }
                m.isHelpMenu = true;
                m.parent = this;
                MenuBarPeer peer = (MenuBarPeer)this.peer;
                if (peer != null) {
                    if (m.peer == null) {
                        m.addNotify();
                    }
                    peer.addHelpMenu(m);
                }
            }
        }
    }

    /**
     * Adds the specified menu to the menu bar.
     * If the menu has been part of another menu bar,
     * removes it from that menu bar.
     *
     * @param        m   the menu to be added
     * @return       the menu added
     * @see          java.awt.MenuBar#remove(int)
     * @see          java.awt.MenuBar#remove(java.awt.MenuComponent)
     */
    public Menu add(Menu m) {
        synchronized (getTreeLock()) {
            if (m.parent != null) {
                m.parent.remove(m);
            }
            m.parent = this;

            MenuBarPeer peer = (MenuBarPeer)this.peer;
            if (peer != null) {
                if (m.peer == null) {
                    m.addNotify();
                }
                menus.addElement(m);
                peer.addMenu(m);
            } else {
                menus.addElement(m);
            }
            return m;
        }
    }

    /**
     * Removes the menu located at the specified
     * index from this menu bar.
     * @param        index   the position of the menu to be removed.
     * @see          java.awt.MenuBar#add(java.awt.Menu)
     */
    public void remove(final int index) {
        synchronized (getTreeLock()) {
            Menu m = getMenu(index);
            menus.removeElementAt(index);
            MenuBarPeer peer = (MenuBarPeer)this.peer;
            if (peer != null) {
                peer.delMenu(index);
                m.removeNotify();
            }
            m.parent = null;
            if (helpMenu == m) {
                helpMenu = null;
                m.isHelpMenu = false;
            }
        }
    }

    /**
     * Removes the specified menu component from this menu bar.
     * @param        m the menu component to be removed.
     * @see          java.awt.MenuBar#add(java.awt.Menu)
     */
    public void remove(MenuComponent m) {
        synchronized (getTreeLock()) {
            int index = menus.indexOf(m);
            if (index >= 0) {
                remove(index);
            }
        }
    }

    /**
     * Gets the number of menus on the menu bar.
     * @return     the number of menus on the menu bar.
     * @since      1.1
     */
    public int getMenuCount() {
        return countMenus();
    }

    /**
     * Gets the number of menus on the menu bar.
     *
     * @return the number of menus on the menu bar.
     * @deprecated As of JDK version 1.1,
     * replaced by {@code getMenuCount()}.
     */
    @Deprecated
    public int countMenus() {
        return getMenuCountImpl();
    }

    /*
     * This is called by the native code, so client code can't
     * be called on the toolkit thread.
     */
    final int getMenuCountImpl() {
        return menus.size();
    }

    /**
     * Gets the specified menu.
     * @param      i the index position of the menu to be returned.
     * @return     the menu at the specified index of this menu bar.
     */
    public Menu getMenu(int i) {
        return getMenuImpl(i);
    }

    /*
     * This is called by the native code, so client code can't
     * be called on the toolkit thread.
     */
    final Menu getMenuImpl(int i) {
        return menus.elementAt(i);
    }

    /**
     * Gets an enumeration of all menu shortcuts this menu bar
     * is managing.
     * @return      an enumeration of menu shortcuts that this
     *                      menu bar is managing.
     * @see         java.awt.MenuShortcut
     * @since       1.1
     */
    public synchronized Enumeration<MenuShortcut> shortcuts() {
        Vector<MenuShortcut> shortcuts = new Vector<>();
        int nmenus = getMenuCount();
        for (int i = 0 ; i < nmenus ; i++) {
            Enumeration<MenuShortcut> e = getMenu(i).shortcuts();
            while (e.hasMoreElements()) {
                shortcuts.addElement(e.nextElement());
            }
        }
        return shortcuts.elements();
    }

    /**
     * Gets the instance of {@code MenuItem} associated
     * with the specified {@code MenuShortcut} object,
     * or {@code null} if none of the menu items being managed
     * by this menu bar is associated with the specified menu
     * shortcut.
     * @param  s the specified menu shortcut.
     * @return the menu item for the specified shortcut.
     * @see java.awt.MenuItem
     * @see java.awt.MenuShortcut
     * @since 1.1
     */
     public MenuItem getShortcutMenuItem(MenuShortcut s) {
        int nmenus = getMenuCount();
        for (int i = 0 ; i < nmenus ; i++) {
            MenuItem mi = getMenu(i).getShortcutMenuItem(s);
            if (mi != null) {
                return mi;
            }
        }
        return null;  // MenuShortcut wasn't found
     }

    /*
     * Post an ACTION_EVENT to the target of the MenuPeer
     * associated with the specified keyboard event (on
     * keydown).  Returns true if there is an associated
     * keyboard event.
     */
    boolean handleShortcut(KeyEvent e) {
        // Is it a key event?
        int id = e.getID();
        if (id != KeyEvent.KEY_PRESSED && id != KeyEvent.KEY_RELEASED) {
            return false;
        }

        // Is the accelerator modifier key pressed?
        int accelKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        if ((e.getModifiersEx() & accelKey) == 0) {
            return false;
        }

        // Pass MenuShortcut on to child menus.
        int nmenus = getMenuCount();
        for (int i = 0 ; i < nmenus ; i++) {
            Menu m = getMenu(i);
            if (m.handleShortcut(e)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deletes the specified menu shortcut.
     * @param     s the menu shortcut to delete.
     * @since     1.1
     */
    public void deleteShortcut(MenuShortcut s) {
        int nmenus = getMenuCount();
        for (int i = 0 ; i < nmenus ; i++) {
            getMenu(i).deleteShortcut(s);
        }
    }

    /* Serialization support.  Restore the (transient) parent
     * fields of Menubar menus here.
     */

    /**
     * The MenuBar's serialized data version.
     *
     * @serial
     */
    private int menuBarSerializedDataVersion = 1;

    /**
     * Writes default serializable fields to stream.
     *
     * @param  s the {@code ObjectOutputStream} to write
     * @throws IOException if an I/O error occurs
     * @see AWTEventMulticaster#save(ObjectOutputStream, String, EventListener)
     * @see #readObject(java.io.ObjectInputStream)
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
     * @see #writeObject(java.io.ObjectOutputStream)
     */
    @Serial
    private void readObject(ObjectInputStream s)
      throws ClassNotFoundException, IOException, HeadlessException
    {
      // HeadlessException will be thrown from MenuComponent's readObject
      s.defaultReadObject();
      for (int i = 0; i < menus.size(); i++) {
        Menu m = menus.elementAt(i);
        m.parent = this;
      }
    }

    /**
     * Initialize JNI field and method IDs
     */
    private static native void initIDs();


/////////////////
// Accessibility support
////////////////

    /**
     * Gets the AccessibleContext associated with this MenuBar.
     * For menu bars, the AccessibleContext takes the form of an
     * AccessibleAWTMenuBar.
     * A new AccessibleAWTMenuBar instance is created if necessary.
     *
     * @return an AccessibleAWTMenuBar that serves as the
     *         AccessibleContext of this MenuBar
     * @since 1.3
     */
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleAWTMenuBar();
        }
        return accessibleContext;
    }

    /**
     * Defined in MenuComponent. Overridden here.
     */
    int getAccessibleChildIndex(MenuComponent child) {
        return menus.indexOf(child);
    }

    /**
     * Inner class of MenuBar used to provide default support for
     * accessibility.  This class is not meant to be used directly by
     * application developers, but is instead meant only to be
     * subclassed by menu component developers.
     * <p>
     * This class implements accessibility support for the
     * {@code MenuBar} class.  It provides an implementation of the
     * Java Accessibility API appropriate to menu bar user-interface elements.
     * @since 1.3
     */
    protected class AccessibleAWTMenuBar extends AccessibleAWTMenuComponent
    {
        /**
         * Use serialVersionUID from JDK 1.3 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = -8577604491830083815L;

        /**
         * Constructs an {@code AccessibleAWTMenuBar}.
         */
        protected AccessibleAWTMenuBar() {}

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         * object
         * @since 1.4
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.MENU_BAR;
        }

    } // class AccessibleAWTMenuBar

}
