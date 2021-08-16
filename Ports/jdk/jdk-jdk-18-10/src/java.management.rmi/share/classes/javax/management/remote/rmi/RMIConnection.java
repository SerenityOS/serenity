/*
 * Copyright (c) 2002, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.Closeable;
import java.io.IOException;
import java.rmi.MarshalledObject;
import java.rmi.Remote;
import java.util.Set;

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
import javax.management.NotCompliantMBeanException;

import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeMBeanException;
import javax.management.RuntimeOperationsException;
import javax.management.remote.NotificationResult;
import javax.security.auth.Subject;

/**
 * <p>RMI object used to forward an MBeanServer request from a client
 * to its MBeanServer implementation on the server side.  There is one
 * Remote object implementing this interface for each remote client
 * connected to an RMI connector.</p>
 *
 * <p>User code does not usually refer to this interface.  It is
 * specified as part of the public API so that different
 * implementations of that API will interoperate.</p>
 *
 * <p>To ensure that client parameters will be deserialized at the
 * server side with the correct classloader, client parameters such as
 * parameters used to invoke a method are wrapped in a {@link
 * MarshalledObject}.  An implementation of this interface must first
 * get the appropriate class loader for the operation and its target,
 * then deserialize the marshalled parameters with this classloader.
 * Except as noted, a parameter that is a
 * <code>MarshalledObject</code> or <code>MarshalledObject[]</code>
 * must not be null; the behavior is unspecified if it is.</p>
 *
 * <p>Class loading aspects are detailed in the
 * <a href="https://jcp.org/aboutJava/communityprocess/mrel/jsr160/index2.html">
 * JMX Specification, version 1.4</a></p>
 *
 * <p>Most methods in this interface parallel methods in the {@link
 * MBeanServerConnection} interface.  Where an aspect of the behavior
 * of a method is not specified here, it is the same as in the
 * corresponding <code>MBeanServerConnection</code> method.
 *
 * @since 1.5
 */
/*
 * Notice that we omit the type parameter from MarshalledObject everywhere,
 * even though it would add useful information to the documentation.  The
 * reason is that it was only added in Mustang (Java SE 6), whereas versions
 * 1.4 and 2.0 of the JMX API must be implementable on Tiger per our
 * commitments for JSR 255.  This is also why we suppress rawtypes warnings.
 */
@SuppressWarnings("rawtypes")
public interface RMIConnection extends Closeable, Remote {
    /**
     * <p>Returns the connection ID.  This string is different for
     * every open connection to a given RMI connector server.</p>
     *
     * @return the connection ID
     *
     * @see RMIConnector#connect RMIConnector.connect
     *
     * @throws IOException if a general communication exception occurred.
     */
    public String getConnectionId() throws IOException;

    /**
     * <p>Closes this connection.  On return from this method, the RMI
     * object implementing this interface is unexported, so further
     * remote calls to it will fail.</p>
     *
     * @throws IOException if the connection could not be closed,
     * or the Remote object could not be unexported, or there was a
     * communication failure when transmitting the remote close
     * request.
     */
    public void close() throws IOException;

    /**
     * Handles the method {@link
     * javax.management.MBeanServerConnection#createMBean(String,
     * ObjectName)}.
     *
     * @param className The class name of the MBean to be instantiated.
     * @param name The object name of the MBean. May be null.
     * @param delegationSubject The <code>Subject</code> containing the
     * delegation principals or <code>null</code> if the authentication
     * principal is used instead.
     *
     * @return An <code>ObjectInstance</code>, containing the
     * <code>ObjectName</code> and the Java class name of the newly
     * instantiated MBean.  If the contained <code>ObjectName</code>
     * is <code>n</code>, the contained Java class name is
     * <code>{@link #getMBeanInfo getMBeanInfo(n)}.getClassName()</code>.
     *
     * @throws ReflectionException Wraps a
     * <code>java.lang.ClassNotFoundException</code> or a
     * <code>java.lang.Exception</code> that occurred
     * when trying to invoke the MBean's constructor.
     * @throws InstanceAlreadyExistsException The MBean is already
     * under the control of the MBean server.
     * @throws MBeanRegistrationException The
     * <code>preRegister</code> (<code>MBeanRegistration</code>
     * interface) method of the MBean has thrown an exception. The
     * MBean will not be registered.
     * @throws MBeanException The constructor of the MBean has
     * thrown an exception.
     * @throws NotCompliantMBeanException This class is not a JMX
     * compliant MBean.
     * @throws RuntimeOperationsException Wraps a
     * <code>java.lang.IllegalArgumentException</code>: The className
     * passed in parameter is null, the <code>ObjectName</code> passed
     * in parameter contains a pattern or no <code>ObjectName</code>
     * is specified for the MBean.
     * @throws SecurityException if the client, or the delegated Subject
     * if any, does not have permission to perform this operation.
     * @throws IOException if a general communication exception occurred.
     */
    public ObjectInstance createMBean(String className,
                                      ObjectName name,
                                      Subject delegationSubject)
        throws
        ReflectionException,
        InstanceAlreadyExistsException,
        MBeanRegistrationException,
        MBeanException,
        NotCompliantMBeanException,
        IOException;

    /**
     * Handles the method {@link
     * javax.management.MBeanServerConnection#createMBean(String,
     * ObjectName, ObjectName)}.
     *
     * @param className The class name of the MBean to be instantiated.
     * @param name The object name of the MBean. May be null.
     * @param loaderName The object name of the class loader to be used.
     * @param delegationSubject The <code>Subject</code> containing the
     * delegation principals or <code>null</code> if the authentication
     * principal is used instead.
     *
     * @return An <code>ObjectInstance</code>, containing the
     * <code>ObjectName</code> and the Java class name of the newly
     * instantiated MBean.  If the contained <code>ObjectName</code>
     * is <code>n</code>, the contained Java class name is
     * <code>{@link #getMBeanInfo getMBeanInfo(n)}.getClassName()</code>.
     *
     * @throws ReflectionException Wraps a
     * <code>java.lang.ClassNotFoundException</code> or a
     * <code>java.lang.Exception</code> that occurred when trying to
     * invoke the MBean's constructor.
     * @throws InstanceAlreadyExistsException The MBean is already
     * under the control of the MBean server.
     * @throws MBeanRegistrationException The
     * <code>preRegister</code> (<code>MBeanRegistration</code>
     * interface) method of the MBean has thrown an exception. The
     * MBean will not be registered.
     * @throws MBeanException The constructor of the MBean has
     * thrown an exception.
     * @throws NotCompliantMBeanException This class is not a JMX
     * compliant MBean.
     * @throws InstanceNotFoundException The specified class loader
     * is not registered in the MBean server.
     * @throws RuntimeOperationsException Wraps a
     * <code>java.lang.IllegalArgumentException</code>: The className
     * passed in parameter is null, the <code>ObjectName</code> passed
     * in parameter contains a pattern or no <code>ObjectName</code>
     * is specified for the MBean.
     * @throws SecurityException if the client, or the delegated Subject
     * if any, does not have permission to perform this operation.
     * @throws IOException if a general communication exception occurred.
     */
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
        IOException;

    /**
     * Handles the method {@link
     * javax.management.MBeanServerConnection#createMBean(String,
     * ObjectName, Object[], String[])}.  The <code>Object[]</code>
     * parameter is wrapped in a <code>MarshalledObject</code>.
     *
     * @param className The class name of the MBean to be instantiated.
     * @param name The object name of the MBean. May be null.
     * @param params An array containing the parameters of the
     * constructor to be invoked, encapsulated into a
     * <code>MarshalledObject</code>.  The encapsulated array can be
     * null, equivalent to an empty array.
     * @param signature An array containing the signature of the
     * constructor to be invoked.  Can be null, equivalent to an empty
     * array.
     * @param delegationSubject The <code>Subject</code> containing the
     * delegation principals or <code>null</code> if the authentication
     * principal is used instead.
     *
     * @return An <code>ObjectInstance</code>, containing the
     * <code>ObjectName</code> and the Java class name of the newly
     * instantiated MBean.  If the contained <code>ObjectName</code>
     * is <code>n</code>, the contained Java class name is
     * <code>{@link #getMBeanInfo getMBeanInfo(n)}.getClassName()</code>.
     *
     * @throws ReflectionException Wraps a
     * <code>java.lang.ClassNotFoundException</code> or a
     * <code>java.lang.Exception</code> that occurred when trying to
     * invoke the MBean's constructor.
     * @throws InstanceAlreadyExistsException The MBean is already
     * under the control of the MBean server.
     * @throws MBeanRegistrationException The
     * <code>preRegister</code> (<code>MBeanRegistration</code>
     * interface) method of the MBean has thrown an exception. The
     * MBean will not be registered.
     * @throws MBeanException The constructor of the MBean has
     * thrown an exception.
     * @throws NotCompliantMBeanException This class is not a JMX
     * compliant MBean.
     * @throws RuntimeOperationsException Wraps a
     * <code>java.lang.IllegalArgumentException</code>: The className
     * passed in parameter is null, the <code>ObjectName</code> passed
     * in parameter contains a pattern, or no <code>ObjectName</code>
     * is specified for the MBean.
     * @throws SecurityException if the client, or the delegated Subject
     * if any, does not have permission to perform this operation.
     * @throws IOException if a general communication exception occurred.
     */
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
        IOException;

    /**
     * Handles the method {@link
     * javax.management.MBeanServerConnection#createMBean(String,
     * ObjectName, ObjectName, Object[], String[])}.  The
     * <code>Object[]</code> parameter is wrapped in a
     * <code>MarshalledObject</code>.
     *
     * @param className The class name of the MBean to be instantiated.
     * @param name The object name of the MBean. May be null.
     * @param loaderName The object name of the class loader to be used.
     * @param params An array containing the parameters of the
     * constructor to be invoked, encapsulated into a
     * <code>MarshalledObject</code>.  The encapsulated array can be
     * null, equivalent to an empty array.
     * @param signature An array containing the signature of the
     * constructor to be invoked.  Can be null, equivalent to an empty
     * array.
     * @param delegationSubject The <code>Subject</code> containing the
     * delegation principals or <code>null</code> if the authentication
     * principal is used instead.
     *
     * @return An <code>ObjectInstance</code>, containing the
     * <code>ObjectName</code> and the Java class name of the newly
     * instantiated MBean.  If the contained <code>ObjectName</code>
     * is <code>n</code>, the contained Java class name is
     * <code>{@link #getMBeanInfo getMBeanInfo(n)}.getClassName()</code>.
     *
     * @throws ReflectionException Wraps a
     * <code>java.lang.ClassNotFoundException</code> or a
     * <code>java.lang.Exception</code> that occurred when trying to
     * invoke the MBean's constructor.
     * @throws InstanceAlreadyExistsException The MBean is already
     * under the control of the MBean server.
     * @throws MBeanRegistrationException The
     * <code>preRegister</code> (<code>MBeanRegistration</code>
     * interface) method of the MBean has thrown an exception. The
     * MBean will not be registered.
     * @throws MBeanException The constructor of the MBean has
     * thrown an exception.
     * @throws NotCompliantMBeanException This class is not a JMX
     * compliant MBean.
     * @throws InstanceNotFoundException The specified class loader
     * is not registered in the MBean server.
     * @throws RuntimeOperationsException Wraps a
     * <code>java.lang.IllegalArgumentException</code>: The className
     * passed in parameter is null, the <code>ObjectName</code> passed
     * in parameter contains a pattern, or no <code>ObjectName</code>
     * is specified for the MBean.
     * @throws SecurityException if the client, or the delegated Subject
     * if any, does not have permission to perform this operation.
     * @throws IOException if a general communication exception occurred.
     */
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
        IOException;

    /**
     * Handles the method
     * {@link javax.management.MBeanServerConnection#unregisterMBean(ObjectName)}.
     *
     * @param name The object name of the MBean to be unregistered.
     * @param delegationSubject The <code>Subject</code> containing the
     * delegation principals or <code>null</code> if the authentication
     * principal is used instead.
     *
     * @throws InstanceNotFoundException The MBean specified is not
     * registered in the MBean server.
     * @throws MBeanRegistrationException The preDeregister
     * ((<code>MBeanRegistration</code> interface) method of the MBean
     * has thrown an exception.
     * @throws RuntimeOperationsException Wraps a
     * <code>java.lang.IllegalArgumentException</code>: The object
     * name in parameter is null or the MBean you are when trying to
     * unregister is the {@link javax.management.MBeanServerDelegate
     * MBeanServerDelegate} MBean.
     * @throws SecurityException if the client, or the delegated Subject
     * if any, does not have permission to perform this operation.
     * @throws IOException if a general communication exception occurred.
     */
    public void unregisterMBean(ObjectName name, Subject delegationSubject)
        throws
        InstanceNotFoundException,
        MBeanRegistrationException,
        IOException;

    /**
     * Handles the method
     * {@link javax.management.MBeanServerConnection#getObjectInstance(ObjectName)}.
     *
     * @param name The object name of the MBean.
     * @param delegationSubject The <code>Subject</code> containing the
     * delegation principals or <code>null</code> if the authentication
     * principal is used instead.
     *
     * @return The <code>ObjectInstance</code> associated with the MBean
     * specified by <var>name</var>.  The contained <code>ObjectName</code>
     * is <code>name</code> and the contained class name is
     * <code>{@link #getMBeanInfo getMBeanInfo(name)}.getClassName()</code>.
     *
     * @throws InstanceNotFoundException The MBean specified is not
     * registered in the MBean server.
     * @throws RuntimeOperationsException Wraps a
     * <code>java.lang.IllegalArgumentException</code>: The object
     * name in parameter is null.
     * @throws SecurityException if the client, or the delegated Subject
     * if any, does not have permission to perform this operation.
     * @throws IOException if a general communication exception occurred.
     */
    public ObjectInstance getObjectInstance(ObjectName name,
                                            Subject delegationSubject)
        throws InstanceNotFoundException, IOException;

    /**
     * Handles the method {@link
     * javax.management.MBeanServerConnection#queryMBeans(ObjectName,
     * QueryExp)}.  The <code>QueryExp</code> is wrapped in a
     * <code>MarshalledObject</code>.
     *
     * @param name The object name pattern identifying the MBeans to
     * be retrieved. If null or no domain and key properties are
     * specified, all the MBeans registered will be retrieved.
     * @param query The query expression to be applied for selecting
     * MBeans, encapsulated into a <code>MarshalledObject</code>. If
     * the <code>MarshalledObject</code> encapsulates a null value no
     * query expression will be applied for selecting MBeans.
     * @param delegationSubject The <code>Subject</code> containing the
     * delegation principals or <code>null</code> if the authentication
     * principal is used instead.
     *
     * @return A set containing the <code>ObjectInstance</code>
     * objects for the selected MBeans.  If no MBean satisfies the
     * query an empty list is returned.
     *
     * @throws SecurityException if the client, or the delegated Subject
     * if any, does not have permission to perform this operation.
     * @throws IOException if a general communication exception occurred.
     */
    public Set<ObjectInstance>
        queryMBeans(ObjectName name,
                    MarshalledObject query,
                    Subject delegationSubject)
        throws IOException;

    /**
     * Handles the method {@link
     * javax.management.MBeanServerConnection#queryNames(ObjectName,
     * QueryExp)}.  The <code>QueryExp</code> is wrapped in a
     * <code>MarshalledObject</code>.
     *
     * @param name The object name pattern identifying the MBean names
     * to be retrieved. If null or no domain and key properties are
     * specified, the name of all registered MBeans will be retrieved.
     * @param query The query expression to be applied for selecting
     * MBeans, encapsulated into a <code>MarshalledObject</code>. If
     * the <code>MarshalledObject</code> encapsulates a null value no
     * query expression will be applied for selecting MBeans.
     * @param delegationSubject The <code>Subject</code> containing the
     * delegation principals or <code>null</code> if the authentication
     * principal is used instead.
     *
     * @return A set containing the ObjectNames for the MBeans
     * selected.  If no MBean satisfies the query, an empty list is
     * returned.
     *
     * @throws SecurityException if the client, or the delegated Subject
     * if any, does not have permission to perform this operation.
     * @throws IOException if a general communication exception occurred.
     */
    public Set<ObjectName>
        queryNames(ObjectName name,
                   MarshalledObject query,
                   Subject delegationSubject)
        throws IOException;

    /**
     * Handles the method
     * {@link javax.management.MBeanServerConnection#isRegistered(ObjectName)}.
     *
     * @param name The object name of the MBean to be checked.
     * @param delegationSubject The <code>Subject</code> containing the
     * delegation principals or <code>null</code> if the authentication
     * principal is used instead.
     *
     * @return True if the MBean is already registered in the MBean
     * server, false otherwise.
     *
     * @throws RuntimeOperationsException Wraps a
     * <code>java.lang.IllegalArgumentException</code>: The object
     * name in parameter is null.
     * @throws SecurityException if the client, or the delegated Subject
     * if any, does not have permission to perform this operation.
     * @throws IOException if a general communication exception occurred.
     */
    public boolean isRegistered(ObjectName name, Subject delegationSubject)
        throws IOException;

    /**
     * Handles the method
     * {@link javax.management.MBeanServerConnection#getMBeanCount()}.
     *
     * @param delegationSubject The <code>Subject</code> containing the
     * delegation principals or <code>null</code> if the authentication
     * principal is used instead.
     *
     * @return the number of MBeans registered.
     *
     * @throws SecurityException if the client, or the delegated Subject
     * if any, does not have permission to perform this operation.
     * @throws IOException if a general communication exception occurred.
     */
    public Integer getMBeanCount(Subject delegationSubject)
        throws IOException;

    /**
     * Handles the method {@link
     * javax.management.MBeanServerConnection#getAttribute(ObjectName,
     * String)}.
     *
     * @param name The object name of the MBean from which the
     * attribute is to be retrieved.
     * @param attribute A String specifying the name of the attribute
     * to be retrieved.
     * @param delegationSubject The <code>Subject</code> containing the
     * delegation principals or <code>null</code> if the authentication
     * principal is used instead.
     *
     * @return  The value of the retrieved attribute.
     *
     * @throws AttributeNotFoundException The attribute specified
     * is not accessible in the MBean.
     * @throws MBeanException Wraps an exception thrown by the
     * MBean's getter.
     * @throws InstanceNotFoundException The MBean specified is not
     * registered in the MBean server.
     * @throws ReflectionException Wraps a
     * <code>java.lang.Exception</code> thrown when trying to invoke
     * the getter.
     * @throws RuntimeOperationsException Wraps a
     * <code>java.lang.IllegalArgumentException</code>: The object
     * name in parameter is null or the attribute in parameter is
     * null.
     * @throws RuntimeMBeanException Wraps a runtime exception thrown
     * by the MBean's getter.
     * @throws SecurityException if the client, or the delegated Subject
     * if any, does not have permission to perform this operation.
     * @throws IOException if a general communication exception occurred.
     *
     * @see #setAttribute
     */
    public Object getAttribute(ObjectName name,
                               String attribute,
                               Subject delegationSubject)
        throws
        MBeanException,
        AttributeNotFoundException,
        InstanceNotFoundException,
        ReflectionException,
        IOException;

    /**
     * Handles the method {@link
     * javax.management.MBeanServerConnection#getAttributes(ObjectName,
     * String[])}.
     *
     * @param name The object name of the MBean from which the
     * attributes are retrieved.
     * @param attributes A list of the attributes to be retrieved.
     * @param delegationSubject The <code>Subject</code> containing the
     * delegation principals or <code>null</code> if the authentication
     * principal is used instead.
     *
     * @return The list of the retrieved attributes.
     *
     * @throws InstanceNotFoundException The MBean specified is not
     * registered in the MBean server.
     * @throws ReflectionException An exception occurred when
     * trying to invoke the getAttributes method of a Dynamic MBean.
     * @throws RuntimeOperationsException Wrap a
     * <code>java.lang.IllegalArgumentException</code>: The object
     * name in parameter is null or attributes in parameter is null.
     * @throws SecurityException if the client, or the delegated Subject
     * if any, does not have permission to perform this operation.
     * @throws IOException if a general communication exception occurred.
     *
     * @see #setAttributes
     */
    public AttributeList getAttributes(ObjectName name,
                                       String[] attributes,
                                       Subject delegationSubject)
        throws
        InstanceNotFoundException,
        ReflectionException,
        IOException;

    /**
     * Handles the method {@link
     * javax.management.MBeanServerConnection#setAttribute(ObjectName,
     * Attribute)}.  The <code>Attribute</code> parameter is wrapped
     * in a <code>MarshalledObject</code>.
     *
     * @param name The name of the MBean within which the attribute is
     * to be set.
     * @param attribute The identification of the attribute to be set
     * and the value it is to be set to, encapsulated into a
     * <code>MarshalledObject</code>.
     * @param delegationSubject The <code>Subject</code> containing the
     * delegation principals or <code>null</code> if the authentication
     * principal is used instead.
     *
     * @throws InstanceNotFoundException The MBean specified is not
     * registered in the MBean server.
     * @throws AttributeNotFoundException The attribute specified
     * is not accessible in the MBean.
     * @throws InvalidAttributeValueException The value specified
     * for the attribute is not valid.
     * @throws MBeanException Wraps an exception thrown by the
     * MBean's setter.
     * @throws ReflectionException Wraps a
     * <code>java.lang.Exception</code> thrown when trying to invoke
     * the setter.
     * @throws RuntimeOperationsException Wraps a
     * <code>java.lang.IllegalArgumentException</code>: The object
     * name in parameter is null or the attribute in parameter is
     * null.
     * @throws SecurityException if the client, or the delegated Subject
     * if any, does not have permission to perform this operation.
     * @throws IOException if a general communication exception occurred.
     *
     * @see #getAttribute
     */
    public void setAttribute(ObjectName name,
                             MarshalledObject attribute,
                             Subject delegationSubject)
        throws
        InstanceNotFoundException,
        AttributeNotFoundException,
        InvalidAttributeValueException,
        MBeanException,
        ReflectionException,
        IOException;

    /**
     * Handles the method {@link
     * javax.management.MBeanServerConnection#setAttributes(ObjectName,
     * AttributeList)}.  The <code>AttributeList</code> parameter is
     * wrapped in a <code>MarshalledObject</code>.
     *
     * @param name The object name of the MBean within which the
     * attributes are to be set.
     * @param attributes A list of attributes: The identification of
     * the attributes to be set and the values they are to be set to,
     * encapsulated into a <code>MarshalledObject</code>.
     * @param delegationSubject The <code>Subject</code> containing the
     * delegation principals or <code>null</code> if the authentication
     * principal is used instead.
     *
     * @return The list of attributes that were set, with their new
     * values.
     *
     * @throws InstanceNotFoundException The MBean specified is not
     * registered in the MBean server.
     * @throws ReflectionException An exception occurred when
     * trying to invoke the getAttributes method of a Dynamic MBean.
     * @throws RuntimeOperationsException Wraps a
     * <code>java.lang.IllegalArgumentException</code>: The object
     * name in parameter is null or attributes in parameter is null.
     * @throws SecurityException if the client, or the delegated Subject
     * if any, does not have permission to perform this operation.
     * @throws IOException if a general communication exception occurred.
     *
     * @see #getAttributes
     */
    public AttributeList setAttributes(ObjectName name,
                          MarshalledObject attributes,
                          Subject delegationSubject)
        throws
        InstanceNotFoundException,
        ReflectionException,
        IOException;

    /**
     * Handles the method {@link
     * javax.management.MBeanServerConnection#invoke(ObjectName,
     * String, Object[], String[])}.  The <code>Object[]</code>
     * parameter is wrapped in a <code>MarshalledObject</code>.
     *
     * @param name The object name of the MBean on which the method is
     * to be invoked.
     * @param operationName The name of the operation to be invoked.
     * @param params An array containing the parameters to be set when
     * the operation is invoked, encapsulated into a
     * <code>MarshalledObject</code>.  The encapsulated array can be
     * null, equivalent to an empty array.
     * @param signature An array containing the signature of the
     * operation. The class objects will be loaded using the same
     * class loader as the one used for loading the MBean on which the
     * operation was invoked.  Can be null, equivalent to an empty
     * array.
     * @param delegationSubject The <code>Subject</code> containing the
     * delegation principals or <code>null</code> if the authentication
     * principal is used instead.
     *
     * @return The object returned by the operation, which represents
     * the result of invoking the operation on the MBean specified.
     *
     * @throws InstanceNotFoundException The MBean specified is not
     * registered in the MBean server.
     * @throws MBeanException Wraps an exception thrown by the
     * MBean's invoked method.
     * @throws ReflectionException Wraps a
     * <code>java.lang.Exception</code> thrown while trying to invoke
     * the method.
     * @throws SecurityException if the client, or the delegated Subject
     * if any, does not have permission to perform this operation.
     * @throws IOException if a general communication exception occurred.
     * @throws RuntimeOperationsException Wraps an {@link
     * IllegalArgumentException} when <code>name</code> or
     * <code>operationName</code> is null.
     */
    public Object invoke(ObjectName name,
                         String operationName,
                         MarshalledObject params,
                         String signature[],
                         Subject delegationSubject)
        throws
        InstanceNotFoundException,
        MBeanException,
        ReflectionException,
        IOException;

    /**
     * Handles the method
     * {@link javax.management.MBeanServerConnection#getDefaultDomain()}.
     *
     * @param delegationSubject The <code>Subject</code> containing the
     * delegation principals or <code>null</code> if the authentication
     * principal is used instead.
     *
     * @return the default domain.
     *
     * @throws SecurityException if the client, or the delegated Subject
     * if any, does not have permission to perform this operation.
     * @throws IOException if a general communication exception occurred.
     */
    public String getDefaultDomain(Subject delegationSubject)
        throws IOException;

    /**
     * Handles the method
     * {@link javax.management.MBeanServerConnection#getDomains()}.
     *
     * @param delegationSubject The <code>Subject</code> containing the
     * delegation principals or <code>null</code> if the authentication
     * principal is used instead.
     *
     * @return the list of domains.
     *
     * @throws SecurityException if the client, or the delegated Subject
     * if any, does not have permission to perform this operation.
     * @throws IOException if a general communication exception occurred.
     */
    public String[] getDomains(Subject delegationSubject)
        throws IOException;

    /**
     * Handles the method
     * {@link javax.management.MBeanServerConnection#getMBeanInfo(ObjectName)}.
     *
     * @param name The name of the MBean to analyze
     * @param delegationSubject The <code>Subject</code> containing the
     * delegation principals or <code>null</code> if the authentication
     * principal is used instead.
     *
     * @return An instance of <code>MBeanInfo</code> allowing the
     * retrieval of all attributes and operations of this MBean.
     *
     * @throws IntrospectionException An exception occurred during
     * introspection.
     * @throws InstanceNotFoundException The MBean specified was
     * not found.
     * @throws ReflectionException An exception occurred when
     * trying to invoke the getMBeanInfo of a Dynamic MBean.
     * @throws SecurityException if the client, or the delegated Subject
     * if any, does not have permission to perform this operation.
     * @throws IOException if a general communication exception occurred.
     * @throws RuntimeOperationsException Wraps a
     * <code>java.lang.IllegalArgumentException</code>: The object
     * name in parameter is null.
     */
    public MBeanInfo getMBeanInfo(ObjectName name, Subject delegationSubject)
        throws
        InstanceNotFoundException,
        IntrospectionException,
        ReflectionException,
        IOException;

    /**
     * Handles the method {@link
     * javax.management.MBeanServerConnection#isInstanceOf(ObjectName,
     * String)}.
     *
     * @param name The <code>ObjectName</code> of the MBean.
     * @param className The name of the class.
     * @param delegationSubject The <code>Subject</code> containing the
     * delegation principals or <code>null</code> if the authentication
     * principal is used instead.
     *
     * @return true if the MBean specified is an instance of the
     * specified class according to the rules above, false otherwise.
     *
     * @throws InstanceNotFoundException The MBean specified is not
     * registered in the MBean server.
     * @throws SecurityException if the client, or the delegated Subject
     * if any, does not have permission to perform this operation.
     * @throws IOException if a general communication exception occurred.
     * @throws RuntimeOperationsException Wraps a
     * <code>java.lang.IllegalArgumentException</code>: The object
     * name in parameter is null.
     */
    public boolean isInstanceOf(ObjectName name,
                                String className,
                                Subject delegationSubject)
        throws InstanceNotFoundException, IOException;

    /**
     * Handles the method {@link
     * javax.management.MBeanServerConnection#addNotificationListener(ObjectName,
     * ObjectName, NotificationFilter, Object)}.  The
     * <code>NotificationFilter</code> parameter is wrapped in a
     * <code>MarshalledObject</code>.  The <code>Object</code>
     * (handback) parameter is also wrapped in a
     * <code>MarshalledObject</code>.
     *
     * @param name The name of the MBean on which the listener should
     * be added.
     * @param listener The object name of the listener which will
     * handle the notifications emitted by the registered MBean.
     * @param filter The filter object, encapsulated into a
     * <code>MarshalledObject</code>. If filter encapsulated in the
     * <code>MarshalledObject</code> has a null value, no filtering
     * will be performed before handling notifications.
     * @param handback The context to be sent to the listener when a
     * notification is emitted, encapsulated into a
     * <code>MarshalledObject</code>.
     * @param delegationSubject The <code>Subject</code> containing the
     * delegation principals or <code>null</code> if the authentication
     * principal is used instead.
     *
     * @throws InstanceNotFoundException The MBean name of the
     * notification listener or of the notification broadcaster does
     * not match any of the registered MBeans.
     * @throws RuntimeOperationsException Wraps an {@link
     * IllegalArgumentException}.  The MBean named by
     * <code>listener</code> exists but does not implement the
     * {@link javax.management.NotificationListener} interface,
     * or <code>name</code> or <code>listener</code> is null.
     * @throws SecurityException if the client, or the delegated Subject
     * if any, does not have permission to perform this operation.
     * @throws IOException if a general communication exception occurred.
     *
     * @see #removeNotificationListener(ObjectName, ObjectName, Subject)
     * @see #removeNotificationListener(ObjectName, ObjectName,
     * MarshalledObject, MarshalledObject, Subject)
     */
    public void addNotificationListener(ObjectName name,
                        ObjectName listener,
                        MarshalledObject filter,
                        MarshalledObject handback,
                        Subject delegationSubject)
        throws InstanceNotFoundException, IOException;

    /**
     * Handles the method {@link
     * javax.management.MBeanServerConnection#removeNotificationListener(ObjectName,
     * ObjectName)}.
     *
     * @param name The name of the MBean on which the listener should
     * be removed.
     * @param listener The object name of the listener to be removed.
     * @param delegationSubject The <code>Subject</code> containing the
     * delegation principals or <code>null</code> if the authentication
     * principal is used instead.
     *
     * @throws InstanceNotFoundException The MBean name provided
     * does not match any of the registered MBeans.
     * @throws ListenerNotFoundException The listener is not
     * registered in the MBean.
     * @throws SecurityException if the client, or the delegated Subject
     * if any, does not have permission to perform this operation.
     * @throws IOException if a general communication exception occurred.
     * @throws RuntimeOperationsException Wraps an {@link
     * IllegalArgumentException} when <code>name</code> or
     * <code>listener</code> is null.
     *
     * @see #addNotificationListener
     */
    public void removeNotificationListener(ObjectName name,
                                           ObjectName listener,
                                           Subject delegationSubject)
        throws
        InstanceNotFoundException,
        ListenerNotFoundException,
        IOException;

    /**
     * Handles the method {@link
     * javax.management.MBeanServerConnection#removeNotificationListener(ObjectName,
     * ObjectName, NotificationFilter, Object)}.  The
     * <code>NotificationFilter</code> parameter is wrapped in a
     * <code>MarshalledObject</code>.  The <code>Object</code>
     * parameter is also wrapped in a <code>MarshalledObject</code>.
     *
     * @param name The name of the MBean on which the listener should
     * be removed.
     * @param listener A listener that was previously added to this
     * MBean.
     * @param filter The filter that was specified when the listener
     * was added, encapsulated into a <code>MarshalledObject</code>.
     * @param handback The handback that was specified when the
     * listener was added, encapsulated into a <code>MarshalledObject</code>.
     * @param delegationSubject The <code>Subject</code> containing the
     * delegation principals or <code>null</code> if the authentication
     * principal is used instead.
     *
     * @throws InstanceNotFoundException The MBean name provided
     * does not match any of the registered MBeans.
     * @throws ListenerNotFoundException The listener is not
     * registered in the MBean, or it is not registered with the given
     * filter and handback.
     * @throws SecurityException if the client, or the delegated Subject
     * if any, does not have permission to perform this operation.
     * @throws IOException if a general communication exception occurred.
     * @throws RuntimeOperationsException Wraps an {@link
     * IllegalArgumentException} when <code>name</code> or
     * <code>listener</code> is null.
     *
     * @see #addNotificationListener
     */
    public void removeNotificationListener(ObjectName name,
                      ObjectName listener,
                      MarshalledObject filter,
                      MarshalledObject handback,
                      Subject delegationSubject)
        throws
        InstanceNotFoundException,
        ListenerNotFoundException,
        IOException;

    // Special Handling of Notifications -------------------------------------

    /**
     * <p>Handles the method {@link
     * javax.management.MBeanServerConnection#addNotificationListener(ObjectName,
     * NotificationListener, NotificationFilter, Object)}.</p>
     *
     * <p>Register for notifications from the given MBeans that match
     * the given filters.  The remote client can subsequently retrieve
     * the notifications using the {@link #fetchNotifications
     * fetchNotifications} method.</p>
     *
     * <p>For each listener, the original
     * <code>NotificationListener</code> and <code>handback</code> are
     * kept on the client side; in order for the client to be able to
     * identify them, the server generates and returns a unique
     * <code>listenerID</code>.  This <code>listenerID</code> is
     * forwarded with the <code>Notifications</code> to the remote
     * client.</p>
     *
     * <p>If any one of the given (name, filter) pairs cannot be
     * registered, then the operation fails with an exception, and no
     * names or filters are registered.</p>
     *
     * @param names the <code>ObjectNames</code> identifying the
     * MBeans emitting the Notifications.
     * @param filters an array of marshalled representations of the
     * <code>NotificationFilters</code>.  Elements of this array can
     * be null.
     * @param delegationSubjects the <code>Subjects</code> on behalf
     * of which the listeners are being added.  Elements of this array
     * can be null.  Also, the <code>delegationSubjects</code>
     * parameter itself can be null, which is equivalent to an array
     * of null values with the same size as the <code>names</code> and
     * <code>filters</code> arrays.
     *
     * @return an array of <code>listenerIDs</code> identifying the
     * local listeners.  This array has the same number of elements as
     * the parameters.
     *
     * @throws IllegalArgumentException if <code>names</code> or
     * <code>filters</code> is null, or if <code>names</code> contains
     * a null element, or if the three arrays do not all have the same
     * size.
     * @throws ClassCastException if one of the elements of
     * <code>filters</code> unmarshalls as a non-null object that is
     * not a <code>NotificationFilter</code>.
     * @throws InstanceNotFoundException if one of the
     * <code>names</code> does not correspond to any registered MBean.
     * @throws SecurityException if, for one of the MBeans, the
     * client, or the delegated Subject if any, does not have
     * permission to add a listener.
     * @throws IOException if a general communication exception occurred.
     */
    public Integer[] addNotificationListeners(ObjectName[] names,
                    MarshalledObject[] filters,
                    Subject[] delegationSubjects)
        throws InstanceNotFoundException, IOException;

    /**
     * <p>Handles the
     * {@link javax.management.MBeanServerConnection#removeNotificationListener(ObjectName,NotificationListener)
     * removeNotificationListener(ObjectName, NotificationListener)} and
     * {@link javax.management.MBeanServerConnection#removeNotificationListener(ObjectName,NotificationListener,NotificationFilter,Object)
     * removeNotificationListener(ObjectName, NotificationListener, NotificationFilter, Object)} methods.</p>
     *
     * <p>This method removes one or more
     * <code>NotificationListener</code>s from a given MBean in the
     * MBean server.</p>
     *
     * <p>The <code>NotificationListeners</code> are identified by the
     * IDs which were returned by the {@link
     * #addNotificationListeners(ObjectName[], MarshalledObject[],
     * Subject[])} method.</p>
     *
     * @param name the <code>ObjectName</code> identifying the MBean
     * emitting the Notifications.
     * @param listenerIDs the list of the IDs corresponding to the
     * listeners to remove.
     * @param delegationSubject The <code>Subject</code> containing the
     * delegation principals or <code>null</code> if the authentication
     * principal is used instead.
     *
     * @throws InstanceNotFoundException if the given
     * <code>name</code> does not correspond to any registered MBean.
     * @throws ListenerNotFoundException if one of the listeners was
     * not found on the server side.  This exception can happen if the
     * MBean discarded a listener for some reason other than a call to
     * <code>MBeanServer.removeNotificationListener</code>.
     * @throws SecurityException if the client, or the delegated Subject
     * if any, does not have permission to remove the listeners.
     * @throws IOException if a general communication exception occurred.
     * @throws IllegalArgumentException if <code>ObjectName</code> or
     * <code>listenerIds</code> is null or if <code>listenerIds</code>
     * contains a null element.
     */
    public void removeNotificationListeners(ObjectName name,
                                            Integer[] listenerIDs,
                                            Subject delegationSubject)
        throws
        InstanceNotFoundException,
        ListenerNotFoundException,
        IOException;

    /**
     * <p>Retrieves notifications from the connector server.  This
     * method can block until there is at least one notification or
     * until the specified timeout is reached.  The method can also
     * return at any time with zero notifications.</p>
     *
     * <p>A notification can be included in the result if its sequence
     * number is no less than <code>clientSequenceNumber</code> and
     * this client has registered at least one listener for the MBean
     * generating the notification, with a filter that accepts the
     * notification.  Each listener that is interested in the
     * notification is identified by an Integer ID that was returned
     * by {@link #addNotificationListeners(ObjectName[],
     * MarshalledObject[], Subject[])}.</p>
     *
     * @param clientSequenceNumber the first sequence number that the
     * client is interested in.  If negative, it is interpreted as
     * meaning the sequence number that the next notification will
     * have.
     *
     * @param maxNotifications the maximum number of different
     * notifications to return.  The <code>TargetedNotification</code>
     * array in the returned <code>NotificationResult</code> can have
     * more elements than this if the same notification appears more
     * than once.  The behavior is unspecified if this parameter is
     * negative.
     *
     * @param timeout the maximum time in milliseconds to wait for a
     * notification to arrive.  This can be 0 to indicate that the
     * method should not wait if there are no notifications, but
     * should return at once.  It can be <code>Long.MAX_VALUE</code>
     * to indicate that there is no timeout.  The behavior is
     * unspecified if this parameter is negative.
     *
     * @return A <code>NotificationResult</code>.
     *
     * @throws IOException if a general communication exception occurred.
     */
    public NotificationResult fetchNotifications(long clientSequenceNumber,
                                                 int maxNotifications,
                                                 long timeout)
            throws IOException;
}
