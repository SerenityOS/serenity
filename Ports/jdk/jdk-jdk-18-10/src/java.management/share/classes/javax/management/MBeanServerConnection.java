/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
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


// java import
import java.io.IOException;
import java.util.Set;


/**
 * This interface represents a way to talk to an MBean server, whether
 * local or remote.  The {@link MBeanServer} interface, representing a
 * local MBean server, extends this interface.
 *
 *
 * @since 1.5
 */
public interface MBeanServerConnection {
    /**
     * <p>Instantiates and registers an MBean in the MBean server.  The
     * MBean server will use its {@link
     * javax.management.loading.ClassLoaderRepository Default Loader
     * Repository} to load the class of the MBean.  An object name is
     * associated with the MBean.  If the object name given is null, the
     * MBean must provide its own name by implementing the {@link
     * javax.management.MBeanRegistration MBeanRegistration} interface
     * and returning the name from the {@link
     * MBeanRegistration#preRegister preRegister} method.</p>
     *
     * <p>This method is equivalent to {@link
     * #createMBean(String,ObjectName,Object[],String[])
     * createMBean(className, name, (Object[]) null, (String[])
     * null)}.</p>
     *
     * @param className The class name of the MBean to be instantiated.
     * @param name The object name of the MBean. May be null.
     *
     * @return An <CODE>ObjectInstance</CODE>, containing the
     * <CODE>ObjectName</CODE> and the Java class name of the newly
     * instantiated MBean.  If the contained <code>ObjectName</code>
     * is <code>n</code>, the contained Java class name is
     * <code>{@link #getMBeanInfo getMBeanInfo(n)}.getClassName()</code>.
     *
     * @exception ReflectionException Wraps a
     * <CODE>java.lang.ClassNotFoundException</CODE> or a
     * <CODE>java.lang.Exception</CODE> that occurred
     * when trying to invoke the MBean's constructor.
     * @exception InstanceAlreadyExistsException The MBean is already
     * under the control of the MBean server.
     * @exception MBeanRegistrationException The
     * <CODE>preRegister</CODE> (<CODE>MBeanRegistration</CODE>
     * interface) method of the MBean has thrown an exception. The
     * MBean will not be registered.
     * @exception RuntimeMBeanException If the MBean's constructor or its
     * {@code preRegister} or {@code postRegister} method threw
     * a {@code RuntimeException}. If the <CODE>postRegister</CODE>
     * (<CODE>MBeanRegistration</CODE> interface) method of the MBean throws a
     * <CODE>RuntimeException</CODE>, the <CODE>createMBean</CODE> method will
     * throw a <CODE>RuntimeMBeanException</CODE>, although the MBean creation
     * and registration succeeded. In such a case, the MBean will be actually
     * registered even though the <CODE>createMBean</CODE> method
     * threw an exception. Note that <CODE>RuntimeMBeanException</CODE> can
     * also be thrown by <CODE>preRegister</CODE>, in which case the MBean
     * will not be registered.
     * @exception RuntimeErrorException If the <CODE>postRegister</CODE>
     * (<CODE>MBeanRegistration</CODE> interface) method of the MBean throws an
     * <CODE>Error</CODE>, the <CODE>createMBean</CODE> method will
     * throw a <CODE>RuntimeErrorException</CODE>, although the MBean creation
     * and registration succeeded. In such a case, the MBean will be actually
     * registered even though the <CODE>createMBean</CODE> method
     * threw an exception.  Note that <CODE>RuntimeErrorException</CODE> can
     * also be thrown by <CODE>preRegister</CODE>, in which case the MBean
     * will not be registered.
     * @exception MBeanException The constructor of the MBean has
     * thrown an exception
     * @exception NotCompliantMBeanException This class is not a JMX
     * compliant MBean
     * @exception RuntimeOperationsException Wraps a
     * <CODE>java.lang.IllegalArgumentException</CODE>: The className
     * passed in parameter is null, the <CODE>ObjectName</CODE> passed
     * in parameter contains a pattern or no <CODE>ObjectName</CODE>
     * is specified for the MBean.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     * @see javax.management.MBeanRegistration
     */
    public ObjectInstance createMBean(String className, ObjectName name)
            throws ReflectionException, InstanceAlreadyExistsException,
                   MBeanRegistrationException, MBeanException,
                   NotCompliantMBeanException, IOException;

    /**
     * <p>Instantiates and registers an MBean in the MBean server.  The
     * class loader to be used is identified by its object name. An
     * object name is associated with the MBean. If the object name of
     * the loader is null, the ClassLoader that loaded the MBean
     * server will be used.  If the MBean's object name given is null,
     * the MBean must provide its own name by implementing the {@link
     * javax.management.MBeanRegistration MBeanRegistration} interface
     * and returning the name from the {@link
     * MBeanRegistration#preRegister preRegister} method.</p>
     *
     * <p>This method is equivalent to {@link
     * #createMBean(String,ObjectName,ObjectName,Object[],String[])
     * createMBean(className, name, loaderName, (Object[]) null,
     * (String[]) null)}.</p>
     *
     * @param className The class name of the MBean to be instantiated.
     * @param name The object name of the MBean. May be null.
     * @param loaderName The object name of the class loader to be used.
     *
     * @return An <CODE>ObjectInstance</CODE>, containing the
     * <CODE>ObjectName</CODE> and the Java class name of the newly
     * instantiated MBean.  If the contained <code>ObjectName</code>
     * is <code>n</code>, the contained Java class name is
     * <code>{@link #getMBeanInfo getMBeanInfo(n)}.getClassName()</code>.
     *
     * @exception ReflectionException Wraps a
     * <CODE>java.lang.ClassNotFoundException</CODE> or a
     * <CODE>java.lang.Exception</CODE> that occurred when trying to
     * invoke the MBean's constructor.
     * @exception InstanceAlreadyExistsException The MBean is already
     * under the control of the MBean server.
     * @exception MBeanRegistrationException The
     * <CODE>preRegister</CODE> (<CODE>MBeanRegistration</CODE>
     * interface) method of the MBean has thrown an exception. The
     * MBean will not be registered.
     * @exception RuntimeMBeanException If the MBean's constructor or its
     * {@code preRegister} or {@code postRegister} method threw
     * a {@code RuntimeException}. If the <CODE>postRegister</CODE>
     * (<CODE>MBeanRegistration</CODE> interface) method of the MBean throws a
     * <CODE>RuntimeException</CODE>, the <CODE>createMBean</CODE> method will
     * throw a <CODE>RuntimeMBeanException</CODE>, although the MBean creation
     * and registration succeeded. In such a case, the MBean will be actually
     * registered even though the <CODE>createMBean</CODE> method
     * threw an exception.  Note that <CODE>RuntimeMBeanException</CODE> can
     * also be thrown by <CODE>preRegister</CODE>, in which case the MBean
     * will not be registered.
     * @exception RuntimeErrorException If the <CODE>postRegister</CODE>
     * (<CODE>MBeanRegistration</CODE> interface) method of the MBean throws an
     * <CODE>Error</CODE>, the <CODE>createMBean</CODE> method will
     * throw a <CODE>RuntimeErrorException</CODE>, although the MBean creation
     * and registration succeeded. In such a case, the MBean will be actually
     * registered even though the <CODE>createMBean</CODE> method
     * threw an exception.  Note that <CODE>RuntimeErrorException</CODE> can
     * also be thrown by <CODE>preRegister</CODE>, in which case the MBean
     * will not be registered.
     * @exception MBeanException The constructor of the MBean has
     * thrown an exception
     * @exception NotCompliantMBeanException This class is not a JMX
     * compliant MBean
     * @exception InstanceNotFoundException The specified class loader
     * is not registered in the MBean server.
     * @exception RuntimeOperationsException Wraps a
     * <CODE>java.lang.IllegalArgumentException</CODE>: The className
     * passed in parameter is null, the <CODE>ObjectName</CODE> passed
     * in parameter contains a pattern or no <CODE>ObjectName</CODE>
     * is specified for the MBean.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     * @see javax.management.MBeanRegistration
     */
    public ObjectInstance createMBean(String className, ObjectName name,
                                      ObjectName loaderName)
            throws ReflectionException, InstanceAlreadyExistsException,
                   MBeanRegistrationException, MBeanException,
                   NotCompliantMBeanException, InstanceNotFoundException,
                   IOException;


    /**
     * Instantiates and registers an MBean in the MBean server.  The
     * MBean server will use its {@link
     * javax.management.loading.ClassLoaderRepository Default Loader
     * Repository} to load the class of the MBean.  An object name is
     * associated with the MBean.  If the object name given is null, the
     * MBean must provide its own name by implementing the {@link
     * javax.management.MBeanRegistration MBeanRegistration} interface
     * and returning the name from the {@link
     * MBeanRegistration#preRegister preRegister} method.
     *
     * @param className The class name of the MBean to be instantiated.
     * @param name The object name of the MBean. May be null.
     * @param params An array containing the parameters of the
     * constructor to be invoked.
     * @param signature An array containing the signature of the
     * constructor to be invoked.
     *
     * @return An <CODE>ObjectInstance</CODE>, containing the
     * <CODE>ObjectName</CODE> and the Java class name of the newly
     * instantiated MBean.  If the contained <code>ObjectName</code>
     * is <code>n</code>, the contained Java class name is
     * <code>{@link #getMBeanInfo getMBeanInfo(n)}.getClassName()</code>.
     *
     * @exception ReflectionException Wraps a
     * <CODE>java.lang.ClassNotFoundException</CODE> or a
     * <CODE>java.lang.Exception</CODE> that occurred when trying to
     * invoke the MBean's constructor.
     * @exception InstanceAlreadyExistsException The MBean is already
     * under the control of the MBean server.
     * @exception MBeanRegistrationException The
     * <CODE>preRegister</CODE> (<CODE>MBeanRegistration</CODE>
     * interface) method of the MBean has thrown an exception. The
     * MBean will not be registered.
     * @exception RuntimeMBeanException If the MBean's constructor or its
     * {@code preRegister} or {@code postRegister} method threw
     * a {@code RuntimeException}. If the <CODE>postRegister</CODE>
     * (<CODE>MBeanRegistration</CODE> interface) method of the MBean throws a
     * <CODE>RuntimeException</CODE>, the <CODE>createMBean</CODE> method will
     * throw a <CODE>RuntimeMBeanException</CODE>, although the MBean creation
     * and registration succeeded. In such a case, the MBean will be actually
     * registered even though the <CODE>createMBean</CODE> method
     * threw an exception.  Note that <CODE>RuntimeMBeanException</CODE> can
     * also be thrown by <CODE>preRegister</CODE>, in which case the MBean
     * will not be registered.
     * @exception RuntimeErrorException If the <CODE>postRegister</CODE>
     * (<CODE>MBeanRegistration</CODE> interface) method of the MBean throws an
     * <CODE>Error</CODE>, the <CODE>createMBean</CODE> method will
     * throw a <CODE>RuntimeErrorException</CODE>, although the MBean creation
     * and registration succeeded. In such a case, the MBean will be actually
     * registered even though the <CODE>createMBean</CODE> method
     * threw an exception.  Note that <CODE>RuntimeErrorException</CODE> can
     * also be thrown by <CODE>preRegister</CODE>, in which case the MBean
     * will not be registered.
     * @exception MBeanException The constructor of the MBean has
     * thrown an exception
     * @exception NotCompliantMBeanException This class is not a JMX
     * compliant MBean
     * @exception RuntimeOperationsException Wraps a
     * <CODE>java.lang.IllegalArgumentException</CODE>: The className
     * passed in parameter is null, the <CODE>ObjectName</CODE> passed
     * in parameter contains a pattern or no <CODE>ObjectName</CODE>
     * is specified for the MBean.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     * @see javax.management.MBeanRegistration
     */
    public ObjectInstance createMBean(String className, ObjectName name,
                                      Object params[], String signature[])
            throws ReflectionException, InstanceAlreadyExistsException,
                   MBeanRegistrationException, MBeanException,
                   NotCompliantMBeanException, IOException;

    /**
     * <p>Instantiates and registers an MBean in the MBean server.  The
     * class loader to be used is identified by its object name. An
     * object name is associated with the MBean. If the object name of
     * the loader is not specified, the ClassLoader that loaded the
     * MBean server will be used.  If the MBean object name given is
     * null, the MBean must provide its own name by implementing the
     * {@link javax.management.MBeanRegistration MBeanRegistration}
     * interface and returning the name from the {@link
     * MBeanRegistration#preRegister preRegister} method.
     *
     * @param className The class name of the MBean to be instantiated.
     * @param name The object name of the MBean. May be null.
     * @param params An array containing the parameters of the
     * constructor to be invoked.
     * @param signature An array containing the signature of the
     * constructor to be invoked.
     * @param loaderName The object name of the class loader to be used.
     *
     * @return An <CODE>ObjectInstance</CODE>, containing the
     * <CODE>ObjectName</CODE> and the Java class name of the newly
     * instantiated MBean.  If the contained <code>ObjectName</code>
     * is <code>n</code>, the contained Java class name is
     * <code>{@link #getMBeanInfo getMBeanInfo(n)}.getClassName()</code>.
     *
     * @exception ReflectionException Wraps a
     * <CODE>java.lang.ClassNotFoundException</CODE> or a
     * <CODE>java.lang.Exception</CODE> that occurred when trying to
     * invoke the MBean's constructor.
     * @exception InstanceAlreadyExistsException The MBean is already
     * under the control of the MBean server.
     * @exception MBeanRegistrationException The
     * <CODE>preRegister</CODE> (<CODE>MBeanRegistration</CODE>
     * interface) method of the MBean has thrown an exception. The
     * MBean will not be registered.
     * @exception RuntimeMBeanException The MBean's constructor or its
     * {@code preRegister} or {@code postRegister} method threw
     * a {@code RuntimeException}. If the <CODE>postRegister</CODE>
     * (<CODE>MBeanRegistration</CODE> interface) method of the MBean throws a
     * <CODE>RuntimeException</CODE>, the <CODE>createMBean</CODE> method will
     * throw a <CODE>RuntimeMBeanException</CODE>, although the MBean creation
     * and registration succeeded. In such a case, the MBean will be actually
     * registered even though the <CODE>createMBean</CODE> method
     * threw an exception.  Note that <CODE>RuntimeMBeanException</CODE> can
     * also be thrown by <CODE>preRegister</CODE>, in which case the MBean
     * will not be registered.
     * @exception RuntimeErrorException If the <CODE>postRegister</CODE> method
     * (<CODE>MBeanRegistration</CODE> interface) method of the MBean throws an
     * <CODE>Error</CODE>, the <CODE>createMBean</CODE> method will
     * throw a <CODE>RuntimeErrorException</CODE>, although the MBean creation
     * and registration succeeded. In such a case, the MBean will be actually
     * registered even though the <CODE>createMBean</CODE> method
     * threw an exception.  Note that <CODE>RuntimeErrorException</CODE> can
     * also be thrown by <CODE>preRegister</CODE>, in which case the MBean
     * will not be registered.
     * @exception MBeanException The constructor of the MBean has
     * thrown an exception
     * @exception NotCompliantMBeanException This class is not a JMX
     * compliant MBean
     * @exception InstanceNotFoundException The specified class loader
     * is not registered in the MBean server.
     * @exception RuntimeOperationsException Wraps a
     * <CODE>java.lang.IllegalArgumentException</CODE>: The className
     * passed in parameter is null, the <CODE>ObjectName</CODE> passed
     * in parameter contains a pattern or no <CODE>ObjectName</CODE>
     * is specified for the MBean.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     * @see javax.management.MBeanRegistration
     */
    public ObjectInstance createMBean(String className, ObjectName name,
                                      ObjectName loaderName, Object params[],
                                      String signature[])
            throws ReflectionException, InstanceAlreadyExistsException,
                   MBeanRegistrationException, MBeanException,
                   NotCompliantMBeanException, InstanceNotFoundException,
                   IOException;

    /**
     * Unregisters an MBean from the MBean server. The MBean is
     * identified by its object name. Once the method has been
     * invoked, the MBean may no longer be accessed by its object
     * name.
     *
     * @param name The object name of the MBean to be unregistered.
     *
     * @exception InstanceNotFoundException The MBean specified is not
     * registered in the MBean server.
     * @exception MBeanRegistrationException The preDeregister
     * ((<CODE>MBeanRegistration</CODE> interface) method of the MBean
     * has thrown an exception.
     * @exception RuntimeMBeanException If the <CODE>postDeregister</CODE>
     * (<CODE>MBeanRegistration</CODE> interface) method of the MBean throws a
     * <CODE>RuntimeException</CODE>, the <CODE>unregisterMBean</CODE> method
     * will throw a <CODE>RuntimeMBeanException</CODE>, although the MBean
     * unregistration succeeded. In such a case, the MBean will be actually
     * unregistered even though the <CODE>unregisterMBean</CODE> method
     * threw an exception.  Note that <CODE>RuntimeMBeanException</CODE> can
     * also be thrown by <CODE>preDeregister</CODE>, in which case the MBean
     * will remain registered.
     * @exception RuntimeErrorException If the <CODE>postDeregister</CODE>
     * (<CODE>MBeanRegistration</CODE> interface) method of the MBean throws an
     * <CODE>Error</CODE>, the <CODE>unregisterMBean</CODE> method will
     * throw a <CODE>RuntimeErrorException</CODE>, although the MBean
     * unregistration succeeded. In such a case, the MBean will be actually
     * unregistered even though the <CODE>unregisterMBean</CODE> method
     * threw an exception.  Note that <CODE>RuntimeMBeanException</CODE> can
     * also be thrown by <CODE>preDeregister</CODE>, in which case the MBean
     * will remain registered.
     * @exception RuntimeOperationsException Wraps a
     * <CODE>java.lang.IllegalArgumentException</CODE>: The object
     * name in parameter is null or the MBean you are when trying to
     * unregister is the {@link javax.management.MBeanServerDelegate
     * MBeanServerDelegate} MBean.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     * @see javax.management.MBeanRegistration
     */
    public void unregisterMBean(ObjectName name)
            throws InstanceNotFoundException, MBeanRegistrationException,
                   IOException;

    /**
     * Gets the <CODE>ObjectInstance</CODE> for a given MBean
     * registered with the MBean server.
     *
     * @param name The object name of the MBean.
     *
     * @return The <CODE>ObjectInstance</CODE> associated with the MBean
     * specified by <VAR>name</VAR>.  The contained <code>ObjectName</code>
     * is <code>name</code> and the contained class name is
     * <code>{@link #getMBeanInfo getMBeanInfo(name)}.getClassName()</code>.
     *
     * @exception InstanceNotFoundException The MBean specified is not
     * registered in the MBean server.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     */
    public ObjectInstance getObjectInstance(ObjectName name)
            throws InstanceNotFoundException, IOException;

    /**
     * Gets MBeans controlled by the MBean server. This method allows
     * any of the following to be obtained: All MBeans, a set of
     * MBeans specified by pattern matching on the
     * <CODE>ObjectName</CODE> and/or a Query expression, a specific
     * MBean. When the object name is null or no domain and key
     * properties are specified, all objects are to be selected (and
     * filtered if a query is specified). It returns the set of
     * <CODE>ObjectInstance</CODE> objects (containing the
     * <CODE>ObjectName</CODE> and the Java Class name) for the
     * selected MBeans.
     *
     * @param name The object name pattern identifying the MBeans to
     * be retrieved. If null or no domain and key properties are
     * specified, all the MBeans registered will be retrieved.
     * @param query The query expression to be applied for selecting
     * MBeans. If null no query expression will be applied for
     * selecting MBeans.
     *
     * @return A set containing the <CODE>ObjectInstance</CODE>
     * objects for the selected MBeans.  If no MBean satisfies the
     * query an empty list is returned.
     *
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     */
    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query)
            throws IOException;

    /**
     * Gets the names of MBeans controlled by the MBean server. This
     * method enables any of the following to be obtained: The names
     * of all MBeans, the names of a set of MBeans specified by
     * pattern matching on the <CODE>ObjectName</CODE> and/or a Query
     * expression, a specific MBean name (equivalent to testing
     * whether an MBean is registered). When the object name is null
     * or no domain and key properties are specified, all objects are
     * selected (and filtered if a query is specified). It returns the
     * set of ObjectNames for the MBeans selected.
     *
     * @param name The object name pattern identifying the MBean names
     * to be retrieved. If null or no domain and key properties are
     * specified, the name of all registered MBeans will be retrieved.
     * @param query The query expression to be applied for selecting
     * MBeans. If null no query expression will be applied for
     * selecting MBeans.
     *
     * @return A set containing the ObjectNames for the MBeans
     * selected.  If no MBean satisfies the query, an empty list is
     * returned.
     *
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     */
    public Set<ObjectName> queryNames(ObjectName name, QueryExp query)
            throws IOException;



    /**
     * Checks whether an MBean, identified by its object name, is
     * already registered with the MBean server.
     *
     * @param name The object name of the MBean to be checked.
     *
     * @return True if the MBean is already registered in the MBean
     * server, false otherwise.
     *
     * @exception RuntimeOperationsException Wraps a
     * <CODE>java.lang.IllegalArgumentException</CODE>: The object
     * name in parameter is null.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     */
    public boolean isRegistered(ObjectName name)
            throws IOException;


    /**
     * Returns the number of MBeans registered in the MBean server.
     *
     * @return the number of MBeans registered.
     *
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     */
    public Integer getMBeanCount()
            throws IOException;

    /**
     * Gets the value of a specific attribute of a named MBean. The MBean
     * is identified by its object name.
     *
     * @param name The object name of the MBean from which the
     * attribute is to be retrieved.
     * @param attribute A String specifying the name of the attribute
     * to be retrieved.
     *
     * @return  The value of the retrieved attribute.
     *
     * @exception AttributeNotFoundException The attribute specified
     * is not accessible in the MBean.
     * @exception MBeanException Wraps an exception thrown by the
     * MBean's getter.
     * @exception InstanceNotFoundException The MBean specified is not
     * registered in the MBean server.
     * @exception ReflectionException Wraps a
     * <CODE>java.lang.Exception</CODE> thrown when trying to invoke
     * the setter.
     * @exception RuntimeOperationsException Wraps a
     * <CODE>java.lang.IllegalArgumentException</CODE>: The object
     * name in parameter is null or the attribute in parameter is
     * null.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     * @see #setAttribute
     */
    public Object getAttribute(ObjectName name, String attribute)
            throws MBeanException, AttributeNotFoundException,
                   InstanceNotFoundException, ReflectionException,
                   IOException;


    /**
     * <p>Retrieves the values of several attributes of a named MBean. The MBean
     * is identified by its object name.</p>
     *
     * <p>If one or more attributes cannot be retrieved for some reason, they
     * will be omitted from the returned {@code AttributeList}.  The caller
     * should check that the list is the same size as the {@code attributes}
     * array.  To discover what problem prevented a given attribute from being
     * retrieved, call {@link #getAttribute getAttribute} for that attribute.</p>
     *
     * <p>Here is an example of calling this method and checking that it
     * succeeded in retrieving all the requested attributes:</p>
     *
     * <pre>
     * String[] attrNames = ...;
     * AttributeList list = mbeanServerConnection.getAttributes(objectName, attrNames);
     * if (list.size() == attrNames.length)
     *     System.out.println("All attributes were retrieved successfully");
     * else {
     *     {@code List<String>} missing = new {@code ArrayList<String>}(<!--
     * -->{@link java.util.Arrays#asList Arrays.asList}(attrNames));
     *     for (Attribute a : list.asList())
     *         missing.remove(a.getName());
     *     System.out.println("Did not retrieve: " + missing);
     * }
     * </pre>
     *
     * @param name The object name of the MBean from which the
     * attributes are retrieved.
     * @param attributes A list of the attributes to be retrieved.
     *
     * @return The list of the retrieved attributes.
     *
     * @exception InstanceNotFoundException The MBean specified is not
     * registered in the MBean server.
     * @exception ReflectionException An exception occurred when
     * trying to invoke the getAttributes method of a Dynamic MBean.
     * @exception RuntimeOperationsException Wrap a
     * <CODE>java.lang.IllegalArgumentException</CODE>: The object
     * name in parameter is null or attributes in parameter is null.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     * @see #setAttributes
     */
    public AttributeList getAttributes(ObjectName name, String[] attributes)
            throws InstanceNotFoundException, ReflectionException,
                   IOException;


    /**
     * Sets the value of a specific attribute of a named MBean. The MBean
     * is identified by its object name.
     *
     * @param name The name of the MBean within which the attribute is
     * to be set.
     * @param attribute The identification of the attribute to be set
     * and the value it is to be set to.
     *
     * @exception InstanceNotFoundException The MBean specified is not
     * registered in the MBean server.
     * @exception AttributeNotFoundException The attribute specified
     * is not accessible in the MBean.
     * @exception InvalidAttributeValueException The value specified
     * for the attribute is not valid.
     * @exception MBeanException Wraps an exception thrown by the
     * MBean's setter.
     * @exception ReflectionException Wraps a
     * <CODE>java.lang.Exception</CODE> thrown when trying to invoke
     * the setter.
     * @exception RuntimeOperationsException Wraps a
     * <CODE>java.lang.IllegalArgumentException</CODE>: The object
     * name in parameter is null or the attribute in parameter is
     * null.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     * @see #getAttribute
     */
    public void setAttribute(ObjectName name, Attribute attribute)
            throws InstanceNotFoundException, AttributeNotFoundException,
                   InvalidAttributeValueException, MBeanException,
                   ReflectionException, IOException;


    /**
     * <p>Sets the values of several attributes of a named MBean. The MBean is
     * identified by its object name.</p>
     *
     * <p>If one or more attributes cannot be set for some reason, they will be
     * omitted from the returned {@code AttributeList}.  The caller should check
     * that the input {@code AttributeList} is the same size as the output one.
     * To discover what problem prevented a given attribute from being retrieved,
     * it will usually be possible to call {@link #setAttribute setAttribute}
     * for that attribute, although this is not guaranteed to work.  (For
     * example, the values of two attributes may have been rejected because
     * they were inconsistent with each other.  Setting one of them alone might
     * be allowed.)
     *
     * <p>Here is an example of calling this method and checking that it
     * succeeded in setting all the requested attributes:</p>
     *
     * <pre>
     * AttributeList inputAttrs = ...;
     * AttributeList outputAttrs = mbeanServerConnection.setAttributes(<!--
     * -->objectName, inputAttrs);
     * if (inputAttrs.size() == outputAttrs.size())
     *     System.out.println("All attributes were set successfully");
     * else {
     *     {@code List<String>} missing = new {@code ArrayList<String>}();
     *     for (Attribute a : inputAttrs.asList())
     *         missing.add(a.getName());
     *     for (Attribute a : outputAttrs.asList())
     *         missing.remove(a.getName());
     *     System.out.println("Did not set: " + missing);
     * }
     * </pre>
     *
     * @param name The object name of the MBean within which the
     * attributes are to be set.
     * @param attributes A list of attributes: The identification of
     * the attributes to be set and the values they are to be set to.
     *
     * @return The list of attributes that were set, with their new
     * values.
     *
     * @exception InstanceNotFoundException The MBean specified is not
     * registered in the MBean server.
     * @exception ReflectionException An exception occurred when
     * trying to invoke the getAttributes method of a Dynamic MBean.
     * @exception RuntimeOperationsException Wraps a
     * <CODE>java.lang.IllegalArgumentException</CODE>: The object
     * name in parameter is null or attributes in parameter is null.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     * @see #getAttributes
     */
    public AttributeList setAttributes(ObjectName name,
                                       AttributeList attributes)
        throws InstanceNotFoundException, ReflectionException, IOException;

    /**
     * <p>Invokes an operation on an MBean.</p>
     *
     * <p>Because of the need for a {@code signature} to differentiate
     * possibly-overloaded operations, it is much simpler to invoke operations
     * through an {@linkplain JMX#newMBeanProxy(MBeanServerConnection, ObjectName,
     * Class) MBean proxy} where possible.  For example, suppose you have a
     * Standard MBean interface like this:</p>
     *
     * <pre>
     * public interface FooMBean {
     *     public int countMatches(String[] patterns, boolean ignoreCase);
     * }
     * </pre>
     *
     * <p>The {@code countMatches} operation can be invoked as follows:</p>
     *
     * <pre>
     * String[] myPatterns = ...;
     * int count = (Integer) mbeanServerConnection.invoke(
     *         objectName,
     *         "countMatches",
     *         new Object[] {myPatterns, true},
     *         new String[] {String[].class.getName(), boolean.class.getName()});
     * </pre>
     *
     * <p>Alternatively, it can be invoked through a proxy as follows:</p>
     *
     * <pre>
     * String[] myPatterns = ...;
     * FooMBean fooProxy = JMX.newMBeanProxy(
     *         mbeanServerConnection, objectName, FooMBean.class);
     * int count = fooProxy.countMatches(myPatterns, true);
     * </pre>
     *
     * @param name The object name of the MBean on which the method is
     * to be invoked.
     * @param operationName The name of the operation to be invoked.
     * @param params An array containing the parameters to be set when
     * the operation is invoked
     * @param signature An array containing the signature of the
     * operation, an array of class names in the format returned by
     * {@link Class#getName()}. The class objects will be loaded using the same
     * class loader as the one used for loading the MBean on which the
     * operation was invoked.
     *
     * @return The object returned by the operation, which represents
     * the result of invoking the operation on the MBean specified.
     *
     * @exception InstanceNotFoundException The MBean specified is not
     * registered in the MBean server.
     * @exception MBeanException Wraps an exception thrown by the
     * MBean's invoked method.
     * @exception ReflectionException Wraps a
     * <CODE>java.lang.Exception</CODE> thrown while trying to invoke
     * the method.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     */
    public Object invoke(ObjectName name, String operationName,
                         Object params[], String signature[])
            throws InstanceNotFoundException, MBeanException,
                   ReflectionException, IOException;



    /**
     * Returns the default domain used for naming the MBean.
     * The default domain name is used as the domain part in the ObjectName
     * of MBeans if no domain is specified by the user.
     *
     * @return the default domain.
     *
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     */
    public String getDefaultDomain()
            throws IOException;

    /**
     * <p>Returns the list of domains in which any MBean is currently
     * registered.  A string is in the returned array if and only if
     * there is at least one MBean registered with an ObjectName whose
     * {@link ObjectName#getDomain() getDomain()} is equal to that
     * string.  The order of strings within the returned array is
     * not defined.</p>
     *
     * @return the list of domains.
     *
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     */
    public String[] getDomains()
            throws IOException;

    /**
     * <p>Adds a listener to a registered MBean.
     * Notifications emitted by the MBean will be forwarded to the listener.</p>
     *
     * @param name The name of the MBean on which the listener should
     * be added.
     * @param listener The listener object which will handle the
     * notifications emitted by the registered MBean.
     * @param filter The filter object. If filter is null, no
     * filtering will be performed before handling notifications.
     * @param handback The context to be sent to the listener when a
     * notification is emitted.
     *
     * @exception InstanceNotFoundException The MBean name provided
     * does not match any of the registered MBeans.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     * @see #removeNotificationListener(ObjectName, NotificationListener)
     * @see #removeNotificationListener(ObjectName, NotificationListener,
     * NotificationFilter, Object)
     */
    public void addNotificationListener(ObjectName name,
                                        NotificationListener listener,
                                        NotificationFilter filter,
                                        Object handback)
            throws InstanceNotFoundException, IOException;


    /**
     * <p>Adds a listener to a registered MBean.</p>
     *
     * <p>A notification emitted by an MBean will be forwarded by the
     * MBeanServer to the listener.  If the source of the notification
     * is a reference to an MBean object, the MBean server will
     * replace it by that MBean's ObjectName.  Otherwise the source is
     * unchanged.</p>
     *
     * <p>The listener object that receives notifications is the one
     * that is registered with the given name at the time this method
     * is called.  Even if it is subsequently unregistered, it will
     * continue to receive notifications.</p>
     *
     * @param name The name of the MBean on which the listener should
     * be added.
     * @param listener The object name of the listener which will
     * handle the notifications emitted by the registered MBean.
     * @param filter The filter object. If filter is null, no
     * filtering will be performed before handling notifications.
     * @param handback The context to be sent to the listener when a
     * notification is emitted.
     *
     * @exception InstanceNotFoundException The MBean name of the
     * notification listener or of the notification broadcaster does
     * not match any of the registered MBeans.
     * @exception RuntimeOperationsException Wraps an {@link
     * IllegalArgumentException}.  The MBean named by
     * <code>listener</code> exists but does not implement the {@link
     * NotificationListener} interface.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     * @see #removeNotificationListener(ObjectName, ObjectName)
     * @see #removeNotificationListener(ObjectName, ObjectName,
     * NotificationFilter, Object)
     */
    public void addNotificationListener(ObjectName name,
                                        ObjectName listener,
                                        NotificationFilter filter,
                                        Object handback)
            throws InstanceNotFoundException, IOException;


    /**
     * Removes a listener from a registered MBean.
     *
     * <P> If the listener is registered more than once, perhaps with
     * different filters or callbacks, this method will remove all
     * those registrations.
     *
     * @param name The name of the MBean on which the listener should
     * be removed.
     * @param listener The object name of the listener to be removed.
     *
     * @exception InstanceNotFoundException The MBean name provided
     * does not match any of the registered MBeans.
     * @exception ListenerNotFoundException The listener is not
     * registered in the MBean.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     * @see #addNotificationListener(ObjectName, ObjectName,
     * NotificationFilter, Object)
     */
    public void removeNotificationListener(ObjectName name,
                                           ObjectName listener)
        throws InstanceNotFoundException, ListenerNotFoundException,
               IOException;

    /**
     * <p>Removes a listener from a registered MBean.</p>
     *
     * <p>The MBean must have a listener that exactly matches the
     * given <code>listener</code>, <code>filter</code>, and
     * <code>handback</code> parameters.  If there is more than one
     * such listener, only one is removed.</p>
     *
     * <p>The <code>filter</code> and <code>handback</code> parameters
     * may be null if and only if they are null in a listener to be
     * removed.</p>
     *
     * @param name The name of the MBean on which the listener should
     * be removed.
     * @param listener The object name of the listener to be removed.
     * @param filter The filter that was specified when the listener
     * was added.
     * @param handback The handback that was specified when the
     * listener was added.
     *
     * @exception InstanceNotFoundException The MBean name provided
     * does not match any of the registered MBeans.
     * @exception ListenerNotFoundException The listener is not
     * registered in the MBean, or it is not registered with the given
     * filter and handback.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     * @see #addNotificationListener(ObjectName, ObjectName,
     * NotificationFilter, Object)
     *
     */
    public void removeNotificationListener(ObjectName name,
                                           ObjectName listener,
                                           NotificationFilter filter,
                                           Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException,
                   IOException;


    /**
     * <p>Removes a listener from a registered MBean.</p>
     *
     * <P> If the listener is registered more than once, perhaps with
     * different filters or callbacks, this method will remove all
     * those registrations.
     *
     * @param name The name of the MBean on which the listener should
     * be removed.
     * @param listener The listener to be removed.
     *
     * @exception InstanceNotFoundException The MBean name provided
     * does not match any of the registered MBeans.
     * @exception ListenerNotFoundException The listener is not
     * registered in the MBean.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     * @see #addNotificationListener(ObjectName, NotificationListener,
     * NotificationFilter, Object)
     */
    public void removeNotificationListener(ObjectName name,
                                           NotificationListener listener)
            throws InstanceNotFoundException, ListenerNotFoundException,
                   IOException;

    /**
     * <p>Removes a listener from a registered MBean.</p>
     *
     * <p>The MBean must have a listener that exactly matches the
     * given <code>listener</code>, <code>filter</code>, and
     * <code>handback</code> parameters.  If there is more than one
     * such listener, only one is removed.</p>
     *
     * <p>The <code>filter</code> and <code>handback</code> parameters
     * may be null if and only if they are null in a listener to be
     * removed.</p>
     *
     * @param name The name of the MBean on which the listener should
     * be removed.
     * @param listener The listener to be removed.
     * @param filter The filter that was specified when the listener
     * was added.
     * @param handback The handback that was specified when the
     * listener was added.
     *
     * @exception InstanceNotFoundException The MBean name provided
     * does not match any of the registered MBeans.
     * @exception ListenerNotFoundException The listener is not
     * registered in the MBean, or it is not registered with the given
     * filter and handback.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     * @see #addNotificationListener(ObjectName, NotificationListener,
     * NotificationFilter, Object)
     *
     */
    public void removeNotificationListener(ObjectName name,
                                           NotificationListener listener,
                                           NotificationFilter filter,
                                           Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException,
                   IOException;

    /**
     * This method discovers the attributes and operations that an
     * MBean exposes for management.
     *
     * @param name The name of the MBean to analyze
     *
     * @return An instance of <CODE>MBeanInfo</CODE> allowing the
     * retrieval of all attributes and operations of this MBean.
     *
     * @exception IntrospectionException An exception occurred during
     * introspection.
     * @exception InstanceNotFoundException The MBean specified was
     * not found.
     * @exception ReflectionException An exception occurred when
     * trying to invoke the getMBeanInfo of a Dynamic MBean.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     */
    public MBeanInfo getMBeanInfo(ObjectName name)
            throws InstanceNotFoundException, IntrospectionException,
                   ReflectionException, IOException;


    /**
     * <p>Returns true if the MBean specified is an instance of the
     * specified class, false otherwise.</p>
     *
     * <p>If <code>name</code> does not name an MBean, this method
     * throws {@link InstanceNotFoundException}.</p>
     *
     * <p>Otherwise, let<br>
     * X be the MBean named by <code>name</code>,<br>
     * L be the ClassLoader of X,<br>
     * N be the class name in X's {@link MBeanInfo}.</p>
     *
     * <p>If N equals <code>className</code>, the result is true.</p>
     *
     * <p>Otherwise, if L successfully loads <code>className</code>
     * and X is an instance of this class, the result is true.
     *
     * <p>Otherwise, if L successfully loads both N and
     * <code>className</code>, and the second class is assignable from
     * the first, the result is true.</p>
     *
     * <p>Otherwise, the result is false.</p>
     *
     * @param name The <CODE>ObjectName</CODE> of the MBean.
     * @param className The name of the class.
     *
     * @return true if the MBean specified is an instance of the
     * specified class according to the rules above, false otherwise.
     *
     * @exception InstanceNotFoundException The MBean specified is not
     * registered in the MBean server.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     * @see Class#isInstance
     */
    public boolean isInstanceOf(ObjectName name, String className)
            throws InstanceNotFoundException, IOException;
}
