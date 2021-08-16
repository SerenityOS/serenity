/*
 * Copyright (c) 2002, 2008, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Map;

/**
 * <p>MBean interface for connector servers.  A JMX API connector server
 * is attached to an MBean server, and establishes connections to that
 * MBean server for remote clients.</p>
 *
 * <p>A newly-created connector server is <em>inactive</em>, and does
 * not yet listen for connections.  Only when its {@link #start start}
 * method has been called does it start listening for connections.</p>
 *
 * @since 1.5
 */
public interface JMXConnectorServerMBean {
    /**
     * <p>Activates the connector server, that is, starts listening for
     * client connections.  Calling this method when the connector
     * server is already active has no effect.  Calling this method
     * when the connector server has been stopped will generate an
     * {@link IOException}.</p>
     *
     * @exception IOException if it is not possible to start listening
     * or if the connector server has been stopped.
     *
     * @exception IllegalStateException if the connector server has
     * not been attached to an MBean server.
     */
    public void start() throws IOException;

    /**
     * <p>Deactivates the connector server, that is, stops listening for
     * client connections.  Calling this method will also close all
     * client connections that were made by this server.  After this
     * method returns, whether normally or with an exception, the
     * connector server will not create any new client
     * connections.</p>
     *
     * <p>Once a connector server has been stopped, it cannot be started
     * again.</p>
     *
     * <p>Calling this method when the connector server has already
     * been stopped has no effect.  Calling this method when the
     * connector server has not yet been started will disable the
     * connector server object permanently.</p>
     *
     * <p>If closing a client connection produces an exception, that
     * exception is not thrown from this method.  A {@link
     * JMXConnectionNotification} with type {@link
     * JMXConnectionNotification#FAILED} is emitted from this MBean
     * with the connection ID of the connection that could not be
     * closed.</p>
     *
     * <p>Closing a connector server is a potentially slow operation.
     * For example, if a client machine with an open connection has
     * crashed, the close operation might have to wait for a network
     * protocol timeout.  Callers that do not want to block in a close
     * operation should do it in a separate thread.</p>
     *
     * @exception IOException if the server cannot be closed cleanly.
     * When this exception is thrown, the server has already attempted
     * to close all client connections.  All client connections are
     * closed except possibly those that generated exceptions when the
     * server attempted to close them.
     */
    public void stop() throws IOException;

    /**
     * <p>Determines whether the connector server is active.  A connector
     * server starts being active when its {@link #start start} method
     * returns successfully and remains active until either its
     * {@link #stop stop} method is called or the connector server
     * fails.</p>
     *
     * @return true if the connector server is active.
     */
    public boolean isActive();

    /**
     * <p>Inserts an object that intercepts requests for the MBean server
     * that arrive through this connector server.  This object will be
     * supplied as the <code>MBeanServer</code> for any new connection
     * created by this connector server.  Existing connections are
     * unaffected.</p>
     *
     * <p>This method can be called more than once with different
     * {@link MBeanServerForwarder} objects.  The result is a chain
     * of forwarders.  The last forwarder added is the first in the chain.
     * In more detail:</p>
     *
     * <ul>
     * <li><p>If this connector server is already associated with an
     * <code>MBeanServer</code> object, then that object is given to
     * {@link MBeanServerForwarder#setMBeanServer
     * mbsf.setMBeanServer}.  If doing so produces an exception, this
     * method throws the same exception without any other effect.</p>
     *
     * <li><p>If this connector is not already associated with an
     * <code>MBeanServer</code> object, or if the
     * <code>mbsf.setMBeanServer</code> call just mentioned succeeds,
     * then <code>mbsf</code> becomes this connector server's
     * <code>MBeanServer</code>.</p>
     * </ul>
     *
     * @param mbsf the new <code>MBeanServerForwarder</code>.
     *
     * @exception IllegalArgumentException if the call to {@link
     * MBeanServerForwarder#setMBeanServer mbsf.setMBeanServer} fails
     * with <code>IllegalArgumentException</code>.  This includes the
     * case where <code>mbsf</code> is null.
     */
    public void setMBeanServerForwarder(MBeanServerForwarder mbsf);

    /**
     * <p>The list of IDs for currently-open connections to this
     * connector server.</p>
     *
     * @return a new string array containing the list of IDs.  If
     * there are no currently-open connections, this array will be
     * empty.
     */
    public String[] getConnectionIds();

    /**
     * <p>The address of this connector server.</p>
     * <p>
     * The address returned may not be the exact original {@link JMXServiceURL}
     * that was supplied when creating the connector server, since the original
     * address may not always be complete. For example the port number may be
     * dynamically allocated when starting the connector server. Instead the
     * address returned is the actual {@link JMXServiceURL} of the
     * {@link JMXConnectorServer}. This is the address that clients supply
     * to {@link JMXConnectorFactory#connect(JMXServiceURL)}.
     * </p>
     * <p>Note that the address returned may be {@code null} if
     *    the {@code JMXConnectorServer} is not yet {@link #isActive active}.
     * </p>
     *
     * @return the address of this connector server, or null if it
     * does not have one.
     */
    public JMXServiceURL getAddress();

    /**
     * <p>The attributes for this connector server.</p>
     *
     * @return a read-only map containing the attributes for this
     * connector server.  Attributes whose values are not serializable
     * are omitted from this map.  If there are no serializable
     * attributes, the returned map is empty.
     */
    public Map<String,?> getAttributes();

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
     * @param env client connection parameters of the same sort that
     * can be provided to {@link JMXConnector#connect(Map)
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
     *
     */
    public JMXConnector toJMXConnector(Map<String,?> env)
        throws IOException;
}
