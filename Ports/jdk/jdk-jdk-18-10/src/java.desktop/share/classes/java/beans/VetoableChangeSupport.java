/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.beans;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serial;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map.Entry;

/**
 * This is a utility class that can be used by beans that support constrained
 * properties.  It manages a list of listeners and dispatches
 * {@link PropertyChangeEvent}s to them.  You can use an instance of this class
 * as a member field of your bean and delegate these types of work to it.
 * The {@link VetoableChangeListener} can be registered for all properties
 * or for a property specified by name.
 * <p>
 * Here is an example of {@code VetoableChangeSupport} usage that follows
 * the rules and recommendations laid out in the JavaBeans specification:
 * <pre>{@code
 * public class MyBean {
 *     private final VetoableChangeSupport vcs = new VetoableChangeSupport(this);
 *
 *     public void addVetoableChangeListener(VetoableChangeListener listener) {
 *         this.vcs.addVetoableChangeListener(listener);
 *     }
 *
 *     public void removeVetoableChangeListener(VetoableChangeListener listener) {
 *         this.vcs.removeVetoableChangeListener(listener);
 *     }
 *
 *     private String value;
 *
 *     public String getValue() {
 *         return this.value;
 *     }
 *
 *     public void setValue(String newValue) throws PropertyVetoException {
 *         String oldValue = this.value;
 *         this.vcs.fireVetoableChange("value", oldValue, newValue);
 *         this.value = newValue;
 *     }
 *
 *     [...]
 * }
 * }</pre>
 * <p>
 * A {@code VetoableChangeSupport} instance is thread-safe.
 * <p>
 * This class is serializable.  When it is serialized it will save
 * (and restore) any listeners that are themselves serializable.  Any
 * non-serializable listeners will be skipped during serialization.
 *
 * @see PropertyChangeSupport
 * @since 1.1
 */
public class VetoableChangeSupport implements Serializable {
    private VetoableChangeListenerMap map = new VetoableChangeListenerMap();

    /**
     * Constructs a {@code VetoableChangeSupport} object.
     *
     * @param sourceBean  The bean to be given as the source for any events.
     */
    public VetoableChangeSupport(Object sourceBean) {
        if (sourceBean == null) {
            throw new NullPointerException();
        }
        source = sourceBean;
    }

    /**
     * Add a VetoableChangeListener to the listener list.
     * The listener is registered for all properties.
     * The same listener object may be added more than once, and will be called
     * as many times as it is added.
     * If {@code listener} is null, no exception is thrown and no action
     * is taken.
     *
     * @param listener  The VetoableChangeListener to be added
     */
    public void addVetoableChangeListener(VetoableChangeListener listener) {
        if (listener == null) {
            return;
        }
        if (listener instanceof VetoableChangeListenerProxy) {
            VetoableChangeListenerProxy proxy =
                    (VetoableChangeListenerProxy)listener;
            // Call two argument add method.
            addVetoableChangeListener(proxy.getPropertyName(),
                                      proxy.getListener());
        } else {
            this.map.add(null, listener);
        }
    }

    /**
     * Remove a VetoableChangeListener from the listener list.
     * This removes a VetoableChangeListener that was registered
     * for all properties.
     * If {@code listener} was added more than once to the same event
     * source, it will be notified one less time after being removed.
     * If {@code listener} is null, or was never added, no exception is
     * thrown and no action is taken.
     *
     * @param listener  The VetoableChangeListener to be removed
     */
    public void removeVetoableChangeListener(VetoableChangeListener listener) {
        if (listener == null) {
            return;
        }
        if (listener instanceof VetoableChangeListenerProxy) {
            VetoableChangeListenerProxy proxy =
                    (VetoableChangeListenerProxy)listener;
            // Call two argument remove method.
            removeVetoableChangeListener(proxy.getPropertyName(),
                                         proxy.getListener());
        } else {
            this.map.remove(null, listener);
        }
    }

    /**
     * Returns an array of all the listeners that were added to the
     * VetoableChangeSupport object with addVetoableChangeListener().
     * <p>
     * If some listeners have been added with a named property, then
     * the returned array will be a mixture of VetoableChangeListeners
     * and {@code VetoableChangeListenerProxy}s. If the calling
     * method is interested in distinguishing the listeners then it must
     * test each element to see if it's a
     * {@code VetoableChangeListenerProxy}, perform the cast, and examine
     * the parameter.
     *
     * <pre>{@code
     * VetoableChangeListener[] listeners = bean.getVetoableChangeListeners();
     * for (int i = 0; i < listeners.length; i++) {
     *        if (listeners[i] instanceof VetoableChangeListenerProxy) {
     *     VetoableChangeListenerProxy proxy =
     *                    (VetoableChangeListenerProxy)listeners[i];
     *     if (proxy.getPropertyName().equals("foo")) {
     *       // proxy is a VetoableChangeListener which was associated
     *       // with the property named "foo"
     *     }
     *   }
     * }
     * }</pre>
     *
     * @see VetoableChangeListenerProxy
     * @return all of the {@code VetoableChangeListeners} added or an
     *         empty array if no listeners have been added
     * @since 1.4
     */
    public VetoableChangeListener[] getVetoableChangeListeners(){
        return this.map.getListeners();
    }

    /**
     * Add a VetoableChangeListener for a specific property.  The listener
     * will be invoked only when a call on fireVetoableChange names that
     * specific property.
     * The same listener object may be added more than once.  For each
     * property,  the listener will be invoked the number of times it was added
     * for that property.
     * If {@code propertyName} or {@code listener} is null, no
     * exception is thrown and no action is taken.
     *
     * @param propertyName  The name of the property to listen on.
     * @param listener  The VetoableChangeListener to be added
     * @since 1.2
     */
    public void addVetoableChangeListener(
                                String propertyName,
                VetoableChangeListener listener) {
        if (listener == null || propertyName == null) {
            return;
        }
        listener = this.map.extract(listener);
        if (listener != null) {
            this.map.add(propertyName, listener);
        }
    }

    /**
     * Remove a VetoableChangeListener for a specific property.
     * If {@code listener} was added more than once to the same event
     * source for the specified property, it will be notified one less time
     * after being removed.
     * If {@code propertyName} is null, no exception is thrown and no
     * action is taken.
     * If {@code listener} is null, or was never added for the specified
     * property, no exception is thrown and no action is taken.
     *
     * @param propertyName  The name of the property that was listened on.
     * @param listener  The VetoableChangeListener to be removed
     * @since 1.2
     */
    public void removeVetoableChangeListener(
                                String propertyName,
                VetoableChangeListener listener) {
        if (listener == null || propertyName == null) {
            return;
        }
        listener = this.map.extract(listener);
        if (listener != null) {
            this.map.remove(propertyName, listener);
        }
    }

    /**
     * Returns an array of all the listeners which have been associated
     * with the named property.
     *
     * @param propertyName  The name of the property being listened to
     * @return all the {@code VetoableChangeListeners} associated with
     *         the named property.  If no such listeners have been added,
     *         or if {@code propertyName} is null, an empty array is
     *         returned.
     * @since 1.4
     */
    public VetoableChangeListener[] getVetoableChangeListeners(String propertyName) {
        return this.map.getListeners(propertyName);
    }

    /**
     * Reports a constrained property update to listeners
     * that have been registered to track updates of
     * all properties or a property with the specified name.
     * <p>
     * Any listener can throw a {@code PropertyVetoException} to veto the update.
     * If one of the listeners vetoes the update, this method passes
     * a new "undo" {@code PropertyChangeEvent} that reverts to the old value
     * to all listeners that already confirmed this update
     * and throws the {@code PropertyVetoException} again.
     * <p>
     * No event is fired if old and new values are equal and non-null.
     * <p>
     * This is merely a convenience wrapper around the more general
     * {@link #fireVetoableChange(PropertyChangeEvent)} method.
     *
     * @param propertyName  the programmatic name of the property that is about to change
     * @param oldValue      the old value of the property
     * @param newValue      the new value of the property
     * @throws PropertyVetoException if one of listeners vetoes the property update
     */
    public void fireVetoableChange(String propertyName, Object oldValue, Object newValue)
            throws PropertyVetoException {
        if (oldValue == null || newValue == null || !oldValue.equals(newValue)) {
            fireVetoableChange(new PropertyChangeEvent(this.source, propertyName, oldValue, newValue));
        }
    }

    /**
     * Reports an integer constrained property update to listeners
     * that have been registered to track updates of
     * all properties or a property with the specified name.
     * <p>
     * Any listener can throw a {@code PropertyVetoException} to veto the update.
     * If one of the listeners vetoes the update, this method passes
     * a new "undo" {@code PropertyChangeEvent} that reverts to the old value
     * to all listeners that already confirmed this update
     * and throws the {@code PropertyVetoException} again.
     * <p>
     * No event is fired if old and new values are equal.
     * <p>
     * This is merely a convenience wrapper around the more general
     * {@link #fireVetoableChange(String, Object, Object)} method.
     *
     * @param propertyName  the programmatic name of the property that is about to change
     * @param oldValue      the old value of the property
     * @param newValue      the new value of the property
     * @throws PropertyVetoException if one of listeners vetoes the property update
     * @since 1.2
     */
    public void fireVetoableChange(String propertyName, int oldValue, int newValue)
            throws PropertyVetoException {
        if (oldValue != newValue) {
            fireVetoableChange(propertyName, Integer.valueOf(oldValue), Integer.valueOf(newValue));
        }
    }

    /**
     * Reports a boolean constrained property update to listeners
     * that have been registered to track updates of
     * all properties or a property with the specified name.
     * <p>
     * Any listener can throw a {@code PropertyVetoException} to veto the update.
     * If one of the listeners vetoes the update, this method passes
     * a new "undo" {@code PropertyChangeEvent} that reverts to the old value
     * to all listeners that already confirmed this update
     * and throws the {@code PropertyVetoException} again.
     * <p>
     * No event is fired if old and new values are equal.
     * <p>
     * This is merely a convenience wrapper around the more general
     * {@link #fireVetoableChange(String, Object, Object)} method.
     *
     * @param propertyName  the programmatic name of the property that is about to change
     * @param oldValue      the old value of the property
     * @param newValue      the new value of the property
     * @throws PropertyVetoException if one of listeners vetoes the property update
     * @since 1.2
     */
    public void fireVetoableChange(String propertyName, boolean oldValue, boolean newValue)
            throws PropertyVetoException {
        if (oldValue != newValue) {
            fireVetoableChange(propertyName, Boolean.valueOf(oldValue), Boolean.valueOf(newValue));
        }
    }

    /**
     * Fires a property change event to listeners
     * that have been registered to track updates of
     * all properties or a property with the specified name.
     * <p>
     * Any listener can throw a {@code PropertyVetoException} to veto the update.
     * If one of the listeners vetoes the update, this method passes
     * a new "undo" {@code PropertyChangeEvent} that reverts to the old value
     * to all listeners that already confirmed this update
     * and throws the {@code PropertyVetoException} again.
     * <p>
     * No event is fired if the given event's old and new values are equal and non-null.
     *
     * @param event  the {@code PropertyChangeEvent} to be fired
     * @throws PropertyVetoException if one of listeners vetoes the property update
     * @since 1.2
     */
    public void fireVetoableChange(PropertyChangeEvent event)
            throws PropertyVetoException {
        Object oldValue = event.getOldValue();
        Object newValue = event.getNewValue();
        if (oldValue == null || newValue == null || !oldValue.equals(newValue)) {
            String name = event.getPropertyName();

            VetoableChangeListener[] common = this.map.get(null);
            VetoableChangeListener[] named = (name != null)
                        ? this.map.get(name)
                        : null;

            VetoableChangeListener[] listeners;
            if (common == null) {
                listeners = named;
            }
            else if (named == null) {
                listeners = common;
            }
            else {
                listeners = new VetoableChangeListener[common.length + named.length];
                System.arraycopy(common, 0, listeners, 0, common.length);
                System.arraycopy(named, 0, listeners, common.length, named.length);
            }
            if (listeners != null) {
                int current = 0;
                try {
                    while (current < listeners.length) {
                        listeners[current].vetoableChange(event);
                        current++;
                    }
                }
                catch (PropertyVetoException veto) {
                    event = new PropertyChangeEvent(this.source, name, newValue, oldValue);
                    for (int i = 0; i < current; i++) {
                        try {
                            listeners[i].vetoableChange(event);
                        }
                        catch (PropertyVetoException exception) {
                            // ignore exceptions that occur during rolling back
                        }
                    }
                    throw veto; // rethrow the veto exception
                }
            }
        }
    }

    /**
     * Check if there are any listeners for a specific property, including
     * those registered on all properties.  If {@code propertyName}
     * is null, only check for listeners registered on all properties.
     *
     * @param propertyName  the property name.
     * @return true if there are one or more listeners for the given property
     * @since 1.2
     */
    public boolean hasListeners(String propertyName) {
        return this.map.hasListeners(propertyName);
    }

    /**
     * Writes serializable fields to stream.
     *
     * @param  s the {@code ObjectOutputStream} to write
     * @throws IOException if an I/O error occurs
     * @serialData Null terminated list of {@code VetoableChangeListeners}.
     * <p>
     * At serialization time we skip non-serializable listeners and
     * only serialize the serializable listeners.
     */
    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        Hashtable<String, VetoableChangeSupport> children = null;
        VetoableChangeListener[] listeners = null;
        synchronized (this.map) {
            for (Entry<String, VetoableChangeListener[]> entry : this.map.getEntries()) {
                String property = entry.getKey();
                if (property == null) {
                    listeners = entry.getValue();
                } else {
                    if (children == null) {
                        children = new Hashtable<>();
                    }
                    VetoableChangeSupport vcs = new VetoableChangeSupport(this.source);
                    vcs.map.set(null, entry.getValue());
                    children.put(property, vcs);
                }
            }
        }
        ObjectOutputStream.PutField fields = s.putFields();
        fields.put("children", children);
        fields.put("source", this.source);
        fields.put("vetoableChangeSupportSerializedDataVersion", 2);
        s.writeFields();

        if (listeners != null) {
            for (VetoableChangeListener l : listeners) {
                if (l instanceof Serializable) {
                    s.writeObject(l);
                }
            }
        }
        s.writeObject(null);
    }

    /**
     * Reads the {@code ObjectInputStream}.
     *
     * @param  s the {@code ObjectInputStream} to read
     * @throws ClassNotFoundException if the class of a serialized object could
     *         not be found
     * @throws IOException if an I/O error occurs
     */
    @Serial
    private void readObject(ObjectInputStream s) throws ClassNotFoundException, IOException {
        this.map = new VetoableChangeListenerMap();

        ObjectInputStream.GetField fields = s.readFields();

        @SuppressWarnings("unchecked")
        Hashtable<String, VetoableChangeSupport> children = (Hashtable<String, VetoableChangeSupport>)fields.get("children", null);
        this.source = fields.get("source", null);
        fields.get("vetoableChangeSupportSerializedDataVersion", 2);

        Object listenerOrNull;
        while (null != (listenerOrNull = s.readObject())) {
            this.map.add(null, (VetoableChangeListener)listenerOrNull);
        }
        if (children != null) {
            for (Entry<String, VetoableChangeSupport> entry : children.entrySet()) {
                for (VetoableChangeListener listener : entry.getValue().getVetoableChangeListeners()) {
                    this.map.add(entry.getKey(), listener);
                }
            }
        }
    }

    /**
     * The object to be provided as the "source" for any generated events.
     */
    private Object source;

    /**
     * @serialField children Hashtable
     *              The list of {@code PropertyChangeListeners}
     * @serialField source Object
     *              The object to be provided as the "source" for any generated
     *              events
     * @serialField vetoableChangeSupportSerializedDataVersion int
     *              The version
     */
    @Serial
    private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("children", Hashtable.class),
            new ObjectStreamField("source", Object.class),
            new ObjectStreamField("vetoableChangeSupportSerializedDataVersion", Integer.TYPE)
    };

    /**
     * Use serialVersionUID from JDK 1.1 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -5090210921595982017L;

    /**
     * This is a {@link ChangeListenerMap ChangeListenerMap} implementation
     * that works with {@link VetoableChangeListener VetoableChangeListener} objects.
     */
    private static final class VetoableChangeListenerMap extends ChangeListenerMap<VetoableChangeListener> {
        private static final VetoableChangeListener[] EMPTY = {};

        /**
         * Creates an array of {@link VetoableChangeListener VetoableChangeListener} objects.
         * This method uses the same instance of the empty array
         * when {@code length} equals {@code 0}.
         *
         * @param length  the array length
         * @return        an array with specified length
         */
        @Override
        protected VetoableChangeListener[] newArray(int length) {
            return (0 < length)
                    ? new VetoableChangeListener[length]
                    : EMPTY;
        }

        /**
         * Creates a {@link VetoableChangeListenerProxy VetoableChangeListenerProxy}
         * object for the specified property.
         *
         * @param name      the name of the property to listen on
         * @param listener  the listener to process events
         * @return          a {@code VetoableChangeListenerProxy} object
         */
        @Override
        protected VetoableChangeListener newProxy(String name, VetoableChangeListener listener) {
            return new VetoableChangeListenerProxy(name, listener);
        }

        /**
         * {@inheritDoc}
         */
        public VetoableChangeListener extract(VetoableChangeListener listener) {
            while (listener instanceof VetoableChangeListenerProxy) {
                listener = ((VetoableChangeListenerProxy) listener).getListener();
            }
            return listener;
        }
    }
}
