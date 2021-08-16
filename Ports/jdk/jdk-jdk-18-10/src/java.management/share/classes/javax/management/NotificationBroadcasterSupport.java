/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

import com.sun.jmx.remote.util.ClassLogger;

/**
 * <p>Provides an implementation of {@link
 * javax.management.NotificationEmitter NotificationEmitter}
 * interface.  This can be used as the super class of an MBean that
 * sends notifications.</p>
 *
 * <p>By default, the notification dispatch model is synchronous.
 * That is, when a thread calls sendNotification, the
 * <code>NotificationListener.handleNotification</code> method of each listener
 * is called within that thread. You can override this default
 * by overriding <code>handleNotification</code> in a subclass, or by passing an
 * Executor to the constructor.</p>
 *
 * <p>If the method call of a filter or listener throws an {@link Exception},
 * then that exception does not prevent other listeners from being invoked.  However,
 * if the method call of a filter or of {@code Executor.execute} or of
 * {@code handleNotification} (when no {@code Excecutor} is specified) throws an
 * {@link Error}, then that {@code Error} is propagated to the caller of
 * {@link #sendNotification sendNotification}.</p>
 *
 * <p>Remote listeners added using the JMX Remote API (see JMXConnector) are not
 * usually called synchronously.  That is, when sendNotification returns, it is
 * not guaranteed that any remote listeners have yet received the notification.</p>
 *
 * @since 1.5
 */
public class NotificationBroadcasterSupport implements NotificationEmitter {
    /**
     * Constructs a NotificationBroadcasterSupport where each listener is invoked by the
     * thread sending the notification. This constructor is equivalent to
     * {@link NotificationBroadcasterSupport#NotificationBroadcasterSupport(Executor,
     * MBeanNotificationInfo[] info) NotificationBroadcasterSupport(null, null)}.
     */
    public NotificationBroadcasterSupport() {
        this(null, (MBeanNotificationInfo[]) null);
    }

    /**
     * Constructs a NotificationBroadcasterSupport where each listener is invoked using
     * the given {@link java.util.concurrent.Executor}. When {@link #sendNotification
     * sendNotification} is called, a listener is selected if it was added with a null
     * {@link NotificationFilter}, or if {@link NotificationFilter#isNotificationEnabled
     * isNotificationEnabled} returns true for the notification being sent. The call to
     * <code>NotificationFilter.isNotificationEnabled</code> takes place in the thread
     * that called <code>sendNotification</code>. Then, for each selected listener,
     * {@link Executor#execute executor.execute} is called with a command
     * that calls the <code>handleNotification</code> method.
     * This constructor is equivalent to
     * {@link NotificationBroadcasterSupport#NotificationBroadcasterSupport(Executor,
     * MBeanNotificationInfo[] info) NotificationBroadcasterSupport(executor, null)}.
     * @param executor an executor used by the method <code>sendNotification</code> to
     * send each notification. If it is null, the thread calling <code>sendNotification</code>
     * will invoke the <code>handleNotification</code> method itself.
     * @since 1.6
     */
    public NotificationBroadcasterSupport(Executor executor) {
        this(executor, (MBeanNotificationInfo[]) null);
    }

    /**
     * <p>Constructs a NotificationBroadcasterSupport with information
     * about the notifications that may be sent.  Each listener is
     * invoked by the thread sending the notification.  This
     * constructor is equivalent to {@link
     * NotificationBroadcasterSupport#NotificationBroadcasterSupport(Executor,
     * MBeanNotificationInfo[] info)
     * NotificationBroadcasterSupport(null, info)}.</p>
     *
     * <p>If the <code>info</code> array is not empty, then it is
     * cloned by the constructor as if by {@code info.clone()}, and
     * each call to {@link #getNotificationInfo()} returns a new
     * clone.</p>
     *
     * @param info an array indicating, for each notification this
     * MBean may send, the name of the Java class of the notification
     * and the notification type.  Can be null, which is equivalent to
     * an empty array.
     *
     * @since 1.6
     */
    public NotificationBroadcasterSupport(MBeanNotificationInfo... info) {
        this(null, info);
    }

    /**
     * <p>Constructs a NotificationBroadcasterSupport with information about the notifications that may be sent,
     * and where each listener is invoked using the given {@link java.util.concurrent.Executor}.</p>
     *
     * <p>When {@link #sendNotification sendNotification} is called, a
     * listener is selected if it was added with a null {@link
     * NotificationFilter}, or if {@link
     * NotificationFilter#isNotificationEnabled isNotificationEnabled}
     * returns true for the notification being sent. The call to
     * <code>NotificationFilter.isNotificationEnabled</code> takes
     * place in the thread that called
     * <code>sendNotification</code>. Then, for each selected
     * listener, {@link Executor#execute executor.execute} is called
     * with a command that calls the <code>handleNotification</code>
     * method.</p>
     *
     * <p>If the <code>info</code> array is not empty, then it is
     * cloned by the constructor as if by {@code info.clone()}, and
     * each call to {@link #getNotificationInfo()} returns a new
     * clone.</p>
     *
     * @param executor an executor used by the method
     * <code>sendNotification</code> to send each notification. If it
     * is null, the thread calling <code>sendNotification</code> will
     * invoke the <code>handleNotification</code> method itself.
     *
     * @param info an array indicating, for each notification this
     * MBean may send, the name of the Java class of the notification
     * and the notification type.  Can be null, which is equivalent to
     * an empty array.
     *
     * @since 1.6
     */
    public NotificationBroadcasterSupport(Executor executor,
                                          MBeanNotificationInfo... info) {
        this.executor = (executor != null) ? executor : defaultExecutor;

        notifInfo = info == null ? NO_NOTIFICATION_INFO : info.clone();
    }

    /**
     * Adds a listener.
     *
     * @param listener The listener to receive notifications.
     * @param filter The filter object. If filter is null, no
     * filtering will be performed before handling notifications.
     * @param handback An opaque object to be sent back to the
     * listener when a notification is emitted. This object cannot be
     * used by the Notification broadcaster object. It should be
     * resent unchanged with the notification to the listener.
     *
     * @exception IllegalArgumentException thrown if the listener is null.
     *
     * @see #removeNotificationListener
     */
    public void addNotificationListener(NotificationListener listener,
                                        NotificationFilter filter,
                                        Object handback) {

        if (listener == null) {
            throw new IllegalArgumentException ("Listener can't be null") ;
        }

        listenerList.add(new ListenerInfo(listener, filter, handback));
    }

    public void removeNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException {

        ListenerInfo wildcard = new WildcardListenerInfo(listener);
        boolean removed =
            listenerList.removeAll(Collections.singleton(wildcard));
        if (!removed)
            throw new ListenerNotFoundException("Listener not registered");
    }

    public void removeNotificationListener(NotificationListener listener,
                                           NotificationFilter filter,
                                           Object handback)
            throws ListenerNotFoundException {

        ListenerInfo li = new ListenerInfo(listener, filter, handback);
        boolean removed = listenerList.remove(li);
        if (!removed) {
            throw new ListenerNotFoundException("Listener not registered " +
                                                "(with this filter and " +
                                                "handback)");
            // or perhaps not registered at all
        }
    }

    public MBeanNotificationInfo[] getNotificationInfo() {
        if (notifInfo.length == 0)
            return notifInfo;
        else
            return notifInfo.clone();
    }


    /**
     * Sends a notification.
     *
     * If an {@code Executor} was specified in the constructor, it will be given one
     * task per selected listener to deliver the notification to that listener.
     *
     * @param notification The notification to send.
     */
    public void sendNotification(Notification notification) {

        if (notification == null) {
            return;
        }

        boolean enabled;

        for (ListenerInfo li : listenerList) {
            try {
                enabled = li.filter == null ||
                    li.filter.isNotificationEnabled(notification);
            } catch (Exception e) {
                if (logger.debugOn()) {
                    logger.debug("sendNotification", e);
                }

                continue;
            }

            if (enabled) {
                executor.execute(new SendNotifJob(notification, li));
            }
        }
    }

    /**
     * <p>This method is called by {@link #sendNotification
     * sendNotification} for each listener in order to send the
     * notification to that listener.  It can be overridden in
     * subclasses to change the behavior of notification delivery,
     * for instance to deliver the notification in a separate
     * thread.</p>
     *
     * <p>The default implementation of this method is equivalent to
     * <pre>
     * listener.handleNotification(notif, handback);
     * </pre>
     *
     * @param listener the listener to which the notification is being
     * delivered.
     * @param notif the notification being delivered to the listener.
     * @param handback the handback object that was supplied when the
     * listener was added.
     *
     */
    protected void handleNotification(NotificationListener listener,
                                      Notification notif, Object handback) {
        listener.handleNotification(notif, handback);
    }

    // private stuff
    private static class ListenerInfo {
        NotificationListener listener;
        NotificationFilter filter;
        Object handback;

        ListenerInfo(NotificationListener listener,
                     NotificationFilter filter,
                     Object handback) {
            this.listener = listener;
            this.filter = filter;
            this.handback = handback;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ListenerInfo))
                return false;
            ListenerInfo li = (ListenerInfo) o;
            if (li instanceof WildcardListenerInfo)
                return (li.listener == listener);
            else
                return (li.listener == listener && li.filter == filter
                        && li.handback == handback);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(listener);
        }
    }

    private static class WildcardListenerInfo extends ListenerInfo {
        WildcardListenerInfo(NotificationListener listener) {
            super(listener, null, null);
        }

        @Override
        public boolean equals(Object o) {
            assert (!(o instanceof WildcardListenerInfo));
            return o.equals(this);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }

    private List<ListenerInfo> listenerList =
        new CopyOnWriteArrayList<ListenerInfo>();

    // since 1.6
    private final Executor executor;
    private final MBeanNotificationInfo[] notifInfo;

    private static final Executor defaultExecutor = new Executor() {
            // DirectExecutor using caller thread
            public void execute(Runnable r) {
                r.run();
            }
        };

    private static final MBeanNotificationInfo[] NO_NOTIFICATION_INFO =
        new MBeanNotificationInfo[0];

    private class SendNotifJob implements Runnable {
        public SendNotifJob(Notification notif, ListenerInfo listenerInfo) {
            this.notif = notif;
            this.listenerInfo = listenerInfo;
        }

        public void run() {
            try {
                handleNotification(listenerInfo.listener,
                                   notif, listenerInfo.handback);
            } catch (Exception e) {
                if (logger.debugOn()) {
                    logger.debug("SendNotifJob-run", e);
                }
            }
        }

        private final Notification notif;
        private final ListenerInfo listenerInfo;
    }

    private static final ClassLogger logger =
        new ClassLogger("javax.management", "NotificationBroadcasterSupport");
}
