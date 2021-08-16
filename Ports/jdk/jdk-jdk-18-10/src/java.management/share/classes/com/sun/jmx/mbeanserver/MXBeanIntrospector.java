/*
 * Copyright (c) 2005, 2008, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.jmx.mbeanserver.MBeanIntrospector.MBeanInfoMap;
import com.sun.jmx.mbeanserver.MBeanIntrospector.PerInterfaceMap;
import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenMBeanOperationInfoSupport;
import javax.management.openmbean.OpenMBeanParameterInfo;
import javax.management.openmbean.OpenMBeanParameterInfoSupport;
import javax.management.openmbean.OpenType;

/**
 * Introspector for MXBeans.  There is exactly one instance of this class.
 *
 * @since 1.6
 */
class MXBeanIntrospector extends MBeanIntrospector<ConvertingMethod> {
    private static final MXBeanIntrospector instance = new MXBeanIntrospector();

    static MXBeanIntrospector getInstance() {
        return instance;
    }

    @Override
    PerInterfaceMap<ConvertingMethod> getPerInterfaceMap() {
        return perInterfaceMap;
    }

    @Override
    MBeanInfoMap getMBeanInfoMap() {
        return mbeanInfoMap;
    }

    @Override
    MBeanAnalyzer<ConvertingMethod> getAnalyzer(Class<?> mbeanInterface)
            throws NotCompliantMBeanException {
        return MBeanAnalyzer.analyzer(mbeanInterface, this);
    }

    @Override
    boolean isMXBean() {
        return true;
    }

    @Override
    ConvertingMethod mFrom(Method m) {
        return ConvertingMethod.from(m);
    }

    @Override
    String getName(ConvertingMethod m) {
        return m.getName();
    }

    @Override
    Type getGenericReturnType(ConvertingMethod m) {
        return m.getGenericReturnType();
    }

    @Override
    Type[] getGenericParameterTypes(ConvertingMethod m) {
        return m.getGenericParameterTypes();
    }

    @Override
    String[] getSignature(ConvertingMethod m) {
        return m.getOpenSignature();
    }

    @Override
    void checkMethod(ConvertingMethod m) {
        m.checkCallFromOpen();
    }

    @Override
    Object invokeM2(ConvertingMethod m, Object target, Object[] args,
                    Object cookie)
            throws InvocationTargetException, IllegalAccessException,
                   MBeanException {
        return m.invokeWithOpenReturn((MXBeanLookup) cookie, target, args);
    }

    @Override
    boolean validParameter(ConvertingMethod m, Object value, int paramNo,
                           Object cookie) {
        if (value == null) {
            // Null is a valid value for all OpenTypes, even though
            // OpenType.isValue(null) will return false.  It can always be
            // matched to the corresponding Java type, except when that
            // type is primitive.
            Type t = m.getGenericParameterTypes()[paramNo];
            return (!(t instanceof Class<?>) || !((Class<?>) t).isPrimitive());
        } else {
            Object v;
            try {
                v = m.fromOpenParameter((MXBeanLookup) cookie, value, paramNo);
            } catch (Exception e) {
                // Ignore the exception and let MBeanIntrospector.invokeSetter()
                // throw the initial exception.
                return true;
            }
            return isValidParameter(m.getMethod(), v, paramNo);
        }
    }

    @Override
    MBeanAttributeInfo getMBeanAttributeInfo(String attributeName,
            ConvertingMethod getter, ConvertingMethod setter) {

        final boolean isReadable = (getter != null);
        final boolean isWritable = (setter != null);
        final boolean isIs = isReadable && getName(getter).startsWith("is");

        final String description = attributeName;

        final OpenType<?> openType;
        final Type originalType;
        if (isReadable) {
            openType = getter.getOpenReturnType();
            originalType = getter.getGenericReturnType();
        } else {
            openType = setter.getOpenParameterTypes()[0];
            originalType = setter.getGenericParameterTypes()[0];
        }
        Descriptor descriptor = typeDescriptor(openType, originalType);
        if (isReadable) {
            descriptor = ImmutableDescriptor.union(descriptor,
                    getter.getDescriptor());
        }
        if (isWritable) {
            descriptor = ImmutableDescriptor.union(descriptor,
                    setter.getDescriptor());
        }

        final MBeanAttributeInfo ai;
        if (canUseOpenInfo(originalType)) {
            ai = new OpenMBeanAttributeInfoSupport(attributeName,
                                                   description,
                                                   openType,
                                                   isReadable,
                                                   isWritable,
                                                   isIs,
                                                   descriptor);
        } else {
            ai = new MBeanAttributeInfo(attributeName,
                                        originalTypeString(originalType),
                                        description,
                                        isReadable,
                                        isWritable,
                                        isIs,
                                        descriptor);
        }
        // could also consult annotations for defaultValue,
        // minValue, maxValue, legalValues

        return ai;
    }

    @Override
    MBeanOperationInfo getMBeanOperationInfo(String operationName,
            ConvertingMethod operation) {
        final Method method = operation.getMethod();
        final String description = operationName;
        /* Ideally this would be an empty string, but
           OMBOperationInfo constructor forbids that.  Also, we
           could consult an annotation to get a useful
           description.  */

        final int impact = MBeanOperationInfo.UNKNOWN;

        final OpenType<?> returnType = operation.getOpenReturnType();
        final Type originalReturnType = operation.getGenericReturnType();
        final OpenType<?>[] paramTypes = operation.getOpenParameterTypes();
        final Type[] originalParamTypes = operation.getGenericParameterTypes();
        final MBeanParameterInfo[] params =
            new MBeanParameterInfo[paramTypes.length];
        boolean openReturnType = canUseOpenInfo(originalReturnType);
        boolean openParameterTypes = true;
        Annotation[][] annots = method.getParameterAnnotations();
        for (int i = 0; i < paramTypes.length; i++) {
            final String paramName = "p" + i;
            final String paramDescription = paramName;
            final OpenType<?> openType = paramTypes[i];
            final Type originalType = originalParamTypes[i];
            Descriptor descriptor =
                typeDescriptor(openType, originalType);
            descriptor = ImmutableDescriptor.union(descriptor,
                    Introspector.descriptorForAnnotations(annots[i]));
            final MBeanParameterInfo pi;
            if (canUseOpenInfo(originalType)) {
                pi = new OpenMBeanParameterInfoSupport(paramName,
                                                       paramDescription,
                                                       openType,
                                                       descriptor);
            } else {
                openParameterTypes = false;
                pi = new MBeanParameterInfo(
                    paramName,
                    originalTypeString(originalType),
                    paramDescription,
                    descriptor);
            }
            params[i] = pi;
        }

        Descriptor descriptor =
            typeDescriptor(returnType, originalReturnType);
        descriptor = ImmutableDescriptor.union(descriptor,
                Introspector.descriptorForElement(method));
        final MBeanOperationInfo oi;
        if (openReturnType && openParameterTypes) {
            /* If the return value and all the parameters can be faithfully
             * represented as OpenType then we return an OpenMBeanOperationInfo.
             * If any of them is a primitive type, we can't.  Compatibility
             * with JSR 174 means that we must return an MBean*Info where
             * the getType() is the primitive type, not its wrapped type as
             * we would get with an OpenMBean*Info.  The OpenType is available
             * in the Descriptor in either case.
             */
            final OpenMBeanParameterInfo[] oparams =
                new OpenMBeanParameterInfo[params.length];
            System.arraycopy(params, 0, oparams, 0, params.length);
            oi = new OpenMBeanOperationInfoSupport(operationName,
                                                   description,
                                                   oparams,
                                                   returnType,
                                                   impact,
                                                   descriptor);
        } else {
            oi = new MBeanOperationInfo(operationName,
                                        description,
                                        params,
                                        openReturnType ?
                                        returnType.getClassName() :
                                        originalTypeString(originalReturnType),
                                        impact,
                                        descriptor);
        }

        return oi;
    }

    @Override
    Descriptor getBasicMBeanDescriptor() {
        return new ImmutableDescriptor("mxbean=true",
                                       "immutableInfo=true");
    }

    @Override
    Descriptor getMBeanDescriptor(Class<?> resourceClass) {
        /* We already have immutableInfo=true in the Descriptor
         * included in the MBeanInfo for the MXBean interface.  This
         * method is being called for the MXBean *class* to add any
         * new items beyond those in the interface Descriptor, which
         * currently it does not.
         */
        return ImmutableDescriptor.EMPTY_DESCRIPTOR;
    }

    private static Descriptor typeDescriptor(OpenType<?> openType,
                                             Type originalType) {
        return new ImmutableDescriptor(
            new String[] {"openType",
                          "originalType"},
            new Object[] {openType,
                          originalTypeString(originalType)});
    }

    /**
     * <p>True if this type can be faithfully represented in an
     * OpenMBean*Info.</p>
     *
     * <p>Compatibility with JSR 174 means that primitive types must be
     * represented by an MBean*Info whose getType() is the primitive type
     * string, e.g. "int".  If we used an OpenMBean*Info then this string
     * would be the wrapped type, e.g. "java.lang.Integer".</p>
     *
     * <p>Compatibility with JMX 1.2 (including J2SE 5.0) means that arrays
     * of primitive types cannot use an ArrayType representing an array of
     * primitives, because that didn't exist in JMX 1.2.</p>
     */
    private static boolean canUseOpenInfo(Type type) {
        if (type instanceof GenericArrayType) {
            return canUseOpenInfo(
                ((GenericArrayType) type).getGenericComponentType());
        } else if (type instanceof Class<?> && ((Class<?>) type).isArray()) {
            return canUseOpenInfo(
                ((Class<?>) type).getComponentType());
        }
        return (!(type instanceof Class<?> && ((Class<?>) type).isPrimitive()));
    }

    private static String originalTypeString(Type type) {
        if (type instanceof Class<?>)
            return ((Class<?>) type).getName();
        else
            return typeName(type);
    }

    static String typeName(Type type) {
        if (type instanceof Class<?>) {
            Class<?> c = (Class<?>) type;
            if (c.isArray())
                return typeName(c.getComponentType()) + "[]";
            else
                return c.getName();
        } else if (type instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType) type;
            return typeName(gat.getGenericComponentType()) + "[]";
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            StringBuilder sb = new StringBuilder();
            sb.append(typeName(pt.getRawType())).append("<");
            String sep = "";
            for (Type t : pt.getActualTypeArguments()) {
                sb.append(sep).append(typeName(t));
                sep = ", ";
            }
            return sb.append(">").toString();
        } else
            return "???";
    }

    private final PerInterfaceMap<ConvertingMethod>
        perInterfaceMap = new PerInterfaceMap<ConvertingMethod>();

    private static final MBeanInfoMap mbeanInfoMap = new MBeanInfoMap();
}
