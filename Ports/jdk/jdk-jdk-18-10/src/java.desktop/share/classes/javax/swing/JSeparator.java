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
import javax.swing.plaf.SeparatorUI;

/**
 * <code>JSeparator</code> provides a general purpose component for
 * implementing divider lines - most commonly used as a divider
 * between menu items that breaks them up into logical groupings.
 * Instead of using <code>JSeparator</code> directly,
 * you can use the <code>JMenu</code> or <code>JPopupMenu</code>
 * <code>addSeparator</code> method to create and add a separator.
 * <code>JSeparator</code>s may also be used elsewhere in a GUI
 * wherever a visual divider is useful.
 *
 * <p>
 *
 * For more information and examples see
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
 * @author Jeff Shapiro
 * @since 1.2
 */
@JavaBean(defaultProperty = "UI", description = "A divider between menu items.")
@SwingContainer(false)
@SuppressWarnings("serial")
public class JSeparator extends JComponent implements SwingConstants, Accessible
{
    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "SeparatorUI";

    private int orientation = HORIZONTAL;

    /** Creates a new horizontal separator. */
    public JSeparator()
    {
        this( HORIZONTAL );
    }

    /**
     * Creates a new separator with the specified horizontal or
     * vertical orientation.
     *
     * @param orientation an integer specifying
     *          <code>SwingConstants.HORIZONTAL</code> or
     *          <code>SwingConstants.VERTICAL</code>
     * @exception IllegalArgumentException if <code>orientation</code>
     *          is neither <code>SwingConstants.HORIZONTAL</code> nor
     *          <code>SwingConstants.VERTICAL</code>
     */
    public JSeparator( int orientation )
    {
        checkOrientation( orientation );
        this.orientation = orientation;
        setFocusable(false);
        updateUI();
    }

    /**
     * Returns the L&amp;F object that renders this component.
     *
     * @return the SeparatorUI object that renders this component
     */
    public SeparatorUI getUI() {
        return (SeparatorUI)ui;
    }

    /**
     * Sets the L&amp;F object that renders this component.
     *
     * @param ui  the SeparatorUI L&amp;F object
     * @see UIDefaults#getUI
     */
    @BeanProperty(hidden = true, visualUpdate = true, description
            = "The UI object that implements the Component's LookAndFeel.")
    public void setUI(SeparatorUI ui) {
        super.setUI(ui);
    }

    /**
     * Resets the UI property to a value from the current look and feel.
     *
     * @see JComponent#updateUI
     */
    public void updateUI() {
        setUI((SeparatorUI)UIManager.getUI(this));
    }


    /**
     * Returns the name of the L&amp;F class that renders this component.
     *
     * @return the string "SeparatorUI"
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
     * Returns the orientation of this separator.
     *
     * @return   The value of the orientation property, one of the
     *           following constants defined in <code>SwingConstants</code>:
     *           <code>VERTICAL</code>, or
     *           <code>HORIZONTAL</code>.
     *
     * @see SwingConstants
     * @see #setOrientation
     */
    public int getOrientation() {
        return this.orientation;
    }

    /**
     * Sets the orientation of the separator.
     * The default value of this property is HORIZONTAL.
     * @param orientation  either <code>SwingConstants.HORIZONTAL</code>
     *                  or <code>SwingConstants.VERTICAL</code>
     * @exception IllegalArgumentException  if <code>orientation</code>
     *          is neither <code>SwingConstants.HORIZONTAL</code>
     *          nor <code>SwingConstants.VERTICAL</code>
     *
     * @see SwingConstants
     * @see #getOrientation
     */
    @BeanProperty(preferred = true, visualUpdate = true, enumerationValues = {
            "SwingConstants.HORIZONTAL",
            "SwingConstants.VERTICAL"}, description
            = "The orientation of the separator.")
    public void setOrientation( int orientation ) {
        if (this.orientation == orientation) {
            return;
        }
        int oldValue = this.orientation;
        checkOrientation( orientation );
        this.orientation = orientation;
        firePropertyChange("orientation", oldValue, orientation);
        revalidate();
        repaint();
    }

    private void checkOrientation( int orientation )
    {
        switch ( orientation )
        {
            case VERTICAL:
            case HORIZONTAL:
                break;
            default:
                throw new IllegalArgumentException( "orientation must be one of: VERTICAL, HORIZONTAL" );
        }
    }


    /**
     * Returns a string representation of this <code>JSeparator</code>.
     * This method
     * is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this <code>JSeparator</code>
     */
    protected String paramString() {
        String orientationString = (orientation == HORIZONTAL ?
                                    "HORIZONTAL" : "VERTICAL");

        return super.paramString() +
        ",orientation=" + orientationString;
    }

/////////////////
// Accessibility support
////////////////

    /**
     * Gets the AccessibleContext associated with this JSeparator.
     * For separators, the AccessibleContext takes the form of an
     * AccessibleJSeparator.
     * A new AccessibleJSeparator instance is created if necessary.
     *
     * @return an AccessibleJSeparator that serves as the
     *         AccessibleContext of this JSeparator
     */
    @BeanProperty(bound = false)
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJSeparator();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * <code>JSeparator</code> class.  It provides an implementation of the
     * Java Accessibility API appropriate to separator user-interface elements.
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
    protected class AccessibleJSeparator extends AccessibleJComponent {

        /**
         * Constructs an {@code AccessibleJSeparator}.
         */
        protected AccessibleJSeparator() {}

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         * object
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.SEPARATOR;
        }
    }
}
