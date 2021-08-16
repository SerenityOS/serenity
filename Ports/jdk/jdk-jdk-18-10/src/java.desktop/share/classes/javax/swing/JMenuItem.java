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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.BeanProperty;
import java.beans.JavaBean;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.MenuDragMouseEvent;
import javax.swing.event.MenuDragMouseListener;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;
import javax.swing.plaf.MenuItemUI;

/**
 * An implementation of an item in a menu. A menu item is essentially a button
 * sitting in a list. When the user selects the "button", the action
 * associated with the menu item is performed. A <code>JMenuItem</code>
 * contained in a <code>JPopupMenu</code> performs exactly that function.
 * <p>
 * Menu items can be configured, and to some degree controlled, by
 * <code><a href="Action.html">Action</a></code>s.  Using an
 * <code>Action</code> with a menu item has many benefits beyond directly
 * configuring a menu item.  Refer to <a href="Action.html#buttonActions">
 * Swing Components Supporting <code>Action</code></a> for more
 * details, and you can find more information in <a
 * href="https://docs.oracle.com/javase/tutorial/uiswing/misc/action.html">How
 * to Use Actions</a>, a section in <em>The Java Tutorial</em>.
 * <p>
 * For further documentation and for examples, see
 * <a
 href="https://docs.oracle.com/javase/tutorial/uiswing/components/menu.html">How to Use Menus</a>
 * in <em>The Java Tutorial.</em>
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
 * @see JPopupMenu
 * @see JMenu
 * @see JCheckBoxMenuItem
 * @see JRadioButtonMenuItem
 * @since 1.2
 */
@JavaBean(defaultProperty = "UIClassID", description = "An item which can be selected in a menu.")
@SwingContainer(false)
@SuppressWarnings("serial")
public class JMenuItem extends AbstractButton implements Accessible,MenuElement  {

    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "MenuItemUI";

    /* diagnostic aids -- should be false for production builds. */
    private static final boolean TRACE =   false; // trace creates and disposes
    private static final boolean VERBOSE = false; // show reuse hits/misses
    private static final boolean DEBUG =   false;  // show bad params, misc.

    private boolean isMouseDragged = false;

    /**
     * Creates a <code>JMenuItem</code> with no set text or icon.
     */
    public JMenuItem() {
        this(null, (Icon)null);
    }

    /**
     * Creates a <code>JMenuItem</code> with the specified icon.
     *
     * @param icon the icon of the <code>JMenuItem</code>
     */
    public JMenuItem(Icon icon) {
        this(null, icon);
    }

    /**
     * Creates a <code>JMenuItem</code> with the specified text.
     *
     * @param text the text of the <code>JMenuItem</code>
     */
    public JMenuItem(String text) {
        this(text, (Icon)null);
    }

    /**
     * Creates a menu item whose properties are taken from the
     * specified <code>Action</code>.
     *
     * @param a the action of the <code>JMenuItem</code>
     * @since 1.3
     */
    public JMenuItem(Action a) {
        this();
        setAction(a);
    }

    /**
     * Creates a <code>JMenuItem</code> with the specified text and icon.
     *
     * @param text the text of the <code>JMenuItem</code>
     * @param icon the icon of the <code>JMenuItem</code>
     */
    public JMenuItem(String text, Icon icon) {
        setModel(new DefaultButtonModel());
        init(text, icon);
        initFocusability();
    }

    /**
     * Creates a <code>JMenuItem</code> with the specified text and
     * keyboard mnemonic.
     *
     * @param text the text of the <code>JMenuItem</code>
     * @param mnemonic the keyboard mnemonic for the <code>JMenuItem</code>
     */
    public JMenuItem(String text, int mnemonic) {
        setModel(new DefaultButtonModel());
        init(text, null);
        setMnemonic(mnemonic);
        initFocusability();
    }

    /**
     * {@inheritDoc}
     */
    public void setModel(ButtonModel newModel) {
        super.setModel(newModel);
        if(newModel instanceof DefaultButtonModel) {
            ((DefaultButtonModel)newModel).setMenuItem(true);
        }
    }

    /**
     * Inititalizes the focusability of the <code>JMenuItem</code>.
     * <code>JMenuItem</code>'s are focusable, but subclasses may
     * want to be, this provides them the opportunity to override this
     * and invoke something else, or nothing at all. Refer to
     * {@link javax.swing.JMenu#initFocusability} for the motivation of
     * this.
     */
    void initFocusability() {
        setFocusable(false);
    }

    /**
     * Initializes the menu item with the specified text and icon.
     *
     * @param text the text of the <code>JMenuItem</code>
     * @param icon the icon of the <code>JMenuItem</code>
     */
    protected void init(String text, Icon icon) {
        if(text != null) {
            setText(text);
        }

        if(icon != null) {
            setIcon(icon);
        }

        // Listen for Focus events
        addFocusListener(new MenuItemFocusListener());
        setUIProperty("borderPainted", Boolean.FALSE);
        setFocusPainted(false);
        setHorizontalTextPosition(JButton.TRAILING);
        setHorizontalAlignment(JButton.LEADING);
        updateUI();
    }

    private static class MenuItemFocusListener implements FocusListener,
        Serializable {
        public void focusGained(FocusEvent event) {}
        public void focusLost(FocusEvent event) {
            // When focus is lost, repaint if
            // the focus information is painted
            JMenuItem mi = (JMenuItem)event.getSource();
            if(mi.isFocusPainted()) {
                mi.repaint();
            }
        }
    }


    /**
     * Sets the look and feel object that renders this component.
     *
     * @param ui  the <code>JMenuItemUI</code> L&amp;F object
     * @see UIDefaults#getUI
     */
    @BeanProperty(hidden = true, visualUpdate = true, description
            = "The UI object that implements the LookAndFeel.")
    public void setUI(MenuItemUI ui) {
        super.setUI(ui);
    }

    /**
     * Resets the UI property with a value from the current look and feel.
     *
     * @see JComponent#updateUI
     */
    public void updateUI() {
        setUI((MenuItemUI)UIManager.getUI(this));
    }


    /**
     * Returns the suffix used to construct the name of the L&amp;F class used to
     * render this component.
     *
     * @return the string "MenuItemUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @BeanProperty(bound = false)
    public String getUIClassID() {
        return uiClassID;
    }


    /**
     * Identifies the menu item as "armed". If the mouse button is
     * released while it is over this item, the menu's action event
     * will fire. If the mouse button is released elsewhere, the
     * event will not fire and the menu item will be disarmed.
     *
     * @param b true to arm the menu item so it can be selected
     */
    @BeanProperty(bound = false, hidden = true, description
            = "Mouse release will fire an action event")
    public void setArmed(boolean b) {
        ButtonModel model = getModel();

        boolean oldValue = model.isArmed();
        if(model.isArmed() != b) {
            model.setArmed(b);
        }
    }

    /**
     * Returns whether the menu item is "armed".
     *
     * @return true if the menu item is armed, and it can be selected
     * @see #setArmed
     */
    public boolean isArmed() {
        ButtonModel model = getModel();
        return model.isArmed();
    }

    /**
     * Enables or disables the menu item.
     *
     * @param b  true to enable the item
     */
    @BeanProperty(preferred = true, description
            = "The enabled state of the component.")
    public void setEnabled(boolean b) {
        // Make sure we aren't armed!
        if (!b && !UIManager.getBoolean("MenuItem.disabledAreNavigable")) {
            setArmed(false);
        }
        super.setEnabled(b);
    }


    /**
     * Returns true since <code>Menu</code>s, by definition,
     * should always be on top of all other windows.  If the menu is
     * in an internal frame false is returned due to the rollover effect
     * for windows laf where the menu is not always on top.
     */
    // package private
    boolean alwaysOnTop() {
        // Fix for bug #4482165
        if (SwingUtilities.getAncestorOfClass(JInternalFrame.class, this) !=
                null) {
            return false;
        }
        return true;
    }


    /* The keystroke which acts as the menu item's accelerator
     */
    private KeyStroke accelerator;

    /**
     * Sets the key combination which invokes the menu item's
     * action listeners without navigating the menu hierarchy. It is the
     * UI's responsibility to install the correct action.  Note that
     * when the keyboard accelerator is typed, it will work whether or
     * not the menu is currently displayed.
     *
     * @param keyStroke the <code>KeyStroke</code> which will
     *          serve as an accelerator
     */
    @BeanProperty(preferred = true, description
            = "The keystroke combination which will invoke the JMenuItem's actionlisteners without navigating the menu hierarchy")
    public void setAccelerator(KeyStroke keyStroke) {
        KeyStroke oldAccelerator = accelerator;
        this.accelerator = keyStroke;
        repaint();
        revalidate();
        firePropertyChange("accelerator", oldAccelerator, accelerator);
    }

    /**
     * Returns the <code>KeyStroke</code> which serves as an accelerator
     * for the menu item.
     * @return a <code>KeyStroke</code> object identifying the
     *          accelerator key
     */
    public KeyStroke getAccelerator() {
        return this.accelerator;
    }

    /**
     * {@inheritDoc}
     *
     * @since 1.3
     */
    protected void configurePropertiesFromAction(Action a) {
        super.configurePropertiesFromAction(a);
        configureAcceleratorFromAction(a);
    }

    void setIconFromAction(Action a) {
        Icon icon = null;
        if (a != null) {
            icon = (Icon)a.getValue(Action.SMALL_ICON);
        }
        setIcon(icon);
    }

    void largeIconChanged(Action a) {
    }

    void smallIconChanged(Action a) {
        setIconFromAction(a);
    }

    void configureAcceleratorFromAction(Action a) {
        KeyStroke ks = (a==null) ? null :
            (KeyStroke)a.getValue(Action.ACCELERATOR_KEY);
        setAccelerator(ks);
    }

    /**
     * {@inheritDoc}
     * @since 1.6
     */
    protected void actionPropertyChanged(Action action, String propertyName) {
        if (propertyName == Action.ACCELERATOR_KEY) {
            configureAcceleratorFromAction(action);
        }
        else {
            super.actionPropertyChanged(action, propertyName);
        }
    }

    /**
     * Processes a mouse event forwarded from the
     * <code>MenuSelectionManager</code> and changes the menu
     * selection, if necessary, by using the
     * <code>MenuSelectionManager</code>'s API.
     * <p>
     * Note: you do not have to forward the event to sub-components.
     * This is done automatically by the <code>MenuSelectionManager</code>.
     *
     * @param e   a <code>MouseEvent</code>
     * @param path  the <code>MenuElement</code> path array
     * @param manager   the <code>MenuSelectionManager</code>
     */
    @SuppressWarnings("deprecation")
    public void processMouseEvent(MouseEvent e,MenuElement[] path,MenuSelectionManager manager) {
        processMenuDragMouseEvent(
                 new MenuDragMouseEvent(e.getComponent(), e.getID(),
                                        e.getWhen(),
                                        e.getModifiers(), e.getX(), e.getY(),
                                        e.getXOnScreen(), e.getYOnScreen(),
                                        e.getClickCount(), e.isPopupTrigger(),
                                        path, manager));
    }


    /**
     * Processes a key event forwarded from the
     * <code>MenuSelectionManager</code> and changes the menu selection,
     * if necessary, by using <code>MenuSelectionManager</code>'s API.
     * <p>
     * Note: you do not have to forward the event to sub-components.
     * This is done automatically by the <code>MenuSelectionManager</code>.
     *
     * @param e  a <code>KeyEvent</code>
     * @param path the <code>MenuElement</code> path array
     * @param manager   the <code>MenuSelectionManager</code>
     */
    @SuppressWarnings("deprecation")
    public void processKeyEvent(KeyEvent e,MenuElement[] path,MenuSelectionManager manager) {
        if (DEBUG) {
            System.out.println("in JMenuItem.processKeyEvent/3 for " + getText() +
                                   "  " + KeyStroke.getKeyStrokeForEvent(e));
        }
        MenuKeyEvent mke = new MenuKeyEvent(e.getComponent(), e.getID(),
                                             e.getWhen(), e.getModifiers(),
                                             e.getKeyCode(), e.getKeyChar(),
                                             path, manager);
        processMenuKeyEvent(mke);

        if (mke.isConsumed())  {
            e.consume();
        }
    }



    /**
     * Handles mouse drag in a menu.
     *
     * @param e  a <code>MenuDragMouseEvent</code> object
     */
    public void processMenuDragMouseEvent(MenuDragMouseEvent e) {
        switch (e.getID()) {
        case MouseEvent.MOUSE_ENTERED:
            isMouseDragged = false; fireMenuDragMouseEntered(e); break;
        case MouseEvent.MOUSE_EXITED:
            isMouseDragged = false; fireMenuDragMouseExited(e); break;
        case MouseEvent.MOUSE_DRAGGED:
            isMouseDragged = true; fireMenuDragMouseDragged(e); break;
        case MouseEvent.MOUSE_RELEASED:
            if(isMouseDragged) fireMenuDragMouseReleased(e); break;
        default:
            break;
        }
    }

    /**
     * Handles a keystroke in a menu.
     *
     * @param e  a <code>MenuKeyEvent</code> object
     */
    public void processMenuKeyEvent(MenuKeyEvent e) {
        if (DEBUG) {
            System.out.println("in JMenuItem.processMenuKeyEvent for " + getText()+
                                   "  " + KeyStroke.getKeyStrokeForEvent(e));
        }
        switch (e.getID()) {
        case KeyEvent.KEY_PRESSED:
            fireMenuKeyPressed(e); break;
        case KeyEvent.KEY_RELEASED:
            fireMenuKeyReleased(e); break;
        case KeyEvent.KEY_TYPED:
            fireMenuKeyTyped(e); break;
        default:
            break;
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.
     *
     * @param event a <code>MenuMouseDragEvent</code>
     * @see EventListenerList
     */
    protected void fireMenuDragMouseEntered(MenuDragMouseEvent event) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==MenuDragMouseListener.class) {
                // Lazily create the event:
                ((MenuDragMouseListener)listeners[i+1]).menuDragMouseEntered(event);
            }
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.
     *
     * @param event a <code>MenuDragMouseEvent</code>
     * @see EventListenerList
     */
    protected void fireMenuDragMouseExited(MenuDragMouseEvent event) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==MenuDragMouseListener.class) {
                // Lazily create the event:
                ((MenuDragMouseListener)listeners[i+1]).menuDragMouseExited(event);
            }
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.
     *
     * @param event a <code>MenuDragMouseEvent</code>
     * @see EventListenerList
     */
    protected void fireMenuDragMouseDragged(MenuDragMouseEvent event) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==MenuDragMouseListener.class) {
                // Lazily create the event:
                ((MenuDragMouseListener)listeners[i+1]).menuDragMouseDragged(event);
            }
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.
     *
     * @param event a <code>MenuDragMouseEvent</code>
     * @see EventListenerList
     */
    protected void fireMenuDragMouseReleased(MenuDragMouseEvent event) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==MenuDragMouseListener.class) {
                // Lazily create the event:
                ((MenuDragMouseListener)listeners[i+1]).menuDragMouseReleased(event);
            }
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.
     *
     * @param event a <code>MenuKeyEvent</code>
     * @see EventListenerList
     */
    protected void fireMenuKeyPressed(MenuKeyEvent event) {
        if (DEBUG) {
            System.out.println("in JMenuItem.fireMenuKeyPressed for " + getText()+
                                   "  " + KeyStroke.getKeyStrokeForEvent(event));
        }
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==MenuKeyListener.class) {
                // Lazily create the event:
                ((MenuKeyListener)listeners[i+1]).menuKeyPressed(event);
            }
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.
     *
     * @param event a <code>MenuKeyEvent</code>
     * @see EventListenerList
     */
    protected void fireMenuKeyReleased(MenuKeyEvent event) {
        if (DEBUG) {
            System.out.println("in JMenuItem.fireMenuKeyReleased for " + getText()+
                                   "  " + KeyStroke.getKeyStrokeForEvent(event));
        }
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==MenuKeyListener.class) {
                // Lazily create the event:
                ((MenuKeyListener)listeners[i+1]).menuKeyReleased(event);
            }
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.
     *
     * @param event a <code>MenuKeyEvent</code>
     * @see EventListenerList
     */
    protected void fireMenuKeyTyped(MenuKeyEvent event) {
        if (DEBUG) {
            System.out.println("in JMenuItem.fireMenuKeyTyped for " + getText()+
                                   "  " + KeyStroke.getKeyStrokeForEvent(event));
        }
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==MenuKeyListener.class) {
                // Lazily create the event:
                ((MenuKeyListener)listeners[i+1]).menuKeyTyped(event);
            }
        }
    }

    /**
     * Called by the <code>MenuSelectionManager</code> when the
     * <code>MenuElement</code> is selected or unselected.
     *
     * @param isIncluded  true if this menu item is on the part of the menu
     *                    path that changed, false if this menu is part of the
     *                    a menu path that changed, but this particular part of
     *                    that path is still the same
     * @see MenuSelectionManager#setSelectedPath(MenuElement[])
     */
    public void menuSelectionChanged(boolean isIncluded) {
        setArmed(isIncluded);
    }

    /**
     * This method returns an array containing the sub-menu
     * components for this menu component.
     *
     * @return an array of <code>MenuElement</code>s
     */
    @BeanProperty(bound = false)
    public MenuElement[] getSubElements() {
        return new MenuElement[0];
    }

    /**
     * Returns the <code>java.awt.Component</code> used to paint
     * this object. The returned component will be used to convert
     * events and detect if an event is inside a menu component.
     *
     * @return the <code>Component</code> that paints this menu item
     */
    public Component getComponent() {
        return this;
    }

    /**
     * Adds a <code>MenuDragMouseListener</code> to the menu item.
     *
     * @param l the <code>MenuDragMouseListener</code> to be added
     */
    public void addMenuDragMouseListener(MenuDragMouseListener l) {
        listenerList.add(MenuDragMouseListener.class, l);
    }

    /**
     * Removes a <code>MenuDragMouseListener</code> from the menu item.
     *
     * @param l the <code>MenuDragMouseListener</code> to be removed
     */
    public void removeMenuDragMouseListener(MenuDragMouseListener l) {
        listenerList.remove(MenuDragMouseListener.class, l);
    }

    /**
     * Returns an array of all the <code>MenuDragMouseListener</code>s added
     * to this JMenuItem with addMenuDragMouseListener().
     *
     * @return all of the <code>MenuDragMouseListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.4
     */
    @BeanProperty(bound = false)
    public MenuDragMouseListener[] getMenuDragMouseListeners() {
        return listenerList.getListeners(MenuDragMouseListener.class);
    }

    /**
     * Adds a <code>MenuKeyListener</code> to the menu item.
     *
     * @param l the <code>MenuKeyListener</code> to be added
     */
    public void addMenuKeyListener(MenuKeyListener l) {
        listenerList.add(MenuKeyListener.class, l);
    }

    /**
     * Removes a <code>MenuKeyListener</code> from the menu item.
     *
     * @param l the <code>MenuKeyListener</code> to be removed
     */
    public void removeMenuKeyListener(MenuKeyListener l) {
        listenerList.remove(MenuKeyListener.class, l);
    }

    /**
     * Returns an array of all the <code>MenuKeyListener</code>s added
     * to this JMenuItem with addMenuKeyListener().
     *
     * @return all of the <code>MenuKeyListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.4
     */
    @BeanProperty(bound = false)
    public MenuKeyListener[] getMenuKeyListeners() {
        return listenerList.getListeners(MenuKeyListener.class);
    }

    /**
     * See JComponent.readObject() for information about serialization
     * in Swing.
     */
    @Serial
    private void readObject(ObjectInputStream s)
        throws IOException, ClassNotFoundException
    {
        s.defaultReadObject();
        if (getUIClassID().equals(uiClassID)) {
            updateUI();
        }
    }

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
     * Returns a string representation of this <code>JMenuItem</code>.
     * This method is intended to be used only for debugging purposes,
     * and the content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this <code>JMenuItem</code>
     */
    protected String paramString() {
        return super.paramString();
    }

/////////////////
// Accessibility support
////////////////

    /**
     * Returns the <code>AccessibleContext</code> associated with this
     * <code>JMenuItem</code>. For <code>JMenuItem</code>s,
     * the <code>AccessibleContext</code> takes the form of an
     * <code>AccessibleJMenuItem</code>.
     * A new AccessibleJMenuItme instance is created if necessary.
     *
     * @return an <code>AccessibleJMenuItem</code> that serves as the
     *         <code>AccessibleContext</code> of this <code>JMenuItem</code>
     */
    @BeanProperty(bound = false)
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJMenuItem();
        }
        return accessibleContext;
    }


    /**
     * This class implements accessibility support for the
     * <code>JMenuItem</code> class.  It provides an implementation of the
     * Java Accessibility API appropriate to menu item user-interface
     * elements.
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
    protected class AccessibleJMenuItem extends AccessibleAbstractButton implements ChangeListener {

        private boolean isArmed = false;
        private boolean hasFocus = false;
        private boolean isPressed = false;
        private boolean isSelected = false;

        AccessibleJMenuItem() {
            super();
            JMenuItem.this.addChangeListener(this);
        }

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         * object
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.MENU_ITEM;
        }

        private void fireAccessibilityFocusedEvent(JMenuItem toCheck) {
            MenuElement [] path =
                MenuSelectionManager.defaultManager().getSelectedPath();
            if (path.length > 0) {
                Object menuItem = path[path.length - 1];
                if (toCheck == menuItem) {
                    firePropertyChange(
                        AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                        null, AccessibleState.FOCUSED);
                }
            }
        }

        /**
         * Supports the change listener interface and fires property changes.
         */
        public void stateChanged(ChangeEvent e) {
            firePropertyChange(AccessibleContext.ACCESSIBLE_VISIBLE_DATA_PROPERTY,
                               Boolean.valueOf(false), Boolean.valueOf(true));
            if (JMenuItem.this.getModel().isArmed()) {
                if (!isArmed) {
                    isArmed = true;
                    firePropertyChange(
                        AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                        null, AccessibleState.ARMED);
                    // Fix for 4848220 moved here to avoid major memory leak
                    // Here we will fire the event in case of JMenuItem
                    // See bug 4910323 for details [zav]
                    fireAccessibilityFocusedEvent(JMenuItem.this);
                }
            } else {
                if (isArmed) {
                    isArmed = false;
                    firePropertyChange(
                        AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                        AccessibleState.ARMED, null);
                }
            }
            if (JMenuItem.this.isFocusOwner()) {
                if (!hasFocus) {
                    hasFocus = true;
                    firePropertyChange(
                        AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                        null, AccessibleState.FOCUSED);
                }
            } else {
                if (hasFocus) {
                    hasFocus = false;
                    firePropertyChange(
                        AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                        AccessibleState.FOCUSED, null);
                }
            }
            if (JMenuItem.this.getModel().isPressed()) {
                if (!isPressed) {
                    isPressed = true;
                    firePropertyChange(
                        AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                        null, AccessibleState.PRESSED);
                }
            } else {
                if (isPressed) {
                    isPressed = false;
                    firePropertyChange(
                        AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                        AccessibleState.PRESSED, null);
                }
            }
            if (JMenuItem.this.getModel().isSelected()) {
                if (!isSelected) {
                    isSelected = true;
                    firePropertyChange(
                        AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                        null, AccessibleState.CHECKED);

                    // Fix for 4848220 moved here to avoid major memory leak
                    // Here we will fire the event in case of JMenu
                    // See bug 4910323 for details [zav]
                    fireAccessibilityFocusedEvent(JMenuItem.this);
                }
            } else {
                if (isSelected) {
                    isSelected = false;
                    firePropertyChange(
                        AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                        AccessibleState.CHECKED, null);
                }
            }

        }
    } // inner class AccessibleJMenuItem


}
