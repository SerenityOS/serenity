/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jmx.remote.security;

import javax.management.Notification;
import javax.management.ObjectName;
import javax.security.auth.Subject;

/**
 * <p>This interface allows to control remote access to the
 * {@code addNotificationListener} and {@code removeNotificationListener}
 * methods when the notification listener parameter is of type
 * {@code NotificationListener} and also allows to control remote access
 * to the notifications being forwarded to the interested remote listeners.</p>
 *
 * <p>An implementation of this interface can be supplied to a
 * {@code JMXConnectorServer} in the environment map through the
 * {@code com.sun.jmx.remote.notification.access.controller}
 * environment map property.</p>
 *
 * @since 1.6
 */
public interface NotificationAccessController {

    /**
     * This method is called when a remote
     * {@link javax.management.remote.JMXConnector} invokes the method
     * {@link javax.management.MBeanServerConnection#addNotificationListener(ObjectName,NotificationListener,NotificationFilter,Object)}.
     *
     * @param connectionId the {@code connectionId} of the remote client
     * adding the listener.
     * @param name the name of the MBean where the listener is to be added.
     * @param subject the authenticated subject representing the remote client.
     *
     * @throws SecurityException if the remote client with the supplied
     * authenticated subject does not have the rights to add a listener
     * to the supplied MBean.
     */
    public void addNotificationListener(String connectionId,
                                        ObjectName name,
                                        Subject subject)
        throws SecurityException;

    /**
     * This method is called when a remote
     * {@link javax.management.remote.JMXConnector} invokes the method
     * {@link javax.management.MBeanServerConnection#removeNotificationListener(ObjectName,NotificationListener)}
     * or the method
     * {@link javax.management.MBeanServerConnection#removeNotificationListener(ObjectName,NotificationListener,NotificationFilter,Object)}.
     *
     * @param connectionId the {@code connectionId} of the remote client
     * removing the listener.
     * @param name the name of the MBean where the listener is to be removed.
     * @param subject the authenticated subject representing the remote client.
     *
     * @throws SecurityException if the remote client with the supplied
     * authenticated subject does not have the rights to remove a listener
     * from the supplied MBean.
     */
    public void removeNotificationListener(String connectionId,
                                           ObjectName name,
                                           Subject subject)
        throws SecurityException;

    /**
     * This method is called before the
     * {@link javax.management.remote.JMXConnectorServer}
     * forwards the notification to the interested remote
     * listener represented by the authenticated subject.
     *
     * @param connectionId the {@code connectionId} of the remote client
     * receiving the notification.
     * @param name the name of the MBean forwarding the notification.
     * @param notification the notification to be forwarded to the interested
     * remote listener.
     * @param subject the authenticated subject representing the remote client.
     *
     * @throws SecurityException if the remote client with
     * the supplied authenticated subject does not have the
     * rights to receive the notification.
     */
    public void fetchNotification(String connectionId,
                                  ObjectName name,
                                  Notification notification,
                                  Subject subject)
        throws SecurityException;
}
