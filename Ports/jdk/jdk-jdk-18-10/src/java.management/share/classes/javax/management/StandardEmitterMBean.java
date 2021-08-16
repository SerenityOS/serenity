/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * <p>An MBean whose management interface is determined by reflection
 * on a Java interface, and that emits notifications.</p>
 *
 * <p>The following example shows how to use the public constructor
 * {@link #StandardEmitterMBean(Object, Class, NotificationEmitter)
 * StandardEmitterMBean(implementation, mbeanInterface, emitter)} to
 * create an MBean emitting notifications with any
 * implementation class name <i>Impl</i>, with a management
 * interface defined (as for current Standard MBeans) by any interface
 * <i>Intf</i>, and with any implementation of the interface
 * {@link NotificationEmitter}. The example uses the class
 * {@link NotificationBroadcasterSupport} as an implementation
 * of the interface {@link NotificationEmitter}.</p>
 *
 *     <pre>
 *     MBeanServer mbs;
 *     ...
 *     final String[] types = new String[] {"sun.disc.space","sun.disc.alarm"};
 *     final MBeanNotificationInfo info = new MBeanNotificationInfo(
 *                                          types,
 *                                          Notification.class.getName(),
 *                                          "Notification about disc info.");
 *     final NotificationEmitter emitter =
 *                    new NotificationBroadcasterSupport(info);
 *
 *     final Intf impl = new Impl(...);
 *     final Object mbean = new StandardEmitterMBean(
 *                                     impl, Intf.class, emitter);
 *     mbs.registerMBean(mbean, objectName);
 *     </pre>
 *
 * @see StandardMBean
 *
 * @since 1.6
 */
public class StandardEmitterMBean extends StandardMBean
        implements NotificationEmitter {

    private static final MBeanNotificationInfo[] NO_NOTIFICATION_INFO =
        new MBeanNotificationInfo[0];

    private final NotificationEmitter emitter;
    private final MBeanNotificationInfo[] notificationInfo;

    /**
     * <p>Make an MBean whose management interface is specified by
     * {@code mbeanInterface}, with the given implementation and
     * where notifications are handled by the given {@code NotificationEmitter}.
     * The resultant MBean implements the {@code NotificationEmitter} interface
     * by forwarding its methods to {@code emitter}.  It is legal and useful
     * for {@code implementation} and {@code emitter} to be the same object.</p>
     *
     * <p>If {@code emitter} is an instance of {@code
     * NotificationBroadcasterSupport} then the MBean's {@link #sendNotification
     * sendNotification} method will call {@code emitter.}{@link
     * NotificationBroadcasterSupport#sendNotification sendNotification}.</p>
     *
     * <p>The array returned by {@link #getNotificationInfo()} on the
     * new MBean is a copy of the array returned by
     * {@code emitter.}{@link NotificationBroadcaster#getNotificationInfo
     * getNotificationInfo()} at the time of construction.  If the array
     * returned by {@code emitter.getNotificationInfo()} later changes,
     * that will have no effect on this object's
     * {@code getNotificationInfo()}.</p>
     *
     * @param <T> the implementation type of the MBean
     * @param implementation the implementation of the MBean interface.
     * @param mbeanInterface a Standard MBean interface.
     * @param emitter the object that will handle notifications.
     *
     * @throws IllegalArgumentException if the {@code mbeanInterface}
     *    does not follow JMX design patterns for Management Interfaces, or
     *    if the given {@code implementation} does not implement the
     *    specified interface, or if {@code emitter} is null.
     */
    public <T> StandardEmitterMBean(T implementation, Class<T> mbeanInterface,
                                    NotificationEmitter emitter) {
        this(implementation, mbeanInterface, false, emitter);
    }

    /**
     * <p>Make an MBean whose management interface is specified by
     * {@code mbeanInterface}, with the given implementation and where
     * notifications are handled by the given {@code
     * NotificationEmitter}.  This constructor can be used to make
     * either Standard MBeans or MXBeans.  The resultant MBean
     * implements the {@code NotificationEmitter} interface by
     * forwarding its methods to {@code emitter}.  It is legal and
     * useful for {@code implementation} and {@code emitter} to be the
     * same object.</p>
     *
     * <p>If {@code emitter} is an instance of {@code
     * NotificationBroadcasterSupport} then the MBean's {@link #sendNotification
     * sendNotification} method will call {@code emitter.}{@link
     * NotificationBroadcasterSupport#sendNotification sendNotification}.</p>
     *
     * <p>The array returned by {@link #getNotificationInfo()} on the
     * new MBean is a copy of the array returned by
     * {@code emitter.}{@link NotificationBroadcaster#getNotificationInfo
     * getNotificationInfo()} at the time of construction.  If the array
     * returned by {@code emitter.getNotificationInfo()} later changes,
     * that will have no effect on this object's
     * {@code getNotificationInfo()}.</p>
     *
     * @param <T> the implementation type of the MBean
     * @param implementation the implementation of the MBean interface.
     * @param mbeanInterface a Standard MBean interface.
     * @param isMXBean If true, the {@code mbeanInterface} parameter
     * names an MXBean interface and the resultant MBean is an MXBean.
     * @param emitter the object that will handle notifications.
     *
     * @throws IllegalArgumentException if the {@code mbeanInterface}
     *    does not follow JMX design patterns for Management Interfaces, or
     *    if the given {@code implementation} does not implement the
     *    specified interface, or if {@code emitter} is null.
     */
    public <T> StandardEmitterMBean(T implementation, Class<T> mbeanInterface,
                                    boolean isMXBean,
                                    NotificationEmitter emitter) {
        super(implementation, mbeanInterface, isMXBean);
        if (emitter == null)
            throw new IllegalArgumentException("Null emitter");
        this.emitter = emitter;
        MBeanNotificationInfo[] infos = emitter.getNotificationInfo();
        if (infos == null || infos.length == 0) {
            this.notificationInfo = NO_NOTIFICATION_INFO;
        } else {
            this.notificationInfo = infos.clone();
        }
    }

    /**
     * <p>Make an MBean whose management interface is specified by
     * {@code mbeanInterface}, and
     * where notifications are handled by the given {@code NotificationEmitter}.
     * The resultant MBean implements the {@code NotificationEmitter} interface
     * by forwarding its methods to {@code emitter}.</p>
     *
     * <p>If {@code emitter} is an instance of {@code
     * NotificationBroadcasterSupport} then the MBean's {@link #sendNotification
     * sendNotification} method will call {@code emitter.}{@link
     * NotificationBroadcasterSupport#sendNotification sendNotification}.</p>
     *
     * <p>The array returned by {@link #getNotificationInfo()} on the
     * new MBean is a copy of the array returned by
     * {@code emitter.}{@link NotificationBroadcaster#getNotificationInfo
     * getNotificationInfo()} at the time of construction.  If the array
     * returned by {@code emitter.getNotificationInfo()} later changes,
     * that will have no effect on this object's
     * {@code getNotificationInfo()}.</p>
     *
     * <p>This constructor must be called from a subclass that implements
     * the given {@code mbeanInterface}.</p>
     *
     * @param mbeanInterface a StandardMBean interface.
     * @param emitter the object that will handle notifications.
     *
     * @throws IllegalArgumentException if the {@code mbeanInterface}
     *    does not follow JMX design patterns for Management Interfaces, or
     *    if {@code this} does not implement the specified interface, or
     *    if {@code emitter} is null.
     */
    protected StandardEmitterMBean(Class<?> mbeanInterface,
                                   NotificationEmitter emitter) {
        this(mbeanInterface, false, emitter);
    }

    /**
     * <p>Make an MBean whose management interface is specified by
     * {@code mbeanInterface}, and where notifications are handled by
     * the given {@code NotificationEmitter}.  This constructor can be
     * used to make either Standard MBeans or MXBeans.  The resultant
     * MBean implements the {@code NotificationEmitter} interface by
     * forwarding its methods to {@code emitter}.</p>
     *
     * <p>If {@code emitter} is an instance of {@code
     * NotificationBroadcasterSupport} then the MBean's {@link #sendNotification
     * sendNotification} method will call {@code emitter.}{@link
     * NotificationBroadcasterSupport#sendNotification sendNotification}.</p>
     *
     * <p>The array returned by {@link #getNotificationInfo()} on the
     * new MBean is a copy of the array returned by
     * {@code emitter.}{@link NotificationBroadcaster#getNotificationInfo
     * getNotificationInfo()} at the time of construction.  If the array
     * returned by {@code emitter.getNotificationInfo()} later changes,
     * that will have no effect on this object's
     * {@code getNotificationInfo()}.</p>
     *
     * <p>This constructor must be called from a subclass that implements
     * the given {@code mbeanInterface}.</p>
     *
     * @param mbeanInterface a StandardMBean interface.
     * @param isMXBean If true, the {@code mbeanInterface} parameter
     * names an MXBean interface and the resultant MBean is an MXBean.
     * @param emitter the object that will handle notifications.
     *
     * @throws IllegalArgumentException if the {@code mbeanInterface}
     *    does not follow JMX design patterns for Management Interfaces, or
     *    if {@code this} does not implement the specified interface, or
     *    if {@code emitter} is null.
     */
    protected StandardEmitterMBean(Class<?> mbeanInterface, boolean isMXBean,
                                   NotificationEmitter emitter) {
        super(mbeanInterface, isMXBean);
        if (emitter == null)
            throw new IllegalArgumentException("Null emitter");
        this.emitter = emitter;
        MBeanNotificationInfo[] infos = emitter.getNotificationInfo();
        if (infos == null || infos.length == 0) {
            this.notificationInfo = NO_NOTIFICATION_INFO;
        } else {
            this.notificationInfo = infos.clone();
        }
    }

    public void removeNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException {
        emitter.removeNotificationListener(listener);
    }

    public void removeNotificationListener(NotificationListener listener,
                                           NotificationFilter filter,
                                           Object handback)
            throws ListenerNotFoundException {
        emitter.removeNotificationListener(listener, filter, handback);
    }

    public void addNotificationListener(NotificationListener listener,
                                        NotificationFilter filter,
                                        Object handback) {
        emitter.addNotificationListener(listener, filter, handback);
    }

    public MBeanNotificationInfo[] getNotificationInfo() {
        // this getter might get called from the super constructor
        // when the notificationInfo has not been properly set yet
        if (notificationInfo == null) {
            return NO_NOTIFICATION_INFO;
        }
        if (notificationInfo.length == 0) {
            return notificationInfo;
        } else {
            return notificationInfo.clone();
        }
    }

    /**
     * <p>Sends a notification.</p>
     *
     * <p>If the {@code emitter} parameter to the constructor was an
     * instance of {@code NotificationBroadcasterSupport} then this
     * method will call {@code emitter.}{@link
     * NotificationBroadcasterSupport#sendNotification
     * sendNotification}.</p>
     *
     * @param n the notification to send.
     *
     * @throws ClassCastException if the {@code emitter} parameter to the
     * constructor was not a {@code NotificationBroadcasterSupport}.
     */
    public void sendNotification(Notification n) {
        if (emitter instanceof NotificationBroadcasterSupport)
            ((NotificationBroadcasterSupport) emitter).sendNotification(n);
        else {
            final String msg =
                "Cannot sendNotification when emitter is not an " +
                "instance of NotificationBroadcasterSupport: " +
                emitter.getClass().getName();
            throw new ClassCastException(msg);
        }
    }

    /**
     * <p>Get the MBeanNotificationInfo[] that will be used in the
     * MBeanInfo returned by this MBean.</p>
     *
     * <p>The default implementation of this method returns
     * {@link #getNotificationInfo()}.</p>
     *
     * @param info The default MBeanInfo derived by reflection.
     * @return the MBeanNotificationInfo[] for the new MBeanInfo.
     */
    @Override
    MBeanNotificationInfo[] getNotifications(MBeanInfo info) {
        return getNotificationInfo();
    }
}
