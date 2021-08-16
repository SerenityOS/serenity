/*
 * Copyright (c) 2003, 2008, Oracle and/or its affiliates. All rights reserved.
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


package javax.management.remote;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

/**
 * <p>Superclass of every connector server.  A connector server is
 * attached to an MBean server.  It listens for client connection
 * requests and creates a connection for each one.</p>
 *
 * <p>A connector server is associated with an MBean server either by
 * registering it in that MBean server, or by passing the MBean server
 * to its constructor.</p>
 *
 * <p>A connector server is inactive when created.  It only starts
 * listening for client connections when the {@link #start() start}
 * method is called.  A connector server stops listening for client
 * connections when the {@link #stop() stop} method is called or when
 * the connector server is unregistered from its MBean server.</p>
 *
 * <p>Stopping a connector server does not unregister it from its
 * MBean server.  A connector server once stopped cannot be
 * restarted.</p>
 *
 * <p>Each time a client connection is made or broken, a notification
 * of class {@link JMXConnectionNotification} is emitted.</p>
 *
 * @since 1.5
 */
public abstract class JMXConnectorServer
        extends NotificationBroadcasterSupport
        implements JMXConnectorServerMBean, MBeanRegistration, JMXAddressable {

    /**
     * <p>Name of the attribute that specifies the authenticator for a
     * connector server.  The value associated with this attribute, if
     * any, must be an object that implements the interface {@link
     * JMXAuthenticator}.</p>
     */
    public static final String AUTHENTICATOR =
        "jmx.remote.authenticator";

    /**
     * <p>Constructs a connector server that will be registered as an
     * MBean in the MBean server it is attached to.  This constructor
     * is typically called by one of the <code>createMBean</code>
     * methods when creating, within an MBean server, a connector
     * server that makes it available remotely.</p>
     */
    public JMXConnectorServer() {
        this(null);
    }

    /**
     * <p>Constructs a connector server that is attached to the given
     * MBean server.  A connector server that is created in this way
     * can be registered in a different MBean server, or not registered
     * in any MBean server.</p>
     *
     * @param mbeanServer the MBean server that this connector server
     * is attached to.  Null if this connector server will be attached
     * to an MBean server by being registered in it.
     */
    public JMXConnectorServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    /**
     * <p>Returns the MBean server that this connector server is
     * attached to.</p>
     *
     * @return the MBean server that this connector server is attached
     * to, or null if it is not yet attached to an MBean server.
     */
    public synchronized MBeanServer getMBeanServer() {
        return mbeanServer;
    }

    public synchronized void setMBeanServerForwarder(MBeanServerForwarder mbsf)
    {
        if (mbsf == null)
            throw new IllegalArgumentException("Invalid null argument: mbsf");

        if (mbeanServer !=  null) mbsf.setMBeanServer(mbeanServer);
        mbeanServer = mbsf;
    }

    public String[] getConnectionIds() {
        synchronized (connectionIds) {
            return connectionIds.toArray(new String[connectionIds.size()]);
        }
    }

    /**
     * <p>Returns a client stub for this connector server.  A client
     * stub is a serializable object whose {@link
     * JMXConnector#connect(Map) connect} method can be used to make
     * one new connection to this connector server.</p>
     *
     * <p>A given connector need not support the generation of client
     * stubs.  However, the connectors specified by the JMX Remote API do
     * (JMXMP Connector and RMI Connector).</p>
     *
     * <p>The default implementation of this method uses {@link
     * #getAddress} and {@link JMXConnectorFactory} to generate the
     * stub, with code equivalent to the following:</p>
     *
     * <pre>
     * JMXServiceURL addr = {@link #getAddress() getAddress()};
     * return {@link JMXConnectorFactory#newJMXConnector(JMXServiceURL, Map)
     *          JMXConnectorFactory.newJMXConnector(addr, env)};
     * </pre>
     *
     * <p>A connector server for which this is inappropriate must
     * override this method so that it either implements the
     * appropriate logic or throws {@link
     * UnsupportedOperationException}.</p>
     *
     * @param env client connection parameters of the same sort that
     * could be provided to {@link JMXConnector#connect(Map)
     * JMXConnector.connect(Map)}.  Can be null, which is equivalent
     * to an empty map.
     *
     * @return a client stub that can be used to make a new connection
     * to this connector server.
     *
     * @exception UnsupportedOperationException if this connector
     * server does not support the generation of client stubs.
     *
     * @exception IllegalStateException if the JMXConnectorServer is
     * not started (see {@link JMXConnectorServerMBean#isActive()}).
     *
     * @exception IOException if a communications problem means that a
     * stub cannot be created.
     **/
    public JMXConnector toJMXConnector(Map<String,?> env)
        throws IOException
    {
        if (!isActive()) throw new
            IllegalStateException("Connector is not active");
        JMXServiceURL addr = getAddress();
        return JMXConnectorFactory.newJMXConnector(addr, env);
    }

    /**
     * <p>Returns an array indicating the notifications that this MBean
     * sends. The implementation in <code>JMXConnectorServer</code>
     * returns an array with one element, indicating that it can emit
     * notifications of class {@link JMXConnectionNotification} with
     * the types defined in that class.  A subclass that can emit other
     * notifications should return an array that contains this element
     * plus descriptions of the other notifications.</p>
     *
     * @return the array of possible notifications.
     */
    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        final String[] types = {
            JMXConnectionNotification.OPENED,
            JMXConnectionNotification.CLOSED,
            JMXConnectionNotification.FAILED,
        };
        final String className = JMXConnectionNotification.class.getName();
        final String description =
            "A client connection has been opened or closed";
        return new MBeanNotificationInfo[] {
            new MBeanNotificationInfo(types, className, description),
        };
    }

    /**
     * <p>Called by a subclass when a new client connection is opened.
     * Adds <code>connectionId</code> to the list returned by {@link
     * #getConnectionIds()}, then emits a {@link
     * JMXConnectionNotification} with type {@link
     * JMXConnectionNotification#OPENED}.</p>
     *
     * @param connectionId the ID of the new connection.  This must be
     * different from the ID of any connection previously opened by
     * this connector server.
     *
     * @param message the message for the emitted {@link
     * JMXConnectionNotification}.  Can be null.  See {@link
     * Notification#getMessage()}.
     *
     * @param userData the <code>userData</code> for the emitted
     * {@link JMXConnectionNotification}.  Can be null.  See {@link
     * Notification#getUserData()}.
     *
     * @exception NullPointerException if <code>connectionId</code> is
     * null.
     */
    protected void connectionOpened(String connectionId,
                                    String message,
                                    Object userData) {

        if (connectionId == null)
            throw new NullPointerException("Illegal null argument");

        synchronized (connectionIds) {
            connectionIds.add(connectionId);
        }

        sendNotification(JMXConnectionNotification.OPENED, connectionId,
                         message, userData);
    }

    /**
     * <p>Called by a subclass when a client connection is closed
     * normally.  Removes <code>connectionId</code> from the list returned
     * by {@link #getConnectionIds()}, then emits a {@link
     * JMXConnectionNotification} with type {@link
     * JMXConnectionNotification#CLOSED}.</p>
     *
     * @param connectionId the ID of the closed connection.
     *
     * @param message the message for the emitted {@link
     * JMXConnectionNotification}.  Can be null.  See {@link
     * Notification#getMessage()}.
     *
     * @param userData the <code>userData</code> for the emitted
     * {@link JMXConnectionNotification}.  Can be null.  See {@link
     * Notification#getUserData()}.
     *
     * @exception NullPointerException if <code>connectionId</code>
     * is null.
     */
    protected void connectionClosed(String connectionId,
                                    String message,
                                    Object userData) {

        if (connectionId == null)
            throw new NullPointerException("Illegal null argument");

        synchronized (connectionIds) {
            connectionIds.remove(connectionId);
        }

        sendNotification(JMXConnectionNotification.CLOSED, connectionId,
                         message, userData);
    }

    /**
     * <p>Called by a subclass when a client connection fails.
     * Removes <code>connectionId</code> from the list returned by
     * {@link #getConnectionIds()}, then emits a {@link
     * JMXConnectionNotification} with type {@link
     * JMXConnectionNotification#FAILED}.</p>
     *
     * @param connectionId the ID of the failed connection.
     *
     * @param message the message for the emitted {@link
     * JMXConnectionNotification}.  Can be null.  See {@link
     * Notification#getMessage()}.
     *
     * @param userData the <code>userData</code> for the emitted
     * {@link JMXConnectionNotification}.  Can be null.  See {@link
     * Notification#getUserData()}.
     *
     * @exception NullPointerException if <code>connectionId</code> is
     * null.
     */
    protected void connectionFailed(String connectionId,
                                    String message,
                                    Object userData) {

        if (connectionId == null)
            throw new NullPointerException("Illegal null argument");

        synchronized (connectionIds) {
            connectionIds.remove(connectionId);
        }

        sendNotification(JMXConnectionNotification.FAILED, connectionId,
                         message, userData);
    }

    private void sendNotification(String type, String connectionId,
                                  String message, Object userData) {
        Notification notif =
            new JMXConnectionNotification(type,
                                          getNotificationSource(),
                                          connectionId,
                                          nextSequenceNumber(),
                                          message,
                                          userData);
        sendNotification(notif);
    }

    private synchronized Object getNotificationSource() {
        if (myName != null)
            return myName;
        else
            return this;
    }

    private static long nextSequenceNumber() {
        synchronized (sequenceNumberLock) {
            return sequenceNumber++;
        }
    }

    // implements MBeanRegistration
    /**
     * <p>Called by an MBean server when this connector server is
     * registered in that MBean server.  This connector server becomes
     * attached to the MBean server and its {@link #getMBeanServer()}
     * method will return <code>mbs</code>.</p>
     *
     * <p>If this connector server is already attached to an MBean
     * server, this method has no effect.  The MBean server it is
     * attached to is not necessarily the one it is being registered
     * in.</p>
     *
     * @param mbs the MBean server in which this connection server is
     * being registered.
     *
     * @param name The object name of the MBean.
     *
     * @return The name under which the MBean is to be registered.
     *
     * @exception NullPointerException if <code>mbs</code> or
     * <code>name</code> is null.
     */
    public synchronized ObjectName preRegister(MBeanServer mbs,
                                               ObjectName name) {
        if (mbs == null || name == null)
            throw new NullPointerException("Null MBeanServer or ObjectName");
        if (mbeanServer == null) {
            mbeanServer = mbs;
            myName = name;
        }
        return name;
    }

    public void postRegister(Boolean registrationDone) {
        // do nothing
    }

    /**
     * <p>Called by an MBean server when this connector server is
     * unregistered from that MBean server.  If this connector server
     * was attached to that MBean server by being registered in it,
     * and if the connector server is still active,
     * then unregistering it will call the {@link #stop stop} method.
     * If the <code>stop</code> method throws an exception, the
     * unregistration attempt will fail.  It is recommended to call
     * the <code>stop</code> method explicitly before unregistering
     * the MBean.</p>
     *
     * @exception IOException if thrown by the {@link #stop stop} method.
     */
    public synchronized void preDeregister() throws Exception {
        if (myName != null && isActive()) {
            stop();
            myName = null; // just in case stop is buggy and doesn't stop
        }
    }

    public void postDeregister() {
        myName = null;
    }

    /**
     * The MBeanServer used by this server to execute a client request.
     */
    private MBeanServer mbeanServer = null;

    /**
     * The name used to registered this server in an MBeanServer.
     * It is null if the this server is not registered or has been unregistered.
     */
    private ObjectName myName;

    private final List<String> connectionIds = new ArrayList<String>();

    private static final int[] sequenceNumberLock = new int[0];
    private static long sequenceNumber;
}
