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
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.BeanProperty;
import java.beans.JavaBean;
import java.beans.Transient;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Vector;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.accessibility.AccessibleStateSet;
import javax.swing.plaf.MenuBarUI;

import sun.awt.SunToolkit;

/**
 * An implementation of a menu bar. You add <code>JMenu</code> objects to the
 * menu bar to construct a menu. When the user selects a <code>JMenu</code>
 * object, its associated <code>JPopupMenu</code> is displayed, allowing the
 * user to select one of the <code>JMenuItems</code> on it.
 * <p>
 * For information and examples of using menu bars see
 * <a
 href="https://docs.oracle.com/javase/tutorial/uiswing/components/menu.html">How to Use Menus</a>,
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
 * <p>
 * <strong>Warning:</strong>
 * By default, pressing the Tab key does not transfer focus from a <code>
 * JMenuBar</code> which is added to a container together with other Swing
 * components, because the <code>focusTraversalKeysEnabled</code> property
 * of <code>JMenuBar</code> is set to <code>false</code>. To resolve this,
 * you should call the <code>JMenuBar.setFocusTraversalKeysEnabled(true)</code>
 * method.
 *
 * @author Georges Saab
 * @author David Karlton
 * @author Arnaud Weber
 * @see JMenu
 * @see JPopupMenu
 * @see JMenuItem
 * @since 1.2
 */
@JavaBean(defaultProperty = "UI", description = "A container for holding and displaying menus.")
@SwingContainer
@SuppressWarnings("serial")
public class JMenuBar extends JComponent implements Accessible,MenuElement
{
    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "MenuBarUI";

    /*
     * Model for the selected subcontrol.
     */
    private transient SingleSelectionModel selectionModel;

    private boolean paintBorder           = true;
    private Insets     margin             = null;

    /* diagnostic aids -- should be false for production builds. */
    private static final boolean TRACE =   false; // trace creates and disposes
    private static final boolean VERBOSE = false; // show reuse hits/misses
    private static final boolean DEBUG =   false;  // show bad params, misc.

    /**
     * Creates a new menu bar.
     */
    public JMenuBar() {
        super();
        setFocusTraversalKeysEnabled(false);
        setSelectionModel(new DefaultSingleSelectionModel());
        updateUI();
    }

    /**
     * Returns the menubar's current UI.
     *
     * @return a {@code MenuBarUI} which is the menubar's current L&amp;F object
     * @see #setUI
     */
    public MenuBarUI getUI() {
        return (MenuBarUI)ui;
    }

    /**
     * Sets the L&amp;F object that renders this component.
     *
     * @param ui the new MenuBarUI L&amp;F object
     * @see UIDefaults#getUI
     */
    @BeanProperty(hidden = true, visualUpdate = true, description
            = "The UI object that implements the Component's LookAndFeel.")
    public void setUI(MenuBarUI ui) {
        super.setUI(ui);
    }

    /**
     * Resets the UI property with a value from the current look and feel.
     *
     * @see JComponent#updateUI
     */
    public void updateUI() {
        Toolkit tk = Toolkit.getDefaultToolkit();
        if (tk instanceof SunToolkit) {
            ((SunToolkit)tk).updateScreenMenuBarUI();
        }
        setUI((MenuBarUI)UIManager.getUI(this));
    }


    /**
     * Returns the name of the L&amp;F class that renders this component.
     *
     * @return the string "MenuBarUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @BeanProperty(bound = false)
    public String getUIClassID() {
        return uiClassID;
    }


    /**
     * Returns the model object that handles single selections.
     *
     * @return the <code>SingleSelectionModel</code> property
     * @see SingleSelectionModel
     */
    public SingleSelectionModel getSelectionModel() {
        return selectionModel;
    }

    /**
     * Sets the model object to handle single selections.
     *
     * @param model the <code>SingleSelectionModel</code> to use
     * @see SingleSelectionModel
     */
    @BeanProperty(description = "The selection model, recording which child is selected.")
    public void setSelectionModel(SingleSelectionModel model) {
        SingleSelectionModel oldValue = selectionModel;
        this.selectionModel = model;
        firePropertyChange("selectionModel", oldValue, selectionModel);
    }


    /**
     * Appends the specified menu to the end of the menu bar.
     *
     * @param c the <code>JMenu</code> component to add
     * @return the menu component
     */
    public JMenu add(JMenu c) {
        super.add(c);
        return c;
    }

    /**
     * Returns the menu at the specified position in the menu bar.
     *
     * @param index  an integer giving the position in the menu bar, where
     *               0 is the first position
     * @return the <code>JMenu</code> at that position, or <code>null</code> if
     *          if there is no <code>JMenu</code> at that position (ie. if
     *          it is a <code>JMenuItem</code>)
     */
    public JMenu getMenu(int index) {
        Component c = getComponentAtIndex(index);
        if (c instanceof JMenu)
            return (JMenu) c;
        return null;
    }

    /**
     * Returns the number of items in the menu bar.
     *
     * @return the number of items in the menu bar
     */
    @BeanProperty(bound = false)
    public int getMenuCount() {
        return getComponentCount();
    }

    /**
     * Sets the help menu that appears when the user selects the
     * "help" option in the menu bar. This method is not yet implemented
     * and will throw an exception.
     *
     * @param menu the JMenu that delivers help to the user
     */
    public void setHelpMenu(JMenu menu) {
        throw new Error("setHelpMenu() not yet implemented.");
    }

    /**
     * Gets the help menu for the menu bar.  This method is not yet
     * implemented and will throw an exception.
     *
     * @return the <code>JMenu</code> that delivers help to the user
     */
    @Transient
    public JMenu getHelpMenu() {
        throw new Error("getHelpMenu() not yet implemented.");
    }

    /**
     * Returns the component at the specified index.
     *
     * @param i an integer specifying the position, where 0 is first
     * @return the <code>Component</code> at the position,
     *          or <code>null</code> for an invalid index
     * @deprecated replaced by <code>getComponent(int i)</code>
     */
    @Deprecated
    public Component getComponentAtIndex(int i) {
        if(i < 0 || i >= getComponentCount()) {
            return null;
        }
        return getComponent(i);
    }

    /**
     * Returns the index of the specified component.
     *
     * @param c  the <code>Component</code> to find
     * @return an integer giving the component's position, where 0 is first;
     *          or -1 if it can't be found
     */
    public int getComponentIndex(Component c) {
        int ncomponents = this.getComponentCount();
        Component[] component = this.getComponents();
        for (int i = 0 ; i < ncomponents ; i++) {
            Component comp = component[i];
            if (comp == c)
                return i;
        }
        return -1;
    }

    /**
     * Sets the currently selected component, producing a
     * a change to the selection model.
     *
     * @param sel the <code>Component</code> to select
     */
    public void setSelected(Component sel) {
        SingleSelectionModel model = getSelectionModel();
        int index = getComponentIndex(sel);
        model.setSelectedIndex(index);
    }

    /**
     * Returns true if the menu bar currently has a component selected.
     *
     * @return true if a selection has been made, else false
     */
    @BeanProperty(bound = false)
    public boolean isSelected() {
        return selectionModel.isSelected();
    }

    /**
     * Returns true if the menu bars border should be painted.
     *
     * @return  true if the border should be painted, else false
     */
    public boolean isBorderPainted() {
        return paintBorder;
    }

    /**
     * Sets whether the border should be painted.
     *
     * @param b if true and border property is not <code>null</code>,
     *          the border is painted.
     * @see #isBorderPainted
     */
    @BeanProperty(visualUpdate = true, description
            = "Whether the border should be painted.")
    public void setBorderPainted(boolean b) {
        boolean oldValue = paintBorder;
        paintBorder = b;
        firePropertyChange("borderPainted", oldValue, paintBorder);
        if (b != oldValue) {
            revalidate();
            repaint();
        }
    }

    /**
     * Paints the menubar's border if <code>BorderPainted</code>
     * property is true.
     *
     * @param g the <code>Graphics</code> context to use for painting
     * @see JComponent#paint
     * @see JComponent#setBorder
     */
    protected void paintBorder(Graphics g) {
        if (isBorderPainted()) {
            super.paintBorder(g);
        }
    }

    /**
     * Sets the margin between the menubar's border and
     * its menus. Setting to <code>null</code> will cause the menubar to
     * use the default margins.
     *
     * @param m an Insets object containing the margin values
     * @see Insets
     */
    @BeanProperty(visualUpdate = true, description
            = "The space between the menubar's border and its contents")
    public void setMargin(Insets m) {
        Insets old = margin;
        this.margin = m;
        firePropertyChange("margin", old, m);
        if (old == null || !old.equals(m)) {
            revalidate();
            repaint();
        }
    }

    /**
     * Returns the margin between the menubar's border and
     * its menus.  If there is no previous margin, it will create
     * a default margin with zero size.
     *
     * @return an <code>Insets</code> object containing the margin values
     * @see Insets
     */
    public Insets getMargin() {
        if(margin == null) {
            return new Insets(0,0,0,0);
        } else {
            return margin;
        }
    }


    /**
     * Implemented to be a <code>MenuElement</code> -- does nothing.
     *
     * @see #getSubElements
     */
    public void processMouseEvent(MouseEvent event,MenuElement[] path,MenuSelectionManager manager) {
    }

    /**
     * Implemented to be a <code>MenuElement</code> -- does nothing.
     *
     * @see #getSubElements
     */
    public void processKeyEvent(KeyEvent e,MenuElement[] path,MenuSelectionManager manager) {
    }

    /**
     * Implemented to be a <code>MenuElement</code> -- does nothing.
     *
     * @see #getSubElements
     */
    public void menuSelectionChanged(boolean isIncluded) {
    }

    /**
     * Implemented to be a <code>MenuElement</code> -- returns the
     * menus in this menu bar.
     * This is the reason for implementing the <code>MenuElement</code>
     * interface -- so that the menu bar can be treated the same as
     * other menu elements.
     * @return an array of menu items in the menu bar.
     */
    @BeanProperty(bound = false)
    public MenuElement[] getSubElements() {
        MenuElement[] result;
        Vector<MenuElement> tmp = new Vector<MenuElement>();
        int c = getComponentCount();
        int i;
        Component m;

        for(i=0 ; i < c ; i++) {
            m = getComponent(i);
            if(m instanceof MenuElement)
                tmp.addElement((MenuElement) m);
        }

        result = new MenuElement[tmp.size()];
        for(i=0,c=tmp.size() ; i < c ; i++)
            result[i] = tmp.elementAt(i);
        return result;
    }

    /**
     * Implemented to be a <code>MenuElement</code>. Returns this object.
     *
     * @return the current <code>Component</code> (this)
     * @see #getSubElements
     */
    public Component getComponent() {
        return this;
    }


    /**
     * Returns a string representation of this <code>JMenuBar</code>.
     * This method
     * is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this <code>JMenuBar</code>
     */
    protected String paramString() {
        String paintBorderString = (paintBorder ?
                                    "true" : "false");
        String marginString = (margin != null ?
                               margin.toString() : "");

        return super.paramString() +
        ",margin=" + marginString +
        ",paintBorder=" + paintBorderString;
    }

/////////////////
// Accessibility support
////////////////

    /**
     * Gets the AccessibleContext associated with this JMenuBar.
     * For JMenuBars, the AccessibleContext takes the form of an
     * AccessibleJMenuBar.
     * A new AccessibleJMenuBar instance is created if necessary.
     *
     * @return an AccessibleJMenuBar that serves as the
     *         AccessibleContext of this JMenuBar
     */
    @BeanProperty(bound = false)
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJMenuBar();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * <code>JMenuBar</code> class.  It provides an implementation of the
     * Java Accessibility API appropriate to menu bar user-interface
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
    protected class AccessibleJMenuBar extends AccessibleJComponent
        implements AccessibleSelection {

        /**
         * Constructs an {@code AccessibleJMenuBar}.
         */
        protected AccessibleJMenuBar() {}

        /**
         * Get the accessible state set of this object.
         *
         * @return an instance of AccessibleState containing the current state
         *         of the object
         */
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet states = super.getAccessibleStateSet();
            return states;
        }

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         * object
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.MENU_BAR;
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
         * Returns 1 if a menu is currently selected in this menu bar.
         *
         * @return 1 if a menu is currently selected, else 0
         */
         public int getAccessibleSelectionCount() {
            if (isSelected()) {
                return 1;
            } else {
                return 0;
            }
         }

        /**
         * Returns the currently selected menu if one is selected,
         * otherwise null.
         */
         public Accessible getAccessibleSelection(int i) {
            if (isSelected()) {
                if (i != 0) {   // single selection model for JMenuBar
                    return null;
                }
                int j = getSelectionModel().getSelectedIndex();
                if (getComponentAtIndex(j) instanceof Accessible) {
                    return (Accessible) getComponentAtIndex(j);
                }
            }
            return null;
         }

        /**
         * Returns true if the current child of this object is selected.
         *
         * @param i the zero-based index of the child in this Accessible
         * object.
         * @see AccessibleContext#getAccessibleChild
         */
        public boolean isAccessibleChildSelected(int i) {
            return (i == getSelectionModel().getSelectedIndex());
        }

        /**
         * Selects the nth menu in the menu bar, forcing it to
         * pop up.  If another menu is popped up, this will force
         * it to close.  If the nth menu is already selected, this
         * method has no effect.
         *
         * @param i the zero-based index of selectable items
         * @see #getAccessibleStateSet
         */
        public void addAccessibleSelection(int i) {
            // first close up any open menu
            int j = getSelectionModel().getSelectedIndex();
            if (i == j) {
                return;
            }
            if (j >= 0 && j < getMenuCount()) {
                JMenu menu = getMenu(j);
                if (menu != null) {
                    MenuSelectionManager.defaultManager().setSelectedPath(null);
//                  menu.setPopupMenuVisible(false);
                }
            }
            // now popup the new menu
            getSelectionModel().setSelectedIndex(i);
            JMenu menu = getMenu(i);
            if (menu != null) {
                MenuElement[] me = new MenuElement[3];
                me[0] = JMenuBar.this;
                me[1] = menu;
                me[2] = menu.getPopupMenu();
                MenuSelectionManager.defaultManager().setSelectedPath(me);
//              menu.setPopupMenuVisible(true);
            }
        }

        /**
         * Removes the nth selected item in the object from the object's
         * selection.  If the nth item isn't currently selected, this
         * method has no effect.  Otherwise, it closes the popup menu.
         *
         * @param i the zero-based index of selectable items
         */
        public void removeAccessibleSelection(int i) {
            if (i >= 0 && i < getMenuCount()) {
                JMenu menu = getMenu(i);
                if (menu != null) {
                    MenuSelectionManager.defaultManager().setSelectedPath(null);
//                  menu.setPopupMenuVisible(false);
                }
                getSelectionModel().setSelectedIndex(-1);
            }
        }

        /**
         * Clears the selection in the object, so that nothing in the
         * object is selected.  This will close any open menu.
         */
        public void clearAccessibleSelection() {
            int i = getSelectionModel().getSelectedIndex();
            if (i >= 0 && i < getMenuCount()) {
                JMenu menu = getMenu(i);
                if (menu != null) {
                    MenuSelectionManager.defaultManager().setSelectedPath(null);
//                  menu.setPopupMenuVisible(false);
                }
            }
            getSelectionModel().setSelectedIndex(-1);
        }

        /**
         * Normally causes every selected item in the object to be selected
         * if the object supports multiple selections.  This method
         * makes no sense in a menu bar, and so does nothing.
         */
        public void selectAllAccessibleSelection() {
        }
    } // internal class AccessibleJMenuBar


    /**
     * Subclassed to check all the child menus.
     * @since 1.3
     */
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e,
                                        int condition, boolean pressed) {
        // See if we have a local binding.
        boolean retValue = super.processKeyBinding(ks, e, condition, pressed);
        if (!retValue) {
            MenuElement[] subElements = getSubElements();
            for (MenuElement subElement : subElements) {
                if (processBindingForKeyStrokeRecursive(
                        subElement, ks, e, condition, pressed)) {
                    return true;
                }
            }
        }
        return retValue;
    }

    static boolean processBindingForKeyStrokeRecursive(MenuElement elem,
                                                       KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        if (elem == null) {
            return false;
        }

        Component c = elem.getComponent();

        if ( !(c.isVisible() || (c instanceof JPopupMenu)) || !c.isEnabled() ) {
            return false;
        }

        if (c != null && c instanceof JComponent &&
            ((JComponent)c).processKeyBinding(ks, e, condition, pressed)) {

            return true;
        }

        MenuElement[] subElements = elem.getSubElements();
        for (MenuElement subElement : subElements) {
            if (processBindingForKeyStrokeRecursive(subElement, ks, e, condition, pressed)) {
                return true;
                // We don't, pass along to children JMenu's
            }
        }
        return false;
    }

    /**
     * Overrides <code>JComponent.addNotify</code> to register this
     * menu bar with the current keyboard manager.
     */
    public void addNotify() {
        super.addNotify();
        KeyboardManager.getCurrentManager().registerMenuBar(this);
    }

    /**
     * Overrides <code>JComponent.removeNotify</code> to unregister this
     * menu bar with the current keyboard manager.
     */
    public void removeNotify() {
        super.removeNotify();
        KeyboardManager.getCurrentManager().unregisterMenuBar(this);
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

        Object[] kvData = new Object[4];
        int n = 0;

        if (selectionModel instanceof Serializable) {
            kvData[n++] = "selectionModel";
            kvData[n++] = selectionModel;
        }

        s.writeObject(kvData);
    }


    /**
     * See JComponent.readObject() for information about serialization
     * in Swing.
     */
    @Serial
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException
    {
        s.defaultReadObject();
        Object[] kvData = (Object[])(s.readObject());

        for(int i = 0; i < kvData.length; i += 2) {
            if (kvData[i] == null) {
                break;
            }
            else if (kvData[i].equals("selectionModel")) {
                selectionModel = (SingleSelectionModel)kvData[i + 1];
            }
        }

    }
}
