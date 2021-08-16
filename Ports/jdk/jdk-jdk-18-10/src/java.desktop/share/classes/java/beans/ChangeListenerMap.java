/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.EventListenerProxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This is an abstract class that provides base functionality
 * for the {@link PropertyChangeSupport PropertyChangeSupport} class
 * and the {@link VetoableChangeSupport VetoableChangeSupport} class.
 *
 * @see PropertyChangeSupport.PropertyChangeListenerMap
 * @see VetoableChangeSupport.VetoableChangeListenerMap
 *
 * @author Sergey A. Malenkov
 */
abstract class ChangeListenerMap<L extends EventListener> {
    private Map<String, L[]> map;

    /**
     * Creates an array of listeners.
     * This method can be optimized by using
     * the same instance of the empty array
     * when {@code length} is equal to {@code 0}.
     *
     * @param length  the array length
     * @return        an array with specified length
     */
    protected abstract L[] newArray(int length);

    /**
     * Creates a proxy listener for the specified property.
     *
     * @param name      the name of the property to listen on
     * @param listener  the listener to process events
     * @return          a proxy listener
     */
    protected abstract L newProxy(String name, L listener);

    /**
     * Adds a listener to the list of listeners for the specified property.
     * This listener is called as many times as it was added.
     *
     * @param name      the name of the property to listen on
     * @param listener  the listener to process events
     */
    public final synchronized void add(String name, L listener) {
        if (this.map == null) {
            this.map = new HashMap<>();
        }
        L[] array = this.map.get(name);
        int size = (array != null)
                ? array.length
                : 0;

        L[] clone = newArray(size + 1);
        clone[size] = listener;
        if (array != null) {
            System.arraycopy(array, 0, clone, 0, size);
        }
        this.map.put(name, clone);
    }

    /**
     * Removes a listener from the list of listeners for the specified property.
     * If the listener was added more than once to the same event source,
     * this listener will be notified one less time after being removed.
     *
     * @param name      the name of the property to listen on
     * @param listener  the listener to process events
     */
    public final synchronized void remove(String name, L listener) {
        if (this.map != null) {
            L[] array = this.map.get(name);
            if (array != null) {
                for (int i = 0; i < array.length; i++) {
                    if (listener.equals(array[i])) {
                        int size = array.length - 1;
                        if (size > 0) {
                            L[] clone = newArray(size);
                            System.arraycopy(array, 0, clone, 0, i);
                            System.arraycopy(array, i + 1, clone, i, size - i);
                            this.map.put(name, clone);
                        }
                        else {
                            this.map.remove(name);
                            if (this.map.isEmpty()) {
                                this.map = null;
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * Returns the list of listeners for the specified property.
     *
     * @param name  the name of the property
     * @return      the corresponding list of listeners
     */
    public final synchronized L[] get(String name) {
        return (this.map != null)
                ? this.map.get(name)
                : null;
    }

    /**
     * Sets new list of listeners for the specified property.
     *
     * @param name       the name of the property
     * @param listeners  new list of listeners
     */
    public final void set(String name, L[] listeners) {
        if (listeners != null) {
            if (this.map == null) {
                this.map = new HashMap<>();
            }
            this.map.put(name, listeners);
        }
        else if (this.map != null) {
            this.map.remove(name);
            if (this.map.isEmpty()) {
                this.map = null;
            }
        }
    }

    /**
     * Returns all listeners in the map.
     *
     * @return an array of all listeners
     */
    public final synchronized L[] getListeners() {
        if (this.map == null) {
            return newArray(0);
        }
        List<L> list = new ArrayList<>();

        L[] listeners = this.map.get(null);
        if (listeners != null) {
            for (L listener : listeners) {
                list.add(listener);
            }
        }
        for (Entry<String, L[]> entry : this.map.entrySet()) {
            String name = entry.getKey();
            if (name != null) {
                for (L listener : entry.getValue()) {
                    list.add(newProxy(name, listener));
                }
            }
        }
        return list.toArray(newArray(list.size()));
    }

    /**
     * Returns listeners that have been associated with the named property.
     *
     * @param name  the name of the property
     * @return an array of listeners for the named property
     */
    public final L[] getListeners(String name) {
        if (name != null) {
            L[] listeners = get(name);
            if (listeners != null) {
                return listeners.clone();
            }
        }
        return newArray(0);
    }

    /**
     * Indicates whether the map contains
     * at least one listener to be notified.
     *
     * @param name  the name of the property
     * @return      {@code true} if at least one listener exists or
     *              {@code false} otherwise
     */
    public final synchronized boolean hasListeners(String name) {
        if (this.map == null) {
            return false;
        }
        L[] array = this.map.get(null);
        return (array != null) || ((name != null) && (null != this.map.get(name)));
    }

    /**
     * Returns a set of entries from the map.
     * Each entry is a pair consisted of the property name
     * and the corresponding list of listeners.
     *
     * @return a set of entries from the map
     */
    public final Set<Entry<String, L[]>> getEntries() {
        return (this.map != null)
                ? this.map.entrySet()
                : Collections.<Entry<String, L[]>>emptySet();
    }

    /**
     * Extracts a real listener from the proxy listener.
     * It is necessary because default proxy class is not serializable.
     *
     * @return a real listener
     */
    public abstract L extract(L listener);
}
