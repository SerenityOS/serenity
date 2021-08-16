/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jmx.mbeanserver;

import com.sun.jmx.interceptor.DefaultMBeanServerInterceptor;
import com.sun.jmx.interceptor.MBeanServerInterceptor;
import static com.sun.jmx.defaults.JmxProperties.MBEANSERVER_LOGGER;

import java.io.ObjectInputStream;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Set;
import java.lang.System.Logger.Level;

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
import javax.management.MBeanPermission;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerPermission;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import javax.management.loading.ClassLoaderRepository;

/**
 * This is the base class for MBean manipulation on the agent side. It
 * contains the methods necessary for the creation, registration, and
 * deletion of MBeans as well as the access methods for registered MBeans.
 * This is the core component of the JMX infrastructure.
 * <P>
 * Every MBean which is added to the MBean server becomes manageable:
 * its attributes and operations become remotely accessible through
 * the connectors/adaptors connected to that MBean server.
 * A Java object cannot be registered in the MBean server unless it is a
 * JMX compliant MBean.
 * <P>
 * When an MBean is registered or unregistered in the MBean server an
 * {@link javax.management.MBeanServerNotification MBeanServerNotification}
 * Notification is emitted. To register an object as listener to
 * MBeanServerNotifications you should call the MBean server method
 * {@link #addNotificationListener addNotificationListener} with
 * the <CODE>ObjectName</CODE> of the
 * {@link javax.management.MBeanServerDelegate MBeanServerDelegate}.
 * This <CODE>ObjectName</CODE> is:
 * <BR>
 * <CODE>JMImplementation:type=MBeanServerDelegate</CODE>.
 *
 * @since 1.5
 */
public final class JmxMBeanServer
    implements SunJmxMBeanServer {

    /** Control the default locking policy of the repository.
     *  By default, we will be using a fair locking policy.
     **/
    public static final boolean DEFAULT_FAIR_LOCK_POLICY = true;

    private final MBeanInstantiator instantiator;
    private final SecureClassLoaderRepository secureClr;

    /** true if interceptors are enabled **/
    private final boolean interceptorsEnabled;

    private final MBeanServer outerShell;

    private volatile MBeanServer mbsInterceptor = null;

    /** The MBeanServerDelegate object representing the MBean Server */
    private final MBeanServerDelegate mBeanServerDelegateObject;

    /**
     * <b>Package:</b> Creates an MBeanServer with the
     * specified default domain name, outer interface, and delegate.
     * <p>The default domain name is used as the domain part in the ObjectName
     * of MBeans if no domain is specified by the user.
     * <ul><b>Note:</b>Using this constructor directly is strongly
     *     discouraged. You should use
     *     {@link javax.management.MBeanServerFactory#createMBeanServer(java.lang.String)}
     *     or
     *     {@link javax.management.MBeanServerFactory#newMBeanServer(java.lang.String)}
     *     instead.
     *     <p>
     *     By default, interceptors are disabled. Use
     *     {@link #JmxMBeanServer(java.lang.String,javax.management.MBeanServer,javax.management.MBeanServerDelegate,boolean)} to enable them.
     * </ul>
     * @param domain The default domain name used by this MBeanServer.
     * @param outer A pointer to the MBeanServer object that must be
     *        passed to the MBeans when invoking their
     *        {@link javax.management.MBeanRegistration} interface.
     * @param delegate A pointer to the MBeanServerDelegate associated
     *        with the new MBeanServer. The new MBeanServer must register
     *        this MBean in its MBean repository.
     * @exception IllegalArgumentException if the instantiator is null.
     */
    JmxMBeanServer(String domain, MBeanServer outer,
                   MBeanServerDelegate delegate) {
        this(domain,outer,delegate,null,false);
    }

    /**
     * <b>Package:</b> Creates an MBeanServer with the
     * specified default domain name, outer interface, and delegate.
     * <p>The default domain name is used as the domain part in the ObjectName
     * of MBeans if no domain is specified by the user.
     * <ul><b>Note:</b>Using this constructor directly is strongly
     *     discouraged. You should use
     *     {@link javax.management.MBeanServerFactory#createMBeanServer(java.lang.String)}
     *     or
     *     {@link javax.management.MBeanServerFactory#newMBeanServer(java.lang.String)}
     *     instead.
     * </ul>
     * @param domain The default domain name used by this MBeanServer.
     * @param outer A pointer to the MBeanServer object that must be
     *        passed to the MBeans when invoking their
     *        {@link javax.management.MBeanRegistration} interface.
     * @param delegate A pointer to the MBeanServerDelegate associated
     *        with the new MBeanServer. The new MBeanServer must register
     *        this MBean in its MBean repository.
     * @param interceptors If <code>true</code>,
     *        {@link MBeanServerInterceptor} will be enabled (default is
     *        <code>false</code>)
     *        Note: this parameter is not taken into account by this
     *        implementation - the default value <code>false</code> is
     *        always used.
     * @exception IllegalArgumentException if the instantiator is null.
     */
    JmxMBeanServer(String domain, MBeanServer outer,
                   MBeanServerDelegate delegate, boolean interceptors) {
        this(domain,outer,delegate,null,false);
    }

    /**
     * <b>Package:</b> Creates an MBeanServer.
     * @param domain The default domain name used by this MBeanServer.
     * @param outer A pointer to the MBeanServer object that must be
     *        passed to the MBeans when invoking their
     *        {@link javax.management.MBeanRegistration} interface.
     * @param delegate A pointer to the MBeanServerDelegate associated
     *        with the new MBeanServer. The new MBeanServer must register
     *        this MBean in its MBean repository.
     * @param instantiator The MBeanInstantiator that will be used to
     *        instantiate MBeans and take care of class loading issues.
     * @param metadata The MetaData object that will be used by the
     *        MBean server in order to invoke the MBean interface of
     *        the registered MBeans.
     * @param interceptors If <code>true</code>,
     *        {@link MBeanServerInterceptor} will be enabled (default is
     *        <code>false</code>).
     */
    JmxMBeanServer(String domain, MBeanServer outer,
                   MBeanServerDelegate    delegate,
                   MBeanInstantiator      instantiator,
                   boolean                interceptors)  {
                   this(domain,outer,delegate,instantiator,interceptors,true);
    }

    /**
     * <b>Package:</b> Creates an MBeanServer.
     * @param domain The default domain name used by this MBeanServer.
     * @param outer A pointer to the MBeanServer object that must be
     *        passed to the MBeans when invoking their
     *        {@link javax.management.MBeanRegistration} interface.
     * @param delegate A pointer to the MBeanServerDelegate associated
     *        with the new MBeanServer. The new MBeanServer must register
     *        this MBean in its MBean repository.
     * @param instantiator The MBeanInstantiator that will be used to
     *        instantiate MBeans and take care of class loading issues.
     * @param metadata The MetaData object that will be used by the
     *        MBean server in order to invoke the MBean interface of
     *        the registered MBeans.
     * @param interceptors If <code>true</code>,
     *        {@link MBeanServerInterceptor} will be enabled (default is
     *        <code>false</code>).
     * @param fairLock If {@code true}, the MBean repository will use a {@link
     *        java.util.concurrent.locks.ReentrantReadWriteLock#ReentrantReadWriteLock(boolean)
     *        fair locking} policy.
     */
    @SuppressWarnings("removal")
    JmxMBeanServer(String domain, MBeanServer outer,
                   MBeanServerDelegate    delegate,
                   MBeanInstantiator      instantiator,
                   boolean                interceptors,
                   boolean                fairLock)  {

        if (instantiator == null) {
            final ModifiableClassLoaderRepository
                clr = new ClassLoaderRepositorySupport();
            instantiator = new MBeanInstantiator(clr);
        }

        final MBeanInstantiator fInstantiator = instantiator;
        this.secureClr = new
            SecureClassLoaderRepository(AccessController.doPrivileged(new PrivilegedAction<ClassLoaderRepository>() {
                @Override
                public ClassLoaderRepository run() {
                    return fInstantiator.getClassLoaderRepository();
                }
            })
        );
        if (delegate == null)
            delegate = new MBeanServerDelegateImpl();
        if (outer == null)
            outer = this;

        this.instantiator = instantiator;
        this.mBeanServerDelegateObject = delegate;
        this.outerShell   = outer;

        final Repository repository = new Repository(domain);
        this.mbsInterceptor =
            new DefaultMBeanServerInterceptor(outer, delegate, instantiator,
                                              repository);
        this.interceptorsEnabled = interceptors;
        initialize();
    }

    /**
     * Tell whether {@link MBeanServerInterceptor}s are enabled on this
     * object.
     * @return <code>true</code> if {@link MBeanServerInterceptor}s are
     *         enabled.
     * @see #newMBeanServer(java.lang.String,javax.management.MBeanServer,javax.management.MBeanServerDelegate,boolean)
     **/
    public boolean interceptorsEnabled() {
        return interceptorsEnabled;
    }

    /**
     * Return the MBeanInstantiator associated to this MBeanServer.
     * @exception UnsupportedOperationException if
     *            {@link MBeanServerInterceptor}s
     *            are not enabled on this object.
     * @see #interceptorsEnabled
     **/
    public MBeanInstantiator getMBeanInstantiator() {
        if (interceptorsEnabled) return instantiator;
        else throw new UnsupportedOperationException(
                       "MBeanServerInterceptors are disabled.");
    }

    /**
     * Instantiates and registers an MBean in the MBean server.
     * The MBean server will use its
     * {@link javax.management.loading.ClassLoaderRepository Default Loader Repository}
     * to load the class of the MBean.
     * An object name is associated to the MBean.
     * If the object name given is null, the MBean can automatically
     * provide its own name by implementing the
     * {@link javax.management.MBeanRegistration MBeanRegistration} interface.
     * The call returns an <CODE>ObjectInstance</CODE> object representing
     * the newly created MBean.
     *
     * @param className The class name of the MBean to be instantiated.
     * @param name The object name of the MBean. May be null.
     *
     * @return  An <CODE>ObjectInstance</CODE>, containing the
     *     <CODE>ObjectName</CODE> and the Java class name of the newly
     *     instantiated MBean.
     *
     * @exception ReflectionException Wraps an
     *     <CODE>{@link java.lang.ClassNotFoundException}</CODE> or an
     *     <CODE>{@link java.lang.Exception}</CODE> that occurred
     *     when trying to invoke the MBean's constructor.
     * @exception InstanceAlreadyExistsException The MBean is already
     *     under the control of the MBean server.
     * @exception MBeanRegistrationException The <CODE>preRegister()</CODE>
     *     (<CODE>MBeanRegistration</CODE> interface) method of the MBean
     *     has thrown an exception. The MBean will not be registered.
     * @exception MBeanException The constructor of the MBean has thrown
     *     an exception.
     * @exception NotCompliantMBeanException This class is not a JMX
     *     compliant MBean.
     * @exception RuntimeOperationsException Wraps an
     *     <CODE>{@link java.lang.IllegalArgumentException}</CODE>:
     *     The className passed in parameter is null, the
     *     <CODE>ObjectName</CODE> passed in parameter contains a pattern
     *     or no <CODE>ObjectName</CODE> is specified for the MBean.
     *
     */
    public ObjectInstance createMBean(String className, ObjectName name)
        throws ReflectionException, InstanceAlreadyExistsException,
               MBeanRegistrationException, MBeanException,
               NotCompliantMBeanException {

        return mbsInterceptor.createMBean(className,
                                          cloneObjectName(name),
                                          (Object[]) null,
                                          (String[]) null);
    }

    /**
     * Instantiates and registers an MBean in the MBean server.
     * The class loader to be used is identified by its object  name.
     * An object name is associated to the MBean.
     * If the object name  of the loader is null, the ClassLoader that
     * loaded the MBean server will be used.
     * If the MBean's object name given is null, the MBean can
     * automatically provide its own name by implementing the
     * {@link javax.management.MBeanRegistration MBeanRegistration} interface.
     * The call returns an <CODE>ObjectInstance</CODE> object representing
     * the newly created MBean.
     *
     * @param className The class name of the MBean to be instantiated.
     * @param name The object name of the MBean. May be null.
     * @param loaderName The object name of the class loader to be used.
     *
     * @return  An <CODE>ObjectInstance</CODE>, containing the
     *     <CODE>ObjectName</CODE> and the Java class name
     *     of the newly instantiated MBean.
     *
     * @exception ReflectionException  Wraps an
     *     <CODE>{@link java.lang.ClassNotFoundException}</CODE> or an
     *     <CODE>{@link java.lang.Exception}</CODE> that occurred when trying
     *     to invoke the MBean's constructor.
     * @exception InstanceAlreadyExistsException The MBean is already
     *     under the control of the MBean server.
     * @exception MBeanRegistrationException The <CODE>preRegister()</CODE>
     *     (<CODE>MBeanRegistration</CODE>  interface) method of the MBean
     *     has thrown an exception. The MBean will not be registered.
     * @exception MBeanException The constructor of the MBean has thrown
     *     an exception
     * @exception NotCompliantMBeanException This class is not a JMX
     *     compliant MBean.
     * @exception InstanceNotFoundException The specified class loader
     *     is not registered in the MBean server.
     * @exception RuntimeOperationsException Wraps an
     *     <CODE>{@link java.lang.IllegalArgumentException}</CODE>: The
     *     className passed in parameter is null, the <CODE>ObjectName</CODE>
     *     passed in parameter contains a pattern or no
     *     <CODE>ObjectName</CODE> is specified for the MBean.
     */
    public ObjectInstance createMBean(String className, ObjectName name,
                                      ObjectName loaderName)
        throws ReflectionException, InstanceAlreadyExistsException,
               MBeanRegistrationException, MBeanException,
               NotCompliantMBeanException, InstanceNotFoundException {

        return mbsInterceptor.createMBean(className,
                                          cloneObjectName(name),
                                          loaderName,
                                          (Object[]) null,
                                          (String[]) null);
    }

    /**
     * Instantiates and registers an MBean in the MBean server.
     * The MBean server will use its
     * {@link javax.management.loading.ClassLoaderRepository Default Loader Repository}
     * to load the class of the MBean.
     * An object name is associated to the MBean.
     * If the object name given is null, the MBean can automatically
     * provide its own name by implementing the
     * {@link javax.management.MBeanRegistration MBeanRegistration} interface.
     * The call returns an <CODE>ObjectInstance</CODE> object representing
     * the newly created MBean.
     *
     * @param className The class name of the MBean to be instantiated.
     * @param name The object name of the MBean. May be null.
     * @param params An array containing the parameters of the constructor
     *     to be invoked.
     * @param signature An array containing the signature of the
     *     constructor to be invoked.
     *
     * @return  An <CODE>ObjectInstance</CODE>, containing the
     *     <CODE>ObjectName</CODE> and the Java class name
     *     of the newly instantiated MBean.
     *
     * @exception ReflectionException Wraps a
     *     <CODE>{@link java.lang.ClassNotFoundException}</CODE> or an
     *     <CODE>{@link java.lang.Exception}</CODE> that occurred
     *     when trying to invoke the MBean's constructor.
     * @exception InstanceAlreadyExistsException The MBean is already
     *     under the control of the MBean server.
     * @exception MBeanRegistrationException The <CODE>preRegister()</CODE>
     *     (<CODE>MBeanRegistration</CODE>  interface) method of the MBean
     *     has thrown an exception. The MBean will not be registered.
     * @exception MBeanException The constructor of the MBean has
     *     thrown an exception.
     * @exception RuntimeOperationsException Wraps an
     *     <CODE>{@link java.lang.IllegalArgumentException}</CODE>: The
     *     className passed in parameter is null, the <CODE>ObjectName</CODE>
     *     passed in parameter contains a pattern or no
     *     <CODE>ObjectName</CODE> is specified for the MBean.
     *
     */
    public ObjectInstance createMBean(String className, ObjectName name,
                                      Object params[], String signature[])
        throws ReflectionException, InstanceAlreadyExistsException,
               MBeanRegistrationException, MBeanException,
               NotCompliantMBeanException  {

        return mbsInterceptor.createMBean(className, cloneObjectName(name),
                                          params, signature);
    }

   /**
     * Instantiates and registers an MBean in the MBean server.
     * The class loader to be used is identified by its object name.
     * An object name is associated to the MBean. If the object name
     * of the loader is not specified, the ClassLoader that loaded the
     * MBean server will be used.
     * If  the MBean object name given is null, the MBean can automatically
     * provide its own name by implementing the
     * {@link javax.management.MBeanRegistration MBeanRegistration} interface.
     * The call returns an <CODE>ObjectInstance</CODE> object representing
     * the newly created MBean.
     *
     * @param className The class name of the MBean to be instantiated.
     * @param name The object name of the MBean. May be null.
     * @param params An array containing the parameters of the constructor
     *      to be invoked.
     * @param signature An array containing the signature of the
     *     constructor to be invoked.
     * @param loaderName The object name of the class loader to be used.
     *
     * @return  An <CODE>ObjectInstance</CODE>, containing the
     *     <CODE>ObjectName</CODE> and the Java class name of the newly
     *     instantiated MBean.
     *
     * @exception ReflectionException Wraps a
     *     <CODE>{@link java.lang.ClassNotFoundException}</CODE> or an
     *     <CODE>{@link java.lang.Exception}</CODE>
     *     that occurred when trying to invoke the MBean's constructor.
     * @exception InstanceAlreadyExistsException The MBean is already
     *     under the control of the MBean server.
     * @exception MBeanRegistrationException The <CODE>preRegister()</CODE>
     *     (<CODE>MBeanRegistration</CODE>  interface) method of the MBean
     *     has thrown an exception. The MBean will not be registered.
     * @exception MBeanException The constructor of the MBean has
     *      thrown an exception
     * @exception InstanceNotFoundException The specified class loader is
     *      not registered in the MBean server.
     * @exception RuntimeOperationsException Wraps an
     *     <CODE>{@link java.lang.IllegalArgumentException}</CODE>: The
     *     className passed in parameter is null, the <CODE>ObjectName</CODE>
     *     passed in parameter contains a pattern or no
     *     <CODE>ObjectName</CODE> is specified for the MBean.
     *
     */
    public ObjectInstance createMBean(String className, ObjectName name,
                                      ObjectName loaderName, Object params[],
                                      String signature[])
        throws ReflectionException, InstanceAlreadyExistsException,
               MBeanRegistrationException, MBeanException,
               NotCompliantMBeanException, InstanceNotFoundException {

        return mbsInterceptor.createMBean(className, cloneObjectName(name),
                                          loaderName, params, signature);
    }

    /**
     * Registers a pre-existing object as an MBean with the MBean server.
     * If the object name given is null, the MBean may automatically
     * provide its own name by implementing the
     * {@link javax.management.MBeanRegistration MBeanRegistration}  interface.
     * The call returns an <CODE>ObjectInstance</CODE> object representing
     * the registered MBean.
     *
     * @param object The  MBean to be registered as an MBean.
     * @param name The object name of the MBean. May be null.
     *
     * @return The <CODE>ObjectInstance</CODE> for the MBean that has been
     *      registered.
     *
     * @exception InstanceAlreadyExistsException The MBean is already
     *      under the control of the MBean server.
     * @exception MBeanRegistrationException The <CODE>preRegister()</CODE>
     *      (<CODE>MBeanRegistration</CODE>  interface) method of the MBean
     *      has thrown an exception. The MBean will not be registered.
     * @exception NotCompliantMBeanException This object is not a JMX
     *      compliant MBean
     * @exception RuntimeOperationsException Wraps an
     *      <CODE>{@link java.lang.IllegalArgumentException}</CODE>: The
     *      object passed in parameter is null or no object name is specified.
     *
     */
    public ObjectInstance registerMBean(Object object, ObjectName name)
        throws InstanceAlreadyExistsException, MBeanRegistrationException,
               NotCompliantMBeanException  {

        return mbsInterceptor.registerMBean(object, cloneObjectName(name));
    }

    /**
     * De-registers an MBean from the MBean server. The MBean is identified by
     * its object name. Once the method has been invoked, the MBean may
     * no longer be accessed by its object name.
     *
     * @param name The object name of the MBean to be de-registered.
     *
     * @exception InstanceNotFoundException The MBean specified is not
     *     registered in the MBean server.
     * @exception MBeanRegistrationException The <code>preDeregister()</code>
     *     (<CODE>MBeanRegistration</CODE>  interface) method of the MBean
     *     has thrown an exception.
     * @exception RuntimeOperationsException Wraps an
     *     <CODE>{@link java.lang.IllegalArgumentException}</CODE>: The
     *     object name in parameter is null or the MBean you are when
     *     trying to de-register is the
     *     {@link javax.management.MBeanServerDelegate MBeanServerDelegate}
     *     MBean.
     **/
    public void unregisterMBean(ObjectName name)
        throws InstanceNotFoundException, MBeanRegistrationException  {
        mbsInterceptor.unregisterMBean(cloneObjectName(name));
    }

    /**
     * Gets the <CODE>ObjectInstance</CODE> for a given MBean registered
     * with the MBean server.
     *
     * @param name The object name of the MBean.
     *
     * @return The <CODE>ObjectInstance</CODE> associated to the MBean
     *       specified by <VAR>name</VAR>.
     *
     * @exception InstanceNotFoundException The MBean specified is not
     *       registered in the MBean server.
     */
    public ObjectInstance getObjectInstance(ObjectName name)
        throws InstanceNotFoundException {

        return mbsInterceptor.getObjectInstance(cloneObjectName(name));
    }

    /**
     * Gets MBeans controlled by the MBean server. This method allows any
     * of the following to be obtained: All MBeans, a set of MBeans specified
     * by pattern matching on the <CODE>ObjectName</CODE> and/or a Query
     * expression, a specific MBean. When the object name is null or no
     * domain and key properties are specified, all objects are to be
     * selected (and filtered if a query is specified). It returns the
     * set of <CODE>ObjectInstance</CODE> objects (containing the
     * <CODE>ObjectName</CODE> and the Java Class name) for
     * the selected MBeans.
     *
     * @param name The object name pattern identifying the MBeans to
     *      be retrieved. If null or no domain and key properties
     *      are specified, all the MBeans registered will be retrieved.
     * @param query The query expression to be applied for selecting
     *      MBeans. If null no query expression will be applied for
     *      selecting MBeans.
     *
     * @return  A set containing the <CODE>ObjectInstance</CODE> objects
     *      for the selected MBeans.
     *      If no MBean satisfies the query an empty list is returned.
     *
     */
    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {

        return mbsInterceptor.queryMBeans(cloneObjectName(name), query);
    }

    /**
     * Gets the names of MBeans controlled by the MBean server. This method
     * enables any of the following to be obtained: The names of all MBeans,
     * the names of a set of MBeans specified by pattern matching on the
     * <CODE>ObjectName</CODE> and/or a Query expression, a specific
     * MBean name (equivalent to testing whether an MBean is registered).
     * When the object name is null or no domain and key properties are
     * specified, all objects are selected (and filtered if a query is
     * specified). It returns the set of ObjectNames for the MBeans
     * selected.
     *
     * @param name The object name pattern identifying the MBeans to be
     *     retrieved. If null or no domain and key properties are
     *     specified, all the MBeans registered will be retrieved.
     * @param query The query expression to be applied for selecting
     *     MBeans. If null no query expression will be applied for
     *     selecting MBeans.
     *
     * @return  A set containing the ObjectNames for the MBeans selected.
     *     If no MBean satisfies the query, an empty list is returned.
     *
     */
    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {

        return mbsInterceptor.queryNames(cloneObjectName(name), query);
    }

    /**
     * Checks whether an MBean, identified by its object name, is already
     * registered with the MBean server.
     *
     * @param name The object name of the MBean to be checked.
     *
     * @return  True if the MBean is already registered in the MBean server,
     *     false otherwise.
     *
     * @exception RuntimeOperationsException Wraps an
     *     <CODE>{@link java.lang.IllegalArgumentException}</CODE>: The object
     *      name in parameter is null.
     *
     */
    public boolean isRegistered(ObjectName name)  {

        return mbsInterceptor.isRegistered(name);
    }

    /**
     * Returns the number of MBeans registered in the MBean server.
     */
    public Integer getMBeanCount()  {

        return mbsInterceptor.getMBeanCount();
    }

    /**
     * Gets the value of a specific attribute of a named MBean. The MBean
     * is identified by its object name.
     *
     * @param name The object name of the MBean from which the attribute
     *     is to be retrieved.
     * @param attribute A String specifying the name of the attribute to be
     *     retrieved.
     *
     * @return  The value of the retrieved attribute.
     *
     * @exception AttributeNotFoundException The attribute specified
     *     is not accessible in the MBean.
     * @exception MBeanException  Wraps an exception thrown by the
     *     MBean's getter.
     * @exception InstanceNotFoundException The MBean specified is not
     *     registered in the MBean server.
     * @exception ReflectionException  Wraps an
     *     <CODE>{@link java.lang.Exception}</CODE> thrown when trying to
     *     invoke the setter.
     * @exception RuntimeOperationsException Wraps an
     *     <CODE>{@link java.lang.IllegalArgumentException}</CODE>:
     *     The object name in parameter is null or the attribute in
     *     parameter is null.
     */
    public Object getAttribute(ObjectName name, String attribute)
        throws MBeanException, AttributeNotFoundException,
               InstanceNotFoundException, ReflectionException {

        return mbsInterceptor.getAttribute(cloneObjectName(name), attribute);
    }


    /**
     * Enables the values of several attributes of a named MBean. The MBean
     * is identified by its object name.
     *
     * @param name The object name of the MBean from which the attributes are
     *     retrieved.
     * @param attributes A list of the attributes to be retrieved.
     *
     * @return The list of the retrieved attributes.
     *
     * @exception InstanceNotFoundException The MBean specified is not
     *     registered in the MBean server.
     * @exception ReflectionException An exception occurred when trying
     *     to invoke the getAttributes method of a Dynamic MBean.
     * @exception RuntimeOperationsException Wrap an
     *     <CODE>{@link java.lang.IllegalArgumentException}</CODE>: The
     *     object name in parameter is null or attributes in parameter
     *     is null.
     *
     */
    public AttributeList getAttributes(ObjectName name, String[] attributes)
        throws InstanceNotFoundException, ReflectionException  {

        return mbsInterceptor.getAttributes(cloneObjectName(name), attributes);

    }

    /**
     * Sets the value of a specific attribute of a named MBean. The MBean
     * is identified by its object name.
     *
     * @param name The name of the MBean within which the attribute is
     *     to be set.
     * @param attribute The identification of the attribute to be set
     *     and the value it is to be set to.
     *
     * @exception InstanceNotFoundException The MBean specified is
     *     not registered in the MBean server.
     * @exception AttributeNotFoundException The attribute specified is
     *     not accessible in the MBean.
     * @exception InvalidAttributeValueException The value specified for
     *     the attribute is not valid.
     * @exception MBeanException Wraps an exception thrown by the
     *     MBean's setter.
     * @exception ReflectionException  Wraps an
     *     <CODE>{@link java.lang.Exception}</CODE> thrown when trying
     *     to invoke the setter.
     * @exception RuntimeOperationsException Wraps an
     *     <CODE>{@link java.lang.IllegalArgumentException}</CODE>: The
     *     object name in parameter is null or the attribute in parameter
     *     is null.
     */
    public void setAttribute(ObjectName name, Attribute attribute)
        throws InstanceNotFoundException, AttributeNotFoundException,
               InvalidAttributeValueException, MBeanException,
               ReflectionException  {

        mbsInterceptor.setAttribute(cloneObjectName(name),
                                    cloneAttribute(attribute));
    }

    /**
     * Sets the values of several attributes of a named MBean. The MBean is
     * identified by its object name.
     *
     * @param name The object name of the MBean within which the
     *     attributes are to  be set.
     * @param attributes A list of attributes: The identification of the
     *     attributes to be set and  the values they are to be set to.
     *
     * @return  The list of attributes that were set, with their new values.
     *
     * @exception InstanceNotFoundException The MBean specified is not
     *      registered in the MBean server.
     * @exception ReflectionException An exception occurred when trying
     *      to invoke the getAttributes method of a Dynamic MBean.
     * @exception RuntimeOperationsException Wraps an
     *      <CODE>{@link java.lang.IllegalArgumentException}</CODE>:
     *     The object name in parameter is null or  attributes in
     *     parameter is null.
     *
     */
    public AttributeList setAttributes(ObjectName name,
                                       AttributeList attributes)
        throws InstanceNotFoundException, ReflectionException  {

        return mbsInterceptor.setAttributes(cloneObjectName(name),
                                            cloneAttributeList(attributes));
    }

    /**
     * Invokes an operation on an MBean.
     *
     * @param name The object name of the MBean on which the method is to be
     *     invoked.
     * @param operationName The name of the operation to be invoked.
     * @param params An array containing the parameters to be set when
     *     the operation is invoked
     * @param signature An array containing the signature of the operation.
     *     The class objects will be loaded using the same class loader as
     *     the one used for loading the MBean on which the operation was
     *     invoked.
     *
     * @return  The object returned by the operation, which represents the
     *      result ofinvoking the operation on the  MBean specified.
     *
     * @exception InstanceNotFoundException The MBean specified is not
     *       registered in the MBean server.
     * @exception MBeanException  Wraps an exception thrown by the MBean's
     *       invoked method.
     * @exception ReflectionException  Wraps an
     *       <CODE>{@link java.lang.Exception}</CODE> thrown while trying
     *        to invoke the method.
     *
     */
    public Object invoke(ObjectName name, String operationName,
                         Object params[], String signature[])
        throws InstanceNotFoundException, MBeanException,
               ReflectionException {
        return mbsInterceptor.invoke(cloneObjectName(name), operationName,
                                     params, signature);
    }

    /**
     * Returns the default domain used for naming the MBean.
     * The default domain name is used as the domain part in the ObjectName
     * of MBeans if no domain is specified by the user.
     */
    public String getDefaultDomain()  {
        return mbsInterceptor.getDefaultDomain();
    }

    // From MBeanServer
    public String[] getDomains() {
        return mbsInterceptor.getDomains();
    }

    /**
     * Adds a listener to a registered MBean.
     *
     * @param name The name of the MBean on which the listener should be added.
     * @param listener The listener object which will handle the
     *        notifications emitted by the registered MBean.
     * @param filter The filter object. If filter is null, no filtering
     *        will be performed before handling notifications.
     * @param handback The context to be sent to the listener when a
     *        notification is emitted.
     *
     * @exception InstanceNotFoundException The MBean name provided does
     *       not match any of the registered MBeans.
     */
    public void addNotificationListener(ObjectName name,
                                        NotificationListener listener,
                                        NotificationFilter filter,
                                        Object handback)
        throws InstanceNotFoundException {

        mbsInterceptor.addNotificationListener(cloneObjectName(name), listener,
                                               filter, handback);
    }

    /**
     * Adds a listener to a registered MBean.
     *
     * @param name The name of the MBean on which the listener should be added.
     * @param listener The object name of the listener which will handle the
     *        notifications emitted by the registered MBean.
     * @param filter The filter object. If filter is null, no filtering will
     *        be performed before handling notifications.
     * @param handback The context to be sent to the listener when a
     *        notification is emitted.
     *
     * @exception InstanceNotFoundException The MBean name of the
     *       notification listener or of the notification broadcaster
     *       does not match any of the registered MBeans.
     */
    public void addNotificationListener(ObjectName name, ObjectName listener,
                                   NotificationFilter filter, Object handback)
        throws InstanceNotFoundException {

        mbsInterceptor.addNotificationListener(cloneObjectName(name), listener,
                                               filter, handback);
    }

    public void removeNotificationListener(ObjectName name,
                                           NotificationListener listener)
            throws InstanceNotFoundException, ListenerNotFoundException {

        mbsInterceptor.removeNotificationListener(cloneObjectName(name),
                                                  listener);
    }

    public void removeNotificationListener(ObjectName name,
                                           NotificationListener listener,
                                           NotificationFilter filter,
                                           Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException {

        mbsInterceptor.removeNotificationListener(cloneObjectName(name),
                                                  listener, filter, handback);
    }

    public void removeNotificationListener(ObjectName name,
                                           ObjectName listener)
        throws InstanceNotFoundException, ListenerNotFoundException {

        mbsInterceptor.removeNotificationListener(cloneObjectName(name),
                                                  listener);
    }

    public void removeNotificationListener(ObjectName name,
                                           ObjectName listener,
                                           NotificationFilter filter,
                                           Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException {

        mbsInterceptor.removeNotificationListener(cloneObjectName(name),
                                                  listener, filter, handback);
    }

    /**
     * This method discovers the attributes and operations that an MBean exposes
     * for management.
     *
     * @param name The name of the MBean to analyze
     *
     * @return  An instance of <CODE>MBeanInfo</CODE> allowing the retrieval of
     * all attributes and operations of this MBean.
     *
     * @exception IntrospectionException An exception occurs during
     * introspection.
     * @exception InstanceNotFoundException The MBean specified is not found.
     * @exception ReflectionException An exception occurred when trying to
     * invoke the getMBeanInfo of a Dynamic MBean.
     */
    public MBeanInfo getMBeanInfo(ObjectName name) throws
    InstanceNotFoundException, IntrospectionException, ReflectionException {

        return mbsInterceptor.getMBeanInfo(cloneObjectName(name));
    }

    /**
     * Instantiates an object using the list of all class loaders registered
     * in the MBean server (using its
     * {@link javax.management.loading.ClassLoaderRepository Default Loader Repository}).
     * The object's class should have a public constructor.
     * It returns a reference to the newly created object.
     * The newly created object is not registered in the MBean server.
     *
     * @param className The class name of the object to be instantiated.
     *
     * @return The newly instantiated object.
     *
     * @exception ReflectionException Wraps the
     *     <CODE>{@link java.lang.ClassNotFoundException}</CODE> or the
     *     <CODE>{@link java.lang.Exception}</CODE> that
     *     occurred when trying to invoke the object's constructor.
     * @exception MBeanException The constructor of the object has thrown
     *     an exception.
     * @exception RuntimeOperationsException Wraps an
     *     <CODE>{@link java.lang.IllegalArgumentException}</CODE>:
     *     The className passed in parameter is null.
     *
     */
    public Object instantiate(String className)
        throws ReflectionException, MBeanException {

        /* Permission check */
        checkMBeanPermission(className, null, null, "instantiate");

        return instantiator.instantiate(className);
    }

    /**
     * Instantiates an object using the class Loader specified by its
     * <CODE>ObjectName</CODE>.
     * If the loader name is null, the ClassLoader that loaded the
     * MBean Server will be used.
     * The object's class should have a public constructor.
     * It returns a reference to the newly created object.
     * The newly created object is not registered in the MBean server.
     *
     * @param className The class name of the MBean to be instantiated.
     * @param loaderName The object name of the class loader to be used.
     *
     * @return The newly instantiated object.
     *
     * @exception ReflectionException Wraps the
     *     <CODE>{@link java.lang.ClassNotFoundException}</CODE> or the
     *     <CODE>{@link java.lang.Exception}</CODE> that
     *     occurred when trying to invoke the object's constructor.
     * @exception MBeanException The constructor of the object has thrown
     *     an exception.
     * @exception InstanceNotFoundException The specified class loader
     *     is not registered in the MBaenServer.
     * @exception RuntimeOperationsException Wraps an
     *     <CODE>{@link java.lang.IllegalArgumentException}</CODE>: The
     *     className passed in parameter is null.
     *
     */
    public Object instantiate(String className, ObjectName loaderName)
        throws ReflectionException, MBeanException,
               InstanceNotFoundException {

        /* Permission check */
        checkMBeanPermission(className, null, null, "instantiate");

        ClassLoader myLoader = outerShell.getClass().getClassLoader();
        return instantiator.instantiate(className, loaderName, myLoader);
    }

    /**
     * Instantiates an object using the list of all class loaders registered
     * in the MBean server (using its
     * {@link javax.management.loading.ClassLoaderRepository Default Loader Repository}).
     * The object's class should have a public constructor.
     * The call returns a reference to the newly created object.
     * The newly created object is not registered in the MBean server.
     *
     * @param className The class name of the object to be instantiated.
     * @param params An array containing the parameters of the constructor
     *     to be invoked.
     * @param signature An array containing the signature of the
     *     constructor to be invoked.
     *
     * @return The newly instantiated object.
     *
     * @exception ReflectionException Wraps the
     *     <CODE>{@link java.lang.ClassNotFoundException}</CODE> or the
     *     <CODE>{@link java.lang.Exception}</CODE> that
     *     occurred when trying to invoke the object's constructor.
     * @exception MBeanException The constructor of the object has thrown
     *     an exception.
     * @exception RuntimeOperationsException Wraps an
     *     <CODE>{@link java.lang.IllegalArgumentException}</CODE>:
     *     The className passed in parameter is null.
     *
     */
    public Object instantiate(String className, Object params[],
                              String signature[])
        throws ReflectionException, MBeanException {

        /* Permission check */
        checkMBeanPermission(className, null, null, "instantiate");

        ClassLoader myLoader = outerShell.getClass().getClassLoader();
        return instantiator.instantiate(className, params, signature,
                                        myLoader);
    }

    /**
     * Instantiates an object. The class loader to be used is identified
     * by its object name. If the object name of the loader is null,
     * the ClassLoader that loaded the MBean server will be used.
     * The object's class should have a public constructor.
     * The call returns a reference to the newly created object.
     * The newly created object is not registered in the MBean server.
     *
     * @param className The class name of the object to be instantiated.
     * @param params An array containing the parameters of the constructor
     *     to be invoked.
     * @param signature An array containing the signature of the constructor
     *     to be invoked.
     * @param loaderName The object name of the class loader to be used.
     *
     * @return The newly instantiated object.
     *
     * @exception ReflectionException Wraps the
     *    <CODE>{@link java.lang.ClassNotFoundException}</CODE> or the
     *    <CODE>{@link java.lang.Exception}</CODE> that
     *    occurred when trying to invoke the object's constructor.
     * @exception MBeanException The constructor of the object has thrown
     *    an exception.
     * @exception InstanceNotFoundException The specified class loader
     *    is not registered in the MBean server.
     * @exception RuntimeOperationsException Wraps an
     *    <CODE>{@link java.lang.IllegalArgumentException}</CODE>:
     *    The className passed in parameter is null.
     *
     */
    public Object instantiate(String className, ObjectName loaderName,
                              Object params[], String signature[])
        throws ReflectionException, MBeanException,
               InstanceNotFoundException {

        /* Permission check */
        checkMBeanPermission(className, null, null, "instantiate");

        ClassLoader myLoader = outerShell.getClass().getClassLoader();
        return instantiator.instantiate(className,loaderName,params,signature,
                                        myLoader);
    }

    /**
     * Returns true if the MBean specified is an instance of the specified
     * class, false otherwise.
     *
     * @param name The <CODE>ObjectName</CODE> of the MBean.
     * @param className The name of the class.
     *
     * @return true if the MBean specified is an instance of the specified
     *     class, false otherwise.
     *
     * @exception InstanceNotFoundException The MBean specified is not
     *     registered in the MBean server.
     */
    public boolean isInstanceOf(ObjectName name, String className)
        throws InstanceNotFoundException {

        return mbsInterceptor.isInstanceOf(cloneObjectName(name), className);
    }

    /**
     * De-serializes a byte array in the context of the class loader
     * of an MBean.
     *
     * @param name The name of the MBean whose class loader should
     *     be used for the de-serialization.
     * @param data The byte array to be de-sererialized.
     *
     * @return  The de-serialized object stream.
     *
     * @exception InstanceNotFoundException The MBean specified is not
     *     found.
     * @exception OperationsException Any of the usual Input/Output
     *     related exceptions.
     *
     */
    @Deprecated
    public ObjectInputStream deserialize(ObjectName name, byte[] data)
        throws InstanceNotFoundException, OperationsException {

        /* Permission check */
        // This call requires MBeanPermission 'getClassLoaderFor'
        final ClassLoader loader = getClassLoaderFor(name);

        return instantiator.deserialize(loader, data);
    }

    /**
     * De-serializes a byte array in the context of a given MBean class loader.
     * The class loader is the one that loaded the class with name "className".
     *
     * @param className The name of the class whose class loader should be
     *      used for the de-serialization.
     * @param data The byte array to be de-sererialized.
     *
     * @return  The de-serialized object stream.
     *
     * @exception OperationsException Any of the usual Input/Output
     *      related exceptions.
     * @exception ReflectionException The specified class could not be
     *      loaded by the default loader repository
     *
     */
    @Deprecated
    public ObjectInputStream deserialize(String className, byte[] data)
        throws OperationsException, ReflectionException {

        if (className == null) {
            throw new  RuntimeOperationsException(
                                        new IllegalArgumentException(),
                                        "Null className passed in parameter");
        }

        /* Permission check */
        // This call requires MBeanPermission 'getClassLoaderRepository'
        final ClassLoaderRepository clr = getClassLoaderRepository();

        Class<?> theClass;
        try {
            if (clr == null) throw new ClassNotFoundException(className);
            theClass = clr.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new ReflectionException(e,
                                          "The given class could not be " +
                                          "loaded by the default loader " +
                                          "repository");
        }

        return instantiator.deserialize(theClass.getClassLoader(), data);
    }

    /**
     * De-serializes a byte array in the context of a given MBean class loader.
     * The class loader is the one that loaded the class with name "className".
     * The name of the class loader to be used for loading the specified
     * class is specified.
     * If null, the MBean Server's class loader will be used.
     *
     * @param className The name of the class whose class loader should be
     *     used for the de-serialization.
     * @param data The byte array to be de-sererialized.
     * @param loaderName The name of the class loader to be used for
     *     loading the specified class.
     *     If null, the MBean Server's class loader will be used.
     *
     * @return  The de-serialized object stream.
     *
     * @exception InstanceNotFoundException The specified class loader
     *     MBean is not found.
     * @exception OperationsException Any of the usual Input/Output
     *     related exceptions.
     * @exception ReflectionException The specified class could not
     *     be loaded by the specified class loader.
     *
     */
    @Deprecated
    public ObjectInputStream deserialize(String className,
                                         ObjectName loaderName,
                                         byte[] data) throws
        InstanceNotFoundException, OperationsException, ReflectionException {

        // Clone ObjectName
        //
        loaderName = cloneObjectName(loaderName);

        /* Permission check */
        // Make this call just to force the 'getClassLoader'
        // permission check
        try {
            getClassLoader(loaderName);
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
        }

        ClassLoader myLoader = outerShell.getClass().getClassLoader();
        return instantiator.deserialize(className, loaderName, data, myLoader);
    }

    /**
     * Initializes this MBeanServer, registering the MBeanServerDelegate.
     * <p>This method must be called once, before using the MBeanServer.
     **/
    @SuppressWarnings("removal")
    private void initialize() {
        if (instantiator == null) throw new
            IllegalStateException("instantiator must not be null.");

        // Registers the MBeanServer identification MBean
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws Exception {
                    mbsInterceptor.registerMBean(
                            mBeanServerDelegateObject,
                            MBeanServerDelegate.DELEGATE_NAME);
                    return null;
                }
            });
        } catch (SecurityException e) {
            if (MBEANSERVER_LOGGER.isLoggable(Level.DEBUG)) {
                MBEANSERVER_LOGGER.log(Level.DEBUG,
                        "Unexpected security exception occurred", e);
            }
            throw e;
        } catch (Exception e) {
            if (MBEANSERVER_LOGGER.isLoggable(Level.DEBUG)) {
                MBEANSERVER_LOGGER.log(Level.DEBUG,
                        "Unexpected exception occurred", e);
            }
            throw new
                IllegalStateException("Can't register delegate.",e);
        }


        /* Add my class loader to the repository
           This can be null if my class loader is the bootstrap
           class loader.  The ClassLoaderRepository knows how
           to handle that case.  */
        ClassLoader myLoader = outerShell.getClass().getClassLoader();
        final ModifiableClassLoaderRepository loaders = AccessController.doPrivileged(new PrivilegedAction<ModifiableClassLoaderRepository>() {

            @Override
            public ModifiableClassLoaderRepository run() {
                return instantiator.getClassLoaderRepository();
            }
        });

        if (loaders != null) {
            loaders.addClassLoader(myLoader);

            /* Add the system class loader, so that if the MBean server is
               loaded by the bootstrap class loader we can still load
               MBeans from the classpath using
               createMBean(className, objectName).

               If this class (JmxMBeanServer) was not loaded by the
               system class loader or a parent of it, then the caller
               must have RuntimePermission("getClassLoader") for the
               getSystemClassLoader() call to succeed.  If the caller
               does not have that permission, any call to
               Class.getClassLoader() will fail.  Since there are lots
               of those in JMX, we better throw the exception now.

               This permission question is irrelevant when JMX is part
               of J2SE (as of 1.5). */
            ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
            if (systemLoader != myLoader)
                loaders.addClassLoader(systemLoader);
        }
    }

    /**
     * Return the MBeanServerInterceptor.
     * @exception UnsupportedOperationException if
     *            {@link MBeanServerInterceptor}s
     *            are not enabled on this object.
     * @see #interceptorsEnabled
     **/
    public synchronized MBeanServer getMBeanServerInterceptor() {
        if (interceptorsEnabled) return mbsInterceptor;
        else throw new UnsupportedOperationException(
                       "MBeanServerInterceptors are disabled.");
    }

    /**
     * Set the MBeanServerInterceptor.
     * @exception UnsupportedOperationException if
     *            {@link MBeanServerInterceptor}s
     *            are not enabled on this object.
     * @see #interceptorsEnabled
     **/
    public synchronized void
        setMBeanServerInterceptor(MBeanServer interceptor) {
        if (!interceptorsEnabled) throw new UnsupportedOperationException(
                       "MBeanServerInterceptors are disabled.");
        if (interceptor == null) throw new
            IllegalArgumentException("MBeanServerInterceptor is null");
        mbsInterceptor = interceptor;
    }

    /**
     * <p>Return the {@link java.lang.ClassLoader} that was used for
     * loading the class of the named MBean.
     * @param mbeanName The ObjectName of the MBean.
     * @return The ClassLoader used for that MBean.
     * @exception InstanceNotFoundException if the named MBean is not found.
     */
    public ClassLoader getClassLoaderFor(ObjectName mbeanName)
        throws InstanceNotFoundException {
        return mbsInterceptor.getClassLoaderFor(cloneObjectName(mbeanName));
    }

    /**
     * <p>Return the named {@link java.lang.ClassLoader}.
     * @param loaderName The ObjectName of the ClassLoader.
     * @return The named ClassLoader.
     * @exception InstanceNotFoundException if the named ClassLoader
     * is not found.
     */
    public ClassLoader getClassLoader(ObjectName loaderName)
        throws InstanceNotFoundException {
        return mbsInterceptor.getClassLoader(cloneObjectName(loaderName));
    }

    /**
     * <p>Return the ClassLoaderRepository for that MBeanServer.
     * @return The ClassLoaderRepository for that MBeanServer.
     **/
    public ClassLoaderRepository getClassLoaderRepository() {
        /* Permission check */
        checkMBeanPermission(null, null, null, "getClassLoaderRepository");
        return secureClr;
    }

    public MBeanServerDelegate getMBeanServerDelegate() {
        if (!interceptorsEnabled) throw new UnsupportedOperationException(
                       "MBeanServerInterceptors are disabled.");
        return mBeanServerDelegateObject;
    }

    // These methods are called by the JMX MBeanServerBuilder.

    /**
     * This method creates a new MBeanServerDelegate for a new MBeanServer.
     * When creating a new MBeanServer the
     * {@link javax.management.MBeanServerBuilder} first calls this method
     * in order to create a new MBeanServerDelegate.
     * <br>Then it calls
     * <code>newMBeanServer(defaultDomain,outer,delegate,interceptors)</code>
     * passing the <var>delegate</var> that should be used by the MBeanServer
     * implementation.
     * <p>Note that the passed <var>delegate</var> might not be directly the
     * MBeanServerDelegate that was returned by this method. It could
     * be, for instance, a new object wrapping the previously
     * returned object.
     *
     * @return A new {@link javax.management.MBeanServerDelegate}.
     **/
    public static MBeanServerDelegate newMBeanServerDelegate() {
        return new MBeanServerDelegateImpl();
    }

    /**
     * This method creates a new MBeanServer implementation object.
     * When creating a new MBeanServer the
     * {@link javax.management.MBeanServerBuilder} first calls
     * <code>newMBeanServerDelegate()</code> in order to obtain a new
     * {@link javax.management.MBeanServerDelegate} for the new
     * MBeanServer. Then it calls
     * <code>newMBeanServer(defaultDomain,outer,delegate)</code>
     * passing the <var>delegate</var> that should be used by the
     * MBeanServer  implementation.
     * <p>Note that the passed <var>delegate</var> might not be directly the
     * MBeanServerDelegate that was returned by this implementation. It could
     * be, for instance, a new object wrapping the previously
     * returned delegate.
     * <p>The <var>outer</var> parameter is a pointer to the MBeanServer that
     * should be passed to the {@link javax.management.MBeanRegistration}
     * interface when registering MBeans inside the MBeanServer.
     * If <var>outer</var> is <code>null</code>, then the MBeanServer
     * implementation is free to use its own <code>this</code> pointer when
     * invoking the {@link javax.management.MBeanRegistration} interface.
     * <p>This makes it possible for a MBeanServer implementation to wrap
     * another MBeanServer implementation, in order to implement, e.g,
     * security checks, or to prevent access to the actual MBeanServer
     * implementation by returning a pointer to a wrapping object.
     *
     * @param defaultDomain Default domain of the new MBeanServer.
     * @param outer A pointer to the MBeanServer object that must be
     *        passed to the MBeans when invoking their
     *        {@link javax.management.MBeanRegistration} interface.
     * @param delegate A pointer to the MBeanServerDelegate associated
     *        with the new MBeanServer. The new MBeanServer must register
     *        this MBean in its MBean repository.
     * @param interceptors If <code>true</code>,
     *        {@link MBeanServerInterceptor}s will be enabled (default is
     *        <code>false</code>).
     *        Note: this parameter is not taken into account by this
     *        implementation - the default value <code>false</code> is
     *        always used.
     * @return A new private implementation of an MBeanServer.
     * @see #interceptorsEnabled
     * @see javax.management.MBeanServerBuilder
     * @see com.sun.jmx.mbeanserver.JmxMBeanServerBuilder
     **/
    public static MBeanServer newMBeanServer(String defaultDomain,
                                             MBeanServer outer,
                                             MBeanServerDelegate delegate,
                                             boolean interceptors) {
        // Determine whether to use fair locking for the repository.
        // Default is true.
        final boolean fairLock = DEFAULT_FAIR_LOCK_POLICY;

        checkNewMBeanServerPermission();

        // This constructor happens to disregard the value of the interceptors
        // flag - that is, it always uses the default value - false.
        // This is admitedly a bug, but we chose not to fix it for now
        // since we would rather not have anybody depending on the Sun private
        // interceptor APIs - which is most probably going to be removed and
        // replaced by a public (javax) feature in the future.
        //
        return new JmxMBeanServer(defaultDomain,outer,delegate,null,
                                  interceptors,fairLock);
    }

    // JMX OBJECT CLONING
    //-------------------

    /**
     * Clone object name.
     */
    private ObjectName cloneObjectName(ObjectName name) {
        if (name != null) {
            return ObjectName.getInstance(name);
        }
        return name;
    }

    /**
     * Clone attribute.
     */
    private Attribute cloneAttribute(Attribute attribute) {
        if (attribute != null) {
            if (!attribute.getClass().equals(Attribute.class)) {
                return new Attribute(attribute.getName(), attribute.getValue());
            }
        }
        return attribute;
    }

    /**
     * Clone attribute list.
     */
    private AttributeList cloneAttributeList(AttributeList list) {
        if (list != null) {
            List<Attribute> alist = list.asList();
            if (!list.getClass().equals(AttributeList.class)) {
                // Create new attribute list
                //
                AttributeList newList = new AttributeList(alist.size());

                // Iterate through list and replace non JMX attributes
                //
                for (Attribute attribute : alist)
                    newList.add(cloneAttribute(attribute));
                return newList;
            } else {
                // Iterate through list and replace non JMX attributes
                //
                for (int i = 0; i < alist.size(); i++) {
                    Attribute attribute = alist.get(i);
                    if (!attribute.getClass().equals(Attribute.class)) {
                        list.set(i, cloneAttribute(attribute));
                    }
                }
                return list;
            }
        }
        return list;
    }

    // SECURITY CHECKS
    //----------------

    private static void checkMBeanPermission(String classname,
                                             String member,
                                             ObjectName objectName,
                                             String actions)
        throws SecurityException {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            Permission perm = new MBeanPermission(classname,
                                                  member,
                                                  objectName,
                                                  actions);
            sm.checkPermission(perm);
        }
    }

    private static void checkNewMBeanServerPermission() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            Permission perm = new MBeanServerPermission("newMBeanServer");
            sm.checkPermission(perm);
        }
    }
}
