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


import static com.sun.jmx.mbeanserver.Util.*;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationBroadcaster;
import javax.management.ReflectionException;
import sun.reflect.misc.ReflectUtil;

/**
 * An introspector for MBeans of a certain type.  There is one instance
 * of this class for Standard MBeans, and one for every MXBeanMappingFactory;
 * these two cases correspond to the two concrete subclasses of this abstract
 * class.
 *
 * @param <M> the representation of methods for this kind of MBean:
 * Method for Standard MBeans, ConvertingMethod for MXBeans.
 *
 * @since 1.6
 */
/*
 * Using a type parameter <M> allows us to deal with the fact that
 * Method and ConvertingMethod have no useful common ancestor, on
 * which we could call getName, getGenericReturnType, etc.  A simpler approach
 * would be to wrap every Method in an object that does have a common
 * ancestor with ConvertingMethod.  But that would mean an extra object
 * for every Method in every Standard MBean interface.
 */
abstract class MBeanIntrospector<M> {
    static final class PerInterfaceMap<M>
            extends WeakHashMap<Class<?>, WeakReference<PerInterface<M>>> {}

    /** The map from interface to PerInterface for this type of MBean. */
    abstract PerInterfaceMap<M> getPerInterfaceMap();
    /**
     * The map from concrete implementation class and interface to
     * MBeanInfo for this type of MBean.
     */
    abstract MBeanInfoMap getMBeanInfoMap();

    /** Make an interface analyzer for this type of MBean. */
    abstract MBeanAnalyzer<M> getAnalyzer(Class<?> mbeanInterface)
    throws NotCompliantMBeanException;

    /** True if MBeans with this kind of introspector are MXBeans. */
    abstract boolean isMXBean();

    /** Find the M corresponding to the given Method. */
    abstract M mFrom(Method m);

    /** Get the name of this method. */
    abstract String getName(M m);

    /**
     * Get the return type of this method.  This is the return type
     * of a method in a Java interface, so for MXBeans it is the
     * declared Java type, not the mapped Open Type.
     */
    abstract Type getGenericReturnType(M m);

    /**
     * Get the parameter types of this method in the Java interface
     * it came from.
     */
    abstract Type[] getGenericParameterTypes(M m);

    /**
     * Get the signature of this method as a caller would have to supply
     * it in MBeanServer.invoke.  For MXBeans, the named types will be
     * the mapped Open Types for the parameters.
     */
    abstract String[] getSignature(M m);

    /**
     * Check that this method is valid.  For example, a method in an
     * MXBean interface is not valid if one of its parameters cannot be
     * mapped to an Open Type.
     */
    abstract void checkMethod(M m);

    /**
     * Invoke the method with the given target and arguments.
     *
     * @param cookie Additional information about the target.  For an
     * MXBean, this is the MXBeanLookup associated with the MXBean.
     */
    /*
     * It would be cleaner if the type of the cookie were a
     * type parameter to this class, but that would involve a lot of
     * messy type parameter propagation just to avoid a couple of casts.
     */
    abstract Object invokeM2(M m, Object target, Object[] args, Object cookie)
    throws InvocationTargetException, IllegalAccessException,
            MBeanException;

    /**
     * Test whether the given value is valid for the given parameter of this
     * M.
     */
    abstract boolean validParameter(M m, Object value, int paramNo,
            Object cookie);

    /**
     * Construct an MBeanAttributeInfo for the given attribute based on the
     * given getter and setter.  One but not both of the getter and setter
     * may be null.
     */
    abstract MBeanAttributeInfo getMBeanAttributeInfo(String attributeName,
            M getter, M setter);
    /**
     * Construct an MBeanOperationInfo for the given operation based on
     * the M it was derived from.
     */
    abstract MBeanOperationInfo getMBeanOperationInfo(String operationName,
            M operation);

    /**
     * Get a Descriptor containing fields that MBeans of this kind will
     * always have.  For example, MXBeans will always have "mxbean=true".
     */
    abstract Descriptor getBasicMBeanDescriptor();

    /**
     * Get a Descriptor containing additional fields beyond the ones
     * from getBasicMBeanDescriptor that MBeans whose concrete class
     * is resourceClass will always have.
     */
    abstract Descriptor getMBeanDescriptor(Class<?> resourceClass);

    /**
     * Get the methods to be analyzed to build the MBean interface.
     */
    final List<Method> getMethods(final Class<?> mbeanType) {
        ReflectUtil.checkPackageAccess(mbeanType);
        return Arrays.asList(mbeanType.getMethods());
    }

    final PerInterface<M> getPerInterface(Class<?> mbeanInterface)
    throws NotCompliantMBeanException {
        PerInterfaceMap<M> map = getPerInterfaceMap();
        synchronized (map) {
            WeakReference<PerInterface<M>> wr = map.get(mbeanInterface);
            PerInterface<M> pi = (wr == null) ? null : wr.get();
            if (pi == null) {
                try {
                    MBeanAnalyzer<M> analyzer = getAnalyzer(mbeanInterface);
                    MBeanInfo mbeanInfo =
                            makeInterfaceMBeanInfo(mbeanInterface, analyzer);
                    pi = new PerInterface<M>(mbeanInterface, this, analyzer,
                            mbeanInfo);
                    wr = new WeakReference<PerInterface<M>>(pi);
                    map.put(mbeanInterface, wr);
                } catch (Exception x) {
                    throw Introspector.throwException(mbeanInterface,x);
                }
            }
            return pi;
        }
    }

    /**
     * Make the MBeanInfo skeleton for the given MBean interface using
     * the given analyzer.  This will never be the MBeanInfo of any real
     * MBean (because the getClassName() must be a concrete class), but
     * its MBeanAttributeInfo[] and MBeanOperationInfo[] can be inserted
     * into such an MBeanInfo, and its Descriptor can be the basis for
     * the MBeanInfo's Descriptor.
     */
    private MBeanInfo makeInterfaceMBeanInfo(Class<?> mbeanInterface,
            MBeanAnalyzer<M> analyzer) {
        final MBeanInfoMaker maker = new MBeanInfoMaker();
        analyzer.visit(maker);
        final String description =
                "Information on the management interface of the MBean";
        return maker.makeMBeanInfo(mbeanInterface, description);
    }

    /** True if the given getter and setter are consistent. */
    final boolean consistent(M getter, M setter) {
        return (getter == null || setter == null ||
                getGenericReturnType(getter).equals(getGenericParameterTypes(setter)[0]));
    }

    /**
     * Invoke the given M on the given target with the given args and cookie.
     * Wrap exceptions appropriately.
     */
    final Object invokeM(M m, Object target, Object[] args, Object cookie)
    throws MBeanException, ReflectionException {
        try {
            return invokeM2(m, target, args, cookie);
        } catch (InvocationTargetException e) {
            unwrapInvocationTargetException(e);
            throw new RuntimeException(e); // not reached
        } catch (IllegalAccessException e) {
            throw new ReflectionException(e, e.toString());
        }
        /* We do not catch and wrap RuntimeException or Error,
         * because we're in a DynamicMBean, so the logic for DynamicMBeans
         * will do the wrapping.
         */
    }

    /**
     * Invoke the given setter on the given target with the given argument
     * and cookie.  Wrap exceptions appropriately.
     */
    /* If the value is of the wrong type for the method we are about to
     * invoke, we are supposed to throw an InvalidAttributeValueException.
     * Rather than making the check always, we invoke the method, then
     * if it throws an exception we check the type to see if that was
     * what caused the exception.  The assumption is that an exception
     * from an invalid type will arise before any user method is ever
     * called (either in reflection or in OpenConverter).
     */
    final void invokeSetter(String name, M setter, Object target, Object arg,
            Object cookie)
            throws MBeanException, ReflectionException,
            InvalidAttributeValueException {
        try {
            invokeM2(setter, target, new Object[] {arg}, cookie);
        } catch (IllegalAccessException e) {
            throw new ReflectionException(e, e.toString());
        } catch (RuntimeException e) {
            maybeInvalidParameter(name, setter, arg, cookie);
            throw e;
        } catch (InvocationTargetException e) {
            maybeInvalidParameter(name, setter, arg, cookie);
            unwrapInvocationTargetException(e);
        }
    }

    private void maybeInvalidParameter(String name, M setter, Object arg,
            Object cookie)
            throws InvalidAttributeValueException {
        if (!validParameter(setter, arg, 0, cookie)) {
            final String msg =
                    "Invalid value for attribute " + name + ": " + arg;
            throw new InvalidAttributeValueException(msg);
        }
    }

    static boolean isValidParameter(Method m, Object value, int paramNo) {
        Class<?> c = m.getParameterTypes()[paramNo];
        try {
            // Following is expensive but we only call this method to determine
            // if an exception is due to an incompatible parameter type.
            // Plain old c.isInstance doesn't work for primitive types.
            Object a = Array.newInstance(c, 1);
            Array.set(a, 0, value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static void
            unwrapInvocationTargetException(InvocationTargetException e)
            throws MBeanException {
        Throwable t = e.getCause();
        if (t instanceof RuntimeException)
            throw (RuntimeException) t;
        else if (t instanceof Error)
            throw (Error) t;
        else
            throw new MBeanException((Exception) t,
                    (t == null ? null : t.toString()));
    }

    /** A visitor that constructs the per-interface MBeanInfo. */
    private class MBeanInfoMaker
            implements MBeanAnalyzer.MBeanVisitor<M> {

        public void visitAttribute(String attributeName,
                M getter,
                M setter) {
            MBeanAttributeInfo mbai =
                    getMBeanAttributeInfo(attributeName, getter, setter);

            attrs.add(mbai);
        }

        public void visitOperation(String operationName,
                M operation) {
            MBeanOperationInfo mboi =
                    getMBeanOperationInfo(operationName, operation);

            ops.add(mboi);
        }

        /** Make an MBeanInfo based on the attributes and operations
         *  found in the interface. */
        MBeanInfo makeMBeanInfo(Class<?> mbeanInterface,
                String description) {
            final MBeanAttributeInfo[] attrArray =
                    attrs.toArray(new MBeanAttributeInfo[0]);
            final MBeanOperationInfo[] opArray =
                    ops.toArray(new MBeanOperationInfo[0]);
            final String interfaceClassName =
                    "interfaceClassName=" + mbeanInterface.getName();
            final Descriptor classNameDescriptor =
                    new ImmutableDescriptor(interfaceClassName);
            final Descriptor mbeanDescriptor = getBasicMBeanDescriptor();
            final Descriptor annotatedDescriptor =
                    Introspector.descriptorForElement(mbeanInterface);
            final Descriptor descriptor =
                DescriptorCache.getInstance().union(
                    classNameDescriptor,
                    mbeanDescriptor,
                    annotatedDescriptor);

            return new MBeanInfo(mbeanInterface.getName(),
                    description,
                    attrArray,
                    null,
                    opArray,
                    null,
                    descriptor);
        }

        private final List<MBeanAttributeInfo> attrs = newList();
        private final List<MBeanOperationInfo> ops = newList();
    }

    /*
     * Looking up the MBeanInfo for a given base class (implementation class)
     * is complicated by the fact that we may use the same base class with
     * several different explicit MBean interfaces via the
     * javax.management.StandardMBean class.  It is further complicated
     * by the fact that we have to be careful not to retain a strong reference
     * to any Class object for fear we would prevent a ClassLoader from being
     * garbage-collected.  So we have a first lookup from the base class
     * to a map for each interface that base class might specify giving
     * the MBeanInfo constructed for that base class and interface.
     */
    static class MBeanInfoMap
            extends WeakHashMap<Class<?>, WeakHashMap<Class<?>, MBeanInfo>> {
    }

    /**
     * Return the MBeanInfo for the given resource, based on the given
     * per-interface data.
     */
    final MBeanInfo getMBeanInfo(Object resource, PerInterface<M> perInterface) {
        MBeanInfo mbi =
                getClassMBeanInfo(resource.getClass(), perInterface);
        MBeanNotificationInfo[] notifs = findNotifications(resource);
        if (notifs == null || notifs.length == 0)
            return mbi;
        else {
            return new MBeanInfo(mbi.getClassName(),
                    mbi.getDescription(),
                    mbi.getAttributes(),
                    mbi.getConstructors(),
                    mbi.getOperations(),
                    notifs,
                    mbi.getDescriptor());
        }
    }

    /**
     * Return the basic MBeanInfo for resources of the given class and
     * per-interface data.  This MBeanInfo might not be the final MBeanInfo
     * for instances of the class, because if the class is a
     * NotificationBroadcaster then each instance gets to decide what
     * MBeanNotificationInfo[] to put in its own MBeanInfo.
     */
    final MBeanInfo getClassMBeanInfo(Class<?> resourceClass,
            PerInterface<M> perInterface) {
        MBeanInfoMap map = getMBeanInfoMap();
        synchronized (map) {
            WeakHashMap<Class<?>, MBeanInfo> intfMap = map.get(resourceClass);
            if (intfMap == null) {
                intfMap = new WeakHashMap<Class<?>, MBeanInfo>();
                map.put(resourceClass, intfMap);
            }
            Class<?> intfClass = perInterface.getMBeanInterface();
            MBeanInfo mbi = intfMap.get(intfClass);
            if (mbi == null) {
                MBeanInfo imbi = perInterface.getMBeanInfo();
                Descriptor descriptor =
                        ImmutableDescriptor.union(imbi.getDescriptor(),
                        getMBeanDescriptor(resourceClass));
                mbi = new MBeanInfo(resourceClass.getName(),
                        imbi.getDescription(),
                        imbi.getAttributes(),
                        findConstructors(resourceClass),
                        imbi.getOperations(),
                        (MBeanNotificationInfo[]) null,
                        descriptor);
                intfMap.put(intfClass, mbi);
            }
            return mbi;
        }
    }

    static MBeanNotificationInfo[] findNotifications(Object moi) {
        if (!(moi instanceof NotificationBroadcaster))
            return null;
        MBeanNotificationInfo[] mbn =
                ((NotificationBroadcaster) moi).getNotificationInfo();
        if (mbn == null)
            return null;
        MBeanNotificationInfo[] result =
                new MBeanNotificationInfo[mbn.length];
        for (int i = 0; i < mbn.length; i++) {
            MBeanNotificationInfo ni = mbn[i];
            if (ni.getClass() != MBeanNotificationInfo.class)
                ni = (MBeanNotificationInfo) ni.clone();
            result[i] = ni;
        }
        return result;
    }

    private static MBeanConstructorInfo[] findConstructors(Class<?> c) {
        Constructor<?>[] cons = c.getConstructors();
        MBeanConstructorInfo[] mbc = new MBeanConstructorInfo[cons.length];
        for (int i = 0; i < cons.length; i++) {
            final String descr = "Public constructor of the MBean";
            mbc[i] = new MBeanConstructorInfo(descr, cons[i]);
        }
        return mbc;
    }

}
