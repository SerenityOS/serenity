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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.plaf.ButtonUI;

/**
 * An implementation of a check box -- an item that can be selected or
 * deselected, and which displays its state to the user.
 * By convention, any number of check boxes in a group can be selected.
 * See <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/button.html">How to Use Buttons, Check Boxes, and Radio Buttons</a>
 * in <em>The Java Tutorial</em>
 * for examples and information on using check boxes.
 * <p>
 * Buttons can be configured, and to some degree controlled, by
 * <code><a href="Action.html">Action</a></code>s.  Using an
 * <code>Action</code> with a button has many benefits beyond directly
 * configuring a button.  Refer to <a href="Action.html#buttonActions">
 * Swing Components Supporting <code>Action</code></a> for more
 * details, and you can find more information in <a
 * href="https://docs.oracle.com/javase/tutorial/uiswing/misc/action.html">How
 * to Use Actions</a>, a section in <em>The Java Tutorial</em>.
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
 * @see JRadioButton
 *
 * @author Jeff Dinkins
 * @since 1.2
 */
@JavaBean(description = "A component which can be selected or deselected.")
@SwingContainer(false)
@SuppressWarnings("serial") // Same-version serialization only
public class JCheckBox extends JToggleButton implements Accessible {

    /** Identifies a change to the flat property. */
    public static final String BORDER_PAINTED_FLAT_CHANGED_PROPERTY = "borderPaintedFlat";

    private boolean flat = false;

    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "CheckBoxUI";


    /**
     * Creates an initially unselected check box button with no text, no icon.
     */
    public JCheckBox () {
        this(null, null, false);
    }

    /**
     * Creates an initially unselected check box with an icon.
     *
     * @param icon  the Icon image to display
     */
    public JCheckBox(Icon icon) {
        this(null, icon, false);
    }

    /**
     * Creates a check box with an icon and specifies whether
     * or not it is initially selected.
     *
     * @param icon  the Icon image to display
     * @param selected a boolean value indicating the initial selection
     *        state. If <code>true</code> the check box is selected
     */
    public JCheckBox(Icon icon, boolean selected) {
        this(null, icon, selected);
    }

    /**
     * Creates an initially unselected check box with text.
     *
     * @param text the text of the check box.
     */
    public JCheckBox (String text) {
        this(text, null, false);
    }

    /**
     * Creates a check box where properties are taken from the
     * Action supplied.
     *
     * @param a the {@code Action} used to specify the new check box
     * @since 1.3
     */
    public JCheckBox(Action a) {
        this();
        setAction(a);
    }


    /**
     * Creates a check box with text and specifies whether
     * or not it is initially selected.
     *
     * @param text the text of the check box.
     * @param selected a boolean value indicating the initial selection
     *        state. If <code>true</code> the check box is selected
     */
    public JCheckBox (String text, boolean selected) {
        this(text, null, selected);
    }

    /**
     * Creates an initially unselected check box with
     * the specified text and icon.
     *
     * @param text the text of the check box.
     * @param icon  the Icon image to display
     */
    public JCheckBox(String text, Icon icon) {
        this(text, icon, false);
    }

    /**
     * Creates a check box with text and icon,
     * and specifies whether or not it is initially selected.
     *
     * @param text the text of the check box.
     * @param icon  the Icon image to display
     * @param selected a boolean value indicating the initial selection
     *        state. If <code>true</code> the check box is selected
     */
    public JCheckBox (String text, Icon icon, boolean selected) {
        super(text, icon, selected);
        setUIProperty("borderPainted", Boolean.FALSE);
        setHorizontalAlignment(LEADING);
    }

    /**
     * Sets the <code>borderPaintedFlat</code> property,
     * which gives a hint to the look and feel as to the
     * appearance of the check box border.
     * This is usually set to <code>true</code> when a
     * <code>JCheckBox</code> instance is used as a
     * renderer in a component such as a <code>JTable</code> or
     * <code>JTree</code>.  The default value for the
     * <code>borderPaintedFlat</code> property is <code>false</code>.
     * This method fires a property changed event.
     * Some look and feels might not implement flat borders;
     * they will ignore this property.
     *
     * @param b <code>true</code> requests that the border be painted flat;
     *          <code>false</code> requests normal borders
     * @see #isBorderPaintedFlat
     * @since 1.3
     */
    @BeanProperty(visualUpdate = true, description
            = "Whether the border is painted flat.")
    public void setBorderPaintedFlat(boolean b) {
        boolean oldValue = flat;
        flat = b;
        firePropertyChange(BORDER_PAINTED_FLAT_CHANGED_PROPERTY, oldValue, flat);
        if (b != oldValue) {
            revalidate();
            repaint();
        }
    }

    /**
     * Gets the value of the <code>borderPaintedFlat</code> property.
     *
     * @return the value of the <code>borderPaintedFlat</code> property
     * @see #setBorderPaintedFlat
     * @since 1.3
     */
    public boolean isBorderPaintedFlat() {
        return flat;
    }

    /**
     * Resets the UI property to a value from the current look and feel.
     *
     * @see JComponent#updateUI
     */
    public void updateUI() {
        setUI((ButtonUI)UIManager.getUI(this));
    }


    /**
     * Returns a string that specifies the name of the L&amp;F class
     * that renders this component.
     *
     * @return the string "CheckBoxUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @BeanProperty(bound = false, expert = true, description
            = "A string that specifies the name of the L&F class")
    public String getUIClassID() {
        return uiClassID;
    }


    /**
     * The icon for checkboxs comes from the look and feel,
     * not the Action; this is overriden to do nothing.
     */
    void setIconFromAction(Action a) {
    }

     /*
      * See readObject and writeObject in JComponent for more
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


    /**
     * Returns a string representation of this JCheckBox. This method
     * is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     * specific new aspects of the JFC components.
     *
     * @return  a string representation of this JCheckBox.
     */
    protected String paramString() {
        return super.paramString();
    }

/////////////////
// Accessibility support
////////////////

    /**
     * Gets the AccessibleContext associated with this JCheckBox.
     * For JCheckBoxes, the AccessibleContext takes the form of an
     * AccessibleJCheckBox.
     * A new AccessibleJCheckBox instance is created if necessary.
     *
     * @return an AccessibleJCheckBox that serves as the
     *         AccessibleContext of this JCheckBox
     */
    @BeanProperty(bound = false, expert = true, description
            = "The AccessibleContext associated with this CheckBox.")
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJCheckBox();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * <code>JCheckBox</code> class.  It provides an implementation of the
     * Java Accessibility API appropriate to check box user-interface
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
    @SuppressWarnings("serial") // Same-version serialization only
    protected class AccessibleJCheckBox extends AccessibleJToggleButton {

        /**
         * Constructs an {@code AccessibleJCheckBox}.
         */
        protected AccessibleJCheckBox() {}

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the object
         * @see AccessibleRole
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.CHECK_BOX;
        }

    } // inner class AccessibleJCheckBox
}
