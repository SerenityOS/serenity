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

import java.beans.BeanProperty;
import java.beans.JavaBean;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

/**
 * A menu item that can be selected or deselected. If selected, the menu
 * item typically appears with a checkmark next to it. If unselected or
 * deselected, the menu item appears without a checkmark. Like a regular
 * menu item, a check box menu item can have either text or a graphic
 * icon associated with it, or both.
 * <p>
 * Either <code>isSelected</code>/<code>setSelected</code> or
 * <code>getState</code>/<code>setState</code> can be used
 * to determine/specify the menu item's selection state. The
 * preferred methods are <code>isSelected</code> and
 * <code>setSelected</code>, which work for all menus and buttons.
 * The <code>getState</code> and <code>setState</code> methods exist for
 * compatibility with other component sets.
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
 * Some times it is required to select several check box menu items from a menu.
 * In this case it is useful that clicking on one check box menu item does not
 * close the menu. Such behavior can be controlled either by client
 * {@link JComponent#putClientProperty} or the Look and Feel
 * {@link UIManager#put} property named
 * {@code "CheckBoxMenuItem.doNotCloseOnMouseClick"}. The default value is
 * {@code false}. Setting the property to {@code true} prevents the menu from
 * closing when it is clicked by the mouse. If the client property is set its
 * value is always used; otherwise the {@literal L&F} property is queried.
 * Note: some {@code L&F}s may ignore this property. All built-in {@code L&F}s
 * inherit this behaviour.
 * <p>
 * For further information and examples of using check box menu items,
 * see <a
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
 *
 * @author Georges Saab
 * @author David Karlton
 * @since 1.2
 */
@JavaBean(description = "A menu item which can be selected or deselected.")
@SwingContainer(false)
@SuppressWarnings("serial") // Same-version serialization only
public class JCheckBoxMenuItem extends JMenuItem implements SwingConstants,
        Accessible
{
    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "CheckBoxMenuItemUI";

    /**
     * Creates an initially unselected check box menu item with no set text or icon.
     */
    public JCheckBoxMenuItem() {
        this(null, null, false);
    }

    /**
     * Creates an initially unselected check box menu item with an icon.
     *
     * @param icon the icon of the {@code JCheckBoxMenuItem}.
     */
    public JCheckBoxMenuItem(Icon icon) {
        this(null, icon, false);
    }

    /**
     * Creates an initially unselected check box menu item with text.
     *
     * @param text the text of the {@code JCheckBoxMenuItem}
     */
    public JCheckBoxMenuItem(String text) {
        this(text, null, false);
    }

    /**
     * Creates a menu item whose properties are taken from the
     * Action supplied.
     *
     * @param a the action of the {@code JCheckBoxMenuItem}
     * @since 1.3
     */
    public JCheckBoxMenuItem(Action a) {
        this();
        setAction(a);
    }

    /**
     * Creates an initially unselected check box menu item with the specified text and icon.
     *
     * @param text the text of the {@code JCheckBoxMenuItem}
     * @param icon the icon of the {@code JCheckBoxMenuItem}
     */
    public JCheckBoxMenuItem(String text, Icon icon) {
        this(text, icon, false);
    }

    /**
     * Creates a check box menu item with the specified text and selection state.
     *
     * @param text the text of the check box menu item.
     * @param b the selected state of the check box menu item
     */
    public JCheckBoxMenuItem(String text, boolean b) {
        this(text, null, b);
    }

    /**
     * Creates a check box menu item with the specified text, icon, and selection state.
     *
     * @param text the text of the check box menu item
     * @param icon the icon of the check box menu item
     * @param b the selected state of the check box menu item
     */
    public JCheckBoxMenuItem(String text, Icon icon, boolean b) {
        super(text, icon);
        setModel(new JToggleButton.ToggleButtonModel());
        setSelected(b);
        setFocusable(false);
    }

    /**
     * Returns the name of the L&amp;F class
     * that renders this component.
     *
     * @return the string "CheckBoxMenuItemUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @BeanProperty(bound = false)
    public String getUIClassID() {
        return uiClassID;
    }

     /**
      * Returns the selected-state of the item. This method
      * exists for AWT compatibility only.  New code should
      * use isSelected() instead.
      *
      * @return true  if the item is selected
      */
    public boolean getState() {
        return isSelected();
    }

    /**
     * Sets the selected-state of the item. This method
     * exists for AWT compatibility only.  New code should
     * use setSelected() instead.
     *
     * @param b  a boolean value indicating the item's
     *           selected-state, where true=selected
     */
    @BeanProperty(bound = false, hidden = true, description
            = "The selection state of the check box menu item")
    public synchronized void setState(boolean b) {
        setSelected(b);
    }


    /**
     * Returns an array (length 1) containing the check box menu item
     * label or null if the check box is not selected.
     *
     * @return an array containing one Object -- the text of the menu item
     *         -- if the item is selected; otherwise null
     */
    @BeanProperty(bound = false)
    public Object[] getSelectedObjects() {
        if (isSelected() == false)
            return null;
        Object[] selectedObjects = new Object[1];
        selectedObjects[0] = getText();
        return selectedObjects;
    }

    /**
     * See readObject() and writeObject() in JComponent for more
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
     * Returns a string representation of this JCheckBoxMenuItem. This method
     * is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this JCheckBoxMenuItem.
     */
    protected String paramString() {
        return super.paramString();
    }

    /**
     * Overriden to return true, JCheckBoxMenuItem supports
     * the selected state.
     */
    boolean shouldUpdateSelectedStateFromAction() {
        return true;
    }

/////////////////
// Accessibility support
////////////////

    /**
     * Gets the AccessibleContext associated with this JCheckBoxMenuItem.
     * For JCheckBoxMenuItems, the AccessibleContext takes the form of an
     * AccessibleJCheckBoxMenuItem.
     * A new AccessibleJCheckBoxMenuItem instance is created if necessary.
     *
     * @return an AccessibleJCheckBoxMenuItem that serves as the
     *         AccessibleContext of this AccessibleJCheckBoxMenuItem
     */
    @BeanProperty(bound = false)
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJCheckBoxMenuItem();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * <code>JCheckBoxMenuItem</code> class.  It provides an implementation
     * of the Java Accessibility API appropriate to checkbox menu item
     * user-interface elements.
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
    @SuppressWarnings("serial") // Same-version serialization only
    protected class AccessibleJCheckBoxMenuItem extends AccessibleJMenuItem {

        /**
         * Constructs an {@code AccessibleJCheckBoxMenuItem}.
         */
        protected AccessibleJCheckBoxMenuItem() {}

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         * object
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.CHECK_BOX;
        }
    } // inner class AccessibleJCheckBoxMenuItem
}
