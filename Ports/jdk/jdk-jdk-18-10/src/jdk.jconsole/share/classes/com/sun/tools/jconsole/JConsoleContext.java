/*
 * Copyright (c) 2006, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.jconsole;

import javax.management.MBeanServerConnection;
import java.beans.PropertyChangeListener;
import javax.swing.event.SwingPropertyChangeSupport;

/**
 * {@code JConsoleContext} represents a JConsole connection to a target
 * application.
 * <p>
 * {@code JConsoleContext} notifies any {@code PropertyChangeListeners}
 * about the {@linkplain #CONNECTION_STATE_PROPERTY <i>ConnectionState</i>}
 * property change to {@link ConnectionState#CONNECTED CONNECTED} and
 * {@link ConnectionState#DISCONNECTED DISCONNECTED}.
 * The {@code JConsoleContext} instance will be the source for
 * any generated events.
 *
 * @since 1.6
 */
public interface JConsoleContext {
    /**
     * The {@link ConnectionState ConnectionState} bound property name.
     */
    public static String CONNECTION_STATE_PROPERTY = "connectionState";

    /**
     * Values for the {@linkplain #CONNECTION_STATE_PROPERTY
     * <i>ConnectionState</i>} bound property.
     */
    public enum ConnectionState {
        /**
         * The connection has been successfully established.
         */
        CONNECTED,
        /**
         * No connection present.
         */
        DISCONNECTED,
        /**
         * The connection is being attempted.
         */
        CONNECTING
    }

    /**
     * Returns the {@link MBeanServerConnection MBeanServerConnection} for the
     * connection to an application.  The returned
     * {@code MBeanServerConnection} object becomes invalid when
     * the connection state is changed to the
     * {@link ConnectionState#DISCONNECTED DISCONNECTED} state.
     *
     * @return the {@code MBeanServerConnection} for the
     * connection to an application.
     */
    public MBeanServerConnection getMBeanServerConnection();

    /**
     * Returns the current connection state.
     * @return the current connection state.
     */
    public ConnectionState getConnectionState();

    /**
     * Add a {@link java.beans.PropertyChangeListener PropertyChangeListener}
     * to the listener list.
     * The listener is registered for all properties.
     * The same listener object may be added more than once, and will be called
     * as many times as it is added.
     * If {@code listener} is {@code null}, no exception is thrown and
     * no action is taken.
     *
     * @param listener  The {@code PropertyChangeListener} to be added
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Removes a {@link java.beans.PropertyChangeListener PropertyChangeListener}
     * from the listener list. This
     * removes a {@code PropertyChangeListener} that was registered for all
     * properties. If {@code listener} was added more than once to the same
     * event source, it will be notified one less time after being removed. If
     * {@code listener} is {@code null}, or was never added, no exception is
     * thrown and no action is taken.
     *
     * @param listener the {@code PropertyChangeListener} to be removed
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);
}
