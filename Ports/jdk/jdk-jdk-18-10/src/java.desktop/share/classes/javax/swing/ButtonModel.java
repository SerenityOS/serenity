/*
 * Copyright (c) 1997, 2006, Oracle and/or its affiliates. All rights reserved.
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


import java.awt.event.*;
import java.awt.*;
import javax.swing.event.*;

/**
 * State model for buttons.
 * <p>
 * This model is used for regular buttons, as well as check boxes
 * and radio buttons, which are special kinds of buttons. In practice,
 * a button's UI takes the responsibility of calling methods on its
 * model to manage the state, as detailed below:
 * <p>
 * In simple terms, pressing and releasing the mouse over a regular
 * button triggers the button and causes and <code>ActionEvent</code>
 * to be fired. The same behavior can be produced via a keyboard key
 * defined by the look and feel of the button (typically the SPACE BAR).
 * Pressing and releasing this key while the button has
 * focus will give the same results. For check boxes and radio buttons, the
 * mouse or keyboard equivalent sequence just described causes the button
 * to become selected.
 * <p>
 * In details, the state model for buttons works as follows
 * when used with the mouse:
 * <br>
 * Pressing the mouse on top of a button makes the model both
 * armed and pressed. As long as the mouse remains down,
 * the model remains pressed, even if the mouse moves
 * outside the button. On the contrary, the model is only
 * armed while the mouse remains pressed within the bounds of
 * the button (it can move in or out of the button, but the model
 * is only armed during the portion of time spent within the button).
 * A button is triggered, and an <code>ActionEvent</code> is fired,
 * when the mouse is released while the model is armed
 * - meaning when it is released over top of the button after the mouse
 * has previously been pressed on that button (and not already released).
 * Upon mouse release, the model becomes unarmed and unpressed.
 * <p>
 * In details, the state model for buttons works as follows
 * when used with the keyboard:
 * <br>
 * Pressing the look and feel defined keyboard key while the button
 * has focus makes the model both armed and pressed. As long as this key
 * remains down, the model remains in this state. Releasing the key sets
 * the model to unarmed and unpressed, triggers the button, and causes an
 * <code>ActionEvent</code> to be fired.
 *
 * @author Jeff Dinkins
 * @since 1.2
 */
public interface ButtonModel extends ItemSelectable {

    /**
     * Indicates partial commitment towards triggering the
     * button.
     *
     * @return <code>true</code> if the button is armed,
     *         and ready to be triggered
     * @see #setArmed
     */
    boolean isArmed();

    /**
     * Indicates if the button has been selected. Only needed for
     * certain types of buttons - such as radio buttons and check boxes.
     *
     * @return <code>true</code> if the button is selected
     */
    boolean isSelected();

    /**
     * Indicates if the button can be selected or triggered by
     * an input device, such as a mouse pointer.
     *
     * @return <code>true</code> if the button is enabled
     */
    boolean isEnabled();

    /**
     * Indicates if the button is pressed.
     *
     * @return <code>true</code> if the button is pressed
     */
    boolean isPressed();

    /**
     * Indicates that the mouse is over the button.
     *
     * @return <code>true</code> if the mouse is over the button
     */
    boolean isRollover();

    /**
     * Marks the button as armed or unarmed.
     *
     * @param b whether or not the button should be armed
     */
    public void setArmed(boolean b);

    /**
     * Selects or deselects the button.
     *
     * @param b <code>true</code> selects the button,
     *          <code>false</code> deselects the button
     */
    public void setSelected(boolean b);

    /**
     * Enables or disables the button.
     *
     * @param b whether or not the button should be enabled
     * @see #isEnabled
     */
    public void setEnabled(boolean b);

    /**
     * Sets the button to pressed or unpressed.
     *
     * @param b whether or not the button should be pressed
     * @see #isPressed
     */
    public void setPressed(boolean b);

    /**
     * Sets or clears the button's rollover state
     *
     * @param b whether or not the button is in the rollover state
     * @see #isRollover
     */
    public void setRollover(boolean b);

    /**
     * Sets the keyboard mnemonic (shortcut key or
     * accelerator key) for the button.
     *
     * @param key an int specifying the accelerator key
     */
    public void setMnemonic(int key);

    /**
     * Gets the keyboard mnemonic for the button.
     *
     * @return an int specifying the accelerator key
     * @see #setMnemonic
     */
    public int  getMnemonic();

    /**
     * Sets the action command string that gets sent as part of the
     * <code>ActionEvent</code> when the button is triggered.
     *
     * @param s the <code>String</code> that identifies the generated event
     * @see #getActionCommand
     * @see java.awt.event.ActionEvent#getActionCommand
     */
    public void setActionCommand(String s);

    /**
     * Returns the action command string for the button.
     *
     * @return the <code>String</code> that identifies the generated event
     * @see #setActionCommand
     */
    public String getActionCommand();

    /**
     * Identifies the group the button belongs to --
     * needed for radio buttons, which are mutually
     * exclusive within their group.
     *
     * @param group the <code>ButtonGroup</code> the button belongs to
     */
    public void setGroup(ButtonGroup group);

    /**
     * Returns the group that the button belongs to.
     * Normally used with radio buttons, which are mutually
     * exclusive within their group.
     *
     * @implSpec The default implementation of this method returns {@code null}.
     * Subclasses should return the group set by setGroup().
     *
     * @return the <code>ButtonGroup</code> that the button belongs to
     * @since 10
     */
    default ButtonGroup getGroup() {
        return null;
    }

    /**
     * Adds an <code>ActionListener</code> to the model.
     *
     * @param l the listener to add
     */
    void addActionListener(ActionListener l);

    /**
     * Removes an <code>ActionListener</code> from the model.
     *
     * @param l the listener to remove
     */
    void removeActionListener(ActionListener l);

    /**
     * Adds an <code>ItemListener</code> to the model.
     *
     * @param l the listener to add
     */
    void addItemListener(ItemListener l);

    /**
     * Removes an <code>ItemListener</code> from the model.
     *
     * @param l the listener to remove
     */
    void removeItemListener(ItemListener l);

    /**
     * Adds a <code>ChangeListener</code> to the model.
     *
     * @param l the listener to add
     */
    void addChangeListener(ChangeListener l);

    /**
     * Removes a <code>ChangeListener</code> from the model.
     *
     * @param l the listener to remove
     */
    void removeChangeListener(ChangeListener l);

}
