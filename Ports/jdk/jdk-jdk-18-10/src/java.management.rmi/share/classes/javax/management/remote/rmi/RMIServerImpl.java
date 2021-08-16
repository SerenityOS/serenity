/*
 * Copyright (c) 2002, 2015, Oracle and/or its affiliates. All rights reserved.
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

package javax.management.remote.rmi;

import com.sun.jmx.remote.internal.ArrayNotificationBuffer;
import com.sun.jmx.remote.internal.NotificationBuffer;
import com.sun.jmx.remote.security.JMXPluggableAuthenticator;
import com.sun.jmx.remote.util.ClassLogger;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.rmi.Remote;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXConnectorServer;
import javax.security.auth.Subject;

/**
 * <p>An RMI object representing a connector server.  Remote clients
 * can make connections using the {@link #newClient(Object)} method.  This
 * method returns an RMI object representing the connection.</p>
 *
 * <p>User code does not usually reference this class directly.
 * RMI connection servers are usually created with the class {@link
 * RMIConnectorServer}.  Remote clients usually create connections
 * either with {@link javax.management.remote.JMXConnectorFactory}
 * or by instantiating {@link RMIConnector}.</p>
 *
 * <p>This is an abstract class.  Concrete subclasses define the
 * details of the client connection objects.</p>
 *
 * @since 1.5
 */
public abstract class RMIServerImpl implements Closeable, RMIServer {
    /**
     * <p>Constructs a new <code>RMIServerImpl</code>.</p>
     *
     * @param env the environment containing attributes for the new
     * <code>RMIServerImpl</code>.  Can be null, which is equivalent
     * to an empty Map.
     */
    public RMIServerImpl(Map<String,?> env) {
        this.env = (env == null) ? Collections.<String,Object>emptyMap() : env;
    }

    void setRMIConnectorServer(RMIConnectorServer connServer)
            throws IOException {
        this.connServer = connServer;
    }

    /**
     * <p>Exports this RMI object.</p>
     *
     * @exception IOException if this RMI object cannot be exported.
     */
    protected abstract void export() throws IOException;

    /**
     * Returns a remotable stub for this server object.
     * @return a remotable stub.
     * @exception IOException if the stub cannot be obtained - e.g the
     *            RMIServerImpl has not been exported yet.
     **/
    public abstract Remote toStub() throws IOException;

    /**
     * <p>Sets the default <code>ClassLoader</code> for this connector
     * server. New client connections will use this classloader.
     * Existing client connections are unaffected.</p>
     *
     * @param cl the new <code>ClassLoader</code> to be used by this
     * connector server.
     *
     * @see #getDefaultClassLoader
     */
    public synchronized void setDefaultClassLoader(ClassLoader cl) {
        this.cl = cl;
    }

    /**
     * <p>Gets the default <code>ClassLoader</code> used by this connector
     * server.</p>
     *
     * @return the default <code>ClassLoader</code> used by this
     * connector server.
     *
     * @see #setDefaultClassLoader
     */
    public synchronized ClassLoader getDefaultClassLoader() {
        return cl;
    }

    /**
     * <p>Sets the <code>MBeanServer</code> to which this connector
     * server is attached. New client connections will interact
     * with this <code>MBeanServer</code>. Existing client connections are
     * unaffected.</p>
     *
     * @param mbs the new <code>MBeanServer</code>.  Can be null, but
     * new client connections will be refused as long as it is.
     *
     * @see #getMBeanServer
     */
    public synchronized void setMBeanServer(MBeanServer mbs) {
        this.mbeanServer = mbs;
    }

    /**
     * <p>The <code>MBeanServer</code> to which this connector server
     * is attached.  This is the last value passed to {@link
     * #setMBeanServer} on this object, or null if that method has
     * never been called.</p>
     *
     * @return the <code>MBeanServer</code> to which this connector
     * is attached.
     *
     * @see #setMBeanServer
     */
    public synchronized MBeanServer getMBeanServer() {
        return mbeanServer;
    }

    public String getVersion() {
        // Expected format is: "protocol-version implementation-name"
        try {
            return "1.0 java_runtime_" +
                    System.getProperty("java.runtime.version");
        } catch (SecurityException e) {
            return "1.0 ";
        }
    }

    /**
     * <p>Creates a new client connection.  This method calls {@link
     * #makeClient makeClient} and adds the returned client connection
     * object to an internal list.  When this
     * <code>RMIServerImpl</code> is shut down via its {@link
     * #close()} method, the {@link RMIConnection#close() close()}
     * method of each object remaining in the list is called.</p>
     *
     * <p>The fact that a client connection object is in this internal
     * list does not prevent it from being garbage collected.</p>
     *
     * @param credentials this object specifies the user-defined
     * credentials to be passed in to the server in order to
     * authenticate the caller before creating the
     * <code>RMIConnection</code>.  Can be null.
     *
     * @return the newly-created <code>RMIConnection</code>.  This is
     * usually the object created by <code>makeClient</code>, though
     * an implementation may choose to wrap that object in another
     * object implementing <code>RMIConnection</code>.
     *
     * @exception IOException if the new client object cannot be
     * created or exported.
     *
     * @exception SecurityException if the given credentials do not allow
     * the server to authenticate the user successfully.
     *
     * @exception IllegalStateException if {@link #getMBeanServer()}
     * is null.
     */
    public RMIConnection newClient(Object credentials) throws IOException {
        return doNewClient(credentials);
    }

    /**
     * This method could be overridden by subclasses defined in this package
     * to perform additional operations specific to the underlying transport
     * before creating the new client connection.
     */
    RMIConnection doNewClient(Object credentials) throws IOException {
        final boolean tracing = logger.traceOn();

        if (tracing) logger.trace("newClient","making new client");

        if (getMBeanServer() == null)
            throw new IllegalStateException("Not attached to an MBean server");

        Subject subject = null;
        JMXAuthenticator authenticator =
            (JMXAuthenticator) env.get(JMXConnectorServer.AUTHENTICATOR);
        if (authenticator == null) {
            /*
             * Create the JAAS-based authenticator only if authentication
             * has been enabled
             */
            if (env.get("jmx.remote.x.password.file") != null ||
                env.get("jmx.remote.x.login.config") != null) {
                authenticator = new JMXPluggableAuthenticator(env);
            }
        }
        if (authenticator != null) {
            if (tracing) logger.trace("newClient","got authenticator: " +
                               authenticator.getClass().getName());
            try {
                subject = authenticator.authenticate(credentials);
            } catch (SecurityException e) {
                logger.trace("newClient", "Authentication failed: " + e);
                throw e;
            }
        }

        if (tracing) {
            if (subject != null)
                logger.trace("newClient","subject is not null");
            else logger.trace("newClient","no subject");
        }

        final String connectionId = makeConnectionId(getProtocol(), subject);

        if (tracing)
            logger.trace("newClient","making new connection: " + connectionId);

        RMIConnection client = makeClient(connectionId, subject);

        dropDeadReferences();
        WeakReference<RMIConnection> wr = new WeakReference<RMIConnection>(client);
        synchronized (clientList) {
            clientList.add(wr);
        }

        connServer.connectionOpened(connectionId, "Connection opened", null);

        synchronized (clientList) {
            if (!clientList.contains(wr)) {
                // can be removed only by a JMXConnectionNotification listener
                throw new IOException("The connection is refused.");
            }
        }

        if (tracing)
            logger.trace("newClient","new connection done: " + connectionId );

        return client;
    }

    /**
     * <p>Creates a new client connection.  This method is called by
     * the public method {@link #newClient(Object)}.</p>
     *
     * @param connectionId the ID of the new connection.  Every
     * connection opened by this connector server will have a
     * different ID.  The behavior is unspecified if this parameter is
     * null.
     *
     * @param subject the authenticated subject.  Can be null.
     *
     * @return the newly-created <code>RMIConnection</code>.
     *
     * @exception IOException if the new client object cannot be
     * created or exported.
     */
    protected abstract RMIConnection makeClient(String connectionId,
                                                Subject subject)
            throws IOException;

    /**
     * <p>Closes a client connection made by {@link #makeClient makeClient}.
     *
     * @param client a connection previously returned by
     * <code>makeClient</code> on which the <code>closeClient</code>
     * method has not previously been called.  The behavior is
     * unspecified if these conditions are violated, including the
     * case where <code>client</code> is null.
     *
     * @exception IOException if the client connection cannot be
     * closed.
     */
    protected abstract void closeClient(RMIConnection client)
            throws IOException;

    /**
     * <p>Returns the protocol string for this object.  The string is
     * <code>rmi</code> for RMI/JRMP.
     *
     * @return the protocol string for this object.
     */
    protected abstract String getProtocol();

    /**
     * <p>Method called when a client connection created by {@link
     * #makeClient makeClient} is closed.  A subclass that defines
     * <code>makeClient</code> must arrange for this method to be
     * called when the resultant object's {@link RMIConnection#close()
     * close} method is called.  This enables it to be removed from
     * the <code>RMIServerImpl</code>'s list of connections.  It is
     * not an error for <code>client</code> not to be in that
     * list.</p>
     *
     * <p>After removing <code>client</code> from the list of
     * connections, this method calls {@link #closeClient
     * closeClient(client)}.</p>
     *
     * @param client the client connection that has been closed.
     *
     * @exception IOException if {@link #closeClient} throws this
     * exception.
     *
     * @exception NullPointerException if <code>client</code> is null.
     */
    protected void clientClosed(RMIConnection client) throws IOException {
        final boolean debug = logger.debugOn();

        if (debug) logger.trace("clientClosed","client="+client);

        if (client == null)
            throw new NullPointerException("Null client");

        synchronized (clientList) {
            dropDeadReferences();
            for (Iterator<WeakReference<RMIConnection>> it = clientList.iterator();
                 it.hasNext(); ) {
                WeakReference<RMIConnection> wr = it.next();
                if (wr.get() == client) {
                    it.remove();
                    break;
                }
            }
            /* It is not a bug for this loop not to find the client.  In
               our close() method, we remove a client from the list before
               calling its close() method.  */
        }

        if (debug) logger.trace("clientClosed", "closing client.");
        closeClient(client);

        if (debug) logger.trace("clientClosed", "sending notif");
        connServer.connectionClosed(client.getConnectionId(),
                                    "Client connection closed", null);

        if (debug) logger.trace("clientClosed","done");
    }

    /**
     * <p>Closes this connection server.  This method first calls the
     * {@link #closeServer()} method so that no new client connections
     * will be accepted.  Then, for each remaining {@link
     * RMIConnection} object returned by {@link #makeClient
     * makeClient}, its {@link RMIConnection#close() close} method is
     * called.</p>
     *
     * <p>The behavior when this method is called more than once is
     * unspecified.</p>
     *
     * <p>If {@link #closeServer()} throws an
     * <code>IOException</code>, the individual connections are
     * nevertheless closed, and then the <code>IOException</code> is
     * thrown from this method.</p>
     *
     * <p>If {@link #closeServer()} returns normally but one or more
     * of the individual connections throws an
     * <code>IOException</code>, then, after closing all the
     * connections, one of those <code>IOException</code>s is thrown
     * from this method.  If more than one connection throws an
     * <code>IOException</code>, it is unspecified which one is thrown
     * from this method.</p>
     *
     * @exception IOException if {@link #closeServer()} or one of the
     * {@link RMIConnection#close()} calls threw
     * <code>IOException</code>.
     */
    public synchronized void close() throws IOException {
        final boolean tracing = logger.traceOn();
        final boolean debug   = logger.debugOn();

        if (tracing) logger.trace("close","closing");

        IOException ioException = null;
        try {
            if (debug)   logger.debug("close","closing Server");
            closeServer();
        } catch (IOException e) {
            if (tracing) logger.trace("close","Failed to close server: " + e);
            if (debug)   logger.debug("close",e);
            ioException = e;
        }

        if (debug)   logger.debug("close","closing Clients");
        // Loop to close all clients
        while (true) {
            synchronized (clientList) {
                if (debug) logger.debug("close","droping dead references");
                dropDeadReferences();

                if (debug) logger.debug("close","client count: "+clientList.size());
                if (clientList.size() == 0)
                    break;
                /* Loop until we find a non-null client.  Because we called
                   dropDeadReferences(), this will usually be the first
                   element of the list, but a garbage collection could have
                   happened in between.  */
                for (Iterator<WeakReference<RMIConnection>> it = clientList.iterator();
                     it.hasNext(); ) {
                    WeakReference<RMIConnection> wr = it.next();
                    RMIConnection client = wr.get();
                    it.remove();
                    if (client != null) {
                        try {
                            client.close();
                        } catch (IOException e) {
                            if (tracing)
                                logger.trace("close","Failed to close client: " + e);
                            if (debug) logger.debug("close",e);
                            if (ioException == null)
                                ioException = e;
                        }
                        break;
                    }
                }
            }
        }

        if(notifBuffer != null)
            notifBuffer.dispose();

        if (ioException != null) {
            if (tracing) logger.trace("close","close failed.");
            throw ioException;
        }

        if (tracing) logger.trace("close","closed.");
    }

    /**
     * <p>Called by {@link #close()} to close the connector server.
     * After returning from this method, the connector server must
     * not accept any new connections.</p>
     *
     * @exception IOException if the attempt to close the connector
     * server failed.
     */
    protected abstract void closeServer() throws IOException;

    private static synchronized String makeConnectionId(String protocol,
                                                        Subject subject) {
        connectionIdNumber++;

        String clientHost = "";
        try {
            clientHost = RemoteServer.getClientHost();
            /*
             * According to the rules specified in the javax.management.remote
             * package description, a numeric IPv6 address (detected by the
             * presence of otherwise forbidden ":" character) forming a part
             * of the connection id must be enclosed in square brackets.
             */
            if (clientHost.contains(":")) {
                clientHost = "[" + clientHost + "]";
            }
        } catch (ServerNotActiveException e) {
            logger.trace("makeConnectionId", "getClientHost", e);
        }

        final StringBuilder buf = new StringBuilder();
        buf.append(protocol).append(":");
        if (clientHost.length() > 0)
            buf.append("//").append(clientHost);
        buf.append(" ");
        if (subject != null) {
            Set<Principal> principals = subject.getPrincipals();
            String sep = "";
            for (Iterator<Principal> it = principals.iterator(); it.hasNext(); ) {
                Principal p = it.next();
                String name = p.getName().replace(' ', '_').replace(';', ':');
                buf.append(sep).append(name);
                sep = ";";
            }
        }
        buf.append(" ").append(connectionIdNumber);
        if (logger.traceOn())
            logger.trace("newConnectionId","connectionId="+buf);
        return buf.toString();
    }

    private void dropDeadReferences() {
        synchronized (clientList) {
            for (Iterator<WeakReference<RMIConnection>> it = clientList.iterator();
                 it.hasNext(); ) {
                WeakReference<RMIConnection> wr = it.next();
                if (wr.get() == null)
                    it.remove();
            }
        }
    }

    synchronized NotificationBuffer getNotifBuffer() {
        //Notification buffer is lazily created when the first client connects
        if(notifBuffer == null)
            notifBuffer =
                ArrayNotificationBuffer.getNotificationBuffer(mbeanServer,
                                                              env);
        return notifBuffer;
    }

    private static final ClassLogger logger =
        new ClassLogger("javax.management.remote.rmi", "RMIServerImpl");

    /** List of WeakReference values.  Each one references an
        RMIConnection created by this object, or null if the
        RMIConnection has been garbage-collected.  */
    private final List<WeakReference<RMIConnection>> clientList =
            new ArrayList<WeakReference<RMIConnection>>();

    private ClassLoader cl;

    private MBeanServer mbeanServer;

    private final Map<String, ?> env;

    private RMIConnectorServer connServer;

    private static int connectionIdNumber;

    private NotificationBuffer notifBuffer;
}
