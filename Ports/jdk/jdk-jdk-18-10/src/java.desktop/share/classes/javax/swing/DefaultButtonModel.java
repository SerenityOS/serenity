/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.Serializable;
import java.util.EventListener;
import javax.swing.event.*;

/**
 * The default implementation of a <code>Button</code> component's data model.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing. As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @author Jeff Dinkins
 * @since 1.2
 */
@SuppressWarnings("serial") // Same-version serialization only
public class DefaultButtonModel implements ButtonModel, Serializable {

    /** The bitmask used to store the state of the button. */
    protected int stateMask = 0;

    /** The action command string fired by the button. */
    protected String actionCommand = null;

    /** The button group that the button belongs to. */
    protected ButtonGroup group = null;

    /** The button's mnemonic. */
    protected int mnemonic = 0;

    /**
     * Only one <code>ChangeEvent</code> is needed per button model
     * instance since the event's only state is the source property.
     * The source of events generated is always "this".
     */
    protected transient ChangeEvent changeEvent = null;

    /** Stores the listeners on this model. */
    protected EventListenerList listenerList = new EventListenerList();

    // controls the usage of the MenuItem.disabledAreNavigable UIDefaults
    // property in the setArmed() method
    private boolean menuItem = false;

    /**
     * Constructs a <code>DefaultButtonModel</code>.
     *
     */
    public DefaultButtonModel() {
        stateMask = 0;
        setEnabled(true);
    }

    /**
     * Identifies the "armed" bit in the bitmask, which
     * indicates partial commitment towards choosing/triggering
     * the button.
     */
    public static final int ARMED = 1 << 0;

    /**
     * Identifies the "selected" bit in the bitmask, which
     * indicates that the button has been selected. Only needed for
     * certain types of buttons - such as radio button or check box.
     */
    public static final int SELECTED = 1 << 1;

    /**
     * Identifies the "pressed" bit in the bitmask, which
     * indicates that the button is pressed.
     */
    public static final int PRESSED = 1 << 2;

    /**
     * Identifies the "enabled" bit in the bitmask, which
     * indicates that the button can be selected by
     * an input device (such as a mouse pointer).
     */
    public static final int ENABLED = 1 << 3;

    /**
     * Identifies the "rollover" bit in the bitmask, which
     * indicates that the mouse is over the button.
     */
    public static final int ROLLOVER = 1 << 4;

    /**
     * {@inheritDoc}
     */
    public void setActionCommand(String actionCommand) {
        this.actionCommand = actionCommand;
    }

    /**
     * {@inheritDoc}
     */
    public String getActionCommand() {
        return actionCommand;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isArmed() {
        return (stateMask & ARMED) != 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSelected() {
        return (stateMask & SELECTED) != 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEnabled() {
        return (stateMask & ENABLED) != 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPressed() {
        return (stateMask & PRESSED) != 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRollover() {
        return (stateMask & ROLLOVER) != 0;
    }

    /**
     * {@inheritDoc}
     */
    public void setArmed(boolean b) {
        if(isMenuItem() &&
                UIManager.getBoolean("MenuItem.disabledAreNavigable")) {
            if ((isArmed() == b)) {
                return;
            }
        } else {
            if ((isArmed() == b) || !isEnabled()) {
                return;
            }
        }

        if (b) {
            stateMask |= ARMED;
        } else {
            stateMask &= ~ARMED;
        }

        fireStateChanged();
    }

    /**
     * {@inheritDoc}
     */
    public void setEnabled(boolean b) {
        if(isEnabled() == b) {
            return;
        }

        if (b) {
            stateMask |= ENABLED;
        } else {
            stateMask &= ~ENABLED;
            // unarm and unpress, just in case
            stateMask &= ~ARMED;
            stateMask &= ~PRESSED;
        }


        fireStateChanged();
    }

    /**
     * {@inheritDoc}
     */
    public void setSelected(boolean b) {
        if (this.isSelected() == b) {
            return;
        }

        if (b) {
            stateMask |= SELECTED;
        } else {
            stateMask &= ~SELECTED;
        }

        fireItemStateChanged(
                new ItemEvent(this,
                              ItemEvent.ITEM_STATE_CHANGED,
                              this,
                              b ?  ItemEvent.SELECTED : ItemEvent.DESELECTED));

        fireStateChanged();

    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("deprecation")
    public void setPressed(boolean b) {
        if((isPressed() == b) || !isEnabled()) {
            return;
        }

        if (b) {
            stateMask |= PRESSED;
        } else {
            stateMask &= ~PRESSED;
        }

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

        fireStateChanged();
    }

    /**
     * {@inheritDoc}
     */
    public void setRollover(boolean b) {
        if((isRollover() == b) || !isEnabled()) {
            return;
        }

        if (b) {
            stateMask |= ROLLOVER;
        } else {
            stateMask &= ~ROLLOVER;
        }

        fireStateChanged();
    }

    /**
     * {@inheritDoc}
     */
    public void setMnemonic(int key) {
        mnemonic = key;
        fireStateChanged();
    }

    /**
     * {@inheritDoc}
     */
    public int getMnemonic() {
        return mnemonic;
    }

    /**
     * {@inheritDoc}
     */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    /**
     * {@inheritDoc}
     */
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    /**
     * Returns an array of all the change listeners
     * registered on this <code>DefaultButtonModel</code>.
     *
     * @return all of this model's <code>ChangeListener</code>s
     *         or an empty
     *         array if no change listeners are currently registered
     *
     * @see #addChangeListener
     * @see #removeChangeListener
     *
     * @since 1.4
     */
    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is created lazily.
     *
     * @see EventListenerList
     */
    protected void fireStateChanged() {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ChangeListener.class) {
                // Lazily create the event:
                if (changeEvent == null)
                    changeEvent = new ChangeEvent(this);
                ((ChangeListener)listeners[i+1]).stateChanged(changeEvent);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    /**
     * {@inheritDoc}
     */
    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    /**
     * Returns an array of all the action listeners
     * registered on this <code>DefaultButtonModel</code>.
     *
     * @return all of this model's <code>ActionListener</code>s
     *         or an empty
     *         array if no action listeners are currently registered
     *
     * @see #addActionListener
     * @see #removeActionListener
     *
     * @since 1.4
     */
    public ActionListener[] getActionListeners() {
        return listenerList.getListeners(ActionListener.class);
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.
     *
     * @param e the <code>ActionEvent</code> to deliver to listeners
     * @see EventListenerList
     */
    protected void fireActionPerformed(ActionEvent e) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ActionListener.class) {
                // Lazily create the event:
                // if (changeEvent == null)
                // changeEvent = new ChangeEvent(this);
                ((ActionListener)listeners[i+1]).actionPerformed(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addItemListener(ItemListener l) {
        listenerList.add(ItemListener.class, l);
    }

    /**
     * {@inheritDoc}
     */
    public void removeItemListener(ItemListener l) {
        listenerList.remove(ItemListener.class, l);
    }

    /**
     * Returns an array of all the item listeners
     * registered on this <code>DefaultButtonModel</code>.
     *
     * @return all of this model's <code>ItemListener</code>s
     *         or an empty
     *         array if no item listeners are currently registered
     *
     * @see #addItemListener
     * @see #removeItemListener
     *
     * @since 1.4
     */
    public ItemListener[] getItemListeners() {
        return listenerList.getListeners(ItemListener.class);
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.
     *
     * @param e the <code>ItemEvent</code> to deliver to listeners
     * @see EventListenerList
     */
    protected void fireItemStateChanged(ItemEvent e) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ItemListener.class) {
                // Lazily create the event:
                // if (changeEvent == null)
                // changeEvent = new ChangeEvent(this);
                ((ItemListener)listeners[i+1]).itemStateChanged(e);
            }
        }
    }

    /**
     * Returns an array of all the objects currently registered as
     * <code><em>Foo</em>Listener</code>s
     * upon this model.
     * <code><em>Foo</em>Listener</code>s
     * are registered using the <code>add<em>Foo</em>Listener</code> method.
     * <p>
     * You can specify the <code>listenerType</code> argument
     * with a class literal, such as <code><em>Foo</em>Listener.class</code>.
     * For example, you can query a <code>DefaultButtonModel</code>
     * instance <code>m</code>
     * for its action listeners
     * with the following code:
     *
     * <pre>ActionListener[] als = (ActionListener[])(m.getListeners(ActionListener.class));</pre>
     *
     * If no such listeners exist,
     * this method returns an empty array.
     *
     * @param <T> the type of requested listeners
     * @param listenerType  the type of listeners requested;
     *          this parameter should specify an interface
     *          that descends from <code>java.util.EventListener</code>
     * @return an array of all objects registered as
     *          <code><em>Foo</em>Listener</code>s
     *          on this model,
     *          or an empty array if no such
     *          listeners have been added
     * @exception ClassCastException if <code>listenerType</code> doesn't
     *          specify a class or interface that implements
     *          <code>java.util.EventListener</code>
     *
     * @see #getActionListeners
     * @see #getChangeListeners
     * @see #getItemListeners
     *
     * @since 1.3
     */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    /** Overridden to return <code>null</code>. */
    public Object[] getSelectedObjects() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void setGroup(ButtonGroup group) {
        this.group = group;
    }

    /**
     * Returns the group that the button belongs to.
     * Normally used with radio buttons, which are mutually
     * exclusive within their group.
     *
     * @return the <code>ButtonGroup</code> that the button belongs to
     *
     * @since 1.3
     */
    public ButtonGroup getGroup() {
        return group;
    }

    boolean isMenuItem() {
        return menuItem;
    }

    void setMenuItem(boolean menuItem) {
        this.menuItem = menuItem;
    }
}
