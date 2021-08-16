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
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Objects;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.plaf.ToolTipUI;

/**
 * Used to display a "Tip" for a Component. Typically components provide api
 * to automate the process of using <code>ToolTip</code>s.
 * For example, any Swing component can use the <code>JComponent</code>
 * <code>setToolTipText</code> method to specify the text
 * for a standard tooltip. A component that wants to create a custom
 * <code>ToolTip</code>
 * display can override <code>JComponent</code>'s <code>createToolTip</code>
 * method and use a subclass of this class.
 * <p>
 * See <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/tooltip.html">How to Use Tool Tips</a>
 * in <em>The Java Tutorial</em>
 * for further documentation.
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
 * @see JComponent#setToolTipText
 * @see JComponent#createToolTip
 * @author Dave Moore
 * @author Rich Shiavi
 * @since 1.2
 */
@SuppressWarnings("serial")
public class JToolTip extends JComponent implements Accessible {
    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "ToolTipUI";

    String tipText;
    JComponent component;

    /** Creates a tool tip. */
    public JToolTip() {
        updateUI();
    }

    /**
     * Returns the L&amp;F object that renders this component.
     *
     * @return the <code>ToolTipUI</code> object that renders this component
     */
    public ToolTipUI getUI() {
        return (ToolTipUI)ui;
    }

    /**
     * Resets the UI property to a value from the current look and feel.
     *
     * @see JComponent#updateUI
     */
    public void updateUI() {
        setUI((ToolTipUI)UIManager.getUI(this));
    }


    /**
     * Returns the name of the L&amp;F class that renders this component.
     *
     * @return the string "ToolTipUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    public String getUIClassID() {
        return uiClassID;
    }


    /**
     * Sets the text to show when the tool tip is displayed.
     * The string <code>tipText</code> may be <code>null</code>.
     *
     * @param tipText the <code>String</code> to display
     */
    @BeanProperty(preferred = true, description
            = "Sets the text of the tooltip")
    public void setTipText(String tipText) {
        String oldValue = this.tipText;
        this.tipText = tipText;
        firePropertyChange("tiptext", oldValue, tipText);

        if (!Objects.equals(oldValue, tipText)) {
            revalidate();
            repaint();
        }
    }

    /**
     * Returns the text that is shown when the tool tip is displayed.
     * The returned value may be <code>null</code>.
     *
     * @return the <code>String</code> that is displayed
     */
    public String getTipText() {
        return tipText;
    }

    /**
     * Specifies the component that the tooltip describes.
     * The component <code>c</code> may be <code>null</code>
     * and will have no effect.
     * <p>
     * This is a bound property.
     *
     * @param c the <code>JComponent</code> being described
     * @see JComponent#createToolTip
     */
    @BeanProperty(description
            = "Sets the component that the tooltip describes.")
    public void setComponent(JComponent c) {
        JComponent oldValue = this.component;

        component = c;
        firePropertyChange("component", oldValue, c);
    }

    /**
     * Returns the component the tooltip applies to.
     * The returned value may be <code>null</code>.
     *
     * @return the component that the tooltip describes
     *
     * @see JComponent#createToolTip
     */
    public JComponent getComponent() {
        return component;
    }

    /**
     * Always returns true since tooltips, by definition,
     * should always be on top of all other windows.
     */
    // package private
    boolean alwaysOnTop() {
        return true;
    }


    /**
     * See <code>readObject</code> and <code>writeObject</code>
     * in <code>JComponent</code> for more
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
     * Returns a string representation of this <code>JToolTip</code>.
     * This method
     * is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this <code>JToolTip</code>
     */
    protected String paramString() {
        String tipTextString = (tipText != null ?
                                tipText : "");

        return super.paramString() +
        ",tipText=" + tipTextString;
    }


/////////////////
// Accessibility support
////////////////

    /**
     * Gets the AccessibleContext associated with this JToolTip.
     * For tool tips, the AccessibleContext takes the form of an
     * AccessibleJToolTip.
     * A new AccessibleJToolTip instance is created if necessary.
     *
     * @return an AccessibleJToolTip that serves as the
     *         AccessibleContext of this JToolTip
     */
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJToolTip();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * <code>JToolTip</code> class.  It provides an implementation of the
     * Java Accessibility API appropriate to tool tip user-interface elements.
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
    protected class AccessibleJToolTip extends AccessibleJComponent {

        /**
         * Constructs an {@code AccessibleJToolTip}.
         */
        protected AccessibleJToolTip() {}

        /**
         * Get the accessible description of this object.
         *
         * @return a localized String describing this object.
         */
        public String getAccessibleDescription() {
            String description = accessibleDescription;

            // fallback to client property
            if (description == null) {
                description = (String)getClientProperty(AccessibleContext.ACCESSIBLE_DESCRIPTION_PROPERTY);
            }
            if (description == null) {
                description = getTipText();
            }
            return description;
        }

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         * object
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.TOOL_TIP;
        }
    }
}
