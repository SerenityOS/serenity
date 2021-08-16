/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.jmx.remote.internal.ClientCommunicatorAdmin;
import com.sun.jmx.remote.internal.ClientListenerInfo;
import com.sun.jmx.remote.internal.ClientNotifForwarder;
import com.sun.jmx.remote.internal.rmi.ProxyRef;
import com.sun.jmx.remote.util.ClassLogger;
import com.sun.jmx.remote.util.EnvHelp;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.lang.module.ModuleDescriptor;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.rmi.MarshalledObject;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.ServerException;
import java.rmi.UnmarshalException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RemoteObject;
import java.rmi.server.RemoteObjectInvocationHandler;
import java.rmi.server.RemoteRef;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.NotificationResult;
import javax.management.remote.JMXAddressable;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.security.auth.Subject;
import jdk.internal.module.Modules;
import sun.reflect.misc.ReflectUtil;
import sun.rmi.server.UnicastRef2;
import sun.rmi.transport.LiveRef;
import java.io.NotSerializableException;

import static java.lang.module.ModuleDescriptor.Modifier.SYNTHETIC;

/**
 * <p>A connection to a remote RMI connector.  Usually, such
 * connections are made using {@link
 * javax.management.remote.JMXConnectorFactory JMXConnectorFactory}.
 * However, specialized applications can use this class directly, for
 * example with an {@link RMIServer} stub obtained without going
 * through JNDI.</p>
 *
 * @since 1.5
 */
public class RMIConnector implements JMXConnector, Serializable, JMXAddressable {

    private static final ClassLogger logger =
            new ClassLogger("javax.management.remote.rmi", "RMIConnector");

    private static final long serialVersionUID = 817323035842634473L;

    static final class Util {
        private Util() {}

        /* This method can be used by code that is deliberately violating the
         * allowed checked casts.  Rather than marking the whole method containing
         * the code with @SuppressWarnings, you can use a call to this method for
         * the exact place where you need to escape the constraints.  Typically
         * you will "import static" this method and then write either
         *    X x = cast(y);
         * or, if that doesn't work (e.g. X is a type variable)
         *    Util.<X>cast(y);
         */
        @SuppressWarnings("unchecked")
        public static <T> T cast(Object x) {
            return (T) x;
        }
    }

    private RMIConnector(RMIServer rmiServer, JMXServiceURL address,
            Map<String, ?> environment) {
        if (rmiServer == null && address == null) throw new
                IllegalArgumentException("rmiServer and jmxServiceURL both null");
        initTransients();

        this.rmiServer = rmiServer;
        this.jmxServiceURL = address;
        if (environment == null) {
            this.env = Collections.emptyMap();
        } else {
            EnvHelp.checkAttributes(environment);
            this.env = Collections.unmodifiableMap(environment);
        }
    }

    /**
     * <p>Constructs an {@code RMIConnector} that will connect
     * the RMI connector server with the given address.</p>
     *
     * <p>The address can refer directly to the connector server,
     * using the following syntax:</p>
     *
     * <pre>
     * service:jmx:rmi://<em>[host[:port]]</em>/stub/<em>encoded-stub</em>
     * </pre>
     *
     * <p>(Here, the square brackets {@code []} are not part of the
     * address but indicate that the host and port are optional.)</p>
     *
     * <p>The address can instead indicate where to find an RMI stub
     * through JNDI, using the following syntax:</p>
     *
     * <pre>
     * service:jmx:rmi://<em>[host[:port]]</em>/jndi/<em>jndi-name</em>
     * </pre>
     *
     * <p>An implementation may also recognize additional address
     * syntaxes, for example:</p>
     *
     * <pre>
     * service:jmx:iiop://<em>[host[:port]]</em>/stub/<em>encoded-stub</em>
     * </pre>
     *
     * @param url the address of the RMI connector server.
     *
     * @param environment additional attributes specifying how to make
     * the connection.  For JNDI-based addresses, these attributes can
     * usefully include JNDI attributes recognized by {@link
     * InitialContext#InitialContext(Hashtable) InitialContext}.  This
     * parameter can be null, which is equivalent to an empty Map.
     *
     * @exception IllegalArgumentException if {@code url}
     * is null.
     */
    public RMIConnector(JMXServiceURL url, Map<String,?> environment) {
        this(null, url, environment);
    }

    /**
     * <p>Constructs an {@code RMIConnector} using the given RMI stub.
     *
     * @param rmiServer an RMI stub representing the RMI connector server.
     * @param environment additional attributes specifying how to make
     * the connection.  This parameter can be null, which is
     * equivalent to an empty Map.
     *
     * @exception IllegalArgumentException if {@code rmiServer}
     * is null.
     */
    public RMIConnector(RMIServer rmiServer, Map<String,?> environment) {
        this(rmiServer, null, environment);
    }

    /**
     * <p>Returns a string representation of this object.  In general,
     * the {@code toString} method returns a string that
     * "textually represents" this object. The result should be a
     * concise but informative representation that is easy for a
     * person to read.</p>
     *
     * @return a String representation of this object.
     **/
    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder(this.getClass().getName());
        b.append(":");
        if (rmiServer != null) {
            b.append(" rmiServer=").append(rmiServer.toString());
        }
        if (jmxServiceURL != null) {
            if (rmiServer!=null) b.append(",");
            b.append(" jmxServiceURL=").append(jmxServiceURL.toString());
        }
        return b.toString();
    }

    /**
     * <p>The address of this connector.</p>
     *
     * @return the address of this connector, or null if it
     * does not have one.
     *
     * @since 1.6
     */
    public JMXServiceURL getAddress() {
        return jmxServiceURL;
    }

    //--------------------------------------------------------------------
    // implements JMXConnector interface
    //--------------------------------------------------------------------

    /**
     * @throws IOException if the connection could not be made because of a
     *   communication problem
     */
    public void connect() throws IOException {
        connect(null);
    }

    /**
     * @throws IOException if the connection could not be made because of a
     *   communication problem
     */
    public synchronized void connect(Map<String,?> environment)
    throws IOException {
        final boolean tracing = logger.traceOn();
        String        idstr   = (tracing?"["+this.toString()+"]":null);

        if (terminated) {
            logger.trace("connect",idstr + " already closed.");
            throw new IOException("Connector closed");
        }
        if (connected) {
            logger.trace("connect",idstr + " already connected.");
            return;
        }

        try {
            if (tracing) logger.trace("connect",idstr + " connecting...");

            final Map<String, Object> usemap =
                    new HashMap<String, Object>((this.env==null) ?
                        Collections.<String, Object>emptyMap() : this.env);


            if (environment != null) {
                EnvHelp.checkAttributes(environment);
                usemap.putAll(environment);
            }

            // Get RMIServer stub from directory or URL encoding if needed.
            if (tracing) logger.trace("connect",idstr + " finding stub...");
            RMIServer stub = (rmiServer!=null)?rmiServer:
                findRMIServer(jmxServiceURL, usemap);

            // Check for secure RMIServer stub if the corresponding
            // client-side environment property is set to "true".
            //
            String stringBoolean =  (String) usemap.get("jmx.remote.x.check.stub");
            boolean checkStub = EnvHelp.computeBooleanFromString(stringBoolean);

            if (checkStub) checkStub(stub, rmiServerImplStubClass);

            if (tracing) logger.trace("connect",idstr + " connecting stub...");
            idstr = (tracing?"["+this.toString()+"]":null);

            // Calling newClient on the RMIServer stub.
            if (tracing)
                logger.trace("connect",idstr + " getting connection...");
            Object credentials = usemap.get(CREDENTIALS);

            try {
                connection = getConnection(stub, credentials, checkStub);
            } catch (java.rmi.RemoteException re) {
                throw re;
            }

            // Always use one of:
            //   ClassLoader provided in Map at connect time,
            //   or contextClassLoader at connect time.
            if (tracing)
                logger.trace("connect",idstr + " getting class loader...");
            defaultClassLoader = EnvHelp.resolveClientClassLoader(usemap);

            usemap.put(JMXConnectorFactory.DEFAULT_CLASS_LOADER,
                    defaultClassLoader);

            rmiNotifClient = new RMINotifClient(defaultClassLoader, usemap);

            env = usemap;
            final long checkPeriod = EnvHelp.getConnectionCheckPeriod(usemap);
            communicatorAdmin = new RMIClientCommunicatorAdmin(checkPeriod);

            connected = true;

            // The connectionId variable is used in doStart(), when
            // reconnecting, to identify the "old" connection.
            //
            connectionId = getConnectionId();

            Notification connectedNotif =
                    new JMXConnectionNotification(JMXConnectionNotification.OPENED,
                    this,
                    connectionId,
                    clientNotifSeqNo++,
                    "Successful connection",
                    null);
            sendNotification(connectedNotif);

            if (tracing) logger.trace("connect",idstr + " done...");
        } catch (IOException e) {
            if (tracing)
                logger.trace("connect",idstr + " failed to connect: " + e);
            throw e;
        } catch (RuntimeException e) {
            if (tracing)
                logger.trace("connect",idstr + " failed to connect: " + e);
            throw e;
        } catch (NamingException e) {
            final String msg = "Failed to retrieve RMIServer stub: " + e;
            if (tracing) logger.trace("connect",idstr + " " + msg);
            throw EnvHelp.initCause(new IOException(msg),e);
        }
    }

    public synchronized String getConnectionId() throws IOException {
        if (terminated || !connected) {
            if (logger.traceOn())
                logger.trace("getConnectionId","["+this.toString()+
                        "] not connected.");

            throw new IOException("Not connected");
        }

        // we do a remote call to have an IOException if the connection is broken.
        // see the bug 4939578
        return connection.getConnectionId();
    }

    public synchronized MBeanServerConnection getMBeanServerConnection()
    throws IOException {
        return getMBeanServerConnection(null);
    }

    public synchronized MBeanServerConnection
            getMBeanServerConnection(Subject delegationSubject)
            throws IOException {

        if (terminated) {
            if (logger.traceOn())
                logger.trace("getMBeanServerConnection","[" + this.toString() +
                        "] already closed.");
            throw new IOException("Connection closed");
        } else if (!connected) {
            if (logger.traceOn())
                logger.trace("getMBeanServerConnection","[" + this.toString() +
                        "] is not connected.");
            throw new IOException("Not connected");
        }

        return getConnectionWithSubject(delegationSubject);
    }

    public void
            addConnectionNotificationListener(NotificationListener listener,
            NotificationFilter filter,
            Object handback) {
        if (listener == null)
            throw new NullPointerException("listener");
        connectionBroadcaster.addNotificationListener(listener, filter,
                handback);
    }

    public void
            removeConnectionNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException {
        if (listener == null)
            throw new NullPointerException("listener");
        connectionBroadcaster.removeNotificationListener(listener);
    }

    public void
            removeConnectionNotificationListener(NotificationListener listener,
            NotificationFilter filter,
            Object handback)
            throws ListenerNotFoundException {
        if (listener == null)
            throw new NullPointerException("listener");
        connectionBroadcaster.removeNotificationListener(listener, filter,
                handback);
    }

    private void sendNotification(Notification n) {
        connectionBroadcaster.sendNotification(n);
    }

    public synchronized void close() throws IOException {
        close(false);
    }

    // allows to do close after setting the flag "terminated" to true.
    // It is necessary to avoid a deadlock, see 6296324
    private synchronized void close(boolean intern) throws IOException {
        final boolean tracing = logger.traceOn();
        final boolean debug   = logger.debugOn();
        final String  idstr   = (tracing?"["+this.toString()+"]":null);

        if (!intern) {
            // Return if already cleanly closed.
            //
            if (terminated) {
                if (closeException == null) {
                    if (tracing) logger.trace("close",idstr + " already closed.");
                    return;
                }
            } else {
                terminated = true;
            }
        }

        if (closeException != null && tracing) {
            // Already closed, but not cleanly. Attempt again.
            //
            if (tracing) {
                logger.trace("close",idstr + " had failed: " + closeException);
                logger.trace("close",idstr + " attempting to close again.");
            }
        }

        String savedConnectionId = null;
        if (connected) {
            savedConnectionId = connectionId;
        }

        closeException = null;

        if (tracing) logger.trace("close",idstr + " closing.");

        if (communicatorAdmin != null) {
            communicatorAdmin.terminate();
        }

        if (rmiNotifClient != null) {
            try {
                rmiNotifClient.terminate();
                if (tracing) logger.trace("close",idstr +
                        " RMI Notification client terminated.");
            } catch (RuntimeException x) {
                closeException = x;
                if (tracing) logger.trace("close",idstr +
                        " Failed to terminate RMI Notification client: " + x);
                if (debug) logger.debug("close",x);
            }
        }

        if (connection != null) {
            try {
                connection.close();
                if (tracing) logger.trace("close",idstr + " closed.");
            } catch (NoSuchObjectException nse) {
                // OK, the server maybe closed itself.
            } catch (IOException e) {
                closeException = e;
                if (tracing) logger.trace("close",idstr +
                        " Failed to close RMIServer: " + e);
                if (debug) logger.debug("close",e);
            }
        }

        // Clean up MBeanServerConnection table
        //
        rmbscMap.clear();

        /* Send notification of closure.  We don't do this if the user
         * never called connect() on the connector, because there's no
         * connection id in that case.  */

        if (savedConnectionId != null) {
            Notification closedNotif =
                    new JMXConnectionNotification(JMXConnectionNotification.CLOSED,
                    this,
                    savedConnectionId,
                    clientNotifSeqNo++,
                    "Client has been closed",
                    null);
            sendNotification(closedNotif);
        }

        // throw exception if needed
        //
        if (closeException != null) {
            if (tracing) logger.trace("close",idstr + " failed to close: " +
                    closeException);
            if (closeException instanceof IOException)
                throw (IOException) closeException;
            if (closeException instanceof RuntimeException)
                throw (RuntimeException) closeException;
            final IOException x =
                    new IOException("Failed to close: " + closeException);
            throw EnvHelp.initCause(x,closeException);
        }
    }

    // added for re-connection
    private Integer addListenerWithSubject(ObjectName name,
                                           MarshalledObject<NotificationFilter> filter,
                                           Subject delegationSubject,
                                           boolean reconnect)
        throws InstanceNotFoundException, IOException {

        final boolean debug = logger.debugOn();
        if (debug)
            logger.debug("addListenerWithSubject",
                    "(ObjectName,MarshalledObject,Subject)");

        final ObjectName[] names = new ObjectName[] {name};
        final MarshalledObject<NotificationFilter>[] filters =
                Util.cast(new MarshalledObject<?>[] {filter});
        final Subject[] delegationSubjects = new Subject[] {
            delegationSubject
        };

        final Integer[] listenerIDs =
                addListenersWithSubjects(names,filters,delegationSubjects,
                reconnect);

        if (debug) logger.debug("addListenerWithSubject","listenerID="
                + listenerIDs[0]);
        return listenerIDs[0];
    }

    // added for re-connection
    private Integer[] addListenersWithSubjects(ObjectName[]       names,
                             MarshalledObject<NotificationFilter>[] filters,
                             Subject[]          delegationSubjects,
                             boolean            reconnect)
        throws InstanceNotFoundException, IOException {

        final boolean debug = logger.debugOn();
        if (debug)
            logger.debug("addListenersWithSubjects",
                    "(ObjectName[],MarshalledObject[],Subject[])");

        final ClassLoader old = pushDefaultClassLoader();
        Integer[] listenerIDs = null;

        try {
            listenerIDs = connection.addNotificationListeners(names,
                    filters,
                    delegationSubjects);
        } catch (NoSuchObjectException noe) {
            // maybe reconnect
            if (reconnect) {
                communicatorAdmin.gotIOException(noe);

                listenerIDs = connection.addNotificationListeners(names,
                        filters,
                        delegationSubjects);
            } else {
                throw noe;
            }
        } catch (IOException ioe) {
            // send a failed notif if necessary
            communicatorAdmin.gotIOException(ioe);
        } finally {
            popDefaultClassLoader(old);
        }

        if (debug) logger.debug("addListenersWithSubjects","registered "
                + ((listenerIDs==null)?0:listenerIDs.length)
                + " listener(s)");
        return listenerIDs;
    }

    //--------------------------------------------------------------------
    // Implementation of MBeanServerConnection
    //--------------------------------------------------------------------
    private class RemoteMBeanServerConnection implements MBeanServerConnection {
        private Subject delegationSubject;

        public RemoteMBeanServerConnection() {
            this(null);
        }

        public RemoteMBeanServerConnection(Subject delegationSubject) {
            this.delegationSubject = delegationSubject;
        }

        public ObjectInstance createMBean(String className,
                ObjectName name)
                throws ReflectionException,
                InstanceAlreadyExistsException,
                MBeanRegistrationException,
                MBeanException,
                NotCompliantMBeanException,
                IOException {
            if (logger.debugOn())
                logger.debug("createMBean(String,ObjectName)",
                        "className=" + className + ", name=" +
                        name);

            final ClassLoader old = pushDefaultClassLoader();
            try {
                return connection.createMBean(className,
                        name,
                        delegationSubject);
            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                return connection.createMBean(className,
                        name,
                        delegationSubject);
            } finally {
                popDefaultClassLoader(old);
            }
        }

        public ObjectInstance createMBean(String className,
                ObjectName name,
                ObjectName loaderName)
                throws ReflectionException,
                InstanceAlreadyExistsException,
                MBeanRegistrationException,
                MBeanException,
                NotCompliantMBeanException,
                InstanceNotFoundException,
                IOException {

            if (logger.debugOn())
                logger.debug("createMBean(String,ObjectName,ObjectName)",
                        "className=" + className + ", name="
                        + name + ", loaderName="
                        + loaderName + ")");

            final ClassLoader old = pushDefaultClassLoader();
            try {
                return connection.createMBean(className,
                        name,
                        loaderName,
                        delegationSubject);

            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                return connection.createMBean(className,
                        name,
                        loaderName,
                        delegationSubject);

            } finally {
                popDefaultClassLoader(old);
            }
        }

        public ObjectInstance createMBean(String className,
                ObjectName name,
                Object params[],
                String signature[])
                throws ReflectionException,
                InstanceAlreadyExistsException,
                MBeanRegistrationException,
                MBeanException,
                NotCompliantMBeanException,
                IOException {
            if (logger.debugOn())
                logger.debug("createMBean(String,ObjectName,Object[],String[])",
                        "className=" + className + ", name="
                        + name + ", signature=" + strings(signature));

            final MarshalledObject<Object[]> sParams =
                    new MarshalledObject<Object[]>(params);
            final ClassLoader old = pushDefaultClassLoader();
            try {
                return connection.createMBean(className,
                        name,
                        sParams,
                        signature,
                        delegationSubject);
            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                return connection.createMBean(className,
                        name,
                        sParams,
                        signature,
                        delegationSubject);
            } finally {
                popDefaultClassLoader(old);
            }
        }

        public ObjectInstance createMBean(String className,
                ObjectName name,
                ObjectName loaderName,
                Object params[],
                String signature[])
                throws ReflectionException,
                InstanceAlreadyExistsException,
                MBeanRegistrationException,
                MBeanException,
                NotCompliantMBeanException,
                InstanceNotFoundException,
                IOException {
            if (logger.debugOn()) logger.debug(
                    "createMBean(String,ObjectName,ObjectName,Object[],String[])",
                    "className=" + className + ", name=" + name + ", loaderName="
                    + loaderName + ", signature=" + strings(signature));

            final MarshalledObject<Object[]> sParams =
                    new MarshalledObject<Object[]>(params);
            final ClassLoader old = pushDefaultClassLoader();
            try {
                return connection.createMBean(className,
                        name,
                        loaderName,
                        sParams,
                        signature,
                        delegationSubject);
            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                return connection.createMBean(className,
                        name,
                        loaderName,
                        sParams,
                        signature,
                        delegationSubject);
            } finally {
                popDefaultClassLoader(old);
            }
        }

        public void unregisterMBean(ObjectName name)
        throws InstanceNotFoundException,
                MBeanRegistrationException,
                IOException {
            if (logger.debugOn())
                logger.debug("unregisterMBean", "name=" + name);

            final ClassLoader old = pushDefaultClassLoader();
            try {
                connection.unregisterMBean(name, delegationSubject);
            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                connection.unregisterMBean(name, delegationSubject);
            } finally {
                popDefaultClassLoader(old);
            }
        }

        public ObjectInstance getObjectInstance(ObjectName name)
        throws InstanceNotFoundException,
                IOException {
            if (logger.debugOn())
                logger.debug("getObjectInstance", "name=" + name);

            final ClassLoader old = pushDefaultClassLoader();
            try {
                return connection.getObjectInstance(name, delegationSubject);
            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                return connection.getObjectInstance(name, delegationSubject);
            } finally {
                popDefaultClassLoader(old);
            }
        }

        public Set<ObjectInstance> queryMBeans(ObjectName name,
                QueryExp query)
                throws IOException {
            if (logger.debugOn()) logger.debug("queryMBeans",
                    "name=" + name + ", query=" + query);

            final MarshalledObject<QueryExp> sQuery =
                    new MarshalledObject<QueryExp>(query);
            final ClassLoader old = pushDefaultClassLoader();
            try {
                return connection.queryMBeans(name, sQuery, delegationSubject);
            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                return connection.queryMBeans(name, sQuery, delegationSubject);
            } finally {
                popDefaultClassLoader(old);
            }
        }

        public Set<ObjectName> queryNames(ObjectName name,
                QueryExp query)
                throws IOException {
            if (logger.debugOn()) logger.debug("queryNames",
                    "name=" + name + ", query=" + query);

            final MarshalledObject<QueryExp> sQuery =
                    new MarshalledObject<QueryExp>(query);
            final ClassLoader old = pushDefaultClassLoader();
            try {
                return connection.queryNames(name, sQuery, delegationSubject);
            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                return connection.queryNames(name, sQuery, delegationSubject);
            } finally {
                popDefaultClassLoader(old);
            }
        }

        public boolean isRegistered(ObjectName name)
        throws IOException {
            if (logger.debugOn())
                logger.debug("isRegistered", "name=" + name);

            final ClassLoader old = pushDefaultClassLoader();
            try {
                return connection.isRegistered(name, delegationSubject);
            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                return connection.isRegistered(name, delegationSubject);
            } finally {
                popDefaultClassLoader(old);
            }
        }

        public Integer getMBeanCount()
        throws IOException {
            if (logger.debugOn()) logger.debug("getMBeanCount", "");

            final ClassLoader old = pushDefaultClassLoader();
            try {
                return connection.getMBeanCount(delegationSubject);
            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                return connection.getMBeanCount(delegationSubject);
            } finally {
                popDefaultClassLoader(old);
            }
        }

        public Object getAttribute(ObjectName name,
                String attribute)
                throws MBeanException,
                AttributeNotFoundException,
                InstanceNotFoundException,
                ReflectionException,
                IOException {
            if (logger.debugOn()) logger.debug("getAttribute",
                    "name=" + name + ", attribute="
                    + attribute);

            final ClassLoader old = pushDefaultClassLoader();
            try {
                return connection.getAttribute(name,
                        attribute,
                        delegationSubject);
            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                return connection.getAttribute(name,
                        attribute,
                        delegationSubject);
            } finally {
                popDefaultClassLoader(old);
            }
        }

        public AttributeList getAttributes(ObjectName name,
                String[] attributes)
                throws InstanceNotFoundException,
                ReflectionException,
                IOException {
            if (logger.debugOn()) logger.debug("getAttributes",
                    "name=" + name + ", attributes="
                    + strings(attributes));

            final ClassLoader old = pushDefaultClassLoader();
            try {
                return connection.getAttributes(name,
                        attributes,
                        delegationSubject);

            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                return connection.getAttributes(name,
                        attributes,
                        delegationSubject);
            } finally {
                popDefaultClassLoader(old);
            }
        }


        public void setAttribute(ObjectName name,
                Attribute attribute)
                throws InstanceNotFoundException,
                AttributeNotFoundException,
                InvalidAttributeValueException,
                MBeanException,
                ReflectionException,
                IOException {

            if (logger.debugOn()) logger.debug("setAttribute",
                    "name=" + name + ", attribute name="
                    + attribute.getName());

            final MarshalledObject<Attribute> sAttribute =
                    new MarshalledObject<Attribute>(attribute);
            final ClassLoader old = pushDefaultClassLoader();
            try {
                connection.setAttribute(name, sAttribute, delegationSubject);
            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                connection.setAttribute(name, sAttribute, delegationSubject);
            } finally {
                popDefaultClassLoader(old);
            }
        }

        public AttributeList setAttributes(ObjectName name,
                AttributeList attributes)
                throws InstanceNotFoundException,
                ReflectionException,
                IOException {

            if (logger.debugOn()) {
                logger.debug("setAttributes",
                    "name=" + name + ", attribute names="
                    + getAttributesNames(attributes));
            }

            final MarshalledObject<AttributeList> sAttributes =
                    new MarshalledObject<AttributeList>(attributes);
            final ClassLoader old = pushDefaultClassLoader();
            try {
                return connection.setAttributes(name,
                        sAttributes,
                        delegationSubject);
            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                return connection.setAttributes(name,
                        sAttributes,
                        delegationSubject);
            } finally {
                popDefaultClassLoader(old);
            }
        }


        public Object invoke(ObjectName name,
                String operationName,
                Object params[],
                String signature[])
                throws InstanceNotFoundException,
                MBeanException,
                ReflectionException,
                IOException {

            if (logger.debugOn()) logger.debug("invoke",
                    "name=" + name
                    + ", operationName=" + operationName
                    + ", signature=" + strings(signature));

            final MarshalledObject<Object[]> sParams =
                    new MarshalledObject<Object[]>(params);
            final ClassLoader old = pushDefaultClassLoader();
            try {
                return connection.invoke(name,
                        operationName,
                        sParams,
                        signature,
                        delegationSubject);
            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                return connection.invoke(name,
                        operationName,
                        sParams,
                        signature,
                        delegationSubject);
            } finally {
                popDefaultClassLoader(old);
            }
        }


        public String getDefaultDomain()
        throws IOException {
            if (logger.debugOn()) logger.debug("getDefaultDomain", "");

            final ClassLoader old = pushDefaultClassLoader();
            try {
                return connection.getDefaultDomain(delegationSubject);
            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                return connection.getDefaultDomain(delegationSubject);
            } finally {
                popDefaultClassLoader(old);
            }
        }

        public String[] getDomains() throws IOException {
            if (logger.debugOn()) logger.debug("getDomains", "");

            final ClassLoader old = pushDefaultClassLoader();
            try {
                return connection.getDomains(delegationSubject);
            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                return connection.getDomains(delegationSubject);
            } finally {
                popDefaultClassLoader(old);
            }
        }

        public MBeanInfo getMBeanInfo(ObjectName name)
        throws InstanceNotFoundException,
                IntrospectionException,
                ReflectionException,
                IOException {

            if (logger.debugOn()) logger.debug("getMBeanInfo", "name=" + name);
            final ClassLoader old = pushDefaultClassLoader();
            try {
                return connection.getMBeanInfo(name, delegationSubject);
            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                return connection.getMBeanInfo(name, delegationSubject);
            } finally {
                popDefaultClassLoader(old);
            }
        }


        public boolean isInstanceOf(ObjectName name,
                String className)
                throws InstanceNotFoundException,
                IOException {
            if (logger.debugOn())
                logger.debug("isInstanceOf", "name=" + name +
                        ", className=" + className);

            final ClassLoader old = pushDefaultClassLoader();
            try {
                return connection.isInstanceOf(name,
                        className,
                        delegationSubject);
            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                return connection.isInstanceOf(name,
                        className,
                        delegationSubject);
            } finally {
                popDefaultClassLoader(old);
            }
        }

        public void addNotificationListener(ObjectName name,
                ObjectName listener,
                NotificationFilter filter,
                Object handback)
                throws InstanceNotFoundException,
                IOException {

            if (logger.debugOn())
                logger.debug("addNotificationListener" +
                        "(ObjectName,ObjectName,NotificationFilter,Object)",
                        "name=" + name + ", listener=" + listener
                        + ", filter=" + filter + ", handback=" + handback);

            final MarshalledObject<NotificationFilter> sFilter =
                    new MarshalledObject<NotificationFilter>(filter);
            final MarshalledObject<Object> sHandback =
                    new MarshalledObject<Object>(handback);
            final ClassLoader old = pushDefaultClassLoader();
            try {
                connection.addNotificationListener(name,
                        listener,
                        sFilter,
                        sHandback,
                        delegationSubject);
            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                connection.addNotificationListener(name,
                        listener,
                        sFilter,
                        sHandback,
                        delegationSubject);
            } finally {
                popDefaultClassLoader(old);
            }
        }

        public void removeNotificationListener(ObjectName name,
                ObjectName listener)
                throws InstanceNotFoundException,
                ListenerNotFoundException,
                IOException {

            if (logger.debugOn()) logger.debug("removeNotificationListener" +
                    "(ObjectName,ObjectName)",
                    "name=" + name
                    + ", listener=" + listener);

            final ClassLoader old = pushDefaultClassLoader();
            try {
                connection.removeNotificationListener(name,
                        listener,
                        delegationSubject);
            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                connection.removeNotificationListener(name,
                        listener,
                        delegationSubject);
            } finally {
                popDefaultClassLoader(old);
            }
        }

        public void removeNotificationListener(ObjectName name,
                ObjectName listener,
                NotificationFilter filter,
                Object handback)
                throws InstanceNotFoundException,
                ListenerNotFoundException,
                IOException {
            if (logger.debugOn())
                logger.debug("removeNotificationListener" +
                        "(ObjectName,ObjectName,NotificationFilter,Object)",
                        "name=" + name
                        + ", listener=" + listener
                        + ", filter=" + filter
                        + ", handback=" + handback);

            final MarshalledObject<NotificationFilter> sFilter =
                    new MarshalledObject<NotificationFilter>(filter);
            final MarshalledObject<Object> sHandback =
                    new MarshalledObject<Object>(handback);
            final ClassLoader old = pushDefaultClassLoader();
            try {
                connection.removeNotificationListener(name,
                        listener,
                        sFilter,
                        sHandback,
                        delegationSubject);
            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                connection.removeNotificationListener(name,
                        listener,
                        sFilter,
                        sHandback,
                        delegationSubject);
            } finally {
                popDefaultClassLoader(old);
            }
        }

        // Specific Notification Handle ----------------------------------

        public void addNotificationListener(ObjectName name,
                NotificationListener listener,
                NotificationFilter filter,
                Object handback)
                throws InstanceNotFoundException,
                IOException {

            final boolean debug = logger.debugOn();

            if (debug)
                logger.debug("addNotificationListener" +
                        "(ObjectName,NotificationListener,"+
                        "NotificationFilter,Object)",
                        "name=" + name
                        + ", listener=" + listener
                        + ", filter=" + filter
                        + ", handback=" + handback);

            final Integer listenerID =
                    addListenerWithSubject(name,
                    new MarshalledObject<NotificationFilter>(filter),
                    delegationSubject,true);
            rmiNotifClient.addNotificationListener(listenerID, name, listener,
                    filter, handback,
                    delegationSubject);
        }

        public void removeNotificationListener(ObjectName name,
                NotificationListener listener)
                throws InstanceNotFoundException,
                ListenerNotFoundException,
                IOException {

            final boolean debug = logger.debugOn();

            if (debug) logger.debug("removeNotificationListener"+
                    "(ObjectName,NotificationListener)",
                    "name=" + name
                    + ", listener=" + listener);

            final Integer[] ret =
                    rmiNotifClient.getListenerIds(name, listener);

            if (debug) logger.debug("removeNotificationListener",
                    "listenerIDs=" + objects(ret));

            final ClassLoader old = pushDefaultClassLoader();

            try {
                connection.removeNotificationListeners(name,
                        ret,
                        delegationSubject);
            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                connection.removeNotificationListeners(name,
                        ret,
                        delegationSubject);
            } finally {
                popDefaultClassLoader(old);
            }
            rmiNotifClient.removeNotificationListener(name, listener);
        }

        public void removeNotificationListener(ObjectName name,
                NotificationListener listener,
                NotificationFilter filter,
                Object handback)
                throws InstanceNotFoundException,
                ListenerNotFoundException,
                IOException {
            final boolean debug = logger.debugOn();

            if (debug)
                logger.debug("removeNotificationListener"+
                        "(ObjectName,NotificationListener,"+
                        "NotificationFilter,Object)",
                        "name=" + name
                        + ", listener=" + listener
                        + ", filter=" + filter
                        + ", handback=" + handback);

            final Integer ret =
                    rmiNotifClient.getListenerId(name, listener,
                    filter, handback);

            if (debug) logger.debug("removeNotificationListener",
                    "listenerID=" + ret);

            final ClassLoader old = pushDefaultClassLoader();
            try {
                connection.removeNotificationListeners(name,
                        new Integer[] {ret},
                        delegationSubject);
            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                connection.removeNotificationListeners(name,
                        new Integer[] {ret},
                        delegationSubject);
            } finally {
                popDefaultClassLoader(old);
            }
            rmiNotifClient.removeNotificationListener(name, listener,
                    filter, handback);
        }
    }

    //--------------------------------------------------------------------
    private class RMINotifClient extends ClientNotifForwarder {
        public RMINotifClient(ClassLoader cl, Map<String, ?> env) {
            super(cl, env);
        }

        protected NotificationResult fetchNotifs(long clientSequenceNumber,
                int maxNotifications,
                long timeout)
                throws IOException, ClassNotFoundException {

            boolean retried = false;
            while (true) { // used for a successful re-connection
                           // or a transient network problem
                try {
                    return connection.fetchNotifications(clientSequenceNumber,
                            maxNotifications,
                            timeout); // return normally
                } catch (IOException ioe) {
                    // Examine the chain of exceptions to determine whether this
                    // is a deserialization issue. If so - we propagate the
                    // appropriate exception to the caller, who will then
                    // proceed with fetching notifications one by one
                    rethrowDeserializationException(ioe);

                    try {
                        communicatorAdmin.gotIOException(ioe);
                        // reconnection OK, back to "while" to do again
                    } catch (IOException ee) {
                        boolean toClose = false;

                        synchronized (this) {
                            if (terminated) {
                                // the connection is closed.
                                throw ioe;
                            } else if (retried) {
                                toClose = true;
                            }
                        }

                        if (toClose) {
                            // JDK-8049303
                            // We received an IOException - but the communicatorAdmin
                            // did not close the connection - possibly because
                            // the original exception was raised by a transient network
                            // problem?
                            // We already know that this exception is not due to a deserialization
                            // issue as we already took care of that before involving the
                            // communicatorAdmin. Moreover - we already made one retry attempt
                            // at fetching the same batch of notifications - and the
                            // problem persisted.
                            // Since trying again doesn't seem to solve the issue, we will now
                            // close the connection. Doing otherwise might cause the
                            // NotifFetcher thread to die silently.
                            final Notification failedNotif =
                                    new JMXConnectionNotification(
                                    JMXConnectionNotification.FAILED,
                                    this,
                                    connectionId,
                                    clientNotifSeqNo++,
                                    "Failed to communicate with the server: " + ioe.toString(),
                                    ioe);

                            sendNotification(failedNotif);

                            try {
                                close(true);
                            } catch (Exception e) {
                                // OK.
                                // We are closing
                            }
                            throw ioe; // the connection is closed here.
                        } else {
                            // JDK-8049303 possible transient network problem,
                            // let's try one more time
                            retried = true;
                        }
                    }
                }
            }
        }

        private void rethrowDeserializationException(IOException ioe)
                throws ClassNotFoundException, IOException {
            // specially treating for an UnmarshalException
            if (ioe instanceof UnmarshalException) {
                NotSerializableException nse = new NotSerializableException();
                nse.initCause(ioe);
                throw nse; // the fix of 6937053 made ClientNotifForwarder.fetchNotifs
                           // fetch one by one with UnmarshalException
            }

            // Not serialization problem, return.
        }

        protected Integer addListenerForMBeanRemovedNotif()
        throws IOException, InstanceNotFoundException {
            NotificationFilterSupport clientFilter =
                    new NotificationFilterSupport();
            clientFilter.enableType(
                    MBeanServerNotification.UNREGISTRATION_NOTIFICATION);
            MarshalledObject<NotificationFilter> sFilter =
                new MarshalledObject<NotificationFilter>(clientFilter);

            Integer[] listenerIDs;
            final ObjectName[] names =
                new ObjectName[] {MBeanServerDelegate.DELEGATE_NAME};
            final MarshalledObject<NotificationFilter>[] filters =
                Util.cast(new MarshalledObject<?>[] {sFilter});
            final Subject[] subjects = new Subject[] {null};
            try {
                listenerIDs =
                        connection.addNotificationListeners(names,
                        filters,
                        subjects);

            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                listenerIDs =
                        connection.addNotificationListeners(names,
                        filters,
                        subjects);
            }
            return listenerIDs[0];
        }

        protected void removeListenerForMBeanRemovedNotif(Integer id)
        throws IOException, InstanceNotFoundException,
                ListenerNotFoundException {
            try {
                connection.removeNotificationListeners(
                        MBeanServerDelegate.DELEGATE_NAME,
                        new Integer[] {id},
                        null);
            } catch (IOException ioe) {
                communicatorAdmin.gotIOException(ioe);

                connection.removeNotificationListeners(
                        MBeanServerDelegate.DELEGATE_NAME,
                        new Integer[] {id},
                        null);
            }

        }

        protected void lostNotifs(String message, long number) {
            final String notifType = JMXConnectionNotification.NOTIFS_LOST;

            final JMXConnectionNotification n =
                new JMXConnectionNotification(notifType,
                                              RMIConnector.this,
                                              connectionId,
                                              clientNotifCounter++,
                                              message,
                                              Long.valueOf(number));
            sendNotification(n);
        }
    }

    private class RMIClientCommunicatorAdmin extends ClientCommunicatorAdmin {
        public RMIClientCommunicatorAdmin(long period) {
            super(period);
        }

        @Override
        public void gotIOException(IOException ioe) throws IOException {
            if (ioe instanceof NoSuchObjectException) {
                // need to restart
                super.gotIOException(ioe);

                return;
            }

            // check if the connection is broken
            try {
                connection.getDefaultDomain(null);
            } catch (IOException ioexc) {
                boolean toClose = false;

                synchronized(this) {
                    if (!terminated) {
                        terminated = true;

                        toClose = true;
                    }
                }

                if (toClose) {
                    // we should close the connection,
                    // but send a failed notif at first
                    final Notification failedNotif =
                            new JMXConnectionNotification(
                            JMXConnectionNotification.FAILED,
                            this,
                            connectionId,
                            clientNotifSeqNo++,
                            "Failed to communicate with the server: "+ioe.toString(),
                            ioe);

                    sendNotification(failedNotif);

                    try {
                        close(true);
                    } catch (Exception e) {
                        // OK.
                        // We are closing
                    }
                }
            }

            // forward the exception
            if (ioe instanceof ServerException) {
                /* Need to unwrap the exception.
                   Some user-thrown exception at server side will be wrapped by
                   rmi into a ServerException.
                   For example, a RMIConnnectorServer will wrap a
                   ClassNotFoundException into a UnmarshalException, and rmi
                   will throw a ServerException at client side which wraps this
                   UnmarshalException.
                   No failed notif here.
                 */
                Throwable tt = ((ServerException)ioe).detail;

                if (tt instanceof IOException) {
                    throw (IOException)tt;
                } else if (tt instanceof RuntimeException) {
                    throw (RuntimeException)tt;
                }
            }

            throw ioe;
        }

        public void reconnectNotificationListeners(ClientListenerInfo[] old) throws IOException {
            final int len  = old.length;
            int i;

            ClientListenerInfo[] clis = new ClientListenerInfo[len];

            final Subject[] subjects = new Subject[len];
            final ObjectName[] names = new ObjectName[len];
            final NotificationListener[] listeners = new NotificationListener[len];
            final NotificationFilter[] filters = new NotificationFilter[len];
            final MarshalledObject<NotificationFilter>[] mFilters =
                    Util.cast(new MarshalledObject<?>[len]);
            final Object[] handbacks = new Object[len];

            for (i=0;i<len;i++) {
                subjects[i]  = old[i].getDelegationSubject();
                names[i]     = old[i].getObjectName();
                listeners[i] = old[i].getListener();
                filters[i]   = old[i].getNotificationFilter();
                mFilters[i]  = new MarshalledObject<NotificationFilter>(filters[i]);
                handbacks[i] = old[i].getHandback();
            }

            try {
                Integer[] ids = addListenersWithSubjects(names,mFilters,subjects,false);

                for (i=0;i<len;i++) {
                    clis[i] = new ClientListenerInfo(ids[i],
                            names[i],
                            listeners[i],
                            filters[i],
                            handbacks[i],
                            subjects[i]);
                }

                rmiNotifClient.postReconnection(clis);

                return;
            } catch (InstanceNotFoundException infe) {
                // OK, we will do one by one
            }

            int j = 0;
            for (i=0;i<len;i++) {
                try {
                    Integer id = addListenerWithSubject(names[i],
                            new MarshalledObject<NotificationFilter>(filters[i]),
                            subjects[i],
                            false);

                    clis[j++] = new ClientListenerInfo(id,
                            names[i],
                            listeners[i],
                            filters[i],
                            handbacks[i],
                            subjects[i]);
                } catch (InstanceNotFoundException infe) {
                    logger.warning("reconnectNotificationListeners",
                            "Can't reconnect listener for " +
                            names[i]);
                }
            }

            if (j != len) {
                ClientListenerInfo[] tmp = clis;
                clis = new ClientListenerInfo[j];
                System.arraycopy(tmp, 0, clis, 0, j);
            }

            rmiNotifClient.postReconnection(clis);
        }

        protected void checkConnection() throws IOException {
            if (logger.debugOn())
                logger.debug("RMIClientCommunicatorAdmin-checkConnection",
                        "Calling the method getDefaultDomain.");

            connection.getDefaultDomain(null);
        }

        protected void doStart() throws IOException {
            // Get RMIServer stub from directory or URL encoding if needed.
            RMIServer stub;
            try {
                stub = (rmiServer!=null)?rmiServer:
                    findRMIServer(jmxServiceURL, env);
            } catch (NamingException ne) {
                throw new IOException("Failed to get a RMI stub: "+ne);
            }

            // Calling newClient on the RMIServer stub.
            Object credentials = env.get(CREDENTIALS);
            connection = stub.newClient(credentials);

            // notif issues
            final ClientListenerInfo[] old = rmiNotifClient.preReconnection();

            reconnectNotificationListeners(old);

            connectionId = getConnectionId();

            Notification reconnectedNotif =
                    new JMXConnectionNotification(JMXConnectionNotification.OPENED,
                    this,
                    connectionId,
                    clientNotifSeqNo++,
                    "Reconnected to server",
                    null);
            sendNotification(reconnectedNotif);

        }

        protected void doStop() {
            try {
                close();
            } catch (IOException ioe) {
                logger.warning("RMIClientCommunicatorAdmin-doStop",
                        "Failed to call the method close():" + ioe);
                logger.debug("RMIClientCommunicatorAdmin-doStop",ioe);
            }
        }
    }

    //--------------------------------------------------------------------
    // Private stuff - Serialization
    //--------------------------------------------------------------------
    /**
     * Read RMIConnector fields from an {@link java.io.ObjectInputStream
     * ObjectInputStream}.
     * Calls {@code s.defaultReadObject()} and then initializes
     * all transient variables that need initializing.
     * @param s The ObjectInputStream to read from.
     * @exception InvalidObjectException if none of <var>rmiServer</var> stub
     *    or <var>jmxServiceURL</var> are set.
     * @see #RMIConnector(JMXServiceURL,Map)
     * @see #RMIConnector(RMIServer,Map)
     **/
    private void readObject(java.io.ObjectInputStream s)
    throws IOException, ClassNotFoundException  {
        s.defaultReadObject();

        if (rmiServer == null && jmxServiceURL == null) throw new
                InvalidObjectException("rmiServer and jmxServiceURL both null");

        initTransients();
    }

    /**
     * Writes the RMIConnector fields to an {@link java.io.ObjectOutputStream
     * ObjectOutputStream}.
     * <p>Connects the underlying RMIServer stub to an ORB, if needed,
     * before serializing it. This is done using the environment
     * map that was provided to the constructor, if any, and as documented
     * in {@link javax.management.remote.rmi}.</p>
     * <p>This method then calls {@code s.defaultWriteObject()}.
     * Usually, <var>rmiServer</var> is null if this object
     * was constructed with a JMXServiceURL, and <var>jmxServiceURL</var>
     * is null if this object is constructed with a RMIServer stub.
     * <p>Note that the environment Map is not serialized, since the objects
     * it contains are assumed to be contextual and relevant only
     * with respect to the local environment (class loader, ORB, etc...).</p>
     * <p>After an RMIConnector is deserialized, it is assumed that the
     * user will call {@link #connect(Map)}, providing a new Map that
     * can contain values which are contextually relevant to the new
     * local environment.</p>
     * <p>Since connection to the ORB is needed prior to serializing, and
     * since the ORB to connect to is one of those contextual parameters,
     * it is not recommended to re-serialize a just de-serialized object -
     * as the de-serialized object has no map. Thus, when an RMIConnector
     * object is needed for serialization or transmission to a remote
     * application, it is recommended to obtain a new RMIConnector stub
     * by calling {@link RMIConnectorServer#toJMXConnector(Map)}.</p>
     * @param s The ObjectOutputStream to write to.
     * @exception InvalidObjectException if none of <var>rmiServer</var> stub
     *    or <var>jmxServiceURL</var> are set.
     * @see #RMIConnector(JMXServiceURL,Map)
     * @see #RMIConnector(RMIServer,Map)
     **/
    private void writeObject(java.io.ObjectOutputStream s)
    throws IOException {
        if (rmiServer == null && jmxServiceURL == null) throw new
                InvalidObjectException("rmiServer and jmxServiceURL both null.");
        s.defaultWriteObject();
    }

    // Initialization of transient variables.
    private void initTransients() {
        rmbscMap = new WeakHashMap<Subject, WeakReference<MBeanServerConnection>>();
        connected = false;
        terminated = false;

        connectionBroadcaster = new NotificationBroadcasterSupport();
    }

    //--------------------------------------------------------------------
    // Private stuff - Check if stub can be trusted.
    //--------------------------------------------------------------------

    private static void checkStub(Remote stub,
            Class<?> stubClass) {

        // Check remote stub is from the expected class.
        //
        if (stub.getClass() != stubClass) {
            if (!Proxy.isProxyClass(stub.getClass())) {
                throw new SecurityException(
                        "Expecting a " + stubClass.getName() + " stub!");
            } else {
                InvocationHandler handler = Proxy.getInvocationHandler(stub);
                if (handler.getClass() != RemoteObjectInvocationHandler.class)
                    throw new SecurityException(
                            "Expecting a dynamic proxy instance with a " +
                            RemoteObjectInvocationHandler.class.getName() +
                            " invocation handler!");
                else
                    stub = (Remote) handler;
            }
        }

        // Check RemoteRef in stub is from the expected class
        // "sun.rmi.server.UnicastRef2".
        //
        RemoteRef ref = ((RemoteObject)stub).getRef();
        if (ref.getClass() != UnicastRef2.class)
            throw new SecurityException(
                    "Expecting a " + UnicastRef2.class.getName() +
                    " remote reference in stub!");

        // Check RMIClientSocketFactory in stub is from the expected class
        // "javax.rmi.ssl.SslRMIClientSocketFactory".
        //
        LiveRef liveRef = ((UnicastRef2)ref).getLiveRef();
        RMIClientSocketFactory csf = liveRef.getClientSocketFactory();
        if (csf == null || csf.getClass() != SslRMIClientSocketFactory.class)
            throw new SecurityException(
                    "Expecting a " + SslRMIClientSocketFactory.class.getName() +
                    " RMI client socket factory in stub!");
    }

    //--------------------------------------------------------------------
    // Private stuff - RMIServer creation
    //--------------------------------------------------------------------

    private RMIServer findRMIServer(JMXServiceURL directoryURL,
            Map<String, Object> environment)
            throws NamingException, IOException {

        String path = directoryURL.getURLPath();
        int end = path.indexOf(';');
        if (end < 0) end = path.length();
        if (path.startsWith("/jndi/"))
            return findRMIServerJNDI(path.substring(6,end), environment);
        else if (path.startsWith("/stub/"))
            return findRMIServerJRMP(path.substring(6,end), environment);
        else {
            final String msg = "URL path must begin with /jndi/ or /stub/ " +
                    "or /ior/: " + path;
            throw new MalformedURLException(msg);
        }
    }

    /**
     * Lookup the RMIServer stub in a directory.
     * @param jndiURL A JNDI URL indicating the location of the Stub
     *                (see {@link javax.management.remote.rmi}), e.g.:
     *   <ul><li>{@code rmi://registry-host:port/rmi-stub-name}</li>
     *       <li>or {@code ldap://ldap-host:port/java-container-dn}</li>
     *   </ul>
     * @param env the environment Map passed to the connector.
     * @return The retrieved RMIServer stub.
     * @exception NamingException if the stub couldn't be found.
     **/
    private RMIServer findRMIServerJNDI(String jndiURL, Map<String, ?> env)
            throws NamingException {

        InitialContext ctx = new InitialContext(EnvHelp.mapToHashtable(env));

        Object objref = ctx.lookup(jndiURL);
        ctx.close();

        return narrowJRMPServer(objref);
    }

    private static RMIServer narrowJRMPServer(Object objref) {

        return (RMIServer) objref;
    }

    private RMIServer findRMIServerJRMP(String base64, Map<String, ?> env)
        throws IOException {
        final byte[] serialized;
        try {
            serialized = base64ToByteArray(base64);
        } catch (IllegalArgumentException e) {
            throw new MalformedURLException("Bad BASE64 encoding: " +
                    e.getMessage());
        }
        final ByteArrayInputStream bin = new ByteArrayInputStream(serialized);

        final ClassLoader loader = EnvHelp.resolveClientClassLoader(env);
        final ObjectInputStream oin =
                (loader == null) ?
                    new ObjectInputStream(bin) :
                    new ObjectInputStreamWithLoader(bin, loader);
        final Object stub;
        try {
            stub = oin.readObject();
        } catch (ClassNotFoundException e) {
            throw new MalformedURLException("Class not found: " + e);
        }
        return (RMIServer)stub;
    }

    private static final class ObjectInputStreamWithLoader
            extends ObjectInputStream {
        ObjectInputStreamWithLoader(InputStream in, ClassLoader cl)
        throws IOException, IllegalArgumentException {
            super(in);
            if (cl == null ) {
              throw new IllegalArgumentException("class loader is null");
            }
            this.loader = cl;
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass classDesc)
                throws IOException, ClassNotFoundException {
            String name = classDesc.getName();
            ReflectUtil.checkPackageAccess(name);
            return Class.forName(name, false, Objects.requireNonNull(loader));
        }

        private final ClassLoader loader;
    }

    private MBeanServerConnection getConnectionWithSubject(Subject delegationSubject) {
        MBeanServerConnection conn = null;

        if (delegationSubject == null) {
            if (nullSubjectConnRef == null
                    || (conn = nullSubjectConnRef.get()) == null) {
                conn = new RemoteMBeanServerConnection(null);
                nullSubjectConnRef = new WeakReference<MBeanServerConnection>(conn);
            }
        } else {
            WeakReference<MBeanServerConnection> wr = rmbscMap.get(delegationSubject);
            if (wr == null || (conn = wr.get()) == null) {
                conn = new RemoteMBeanServerConnection(delegationSubject);
                rmbscMap.put(delegationSubject, new WeakReference<MBeanServerConnection>(conn));
            }
        }
        return conn;
    }

    /*
       The following section of code avoids a class loading problem
       with RMI.  The problem is that an RMI stub, when deserializing
       a remote method return value or exception, will first of all
       consult the first non-bootstrap class loader it finds in the
       call stack.  This can lead to behavior that is not portable
       between implementations of the JMX Remote API.  Notably, an
       implementation on J2SE 1.4 will find the RMI stub's loader on
       the stack.  But in J2SE 5, this stub is loaded by the
       bootstrap loader, so RMI will find the loader of the user code
       that called an MBeanServerConnection method.

       To avoid this problem, we take advantage of what the RMI stub
       is doing internally.  Each remote call will end up calling
       ref.invoke(...), where ref is the RemoteRef parameter given to
       the RMI stub's constructor.  It is within this call that the
       deserialization will happen.  So we fabricate our own RemoteRef
       that delegates everything to the "real" one but that is loaded
       by a class loader that knows no other classes.  The class
       loader NoCallStackClassLoader does this: the RemoteRef is an
       instance of the class named by proxyRefClassName, which is
       fabricated by the class loader using byte code that is defined
       by the string below.

       The call stack when the deserialization happens is thus this:
       MBeanServerConnection.getAttribute (or whatever)
       -> RMIConnectionImpl_Stub.getAttribute
          -> ProxyRef.invoke(...getAttribute...)
             -> UnicastRef.invoke(...getAttribute...)
                -> internal RMI stuff

       Here UnicastRef is the RemoteRef created when the stub was
       deserialized (which is of some RMI internal class).  It and the
       "internal RMI stuff" are loaded by the bootstrap loader, so are
       transparent to the stack search.  The first non-bootstrap
       loader found is our ProxyRefLoader, as required.

       In a future version of this code as integrated into J2SE 5,
       this workaround could be replaced by direct access to the
       internals of RMI.  For now, we use the same code base for J2SE
       and for the standalone Reference Implementation.

       The byte code below encodes the following class, compiled using
       J2SE 1.4.2 with the -g:none option.

        package jdk.jmx.remote.internal.rmi;

        import java.lang.reflect.Method;
        import java.rmi.Remote;
        import java.rmi.server.RemoteRef;
        import com.sun.jmx.remote.internal.rmi.ProxyRef;

        public class PRef extends ProxyRef {
            public PRef(RemoteRef ref) {
                super(ref);
            }

            public Object invoke(Remote obj, Method method,
                                 Object[] params, long opnum)
                    throws Exception {
                return ref.invoke(obj, method, params, opnum);
            }
        }
     */

    private static final String rmiServerImplStubClassName =
        RMIServer.class.getName() + "Impl_Stub";
    private static final Class<?> rmiServerImplStubClass;
    private static final String rmiConnectionImplStubClassName =
            RMIConnection.class.getName() + "Impl_Stub";
    private static final Class<?> rmiConnectionImplStubClass;
    private static final String pRefClassName =
        "jdk.jmx.remote.internal.rmi.PRef";
    private static final Constructor<?> proxyRefConstructor;
    static {
        final String pRefByteCodeString =
                "\312\376\272\276\0\0\0\65\0\27\12\0\5\0\15\11\0\4\0\16\13\0\17"+
                "\0\20\7\0\21\7\0\22\1\0\6<init>\1\0\36(Ljava/rmi/server/Remote"+
                "Ref;)V\1\0\4Code\1\0\6invoke\1\0S(Ljava/rmi/Remote;Ljava/lang/"+
                "reflect/Method;[Ljava/lang/Object;J)Ljava/lang/Object;\1\0\12E"+
                "xceptions\7\0\23\14\0\6\0\7\14\0\24\0\25\7\0\26\14\0\11\0\12\1"+
                "\0 jdk/jmx/remote/internal/rmi/PRef\1\0(com/sun/jmx/remote/int"+
                "ernal/rmi/ProxyRef\1\0\23java/lang/Exception\1\0\3ref\1\0\33Lj"+
                "ava/rmi/server/RemoteRef;\1\0\31java/rmi/server/RemoteRef\0!\0"+
                "\4\0\5\0\0\0\0\0\2\0\1\0\6\0\7\0\1\0\10\0\0\0\22\0\2\0\2\0\0\0"+
                "\6*+\267\0\1\261\0\0\0\0\0\1\0\11\0\12\0\2\0\10\0\0\0\33\0\6\0"+
                "\6\0\0\0\17*\264\0\2+,-\26\4\271\0\3\6\0\260\0\0\0\0\0\13\0\0\0"+
                "\4\0\1\0\14\0\0";
        final byte[] pRefByteCode =
                NoCallStackClassLoader.stringToBytes(pRefByteCodeString);
        PrivilegedExceptionAction<Constructor<?>> action =
                new PrivilegedExceptionAction<Constructor<?>>() {
            public Constructor<?> run() throws Exception {
                Class<RMIConnector> thisClass = RMIConnector.class;
                ClassLoader thisLoader = thisClass.getClassLoader();
                ProtectionDomain thisProtectionDomain =
                        thisClass.getProtectionDomain();

                String proxyRefCName = ProxyRef.class.getName();
                ClassLoader cl =
                        new NoCallStackClassLoader(pRefClassName,
                        pRefByteCode,
                        new String[] { proxyRefCName },
                        thisLoader,
                        thisProtectionDomain);

                Module jmxModule = ProxyRef.class.getModule();
                Module rmiModule = RemoteRef.class.getModule();

                String pkg = packageOf(pRefClassName);
                assert pkg != null && pkg.length() > 0 &&
                        !pkg.equals(packageOf(proxyRefCName));

                ModuleDescriptor descriptor =
                    ModuleDescriptor.newModule("jdk.remoteref", Set.of(SYNTHETIC))
                        .packages(Set.of(pkg))
                        .build();
                Module m = Modules.defineModule(cl, descriptor, null);

                // jdk.remoteref needs to read to java.base and jmxModule
                Modules.addReads(m, Object.class.getModule());
                Modules.addReads(m, jmxModule);
                Modules.addReads(m, rmiModule);

                // jdk.remoteref needs access to ProxyRef class
                Modules.addExports(jmxModule, packageOf(proxyRefCName), m);

                // java.management needs to instantiate the fabricated RemoteRef class
                Modules.addReads(jmxModule, m);
                Modules.addExports(m, pkg, jmxModule);

                Class<?> c = cl.loadClass(pRefClassName);
                return c.getConstructor(RemoteRef.class);
            }
        };

        Class<?> serverStubClass;
        try {
            serverStubClass = Class.forName(rmiServerImplStubClassName);
        } catch (Exception e) {
            logger.error("<clinit>",
                    "Failed to instantiate " +
                    rmiServerImplStubClassName + ": " + e);
            logger.debug("<clinit>",e);
            serverStubClass = null;
        }
        rmiServerImplStubClass = serverStubClass;

        Class<?> stubClass;
        Constructor<?> constr;
        try {
            stubClass = Class.forName(rmiConnectionImplStubClassName);
            @SuppressWarnings("removal")
            Constructor<?> tmp = (Constructor<?>) AccessController.doPrivileged(action);
            constr = tmp;
        } catch (Exception e) {
            logger.error("<clinit>",
                    "Failed to initialize proxy reference constructor "+
                    "for " + rmiConnectionImplStubClassName + ": " + e);
            logger.debug("<clinit>",e);
            stubClass = null;
            constr = null;
        }
        rmiConnectionImplStubClass = stubClass;
        proxyRefConstructor = constr;
    }

    private static String packageOf(String cn) {
        int i = cn.lastIndexOf('.');
        return i > 0 ? cn.substring(0, i) : "";
    }

    private static RMIConnection shadowJrmpStub(RemoteObject stub)
    throws InstantiationException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException {
        RemoteRef ref = stub.getRef();
        RemoteRef proxyRef = (RemoteRef)
            proxyRefConstructor.newInstance(new Object[] {ref});
        final Constructor<?> rmiConnectionImplStubConstructor =
            rmiConnectionImplStubClass.getConstructor(RemoteRef.class);
        Object[] args = {proxyRef};
        RMIConnection proxyStub = (RMIConnection)
        rmiConnectionImplStubConstructor.newInstance(args);
        return proxyStub;
    }

    private static RMIConnection getConnection(RMIServer server,
            Object credentials,
            boolean checkStub)
            throws IOException {
        RMIConnection c = server.newClient(credentials);
        if (checkStub) checkStub(c, rmiConnectionImplStubClass);
        try {
            if (c.getClass() == rmiConnectionImplStubClass)
                return shadowJrmpStub((RemoteObject) c);
            logger.trace("getConnection",
                    "Did not wrap " + c.getClass() + " to foil " +
                    "stack search for classes: class loading semantics " +
                    "may be incorrect");
        } catch (Exception e) {
            logger.error("getConnection",
                    "Could not wrap " + c.getClass() + " to foil " +
                    "stack search for classes: class loading semantics " +
                    "may be incorrect: " + e);
            logger.debug("getConnection",e);
            // so just return the original stub, which will work for all
            // but the most exotic class loading situations
        }
        return c;
    }

    private static byte[] base64ToByteArray(String s) {
        int sLen = s.length();
        int numGroups = sLen/4;
        if (4*numGroups != sLen)
            throw new IllegalArgumentException(
                    "String length must be a multiple of four.");
        int missingBytesInLastGroup = 0;
        int numFullGroups = numGroups;
        if (sLen != 0) {
            if (s.charAt(sLen-1) == '=') {
                missingBytesInLastGroup++;
                numFullGroups--;
            }
            if (s.charAt(sLen-2) == '=')
                missingBytesInLastGroup++;
        }
        byte[] result = new byte[3*numGroups - missingBytesInLastGroup];

        // Translate all full groups from base64 to byte array elements
        int inCursor = 0, outCursor = 0;
        for (int i=0; i<numFullGroups; i++) {
            int ch0 = base64toInt(s.charAt(inCursor++));
            int ch1 = base64toInt(s.charAt(inCursor++));
            int ch2 = base64toInt(s.charAt(inCursor++));
            int ch3 = base64toInt(s.charAt(inCursor++));
            result[outCursor++] = (byte) ((ch0 << 2) | (ch1 >> 4));
            result[outCursor++] = (byte) ((ch1 << 4) | (ch2 >> 2));
            result[outCursor++] = (byte) ((ch2 << 6) | ch3);
        }

        // Translate partial group, if present
        if (missingBytesInLastGroup != 0) {
            int ch0 = base64toInt(s.charAt(inCursor++));
            int ch1 = base64toInt(s.charAt(inCursor++));
            result[outCursor++] = (byte) ((ch0 << 2) | (ch1 >> 4));

            if (missingBytesInLastGroup == 1) {
                int ch2 = base64toInt(s.charAt(inCursor++));
                result[outCursor++] = (byte) ((ch1 << 4) | (ch2 >> 2));
            }
        }
        // assert inCursor == s.length()-missingBytesInLastGroup;
        // assert outCursor == result.length;
        return result;
    }

    /**
     * Translates the specified character, which is assumed to be in the
     * "Base 64 Alphabet" into its equivalent 6-bit positive integer.
     *
     * @throws IllegalArgumentException if
     *        c is not in the Base64 Alphabet.
     */
    private static int base64toInt(char c) {
        int result;

        if (c >= base64ToInt.length)
            result = -1;
        else
            result = base64ToInt[c];

        if (result < 0)
            throw new IllegalArgumentException("Illegal character " + c);
        return result;
    }

    /**
     * This array is a lookup table that translates unicode characters
     * drawn from the "Base64 Alphabet" (as specified in Table 1 of RFC 2045)
     * into their 6-bit positive integer equivalents.  Characters that
     * are not in the Base64 alphabet but fall within the bounds of the
     * array are translated to -1.
     */
    private static final byte base64ToInt[] = {
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63, 52, 53, 54,
        55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4,
        5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
        24, 25, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34,
        35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51
    };

    //--------------------------------------------------------------------
    // Private stuff - Find / Set default class loader
    //--------------------------------------------------------------------
    @SuppressWarnings("removal")
    private ClassLoader pushDefaultClassLoader() {
        final Thread t = Thread.currentThread();
        final ClassLoader old =  t.getContextClassLoader();
        if (defaultClassLoader != null)
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    t.setContextClassLoader(defaultClassLoader);
                    return null;
                }
            });
            return old;
    }

    @SuppressWarnings("removal")
    private void popDefaultClassLoader(final ClassLoader old) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                Thread.currentThread().setContextClassLoader(old);
                return null;
            }
        });
    }

    //--------------------------------------------------------------------
    // Private variables
    //--------------------------------------------------------------------
    /**
     * @serial The RMIServer stub of the RMI JMX Connector server to
     * which this client connector is (or will be) connected. This
     * field can be null when <var>jmxServiceURL</var> is not
     * null. This includes the case where <var>jmxServiceURL</var>
     * contains a serialized RMIServer stub. If both
     * <var>rmiServer</var> and <var>jmxServiceURL</var> are null then
     * serialization will fail.
     *
     * @see #RMIConnector(RMIServer,Map)
     **/
    @SuppressWarnings("serial") // Not statically typed as Serializable
    private final RMIServer rmiServer;

    /**
     * @serial The JMXServiceURL of the RMI JMX Connector server to
     * which this client connector will be connected. This field can
     * be null when <var>rmiServer</var> is not null. If both
     * <var>rmiServer</var> and <var>jmxServiceURL</var> are null then
     * serialization will fail.
     *
     * @see #RMIConnector(JMXServiceURL,Map)
     **/
    private final JMXServiceURL jmxServiceURL;

    // ---------------------------------------------------------
    // WARNING - WARNING - WARNING - WARNING - WARNING - WARNING
    // ---------------------------------------------------------
    // Any transient variable which needs to be initialized should
    // be initialized in the method initTransient()
    private transient Map<String, Object> env;
    private transient ClassLoader defaultClassLoader;
    private transient RMIConnection connection;
    private transient String connectionId;

    private transient long clientNotifSeqNo = 0;

    private transient WeakHashMap<Subject, WeakReference<MBeanServerConnection>> rmbscMap;
    private transient WeakReference<MBeanServerConnection> nullSubjectConnRef = null;

    private transient RMINotifClient rmiNotifClient;
    // = new RMINotifClient(new Integer(0));

    private transient long clientNotifCounter = 0;

    private transient boolean connected;
    // = false;
    private transient boolean terminated;
    // = false;

    private transient Exception closeException;

    private transient NotificationBroadcasterSupport connectionBroadcaster;

    private transient ClientCommunicatorAdmin communicatorAdmin;

    /**
     * A static WeakReference to an {@link org.omg.CORBA.ORB ORB} to
     * connect unconnected stubs.
     **/
    private static volatile WeakReference<Object> orb = null;

    // TRACES & DEBUG
    //---------------
    private static String objects(final Object[] objs) {
        if (objs == null)
            return "null";
        else
            return Arrays.asList(objs).toString();
    }

    private static String strings(final String[] strs) {
        return objects(strs);
    }

    static String getAttributesNames(AttributeList attributes) {
        return attributes != null ?
                attributes.asList().stream()
                        .map(Attribute::getName)
                        .collect(Collectors.joining(", ", "[", "]"))
                : "[]";
    }
}
