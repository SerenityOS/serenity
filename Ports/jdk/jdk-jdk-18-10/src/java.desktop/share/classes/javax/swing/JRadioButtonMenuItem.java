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
 * An implementation of a radio button menu item.
 * A <code>JRadioButtonMenuItem</code> is
 * a menu item that is part of a group of menu items in which only one
 * item in the group can be selected. The selected item displays its
 * selected state. Selecting it causes any other selected item to
 * switch to the unselected state.
 * To control the selected state of a group of radio button menu items,
 * use a <code>ButtonGroup</code> object.
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
 * Some menus can have several button groups with radio button menu items. In
 * this case it is useful that clicking on one radio button menu item does not
 * close the menu. Such behavior can be controlled either by client
 * {@link JComponent#putClientProperty} or the Look and Feel
 * {@link UIManager#put} property named
 * {@code "RadioButtonMenuItem.doNotCloseOnMouseClick"}. The default value is
 * {@code false}. Setting the property to {@code true} prevents the menu from
 * closing when it is clicked by the mouse. If the client property is set its
 * value is always used; otherwise the {@literal L&F} property is queried.
 * Note: some {@code L&F}s may ignore this property. All built-in {@code L&F}s
 * inherit this behaviour.
 * <p>
 * For further documentation and examples see
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
 *
 * @author Georges Saab
 * @author David Karlton
 * @see ButtonGroup
 * @since 1.2
 */
@JavaBean(description = "A component within a group of menu items which can be selected.")
@SwingContainer(false)
@SuppressWarnings("serial") // Same-version serialization only
public class JRadioButtonMenuItem extends JMenuItem implements Accessible {
    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "RadioButtonMenuItemUI";

    /**
     * Creates a <code>JRadioButtonMenuItem</code> with no set text or icon.
     */
    public JRadioButtonMenuItem() {
        this(null, null, false);
    }

    /**
     * Creates a <code>JRadioButtonMenuItem</code> with an icon.
     *
     * @param icon the <code>Icon</code> to display on the
     *          <code>JRadioButtonMenuItem</code>
     */
    public JRadioButtonMenuItem(Icon icon) {
        this(null, icon, false);
    }

    /**
     * Creates a <code>JRadioButtonMenuItem</code> with text.
     *
     * @param text the text of the <code>JRadioButtonMenuItem</code>
     */
    public JRadioButtonMenuItem(String text) {
        this(text, null, false);
    }

    /**
     * Creates a radio button menu item whose properties are taken from the
     * <code>Action</code> supplied.
     *
     * @param  a the <code>Action</code> on which to base the radio
     *          button menu item
     *
     * @since 1.3
     */
    public JRadioButtonMenuItem(Action a) {
        this();
        setAction(a);
    }

    /**
     * Creates a radio button menu item with the specified text
     * and <code>Icon</code>.
     *
     * @param text the text of the <code>JRadioButtonMenuItem</code>
     * @param icon the icon to display on the <code>JRadioButtonMenuItem</code>
     */
    public JRadioButtonMenuItem(String text, Icon icon) {
        this(text, icon, false);
    }

    /**
     * Creates a radio button menu item with the specified text
     * and selection state.
     *
     * @param text the text of the <code>CheckBoxMenuItem</code>
     * @param selected the selected state of the <code>CheckBoxMenuItem</code>
     */
    public JRadioButtonMenuItem(String text, boolean selected) {
        this(text);
        setSelected(selected);
    }

    /**
     * Creates a radio button menu item with the specified image
     * and selection state, but no text.
     *
     * @param icon  the image that the button should display
     * @param selected  if true, the button is initially selected;
     *                  otherwise, the button is initially unselected
     */
    public JRadioButtonMenuItem(Icon icon, boolean selected) {
        this(null, icon, selected);
    }

    /**
     * Creates a radio button menu item that has the specified
     * text, image, and selection state.  All other constructors
     * defer to this one.
     *
     * @param text  the string displayed on the radio button
     * @param icon  the image that the button should display
     * @param selected if {@code true}, the button is initially selected,
     *                 otherwise, the button is initially unselected
     */
    public JRadioButtonMenuItem(String text, Icon icon, boolean selected) {
        super(text, icon);
        setModel(new JToggleButton.ToggleButtonModel());
        setSelected(selected);
        setFocusable(false);
    }

    /**
     * Returns the name of the L&amp;F class that renders this component.
     *
     * @return the string "RadioButtonMenuItemUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @BeanProperty(bound = false)
    public String getUIClassID() {
        return uiClassID;
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
     * Returns a string representation of this
     * <code>JRadioButtonMenuItem</code>.  This method
     * is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this
     *          <code>JRadioButtonMenuItem</code>
     */
    protected String paramString() {
        return super.paramString();
    }

    /**
     * Overriden to return true, JRadioButtonMenuItem supports
     * the selected state.
     */
    boolean shouldUpdateSelectedStateFromAction() {
        return true;
    }

/////////////////
// Accessibility support
////////////////

    /**
     * Gets the AccessibleContext associated with this JRadioButtonMenuItem.
     * For JRadioButtonMenuItems, the AccessibleContext takes the form of an
     * AccessibleJRadioButtonMenuItem.
     * A new AccessibleJRadioButtonMenuItem instance is created if necessary.
     *
     * @return an AccessibleJRadioButtonMenuItem that serves as the
     *         AccessibleContext of this JRadioButtonMenuItem
     */
    @BeanProperty(bound = false)
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJRadioButtonMenuItem();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * <code>JRadioButtonMenuItem</code> class.  It provides an
     * implementation of the Java Accessibility API appropriate to
     * <code>JRadioButtonMenuItem</code> user-interface elements.
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
    protected class AccessibleJRadioButtonMenuItem extends AccessibleJMenuItem {

        /**
         * Constructs an {@code AccessibleJRadioButtonMenuItem}.
         */
        protected AccessibleJRadioButtonMenuItem() {}

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         * object
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.RADIO_BUTTON;
        }
    } // inner class AccessibleJRadioButtonMenuItem
}
