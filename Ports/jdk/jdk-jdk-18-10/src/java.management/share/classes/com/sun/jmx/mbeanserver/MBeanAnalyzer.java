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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.NotCompliantMBeanException;

/**
 * <p>An analyzer for a given MBean interface.  The analyzer can
 * be for Standard MBeans or MXBeans, depending on the MBeanIntrospector
 * passed at construction.
 *
 * <p>The analyzer can
 * visit the attributes and operations of the interface, calling
 * a caller-supplied visitor method for each one.</p>
 *
 * @param <M> Method or ConvertingMethod according as this is a
 * Standard MBean or an MXBean.
 *
 * @since 1.6
 */
class MBeanAnalyzer<M> {
    static interface MBeanVisitor<M> {
        public void visitAttribute(String attributeName,
                M getter,
                M setter);
        public void visitOperation(String operationName,
                M operation);
    }

    void visit(MBeanVisitor<M> visitor) {
        // visit attributes
        for (Map.Entry<String, AttrMethods<M>> entry : attrMap.entrySet()) {
            String name = entry.getKey();
            AttrMethods<M> am = entry.getValue();
            visitor.visitAttribute(name, am.getter, am.setter);
        }

        // visit operations
        for (Map.Entry<String, List<M>> entry : opMap.entrySet()) {
            for (M m : entry.getValue())
                visitor.visitOperation(entry.getKey(), m);
        }
    }

    /* Map op name to method */
    private Map<String, List<M>> opMap = newInsertionOrderMap();
    /* Map attr name to getter and/or setter */
    private Map<String, AttrMethods<M>> attrMap = newInsertionOrderMap();

    private static class AttrMethods<M> {
        M getter;
        M setter;
    }

    /**
     * <p>Return an MBeanAnalyzer for the given MBean interface and
     * MBeanIntrospector.  Calling this method twice with the same
     * parameters may return the same object or two different but
     * equivalent objects.
     */
    // Currently it's two different but equivalent objects.  This only
    // really impacts proxy generation.  For MBean creation, the
    // cached PerInterface object for an MBean interface means that
    // an analyzer will not be recreated for a second MBean using the
    // same interface.
    static <M> MBeanAnalyzer<M> analyzer(Class<?> mbeanType,
            MBeanIntrospector<M> introspector)
            throws NotCompliantMBeanException {
        return new MBeanAnalyzer<M>(mbeanType, introspector);
    }

    private MBeanAnalyzer(Class<?> mbeanType,
            MBeanIntrospector<M> introspector)
            throws NotCompliantMBeanException {
        if (!mbeanType.isInterface()) {
            throw new NotCompliantMBeanException("Not an interface: " +
                    mbeanType.getName());
        } else if (!Modifier.isPublic(mbeanType.getModifiers()) &&
                   !Introspector.ALLOW_NONPUBLIC_MBEAN) {
            throw new NotCompliantMBeanException("Interface is not public: " +
                mbeanType.getName());
        }

        try {
            initMaps(mbeanType, introspector);
        } catch (Exception x) {
            throw Introspector.throwException(mbeanType,x);
        }
    }

    // Introspect the mbeanInterface and initialize this object's maps.
    //
    private void initMaps(Class<?> mbeanType,
            MBeanIntrospector<M> introspector) throws Exception {
        final List<Method> methods1 = introspector.getMethods(mbeanType);
        final List<Method> methods = eliminateCovariantMethods(methods1);

        /* Run through the methods to detect inconsistencies and to enable
           us to give getter and setter together to visitAttribute. */
        for (Method m : methods) {
            final String name = m.getName();
            final int nParams = m.getParameterTypes().length;

            final M cm = introspector.mFrom(m);

            String attrName = "";
            if (name.startsWith("get"))
                attrName = name.substring(3);
            else if (name.startsWith("is")
            && m.getReturnType() == boolean.class)
                attrName = name.substring(2);

            if (attrName.length() != 0 && nParams == 0
                    && m.getReturnType() != void.class) {
                // It's a getter
                // Check we don't have both isX and getX
                AttrMethods<M> am = attrMap.get(attrName);
                if (am == null)
                    am = new AttrMethods<M>();
                else {
                    if (am.getter != null) {
                        final String msg = "Attribute " + attrName +
                                " has more than one getter";
                        throw new NotCompliantMBeanException(msg);
                    }
                }
                am.getter = cm;
                attrMap.put(attrName, am);
            } else if (name.startsWith("set") && name.length() > 3
                    && nParams == 1 &&
                    m.getReturnType() == void.class) {
                // It's a setter
                attrName = name.substring(3);
                AttrMethods<M> am = attrMap.get(attrName);
                if (am == null)
                    am = new AttrMethods<M>();
                else if (am.setter != null) {
                    final String msg = "Attribute " + attrName +
                            " has more than one setter";
                    throw new NotCompliantMBeanException(msg);
                }
                am.setter = cm;
                attrMap.put(attrName, am);
            } else {
                // It's an operation
                List<M> cms = opMap.get(name);
                if (cms == null)
                    cms = newList();
                cms.add(cm);
                opMap.put(name, cms);
            }
        }
        /* Check that getters and setters are consistent. */
        for (Map.Entry<String, AttrMethods<M>> entry : attrMap.entrySet()) {
            AttrMethods<M> am = entry.getValue();
            if (!introspector.consistent(am.getter, am.setter)) {
                final String msg = "Getter and setter for " + entry.getKey() +
                        " have inconsistent types";
                throw new NotCompliantMBeanException(msg);
            }
        }
    }

    /**
     * A comparator that defines a total order so that methods have the
     * same name and identical signatures appear next to each others.
     * The methods are sorted in such a way that methods which
     * override each other will sit next to each other, with the
     * overridden method first - e.g. Object getFoo() is placed before
     * Integer getFoo(). This makes it possible to determine whether
     * a method overrides another one simply by looking at the method(s)
     * that precedes it in the list. (see eliminateCovariantMethods).
     **/
    private static class MethodOrder implements Comparator<Method> {
        public int compare(Method a, Method b) {
            final int cmp = a.getName().compareTo(b.getName());
            if (cmp != 0) return cmp;
            final Class<?>[] aparams = a.getParameterTypes();
            final Class<?>[] bparams = b.getParameterTypes();
            if (aparams.length != bparams.length)
                return aparams.length - bparams.length;
            if (!Arrays.equals(aparams, bparams)) {
                return Arrays.toString(aparams).
                        compareTo(Arrays.toString(bparams));
            }
            final Class<?> aret = a.getReturnType();
            final Class<?> bret = b.getReturnType();
            if (aret == bret) return 0;

            // Super type comes first: Object, Number, Integer
            if (aret.isAssignableFrom(bret))
                return -1;
            return +1;      // could assert bret.isAssignableFrom(aret)
        }
        public static final MethodOrder instance = new MethodOrder();
    }


    /* Eliminate methods that are overridden with a covariant return type.
       Reflection will return both the original and the overriding method
       but only the overriding one is of interest.  We return the methods
       in the same order they arrived in.  This isn't required by the spec
       but existing code may depend on it and users may be used to seeing
       operations or attributes appear in a particular order.

       Because of the way this method works, if the same Method appears
       more than once in the given List then it will be completely deleted!
       So don't do that.  */
    static List<Method>
            eliminateCovariantMethods(List<Method> startMethods) {
        // We are assuming that you never have very many methods with the
        // same name, so it is OK to use algorithms that are quadratic
        // in the number of methods with the same name.

        final int len = startMethods.size();
        final Method[] sorted = startMethods.toArray(new Method[len]);
        Arrays.sort(sorted,MethodOrder.instance);
        final Set<Method> overridden = newSet();
        for (int i=1;i<len;i++) {
            final Method m0 = sorted[i-1];
            final Method m1 = sorted[i];

            // Methods that don't have the same name can't override each other
            if (!m0.getName().equals(m1.getName())) continue;

            // Methods that have the same name and same signature override
            // each other. In that case, the second method overrides the first,
            // due to the way we have sorted them in MethodOrder.
            if (Arrays.equals(m0.getParameterTypes(),
                    m1.getParameterTypes())) {
                if (!overridden.add(m0))
                    throw new RuntimeException("Internal error: duplicate Method");
            }
        }

        final List<Method> methods = newList(startMethods);
        methods.removeAll(overridden);
        return methods;
    }


}
