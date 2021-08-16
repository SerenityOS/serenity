/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.java.accessibility.util;

import java.util.*;
import java.beans.*;
import java.awt.*;
import java.awt.event.*;
import javax.accessibility.*;

/**
 * <P>{@code AccessibilityEventMonitor} implements a PropertyChange listener
 * on every UI object that implements interface {@code Accessible} in the Java
 * Virtual Machine.  The events captured by these listeners are made available
 * through listeners supported by {@code AccessibilityEventMonitor}.
 * With this, all the individual events on each of the UI object
 * instances are funneled into one set of PropertyChange listeners.
 * <p>This class depends upon {@link EventQueueMonitor}, which provides the base
 * level support for capturing the top-level containers as they are created.
 *
 */

public class AccessibilityEventMonitor {

    /**
     * Constructs an {@code AccessibilityEventMonitor}.
     */
    public AccessibilityEventMonitor() {}

    // listeners
    /**
     * The current list of registered {@link java.beans.PropertyChangeListener
     * PropertyChangeListener} classes.
     *
     * @see #addPropertyChangeListener
     * @see #removePropertyChangeListener
     */
    static protected final AccessibilityListenerList listenerList =
        new AccessibilityListenerList();


    /**
     * The actual listener that is installed on the component instances.
     * This listener calls the other registered listeners when an event
     * occurs.  By doing things this way, the actual number of listeners
     * installed on a component instance is drastically reduced.
     */
    static private final AccessibilityEventListener accessibilityListener =
        new AccessibilityEventListener();

    /**
     * Adds the specified listener to receive all PropertyChange events on
     * each UI object instance in the Java Virtual Machine as they occur.
     * <P>Note: This listener is automatically added to all component
     * instances created after this method is called.  In addition, it
     * is only added to UI object instances that support this listener type.
     *
     * @param l the listener to add
     *
     * @see #removePropertyChangeListener
     */
    static public void addPropertyChangeListener(PropertyChangeListener l) {
        if (listenerList.getListenerCount(PropertyChangeListener.class) == 0) {
            accessibilityListener.installListeners();
        }
        listenerList.add(PropertyChangeListener.class, l);
    }

    /**
     * Removes the specified listener so it no longer receives PropertyChange
     * events when they occur.
     * @see #addPropertyChangeListener
     * @param l the listener to remove
     */
    static public void removePropertyChangeListener(PropertyChangeListener l) {
        listenerList.remove(PropertyChangeListener.class, l);
        if (listenerList.getListenerCount(PropertyChangeListener.class) == 0) {
            accessibilityListener.removeListeners();
        }
    }


    /**
     * AccessibilityEventListener is the class that does all the work for
     * AccessibilityEventMonitor.  It is not intended for use by any other
     * class except AccessibilityEventMonitor.
     *
     */

    static class AccessibilityEventListener implements TopLevelWindowListener,
                PropertyChangeListener {

        /**
         * Create a new instance of this class and install it on each component
         * instance in the virtual machine that supports any of the currently
         * registered listeners in AccessibilityEventMonitor.  Also registers
         * itself as a TopLevelWindowListener with EventQueueMonitor so it can
         * automatically add new listeners to new components.
         * @see EventQueueMonitor
         * @see AccessibilityEventMonitor
         */
        public AccessibilityEventListener() {
            EventQueueMonitor.addTopLevelWindowListener(this);
        }

        /**
         * Installs PropertyChange listeners on all Accessible objects based
         * upon the current topLevelWindows cached by EventQueueMonitor.
         * @see EventQueueMonitor
         * @see AWTEventMonitor
         */
        protected void installListeners() {
            Window[] topLevelWindows = EventQueueMonitor.getTopLevelWindows();
            if (topLevelWindows != null) {
                for (int i = 0; i < topLevelWindows.length; i++) {
                    if (topLevelWindows[i] instanceof Accessible) {
                        installListeners((Accessible) topLevelWindows[i]);
                    }
                }
            }
        }

        /**
         * Installs PropertyChange listeners to the Accessible object, and it's
         * children (so long as the object isn't of TRANSIENT state).
         * @param a the Accessible object to add listeners to
         */
        protected void installListeners(Accessible a) {
            installListeners(a.getAccessibleContext());
        }

        /**
         * Installs PropertyChange listeners to the AccessibleContext object,
         * and it's * children (so long as the object isn't of TRANSIENT state).
         * @param a the Accessible object to add listeners to
         */
        private void installListeners(AccessibleContext ac) {

            if (ac != null) {
                AccessibleStateSet states = ac.getAccessibleStateSet();
                if (!states.contains(AccessibleState.TRANSIENT)) {
                    ac.addPropertyChangeListener(this);
                    /*
                     * Don't add listeners to transient children. Components
                     * with transient children should return an AccessibleStateSet
                     * containing AccessibleState.MANAGES_DESCENDANTS. Components
                     * may not explicitly return the MANAGES_DESCENDANTS state.
                     * In this case, don't add listeners to the children of
                     * lists, tables and trees.
                     */
                    AccessibleStateSet set = ac.getAccessibleStateSet();
                    if (set.contains(_AccessibleState.MANAGES_DESCENDANTS)) {
                        return;
                    }
                    AccessibleRole role = ac.getAccessibleRole();
                    if (role == AccessibleRole.LIST ||
                        role == AccessibleRole.TREE) {
                        return;
                    }
                    if (role == AccessibleRole.TABLE) {
                        // handle Oracle tables containing tables
                        Accessible child = ac.getAccessibleChild(0);
                        if (child != null) {
                            AccessibleContext ac2 = child.getAccessibleContext();
                            if (ac2 != null) {
                                role = ac2.getAccessibleRole();
                                if (role != null && role != AccessibleRole.TABLE) {
                                    return;
                                }
                            }
                        }
                    }
                    int count = ac.getAccessibleChildrenCount();
                    for (int i = 0; i < count; i++) {
                        Accessible child = ac.getAccessibleChild(i);
                        if (child != null) {
                            installListeners(child);
                        }
                    }
                }
            }
        }

        /**
         * Removes PropertyChange listeners on all Accessible objects based
         * upon the topLevelWindows cached by EventQueueMonitor.
         * @param eventID the event ID
         * @see EventID
         */
        protected void removeListeners() {
            Window[] topLevelWindows = EventQueueMonitor.getTopLevelWindows();
            if (topLevelWindows != null) {
                for (int i = 0; i < topLevelWindows.length; i++) {
                    if (topLevelWindows[i] instanceof Accessible) {
                        removeListeners((Accessible) topLevelWindows[i]);
                    }
                }
            }
        }

        /**
         * Removes PropertyChange listeners for the given Accessible object,
         * it's children (so long as the object isn't of TRANSIENT state).
         * @param a the Accessible object to remove listeners from
         */
        protected void removeListeners(Accessible a) {
            removeListeners(a.getAccessibleContext());
        }

        /**
         * Removes PropertyChange listeners for the given AccessibleContext
         * object, it's children (so long as the object isn't of TRANSIENT
         * state).
         * @param a the Accessible object to remove listeners from
         */
        private void removeListeners(AccessibleContext ac) {


            if (ac != null) {
                // Listeners are not added to transient components.
                AccessibleStateSet states = ac.getAccessibleStateSet();
                if (!states.contains(AccessibleState.TRANSIENT)) {
                    ac.removePropertyChangeListener(this);
                    /*
                     * Listeners are not added to transient children. Components
                     * with transient children should return an AccessibleStateSet
                     * containing AccessibleState.MANAGES_DESCENDANTS. Components
                     * may not explicitly return the MANAGES_DESCENDANTS state.
                     * In this case, don't remove listeners from the children of
                     * lists, tables and trees.
                     */
                    if (states.contains(_AccessibleState.MANAGES_DESCENDANTS)) {
                        return;
                    }
                    AccessibleRole role = ac.getAccessibleRole();
                    if (role == AccessibleRole.LIST ||
                        role == AccessibleRole.TABLE ||
                        role == AccessibleRole.TREE) {
                        return;
                    }
                    int count = ac.getAccessibleChildrenCount();
                    for (int i = 0; i < count; i++) {
                        Accessible child = ac.getAccessibleChild(i);
                        if (child != null) {
                            removeListeners(child);
                        }
                    }
                }
            }
        }

        /********************************************************************/
        /*                                                                  */
        /* Listener Interface Methods                                       */
        /*                                                                  */
        /********************************************************************/

        /* TopLevelWindow Methods ***************************************/

        /**
         * Called when top level window is created.
         * @see EventQueueMonitor
         * @see EventQueueMonitor#addTopLevelWindowListener
         */
        public void topLevelWindowCreated(Window w) {
            if (w instanceof Accessible) {
                installListeners((Accessible) w);
            }
        }

        /**
         * Called when top level window is destroyed.
         * @see EventQueueMonitor
         * @see EventQueueMonitor#addTopLevelWindowListener
         */
        public void topLevelWindowDestroyed(Window w) {
            if (w instanceof Accessible) {
                removeListeners((Accessible) w);
            }
        }


        /* PropertyChangeListener Methods **************************************/

        public void propertyChange(PropertyChangeEvent e) {
            // propogate the event
            Object[] listeners =
                    AccessibilityEventMonitor.listenerList.getListenerList();
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==PropertyChangeListener.class) {
                    ((PropertyChangeListener)listeners[i+1]).propertyChange(e);
                }
            }

            // handle childbirth/death
            String name = e.getPropertyName();
            if (name.compareTo(AccessibleContext.ACCESSIBLE_CHILD_PROPERTY) == 0) {
                Object oldValue = e.getOldValue();
                Object newValue = e.getNewValue();

                if ((oldValue == null) ^ (newValue == null)) { // one null, not both
                    if (oldValue != null) {
                        // this Accessible is a child that's going away
                        if (oldValue instanceof Accessible) {
                            Accessible a = (Accessible) oldValue;
                            removeListeners(a.getAccessibleContext());
                        } else if (oldValue instanceof AccessibleContext) {
                            removeListeners((AccessibleContext) oldValue);
                        }
                    } else if (newValue != null) {
                        // this Accessible is a child was just born
                        if (newValue instanceof Accessible) {
                            Accessible a = (Accessible) newValue;
                            installListeners(a.getAccessibleContext());
                        } else if (newValue instanceof AccessibleContext) {
                            installListeners((AccessibleContext) newValue);
                        }
                    }
                } else {
                    System.out.println("ERROR in usage of PropertyChangeEvents for: " + e.toString());
                }
            }
        }
    }
}

/*
 * workaround for no public AccessibleState constructor
 */
class _AccessibleState extends AccessibleState {
    /**
     * Indicates this object is responsible for managing its
     * subcomponents.  This is typically used for trees and tables
     * that have a large number of subcomponents and where the
     * objects are created only when needed and otherwise remain virtual.
     * The application should not manage the subcomponents directly.
     */
    public static final _AccessibleState MANAGES_DESCENDANTS
        = new _AccessibleState ("managesDescendants");

    /**
     * Creates a new AccessibleState using the given locale independent key.
     * This should not be a public method.  Instead, it is used to create
     * the constants in this file to make it a strongly typed enumeration.
     * Subclasses of this class should enforce similar policy.
     * <p>
     * The key String should be a locale independent key for the state.
     * It is not intended to be used as the actual String to display
     * to the user.  To get the localized string, use toDisplayString.
     *
     * @param key the locale independent name of the state.
     * @see AccessibleBundle#toDisplayString
     */
    protected _AccessibleState(String key) {
        super(key);
    }
}
