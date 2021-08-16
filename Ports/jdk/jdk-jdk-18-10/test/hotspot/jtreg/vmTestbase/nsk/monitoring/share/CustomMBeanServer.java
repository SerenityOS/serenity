/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package nsk.monitoring.share;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.lang.management.*;
import javax.management.*;
import javax.management.loading.*;
import nsk.share.*;

/**
 * The <code>CustomMBeanServer</code> implemenets the
 * {@link javax.management.MBeanServer MBeanServer} interface to provide
 * minimal funcionality for JSR-174 testing.
 * <p>Insignificant methods that are not used just throw {@link TestBug TestBug}
 * with "not implemented" message. If this exception is caught during test
 * execution, the corresponding method must be implemented.
 */

public class CustomMBeanServer implements MBeanServer {

        // Name of the system property, which specifies class name of MBean server
        final static String SERVER_BUILDER_PROPERTY
                = "javax.management.builder.initial";

        // Class name of MBeanServer builder that creates CustomMBeanServer
        final static String CUSTOM_SERVER_BUILDER
                = CustomMBeanServerBuilder.class.getCanonicalName();

        // Default MBeanServer builder
        final static String DEFAULT_SERVER_BUILDER = "";

        // Internal trace level
        private final static int TRACE_ALL = 10;

        // Prefix to print while logging
        private final static String LOG_PREFIX = "CustomMBeanServer> ";

        private final static String BROADCASTER_ITNF_NAME =
                "javax.management.NotificationBroadcaster";

        private final static String DYNAMICMBEAN_ITNF_NAME =
                "javax.management.DynamicMBean";

        // Private variables
        private String defaultDomain;
        private CustomMBeanRegistration register = new CustomMBeanRegistration();
        private Log.Logger log;
        private Hashtable<ObjectName, ObjectKeeper> registeredMBeans = new Hashtable<ObjectName, ObjectKeeper>();
        // StandardMBean view of registered MBeans
        private Map<ObjectName, DynamicMBean> registeredMBeansStd = new HashMap<ObjectName, DynamicMBean>();

        // Inner class to connect ObjectInstance and Object
        class ObjectKeeper {
                ObjectInstance instance;
                Object object;

                ObjectKeeper(ObjectInstance instance, Object object) {
                        this.instance = instance;
                        this.object = object;
                }
        }

        /**
         * Creates a new <code>CustomMBeanServer</code> object.
         *
         * @param defaultDomain default domain of the new MBeanServer
         */
        public CustomMBeanServer(String defaultDomain) {
                this.defaultDomain = defaultDomain;
        }

        /**
         * Instantiates and registers an MBean in the MBean server.
         *
         * @see javax.management.MBeanServer#createMBean(String, ObjectName)
         */
        public ObjectInstance createMBean(String className, ObjectName name)
                throws ReflectionException,
                       InstanceAlreadyExistsException,
                       MBeanRegistrationException,
                       MBeanException,
                       NotCompliantMBeanException {
                               throw new TestBug("not implemented");
                       }

        /**
         * Instantiates and registers an MBean in the MBean server.
         *
         * @see javax.management.MBeanServer#createMBean(String, ObjectName,
         *      Object[], String[])
         */
        public ObjectInstance createMBean(String className, ObjectName name,
                        Object[] params, String[] signature)
                throws ReflectionException,
                       InstanceAlreadyExistsException,
                       MBeanRegistrationException,
                       MBeanException,
                       NotCompliantMBeanException {
                               throw new TestBug("not implemented");
                       }

        /**
         * Instantiates and registers an MBean in the MBean server.
         *
         * @see javax.management.MBeanServer#createMBean(String, ObjectName,
         *      ObjectName)
         */
        public ObjectInstance createMBean(String className, ObjectName name,
                        ObjectName loaderName)
                throws ReflectionException,
                       InstanceAlreadyExistsException,
                       MBeanRegistrationException,
                       MBeanException,
                       NotCompliantMBeanException,
                       InstanceNotFoundException {
                               throw new TestBug("not implemented");
                       }

        /**
         * Instantiates and registers an MBean in the MBean server.
         *
         * @see javax.management.MBeanServer#createMBean(String, ObjectName,
         *      ObjectName, Object[], String[])
         */
        public ObjectInstance createMBean(String className, ObjectName name,
                        ObjectName loaderName, Object[] params,
                        String[] signature)
                throws ReflectionException,
                       InstanceAlreadyExistsException,
                       MBeanRegistrationException,
                       MBeanException,
                       NotCompliantMBeanException,
                       InstanceNotFoundException {
                               throw new TestBug("not implemented");
                       }

        /**
         * Registers a pre-existing object as an MBean with the MBean server
         *
         * @see javax.management.MBeanServer#registerMBean(Object, ObjectName)
         */
        public ObjectInstance registerMBean(Object object, ObjectName name)
                throws InstanceAlreadyExistsException,
                       MBeanRegistrationException,
                       NotCompliantMBeanException {
                               ObjectName newName = null;

                               try {
                                       newName = register.preRegister(this, name);
                               } catch (Exception e) {
                                       register.postRegister(Boolean.valueOf(false));
                                       throw new MBeanRegistrationException(e);
                               }

                               // The log object may not be initialized by that time, so try
                               // to check it
                               if (log != null)
                                       log.trace(TRACE_ALL, "[registerMBean] " + newName);

                               if  (isRegistered(newName)) {
                                       register.postRegister(Boolean.valueOf(false));
                                       throw new InstanceAlreadyExistsException("already registered");
                               }

                               ObjectInstance instance = null;
                               try {
                                       instance = new ObjectInstance(newName, object.getClass().getName());
                               } catch (IllegalArgumentException e) {
                                       throw new RuntimeOperationsException(e);
                               }
                               registeredMBeans.put(newName, new ObjectKeeper(instance, object));
                               register.postRegister(Boolean.valueOf(true));
                               return instance;
                       }

        /**
         * Unregisters an MBean from the MBean server.
         *
         * @see javax.management.MBeanServer#unregisterMBean(ObjectName)
         */
        public void unregisterMBean(ObjectName name)
                throws InstanceNotFoundException,
                       MBeanRegistrationException {
                               throw new TestBug("not implemented");
                       }

        /**
         * Gets the <code>ObjectInstance</code> for a given MBean registered with
         * the MBean server.
         *
         * @see javax.management.MBeanServer#getObjectInstance(ObjectName)
         */
        public ObjectInstance getObjectInstance(ObjectName name)
                throws InstanceNotFoundException {
                        throw new TestBug("not implemented");
                }

        /**
         * Gets MBeans controlled by the MBean server.
         *
         * @see javax.management.MBeanServer#queryMBeans(ObjectName, QueryExp)
         */
        public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
                if ( (name != null) || (query != null) )
                        throw new TestBug("not implemented");

                HashSet<ObjectInstance> result = new HashSet<ObjectInstance>();
                Enumeration enumeration = registeredMBeans.elements();
                while (enumeration.hasMoreElements()) {
                        ObjectKeeper keeper = (ObjectKeeper) enumeration.nextElement();
                        result.add(keeper.instance);
                }
                return result;
        }

        /**
         * Gets the names of MBeans controlled by the MBean server.
         *
         * @see javax.management.MBeanServer#queryNames(ObjectName, QueryExp)
         */
        public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
                if (query != null)
                        throw new TestBug("not implemented");

                HashSet<ObjectName> result = new HashSet<ObjectName>();
                Enumeration enumeration = registeredMBeans.elements();
                while (enumeration.hasMoreElements()) {
                        ObjectKeeper keeper = (ObjectKeeper) enumeration.nextElement();
                        ObjectName obj = keeper.instance.getObjectName();
                        if ((name == null) || (name.apply(obj))) {
                            result.add(obj);
                        }
                }
                return result;
        }

        /**
         * Checks whether an MBean, identified by its object name, is
         * already registered with the MBean server.
         *
         * @see javax.management.MBeanServer#isRegistered(ObjectName)
         */
        public boolean isRegistered(ObjectName name) {
                return registeredMBeans.containsKey(name);
        }

        /**
         * Returns the number of MBeans registered in the MBean server.
         *
         * @see javax.management.MBeanServer#getMBeanCount()
         */
        public Integer getMBeanCount() {
                throw new TestBug("not implemented");
        }

        /**
         * Gets the value of a specific attribute of a named MBean.
         *
         * @see javax.management.MBeanServer#getAttribute(ObjectName, String)
         */
        public Object getAttribute(ObjectName name, String attribute)
                throws MBeanException,
                       AttributeNotFoundException,
                       InstanceNotFoundException,
                       ReflectionException {

                               if (log != null)
                                       log.trace(TRACE_ALL, "[getAttribute] " + name + "> " + attribute);

                               DynamicMBean mbean = getObject(name);
                               Object result = mbean.getAttribute(attribute);
                               if (result instanceof List) {
                                       List list = (List) result;
                                       Object[] arr = new Object[list.size()];
                                       int i = 0;
                                       for (Object o : list)
                                               arr[i++] = o;
                                       return arr;
                               }
                               return result;
                       }

        /**
         * Gets the values of several attributes of a named MBean.
         *
         * @see javax.management.MBeanServer#getAttributes(ObjectName, String[])
         */
        public AttributeList getAttributes(ObjectName name, String[] attributes)
                throws InstanceNotFoundException,
                       ReflectionException {
                               throw new TestBug("not implemented");
                       }

        /**
         * Sets the value of a specific attribute of a named MBean.
         *
         * @see javax.management.MBeanServer#setAttribute(ObjectName, Attribute)
         */
        public void setAttribute(ObjectName name, Attribute attribute)
                throws InstanceNotFoundException,
                       AttributeNotFoundException,
                       InvalidAttributeValueException,
                       MBeanException,
                       ReflectionException {

                               if (log != null)
                                       log.trace(TRACE_ALL, "[setAttribute] " + name + "> " + attribute);

                               DynamicMBean mbean = getObject(name);
                               mbean.setAttribute(attribute);
                       }

        /**
         * Sets the values of several attributes of a named MBean.
         *
         * @see javax.management.MBeanServer#setAttributes(ObjectName,
         *      AttributeList)
         */
        public AttributeList setAttributes(ObjectName name,
                        AttributeList attributes)
                throws InstanceNotFoundException,
                       ReflectionException {
                               throw new TestBug("not implemented");
                       }

        /**
         * Invokes an operation on an MBean.
         *
         * @see javax.management.MBeanServer#invoke(ObjectName, String,
         *      Object[], String[])
         */
        public Object invoke(ObjectName name, String operationName,
                        Object[] params, String[] signature)
                throws InstanceNotFoundException,
                       MBeanException,
                       ReflectionException {

                               if (log != null)
                                       log.trace(TRACE_ALL, "[invoke] " + name + "> "
                                                       + operationName);
                               return invokeObjectMethod(name, operationName, params, signature);
                       }

        /**
         * Returns the default domain used for naming the MBean.
         *
         * @see javax.management.MBeanServer#getDefaultDomain()
         */
        public String getDefaultDomain() {
                throw new TestBug("not implemented");
        }

        /**
         * Returns the list of domains in which any MBean is currently
         * registered.
         *
         * @see javax.management.MBeanServer#getDomains()
         */
        public String[] getDomains() {
                throw new TestBug("not implemented");
        }

        /**
         * Adds a listener to a registered MBean.
         *
         * @see javax.management.MBeanServer#addNotificationListener(ObjectName,
         *      NotificationListener, NotificationFilter, Object)
         */
        public void addNotificationListener(ObjectName name,
                        NotificationListener listener,
                        NotificationFilter filter,
                        Object handback) throws InstanceNotFoundException {
                getNotificationBroadcaster(name).addNotificationListener(listener, filter, handback);
        }

        /**
         * Adds a listener to a registered MBean.
         *
         * @see javax.management.MBeanServer#addNotificationListener(ObjectName,
         *      ObjectName, NotificationFilter, Object)
         */
        public void addNotificationListener(ObjectName name, ObjectName listener,
                        NotificationFilter filter,
                        Object handback)
                throws InstanceNotFoundException {
                        throw new TestBug("not implemented");
                }

        /**
         * Removes a listener from a registered MBean.
         *
         * @see javax.management.MBeanServer#removeNotificationListener(ObjectName,
         *      ObjectName)
         */
        public void removeNotificationListener(ObjectName name, ObjectName listener)
                throws InstanceNotFoundException, ListenerNotFoundException {
                        throw new TestBug("not implemented");
                }

        /**
         * Removes a listener from a registered MBean.
         *
         * @see javax.management.MBeanServer#removeNotificationListener(ObjectName,
         *      ObjectName, NotificationFilter, Object)
         */
        public void removeNotificationListener(ObjectName name,
                        ObjectName listener,
                        NotificationFilter filter,
                        Object handback)
                throws InstanceNotFoundException,
                       ListenerNotFoundException {
                               throw new TestBug("not implemented");
                       }

        /**
         * Removes a listener from a registered MBean.
         *
         * @see javax.management.MBeanServer#removeNotificationListener(ObjectName,
         *      NotificationListener)
         */
        public void removeNotificationListener(ObjectName name,
                        NotificationListener listener)
                throws InstanceNotFoundException,
                       ListenerNotFoundException {
                               throw new TestBug("not implemented");
                       }

        /**
         * Removes a listener from a registered MBean.
         *
         * @see javax.management.MBeanServer#removeNotificationListener(ObjectName,
         *      NotificationListener, NotificationFilter, Object)
         */
        public void removeNotificationListener(ObjectName name,
                        NotificationListener listener,
                        NotificationFilter filter,
                        Object handback)
                throws InstanceNotFoundException,
                       ListenerNotFoundException {
                       }

        /**
         * This method discovers the attributes and operations that an
         * MBean exposes for management.
         *
         * @see javax.management.MBeanServer#getMBeanInfo(ObjectName)
         */
        public MBeanInfo getMBeanInfo(ObjectName name)
                throws InstanceNotFoundException,
                       IntrospectionException,
                       ReflectionException {
                               throw new TestBug("not implemented");
                       }

        /**
         * Returns true if the MBean specified is an instance of the
         * specified class, false otherwise.
         *
         * @see javax.management.MBeanServer#isInstanceOf(ObjectName, String)
         */
        public boolean isInstanceOf(ObjectName name, String className)
                throws InstanceNotFoundException {
                        //        DynamicMBean mbean = getObject(name);
                        //        MBeanInfo info = mbean.getMBeanInfo();
                        //        return info.getClassName().compareTo(className) == 0;

                        DynamicMBean mbean = getObject(name);
                        MBeanInfo info = mbean.getMBeanInfo();
                        String infoClassName = info.getClassName();

                        if (log != null) {
                                log.trace(TRACE_ALL, "[isInstanceOf] name=" + name);
                                log.trace(TRACE_ALL, "[isInstanceOf] className=" + className);
                        }

                        if (infoClassName.equals(className)) {
                                if (log != null)
                                        log.trace(TRACE_ALL, "[isInstanceOf] infoClassName is equal className. return true");
                                return true;
                        }

                        try {
                                ClassLoader cl = mbean.getClass().getClassLoader();
                                Class<?> classNameClass = loadClass(className,cl);
                                if (classNameClass == null) {
                                        if (log != null)
                                                log.trace(TRACE_ALL, "[isInstanceOf] classNameClass is null. return false");
                                        return false;
                                }

                                if (classNameClass.isInstance(mbean)) {
                                        if (log != null)
                                                log.trace(TRACE_ALL, "[isInstanceOf] mbean is instance of classNameClass. return true");
                                        return true;
                                }

                                Class<?> instanceClass = loadClass(infoClassName,cl);
                                if (instanceClass == null) {
                                        if (log != null)
                                                log.trace(TRACE_ALL, "[isInstanceOf] instanceClass is null. return false");
                                        return false;
                                }

                                boolean isAssignable = classNameClass.isAssignableFrom(instanceClass);
                                if (log != null)
                                        log.trace(TRACE_ALL, "[isInstanceOf] classNameClass is assignable="+isAssignable);
                                return isAssignable;
                        } catch (ReflectionException e) {
                                if (log != null) {
                                        log.trace(TRACE_ALL, "[isInstanceOf] "+e.getMessage());
                                        e.printStackTrace(log.getOutStream());
                                }
                                return false;
                        } catch (Exception e) {
                                /* Could be SecurityException or ClassNotFoundException */
                                if (log != null) {
                                        log.trace(TRACE_ALL, "[isInstanceOf] "+e.getMessage());
                                        e.printStackTrace(log.getOutStream());
                                }
                                return false;
                        }

                }

        /**
         * Load a class with the specified loader, or with this object
         * class loader if the specified loader is null.
         **/
        static Class loadClass(String className, ClassLoader loader)
                throws ReflectionException {

                        Class theClass = null;
                        if (className == null) {
                                throw new RuntimeOperationsException(new
                                                IllegalArgumentException("The class name cannot be null"),
                                                "Exception occured during object instantiation");
                        }
                        try {
                                if (loader == null)
                                        loader = CustomMBeanServer.class.getClassLoader();
                                if (loader != null) {
                                        theClass = Class.forName(className, false, loader);
                                } else {
                                        theClass = Class.forName(className);
                                }
                        } catch (ClassNotFoundException e) {
                                throw new ReflectionException(e,
                                                "The MBean class could not be loaded by the context classloader");
                        }
                        return theClass;
                }


        /**
         * Instantiates an object using the list of all class loaders
         * registered in the MBean server's.
         *
         * @see javax.management.MBeanServer#instantiate(String)
         */
        public Object instantiate(String className)
                throws ReflectionException,
                       MBeanException {
                               throw new TestBug("not implemented");
                       }

        /**
         * Instantiates an object using the list of all class loaders
         * registered in the MBean server's.
         *
         * @see javax.management.MBeanServer#instantiate(String, ObjectName)
         */
        public Object instantiate(String className, ObjectName loaderName)
                throws ReflectionException,
                       MBeanException,
                       InstanceNotFoundException {
                               throw new TestBug("not implemented");
                       }

        /**
         * Instantiates an object using the list of all class loaders
         * registered in the MBean server's.
         *
         * @see javax.management.MBeanServer#instantiate(String, Object[],
         *      String[])
         */
        public Object instantiate(String className, Object[] params,
                        String[] signature)
                throws ReflectionException,
                       MBeanException {
                               throw new TestBug("not implemented");
                       }

        /**
         * Instantiates an object using the list of all class loaders
         * registered in the MBean server's.
         *
         * @see javax.management.MBeanServer#instantiate(String, ObjectName,
         *      Object[], String[])
         */
        public Object instantiate(String className, ObjectName loaderName,
                        Object[] params, String[] signature)
                throws ReflectionException,
                       MBeanException,
                       InstanceNotFoundException {
                               throw new TestBug("not implemented");
                       }

        /**
         * De-serializes a byte array in the context of the class loader
         * of an MBean.
         *
         * @see javax.management.MBeanServer#deserialize(ObjectName, byte[])
         */
        public ObjectInputStream deserialize(ObjectName name, byte[] data)
                throws InstanceNotFoundException,
                       OperationsException {
                               throw new TestBug("not implemented");
                       }

        /**
         * De-serializes a byte array in the context of the class loader
         * of an MBean.
         *
         * @see javax.management.MBeanServer#deserialize(String, byte[])
         */
        public ObjectInputStream deserialize(String className, byte[] data)
                throws OperationsException,
                       ReflectionException {
                               throw new TestBug("not implemented");
                       }

        /**
         * De-serializes a byte array in the context of the class loader
         * of an MBean.
         *
         * @see javax.management.MBeanServer#deserialize(String, ObjectName, byte[])
         */
        public ObjectInputStream deserialize(String className,
                        ObjectName loaderName,
                        byte[] data)
                throws InstanceNotFoundException,
                       OperationsException,
                       ReflectionException {
                               throw new TestBug("not implemented");
                       }

        /**
         * Return the {@link java.lang.ClassLoader} that was used for
         * loading the class of the named MBean.
         *
         * @see javax.management.MBeanServer#getClassLoaderFor(ObjectName)
         */
        public ClassLoader getClassLoaderFor(ObjectName mbeanName)
                throws InstanceNotFoundException {
                        throw new TestBug("not implemented");
                }

        /**
         * Return the named {@link java.lang.ClassLoader}.
         *
         * @see javax.management.MBeanServer#getClassLoader(ObjectName)
         */
        public ClassLoader getClassLoader(ObjectName loaderName)
                throws InstanceNotFoundException {
                        throw new TestBug("not implemented");
                }

        /**
         * Return the named {@link java.lang.ClassLoader}.
         *
         * @see javax.management.MBeanServer#getClassLoader(ObjectName)
         */
        public ClassLoaderRepository getClassLoaderRepository() {
                throw new TestBug("not implemented");
        }

        /**
         * Initializes {@link Log <code>Log</code>} object.
         *
         * @param log a new <code>Log</code> object.
         */
        public void setLog(Log log) {
                this.log = new Log.Logger(log, LOG_PREFIX + "> ");
        }

        // **********************************************************************
        //
        // Private methods
        //
        // **********************************************************************

        /**
         * Gets the object reference for a given MBean registered with the MBean
         * server.
         *
         * @param name The object name of the MBean.
         *
         * @return The MBean object, specified by <code>name</code>.
         *
         * @throws InstanceNotFoundException The MBean specified is not registered
         *         in the MBean server.
         */
        private DynamicMBean getObject(ObjectName name) throws InstanceNotFoundException {
                DynamicMBean mbean = registeredMBeansStd.get(name);
                if (mbean == null) {
                        ObjectKeeper objKeeper = registeredMBeans.get(name);
                        if (objKeeper == null)
                                throw new InstanceNotFoundException();
                        Object object = objKeeper.object;
                        if (object instanceof DynamicMBean)
                                mbean = (DynamicMBean) object;
                        else
                                mbean = new StandardMBean(object, getMBeanInterface(object), true);
                        registeredMBeansStd.put(name, mbean);
                }
                return mbean;
                /*
                   ObjectKeeper objKeeper = (ObjectKeeper) registeredMBeans.get(name);

                   if (objKeeper == null)
                   throw new InstanceNotFoundException();

                   Class superOfMBeans = null;
                   try {
                   superOfMBeans = Class.forName(DYNAMICMBEAN_ITNF_NAME);
                   } catch (ClassNotFoundException e) {
                   throw new InstanceNotFoundException();
                   }

                   if (superOfMBeans.isAssignableFrom(objKeeper.object.getClass())) {
                   return (DynamicMBean )objKeeper.object;
                   }

                   return null;
                   */
        }

        /**
         * Obtain NotificationBroadcaster for given MBean registered with the MBean
         * server.
         *
         * @param name The object name of the MBean.
         *
         * @return The MBean object, specified by <code>name</code>.
         *
         * @throws InstanceNotFoundException if MBean specified is not registered
         *         in the MBean server.
         */
        private NotificationBroadcaster getNotificationBroadcaster(ObjectName name) throws InstanceNotFoundException {
                ObjectKeeper objKeeper = (ObjectKeeper) registeredMBeans.get(name);
                if (objKeeper == null)
                        throw new InstanceNotFoundException();
                Object mbean = objKeeper.object;
                if (mbean instanceof NotificationBroadcaster)
                        return (NotificationBroadcaster) mbean;
                throw new InstanceNotFoundException();
        }

        /**
         * Invoke the method
         */
        private Object invokeObjectMethod(ObjectName name, String methodName,
                        Object[] params, String[] signature) throws InstanceNotFoundException,
                MBeanException,
                ReflectionException {

                        if (log != null)
                                log.trace(TRACE_ALL, "[invokeObjectMethod] " + name + "> "
                                                + methodName);

                        DynamicMBean mbean = getObject(name);
                        return mbean.invoke(methodName, params, signature);
                }

        private Class getInterface(Class cl, String prefix) {
                Class[] interfaces = cl.getInterfaces();
                if (interfaces == null || interfaces.length == 0)
                        return null;
                for (Class c : interfaces) {
                        if (c.getName().startsWith(prefix))
                                return c;
                        c = getInterface(c, prefix);
                        if (c != null)
                                return c;
                }
                return null;
        }

        /**
         * Discover MBean interface of the bean.
         *
         * Note: this is very specialized for java.lang.management
         * and java.util.logging tests.
         * It is generally not correct for any MBean.
         *
         * @param object the bean
         * @return interface class
         */
        private Class getMBeanInterface(Object object) throws InstanceNotFoundException {
                String className = object.getClass().getName();
                Class<?> iface = null;
                if (className.startsWith("java.lang.management"))
                        iface = getInterface(object.getClass(), "java.lang.management");
                else if (className.startsWith("java.util.logging"))
                        iface = getInterface(object.getClass(), "java.util.logging");
                else if (className.startsWith("sun.management"))
                        iface = getInterface(object.getClass(), "java.lang.management");
                if (iface != null)
                        return iface;
                Class<?>[] interfaces = object.getClass().getInterfaces();
                System.out.println(object);
                System.out.println(object.getClass());
                System.out.println(interfaces.length);
                for (Class<?> c : interfaces) {
                        System.out.println(c.getName());
                }
                throw new TestBug("No suitable implemented interface found for: " + object + " class: " + object.getClass());
        }
}
