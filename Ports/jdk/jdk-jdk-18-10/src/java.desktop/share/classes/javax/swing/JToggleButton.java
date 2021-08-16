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

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.BeanProperty;
import java.beans.JavaBean;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Iterator;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.swing.plaf.ButtonUI;

/**
 * An implementation of a two-state button.
 * The <code>JRadioButton</code> and <code>JCheckBox</code> classes
 * are subclasses of this class.
 * For information on using them see
 * <a
 href="https://docs.oracle.com/javase/tutorial/uiswing/components/button.html">How to Use Buttons, Check Boxes, and Radio Buttons</a>,
 * a section in <em>The Java Tutorial</em>.
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
 * @see JCheckBox
 * @author Jeff Dinkins
 * @since 1.2
 */
@JavaBean(defaultProperty = "UIClassID", description = "An implementation of a two-state button.")
@SwingContainer(false)
@SuppressWarnings("serial") // Same-version serialization only
public class JToggleButton extends AbstractButton implements Accessible {

    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "ToggleButtonUI";

    /**
     * Creates an initially unselected toggle button
     * without setting the text or image.
     */
    public JToggleButton () {
        this(null, null, false);
    }

    /**
     * Creates an initially unselected toggle button
     * with the specified image but no text.
     *
     * @param icon  the image that the button should display
     */
    public JToggleButton(Icon icon) {
        this(null, icon, false);
    }

    /**
     * Creates a toggle button with the specified image
     * and selection state, but no text.
     *
     * @param icon  the image that the button should display
     * @param selected  if true, the button is initially selected;
     *                  otherwise, the button is initially unselected
     */
    public JToggleButton(Icon icon, boolean selected) {
        this(null, icon, selected);
    }

    /**
     * Creates an unselected toggle button with the specified text.
     *
     * @param text  the string displayed on the toggle button
     */
    public JToggleButton (String text) {
        this(text, null, false);
    }

    /**
     * Creates a toggle button with the specified text
     * and selection state.
     *
     * @param text  the string displayed on the toggle button
     * @param selected  if true, the button is initially selected;
     *                  otherwise, the button is initially unselected
     */
    public JToggleButton (String text, boolean selected) {
        this(text, null, selected);
    }

    /**
     * Creates a toggle button where properties are taken from the
     * Action supplied.
     *
     * @param a an instance of an {@code Action}
     * @since 1.3
     */
    public JToggleButton(Action a) {
        this();
        setAction(a);
    }

    /**
     * Creates a toggle button that has the specified text and image,
     * and that is initially unselected.
     *
     * @param text the string displayed on the button
     * @param icon  the image that the button should display
     */
    public JToggleButton(String text, Icon icon) {
        this(text, icon, false);
    }

    /**
     * Creates a toggle button with the specified text, image, and
     * selection state.
     *
     * @param text the text of the toggle button
     * @param icon  the image that the button should display
     * @param selected  if true, the button is initially selected;
     *                  otherwise, the button is initially unselected
     */
    public JToggleButton (String text, Icon icon, boolean selected) {
        // Create the model
        setModel(new ToggleButtonModel());

        model.setSelected(selected);

        // initialize
        init(text, icon);
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
     * Returns a string that specifies the name of the l&amp;f class
     * that renders this component.
     *
     * @return the string "ToggleButtonUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @BeanProperty(bound = false, description
            = "A string that specifies the name of the L&F class")
    public String getUIClassID() {
        return uiClassID;
    }


    /**
     * Overriden to return true, JToggleButton supports
     * the selected state.
     */
    boolean shouldUpdateSelectedStateFromAction() {
        return true;
    }

    private JToggleButton getGroupSelection(FocusEvent.Cause cause) {
        switch (cause) {
          case ACTIVATION:
          case TRAVERSAL:
          case TRAVERSAL_UP:
          case TRAVERSAL_DOWN:
          case TRAVERSAL_FORWARD:
          case TRAVERSAL_BACKWARD:
            ButtonModel model = getModel();
            JToggleButton selection = this;
            if (model != null) {
                ButtonGroup group = model.getGroup();
                if (group != null && group.getSelection() != null
                                                  && !group.isSelected(model)) {
                    Iterator<AbstractButton> iterator =
                                               group.getElements().asIterator();
                    while (iterator.hasNext()) {
                        AbstractButton member = iterator.next();
                        if (group.isSelected(member.getModel())) {
                            if (member instanceof JToggleButton &&
                                member.isVisible() && member.isDisplayable() &&
                                member.isEnabled() && member.isFocusable()) {
                                selection = (JToggleButton) member;
                            }
                            break;
                        }
                    }
                }
            }
            return selection;
          default:
            return this;
        }
    }

    /**
     * If this toggle button is a member of the {@link ButtonGroup} which has
     * another toggle button which is selected and can be the focus owner,
     * and the focus cause argument denotes window activation or focus
     * traversal action of any direction the result of the method execution
     * is the same as calling
     * {@link Component#requestFocus(FocusEvent.Cause)} on the toggle button
     * selected in the group.
     * In all other cases the result of the method is the same as calling
     * {@link Component#requestFocus(FocusEvent.Cause)} on this toggle button.
     *
     * @param  cause the cause why the focus is requested
     * @see ButtonGroup
     * @see Component#requestFocus(FocusEvent.Cause)
     * @see FocusEvent.Cause
     *
     * @since 9
     */
    @Override
    public void requestFocus(FocusEvent.Cause cause) {
        getGroupSelection(cause).requestFocusUnconditionally(cause);
    }

    private void requestFocusUnconditionally(FocusEvent.Cause cause) {
        super.requestFocus(cause);
    }

    /**
     * If this toggle button is a member of the {@link ButtonGroup} which has
     * another toggle button which is selected and can be the focus owner,
     * and the focus cause argument denotes window activation or focus
     * traversal action of any direction the result of the method execution
     * is the same as calling
     * {@link Component#requestFocusInWindow(FocusEvent.Cause)} on the toggle
     * button selected in the group.
     * In all other cases the result of the method is the same as calling
     * {@link Component#requestFocusInWindow(FocusEvent.Cause)} on this toggle
     * button.
     *
     * @param  cause the cause why the focus is requested
     * @see ButtonGroup
     * @see Component#requestFocusInWindow(FocusEvent.Cause)
     * @see FocusEvent.Cause
     *
     * @since 9
     */
    public boolean requestFocusInWindow(FocusEvent.Cause cause) {
        return getGroupSelection(cause)
                                    .requestFocusInWindowUnconditionally(cause);
    }

    private boolean requestFocusInWindowUnconditionally(FocusEvent.Cause cause) {
        return super.requestFocusInWindow(cause);
    }

    // *********************************************************************

    /**
     * The ToggleButton model
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
    public static class ToggleButtonModel extends DefaultButtonModel {

        /**
         * Creates a new ToggleButton Model
         */
        public ToggleButtonModel () {
        }

        /**
         * Checks if the button is selected.
         */
        public boolean isSelected() {
//              if(getGroup() != null) {
//                  return getGroup().isSelected(this);
//              } else {
                return (stateMask & SELECTED) != 0;
//              }
        }


        /**
         * Sets the selected state of the button.
         * @param b true selects the toggle button,
         *          false deselects the toggle button.
         */
        public void setSelected(boolean b) {
            ButtonGroup group = getGroup();
            if (group != null) {
                // use the group model instead
                group.setSelected(this, b);
                b = group.isSelected(this);
            }

            if (isSelected() == b) {
                return;
            }

            if (b) {
                stateMask |= SELECTED;
            } else {
                stateMask &= ~SELECTED;
            }

            // Send ChangeEvent
            fireStateChanged();

            // Send ItemEvent
            fireItemStateChanged(
                    new ItemEvent(this,
                                  ItemEvent.ITEM_STATE_CHANGED,
                                  this,
                                  this.isSelected() ?  ItemEvent.SELECTED : ItemEvent.DESELECTED));

        }

        /**
         * Sets the pressed state of the toggle button.
         */
        @SuppressWarnings("deprecation")
        public void setPressed(boolean b) {
            if ((isPressed() == b) || !isEnabled()) {
                return;
            }

            if (b == false && isArmed()) {
                setSelected(!this.isSelected());
            }

            if (b) {
                stateMask |= PRESSED;
            } else {
                stateMask &= ~PRESSED;
            }

            fireStateChanged();

            if(!isPressed() && isArmed()) {
                int modifiers = 0;
                AWTEvent currentEvent = EventQueue.getCurrentEvent();
                if (currentEvent instanceof InputEvent) {
                    modifiers = ((InputEvent)currentEvent).getModifiers();
                } else if (currentEvent instanceof ActionEvent) {
                    modifiers = ((ActionEvent)currentEvent).getModifiers();
                }
                fireActionPerformed(
                    new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                                    getActionCommand(),
                                    EventQueue.getMostRecentEventTime(),
                                    modifiers));
            }

        }
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
     * Returns a string representation of this JToggleButton. This method
     * is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this JToggleButton.
     */
    protected String paramString() {
        return super.paramString();
    }


/////////////////
// Accessibility support
////////////////

    /**
     * Gets the AccessibleContext associated with this JToggleButton.
     * For toggle buttons, the AccessibleContext takes the form of an
     * AccessibleJToggleButton.
     * A new AccessibleJToggleButton instance is created if necessary.
     *
     * @return an AccessibleJToggleButton that serves as the
     *         AccessibleContext of this JToggleButton
     */
    @BeanProperty(bound = false, expert = true, description
            = "The AccessibleContext associated with this ToggleButton.")
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJToggleButton();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * <code>JToggleButton</code> class.  It provides an implementation of the
     * Java Accessibility API appropriate to toggle button user-interface
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
    protected class AccessibleJToggleButton extends AccessibleAbstractButton
            implements ItemListener {

        /**
         * Constructs {@code AccessibleJToggleButton}
         */
        public AccessibleJToggleButton() {
            super();
            JToggleButton.this.addItemListener(this);
        }

        /**
         * Fire accessible property change events when the state of the
         * toggle button changes.
         */
        public void itemStateChanged(ItemEvent e) {
            JToggleButton tb = (JToggleButton) e.getSource();
            if (JToggleButton.this.accessibleContext != null) {
                if (tb.isSelected()) {
                    JToggleButton.this.accessibleContext.firePropertyChange(
                            AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                            null, AccessibleState.CHECKED);
                } else {
                    JToggleButton.this.accessibleContext.firePropertyChange(
                            AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                            AccessibleState.CHECKED, null);
                }
            }
        }

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         * object
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.TOGGLE_BUTTON;
        }
    } // inner class AccessibleJToggleButton
}
