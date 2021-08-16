/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.beans.decoder;

import com.sun.beans.finder.MethodFinder;

import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import sun.reflect.misc.MethodUtil;

/**
 * This class is intended to handle &lt;property&gt; element.
 * This element simplifies access to the properties.
 * If the {@code index} attribute is specified
 * this element uses additional {@code int} parameter.
 * If the {@code name} attribute is not specified
 * this element uses method "get" as getter
 * and method "set" as setter.
 * This element defines getter if it contains no argument.
 * It returns the value of the property in this case.
 * For example:<pre>
 * &lt;property name="object" index="10"/&gt;</pre>
 * is shortcut to<pre>
 * &lt;method name="getObject"&gt;
 *     &lt;int&gt;10&lt;/int&gt;
 * &lt;/method&gt;</pre>
 * which is equivalent to {@code getObject(10)} in Java code.
 * This element defines setter if it contains one argument.
 * It does not return the value of the property in this case.
 * For example:<pre>
 * &lt;property&gt;&lt;int&gt;0&lt;/int&gt;&lt;/property&gt;</pre>
 * is shortcut to<pre>
 * &lt;method name="set"&gt;
 *     &lt;int&gt;0&lt;/int&gt;
 * &lt;/method&gt;</pre>
 * which is equivalent to {@code set(0)} in Java code.
 * <p>The following attributes are supported:
 * <dl>
 * <dt>name
 * <dd>the property name
 * <dt>index
 * <dd>the property index
 * <dt>id
 * <dd>the identifier of the variable that is intended to store the result
 * </dl>
 *
 * @since 1.7
 *
 * @author Sergey A. Malenkov
 */
final class PropertyElementHandler extends AccessorElementHandler {
    static final String GETTER = "get"; // NON-NLS: the getter prefix
    static final String SETTER = "set"; // NON-NLS: the setter prefix

    private Integer index;

    /**
     * Parses attributes of the element.
     * The following attributes are supported:
     * <dl>
     * <dt>name
     * <dd>the property name
     * <dt>index
     * <dd>the property index
     * <dt>id
     * <dd>the identifier of the variable that is intended to store the result
     * </dl>
     *
     * @param name   the attribute name
     * @param value  the attribute value
     */
    @Override
    public void addAttribute(String name, String value) {
        if (name.equals("index")) { // NON-NLS: the attribute name
            this.index = Integer.valueOf(value);
        } else {
            super.addAttribute(name, value);
        }
    }

    /**
     * Tests whether the value of this element can be used
     * as an argument of the element that contained in this one.
     *
     * @return {@code true} if the value of this element should be used
     *         as an argument of the element that contained in this one,
     *         {@code false} otherwise
     */
    @Override
    protected boolean isArgument() {
        return false; // non-static accessor cannot be used an argument
    }

    /**
     * Returns the value of the property with specified {@code name}.
     *
     * @param name  the name of the property
     * @return the value of the specified property
     */
    @Override
    protected Object getValue(String name) {
        try {
            return getPropertyValue(getContextBean(), name, this.index);
        }
        catch (Exception exception) {
            getOwner().handleException(exception);
        }
        return null;
    }

    /**
     * Sets the new value for the property with specified {@code name}.
     *
     * @param name   the name of the property
     * @param value  the new value for the specified property
     */
    @Override
    protected void setValue(String name, Object value) {
        try {
            setPropertyValue(getContextBean(), name, this.index, value);
        }
        catch (Exception exception) {
            getOwner().handleException(exception);
        }
    }

    /**
     * Performs the search of the getter for the property
     * with specified {@code name} in specified class
     * and returns value of the property.
     *
     * @param bean   the context bean that contains property
     * @param name   the name of the property
     * @param index  the index of the indexed property
     * @return the value of the property
     * @throws IllegalAccessException    if the property is not accesible
     * @throws IntrospectionException    if the bean introspection is failed
     * @throws InvocationTargetException if the getter cannot be invoked
     * @throws NoSuchMethodException     if the getter is not found
     */
    private static Object getPropertyValue(Object bean, String name, Integer index) throws IllegalAccessException, IntrospectionException, InvocationTargetException, NoSuchMethodException {
        Class<?> type = bean.getClass();
        if (index == null) {
            return MethodUtil.invoke(findGetter(type, name), bean, new Object[] {});
        } else if (type.isArray() && (name == null)) {
            return Array.get(bean, index);
        } else {
            return MethodUtil.invoke(findGetter(type, name, int.class), bean, new Object[] {index});
        }
    }

    /**
     * Performs the search of the setter for the property
     * with specified {@code name} in specified class
     * and updates value of the property.
     *
     * @param bean   the context bean that contains property
     * @param name   the name of the property
     * @param index  the index of the indexed property
     * @param value  the new value for the property
     * @throws IllegalAccessException    if the property is not accesible
     * @throws IntrospectionException    if the bean introspection is failed
     * @throws InvocationTargetException if the setter cannot be invoked
     * @throws NoSuchMethodException     if the setter is not found
     */
    private static void setPropertyValue(Object bean, String name, Integer index, Object value) throws IllegalAccessException, IntrospectionException, InvocationTargetException, NoSuchMethodException {
        Class<?> type = bean.getClass();
        Class<?> param = (value != null)
                ? value.getClass()
                : null;

        if (index == null) {
            MethodUtil.invoke(findSetter(type, name, param), bean, new Object[] {value});
        } else if (type.isArray() && (name == null)) {
            Array.set(bean, index, value);
        } else {
            MethodUtil.invoke(findSetter(type, name, int.class, param), bean, new Object[] {index, value});
        }
    }

    /**
     * Performs the search of the getter for the property
     * with specified {@code name} in specified class.
     *
     * @param type  the class that contains method
     * @param name  the name of the property
     * @param args  the method arguments
     * @return method object that represents found getter
     * @throws IntrospectionException if the bean introspection is failed
     * @throws NoSuchMethodException  if method is not found
     */
    private static Method findGetter(Class<?> type, String name, Class<?>...args) throws IntrospectionException, NoSuchMethodException {
        if (name == null) {
            return MethodFinder.findInstanceMethod(type, GETTER, args);
        }
        PropertyDescriptor pd = getProperty(type, name);
        if (args.length == 0) {
            Method method = pd.getReadMethod();
            if (method != null) {
                return method;
            }
        } else if (pd instanceof IndexedPropertyDescriptor) {
            IndexedPropertyDescriptor ipd = (IndexedPropertyDescriptor) pd;
            Method method = ipd.getIndexedReadMethod();
            if (method != null) {
                return method;
            }
        }
        throw new IntrospectionException("Could not find getter for the " + name + " property");
    }

    /**
     * Performs the search of the setter for the property
     * with specified {@code name} in specified class.
     *
     * @param type  the class that contains method
     * @param name  the name of the property
     * @param args  the method arguments
     * @return method object that represents found setter
     * @throws IntrospectionException if the bean introspection is failed
     * @throws NoSuchMethodException  if method is not found
     */
    private static Method findSetter(Class<?> type, String name, Class<?>...args) throws IntrospectionException, NoSuchMethodException {
        if (name == null) {
            return MethodFinder.findInstanceMethod(type, SETTER, args);
        }
        PropertyDescriptor pd = getProperty(type, name);
        if (args.length == 1) {
            Method method = pd.getWriteMethod();
            if (method != null) {
                return method;
            }
        } else if (pd instanceof IndexedPropertyDescriptor) {
            IndexedPropertyDescriptor ipd = (IndexedPropertyDescriptor) pd;
            Method method = ipd.getIndexedWriteMethod();
            if (method != null) {
                return method;
            }
        }
        throw new IntrospectionException("Could not find setter for the " + name + " property");
    }

    /**
     * Performs the search of the descriptor for the property
     * with specified {@code name} in specified class.
     *
     * @param type  the class to introspect
     * @param name  the property name
     * @return descriptor for the named property
     * @throws IntrospectionException if property descriptor is not found
     */
    private static PropertyDescriptor getProperty(Class<?> type, String name) throws IntrospectionException {
        for (PropertyDescriptor pd : Introspector.getBeanInfo(type).getPropertyDescriptors()) {
            if (name.equals(pd.getName())) {
                return pd;
            }
        }
        throw new IntrospectionException("Could not find the " + name + " property descriptor");
    }
}
