/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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


import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import com.sun.jmx.mbeanserver.MXBeanMappingFactory;
import sun.reflect.misc.ReflectUtil;

/**
 * Base class for MBeans.  There is one instance of this class for
 * every Standard MBean and every MXBean.  We try to limit the amount
 * of information per instance so we can handle very large numbers of
 * MBeans comfortably.
 *
 * @param <M> either Method or ConvertingMethod, for Standard MBeans
 * and MXBeans respectively.
 *
 * @since 1.6
 */
/*
 * We maintain a couple of caches to increase sharing between
 * different MBeans of the same type and also to reduce creation time
 * for the second and subsequent instances of the same type.
 *
 * The first cache maps from an MBean interface to a PerInterface
 * object containing information parsed out of the interface.  The
 * interface is either a Standard MBean interface or an MXBean
 * interface, and there is one cache for each case.
 *
 * The PerInterface includes an MBeanInfo.  This contains the
 * attributes and operations parsed out of the interface's methods,
 * plus a basic Descriptor for the interface containing at least the
 * interfaceClassName field and any fields derived from annotations on
 * the interface.  This MBeanInfo can never be the MBeanInfo for any
 * actual MBean, because an MBeanInfo's getClassName() is the name of
 * a concrete class and we don't know what the class will be.
 * Furthermore a real MBeanInfo may need to add constructors and/or
 * notifications to the MBeanInfo.
 *
 * The PerInterface also contains an MBeanDispatcher which is able to
 * route getAttribute, setAttribute, and invoke to the appropriate
 * method of the interface, including doing any necessary translation
 * of parameters and return values for MXBeans.
 *
 * The PerInterface also contains the original Class for the interface.
 *
 * We need to be careful about references.  When there are no MBeans
 * with a given interface, there must not be any strong references to
 * the interface Class.  Otherwise it could never be garbage collected,
 * and neither could its ClassLoader or any other classes loaded by
 * its ClassLoader.  Therefore the cache must wrap the PerInterface
 * in a WeakReference.  Each instance of MBeanSupport has a strong
 * reference to its PerInterface, which prevents PerInterface instances
 * from being garbage-collected prematurely.
 *
 * The second cache maps from a concrete class and an MBean interface
 * that that class implements to the MBeanInfo for that class and
 * interface.  (The ability to specify an interface separately comes
 * from the class StandardMBean.  MBeans registered directly in the
 * MBean Server will always have the same interface here.)
 *
 * The MBeanInfo in this second cache will be the MBeanInfo from the
 * PerInterface cache for the given itnerface, but with the
 * getClassName() having the concrete class's name, and the public
 * constructors based on the concrete class's constructors.  This
 * MBeanInfo can be shared between all instances of the concrete class
 * specifying the same interface, except instances that are
 * NotificationBroadcasters.  NotificationBroadcasters supply the
 * MBeanNotificationInfo[] in the MBeanInfo based on the instance
 * method NotificationBroadcaster.getNotificationInfo(), so two
 * instances of the same concrete class do not necessarily have the
 * same MBeanNotificationInfo[].  Currently we do not try to detect
 * when they do, although it would probably be worthwhile doing that
 * since it is a very common case.
 *
 * Standard MBeans additionally have the property that
 * getNotificationInfo() must in principle be called every time
 * getMBeanInfo() is called for the MBean, since the returned array is
 * allowed to change over time.  We attempt to reduce the cost of
 * doing this by detecting when the Standard MBean is a subclass of
 * NotificationBroadcasterSupport that does not override
 * getNotificationInfo(), meaning that the MBeanNotificationInfo[] is
 * the one that was supplied to the constructor.  MXBeans do not have
 * this problem because their getNotificationInfo() method is called
 * only once.
 *
 */
public abstract class MBeanSupport<M>
        implements DynamicMBean2, MBeanRegistration {

    <T> MBeanSupport(T resource, Class<T> mbeanInterfaceType)
            throws NotCompliantMBeanException {
        if (mbeanInterfaceType == null)
            throw new NotCompliantMBeanException("Null MBean interface");
        if (!mbeanInterfaceType.isInstance(resource)) {
            final String msg =
                "Resource class " + resource.getClass().getName() +
                " is not an instance of " + mbeanInterfaceType.getName();
            throw new NotCompliantMBeanException(msg);
        }
        ReflectUtil.checkPackageAccess(mbeanInterfaceType);
        this.resource = resource;
        MBeanIntrospector<M> introspector = getMBeanIntrospector();
        this.perInterface = introspector.getPerInterface(mbeanInterfaceType);
        this.mbeanInfo = introspector.getMBeanInfo(resource, perInterface);
    }

    /** Return the appropriate introspector for this type of MBean. */
    abstract MBeanIntrospector<M> getMBeanIntrospector();

    /**
     * Return a cookie for this MBean.  This cookie will be passed to
     * MBean method invocations where it can supply additional information
     * to the invocation.  For example, with MXBeans it can be used to
     * supply the MXBeanLookup context for resolving inter-MXBean references.
     */
    abstract Object getCookie();

    public final boolean isMXBean() {
        return perInterface.isMXBean();
    }

    // Methods that javax.management.StandardMBean should call from its
    // preRegister and postRegister, given that it is not supposed to
    // call the contained object's preRegister etc methods even if it has them
    public abstract void register(MBeanServer mbs, ObjectName name)
            throws Exception;
    public abstract void unregister();

    public final ObjectName preRegister(MBeanServer server, ObjectName name)
            throws Exception {
        if (resource instanceof MBeanRegistration)
            name = ((MBeanRegistration) resource).preRegister(server, name);
        return name;
    }

    public final void preRegister2(MBeanServer server, ObjectName name)
            throws Exception {
        register(server, name);
    }

    public final void registerFailed() {
        unregister();
    }

    public final void postRegister(Boolean registrationDone) {
        if (resource instanceof MBeanRegistration)
            ((MBeanRegistration) resource).postRegister(registrationDone);
    }

    public final void preDeregister() throws Exception {
        if (resource instanceof MBeanRegistration)
            ((MBeanRegistration) resource).preDeregister();
    }

    public final void postDeregister() {
        // Undo any work from registration.  We do this in postDeregister
        // not preDeregister, because if the user preDeregister throws an
        // exception then the MBean is not unregistered.
        try {
            unregister();
        } finally {
            if (resource instanceof MBeanRegistration)
                ((MBeanRegistration) resource).postDeregister();
        }
    }

    public final Object getAttribute(String attribute)
            throws AttributeNotFoundException,
                   MBeanException,
                   ReflectionException {
        return perInterface.getAttribute(resource, attribute, getCookie());
    }

    public final AttributeList getAttributes(String[] attributes) {
        final AttributeList result = new AttributeList(attributes.length);
        for (String attrName : attributes) {
            try {
                final Object attrValue = getAttribute(attrName);
                result.add(new Attribute(attrName, attrValue));
            } catch (Exception e) {
                // OK: attribute is not included in returned list, per spec
                // XXX: log the exception
            }
        }
        return result;
    }

    public final void setAttribute(Attribute attribute)
            throws AttributeNotFoundException,
                   InvalidAttributeValueException,
                   MBeanException,
                   ReflectionException {
        final String name = attribute.getName();
        final Object value = attribute.getValue();
        perInterface.setAttribute(resource, name, value, getCookie());
    }

    public final AttributeList setAttributes(AttributeList attributes) {
        final AttributeList result = new AttributeList(attributes.size());
        for (Object attrObj : attributes) {
            // We can't use AttributeList.asList because it has side-effects
            Attribute attr = (Attribute) attrObj;
            try {
                setAttribute(attr);
                result.add(new Attribute(attr.getName(), attr.getValue()));
            } catch (Exception e) {
                // OK: attribute is not included in returned list, per spec
                // XXX: log the exception
            }
        }
        return result;
    }

    public final Object invoke(String operation, Object[] params,
                         String[] signature)
            throws MBeanException, ReflectionException {
        return perInterface.invoke(resource, operation, params, signature,
                                   getCookie());
    }

    // Overridden by StandardMBeanSupport
    public MBeanInfo getMBeanInfo() {
        return mbeanInfo;
    }

    public final String getClassName() {
        return resource.getClass().getName();
    }

    public final Object getResource() {
        return resource;
    }

    public final Class<?> getMBeanInterface() {
        return perInterface.getMBeanInterface();
    }

    private final MBeanInfo mbeanInfo;
    private final Object resource;
    private final PerInterface<M> perInterface;
}
