/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.security.AccessController;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;

import static com.sun.jmx.mbeanserver.Util.*;

/**
 * Per-MBean-interface behavior.  A single instance of this class can be shared
 * by all MBeans of the same kind (Standard MBean or MXBean) that have the same
 * MBean interface.
 *
 * @since 1.6
 */
final class PerInterface<M> {
    PerInterface(Class<?> mbeanInterface, MBeanIntrospector<M> introspector,
                 MBeanAnalyzer<M> analyzer, MBeanInfo mbeanInfo) {
        this.mbeanInterface = mbeanInterface;
        this.introspector = introspector;
        this.mbeanInfo = mbeanInfo;
        analyzer.visit(new InitMaps());
    }

    Class<?> getMBeanInterface() {
        return mbeanInterface;
    }

    MBeanInfo getMBeanInfo() {
        return mbeanInfo;
    }

    boolean isMXBean() {
        return introspector.isMXBean();
    }

    Object getAttribute(Object resource, String attribute, Object cookie)
            throws AttributeNotFoundException,
                   MBeanException,
                   ReflectionException {

        final M cm = getters.get(attribute);
        if (cm == null) {
            final String msg;
            if (setters.containsKey(attribute))
                msg = "Write-only attribute: " + attribute;
            else
                msg = "No such attribute: " + attribute;
            throw new AttributeNotFoundException(msg);
        }
        return introspector.invokeM(cm, resource, (Object[]) null, cookie);
    }

    void setAttribute(Object resource, String attribute, Object value,
                      Object cookie)
            throws AttributeNotFoundException,
                   InvalidAttributeValueException,
                   MBeanException,
                   ReflectionException {

        final M cm = setters.get(attribute);
        if (cm == null) {
            final String msg;
            if (getters.containsKey(attribute))
                msg = "Read-only attribute: " + attribute;
            else
                msg = "No such attribute: " + attribute;
            throw new AttributeNotFoundException(msg);
        }
        introspector.invokeSetter(attribute, cm, resource, value, cookie);
    }

    Object invoke(Object resource, String operation, Object[] params,
                  String[] signature, Object cookie)
            throws MBeanException, ReflectionException {

        final List<MethodAndSig> list = ops.get(operation);
        if (list == null) {
            final String msg = "No such operation: " + operation;
            return noSuchMethod(msg, resource, operation, params, signature,
                                cookie);
        }
        if (signature == null)
            signature = new String[0];
        MethodAndSig found = null;
        for (MethodAndSig mas : list) {
            if (Arrays.equals(mas.signature, signature)) {
                found = mas;
                break;
            }
        }
        if (found == null) {
            final String badSig = sigString(signature);
            final String msg;
            if (list.size() == 1) {  // helpful exception message
                msg = "Signature mismatch for operation " + operation +
                        ": " + badSig + " should be " +
                        sigString(list.get(0).signature);
            } else {
                msg = "Operation " + operation + " exists but not with " +
                        "this signature: " + badSig;
            }
            return noSuchMethod(msg, resource, operation, params, signature,
                                cookie);
        }
        return introspector.invokeM(found.method, resource, params, cookie);
    }

    /*
     * This method is called when invoke doesn't find the named method.
     * Before throwing an exception, we check to see whether the
     * jmx.invoke.getters property is set, and if so whether the method
     * being invoked might be a getter or a setter.  If so we invoke it
     * and return the result.  This is for compatibility
     * with code based on JMX RI 1.0 or 1.1 which allowed invoking getters
     * and setters.  It is *not* recommended that new code use this feature.
     *
     * Since this method is either going to throw an exception or use
     * functionality that is strongly discouraged, we consider that its
     * performance is not very important.
     *
     * A simpler way to implement the functionality would be to add the getters
     * and setters to the operations map when jmx.invoke.getters is set.
     * However, that means that the property is consulted when an MBean
     * interface is being introspected and not thereafter.  Previously,
     * the property was consulted on every invocation.  So this simpler
     * implementation could potentially break code that sets and unsets
     * the property at different times.
     */
    @SuppressWarnings("removal")
    private Object noSuchMethod(String msg, Object resource, String operation,
                                Object[] params, String[] signature,
                                Object cookie)
            throws MBeanException, ReflectionException {

        // Construct the exception that we will probably throw
        final NoSuchMethodException nsme =
            new NoSuchMethodException(operation + sigString(signature));
        final ReflectionException exception =
            new ReflectionException(nsme, msg);

        if (introspector.isMXBean())
            throw exception; // No compatibility requirement here

        // Is the compatibility property set?
        GetPropertyAction act = new GetPropertyAction("jmx.invoke.getters");
        String invokeGettersS;
        try {
            invokeGettersS = AccessController.doPrivileged(act);
        } catch (Exception e) {
            // We don't expect an exception here but if we get one then
            // we'll simply assume that the property is not set.
            invokeGettersS = null;
        }
        if (invokeGettersS == null)
            throw exception;

        int rest = 0;
        Map<String, M> methods = null;
        if (signature == null || signature.length == 0) {
            if (operation.startsWith("get"))
                rest = 3;
            else if (operation.startsWith("is"))
                rest = 2;
            if (rest != 0)
                methods = getters;
        } else if (signature.length == 1 &&
                   operation.startsWith("set")) {
            rest = 3;
            methods = setters;
        }

        if (rest != 0) {
            String attrName = operation.substring(rest);
            M method = methods.get(attrName);
            if (method != null && introspector.getName(method).equals(operation)) {
                String[] msig = introspector.getSignature(method);
                if ((signature == null && msig.length == 0) ||
                        Arrays.equals(signature, msig)) {
                    return introspector.invokeM(method, resource, params, cookie);
                }
            }
        }

        throw exception;
    }

    private String sigString(String[] signature) {
        StringBuilder b = new StringBuilder("(");
        if (signature != null) {
            for (String s : signature) {
                if (b.length() > 1)
                    b.append(", ");
                b.append(s);
            }
        }
        return b.append(")").toString();
    }

    /**
     * Visitor that sets up the method maps (operations, getters, setters).
     */
    private class InitMaps implements MBeanAnalyzer.MBeanVisitor<M> {
        public void visitAttribute(String attributeName,
                                   M getter,
                                   M setter) {
            if (getter != null) {
                introspector.checkMethod(getter);
                final Object old = getters.put(attributeName, getter);
                assert(old == null);
            }
            if (setter != null) {
                introspector.checkMethod(setter);
                final Object old = setters.put(attributeName, setter);
                assert(old == null);
            }
        }

        public void visitOperation(String operationName,
                                   M operation) {
            introspector.checkMethod(operation);
            final String[] sig = introspector.getSignature(operation);
            final MethodAndSig mas = new MethodAndSig();
            mas.method = operation;
            mas.signature = sig;
            List<MethodAndSig> list = ops.get(operationName);
            if (list == null)
                list = Collections.singletonList(mas);
            else {
                if (list.size() == 1)
                    list = newList(list);
                list.add(mas);
            }
            ops.put(operationName, list);
        }
    }

    private class MethodAndSig {
        M method;
        String[] signature;
    }

    private final Class<?> mbeanInterface;
    private final MBeanIntrospector<M> introspector;
    private final MBeanInfo mbeanInfo;
    private final Map<String, M> getters = newMap();
    private final Map<String, M> setters = newMap();
    private final Map<String, List<MethodAndSig>> ops = newMap();
}
