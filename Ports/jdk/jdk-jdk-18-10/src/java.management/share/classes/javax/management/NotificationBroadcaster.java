/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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


package javax.management;

import java.util.concurrent.CopyOnWriteArrayList;  // for Javadoc

/**
 * <p>Interface implemented by an MBean that emits Notifications. It
 * allows a listener to be registered with the MBean as a notification
 * listener.</p>
 *
 * <h2>Notification dispatch</h2>
 *
 * <p>When an MBean emits a notification, it considers each listener that has been
 * added with {@link #addNotificationListener addNotificationListener} and not
 * subsequently removed with {@link #removeNotificationListener removeNotificationListener}.
 * If a filter was provided with that listener, and if the filter's
 * {@link NotificationFilter#isNotificationEnabled isNotificationEnabled} method returns
 * false, the listener is ignored.  Otherwise, the listener's
 * {@link NotificationListener#handleNotification handleNotification} method is called with
 * the notification, as well as the handback object that was provided to
 * {@code addNotificationListener}.</p>
 *
 * <p>If the same listener is added more than once, it is considered as many times as it was
 * added.  It is often useful to add the same listener with different filters or handback
 * objects.</p>
 *
 * <p>Implementations of this interface can differ regarding the thread in which the methods
 * of filters and listeners are called.</p>
 *
 * <p>If the method call of a filter or listener throws an {@link Exception}, then that
 * exception should not prevent other listeners from being invoked.  However, if the method
 * call throws an {@link Error}, then it is recommended that processing of the notification
 * stop at that point, and if it is possible to propagate the {@code Error} to the sender of
 * the notification, this should be done.</p>
 *
 * <p>New code should use the {@link NotificationEmitter} interface
 * instead.</p>
 *
 * <p>Implementations of this interface and of {@code NotificationEmitter}
 * should be careful about synchronization.  In particular, it is not a good
 * idea for an implementation to hold any locks while it is calling a
 * listener.  To deal with the possibility that the list of listeners might
 * change while a notification is being dispatched, a good strategy is to
 * use a {@link CopyOnWriteArrayList} for this list.
 *
 * @since 1.5
 */
public interface NotificationBroadcaster {

    /**
     * Adds a listener to this MBean.
     *
     * @param listener The listener object which will handle the
     * notifications emitted by the broadcaster.
     * @param filter The filter object. If filter is null, no
     * filtering will be performed before handling notifications.
     * @param handback An opaque object to be sent back to the
     * listener when a notification is emitted. This object cannot be
     * used by the Notification broadcaster object. It should be
     * resent unchanged with the notification to the listener.
     *
     * @exception IllegalArgumentException Listener parameter is null.
     *
     * @see #removeNotificationListener
     */
    public void addNotificationListener(NotificationListener listener,
                                        NotificationFilter filter,
                                        Object handback)
            throws java.lang.IllegalArgumentException;

    /**
     * Removes a listener from this MBean.  If the listener
     * has been registered with different handback objects or
     * notification filters, all entries corresponding to the listener
     * will be removed.
     *
     * @param listener A listener that was previously added to this
     * MBean.
     *
     * @exception ListenerNotFoundException The listener is not
     * registered with the MBean.
     *
     * @see #addNotificationListener
     * @see NotificationEmitter#removeNotificationListener
     */
    public void removeNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException;

    /**
     * <p>Returns an array indicating, for each notification this
     * MBean may send, the name of the Java class of the notification
     * and the notification type.</p>
     *
     * <p>It is not illegal for the MBean to send notifications not
     * described in this array.  However, some clients of the MBean
     * server may depend on the array being complete for their correct
     * functioning.</p>
     *
     * @return the array of possible notifications.
     */
    public MBeanNotificationInfo[] getNotificationInfo();
}
