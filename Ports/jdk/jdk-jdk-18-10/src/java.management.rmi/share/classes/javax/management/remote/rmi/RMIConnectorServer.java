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

package javax.management.remote.rmi;


import com.sun.jmx.remote.security.MBeanServerFileAccessController;
import com.sun.jmx.remote.util.ClassLogger;
import com.sun.jmx.remote.util.EnvHelp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.remote.JMXAuthenticator;

import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.MBeanServerForwarder;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * <p>A JMX API connector server that creates RMI-based connections
 * from remote clients.  Usually, such connector servers are made
 * using {@link javax.management.remote.JMXConnectorServerFactory
 * JMXConnectorServerFactory}.  However, specialized applications can
 * use this class directly, for example with an {@link RMIServerImpl}
 * object.</p>
 *
 * @since 1.5
 */
public class RMIConnectorServer extends JMXConnectorServer {
    /**
     * <p>Name of the attribute that specifies whether the {@link
     * RMIServer} stub that represents an RMI connector server should
     * override an existing stub at the same address.  The value
     * associated with this attribute, if any, should be a string that
     * is equal, ignoring case, to <code>"true"</code> or
     * <code>"false"</code>.  The default value is false.</p>
     */
    public static final String JNDI_REBIND_ATTRIBUTE =
        "jmx.remote.jndi.rebind";

    /**
     * <p>Name of the attribute that specifies the {@link
     * RMIClientSocketFactory} for the RMI objects created in
     * conjunction with this connector. The value associated with this
     * attribute must be of type <code>RMIClientSocketFactory</code> and can
     * only be specified in the <code>Map</code> argument supplied when
     * creating a connector server.</p>
     */
    public static final String RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE =
        "jmx.remote.rmi.client.socket.factory";

    /**
     * <p>Name of the attribute that specifies the {@link
     * RMIServerSocketFactory} for the RMI objects created in
     * conjunction with this connector. The value associated with this
     * attribute must be of type <code>RMIServerSocketFactory</code> and can
     * only be specified in the <code>Map</code> argument supplied when
     * creating a connector server.</p>
     */
    public static final String RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE =
        "jmx.remote.rmi.server.socket.factory";

    /**
    * Name of the attribute that specifies an
    * {@link ObjectInputFilter} pattern string to filter classes acceptable
    * for {@link RMIServer#newClient(java.lang.Object) RMIServer.newClient()}
    * remote method call.
    * <p>
    * The filter pattern must be in same format as used in
    * {@link java.io.ObjectInputFilter.Config#createFilter}
    * <p>
    * This list of classes allowed by filter should correspond to the
    * transitive closure of the credentials class (or classes) used by the
    * installed {@linkplain JMXAuthenticator} associated with the
    * {@linkplain RMIServer} implementation.
    * If the attribute is not set then any class is deemed acceptable.
    * @see ObjectInputFilter
    */
    public static final String CREDENTIALS_FILTER_PATTERN =
        "jmx.remote.rmi.server.credentials.filter.pattern";

    /**
     * This attribute defines a pattern from which to create a
     * {@link java.io.ObjectInputFilter} that will be used when deserializing
     * objects sent to the {@code JMXConnectorServer} by any client.
     * <p>
     * The filter will be called for any class found in the serialized
     * stream sent to server by client, including all JMX defined classes
     * (such as {@link javax.management.ObjectName}), all method parameters,
     * and, if present in the stream, all classes transitively referred by
     * the serial form of any deserialized object.
     * The pattern must be in same format as used in
     * {@link java.io.ObjectInputFilter.Config#createFilter}.
     * It may define an allow-list of permitted classes, a reject-list of
     * rejected classes, a maximum depth for the deserialized objects,
     * etc.
     * <p>
     * To be functional, the filter should allow at least all the
     * concrete types in the transitive closure of all objects that
     * might get serialized when serializing all JMX classes referred
     * as parameters in the {@link
     * javax.management.remote.rmi.RMIConnection} interface,
     * plus all classes that a {@link javax.management.remote.rmi.RMIConnector client}
     * might need to transmit wrapped in {@linkplain java.rmi.MarshalledObject
     * marshalled objects} in order to interoperate with the MBeans registered
     * in the {@code MBeanServer}. That would potentially include all the
     * concrete {@linkplain javax.management.openmbean  JMX OpenTypes} and the
     * classes they use in their serial form.
     * <p>
     * Care must be taken when defining such a filter, as defining
     * an allow-list that is too narrow or a reject-list that is too wide may
     * prevent legitimate clients from interoperating with the
     * {@code JMXConnectorServer}.
     */
    public static final String SERIAL_FILTER_PATTERN =
       "jmx.remote.rmi.server.serial.filter.pattern";

    /**
     * <p>Makes an <code>RMIConnectorServer</code>.
     * This is equivalent to calling {@link #RMIConnectorServer(
     * JMXServiceURL,Map,RMIServerImpl,MBeanServer)
     * RMIConnectorServer(directoryURL,environment,null,null)}</p>
     *
     * @param url the URL defining how to create the connector server.
     * Cannot be null.
     *
     * @param environment attributes governing the creation and
     * storing of the RMI object.  Can be null, which is equivalent to
     * an empty Map.
     *
     * @exception IllegalArgumentException if <code>url</code> is null.
     *
     * @exception MalformedURLException if <code>url</code> does not
     * conform to the syntax for an RMI connector, or if its protocol
     * is not recognized by this implementation. Only "rmi" is valid when
     * this constructor is used.
     *
     * @exception IOException if the connector server cannot be created
     * for some reason or if it is inevitable that its {@link #start()
     * start} method will fail.
     */
    public RMIConnectorServer(JMXServiceURL url, Map<String,?> environment)
            throws IOException {
        this(url, environment, (MBeanServer) null);
    }

    /**
     * <p>Makes an <code>RMIConnectorServer</code> for the given MBean
     * server.
     * This is equivalent to calling {@link #RMIConnectorServer(
     * JMXServiceURL,Map,RMIServerImpl,MBeanServer)
     * RMIConnectorServer(directoryURL,environment,null,mbeanServer)}</p>
     *
     * @param url the URL defining how to create the connector server.
     * Cannot be null.
     *
     * @param environment attributes governing the creation and
     * storing of the RMI object.  Can be null, which is equivalent to
     * an empty Map.
     *
     * @param mbeanServer the MBean server to which the new connector
     * server is attached, or null if it will be attached by being
     * registered as an MBean in the MBean server.
     *
     * @exception IllegalArgumentException if <code>url</code> is null.
     *
     * @exception MalformedURLException if <code>url</code> does not
     * conform to the syntax for an RMI connector, or if its protocol
     * is not recognized by this implementation. Only "rmi" is valid
     * when this constructor is used.
     *
     * @exception IOException if the connector server cannot be created
     * for some reason or if it is inevitable that its {@link #start()
     * start} method will fail.
     */
    public RMIConnectorServer(JMXServiceURL url, Map<String,?> environment,
                              MBeanServer mbeanServer)
            throws IOException {
        this(url, environment, (RMIServerImpl) null, mbeanServer);
    }

    /**
     * <p>Makes an <code>RMIConnectorServer</code> for the given MBean
     * server.</p>
     *
     * @param url the URL defining how to create the connector server.
     * Cannot be null.
     *
     * @param environment attributes governing the creation and
     * storing of the RMI object.  Can be null, which is equivalent to
     * an empty Map.
     *
     * @param rmiServerImpl An implementation of the RMIServer interface,
     *  consistent with the protocol type specified in <var>url</var>.
     *  If this parameter is non null, the protocol type specified by
     *  <var>url</var> is not constrained, and is assumed to be valid.
     *  Otherwise, only "rmi" will be recognized.
     *
     * @param mbeanServer the MBean server to which the new connector
     * server is attached, or null if it will be attached by being
     * registered as an MBean in the MBean server.
     *
     * @exception IllegalArgumentException if <code>url</code> is null.
     *
     * @exception MalformedURLException if <code>url</code> does not
     * conform to the syntax for an RMI connector, or if its protocol
     * is not recognized by this implementation. Only "rmi" is recognized
     * when <var>rmiServerImpl</var> is null.
     *
     * @exception IOException if the connector server cannot be created
     * for some reason or if it is inevitable that its {@link #start()
     * start} method will fail.
     *
     * @see #start
     */
    public RMIConnectorServer(JMXServiceURL url, Map<String,?> environment,
                              RMIServerImpl rmiServerImpl,
                              MBeanServer mbeanServer)
            throws IOException {
        super(mbeanServer);

        if (url == null) throw new
            IllegalArgumentException("Null JMXServiceURL");
        if (rmiServerImpl == null) {
            final String prt = url.getProtocol();
            if (prt == null || !(prt.equals("rmi"))) {
                final String msg = "Invalid protocol type: " + prt;
                throw new MalformedURLException(msg);
            }
            final String urlPath = url.getURLPath();
            if (!urlPath.isEmpty()
                && !urlPath.equals("/")
                && !urlPath.startsWith("/jndi/")) {
                final String msg = "URL path must be empty or start with " +
                    "/jndi/";
                throw new MalformedURLException(msg);
            }
        }

        if (environment == null)
            this.attributes = Collections.emptyMap();
        else {
            EnvHelp.checkAttributes(environment);
            this.attributes = Collections.unmodifiableMap(environment);
        }

        this.address = url;
        this.rmiServerImpl = rmiServerImpl;
    }

    /**
     * <p>Returns a client stub for this connector server.  A client
     * stub is a serializable object whose {@link
     * JMXConnector#connect(Map) connect} method can be used to make
     * one new connection to this connector server.</p>
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
     * not started (see {@link #isActive()}).
     *
     * @exception IOException if a communications problem means that a
     * stub cannot be created.
     **/
    public JMXConnector toJMXConnector(Map<String,?> env) throws IOException {
        // The serialized for of rmiServerImpl is automatically
        // a RMI server stub.
        if (!isActive()) throw new
            IllegalStateException("Connector is not active");

        // Merge maps
        Map<String, Object> usemap = new HashMap<String, Object>(
                (this.attributes==null)?Collections.<String, Object>emptyMap():
                    this.attributes);

        if (env != null) {
            EnvHelp.checkAttributes(env);
            usemap.putAll(env);
        }

        usemap = EnvHelp.filterAttributes(usemap);

        final RMIServer stub=(RMIServer)rmiServerImpl.toStub();

        return new RMIConnector(stub, usemap);
    }

    /**
     * <p>Activates the connector server, that is starts listening for
     * client connections.  Calling this method when the connector
     * server is already active has no effect.  Calling this method
     * when the connector server has been stopped will generate an
     * <code>IOException</code>.</p>
     *
     * <p>The behavior of this method when called for the first time
     * depends on the parameters that were supplied at construction,
     * as described below.</p>
     *
     * <p>First, an object of a subclass of {@link RMIServerImpl} is
     * required, to export the connector server through RMI:</p>
     *
     * <ul>
     *
     * <li>If an <code>RMIServerImpl</code> was supplied to the
     * constructor, it is used.
     *
     * <li>Otherwise, if the <code>JMXServiceURL</code>
     * was null, or its protocol part was <code>rmi</code>, an object
     * of type {@link RMIJRMPServerImpl} is created.
     *
     * <li>Otherwise, the implementation can create an
     * implementation-specific {@link RMIServerImpl} or it can throw
     * {@link MalformedURLException}.
     *
     * </ul>
     *
     * <p>If the given address includes a JNDI directory URL as
     * specified in the package documentation for {@link
     * javax.management.remote.rmi}, then this
     * <code>RMIConnectorServer</code> will bootstrap by binding the
     * <code>RMIServerImpl</code> to the given address.</p>
     *
     * <p>If the URL path part of the <code>JMXServiceURL</code> was
     * empty or a single slash (<code>/</code>), then the RMI object
     * will not be bound to a directory.  Instead, a reference to it
     * will be encoded in the URL path of the RMIConnectorServer
     * address (returned by {@link #getAddress()}).  The encodings for
     * <code>rmi</code> are described in the package documentation for
     * {@link javax.management.remote.rmi}.</p>
     *
     * <p>The behavior when the URL path is neither empty nor a JNDI
     * directory URL, or when the protocol is not <code>rmi</code>,
     * is implementation defined, and may include throwing
     * {@link MalformedURLException} when the connector server is created
     * or when it is started.</p>
     *
     * @exception IllegalStateException if the connector server has
     * not been attached to an MBean server.
     * @exception IOException if the connector server cannot be
     * started.
     */
    public synchronized void start() throws IOException {
        final boolean tracing = logger.traceOn();

        if (state == STARTED) {
            if (tracing) logger.trace("start", "already started");
            return;
        } else if (state == STOPPED) {
            if (tracing) logger.trace("start", "already stopped");
            throw new IOException("The server has been stopped.");
        }

        if (getMBeanServer() == null)
            throw new IllegalStateException("This connector server is not " +
                                            "attached to an MBean server");

        // Check the internal access file property to see
        // if an MBeanServerForwarder is to be provided
        //
        if (attributes != null) {
            // Check if access file property is specified
            //
            String accessFile =
                (String) attributes.get("jmx.remote.x.access.file");
            if (accessFile != null) {
                // Access file property specified, create an instance
                // of the MBeanServerFileAccessController class
                //
                MBeanServerForwarder mbsf;
                try {
                    mbsf = new MBeanServerFileAccessController(accessFile);
                } catch (IOException e) {
                    throw EnvHelp.initCause(
                        new IllegalArgumentException(e.getMessage()), e);
                }
                // Set the MBeanServerForwarder
                //
                setMBeanServerForwarder(mbsf);
            }
        }

        try {
            if (tracing) logger.trace("start", "setting default class loader");
            defaultClassLoader = EnvHelp.resolveServerClassLoader(
                    attributes, getMBeanServer());
        } catch (InstanceNotFoundException infc) {
            IllegalArgumentException x = new
                IllegalArgumentException("ClassLoader not found: "+infc);
            throw EnvHelp.initCause(x,infc);
        }

        if (tracing) logger.trace("start", "setting RMIServer object");
        final RMIServerImpl rmiServer;

        if (rmiServerImpl != null)
            rmiServer = rmiServerImpl;
        else
            rmiServer = newServer();

        rmiServer.setMBeanServer(getMBeanServer());
        rmiServer.setDefaultClassLoader(defaultClassLoader);
        rmiServer.setRMIConnectorServer(this);
        rmiServer.export();

        try {
            if (tracing) logger.trace("start", "getting RMIServer object to export");
            final RMIServer objref = objectToBind(rmiServer, attributes);

            if (address != null && address.getURLPath().startsWith("/jndi/")) {
                final String jndiUrl = address.getURLPath().substring(6);

                if (tracing)
                    logger.trace("start", "Using external directory: " + jndiUrl);

                String stringBoolean = (String) attributes.get(JNDI_REBIND_ATTRIBUTE);
                final boolean rebind = EnvHelp.computeBooleanFromString( stringBoolean );

                if (tracing)
                    logger.trace("start", JNDI_REBIND_ATTRIBUTE + "=" + rebind);

                try {
                    if (tracing) logger.trace("start", "binding to " + jndiUrl);

                    final Hashtable<?, ?> usemap = EnvHelp.mapToHashtable(attributes);

                    bind(jndiUrl, usemap, objref, rebind);

                    boundJndiUrl = jndiUrl;
                } catch (NamingException e) {
                    // fit e in the nested exception if we are on 1.4
                    throw newIOException("Cannot bind to URL ["+jndiUrl+"]: "
                                         + e, e);
                }
            } else {
                // if jndiURL is null, we must encode the stub into the URL.
                if (tracing) logger.trace("start", "Encoding URL");

                encodeStubInAddress(objref, attributes);

                if (tracing) logger.trace("start", "Encoded URL: " + this.address);
            }
        } catch (Exception e) {
            try {
                rmiServer.close();
            } catch (Exception x) {
                // OK: we are already throwing another exception
            }
            if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            else if (e instanceof IOException)
                throw (IOException) e;
            else
                throw newIOException("Got unexpected exception while " +
                                     "starting the connector server: "
                                     + e, e);
        }

        rmiServerImpl = rmiServer;

        synchronized(openedServers) {
            openedServers.add(this);
        }

        state = STARTED;

        if (tracing) {
            logger.trace("start", "Connector Server Address = " + address);
            logger.trace("start", "started.");
        }
    }

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
     * JMXConnectionNotification} is emitted from this MBean with the
     * connection ID of the connection that could not be closed.</p>
     *
     * <p>Closing a connector server is a potentially slow operation.
     * For example, if a client machine with an open connection has
     * crashed, the close operation might have to wait for a network
     * protocol timeout.  Callers that do not want to block in a close
     * operation should do it in a separate thread.</p>
     *
     * <p>This method calls the method {@link RMIServerImpl#close()
     * close} on the connector server's <code>RMIServerImpl</code>
     * object.</p>
     *
     * <p>If the <code>RMIServerImpl</code> was bound to a JNDI
     * directory by the {@link #start() start} method, it is unbound
     * from the directory by this method.</p>
     *
     * @exception IOException if the server cannot be closed cleanly,
     * or if the <code>RMIServerImpl</code> cannot be unbound from the
     * directory.  When this exception is thrown, the server has
     * already attempted to close all client connections, if
     * appropriate; to call {@link RMIServerImpl#close()}; and to
     * unbind the <code>RMIServerImpl</code> from its directory, if
     * appropriate.  All client connections are closed except possibly
     * those that generated exceptions when the server attempted to
     * close them.
     */
    public void stop() throws IOException {
        final boolean tracing = logger.traceOn();

        synchronized (this) {
            if (state == STOPPED) {
                if (tracing) logger.trace("stop","already stopped.");
                return;
            } else if (state == CREATED) {
                if (tracing) logger.trace("stop","not started yet.");
            }

            if (tracing) logger.trace("stop", "stopping.");
            state = STOPPED;
        }

        synchronized(openedServers) {
            openedServers.remove(this);
        }

        IOException exception = null;

        // rmiServerImpl can be null if stop() called without start()
        if (rmiServerImpl != null) {
            try {
                if (tracing) logger.trace("stop", "closing RMI server.");
                rmiServerImpl.close();
            } catch (IOException e) {
                if (tracing) logger.trace("stop", "failed to close RMI server: " + e);
                if (logger.debugOn()) logger.debug("stop",e);
                exception = e;
            }
        }

        if (boundJndiUrl != null) {
            try {
                if (tracing)
                    logger.trace("stop",
                          "unbind from external directory: " + boundJndiUrl);

                final Hashtable<?, ?> usemap = EnvHelp.mapToHashtable(attributes);

                InitialContext ctx =
                    new InitialContext(usemap);

                ctx.unbind(boundJndiUrl);

                ctx.close();
            } catch (NamingException e) {
                if (tracing) logger.trace("stop", "failed to unbind RMI server: "+e);
                if (logger.debugOn()) logger.debug("stop",e);
                // fit e in as the nested exception if we are on 1.4
                if (exception == null)
                    exception = newIOException("Cannot bind to URL: " + e, e);
            }
        }

        if (exception != null) throw exception;

        if (tracing) logger.trace("stop", "stopped");
    }

    public synchronized boolean isActive() {
        return (state == STARTED);
    }

    public JMXServiceURL getAddress() {
        if (!isActive())
            return null;
        return address;
    }

    public Map<String,?> getAttributes() {
        Map<String, ?> map = EnvHelp.filterAttributes(attributes);
        return Collections.unmodifiableMap(map);
    }

    @Override
    public synchronized
        void setMBeanServerForwarder(MBeanServerForwarder mbsf) {
        super.setMBeanServerForwarder(mbsf);
        if (rmiServerImpl != null)
            rmiServerImpl.setMBeanServer(getMBeanServer());
    }

    /* We repeat the definitions of connection{Opened,Closed,Failed}
       here so that they are accessible to other classes in this package
       even though they have protected access.  */

    @Override
    protected void connectionOpened(String connectionId, String message,
                                    Object userData) {
        super.connectionOpened(connectionId, message, userData);
    }

    @Override
    protected void connectionClosed(String connectionId, String message,
                                    Object userData) {
        super.connectionClosed(connectionId, message, userData);
    }

    @Override
    protected void connectionFailed(String connectionId, String message,
                                    Object userData) {
        super.connectionFailed(connectionId, message, userData);
    }

    /**
     * Bind a stub to a registry.
     * @param jndiUrl URL of the stub in the registry, extracted
     *        from the <code>JMXServiceURL</code>.
     * @param attributes A Hashtable containing environment parameters,
     *        built from the Map specified at this object creation.
     * @param rmiServer The object to bind in the registry
     * @param rebind true if the object must be rebound.
     **/
    void bind(String jndiUrl, Hashtable<?, ?> attributes,
              RMIServer rmiServer, boolean rebind)
        throws NamingException, MalformedURLException {
        // if jndiURL is not null, we nust bind the stub to a
        // directory.
        InitialContext ctx =
            new InitialContext(attributes);

        if (rebind)
            ctx.rebind(jndiUrl, rmiServer);
        else
            ctx.bind(jndiUrl, rmiServer);
        ctx.close();
    }

    /**
     * Creates a new RMIServerImpl.
     **/
    RMIServerImpl newServer() throws IOException {
        final int port;
        if (address == null)
            port = 0;
        else
            port = address.getPort();

        return newJRMPServer(attributes, port);
    }

    /**
     * Encode a stub into the JMXServiceURL.
     * @param rmiServer The stub object to encode in the URL
     * @param attributes A Map containing environment parameters,
     *        built from the Map specified at this object creation.
     **/
    private void encodeStubInAddress(
            RMIServer rmiServer, Map<String, ?> attributes)
            throws IOException {

        final String protocol, host;
        final int port;

        if (address == null) {
            protocol = "rmi";
            host = null; // will default to local host name
            port = 0;
        } else {
            protocol = address.getProtocol();
            host = (address.getHost().isEmpty()) ? null : address.getHost();
            port = address.getPort();
        }

        final String urlPath = encodeStub(rmiServer, attributes);

        address = new JMXServiceURL(protocol, host, port, urlPath);
    }

    /**
     * Returns the IOR of the given rmiServer.
     **/
    static String encodeStub(
            RMIServer rmiServer, Map<String, ?> env) throws IOException {
        return "/stub/" + encodeJRMPStub(rmiServer, env);
    }

    static String encodeJRMPStub(
            RMIServer rmiServer, Map<String, ?> env)
            throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(rmiServer);
        oout.close();
        byte[] bytes = bout.toByteArray();
        return byteArrayToBase64(bytes);
    }

    /**
     * Object that we will bind to the registry.
     * This object is a stub connected to our RMIServerImpl.
     **/
    private static RMIServer objectToBind(
            RMIServerImpl rmiServer, Map<String, ?> env)
        throws IOException {
        return (RMIServer)rmiServer.toStub();
    }

    private static RMIServerImpl newJRMPServer(Map<String, ?> env, int port)
            throws IOException {
        RMIClientSocketFactory csf = (RMIClientSocketFactory)
            env.get(RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE);
        RMIServerSocketFactory ssf = (RMIServerSocketFactory)
            env.get(RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE);
        return new RMIJRMPServerImpl(port, csf, ssf, env);
    }

    private static String byteArrayToBase64(byte[] a) {
        int aLen = a.length;
        int numFullGroups = aLen/3;
        int numBytesInPartialGroup = aLen - 3*numFullGroups;
        int resultLen = 4*((aLen + 2)/3);
        final StringBuilder result = new StringBuilder(resultLen);

        // Translate all full groups from byte array elements to Base64
        int inCursor = 0;
        for (int i=0; i<numFullGroups; i++) {
            int byte0 = a[inCursor++] & 0xff;
            int byte1 = a[inCursor++] & 0xff;
            int byte2 = a[inCursor++] & 0xff;
            result.append(intToAlpha[byte0 >> 2]);
            result.append(intToAlpha[(byte0 << 4)&0x3f | (byte1 >> 4)]);
            result.append(intToAlpha[(byte1 << 2)&0x3f | (byte2 >> 6)]);
            result.append(intToAlpha[byte2 & 0x3f]);
        }

        // Translate partial group if present
        if (numBytesInPartialGroup != 0) {
            int byte0 = a[inCursor++] & 0xff;
            result.append(intToAlpha[byte0 >> 2]);
            if (numBytesInPartialGroup == 1) {
                result.append(intToAlpha[(byte0 << 4) & 0x3f]);
                result.append("==");
            } else {
                // assert numBytesInPartialGroup == 2;
                int byte1 = a[inCursor++] & 0xff;
                result.append(intToAlpha[(byte0 << 4)&0x3f | (byte1 >> 4)]);
                result.append(intToAlpha[(byte1 << 2)&0x3f]);
                result.append('=');
            }
        }
        // assert inCursor == a.length;
        // assert result.length() == resultLen;
        return result.toString();
    }

    /**
     * This array is a lookup table that translates 6-bit positive integer
     * index values into their "Base64 Alphabet" equivalents as specified
     * in Table 1 of RFC 2045.
     */
    private static final char intToAlpha[] = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
    };

    /**
     * Construct a new IOException with a nested exception.
     * The nested exception is set only if JDK {@literal >= 1.4}
     */
    private static IOException newIOException(String message,
                                              Throwable cause) {
        final IOException x = new IOException(message);
        return EnvHelp.initCause(x,cause);
    }


    // Private variables
    // -----------------

    private static ClassLogger logger =
        new ClassLogger("javax.management.remote.rmi", "RMIConnectorServer");

    private JMXServiceURL address;
    private RMIServerImpl rmiServerImpl;
    private final Map<String, ?> attributes;
    private ClassLoader defaultClassLoader = null;

    private String boundJndiUrl;

    // state
    private static final int CREATED = 0;
    private static final int STARTED = 1;
    private static final int STOPPED = 2;

    private int state = CREATED;
    private static final Set<RMIConnectorServer> openedServers =
            new HashSet<RMIConnectorServer>();
}
