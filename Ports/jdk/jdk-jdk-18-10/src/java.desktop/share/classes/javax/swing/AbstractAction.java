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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.security.AccessController;

import javax.swing.event.SwingPropertyChangeSupport;

import sun.security.action.GetPropertyAction;

/**
 * This class provides default implementations for the JFC <code>Action</code>
 * interface. Standard behaviors like the get and set methods for
 * <code>Action</code> object properties (icon, text, and enabled) are defined
 * here. The developer need only subclass this abstract class and
 * define the <code>actionPerformed</code> method.
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
 * @see Action
 * @since 1.2
 */
@SuppressWarnings("serial") // Same-version serialization only
public abstract class AbstractAction implements Action, Cloneable, Serializable
{
    /**
     * Whether or not actions should reconfigure all properties on null.
     */
    private static Boolean RECONFIGURE_ON_NULL;

    /**
     * Specifies whether action is enabled; the default is true.
     */
    protected boolean enabled = true;


    /**
     * Contains the array of key bindings.
     */
    private transient ArrayTable arrayTable;

    /**
     * Whether or not to reconfigure all action properties from the
     * specified event.
     */
    @SuppressWarnings("removal")
    static boolean shouldReconfigure(PropertyChangeEvent e) {
        if (e.getPropertyName() == null) {
            synchronized(AbstractAction.class) {
                if (RECONFIGURE_ON_NULL == null) {
                    RECONFIGURE_ON_NULL = Boolean.valueOf(
                        AccessController.doPrivileged(new GetPropertyAction(
                        "swing.actions.reconfigureOnNull", "false")));
                }
                return RECONFIGURE_ON_NULL;
            }
        }
        return false;
    }

    /**
     * Sets the enabled state of a component from an Action.
     *
     * @param c the Component to set the enabled state on
     * @param a the Action to set the enabled state from, may be null
     */
    static void setEnabledFromAction(JComponent c, Action a) {
        c.setEnabled((a != null) ? a.isEnabled() : true);
    }

    /**
     * Sets the tooltip text of a component from an Action.
     *
     * @param c the Component to set the tooltip text on
     * @param a the Action to set the tooltip text from, may be null
     */
    static void setToolTipTextFromAction(JComponent c, Action a) {
        c.setToolTipText(a != null ?
                         (String)a.getValue(Action.SHORT_DESCRIPTION) : null);
    }

    static boolean hasSelectedKey(Action a) {
        return (a != null && a.getValue(Action.SELECTED_KEY) != null);
    }

    static boolean isSelected(Action a) {
        return Boolean.TRUE.equals(a.getValue(Action.SELECTED_KEY));
    }



    /**
     * Creates an {@code Action}.
     */
    public AbstractAction() {
    }

    /**
     * Creates an {@code Action} with the specified name.
     *
     * @param name the name ({@code Action.NAME}) for the action; a
     *        value of {@code null} is ignored
     */
    public AbstractAction(String name) {
        putValue(Action.NAME, name);
    }

    /**
     * Creates an {@code Action} with the specified name and small icon.
     *
     * @param name the name ({@code Action.NAME}) for the action; a
     *        value of {@code null} is ignored
     * @param icon the small icon ({@code Action.SMALL_ICON}) for the action; a
     *        value of {@code null} is ignored
     */
    public AbstractAction(String name, Icon icon) {
        this(name);
        putValue(Action.SMALL_ICON, icon);
    }

    /**
     * Gets the <code>Object</code> associated with the specified key.
     *
     * @param key a string containing the specified <code>key</code>
     * @return the binding <code>Object</code> stored with this key; if there
     *          are no keys, it will return <code>null</code>
     * @see Action#getValue
     */
    public Object getValue(String key) {
        if (key == "enabled") {
            return enabled;
        }
        if (arrayTable == null) {
            return null;
        }
        return arrayTable.get(key);
    }

    /**
     * Sets the <code>Value</code> associated with the specified key.
     *
     * @param key  the <code>String</code> that identifies the stored object
     * @param newValue the <code>Object</code> to store using this key
     * @see Action#putValue
     */
    public void putValue(String key, Object newValue) {
        Object oldValue = null;
        if (key == "enabled") {
            // Treat putValue("enabled") the same way as a call to setEnabled.
            // If we don't do this it means the two may get out of sync, and a
            // bogus property change notification would be sent.
            //
            // To avoid dependencies between putValue & setEnabled this
            // directly changes enabled. If we instead called setEnabled
            // to change enabled, it would be possible for stack
            // overflow in the case where a developer implemented setEnabled
            // in terms of putValue.
            if (newValue == null || !(newValue instanceof Boolean)) {
                newValue = false;
            }
            oldValue = enabled;
            enabled = (Boolean)newValue;
        } else {
            if (arrayTable == null) {
                arrayTable = new ArrayTable();
            }
            if (arrayTable.containsKey(key))
                oldValue = arrayTable.get(key);
            // Remove the entry for key if newValue is null
            // else put in the newValue for key.
            if (newValue == null) {
                arrayTable.remove(key);
            } else {
                arrayTable.put(key,newValue);
            }
        }
        firePropertyChange(key, oldValue, newValue);
    }

    /**
     * Returns true if the action is enabled.
     *
     * @return true if the action is enabled, false otherwise
     * @see Action#isEnabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether the {@code Action} is enabled. The default is {@code true}.
     *
     * @param newValue  {@code true} to enable the action, {@code false} to
     *                  disable it
     * @see Action#setEnabled
     */
    public void setEnabled(boolean newValue) {
        boolean oldValue = this.enabled;

        if (oldValue != newValue) {
            this.enabled = newValue;
            firePropertyChange("enabled",
                               Boolean.valueOf(oldValue), Boolean.valueOf(newValue));
        }
    }


    /**
     * Returns an array of <code>Object</code>s which are keys for
     * which values have been set for this <code>AbstractAction</code>,
     * or <code>null</code> if no keys have values set.
     * @return an array of key objects, or <code>null</code> if no
     *                  keys have values set
     * @since 1.3
     */
    public Object[] getKeys() {
        if (arrayTable == null) {
            return null;
        }
        Object[] keys = new Object[arrayTable.size()];
        arrayTable.getKeys(keys);
        return keys;
    }

    /**
     * If any <code>PropertyChangeListeners</code> have been registered, the
     * <code>changeSupport</code> field describes them.
     */
    protected SwingPropertyChangeSupport changeSupport;

    /**
     * Supports reporting bound property changes.  This method can be called
     * when a bound property has changed and it will send the appropriate
     * <code>PropertyChangeEvent</code> to any registered
     * <code>PropertyChangeListeners</code>.
     *
     * @param propertyName  the name of the property that has changed
     * @param oldValue  the old value of the property
     * @param newValue  the new value of the property
     */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (changeSupport == null ||
            (oldValue != null && newValue != null && oldValue.equals(newValue))) {
            return;
        }
        changeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }


    /**
     * Adds a <code>PropertyChangeListener</code> to the listener list.
     * The listener is registered for all properties.
     * <p>
     * A <code>PropertyChangeEvent</code> will get fired in response to setting
     * a bound property, e.g. <code>setFont</code>, <code>setBackground</code>,
     * or <code>setForeground</code>.
     * Note that if the current component is inheriting its foreground,
     * background, or font from its container, then no event will be
     * fired in response to a change in the inherited property.
     *
     * @param listener  The <code>PropertyChangeListener</code> to be added
     *
     * @see Action#addPropertyChangeListener
     */
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null) {
            changeSupport = new SwingPropertyChangeSupport(this);
        }
        changeSupport.addPropertyChangeListener(listener);
    }


    /**
     * Removes a <code>PropertyChangeListener</code> from the listener list.
     * This removes a <code>PropertyChangeListener</code> that was registered
     * for all properties.
     *
     * @param listener  the <code>PropertyChangeListener</code> to be removed
     *
     * @see Action#removePropertyChangeListener
     */
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null) {
            return;
        }
        changeSupport.removePropertyChangeListener(listener);
    }


    /**
     * Returns an array of all the <code>PropertyChangeListener</code>s added
     * to this AbstractAction with addPropertyChangeListener().
     *
     * @return all of the <code>PropertyChangeListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.4
     */
    public synchronized PropertyChangeListener[] getPropertyChangeListeners() {
        if (changeSupport == null) {
            return new PropertyChangeListener[0];
        }
        return changeSupport.getPropertyChangeListeners();
    }


    /**
     * Clones the abstract action. This gives the clone
     * its own copy of the key/value list,
     * which is not handled for you by <code>Object.clone()</code>.
     **/

    protected Object clone() throws CloneNotSupportedException {
        AbstractAction newAction = (AbstractAction)super.clone();
        synchronized(this) {
            if (arrayTable != null) {
                newAction.arrayTable = (ArrayTable)arrayTable.clone();
            }
        }
        return newAction;
    }

    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        // Store the default fields
        s.defaultWriteObject();

        // And the keys
        ArrayTable.writeArrayTable(s, arrayTable);
    }

    @Serial
    private void readObject(ObjectInputStream s) throws ClassNotFoundException,
        IOException {
        s.defaultReadObject();
        for (int counter = s.readInt() - 1; counter >= 0; counter--) {
            putValue((String)s.readObject(), s.readObject());
        }
    }
}
