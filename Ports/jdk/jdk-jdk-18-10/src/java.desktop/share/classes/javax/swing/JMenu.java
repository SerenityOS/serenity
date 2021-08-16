/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.BeanProperty;
import java.beans.JavaBean;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Vector;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.plaf.MenuItemUI;
import javax.swing.plaf.PopupMenuUI;

/**
 * An implementation of a menu -- a popup window containing
 * <code>JMenuItem</code>s that
 * is displayed when the user selects an item on the <code>JMenuBar</code>.
 * In addition to <code>JMenuItem</code>s, a <code>JMenu</code> can
 * also contain <code>JSeparator</code>s.
 * <p>
 * In essence, a menu is a button with an associated <code>JPopupMenu</code>.
 * When the "button" is pressed, the <code>JPopupMenu</code> appears. If the
 * "button" is on the <code>JMenuBar</code>, the menu is a top-level window.
 * If the "button" is another menu item, then the <code>JPopupMenu</code> is
 * "pull-right" menu.
 * <p>
 * Menus can be configured, and to some degree controlled, by
 * <code><a href="Action.html">Action</a></code>s.  Using an
 * <code>Action</code> with a menu has many benefits beyond directly
 * configuring a menu.  Refer to <a href="Action.html#buttonActions">
 * Swing Components Supporting <code>Action</code></a> for more
 * details, and you can find more information in <a
 * href="https://docs.oracle.com/javase/tutorial/uiswing/misc/action.html">How
 * to Use Actions</a>, a section in <em>The Java Tutorial</em>.
 * <p>
 * For information and examples of using menus see
 * <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/menu.html">How to Use Menus</a>,
 * a section in <em>The Java Tutorial.</em>
 * <p>
 * <strong>Warning:</strong> Swing is not thread safe. For more
 * information see <a
 * href="package-summary.html#threading">Swing's Threading
 * Policy</a>.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @author Georges Saab
 * @author David Karlton
 * @author Arnaud Weber
 * @see JMenuItem
 * @see JSeparator
 * @see JMenuBar
 * @see JPopupMenu
 * @since 1.2
 */
@JavaBean(description = "A popup window containing menu items displayed in a menu bar.")
@SwingContainer
@SuppressWarnings("serial")
public class JMenu extends JMenuItem implements Accessible,MenuElement
{
    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "MenuUI";

    /*
     * The popup menu portion of the menu.
     */
    private JPopupMenu popupMenu;

    /*
     * The button's model listeners.  Default is <code>null</code>.
     */
    private ChangeListener menuChangeListener = null;

    /*
     * Only one <code>MenuEvent</code> is needed for each menu since the
     * event's only state is the source property.  The source of events
     * generated is always "this".  Default is <code>null</code>.
     */
    private MenuEvent menuEvent = null;

    /*
     * Used by the look and feel (L&F) code to handle
     * implementation specific menu behaviors.
     */
    private int delay;

     /*
      * Location of the popup component. Location is <code>null</code>
      * if it was not customized by <code>setMenuLocation</code>
      */
     private Point customMenuLocation = null;

    /* Diagnostic aids -- should be false for production builds. */
    private static final boolean TRACE =   false; // trace creates and disposes
    private static final boolean VERBOSE = false; // show reuse hits/misses
    private static final boolean DEBUG =   false;  // show bad params, misc.

    /**
     * Constructs a new <code>JMenu</code> with no text.
     */
    public JMenu() {
        this("");
    }

    /**
     * Constructs a new <code>JMenu</code> with the supplied string
     * as its text.
     *
     * @param s  the text for the menu label
     */
    public JMenu(String s) {
        super(s);
    }

    /**
     * Constructs a menu whose properties are taken from the
     * <code>Action</code> supplied.
     * @param a an <code>Action</code>
     *
     * @since 1.3
     */
    public JMenu(Action a) {
        this();
        setAction(a);
    }

    /**
     * Constructs a new <code>JMenu</code> with the supplied string as
     * its text and specified as a tear-off menu or not.
     *
     * @param s the text for the menu label
     * @param b can the menu be torn off (not yet implemented)
     */
    public JMenu(String s, boolean b) {
        this(s);
    }


    /**
     * Overriden to do nothing. We want JMenu to be focusable, but
     * <code>JMenuItem</code> doesn't want to be, thus we override this
     * do nothing. We don't invoke <code>setFocusable(true)</code> after
     * super's constructor has completed as this has the side effect that
     * <code>JMenu</code> will be considered traversable via the
     * keyboard, which we don't want. Making a Component traversable by
     * the keyboard after invoking <code>setFocusable(true)</code> is OK,
     * as <code>setFocusable</code> is new API
     * and is speced as such, but internally we don't want to use it like
     * this else we change the keyboard traversability.
     */
    void initFocusability() {
    }

    /**
     * Resets the UI property with a value from the current look and feel.
     *
     * @see JComponent#updateUI
     */
    public void updateUI() {
        setUI((MenuItemUI)UIManager.getUI(this));

        if ( popupMenu != null )
          {
            popupMenu.setUI((PopupMenuUI)UIManager.getUI(popupMenu));
          }

    }


    /**
     * Returns the name of the L&amp;F class that renders this component.
     *
     * @return the string "MenuUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @BeanProperty(bound = false)
    public String getUIClassID() {
        return uiClassID;
    }

    //    public void repaint(long tm, int x, int y, int width, int height) {
    //        Thread.currentThread().dumpStack();
    //        super.repaint(tm,x,y,width,height);
    //    }

    /**
     * Sets the data model for the "menu button" -- the label
     * that the user clicks to open or close the menu.
     *
     * @param newModel the <code>ButtonModel</code>
     * @see #getModel
     */
    public void setModel(ButtonModel newModel) {
        ButtonModel oldModel = getModel();

        super.setModel(newModel);

        if (oldModel != null && menuChangeListener != null) {
            oldModel.removeChangeListener(menuChangeListener);
            menuChangeListener = null;
        }

        model = newModel;

        if (newModel != null) {
            menuChangeListener = createMenuChangeListener();
            newModel.addChangeListener(menuChangeListener);
        }
    }

    /**
     * Returns true if the menu is currently selected (highlighted).
     *
     * @return true if the menu is selected, else false
     */
    public boolean isSelected() {
        return getModel().isSelected();
    }

    /**
     * Sets the selection status of the menu.
     *
     * @param b  true to select (highlight) the menu; false to de-select
     *          the menu
     */
    @BeanProperty(expert = true, hidden = true, description
            = "When the menu is selected, its popup child is shown.")
    public void setSelected(boolean b) {
        ButtonModel model = getModel();
        boolean oldValue = model.isSelected();

        // TIGER - 4840653
        // Removed code which fired an AccessibleState.SELECTED
        // PropertyChangeEvent since this resulted in two
        // identical events being fired since
        // AbstractButton.fireItemStateChanged also fires the
        // same event. This caused screen readers to speak the
        // name of the item twice.

        if (b != model.isSelected()) {
            getModel().setSelected(b);
        }
    }

    /**
     * Returns true if the menu's popup window is visible.
     *
     * @return true if the menu is visible, else false
     */
    public boolean isPopupMenuVisible() {
        ensurePopupMenuCreated();
        return popupMenu.isVisible();
    }

    /**
     * Sets the visibility of the menu's popup.  If the menu is
     * not enabled, this method will have no effect.
     *
     * @param b  a boolean value -- true to make the menu visible,
     *           false to hide it
     */
    @BeanProperty(bound = false, expert = true, hidden = true, description
            = "The popup menu's visibility")
    public void setPopupMenuVisible(boolean b) {
        if (DEBUG) {
            System.out.println("in JMenu.setPopupMenuVisible " + b);
            // Thread.dumpStack();
        }

        boolean isVisible = isPopupMenuVisible();
        if (b != isVisible && (isEnabled() || !b)) {
            ensurePopupMenuCreated();
            if ((b==true) && isShowing()) {
                // Set location of popupMenu (pulldown or pullright)
                Point p = getCustomMenuLocation();
                if (p == null) {
                    p = getPopupMenuOrigin();
                }
                getPopupMenu().show(this, p.x, p.y);
            } else {
                getPopupMenu().setVisible(false);
            }
        }
    }

    /**
     * Computes the origin for the <code>JMenu</code>'s popup menu.
     * This method uses Look and Feel properties named
     * <code>Menu.menuPopupOffsetX</code>,
     * <code>Menu.menuPopupOffsetY</code>,
     * <code>Menu.submenuPopupOffsetX</code>, and
     * <code>Menu.submenuPopupOffsetY</code>
     * to adjust the exact location of popup.
     *
     * @return a <code>Point</code> in the coordinate space of the
     *          menu which should be used as the origin
     *          of the <code>JMenu</code>'s popup menu
     *
     * @since 1.3
     */
    protected Point getPopupMenuOrigin() {
        int x;
        int y;
        JPopupMenu pm = getPopupMenu();
        // Figure out the sizes needed to caclulate the menu position
        Dimension s = getSize();
        Dimension pmSize = pm.getSize();
        // For the first time the menu is popped up,
        // the size has not yet been initiated
        if (pmSize.width==0) {
            pmSize = pm.getPreferredSize();
        }
        Point position = getLocationOnScreen();
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        GraphicsConfiguration gc = getGraphicsConfiguration();
        Rectangle screenBounds = new Rectangle(toolkit.getScreenSize());
        GraphicsEnvironment ge =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gd = ge.getScreenDevices();
        for(int i = 0; i < gd.length; i++) {
            if(gd[i].getType() == GraphicsDevice.TYPE_RASTER_SCREEN) {
                GraphicsConfiguration dgc =
                    gd[i].getDefaultConfiguration();
                if(dgc.getBounds().contains(position)) {
                    gc = dgc;
                    break;
                }
            }
        }


        if (gc != null) {
            screenBounds = gc.getBounds();
            // take screen insets (e.g. taskbar) into account
            Insets screenInsets = toolkit.getScreenInsets(gc);

            screenBounds.width -=
                        Math.abs(screenInsets.left + screenInsets.right);
            screenBounds.height -=
                        Math.abs(screenInsets.top + screenInsets.bottom);
            position.x -= Math.abs(screenInsets.left);
            position.y -= Math.abs(screenInsets.top);
        }

        Container parent = getParent();
        if (parent instanceof JPopupMenu) {
            // We are a submenu (pull-right)
            int xOffset = UIManager.getInt("Menu.submenuPopupOffsetX");
            int yOffset = UIManager.getInt("Menu.submenuPopupOffsetY");

            if( SwingUtilities.isLeftToRight(this) ) {
                // First determine x:
                x = s.width + xOffset;   // Prefer placement to the right
                if (position.x + x + pmSize.width >= screenBounds.width
                                                     + screenBounds.x &&
                    // popup doesn't fit - place it wherever there's more room
                    screenBounds.width - s.width < 2*(position.x
                                                    - screenBounds.x)) {

                    x = 0 - xOffset - pmSize.width;
                }
            } else {
                // First determine x:
                x = 0 - xOffset - pmSize.width; // Prefer placement to the left
                if (position.x + x < screenBounds.x &&
                    // popup doesn't fit - place it wherever there's more room
                    screenBounds.width - s.width > 2*(position.x -
                                                    screenBounds.x)) {

                    x = s.width + xOffset;
                }
            }
            // Then the y:
            y = yOffset;                     // Prefer dropping down
            if (position.y + y + pmSize.height >= screenBounds.height
                                                  + screenBounds.y &&
                // popup doesn't fit - place it wherever there's more room
                screenBounds.height - s.height < 2*(position.y
                                                  - screenBounds.y)) {

                y = s.height - yOffset - pmSize.height;
            }
        } else {
            // We are a toplevel menu (pull-down)
            int xOffset = UIManager.getInt("Menu.menuPopupOffsetX");
            int yOffset = UIManager.getInt("Menu.menuPopupOffsetY");

            if( SwingUtilities.isLeftToRight(this) ) {
                // First determine the x:
                x = xOffset;                   // Extend to the right
                if (position.x + x + pmSize.width >= screenBounds.width
                                                     + screenBounds.x &&
                    // popup doesn't fit - place it wherever there's more room
                    screenBounds.width - s.width < 2*(position.x
                                                    - screenBounds.x)) {

                    x = s.width - xOffset - pmSize.width;
                }
            } else {
                // First determine the x:
                x = s.width - xOffset - pmSize.width; // Extend to the left
                if (position.x + x < screenBounds.x &&
                    // popup doesn't fit - place it wherever there's more room
                    screenBounds.width - s.width > 2*(position.x
                                                    - screenBounds.x)) {

                    x = xOffset;
                }
            }
            // Then the y:
            y = s.height + yOffset;    // Prefer dropping down
            if (position.y + y + pmSize.height >= screenBounds.height
                                                  + screenBounds.y &&
                // popup doesn't fit - place it wherever there's more room
                screenBounds.height - s.height < 2*(position.y
                                                  - screenBounds.y)) {

                y = 0 - yOffset - pmSize.height;   // Otherwise drop 'up'
            }
        }
        return new Point(x,y);
    }


    /**
     * Returns the suggested delay, in milliseconds, before submenus
     * are popped up or down.
     * Each look and feel (L&amp;F) may determine its own policy for
     * observing the <code>delay</code> property.
     * In most cases, the delay is not observed for top level menus
     * or while dragging.  The default for <code>delay</code> is 0.
     * This method is a property of the look and feel code and is used
     * to manage the idiosyncrasies of the various UI implementations.
     *
     *
     * @return the <code>delay</code> property
     */
    public int getDelay() {
        return delay;
    }

    /**
     * Sets the suggested delay before the menu's <code>PopupMenu</code>
     * is popped up or down.  Each look and feel (L&amp;F) may determine
     * it's own policy for observing the delay property.  In most cases,
     * the delay is not observed for top level menus or while dragging.
     * This method is a property of the look and feel code and is used
     * to manage the idiosyncrasies of the various UI implementations.
     *
     * @param       d the number of milliseconds to delay
     * @exception   IllegalArgumentException if <code>d</code>
     *                       is less than 0
     */
    @BeanProperty(bound = false, expert = true, description
            = "The delay between menu selection and making the popup menu visible")
    public void setDelay(int d) {
        if (d < 0)
            throw new IllegalArgumentException("Delay must be a positive integer");

        delay = d;
    }

    /**
     * The window-closing listener for the popup.
     *
     * @see WinListener
     */
    protected WinListener popupListener;

    private void ensurePopupMenuCreated() {
        if (popupMenu == null) {
            final JMenu thisMenu = this;
            this.popupMenu = new JPopupMenu();
            popupMenu.setInvoker(this);
            popupListener = createWinListener(popupMenu);
        }
    }

    /*
     * Return the customized location of the popup component.
     */
    private Point getCustomMenuLocation() {
        return customMenuLocation;
    }

    /**
     * Sets the location of the popup component.
     *
     * @param x the x coordinate of the popup's new position
     * @param y the y coordinate of the popup's new position
     */
    public void setMenuLocation(int x, int y) {
        customMenuLocation = new Point(x, y);
        if (popupMenu != null)
            popupMenu.setLocation(x, y);
    }

    /**
     * Appends a menu item to the end of this menu.
     * Returns the menu item added.
     *
     * @param menuItem the <code>JMenuitem</code> to be added
     * @return the <code>JMenuItem</code> added
     */
    public JMenuItem add(JMenuItem menuItem) {
        ensurePopupMenuCreated();
        return popupMenu.add(menuItem);
    }

    /**
     * Appends a component to the end of this menu.
     * Returns the component added.
     *
     * @param c the <code>Component</code> to add
     * @return the <code>Component</code> added
     */
    public Component add(Component c) {
        ensurePopupMenuCreated();
        popupMenu.add(c);
        return c;
    }

    /**
     * Adds the specified component to this container at the given
     * position. If <code>index</code> equals -1, the component will
     * be appended to the end.
     * @param     c   the <code>Component</code> to add
     * @param     index    the position at which to insert the component
     * @return    the <code>Component</code> added
     * @see       #remove
     * @see java.awt.Container#add(Component, int)
     */
    public Component add(Component c, int index) {
        ensurePopupMenuCreated();
        popupMenu.add(c, index);
        return c;
    }

    /**
     * Creates a new menu item with the specified text and appends
     * it to the end of this menu.
     *
     * @param s the string for the menu item to be added
     * @return the new {@code JMenuItem}
     */
    public JMenuItem add(String s) {
        return add(new JMenuItem(s));
    }

    /**
     * Creates a new menu item attached to the specified {@code Action} object
     * and appends it to the end of this menu.
     *
     * @param a the {@code Action} for the menu item to be added
     * @return the new {@code JMenuItem}
     * @see Action
     */
    public JMenuItem add(Action a) {
        JMenuItem mi = createActionComponent(a);
        mi.setAction(a);
        add(mi);
        return mi;
    }

    /**
     * Factory method which creates the <code>JMenuItem</code> for
     * <code>Action</code>s added to the <code>JMenu</code>.
     *
     * @param a the <code>Action</code> for the menu item to be added
     * @return the new menu item
     * @see Action
     *
     * @since 1.3
     */
    protected JMenuItem createActionComponent(Action a) {
        JMenuItem mi = new JMenuItem() {
            protected PropertyChangeListener createActionPropertyChangeListener(Action a) {
                PropertyChangeListener pcl = createActionChangeListener(this);
                if (pcl == null) {
                    pcl = super.createActionPropertyChangeListener(a);
                }
                return pcl;
            }
        };
        mi.setHorizontalTextPosition(JButton.TRAILING);
        mi.setVerticalTextPosition(JButton.CENTER);
        return mi;
    }

    /**
     * Returns a properly configured {@code PropertyChangeListener}
     * which updates the control as changes to the {@code Action} occur.
     *
     * @param b a menu item for which to create a {@code PropertyChangeListener}
     * @return a {@code PropertyChangeListener} for {@code b}
     */
    protected PropertyChangeListener createActionChangeListener(JMenuItem b) {
        return b.createActionPropertyChangeListener0(b.getAction());
    }

    /**
     * Appends a new separator to the end of the menu.
     */
    public void addSeparator()
    {
        ensurePopupMenuCreated();
        popupMenu.addSeparator();
    }

    /**
     * Inserts a new menu item with the specified text at a
     * given position.
     *
     * @param s the text for the menu item to add
     * @param pos an integer specifying the position at which to add the
     *               new menu item
     * @exception IllegalArgumentException when the value of
     *                  <code>pos</code> &lt; 0
     */
    public void insert(String s, int pos) {
        if (pos < 0) {
            throw new IllegalArgumentException("index less than zero.");
        }

        ensurePopupMenuCreated();
        popupMenu.insert(new JMenuItem(s), pos);
    }

    /**
     * Inserts the specified <code>JMenuitem</code> at a given position.
     *
     * @param mi the <code>JMenuitem</code> to add
     * @param pos an integer specifying the position at which to add the
     *               new <code>JMenuitem</code>
     * @return the new menu item
     * @exception IllegalArgumentException if the value of
     *                  <code>pos</code> &lt; 0
     */
    public JMenuItem insert(JMenuItem mi, int pos) {
        if (pos < 0) {
            throw new IllegalArgumentException("index less than zero.");
        }
        ensurePopupMenuCreated();
        popupMenu.insert(mi, pos);
        return mi;
    }

    /**
     * Inserts a new menu item attached to the specified <code>Action</code>
     * object at a given position.
     *
     * @param a the <code>Action</code> object for the menu item to add
     * @param pos an integer specifying the position at which to add the
     *               new menu item
     * @return the new menu item
     * @exception IllegalArgumentException if the value of
     *                  <code>pos</code> &lt; 0
     */
    public JMenuItem insert(Action a, int pos) {
        if (pos < 0) {
            throw new IllegalArgumentException("index less than zero.");
        }

        ensurePopupMenuCreated();
        JMenuItem mi = new JMenuItem(a);
        mi.setHorizontalTextPosition(JButton.TRAILING);
        mi.setVerticalTextPosition(JButton.CENTER);
        popupMenu.insert(mi, pos);
        return mi;
    }

    /**
     * Inserts a separator at the specified position.
     *
     * @param       index an integer specifying the position at which to
     *                    insert the menu separator
     * @exception   IllegalArgumentException if the value of
     *                       <code>index</code> &lt; 0
     */
    public void insertSeparator(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index less than zero.");
        }

        ensurePopupMenuCreated();
        popupMenu.insert( new JPopupMenu.Separator(), index );
    }

    /**
     * Returns the {@code JMenuItem} at the specified position.
     * If the component at {@code pos} is not a menu item,
     * {@code null} is returned.
     * This method is included for AWT compatibility.
     *
     * @param pos  an integer specifying the position
     * @return  the menu item at the specified position; or <code>null</code>
     *          if the item as the specified position is not a menu item
     * @exception  IllegalArgumentException if the value of
     *             {@code pos} &lt; 0
     */
    public JMenuItem getItem(int pos) {
        if (pos < 0) {
            throw new IllegalArgumentException("index less than zero.");
        }

        Component c = getMenuComponent(pos);
        if (c instanceof JMenuItem) {
            JMenuItem mi = (JMenuItem) c;
            return mi;
        }

        // 4173633
        return null;
    }

    /**
     * Returns the number of items on the menu, including separators.
     * This method is included for AWT compatibility.
     *
     * @return an integer equal to the number of items on the menu
     * @see #getMenuComponentCount
     */
    @BeanProperty(bound = false)
    public int getItemCount() {
        return getMenuComponentCount();
    }

    /**
     * Returns true if the menu can be torn off.  This method is not
     * yet implemented.
     *
     * @return true if the menu can be torn off, else false
     * @exception  Error  if invoked -- this method is not yet implemented
     */
    @BeanProperty(bound = false)
    public boolean isTearOff() {
        throw new Error("boolean isTearOff() {} not yet implemented");
    }

    /**
     * Removes the specified menu item from this menu.  If there is no
     * popup menu, this method will have no effect.
     *
     * @param    item the <code>JMenuItem</code> to be removed from the menu
     */
    public void remove(JMenuItem item) {
        if (popupMenu != null)
            popupMenu.remove(item);
    }

    /**
     * Removes the menu item at the specified index from this menu.
     *
     * @param       pos the position of the item to be removed
     * @exception   IllegalArgumentException if the value of
     *                       <code>pos</code> &lt; 0, or if <code>pos</code>
     *                       is greater than the number of menu items
     */
    public void remove(int pos) {
        if (pos < 0) {
            throw new IllegalArgumentException("index less than zero.");
        }
        if (pos > getItemCount()) {
            throw new IllegalArgumentException("index greater than the number of items.");
        }
        if (popupMenu != null)
            popupMenu.remove(pos);
    }

    /**
     * Removes the component <code>c</code> from this menu.
     *
     * @param       c the component to be removed
     */
    public void remove(Component c) {
        if (popupMenu != null)
            popupMenu.remove(c);
    }

    /**
     * Removes all menu items from this menu.
     */
    public void removeAll() {
        if (popupMenu != null)
            popupMenu.removeAll();
    }

    /**
     * Returns the number of components on the menu.
     *
     * @return an integer containing the number of components on the menu
     */
    @BeanProperty(bound = false)
    public int getMenuComponentCount() {
        int componentCount = 0;
        if (popupMenu != null)
            componentCount = popupMenu.getComponentCount();
        return componentCount;
    }

    /**
     * Returns the component at position <code>n</code>.
     *
     * @param n the position of the component to be returned
     * @return the component requested, or <code>null</code>
     *                  if there is no popup menu
     *
     */
    public Component getMenuComponent(int n) {
        if (popupMenu != null)
            return popupMenu.getComponent(n);

        return null;
    }

    /**
     * Returns an array of <code>Component</code>s of the menu's
     * subcomponents.  Note that this returns all <code>Component</code>s
     * in the popup menu, including separators.
     *
     * @return an array of <code>Component</code>s or an empty array
     *          if there is no popup menu
     */
    @BeanProperty(bound = false)
    public Component[] getMenuComponents() {
        if (popupMenu != null)
            return popupMenu.getComponents();

        return new Component[0];
    }

    /**
     * Returns true if the menu is a 'top-level menu', that is, if it is
     * the direct child of a menubar.
     *
     * @return true if the menu is activated from the menu bar;
     *         false if the menu is activated from a menu item
     *         on another menu
     */
    @BeanProperty(bound = false)
    public boolean isTopLevelMenu() {
        return getParent() instanceof JMenuBar;

    }

    /**
     * Returns true if the specified component exists in the
     * submenu hierarchy.
     *
     * @param c the <code>Component</code> to be tested
     * @return true if the <code>Component</code> exists, false otherwise
     */
    public boolean isMenuComponent(Component c) {
        // Are we in the MenuItem part of the menu
        if (c == this)
            return true;
        // Are we in the PopupMenu?
        if (c instanceof JPopupMenu) {
            JPopupMenu comp = (JPopupMenu) c;
            if (comp == this.getPopupMenu())
                return true;
        }
        // Are we in a Component on the PopupMenu
        int ncomponents = this.getMenuComponentCount();
        Component[] component = this.getMenuComponents();
        for (int i = 0 ; i < ncomponents ; i++) {
            Component comp = component[i];
            // Are we in the current component?
            if (comp == c)
                return true;
            // Hmmm, what about Non-menu containers?

            // Recursive call for the Menu case
            if (comp instanceof JMenu) {
                JMenu subMenu = (JMenu) comp;
                if (subMenu.isMenuComponent(c))
                    return true;
            }
        }
        return false;
    }


    /*
     * Returns a point in the coordinate space of this menu's popupmenu
     * which corresponds to the point <code>p</code> in the menu's
     * coordinate space.
     *
     * @param p the point to be translated
     * @return the point in the coordinate space of this menu's popupmenu
     */
    private Point translateToPopupMenu(Point p) {
        return translateToPopupMenu(p.x, p.y);
    }

    /*
     * Returns a point in the coordinate space of this menu's popupmenu
     * which corresponds to the point (x,y) in the menu's coordinate space.
     *
     * @param x the x coordinate of the point to be translated
     * @param y the y coordinate of the point to be translated
     * @return the point in the coordinate space of this menu's popupmenu
     */
    private Point translateToPopupMenu(int x, int y) {
            int newX;
            int newY;

            if (getParent() instanceof JPopupMenu) {
                newX = x - getSize().width;
                newY = y;
            } else {
                newX = x;
                newY = y - getSize().height;
            }

            return new Point(newX, newY);
        }

    /**
     * Returns the popupmenu associated with this menu.  If there is
     * no popupmenu, it will create one.
     *
     * @return the {@code JPopupMenu} associated with this menu
     */
    @BeanProperty(bound = false)
    public JPopupMenu getPopupMenu() {
        ensurePopupMenuCreated();
        return popupMenu;
    }

    /**
     * Adds a listener for menu events.
     *
     * @param l the listener to be added
     */
    public void addMenuListener(MenuListener l) {
        listenerList.add(MenuListener.class, l);
    }

    /**
     * Removes a listener for menu events.
     *
     * @param l the listener to be removed
     */
    public void removeMenuListener(MenuListener l) {
        listenerList.remove(MenuListener.class, l);
    }

    /**
     * Returns an array of all the <code>MenuListener</code>s added
     * to this JMenu with addMenuListener().
     *
     * @return all of the <code>MenuListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.4
     */
    @BeanProperty(bound = false)
    public MenuListener[] getMenuListeners() {
        return listenerList.getListeners(MenuListener.class);
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is created lazily.
     *
     * @exception Error  if there is a <code>null</code> listener
     * @see EventListenerList
     */
    protected void fireMenuSelected() {
        if (DEBUG) {
            System.out.println("In JMenu.fireMenuSelected");
        }
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==MenuListener.class) {
                if (listeners[i+1]== null) {
                    throw new Error(getText() +" has a NULL Listener!! " + i);
                } else {
                    // Lazily create the event:
                    if (menuEvent == null)
                        menuEvent = new MenuEvent(this);
                    ((MenuListener)listeners[i+1]).menuSelected(menuEvent);
                }
            }
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is created lazily.
     *
     * @exception Error if there is a <code>null</code> listener
     * @see EventListenerList
     */
    protected void fireMenuDeselected() {
        if (DEBUG) {
            System.out.println("In JMenu.fireMenuDeselected");
        }
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==MenuListener.class) {
                if (listeners[i+1]== null) {
                    throw new Error(getText() +" has a NULL Listener!! " + i);
                } else {
                    // Lazily create the event:
                    if (menuEvent == null)
                        menuEvent = new MenuEvent(this);
                    ((MenuListener)listeners[i+1]).menuDeselected(menuEvent);
                }
            }
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is created lazily.
     *
     * @exception Error if there is a <code>null</code> listener
     * @see EventListenerList
     */
    protected void fireMenuCanceled() {
        if (DEBUG) {
            System.out.println("In JMenu.fireMenuCanceled");
        }
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==MenuListener.class) {
                if (listeners[i+1]== null) {
                    throw new Error(getText() +" has a NULL Listener!! "
                                       + i);
                } else {
                    // Lazily create the event:
                    if (menuEvent == null)
                        menuEvent = new MenuEvent(this);
                    ((MenuListener)listeners[i+1]).menuCanceled(menuEvent);
                }
            }
        }
    }

    // Overriden to do nothing, JMenu doesn't support an accelerator
    void configureAcceleratorFromAction(Action a) {
    }

    @SuppressWarnings("serial")
    class MenuChangeListener implements ChangeListener, Serializable {
        boolean isSelected = false;
        public void stateChanged(ChangeEvent e) {
            ButtonModel model = (ButtonModel) e.getSource();
            boolean modelSelected = model.isSelected();

            if (modelSelected != isSelected) {
                if (modelSelected == true) {
                    fireMenuSelected();
                } else {
                    fireMenuDeselected();
                }
                isSelected = modelSelected;
            }
        }
    }

    private ChangeListener createMenuChangeListener() {
        return new MenuChangeListener();
    }


    /**
     * Creates a window-closing listener for the popup.
     *
     * @param p the <code>JPopupMenu</code>
     * @return the new window-closing listener
     *
     * @see WinListener
     */
    protected WinListener createWinListener(JPopupMenu p) {
        return new WinListener(p);
    }

    /**
     * A listener class that watches for a popup window closing.
     * When the popup is closing, the listener deselects the menu.
     * <p>
     * <strong>Warning:</strong>
     * Serialized objects of this class will not be compatible with
     * future Swing releases. The current serialization support is
     * appropriate for short term storage or RMI between applications running
     * the same version of Swing.  As of 1.4, support for long term storage
     * of all JavaBeans
     * has been added to the <code>java.beans</code> package.
     * Please see {@link java.beans.XMLEncoder}.
     */
    @SuppressWarnings("serial")
    protected class WinListener extends WindowAdapter implements Serializable {
        JPopupMenu popupMenu;
        /**
         *  Create the window listener for the specified popup.
         *
         * @param p the popup menu for which to create a listener
         * @since 1.4
         */
        public WinListener(JPopupMenu p) {
            this.popupMenu = p;
        }
        /**
         * Deselect the menu when the popup is closed from outside.
         */
        public void windowClosing(WindowEvent e) {
            setSelected(false);
        }
    }

    /**
     * Messaged when the menubar selection changes to activate or
     * deactivate this menu.
     * Overrides <code>JMenuItem.menuSelectionChanged</code>.
     *
     * @param isIncluded  true if this menu is active, false if
     *        it is not
     */
    public void menuSelectionChanged(boolean isIncluded) {
        if (DEBUG) {
            System.out.println("In JMenu.menuSelectionChanged to " + isIncluded);
        }
        setSelected(isIncluded);
    }

    /**
     * Returns an array of <code>MenuElement</code>s containing the submenu
     * for this menu component.  If popup menu is <code>null</code> returns
     * an empty array.  This method is required to conform to the
     * <code>MenuElement</code> interface.  Note that since
     * <code>JSeparator</code>s do not conform to the <code>MenuElement</code>
     * interface, this array will only contain <code>JMenuItem</code>s.
     *
     * @return an array of <code>MenuElement</code> objects
     */
    @BeanProperty(bound = false)
    public MenuElement[] getSubElements() {
        if(popupMenu == null)
            return new MenuElement[0];
        else {
            MenuElement[] result = new MenuElement[1];
            result[0] = popupMenu;
            return result;
        }
    }


    // implements javax.swing.MenuElement
    /**
     * Returns the <code>java.awt.Component</code> used to
     * paint this <code>MenuElement</code>.
     * The returned component is used to convert events and detect if
     * an event is inside a menu component.
     */
    public Component getComponent() {
        return this;
    }


    /**
     * Sets the <code>ComponentOrientation</code> property of this menu
     * and all components contained within it. This includes all
     * components returned by {@link #getMenuComponents getMenuComponents}.
     *
     * @param o the new component orientation of this menu and
     *        the components contained within it.
     * @exception NullPointerException if <code>orientation</code> is null.
     * @see java.awt.Component#setComponentOrientation
     * @see java.awt.Component#getComponentOrientation
     * @since 1.4
     */
    public void applyComponentOrientation(ComponentOrientation o) {
        super.applyComponentOrientation(o);

        if ( popupMenu != null ) {
            int ncomponents = getMenuComponentCount();
            for (int i = 0 ; i < ncomponents ; ++i) {
                getMenuComponent(i).applyComponentOrientation(o);
            }
            popupMenu.setComponentOrientation(o);
        }
    }

    /**
     * Sets the orientation for this menu and its associated popup menu
     * determined by the {@code ComponentOrientation} argument.
     *
     * @param o the new orientation for this menu and
     *          its associated popup menu.
     */
    public void setComponentOrientation(ComponentOrientation o) {
        super.setComponentOrientation(o);
        if ( popupMenu != null ) {
            popupMenu.setComponentOrientation(o);
        }
    }

    /**
     * <code>setAccelerator</code> is not defined for <code>JMenu</code>.
     * Use <code>setMnemonic</code> instead.
     * @param keyStroke  the keystroke combination which will invoke
     *                  the <code>JMenuItem</code>'s actionlisteners
     *                  without navigating the menu hierarchy
     * @exception Error  if invoked -- this method is not defined for JMenu.
     *                  Use <code>setMnemonic</code> instead
     */
    public void setAccelerator(KeyStroke keyStroke) {
        throw new Error("setAccelerator() is not defined for JMenu.  Use setMnemonic() instead.");
    }

    /**
     * Processes key stroke events such as mnemonics and accelerators.
     *
     * @param evt  the key event to be processed
     */
    protected void processKeyEvent(KeyEvent evt) {
        MenuSelectionManager.defaultManager().processKeyEvent(evt);
        if (evt.isConsumed())
            return;

        super.processKeyEvent(evt);
    }

    /**
     * Programmatically performs a "click".  This overrides the method
     * <code>AbstractButton.doClick</code> in order to make the menu pop up.
     * @param pressTime  indicates the number of milliseconds the
     *          button was pressed for
     */
    public void doClick(int pressTime) {
        MenuElement[] me = buildMenuElementArray(this);
        MenuSelectionManager.defaultManager().setSelectedPath(me);
    }

    /*
     * Build an array of menu elements - from <code>PopupMenu</code> to
     * the root <code>JMenuBar</code>.
     * @param  leaf  the leaf node from which to start building up the array
     * @return the array of menu items
     */
    private MenuElement[] buildMenuElementArray(JMenu leaf) {
        Vector<MenuElement> elements = new Vector<>();
        Component current = leaf.getPopupMenu();
        JPopupMenu pop;
        JMenu menu;
        JMenuBar bar;

        while (true) {
            if (current instanceof JPopupMenu) {
                pop = (JPopupMenu) current;
                elements.insertElementAt(pop, 0);
                current = pop.getInvoker();
            } else if (current instanceof JMenu) {
                menu = (JMenu) current;
                elements.insertElementAt(menu, 0);
                current = menu.getParent();
            } else if (current instanceof JMenuBar) {
                bar = (JMenuBar) current;
                elements.insertElementAt(bar, 0);
                break;
            } else {
                break;
            }
        }
        MenuElement[] me = new MenuElement[elements.size()];
        elements.copyInto(me);
        return me;
    }


    /**
     * See <code>readObject</code> and <code>writeObject</code> in
     * <code>JComponent</code> for more
     * information about serialization in Swing.
     */
    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        if (getUIClassID().equals(uiClassID)) {
            byte count = JComponent.getWriteObjCounter(this);
            JComponent.setWriteObjCounter(this, --count);
            if (count == 0 && ui != null) {
                ui.installUI(this);
            }
        }
    }


    /**
     * Returns a string representation of this <code>JMenu</code>. This
     * method is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this JMenu.
     */
    protected String paramString() {
        return super.paramString();
    }


/////////////////
// Accessibility support
////////////////

    /**
     * Gets the AccessibleContext associated with this JMenu.
     * For JMenus, the AccessibleContext takes the form of an
     * AccessibleJMenu.
     * A new AccessibleJMenu instance is created if necessary.
     *
     * @return an AccessibleJMenu that serves as the
     *         AccessibleContext of this JMenu
     */
    @BeanProperty(bound = false)
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJMenu();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * <code>JMenu</code> class.  It provides an implementation of the
     * Java Accessibility API appropriate to menu user-interface elements.
     * <p>
     * <strong>Warning:</strong>
     * Serialized objects of this class will not be compatible with
     * future Swing releases. The current serialization support is
     * appropriate for short term storage or RMI between applications running
     * the same version of Swing.  As of 1.4, support for long term storage
     * of all JavaBeans
     * has been added to the <code>java.beans</code> package.
     * Please see {@link java.beans.XMLEncoder}.
     */
    @SuppressWarnings("serial")
    protected class AccessibleJMenu extends AccessibleJMenuItem
        implements AccessibleSelection {

        /**
         * Constructs an {@code AccessibleJMenu}.
         */
        protected AccessibleJMenu() {}

        /**
         * Returns the number of accessible children in the object.  If all
         * of the children of this object implement Accessible, than this
         * method should return the number of children of this object.
         *
         * @return the number of accessible children in the object.
         */
        public int getAccessibleChildrenCount() {
            Component[] children = getMenuComponents();
            int count = 0;
            for (Component child : children) {
                if (child instanceof Accessible) {
                    count++;
                }
            }
            return count;
        }

        /**
         * Returns the nth Accessible child of the object.
         *
         * @param i zero-based index of child
         * @return the nth Accessible child of the object
         */
        public Accessible getAccessibleChild(int i) {
            Component[] children = getMenuComponents();
            int count = 0;
            for (Component child : children) {
                if (child instanceof Accessible) {
                    if (count == i) {
                        if (child instanceof JComponent) {
                            // FIXME:  [[[WDW - probably should set this when
                            // the component is added to the menu.  I tried
                            // to do this in most cases, but the separators
                            // added by addSeparator are hard to get to.]]]
                            AccessibleContext ac = child.getAccessibleContext();
                            ac.setAccessibleParent(JMenu.this);
                        }
                        return (Accessible) child;
                    } else {
                        count++;
                    }
                }
            }
            return null;
        }

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         * object
         * @see AccessibleRole
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.MENU;
        }

        /**
         * Get the AccessibleSelection associated with this object.  In the
         * implementation of the Java Accessibility API for this class,
         * return this object, which is responsible for implementing the
         * AccessibleSelection interface on behalf of itself.
         *
         * @return this object
         */
        public AccessibleSelection getAccessibleSelection() {
            return this;
        }

        /**
         * Returns 1 if a sub-menu is currently selected in this menu.
         *
         * @return 1 if a menu is currently selected, else 0
         */
        public int getAccessibleSelectionCount() {
            MenuElement[] me =
                MenuSelectionManager.defaultManager().getSelectedPath();
            if (me != null) {
                for (int i = 0; i < me.length; i++) {
                    if (me[i] == JMenu.this) {   // this menu is selected
                        if (i+1 < me.length) {
                            return 1;
                        }
                    }
                }
            }
            return 0;
        }

        /**
         * Returns the currently selected sub-menu if one is selected,
         * otherwise null (there can only be one selection, and it can
         * only be a sub-menu, as otherwise menu items don't remain
         * selected).
         */
        public Accessible getAccessibleSelection(int i) {
            // if i is a sub-menu & popped, return it
            if (i < 0 || i >= getItemCount()) {
                return null;
            }
            MenuElement[] me =
                MenuSelectionManager.defaultManager().getSelectedPath();
            if (me != null) {
                for (int j = 0; j < me.length; j++) {
                    if (me[j] == JMenu.this) {   // this menu is selected
                        // so find the next JMenuItem in the MenuElement
                        // array, and return it!
                        while (++j < me.length) {
                            if (me[j] instanceof JMenuItem) {
                                return (Accessible) me[j];
                            }
                        }
                    }
                }
            }
            return null;
        }

        /**
         * Returns true if the current child of this object is selected
         * (that is, if this child is a popped-up submenu).
         *
         * @param i the zero-based index of the child in this Accessible
         * object.
         * @see AccessibleContext#getAccessibleChild
         */
        public boolean isAccessibleChildSelected(int i) {
            // if i is a sub-menu and is pop-ed up, return true, else false
            MenuElement[] me =
                MenuSelectionManager.defaultManager().getSelectedPath();
            if (me != null) {
                JMenuItem mi = JMenu.this.getItem(i);
                for (int j = 0; j < me.length; j++) {
                    if (me[j] == mi) {
                        return true;
                    }
                }
            }
            return false;
        }


        /**
         * Selects the <code>i</code>th menu in the menu.
         * If that item is a submenu,
         * it will pop up in response.  If a different item is already
         * popped up, this will force it to close.  If this is a sub-menu
         * that is already popped up (selected), this method has no
         * effect.
         *
         * @param i the index of the item to be selected
         * @see #getAccessibleStateSet
         */
        public void addAccessibleSelection(int i) {
            if (i < 0 || i >= getItemCount()) {
                return;
            }
            JMenuItem mi = getItem(i);
            if (mi != null) {
                if (mi instanceof JMenu) {
                    MenuElement[] me = buildMenuElementArray((JMenu) mi);
                    MenuSelectionManager.defaultManager().setSelectedPath(me);
                } else {
                    MenuSelectionManager.defaultManager().setSelectedPath(null);
                }
            }
        }

        /**
         * Removes the nth item from the selection.  In general, menus
         * can only have one item within them selected at a time
         * (e.g. one sub-menu popped open).
         *
         * @param i the zero-based index of the selected item
         */
        public void removeAccessibleSelection(int i) {
            if (i < 0 || i >= getItemCount()) {
                return;
            }
            JMenuItem mi = getItem(i);
            if (mi != null && mi instanceof JMenu) {
                if (mi.isSelected()) {
                    MenuElement[] old =
                        MenuSelectionManager.defaultManager().getSelectedPath();
                    MenuElement[] me = new MenuElement[old.length-2];
                    for (int j = 0; j < old.length -2; j++) {
                        me[j] = old[j];
                    }
                    MenuSelectionManager.defaultManager().setSelectedPath(me);
                }
            }
        }

        /**
         * Clears the selection in the object, so that nothing in the
         * object is selected.  This will close any open sub-menu.
         */
        public void clearAccessibleSelection() {
            // if this menu is selected, reset selection to only go
            // to this menu; else do nothing
            MenuElement[] old =
                MenuSelectionManager.defaultManager().getSelectedPath();
            if (old != null) {
                for (int j = 0; j < old.length; j++) {
                    if (old[j] == JMenu.this) {  // menu is in the selection!
                        MenuElement[] me = new MenuElement[j+1];
                        System.arraycopy(old, 0, me, 0, j);
                        me[j] = JMenu.this.getPopupMenu();
                        MenuSelectionManager.defaultManager().setSelectedPath(me);
                    }
                }
            }
        }

        /**
         * Normally causes every selected item in the object to be selected
         * if the object supports multiple selections.  This method
         * makes no sense in a menu bar, and so does nothing.
         */
        public void selectAllAccessibleSelection() {
        }
    } // inner class AccessibleJMenu

}
