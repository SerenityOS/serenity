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

import java.io.IOException;
import java.rmi.MarshalledObject;
import java.rmi.UnmarshalException;
import java.rmi.server.Unreferenced;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Permission;
import java.security.Permissions;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.management.*;
import javax.management.remote.JMXServerErrorException;
import javax.management.remote.NotificationResult;
import javax.security.auth.Subject;
import sun.reflect.misc.ReflectUtil;

import static javax.management.remote.rmi.RMIConnector.Util.cast;
import com.sun.jmx.remote.internal.ServerCommunicatorAdmin;
import com.sun.jmx.remote.internal.ServerNotifForwarder;
import com.sun.jmx.remote.security.JMXSubjectDomainCombiner;
import com.sun.jmx.remote.security.SubjectDelegator;
import com.sun.jmx.remote.util.ClassLoaderWithRepository;
import com.sun.jmx.remote.util.ClassLogger;
import com.sun.jmx.remote.util.EnvHelp;
import com.sun.jmx.remote.util.OrderClassLoaders;
import javax.management.loading.ClassLoaderRepository;

/**
 * <p>Implementation of the {@link RMIConnection} interface.  User
 * code will not usually reference this class.</p>
 *
 * @since 1.5
 */
/*
 * Notice that we omit the type parameter from MarshalledObject everywhere,
 * even though it would add useful information to the documentation.  The
 * reason is that it was only added in Mustang (Java SE 6), whereas versions
 * 1.4 and 2.0 of the JMX API must be implementable on Tiger per our
 * commitments for JSR 255.
 */
public class RMIConnectionImpl implements RMIConnection, Unreferenced {

    /**
     * Constructs a new {@link RMIConnection}. This connection can be
     * used with the JRMP transport. This object does
     * not export itself: it is the responsibility of the caller to
     * export it appropriately (see {@link
     * RMIJRMPServerImpl#makeClient(String,Subject)}).
     *
     * @param rmiServer The RMIServerImpl object for which this
     * connection is created.  The behavior is unspecified if this
     * parameter is null.
     * @param connectionId The ID for this connection.  The behavior
     * is unspecified if this parameter is null.
     * @param defaultClassLoader The default ClassLoader to be used
     * when deserializing marshalled objects.  Can be null, to signify
     * the bootstrap class loader.
     * @param subject the authenticated subject to be used for
     * authorization.  Can be null, to signify that no subject has
     * been authenticated.
     * @param env the environment containing attributes for the new
     * <code>RMIServerImpl</code>.  Can be null, equivalent to an
     * empty map.
     */
    @SuppressWarnings("removal")
    public RMIConnectionImpl(RMIServerImpl rmiServer,
                             String connectionId,
                             ClassLoader defaultClassLoader,
                             Subject subject,
                             Map<String,?> env) {
        if (rmiServer == null || connectionId == null)
            throw new NullPointerException("Illegal null argument");
        if (env == null)
            env = Collections.emptyMap();
        this.rmiServer = rmiServer;
        this.connectionId = connectionId;
        this.defaultClassLoader = defaultClassLoader;

        this.subjectDelegator = new SubjectDelegator();
        this.subject = subject;
        if (subject == null) {
            this.acc = null;
            this.removeCallerContext = false;
        } else {
            this.removeCallerContext =
                SubjectDelegator.checkRemoveCallerContext(subject);
            if (this.removeCallerContext) {
                this.acc =
                    JMXSubjectDomainCombiner.getDomainCombinerContext(subject);
            } else {
                this.acc =
                    JMXSubjectDomainCombiner.getContext(subject);
            }
        }
        this.mbeanServer = rmiServer.getMBeanServer();

        final ClassLoader dcl = defaultClassLoader;

        ClassLoaderRepository repository = AccessController.doPrivileged(
            new PrivilegedAction<ClassLoaderRepository>() {
                public ClassLoaderRepository run() {
                    return mbeanServer.getClassLoaderRepository();
                }
            },
            withPermissions(new MBeanPermission("*", "getClassLoaderRepository"))
        );
        this.classLoaderWithRepository = AccessController.doPrivileged(
            new PrivilegedAction<ClassLoaderWithRepository>() {
                public ClassLoaderWithRepository run() {
                    return new ClassLoaderWithRepository(
                        repository,
                        dcl);
                }
            },
            withPermissions(new RuntimePermission("createClassLoader"))
        );

        this.defaultContextClassLoader =
            AccessController.doPrivileged(
                new PrivilegedAction<ClassLoader>() {
            @Override
                    public ClassLoader run() {
                        return new CombinedClassLoader(Thread.currentThread().getContextClassLoader(),
                                dcl);
                    }
                });

        serverCommunicatorAdmin = new
          RMIServerCommunicatorAdmin(EnvHelp.getServerConnectionTimeout(env));

        this.env = env;
    }

    @SuppressWarnings("removal")
    private static AccessControlContext withPermissions(Permission ... perms){
        Permissions col = new Permissions();

        for (Permission thePerm : perms ) {
            col.add(thePerm);
        }

        final ProtectionDomain pd = new ProtectionDomain(null, col);
        return new AccessControlContext( new ProtectionDomain[] { pd });
    }

    private synchronized ServerNotifForwarder getServerNotifFwd() {
        // Lazily created when first use. Mainly when
        // addNotificationListener is first called.
        if (serverNotifForwarder == null)
            serverNotifForwarder =
                new ServerNotifForwarder(mbeanServer,
                                         env,
                                         rmiServer.getNotifBuffer(),
                                         connectionId);
        return serverNotifForwarder;
    }

    public String getConnectionId() throws IOException {
        // We should call reqIncomming() here... shouldn't we?
        return connectionId;
    }

    public void close() throws IOException {
        final boolean debug = logger.debugOn();
        final String  idstr = (debug?"["+this.toString()+"]":null);

        synchronized (this) {
            if (terminated) {
                if (debug) logger.debug("close",idstr + " already terminated.");
                return;
            }

            if (debug) logger.debug("close",idstr + " closing.");

            terminated = true;

            if (serverCommunicatorAdmin != null) {
                serverCommunicatorAdmin.terminate();
            }

            if (serverNotifForwarder != null) {
                serverNotifForwarder.terminate();
            }
        }

        rmiServer.clientClosed(this);

        if (debug) logger.debug("close",idstr + " closed.");
    }

    public void unreferenced() {
        logger.debug("unreferenced", "called");
        try {
            close();
            logger.debug("unreferenced", "done");
        } catch (IOException e) {
            logger.fine("unreferenced", e);
        }
    }

    //-------------------------------------------------------------------------
    // MBeanServerConnection Wrapper
    //-------------------------------------------------------------------------

    public ObjectInstance createMBean(String className,
                                      ObjectName name,
                                      Subject delegationSubject)
        throws
        ReflectionException,
        InstanceAlreadyExistsException,
        MBeanRegistrationException,
        MBeanException,
        NotCompliantMBeanException,
        IOException {
        try {
            final Object params[] =
                new Object[] { className, name };

            if (logger.debugOn())
                logger.debug("createMBean(String,ObjectName)",
                             "connectionId=" + connectionId +", className=" +
                             className+", name=" + name);

            return (ObjectInstance)
                doPrivilegedOperation(
                  CREATE_MBEAN,
                  params,
                  delegationSubject);
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof ReflectionException)
                throw (ReflectionException) e;
            if (e instanceof InstanceAlreadyExistsException)
                throw (InstanceAlreadyExistsException) e;
            if (e instanceof MBeanRegistrationException)
                throw (MBeanRegistrationException) e;
            if (e instanceof MBeanException)
                throw (MBeanException) e;
            if (e instanceof NotCompliantMBeanException)
                throw (NotCompliantMBeanException) e;
            if (e instanceof IOException)
                throw (IOException) e;
            throw newIOException("Got unexpected server exception: " + e, e);
        }
    }

    public ObjectInstance createMBean(String className,
                                      ObjectName name,
                                      ObjectName loaderName,
                                      Subject delegationSubject)
        throws
        ReflectionException,
        InstanceAlreadyExistsException,
        MBeanRegistrationException,
        MBeanException,
        NotCompliantMBeanException,
        InstanceNotFoundException,
        IOException {
        try {
            final Object params[] =
                new Object[] { className, name, loaderName };

            if (logger.debugOn())
                logger.debug("createMBean(String,ObjectName,ObjectName)",
                      "connectionId=" + connectionId
                      +", className=" + className
                      +", name=" + name
                      +", loaderName=" + loaderName);

            return (ObjectInstance)
                doPrivilegedOperation(
                  CREATE_MBEAN_LOADER,
                  params,
                  delegationSubject);
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof ReflectionException)
                throw (ReflectionException) e;
            if (e instanceof InstanceAlreadyExistsException)
                throw (InstanceAlreadyExistsException) e;
            if (e instanceof MBeanRegistrationException)
                throw (MBeanRegistrationException) e;
            if (e instanceof MBeanException)
                throw (MBeanException) e;
            if (e instanceof NotCompliantMBeanException)
                throw (NotCompliantMBeanException) e;
            if (e instanceof InstanceNotFoundException)
                throw (InstanceNotFoundException) e;
            if (e instanceof IOException)
                throw (IOException) e;
            throw newIOException("Got unexpected server exception: " + e, e);
        }
    }

    @SuppressWarnings("rawtypes")  // MarshalledObject
    public ObjectInstance createMBean(String className,
                                      ObjectName name,
                                      MarshalledObject params,
                                      String signature[],
                                      Subject delegationSubject)
        throws
        ReflectionException,
        InstanceAlreadyExistsException,
        MBeanRegistrationException,
        MBeanException,
        NotCompliantMBeanException,
        IOException {

        final Object[] values;
        final boolean debug = logger.debugOn();

        if (debug) logger.debug(
                  "createMBean(String,ObjectName,Object[],String[])",
                  "connectionId=" + connectionId
                  +", unwrapping parameters using classLoaderWithRepository.");

        values =
            nullIsEmpty(unwrap(params, classLoaderWithRepository, Object[].class,delegationSubject));

        try {
            final Object params2[] =
                new Object[] { className, name, values,
                               nullIsEmpty(signature) };

            if (debug)
               logger.debug("createMBean(String,ObjectName,Object[],String[])",
                             "connectionId=" + connectionId
                             +", className=" + className
                             +", name=" + name
                             +", signature=" + strings(signature));

            return (ObjectInstance)
                doPrivilegedOperation(
                  CREATE_MBEAN_PARAMS,
                  params2,
                  delegationSubject);
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof ReflectionException)
                throw (ReflectionException) e;
            if (e instanceof InstanceAlreadyExistsException)
                throw (InstanceAlreadyExistsException) e;
            if (e instanceof MBeanRegistrationException)
                throw (MBeanRegistrationException) e;
            if (e instanceof MBeanException)
                throw (MBeanException) e;
            if (e instanceof NotCompliantMBeanException)
                throw (NotCompliantMBeanException) e;
            if (e instanceof IOException)
                throw (IOException) e;
            throw newIOException("Got unexpected server exception: " + e, e);
        }
    }

    @SuppressWarnings("rawtypes")  // MarshalledObject
    public ObjectInstance createMBean(String className,
                                 ObjectName name,
                                 ObjectName loaderName,
                                 MarshalledObject params,
                                 String signature[],
                                 Subject delegationSubject)
        throws
        ReflectionException,
        InstanceAlreadyExistsException,
        MBeanRegistrationException,
        MBeanException,
        NotCompliantMBeanException,
        InstanceNotFoundException,
        IOException {

        final Object[] values;
        final boolean debug = logger.debugOn();

        if (debug) logger.debug(
                 "createMBean(String,ObjectName,ObjectName,Object[],String[])",
                 "connectionId=" + connectionId
                 +", unwrapping params with MBean extended ClassLoader.");

        values = nullIsEmpty(unwrap(params,
                                    getClassLoader(loaderName),
                                    defaultClassLoader,
                                    Object[].class,delegationSubject));

        try {
            final Object params2[] =
               new Object[] { className, name, loaderName, values,
                              nullIsEmpty(signature) };

           if (debug) logger.debug(
                 "createMBean(String,ObjectName,ObjectName,Object[],String[])",
                 "connectionId=" + connectionId
                 +", className=" + className
                 +", name=" + name
                 +", loaderName=" + loaderName
                 +", signature=" + strings(signature));

            return (ObjectInstance)
                doPrivilegedOperation(
                  CREATE_MBEAN_LOADER_PARAMS,
                  params2,
                  delegationSubject);
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof ReflectionException)
                throw (ReflectionException) e;
            if (e instanceof InstanceAlreadyExistsException)
                throw (InstanceAlreadyExistsException) e;
            if (e instanceof MBeanRegistrationException)
                throw (MBeanRegistrationException) e;
            if (e instanceof MBeanException)
                throw (MBeanException) e;
            if (e instanceof NotCompliantMBeanException)
                throw (NotCompliantMBeanException) e;
            if (e instanceof InstanceNotFoundException)
                throw (InstanceNotFoundException) e;
            if (e instanceof IOException)
                throw (IOException) e;
            throw newIOException("Got unexpected server exception: " + e, e);
        }
    }

    public void unregisterMBean(ObjectName name, Subject delegationSubject)
        throws
        InstanceNotFoundException,
        MBeanRegistrationException,
        IOException {
        try {
            final Object params[] = new Object[] { name };

            if (logger.debugOn()) logger.debug("unregisterMBean",
                 "connectionId=" + connectionId
                 +", name="+name);

            doPrivilegedOperation(
              UNREGISTER_MBEAN,
              params,
              delegationSubject);
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof InstanceNotFoundException)
                throw (InstanceNotFoundException) e;
            if (e instanceof MBeanRegistrationException)
                throw (MBeanRegistrationException) e;
            if (e instanceof IOException)
                throw (IOException) e;
            throw newIOException("Got unexpected server exception: " + e, e);
        }
    }

    public ObjectInstance getObjectInstance(ObjectName name,
                                            Subject delegationSubject)
        throws
        InstanceNotFoundException,
        IOException {

        checkNonNull("ObjectName", name);

        try {
            final Object params[] = new Object[] { name };

            if (logger.debugOn()) logger.debug("getObjectInstance",
                 "connectionId=" + connectionId
                 +", name="+name);

            return (ObjectInstance)
                doPrivilegedOperation(
                  GET_OBJECT_INSTANCE,
                  params,
                  delegationSubject);
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof InstanceNotFoundException)
                throw (InstanceNotFoundException) e;
            if (e instanceof IOException)
                throw (IOException) e;
            throw newIOException("Got unexpected server exception: " + e, e);
        }
    }

    @SuppressWarnings("rawtypes")  // MarshalledObject
    public Set<ObjectInstance>
        queryMBeans(ObjectName name,
                    MarshalledObject query,
                    Subject delegationSubject)
        throws IOException {
        final QueryExp queryValue;
        final boolean debug=logger.debugOn();

        if (debug) logger.debug("queryMBeans",
                 "connectionId=" + connectionId
                 +" unwrapping query with defaultClassLoader.");

        queryValue = unwrap(query, defaultContextClassLoader, QueryExp.class, delegationSubject);

        try {
            final Object params[] = new Object[] { name, queryValue };

            if (debug) logger.debug("queryMBeans",
                 "connectionId=" + connectionId
                 +", name="+name +", query="+query);

            return cast(
                doPrivilegedOperation(
                  QUERY_MBEANS,
                  params,
                  delegationSubject));
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof IOException)
                throw (IOException) e;
            throw newIOException("Got unexpected server exception: " + e, e);
        }
    }

    @SuppressWarnings("rawtypes")  // MarshalledObject
    public Set<ObjectName>
        queryNames(ObjectName name,
                   MarshalledObject query,
                   Subject delegationSubject)
        throws IOException {
        final QueryExp queryValue;
        final boolean debug=logger.debugOn();

        if (debug) logger.debug("queryNames",
                 "connectionId=" + connectionId
                 +" unwrapping query with defaultClassLoader.");

        queryValue = unwrap(query, defaultContextClassLoader, QueryExp.class, delegationSubject);

        try {
            final Object params[] = new Object[] { name, queryValue };

            if (debug) logger.debug("queryNames",
                 "connectionId=" + connectionId
                 +", name="+name +", query="+query);

            return cast(
                doPrivilegedOperation(
                  QUERY_NAMES,
                  params,
                  delegationSubject));
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof IOException)
                throw (IOException) e;
            throw newIOException("Got unexpected server exception: " + e, e);
        }
    }

    public boolean isRegistered(ObjectName name,
                                Subject delegationSubject) throws IOException {
        try {
            final Object params[] = new Object[] { name };
            return ((Boolean)
                doPrivilegedOperation(
                  IS_REGISTERED,
                  params,
                  delegationSubject)).booleanValue();
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof IOException)
                throw (IOException) e;
            throw newIOException("Got unexpected server exception: " + e, e);
        }
    }

    public Integer getMBeanCount(Subject delegationSubject)
        throws IOException {
        try {
            final Object params[] = new Object[] { };

            if (logger.debugOn()) logger.debug("getMBeanCount",
                 "connectionId=" + connectionId);

            return (Integer)
                doPrivilegedOperation(
                  GET_MBEAN_COUNT,
                  params,
                  delegationSubject);
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof IOException)
                throw (IOException) e;
            throw newIOException("Got unexpected server exception: " + e, e);
        }
    }

    public Object getAttribute(ObjectName name,
                               String attribute,
                               Subject delegationSubject)
        throws
        MBeanException,
        AttributeNotFoundException,
        InstanceNotFoundException,
        ReflectionException,
        IOException {
        try {
            final Object params[] = new Object[] { name, attribute };
            if (logger.debugOn()) logger.debug("getAttribute",
                                   "connectionId=" + connectionId
                                   +", name=" + name
                                   +", attribute="+ attribute);

            return
                doPrivilegedOperation(
                  GET_ATTRIBUTE,
                  params,
                  delegationSubject);
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof MBeanException)
                throw (MBeanException) e;
            if (e instanceof AttributeNotFoundException)
                throw (AttributeNotFoundException) e;
            if (e instanceof InstanceNotFoundException)
                throw (InstanceNotFoundException) e;
            if (e instanceof ReflectionException)
                throw (ReflectionException) e;
            if (e instanceof IOException)
                throw (IOException) e;
            throw newIOException("Got unexpected server exception: " + e, e);
        }
    }

    public AttributeList getAttributes(ObjectName name,
                                       String[] attributes,
                                       Subject delegationSubject)
        throws
        InstanceNotFoundException,
        ReflectionException,
        IOException {
        try {
            final Object params[] = new Object[] { name, attributes };

            if (logger.debugOn()) logger.debug("getAttributes",
                                   "connectionId=" + connectionId
                                   +", name=" + name
                                   +", attributes="+ strings(attributes));

            return (AttributeList)
                doPrivilegedOperation(
                  GET_ATTRIBUTES,
                  params,
                  delegationSubject);
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof InstanceNotFoundException)
                throw (InstanceNotFoundException) e;
            if (e instanceof ReflectionException)
                throw (ReflectionException) e;
            if (e instanceof IOException)
                throw (IOException) e;
            throw newIOException("Got unexpected server exception: " + e, e);
        }
    }

    @SuppressWarnings("rawtypes")  // MarshalledObject
    public void setAttribute(ObjectName name,
                             MarshalledObject attribute,
                             Subject delegationSubject)
        throws
        InstanceNotFoundException,
        AttributeNotFoundException,
        InvalidAttributeValueException,
        MBeanException,
        ReflectionException,
        IOException {
        final Attribute attr;
        final boolean debug=logger.debugOn();

        if (debug) logger.debug("setAttribute",
                 "connectionId=" + connectionId
                 +" unwrapping attribute with MBean extended ClassLoader.");

        attr = unwrap(attribute,
                      getClassLoaderFor(name),
                      defaultClassLoader,
                      Attribute.class, delegationSubject);

        try {
            final Object params[] = new Object[] { name, attr };

            if (debug) logger.debug("setAttribute",
                             "connectionId=" + connectionId
                             +", name="+name
                             +", attribute name="+attr.getName());

            doPrivilegedOperation(
              SET_ATTRIBUTE,
              params,
              delegationSubject);
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof InstanceNotFoundException)
                throw (InstanceNotFoundException) e;
            if (e instanceof AttributeNotFoundException)
                throw (AttributeNotFoundException) e;
            if (e instanceof InvalidAttributeValueException)
                throw (InvalidAttributeValueException) e;
            if (e instanceof MBeanException)
                throw (MBeanException) e;
            if (e instanceof ReflectionException)
                throw (ReflectionException) e;
            if (e instanceof IOException)
                throw (IOException) e;
            throw newIOException("Got unexpected server exception: " + e, e);
        }
    }

    @SuppressWarnings("rawtypes")  // MarshalledObject
    public AttributeList setAttributes(ObjectName name,
                         MarshalledObject attributes,
                         Subject delegationSubject)
        throws
        InstanceNotFoundException,
        ReflectionException,
        IOException {
        final AttributeList attrlist;
        final boolean debug=logger.debugOn();

        if (debug) logger.debug("setAttributes",
                 "connectionId=" + connectionId
                 +" unwrapping attributes with MBean extended ClassLoader.");

        attrlist =
            unwrap(attributes,
                   getClassLoaderFor(name),
                   defaultClassLoader,
                   AttributeList.class, delegationSubject);

        try {
            final Object params[] = new Object[] { name, attrlist };

            if (debug) logger.debug("setAttributes",
                             "connectionId=" + connectionId
                             +", name="+name
                             +", attribute names="+RMIConnector.getAttributesNames(attrlist));

            return (AttributeList)
                doPrivilegedOperation(
                  SET_ATTRIBUTES,
                  params,
                  delegationSubject);
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof InstanceNotFoundException)
                throw (InstanceNotFoundException) e;
            if (e instanceof ReflectionException)
                throw (ReflectionException) e;
            if (e instanceof IOException)
                throw (IOException) e;
            throw newIOException("Got unexpected server exception: " + e, e);
        }
    }

    @SuppressWarnings("rawtypes")  // MarshalledObject
    public Object invoke(ObjectName name,
                         String operationName,
                         MarshalledObject params,
                         String signature[],
                         Subject delegationSubject)
        throws
        InstanceNotFoundException,
        MBeanException,
        ReflectionException,
        IOException {

        checkNonNull("ObjectName", name);
        checkNonNull("Operation name", operationName);

        final Object[] values;
        final boolean debug=logger.debugOn();

        if (debug) logger.debug("invoke",
                 "connectionId=" + connectionId
                 +" unwrapping params with MBean extended ClassLoader.");

        values = nullIsEmpty(unwrap(params,
                                    getClassLoaderFor(name),
                                    defaultClassLoader,
                                    Object[].class, delegationSubject));

        try {
            final Object params2[] =
                new Object[] { name, operationName, values,
                               nullIsEmpty(signature) };

            if (debug) logger.debug("invoke",
                             "connectionId=" + connectionId
                             +", name="+name
                             +", operationName="+operationName
                             +", signature="+strings(signature));

            return
                doPrivilegedOperation(
                  INVOKE,
                  params2,
                  delegationSubject);
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof InstanceNotFoundException)
                throw (InstanceNotFoundException) e;
            if (e instanceof MBeanException)
                throw (MBeanException) e;
            if (e instanceof ReflectionException)
                throw (ReflectionException) e;
            if (e instanceof IOException)
                throw (IOException) e;
            throw newIOException("Got unexpected server exception: " + e, e);
        }
    }

    public String getDefaultDomain(Subject delegationSubject)
        throws IOException {
        try {
            final Object params[] = new Object[] { };

            if (logger.debugOn())  logger.debug("getDefaultDomain",
                                    "connectionId=" + connectionId);

            return (String)
                doPrivilegedOperation(
                  GET_DEFAULT_DOMAIN,
                  params,
                  delegationSubject);
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof IOException)
                throw (IOException) e;
            throw newIOException("Got unexpected server exception: " + e, e);
        }
    }

    public String[] getDomains(Subject delegationSubject) throws IOException {
        try {
            final Object params[] = new Object[] { };

            if (logger.debugOn())  logger.debug("getDomains",
                                    "connectionId=" + connectionId);

            return (String[])
                doPrivilegedOperation(
                  GET_DOMAINS,
                  params,
                  delegationSubject);
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof IOException)
                throw (IOException) e;
            throw newIOException("Got unexpected server exception: " + e, e);
        }
    }

    public MBeanInfo getMBeanInfo(ObjectName name, Subject delegationSubject)
        throws
        InstanceNotFoundException,
        IntrospectionException,
        ReflectionException,
        IOException {

        checkNonNull("ObjectName", name);

        try {
            final Object params[] = new Object[] { name };

            if (logger.debugOn())  logger.debug("getMBeanInfo",
                                    "connectionId=" + connectionId
                                    +", name="+name);

            return (MBeanInfo)
                doPrivilegedOperation(
                  GET_MBEAN_INFO,
                  params,
                  delegationSubject);
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof InstanceNotFoundException)
                throw (InstanceNotFoundException) e;
            if (e instanceof IntrospectionException)
                throw (IntrospectionException) e;
            if (e instanceof ReflectionException)
                throw (ReflectionException) e;
            if (e instanceof IOException)
                throw (IOException) e;
            throw newIOException("Got unexpected server exception: " + e, e);
        }
    }

    public boolean isInstanceOf(ObjectName name,
                                String className,
                                Subject delegationSubject)
        throws InstanceNotFoundException, IOException {

        checkNonNull("ObjectName", name);

        try {
            final Object params[] = new Object[] { name, className };

            if (logger.debugOn())  logger.debug("isInstanceOf",
                                    "connectionId=" + connectionId
                                    +", name="+name
                                    +", className="+className);

            return ((Boolean)
                doPrivilegedOperation(
                  IS_INSTANCE_OF,
                  params,
                  delegationSubject)).booleanValue();
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof InstanceNotFoundException)
                throw (InstanceNotFoundException) e;
            if (e instanceof IOException)
                throw (IOException) e;
            throw newIOException("Got unexpected server exception: " + e, e);
        }
    }

    @SuppressWarnings("rawtypes")  // MarshalledObject
    public Integer[] addNotificationListeners(ObjectName[] names,
                      MarshalledObject[] filters,
                      Subject[] delegationSubjects)
            throws InstanceNotFoundException, IOException {

        if (names == null || filters == null) {
            throw new IllegalArgumentException("Got null arguments.");
        }

        Subject[] sbjs = (delegationSubjects != null) ? delegationSubjects :
        new Subject[names.length];
        if (names.length != filters.length || filters.length != sbjs.length) {
            final String msg =
                "The value lengths of 3 parameters are not same.";
            throw new IllegalArgumentException(msg);
        }

        for (int i=0; i<names.length; i++) {
            if (names[i] == null) {
                throw new IllegalArgumentException("Null Object name.");
            }
        }

        int i=0;
        ClassLoader targetCl;
        NotificationFilter[] filterValues =
            new NotificationFilter[names.length];
        Integer[] ids = new Integer[names.length];
        final boolean debug=logger.debugOn();

        try {
            for (; i<names.length; i++) {
                targetCl = getClassLoaderFor(names[i]);

                if (debug) logger.debug("addNotificationListener"+
                                        "(ObjectName,NotificationFilter)",
                                        "connectionId=" + connectionId +
                      " unwrapping filter with target extended ClassLoader.");

                filterValues[i] =
                    unwrap(filters[i], targetCl, defaultClassLoader,
                           NotificationFilter.class, sbjs[i]);

                if (debug) logger.debug("addNotificationListener"+
                                        "(ObjectName,NotificationFilter)",
                                        "connectionId=" + connectionId
                                        +", name=" + names[i]
                                        +", filter=" + filterValues[i]);

                ids[i] = (Integer)
                    doPrivilegedOperation(ADD_NOTIFICATION_LISTENERS,
                                          new Object[] { names[i],
                                                         filterValues[i] },
                                          sbjs[i]);
            }

            return ids;
        } catch (Exception e) {
            // remove all registered listeners
            for (int j=0; j<i; j++) {
                try {
                    getServerNotifFwd().removeNotificationListener(names[j],
                                                                   ids[j]);
                } catch (Exception eee) {
                    // strange
                }
            }

            if (e instanceof PrivilegedActionException) {
                e = extractException(e);
            }

            if (e instanceof ClassCastException) {
                throw (ClassCastException) e;
            } else if (e instanceof IOException) {
                throw (IOException)e;
            } else if (e instanceof InstanceNotFoundException) {
                throw (InstanceNotFoundException) e;
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw newIOException("Got unexpected server exception: "+e,e);
            }
        }
    }

    @SuppressWarnings("rawtypes")  // MarshalledObject
    public void addNotificationListener(ObjectName name,
                       ObjectName listener,
                       MarshalledObject filter,
                       MarshalledObject handback,
                       Subject delegationSubject)
        throws InstanceNotFoundException, IOException {

        checkNonNull("Target MBean name", name);
        checkNonNull("Listener MBean name", listener);

        final NotificationFilter filterValue;
        final Object handbackValue;
        final boolean debug=logger.debugOn();

        final ClassLoader targetCl = getClassLoaderFor(name);

        if (debug) logger.debug("addNotificationListener"+
                 "(ObjectName,ObjectName,NotificationFilter,Object)",
                 "connectionId=" + connectionId
                 +" unwrapping filter with target extended ClassLoader.");

        filterValue =
            unwrap(filter, targetCl, defaultClassLoader, NotificationFilter.class, delegationSubject);

        if (debug) logger.debug("addNotificationListener"+
                 "(ObjectName,ObjectName,NotificationFilter,Object)",
                 "connectionId=" + connectionId
                 +" unwrapping handback with target extended ClassLoader.");

        handbackValue =
            unwrap(handback, targetCl, defaultClassLoader, Object.class, delegationSubject);

        try {
            final Object params[] =
                new Object[] { name, listener, filterValue, handbackValue };

            if (debug) logger.debug("addNotificationListener"+
                 "(ObjectName,ObjectName,NotificationFilter,Object)",
                             "connectionId=" + connectionId
                             +", name=" + name
                             +", listenerName=" + listener
                             +", filter=" + filterValue
                             +", handback=" + handbackValue);

            doPrivilegedOperation(
              ADD_NOTIFICATION_LISTENER_OBJECTNAME,
              params,
              delegationSubject);
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof InstanceNotFoundException)
                throw (InstanceNotFoundException) e;
            if (e instanceof IOException)
                throw (IOException) e;
            throw newIOException("Got unexpected server exception: " + e, e);
        }
    }

    public void removeNotificationListeners(ObjectName name,
                                            Integer[] listenerIDs,
                                            Subject delegationSubject)
        throws
        InstanceNotFoundException,
        ListenerNotFoundException,
        IOException {

        if (name == null || listenerIDs == null)
            throw new IllegalArgumentException("Illegal null parameter");

        for (int i = 0; i < listenerIDs.length; i++) {
            if (listenerIDs[i] == null)
                throw new IllegalArgumentException("Null listener ID");
        }

        try {
            final Object params[] = new Object[] { name, listenerIDs };

            if (logger.debugOn()) logger.debug("removeNotificationListener"+
                                   "(ObjectName,Integer[])",
                                   "connectionId=" + connectionId
                                   +", name=" + name
                                   +", listenerIDs=" + objects(listenerIDs));

            doPrivilegedOperation(
              REMOVE_NOTIFICATION_LISTENER,
              params,
              delegationSubject);
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof InstanceNotFoundException)
                throw (InstanceNotFoundException) e;
            if (e instanceof ListenerNotFoundException)
                throw (ListenerNotFoundException) e;
            if (e instanceof IOException)
                throw (IOException) e;
            throw newIOException("Got unexpected server exception: " + e, e);
        }
    }

    public void removeNotificationListener(ObjectName name,
                                           ObjectName listener,
                                           Subject delegationSubject)
        throws
        InstanceNotFoundException,
        ListenerNotFoundException,
        IOException {

        checkNonNull("Target MBean name", name);
        checkNonNull("Listener MBean name", listener);

        try {
            final Object params[] = new Object[] { name, listener };

            if (logger.debugOn()) logger.debug("removeNotificationListener"+
                                   "(ObjectName,ObjectName)",
                                   "connectionId=" + connectionId
                                   +", name=" + name
                                   +", listenerName=" + listener);

            doPrivilegedOperation(
              REMOVE_NOTIFICATION_LISTENER_OBJECTNAME,
              params,
              delegationSubject);
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof InstanceNotFoundException)
                throw (InstanceNotFoundException) e;
            if (e instanceof ListenerNotFoundException)
                throw (ListenerNotFoundException) e;
            if (e instanceof IOException)
                throw (IOException) e;
            throw newIOException("Got unexpected server exception: " + e, e);
        }
    }

    @SuppressWarnings("rawtypes")  // MarshalledObject
    public void removeNotificationListener(ObjectName name,
                        ObjectName listener,
                        MarshalledObject filter,
                        MarshalledObject handback,
                        Subject delegationSubject)
        throws
        InstanceNotFoundException,
        ListenerNotFoundException,
        IOException {

        checkNonNull("Target MBean name", name);
        checkNonNull("Listener MBean name", listener);

        final NotificationFilter filterValue;
        final Object handbackValue;
        final boolean debug=logger.debugOn();

        final ClassLoader targetCl = getClassLoaderFor(name);

        if (debug) logger.debug("removeNotificationListener"+
                 "(ObjectName,ObjectName,NotificationFilter,Object)",
                 "connectionId=" + connectionId
                 +" unwrapping filter with target extended ClassLoader.");

        filterValue =
            unwrap(filter, targetCl, defaultClassLoader, NotificationFilter.class, delegationSubject);

        if (debug) logger.debug("removeNotificationListener"+
                 "(ObjectName,ObjectName,NotificationFilter,Object)",
                 "connectionId=" + connectionId
                 +" unwrapping handback with target extended ClassLoader.");

        handbackValue =
            unwrap(handback, targetCl, defaultClassLoader, Object.class, delegationSubject);

        try {
            final Object params[] =
                new Object[] { name, listener, filterValue, handbackValue };

            if (debug) logger.debug("removeNotificationListener"+
                 "(ObjectName,ObjectName,NotificationFilter,Object)",
                             "connectionId=" + connectionId
                             +", name=" + name
                             +", listenerName=" + listener
                             +", filter=" + filterValue
                             +", handback=" + handbackValue);

            doPrivilegedOperation(
              REMOVE_NOTIFICATION_LISTENER_OBJECTNAME_FILTER_HANDBACK,
              params,
              delegationSubject);
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof InstanceNotFoundException)
                throw (InstanceNotFoundException) e;
            if (e instanceof ListenerNotFoundException)
                throw (ListenerNotFoundException) e;
            if (e instanceof IOException)
                throw (IOException) e;
            throw newIOException("Got unexpected server exception: " + e, e);
        }
    }

    @SuppressWarnings("removal")
    public NotificationResult fetchNotifications(long clientSequenceNumber,
                                                 int maxNotifications,
                                                 long timeout)
        throws IOException {

        if (logger.debugOn()) logger.debug("fetchNotifications",
                               "connectionId=" + connectionId
                               +", timeout=" + timeout);

        if (maxNotifications < 0 || timeout < 0)
            throw new IllegalArgumentException("Illegal negative argument");

        final boolean serverTerminated =
            serverCommunicatorAdmin.reqIncoming();
        try {
            if (serverTerminated) {
                // we must not call fetchNotifs() if the server is
                // terminated (timeout elapsed).
                // returns null to force the client to stop fetching
                if (logger.debugOn()) logger.debug("fetchNotifications",
                               "The notification server has been closed, "
                                       + "returns null to force the client to stop fetching");
                return null;
            }
            final long csn = clientSequenceNumber;
            final int mn = maxNotifications;
            final long t = timeout;
            PrivilegedAction<NotificationResult> action =
                new PrivilegedAction<NotificationResult>() {
                    public NotificationResult run() {
                        return getServerNotifFwd().fetchNotifs(csn, t, mn);
                    }
            };
            if (acc == null)
                return action.run();
            else
                return AccessController.doPrivileged(action, acc);
        } finally {
            serverCommunicatorAdmin.rspOutgoing();
        }
    }

    /**
     * <p>Returns a string representation of this object.  In general,
     * the <code>toString</code> method returns a string that
     * "textually represents" this object. The result should be a
     * concise but informative representation that is easy for a
     * person to read.</p>
     *
     * @return a String representation of this object.
     **/
    @Override
    public String toString() {
        return super.toString() + ": connectionId=" + connectionId;
    }

    //------------------------------------------------------------------------
    // private classes
    //------------------------------------------------------------------------

    private class PrivilegedOperation
            implements PrivilegedExceptionAction<Object> {

        public PrivilegedOperation(int operation, Object[] params) {
            this.operation = operation;
            this.params = params;
        }

        public Object run() throws Exception {
            return doOperation(operation, params);
        }

        private int operation;
        private Object[] params;
    }

    //------------------------------------------------------------------------
    // private classes
    //------------------------------------------------------------------------
    private class RMIServerCommunicatorAdmin extends ServerCommunicatorAdmin {
        public RMIServerCommunicatorAdmin(long timeout) {
            super(timeout);
        }

        protected void doStop() {
            try {
                close();
            } catch (IOException ie) {
                logger.warning("RMIServerCommunicatorAdmin-doStop",
                               "Failed to close: " + ie);
                logger.debug("RMIServerCommunicatorAdmin-doStop",ie);
            }
        }

    }


    //------------------------------------------------------------------------
    // private methods
    //------------------------------------------------------------------------

    @SuppressWarnings("removal")
    private ClassLoader getClassLoader(final ObjectName name)
        throws InstanceNotFoundException {
        try {
            return
                AccessController.doPrivileged(
                    new PrivilegedExceptionAction<ClassLoader>() {
                        public ClassLoader run() throws InstanceNotFoundException {
                            return mbeanServer.getClassLoader(name);
                        }
                    },
                    withPermissions(new MBeanPermission("*", "getClassLoader"))
            );
        } catch (PrivilegedActionException pe) {
            throw (InstanceNotFoundException) extractException(pe);
        }
    }

    @SuppressWarnings("removal")
    private ClassLoader getClassLoaderFor(final ObjectName name)
        throws InstanceNotFoundException {
        try {
            return (ClassLoader)
                AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Object>() {
                        public Object run() throws InstanceNotFoundException {
                            return mbeanServer.getClassLoaderFor(name);
                        }
                    },
                    withPermissions(new MBeanPermission("*", "getClassLoaderFor"))
            );
        } catch (PrivilegedActionException pe) {
            throw (InstanceNotFoundException) extractException(pe);
        }
    }

    @SuppressWarnings("removal")
    private Object doPrivilegedOperation(final int operation,
                                         final Object[] params,
                                         final Subject delegationSubject)
        throws PrivilegedActionException, IOException {

        serverCommunicatorAdmin.reqIncoming();
        try {

            final AccessControlContext reqACC;
            if (delegationSubject == null)
                reqACC = acc;
            else {
                if (subject == null) {
                    final String msg =
                        "Subject delegation cannot be enabled unless " +
                        "an authenticated subject is put in place";
                    throw new SecurityException(msg);
                }
                reqACC = subjectDelegator.delegatedContext(
                    acc, delegationSubject, removeCallerContext);
            }

            PrivilegedOperation op =
                new PrivilegedOperation(operation, params);
            if (reqACC == null) {
                try {
                    return op.run();
                } catch (Exception e) {
                    if (e instanceof RuntimeException)
                        throw (RuntimeException) e;
                    throw new PrivilegedActionException(e);
                }
            } else {
                return AccessController.doPrivileged(op, reqACC);
            }
        } catch (Error e) {
            throw new JMXServerErrorException(e.toString(),e);
        } finally {
            serverCommunicatorAdmin.rspOutgoing();
        }
    }

    private Object doOperation(int operation, Object[] params)
        throws Exception {

        switch (operation) {

        case CREATE_MBEAN:
            return mbeanServer.createMBean((String)params[0],
                                           (ObjectName)params[1]);

        case CREATE_MBEAN_LOADER:
            return mbeanServer.createMBean((String)params[0],
                                           (ObjectName)params[1],
                                           (ObjectName)params[2]);

        case CREATE_MBEAN_PARAMS:
            return mbeanServer.createMBean((String)params[0],
                                           (ObjectName)params[1],
                                           (Object[])params[2],
                                           (String[])params[3]);

        case CREATE_MBEAN_LOADER_PARAMS:
            return mbeanServer.createMBean((String)params[0],
                                           (ObjectName)params[1],
                                           (ObjectName)params[2],
                                           (Object[])params[3],
                                           (String[])params[4]);

        case GET_ATTRIBUTE:
            return mbeanServer.getAttribute((ObjectName)params[0],
                                            (String)params[1]);

        case GET_ATTRIBUTES:
            return mbeanServer.getAttributes((ObjectName)params[0],
                                             (String[])params[1]);

        case GET_DEFAULT_DOMAIN:
            return mbeanServer.getDefaultDomain();

        case GET_DOMAINS:
            return mbeanServer.getDomains();

        case GET_MBEAN_COUNT:
            return mbeanServer.getMBeanCount();

        case GET_MBEAN_INFO:
            return mbeanServer.getMBeanInfo((ObjectName)params[0]);

        case GET_OBJECT_INSTANCE:
            return mbeanServer.getObjectInstance((ObjectName)params[0]);

        case INVOKE:
            return mbeanServer.invoke((ObjectName)params[0],
                                      (String)params[1],
                                      (Object[])params[2],
                                      (String[])params[3]);

        case IS_INSTANCE_OF:
            return mbeanServer.isInstanceOf((ObjectName)params[0],
                                            (String)params[1])
                ? Boolean.TRUE : Boolean.FALSE;

        case IS_REGISTERED:
            return mbeanServer.isRegistered((ObjectName)params[0])
                ? Boolean.TRUE : Boolean.FALSE;

        case QUERY_MBEANS:
            return mbeanServer.queryMBeans((ObjectName)params[0],
                                           (QueryExp)params[1]);

        case QUERY_NAMES:
            return mbeanServer.queryNames((ObjectName)params[0],
                                          (QueryExp)params[1]);

        case SET_ATTRIBUTE:
            mbeanServer.setAttribute((ObjectName)params[0],
                                     (Attribute)params[1]);
            return null;

        case SET_ATTRIBUTES:
            return mbeanServer.setAttributes((ObjectName)params[0],
                                             (AttributeList)params[1]);

        case UNREGISTER_MBEAN:
            mbeanServer.unregisterMBean((ObjectName)params[0]);
            return null;

        case ADD_NOTIFICATION_LISTENERS:
            return getServerNotifFwd().addNotificationListener(
                                                (ObjectName)params[0],
                                                (NotificationFilter)params[1]);

        case ADD_NOTIFICATION_LISTENER_OBJECTNAME:
            mbeanServer.addNotificationListener((ObjectName)params[0],
                                                (ObjectName)params[1],
                                                (NotificationFilter)params[2],
                                                params[3]);
            return null;

        case REMOVE_NOTIFICATION_LISTENER:
            getServerNotifFwd().removeNotificationListener(
                                                   (ObjectName)params[0],
                                                   (Integer[])params[1]);
            return null;

        case REMOVE_NOTIFICATION_LISTENER_OBJECTNAME:
            mbeanServer.removeNotificationListener((ObjectName)params[0],
                                                   (ObjectName)params[1]);
            return null;

        case REMOVE_NOTIFICATION_LISTENER_OBJECTNAME_FILTER_HANDBACK:
            mbeanServer.removeNotificationListener(
                                          (ObjectName)params[0],
                                          (ObjectName)params[1],
                                          (NotificationFilter)params[2],
                                          params[3]);
            return null;

        default:
            throw new IllegalArgumentException("Invalid operation");
        }
    }

    private static class SetCcl implements PrivilegedExceptionAction<ClassLoader> {
        private final ClassLoader classLoader;

        SetCcl(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        public ClassLoader run() {
            Thread currentThread = Thread.currentThread();
            ClassLoader old = currentThread.getContextClassLoader();
            currentThread.setContextClassLoader(classLoader);
            return old;
        }
    }

    @SuppressWarnings("removal")
    private <T> T unwrap(final MarshalledObject<?> mo,
                                final ClassLoader cl,
                                final Class<T> wrappedClass,
                                Subject delegationSubject)
            throws IOException {
        if (mo == null) {
            return null;
        }
        try {
            final ClassLoader old = AccessController.doPrivileged(new SetCcl(cl));
            try{
                final AccessControlContext reqACC;
                if (delegationSubject == null)
                    reqACC = acc;
                else {
                    if (subject == null) {
                        final String msg =
                            "Subject delegation cannot be enabled unless " +
                            "an authenticated subject is put in place";
                        throw new SecurityException(msg);
                    }
                    reqACC = subjectDelegator.delegatedContext(
                        acc, delegationSubject, removeCallerContext);
                }
                if(reqACC != null){
                    return AccessController.doPrivileged(
                            (PrivilegedExceptionAction<T>) () ->
                                    wrappedClass.cast(mo.get()), reqACC);
                }else{
                    return wrappedClass.cast(mo.get());
                }
            }finally{
                AccessController.doPrivileged(new SetCcl(old));
            }
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            if (e instanceof ClassNotFoundException) {
                throw new UnmarshalException(e.toString(), e);
            }
            logger.warning("unwrap", "Failed to unmarshall object: " + e);
            logger.debug("unwrap", e);
        }catch (ClassNotFoundException ex) {
            logger.warning("unwrap", "Failed to unmarshall object: " + ex);
            logger.debug("unwrap", ex);
            throw new UnmarshalException(ex.toString(), ex);
        }
        return null;
    }

    private <T> T unwrap(final MarshalledObject<?> mo,
                                final ClassLoader cl1,
                                final ClassLoader cl2,
                                final Class<T> wrappedClass,
                                Subject delegationSubject)
        throws IOException {
        if (mo == null) {
            return null;
        }
        try {
            @SuppressWarnings("removal")
            ClassLoader orderCL = AccessController.doPrivileged(
                new PrivilegedExceptionAction<ClassLoader>() {
                    public ClassLoader run() throws Exception {
                        return new CombinedClassLoader(Thread.currentThread().getContextClassLoader(),
                                new OrderClassLoaders(cl1, cl2));
                    }
                }
            );
            return unwrap(mo, orderCL, wrappedClass,delegationSubject);
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            if (e instanceof ClassNotFoundException) {
                throw new UnmarshalException(e.toString(), e);
            }
            logger.warning("unwrap", "Failed to unmarshall object: " + e);
            logger.debug("unwrap", e);
        }
        return null;
    }

    /**
     * Construct a new IOException with a nested exception.
     * The nested exception is set only if JDK {@literal >= 1.4}
     */
    private static IOException newIOException(String message,
                                              Throwable cause) {
        final IOException x = new IOException(message);
        return EnvHelp.initCause(x,cause);
    }

    /**
     * Iterate until we extract the real exception
     * from a stack of PrivilegedActionExceptions.
     */
    private static Exception extractException(Exception e) {
        while (e instanceof PrivilegedActionException) {
            e = ((PrivilegedActionException)e).getException();
        }
        return e;
    }

    private static final Object[] NO_OBJECTS = new Object[0];
    private static final String[] NO_STRINGS = new String[0];

    /*
     * The JMX spec doesn't explicitly say that a null Object[] or
     * String[] in e.g. MBeanServer.invoke is equivalent to an empty
     * array, but the RI behaves that way.  In the interests of
     * maximal interoperability, we make it so even when we're
     * connected to some other JMX implementation that might not do
     * that.  This should be clarified in the next version of JMX.
     */
    private static Object[] nullIsEmpty(Object[] array) {
        return (array == null) ? NO_OBJECTS : array;
    }

    private static String[] nullIsEmpty(String[] array) {
        return (array == null) ? NO_STRINGS : array;
    }

    /*
     * Similarly, the JMX spec says for some but not all methods in
     * MBeanServer that take an ObjectName target, that if it's null
     * you get this exception.  We specify it for all of them, and
     * make it so for the ones where it's not specified in JMX even if
     * the JMX implementation doesn't do so.
     */
    private static void checkNonNull(String what, Object x) {
        if (x == null) {
            RuntimeException wrapped =
                new IllegalArgumentException(what + " must not be null");
            throw new RuntimeOperationsException(wrapped);
        }
    }

    //------------------------------------------------------------------------
    // private variables
    //------------------------------------------------------------------------

    private final Subject subject;

    private final SubjectDelegator subjectDelegator;

    private final boolean removeCallerContext;

    @SuppressWarnings("removal")
    private final AccessControlContext acc;

    private final RMIServerImpl rmiServer;

    private final MBeanServer mbeanServer;

    private final ClassLoader defaultClassLoader;

    private final ClassLoader defaultContextClassLoader;

    private final ClassLoaderWithRepository classLoaderWithRepository;

    private boolean terminated = false;

    private final String connectionId;

    private final ServerCommunicatorAdmin serverCommunicatorAdmin;

    // Method IDs for doOperation
    //---------------------------

    private static final int
        ADD_NOTIFICATION_LISTENERS                              = 1;
    private static final int
        ADD_NOTIFICATION_LISTENER_OBJECTNAME                    = 2;
    private static final int
        CREATE_MBEAN                                            = 3;
    private static final int
        CREATE_MBEAN_PARAMS                                     = 4;
    private static final int
        CREATE_MBEAN_LOADER                                     = 5;
    private static final int
        CREATE_MBEAN_LOADER_PARAMS                              = 6;
    private static final int
        GET_ATTRIBUTE                                           = 7;
    private static final int
        GET_ATTRIBUTES                                          = 8;
    private static final int
        GET_DEFAULT_DOMAIN                                      = 9;
    private static final int
        GET_DOMAINS                                             = 10;
    private static final int
        GET_MBEAN_COUNT                                         = 11;
    private static final int
        GET_MBEAN_INFO                                          = 12;
    private static final int
        GET_OBJECT_INSTANCE                                     = 13;
    private static final int
        INVOKE                                                  = 14;
    private static final int
        IS_INSTANCE_OF                                          = 15;
    private static final int
        IS_REGISTERED                                           = 16;
    private static final int
        QUERY_MBEANS                                            = 17;
    private static final int
        QUERY_NAMES                                             = 18;
    private static final int
        REMOVE_NOTIFICATION_LISTENER                            = 19;
    private static final int
        REMOVE_NOTIFICATION_LISTENER_OBJECTNAME                 = 20;
    private static final int
        REMOVE_NOTIFICATION_LISTENER_OBJECTNAME_FILTER_HANDBACK = 21;
    private static final int
        SET_ATTRIBUTE                                           = 22;
    private static final int
        SET_ATTRIBUTES                                          = 23;
    private static final int
        UNREGISTER_MBEAN                                        = 24;

    // SERVER NOTIFICATION
    //--------------------

    private ServerNotifForwarder serverNotifForwarder;
    private Map<String, ?> env;

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

    private static final ClassLogger logger =
        new ClassLogger("javax.management.remote.rmi", "RMIConnectionImpl");

    private static final class CombinedClassLoader extends ClassLoader {

        private static final class ClassLoaderWrapper extends ClassLoader {
            ClassLoaderWrapper(ClassLoader cl) {
                super(cl);
            }

            @Override
            protected Class<?> loadClass(String name, boolean resolve)
                    throws ClassNotFoundException {
                return super.loadClass(name, resolve);
            }
        };

        final ClassLoaderWrapper defaultCL;

        private CombinedClassLoader(ClassLoader parent, ClassLoader defaultCL) {
            super(parent);
            this.defaultCL = new ClassLoaderWrapper(defaultCL);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException {
            ReflectUtil.checkPackageAccess(name);
            try {
                super.loadClass(name, resolve);
            } catch(Exception e) {
                for(Throwable t = e; t != null; t = t.getCause()) {
                    if(t instanceof SecurityException) {
                        throw t==e?(SecurityException)t:new SecurityException(t.getMessage(), e);
                    }
                }
            }
            final Class<?> cl = defaultCL.loadClass(name, resolve);
            return cl;
        }

    }
}
