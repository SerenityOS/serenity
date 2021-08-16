/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jmx.remote.security;

import com.sun.jmx.mbeanserver.GetPropertyAction;
import java.io.ObjectInputStream;
import java.security.AccessController;
import java.util.Set;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.loading.ClassLoaderRepository;
import javax.management.remote.MBeanServerForwarder;

/**
 * <p>An object of this class implements the MBeanServer interface
 * and, for each of its methods, calls an appropriate checking method
 * and then forwards the request to a wrapped MBeanServer object.  The
 * checking method may throw a RuntimeException if the operation is
 * not allowed; in this case the request is not forwarded to the
 * wrapped object.</p>
 *
 * <p>A typical use of this class is to insert it between a connector server
 * such as the RMI connector and the MBeanServer with which the connector
 * is associated.  Requests from the connector client can then be filtered
 * and those operations that are not allowed, or not allowed in a particular
 * context, can be rejected by throwing a <code>SecurityException</code>
 * in the corresponding <code>check*</code> method.</p>
 *
 * <p>This is an abstract class, because in its implementation none of
 * the checking methods does anything.  To be useful, it must be
 * subclassed and at least one of the checking methods overridden to
 * do some checking.  Some or all of the MBeanServer methods may also
 * be overridden, for instance if the default checking behavior is
 * inappropriate.</p>
 *
 * <p>If there is no SecurityManager, then the access controller will refuse
 * to create an MBean that is a ClassLoader, which includes MLets, or to
 * execute the method addURL on an MBean that is an MLet. This prevents
 * people from opening security holes unintentionally. Otherwise, it
 * would not be obvious that granting write access grants the ability to
 * download and execute arbitrary code in the target MBean server. Advanced
 * users who do want the ability to use MLets are presumably advanced enough
 * to handle policy files and security managers.</p>
 */
public abstract class MBeanServerAccessController
        implements MBeanServerForwarder {

    public MBeanServer getMBeanServer() {
        return mbs;
    }

    public void setMBeanServer(MBeanServer mbs) {
        if (mbs == null)
            throw new IllegalArgumentException("Null MBeanServer");
        if (this.mbs != null)
            throw new IllegalArgumentException("MBeanServer object already " +
                                               "initialized");
        this.mbs = mbs;
    }

    /**
     * Check if the caller can do read operations. This method does
     * nothing if so, otherwise throws SecurityException.
     */
    protected abstract void checkRead();

    /**
     * Check if the caller can do write operations.  This method does
     * nothing if so, otherwise throws SecurityException.
     */
    protected abstract void checkWrite();

    /**
     * Check if the caller can create the named class.  The default
     * implementation of this method calls {@link #checkWrite()}.
     */
    protected void checkCreate(String className) {
        checkWrite();
    }

    /**
     * Check if the caller can unregister the named MBean.  The default
     * implementation of this method calls {@link #checkWrite()}.
     */
    protected void checkUnregister(ObjectName name) {
        checkWrite();
    }

    //--------------------------------------------
    //--------------------------------------------
    //
    // Implementation of the MBeanServer interface
    //
    //--------------------------------------------
    //--------------------------------------------

    /**
     * Call <code>checkRead()</code>, then forward this method to the
     * wrapped object.
     */
    public void addNotificationListener(ObjectName name,
                                        NotificationListener listener,
                                        NotificationFilter filter,
                                        Object handback)
        throws InstanceNotFoundException {
        checkRead();
        getMBeanServer().addNotificationListener(name, listener,
                                                 filter, handback);
    }

    /**
     * Call <code>checkRead()</code>, then forward this method to the
     * wrapped object.
     */
    public void addNotificationListener(ObjectName name,
                                        ObjectName listener,
                                        NotificationFilter filter,
                                        Object handback)
        throws InstanceNotFoundException {
        checkRead();
        getMBeanServer().addNotificationListener(name, listener,
                                                 filter, handback);
    }

    /**
     * Call <code>checkCreate(className)</code>, then forward this method to the
     * wrapped object.
     */
    public ObjectInstance createMBean(String className, ObjectName name)
        throws
        ReflectionException,
        InstanceAlreadyExistsException,
        MBeanRegistrationException,
        MBeanException,
        NotCompliantMBeanException {
        checkCreate(className);
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            Object object = getMBeanServer().instantiate(className);
            checkClassLoader(object);
            return getMBeanServer().registerMBean(object, name);
        } else {
            return getMBeanServer().createMBean(className, name);
        }
    }

    /**
     * Call <code>checkCreate(className)</code>, then forward this method to the
     * wrapped object.
     */
    public ObjectInstance createMBean(String className, ObjectName name,
                                      Object params[], String signature[])
        throws
        ReflectionException,
        InstanceAlreadyExistsException,
        MBeanRegistrationException,
        MBeanException,
        NotCompliantMBeanException {
        checkCreate(className);
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            Object object = getMBeanServer().instantiate(className,
                                                         params,
                                                         signature);
            checkClassLoader(object);
            return getMBeanServer().registerMBean(object, name);
        } else {
            return getMBeanServer().createMBean(className, name,
                                                params, signature);
        }
    }

    /**
     * Call <code>checkCreate(className)</code>, then forward this method to the
     * wrapped object.
     */
    public ObjectInstance createMBean(String className,
                                      ObjectName name,
                                      ObjectName loaderName)
        throws
        ReflectionException,
        InstanceAlreadyExistsException,
        MBeanRegistrationException,
        MBeanException,
        NotCompliantMBeanException,
        InstanceNotFoundException {
        checkCreate(className);
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            Object object = getMBeanServer().instantiate(className,
                                                         loaderName);
            checkClassLoader(object);
            return getMBeanServer().registerMBean(object, name);
        } else {
            return getMBeanServer().createMBean(className, name, loaderName);
        }
    }

    /**
     * Call <code>checkCreate(className)</code>, then forward this method to the
     * wrapped object.
     */
    public ObjectInstance createMBean(String className,
                                      ObjectName name,
                                      ObjectName loaderName,
                                      Object params[],
                                      String signature[])
        throws
        ReflectionException,
        InstanceAlreadyExistsException,
        MBeanRegistrationException,
        MBeanException,
        NotCompliantMBeanException,
        InstanceNotFoundException {
        checkCreate(className);
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            Object object = getMBeanServer().instantiate(className,
                                                         loaderName,
                                                         params,
                                                         signature);
            checkClassLoader(object);
            return getMBeanServer().registerMBean(object, name);
        } else {
            return getMBeanServer().createMBean(className, name, loaderName,
                                                params, signature);
        }
    }

    /**
     * Call <code>checkRead()</code>, then forward this method to the
     * wrapped object.
     */
    @Deprecated
    public ObjectInputStream deserialize(ObjectName name, byte[] data)
        throws InstanceNotFoundException, OperationsException {
        checkRead();
        return getMBeanServer().deserialize(name, data);
    }

    /**
     * Call <code>checkRead()</code>, then forward this method to the
     * wrapped object.
     */
    @Deprecated
    public ObjectInputStream deserialize(String className, byte[] data)
        throws OperationsException, ReflectionException {
        checkRead();
        return getMBeanServer().deserialize(className, data);
    }

    /**
     * Call <code>checkRead()</code>, then forward this method to the
     * wrapped object.
     */
    @Deprecated
    public ObjectInputStream deserialize(String className,
                                         ObjectName loaderName,
                                         byte[] data)
        throws
        InstanceNotFoundException,
        OperationsException,
        ReflectionException {
        checkRead();
        return getMBeanServer().deserialize(className, loaderName, data);
    }

    /**
     * Call <code>checkRead()</code>, then forward this method to the
     * wrapped object.
     */
    public Object getAttribute(ObjectName name, String attribute)
        throws
        MBeanException,
        AttributeNotFoundException,
        InstanceNotFoundException,
        ReflectionException {
        checkRead();
        return getMBeanServer().getAttribute(name, attribute);
    }

    /**
     * Call <code>checkRead()</code>, then forward this method to the
     * wrapped object.
     */
    public AttributeList getAttributes(ObjectName name, String[] attributes)
        throws InstanceNotFoundException, ReflectionException {
        checkRead();
        return getMBeanServer().getAttributes(name, attributes);
    }

    /**
     * Call <code>checkRead()</code>, then forward this method to the
     * wrapped object.
     */
    public ClassLoader getClassLoader(ObjectName loaderName)
        throws InstanceNotFoundException {
        checkRead();
        return getMBeanServer().getClassLoader(loaderName);
    }

    /**
     * Call <code>checkRead()</code>, then forward this method to the
     * wrapped object.
     */
    public ClassLoader getClassLoaderFor(ObjectName mbeanName)
        throws InstanceNotFoundException {
        checkRead();
        return getMBeanServer().getClassLoaderFor(mbeanName);
    }

    /**
     * Call <code>checkRead()</code>, then forward this method to the
     * wrapped object.
     */
    public ClassLoaderRepository getClassLoaderRepository() {
        checkRead();
        return getMBeanServer().getClassLoaderRepository();
    }

    /**
     * Call <code>checkRead()</code>, then forward this method to the
     * wrapped object.
     */
    public String getDefaultDomain() {
        checkRead();
        return getMBeanServer().getDefaultDomain();
    }

    /**
     * Call <code>checkRead()</code>, then forward this method to the
     * wrapped object.
     */
    public String[] getDomains() {
        checkRead();
        return getMBeanServer().getDomains();
    }

    /**
     * Call <code>checkRead()</code>, then forward this method to the
     * wrapped object.
     */
    public Integer getMBeanCount() {
        checkRead();
        return getMBeanServer().getMBeanCount();
    }

    /**
     * Call <code>checkRead()</code>, then forward this method to the
     * wrapped object.
     */
    public MBeanInfo getMBeanInfo(ObjectName name)
        throws
        InstanceNotFoundException,
        IntrospectionException,
        ReflectionException {
        checkRead();
        return getMBeanServer().getMBeanInfo(name);
    }

    /**
     * Call <code>checkRead()</code>, then forward this method to the
     * wrapped object.
     */
    public ObjectInstance getObjectInstance(ObjectName name)
        throws InstanceNotFoundException {
        checkRead();
        return getMBeanServer().getObjectInstance(name);
    }

    /**
     * Call <code>checkCreate(className)</code>, then forward this method to the
     * wrapped object.
     */
    public Object instantiate(String className)
        throws ReflectionException, MBeanException {
        checkCreate(className);
        return getMBeanServer().instantiate(className);
    }

    /**
     * Call <code>checkCreate(className)</code>, then forward this method to the
     * wrapped object.
     */
    public Object instantiate(String className,
                              Object params[],
                              String signature[])
        throws ReflectionException, MBeanException {
        checkCreate(className);
        return getMBeanServer().instantiate(className, params, signature);
    }

    /**
     * Call <code>checkCreate(className)</code>, then forward this method to the
     * wrapped object.
     */
    public Object instantiate(String className, ObjectName loaderName)
        throws ReflectionException, MBeanException, InstanceNotFoundException {
        checkCreate(className);
        return getMBeanServer().instantiate(className, loaderName);
    }

    /**
     * Call <code>checkCreate(className)</code>, then forward this method to the
     * wrapped object.
     */
    public Object instantiate(String className, ObjectName loaderName,
                              Object params[], String signature[])
        throws ReflectionException, MBeanException, InstanceNotFoundException {
        checkCreate(className);
        return getMBeanServer().instantiate(className, loaderName,
                                            params, signature);
    }

    /**
     * Call <code>checkWrite()</code>, then forward this method to the
     * wrapped object.
     */
    public Object invoke(ObjectName name, String operationName,
                         Object params[], String signature[])
        throws
        InstanceNotFoundException,
        MBeanException,
        ReflectionException {
        checkWrite();
        checkMLetMethods(name, operationName);
        return getMBeanServer().invoke(name, operationName, params, signature);
    }

    /**
     * Call <code>checkRead()</code>, then forward this method to the
     * wrapped object.
     */
    public boolean isInstanceOf(ObjectName name, String className)
        throws InstanceNotFoundException {
        checkRead();
        return getMBeanServer().isInstanceOf(name, className);
    }

    /**
     * Call <code>checkRead()</code>, then forward this method to the
     * wrapped object.
     */
    public boolean isRegistered(ObjectName name) {
        checkRead();
        return getMBeanServer().isRegistered(name);
    }

    /**
     * Call <code>checkRead()</code>, then forward this method to the
     * wrapped object.
     */
    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
        checkRead();
        return getMBeanServer().queryMBeans(name, query);
    }

    /**
     * Call <code>checkRead()</code>, then forward this method to the
     * wrapped object.
     */
    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        checkRead();
        return getMBeanServer().queryNames(name, query);
    }

    /**
     * Call <code>checkWrite()</code>, then forward this method to the
     * wrapped object.
     */
    public ObjectInstance registerMBean(Object object, ObjectName name)
        throws
        InstanceAlreadyExistsException,
        MBeanRegistrationException,
        NotCompliantMBeanException {
        checkWrite();
        return getMBeanServer().registerMBean(object, name);
    }

    /**
     * Call <code>checkRead()</code>, then forward this method to the
     * wrapped object.
     */
    public void removeNotificationListener(ObjectName name,
                                           NotificationListener listener)
        throws InstanceNotFoundException, ListenerNotFoundException {
        checkRead();
        getMBeanServer().removeNotificationListener(name, listener);
    }

    /**
     * Call <code>checkRead()</code>, then forward this method to the
     * wrapped object.
     */
    public void removeNotificationListener(ObjectName name,
                                           NotificationListener listener,
                                           NotificationFilter filter,
                                           Object handback)
        throws InstanceNotFoundException, ListenerNotFoundException {
        checkRead();
        getMBeanServer().removeNotificationListener(name, listener,
                                                    filter, handback);
    }

    /**
     * Call <code>checkRead()</code>, then forward this method to the
     * wrapped object.
     */
    public void removeNotificationListener(ObjectName name,
                                           ObjectName listener)
        throws InstanceNotFoundException, ListenerNotFoundException {
        checkRead();
        getMBeanServer().removeNotificationListener(name, listener);
    }

    /**
     * Call <code>checkRead()</code>, then forward this method to the
     * wrapped object.
     */
    public void removeNotificationListener(ObjectName name,
                                           ObjectName listener,
                                           NotificationFilter filter,
                                           Object handback)
        throws InstanceNotFoundException, ListenerNotFoundException {
        checkRead();
        getMBeanServer().removeNotificationListener(name, listener,
                                                    filter, handback);
    }

    /**
     * Call <code>checkWrite()</code>, then forward this method to the
     * wrapped object.
     */
    public void setAttribute(ObjectName name, Attribute attribute)
        throws
        InstanceNotFoundException,
        AttributeNotFoundException,
        InvalidAttributeValueException,
        MBeanException,
        ReflectionException {
        checkWrite();
        getMBeanServer().setAttribute(name, attribute);
    }

    /**
     * Call <code>checkWrite()</code>, then forward this method to the
     * wrapped object.
     */
    public AttributeList setAttributes(ObjectName name,
                                       AttributeList attributes)
        throws InstanceNotFoundException, ReflectionException {
        checkWrite();
        return getMBeanServer().setAttributes(name, attributes);
    }

    /**
     * Call <code>checkUnregister()</code>, then forward this method to the
     * wrapped object.
     */
    public void unregisterMBean(ObjectName name)
        throws InstanceNotFoundException, MBeanRegistrationException {
        checkUnregister(name);
        getMBeanServer().unregisterMBean(name);
    }

    //----------------
    // PRIVATE METHODS
    //----------------

    private void checkClassLoader(Object object) {
        if (object instanceof ClassLoader)
            throw new SecurityException("Access denied! Creating an " +
                                        "MBean that is a ClassLoader " +
                                        "is forbidden unless a security " +
                                        "manager is installed.");
    }

    private void checkMLetMethods(ObjectName name, String operation)
    throws InstanceNotFoundException {
        // Check if security manager installed
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return;
        }
        // Check for addURL and getMBeansFromURL methods
        if (!operation.equals("addURL") &&
                !operation.equals("getMBeansFromURL")) {
            return;
        }
        // Check if MBean is instance of MLet
        if (!getMBeanServer().isInstanceOf(name,
                "javax.management.loading.MLet")) {
            return;
        }
        // Throw security exception
        if (operation.equals("addURL")) { // addURL
            throw new SecurityException("Access denied! MLet method addURL " +
                    "cannot be invoked unless a security manager is installed.");
        } else { // getMBeansFromURL
            // Whether or not calling getMBeansFromURL is allowed is controlled
            // by the value of the "jmx.remote.x.mlet.allow.getMBeansFromURL"
            // system property. If the value of this property is true, calling
            // the MLet's getMBeansFromURL method is allowed. The default value
            // for this property is false.
            final String propName = "jmx.remote.x.mlet.allow.getMBeansFromURL";
            GetPropertyAction propAction = new GetPropertyAction(propName);
            @SuppressWarnings("removal")
            String propValue = AccessController.doPrivileged(propAction);
            boolean allowGetMBeansFromURL = "true".equalsIgnoreCase(propValue);
            if (!allowGetMBeansFromURL) {
                throw new SecurityException("Access denied! MLet method " +
                        "getMBeansFromURL cannot be invoked unless a " +
                        "security manager is installed or the system property " +
                        "-Djmx.remote.x.mlet.allow.getMBeansFromURL=true " +
                        "is specified.");
            }
        }
    }

    //------------------
    // PRIVATE VARIABLES
    //------------------

    private MBeanServer mbs;
}
