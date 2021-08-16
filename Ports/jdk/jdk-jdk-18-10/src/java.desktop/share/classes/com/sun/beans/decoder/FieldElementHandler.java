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

import com.sun.beans.finder.FieldFinder;

import java.lang.reflect.Field;

/**
 * This class is intended to handle &lt;field&gt; element.
 * This element simplifies access to the fields.
 * If the {@code class} attribute is specified
 * this element accesses static field of specified class.
 * This element defines getter if it contains no argument.
 * It returns the value of the field in this case.
 * For example:<pre>
 * &lt;field name="TYPE" class="java.lang.Long"/&gt;</pre>
 * is equivalent to {@code Long.TYPE} in Java code.
 * This element defines setter if it contains one argument.
 * It does not return the value of the field in this case.
 * For example:<pre>
 * &lt;field name="id"&gt;&lt;int&gt;0&lt;/int&gt;&lt;/field&gt;</pre>
 * is equivalent to {@code id = 0} in Java code.
 * <p>The following attributes are supported:
 * <dl>
 * <dt>name
 * <dd>the field name
 * <dt>class
 * <dd>the type is used for static fields only
 * <dt>id
 * <dd>the identifier of the variable that is intended to store the result
 * </dl>
 *
 * @since 1.7
 *
 * @author Sergey A. Malenkov
 */
final class FieldElementHandler extends AccessorElementHandler {
    private Class<?> type;

    /**
     * Parses attributes of the element.
     * The following attributes are supported:
     * <dl>
     * <dt>name
     * <dd>the field name
     * <dt>class
     * <dd>the type is used for static fields only
     * <dt>id
     * <dd>the identifier of the variable that is intended to store the result
     * </dl>
     *
     * @param name   the attribute name
     * @param value  the attribute value
     */
    @Override
    public void addAttribute(String name, String value) {
        if (name.equals("class")) { // NON-NLS: the attribute name
            this.type = getOwner().findClass(value);
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
        return super.isArgument() && (this.type != null); // only static accessor can be used an argument
    }

    /**
     * Returns the context of the field.
     * The context of the static field is the class object.
     * The context of the non-static field is the value of the parent element.
     *
     * @return the context of the field
     */
    @Override
    protected Object getContextBean() {
        return (this.type != null)
                ? this.type
                : super.getContextBean();
    }

    /**
     * Returns the value of the field with specified {@code name}.
     *
     * @param name  the name of the field
     * @return the value of the specified field
     */
    @Override
    protected Object getValue(String name) {
        try {
            return getFieldValue(getContextBean(), name);
        }
        catch (Exception exception) {
            getOwner().handleException(exception);
        }
        return null;
    }

    /**
     * Sets the new value for the field with specified {@code name}.
     *
     * @param name   the name of the field
     * @param value  the new value for the specified field
     */
    @Override
    protected void setValue(String name, Object value) {
        try {
            setFieldValue(getContextBean(), name, value);
        }
        catch (Exception exception) {
            getOwner().handleException(exception);
        }
    }

    /**
     * Performs the search of the field with specified {@code name}
     * in specified context and returns its value.
     *
     * @param bean  the context bean that contains field
     * @param name  the name of the field
     * @return the value of the field
     * @throws IllegalAccessException if the field is not accesible
     * @throws NoSuchFieldException   if the field is not found
     */
    static Object getFieldValue(Object bean, String name) throws IllegalAccessException, NoSuchFieldException {
        return findField(bean, name).get(bean);
    }

    /**
     * Performs the search of the field with specified {@code name}
     * in specified context and updates its value.
     *
     * @param bean   the context bean that contains field
     * @param name   the name of the field
     * @param value  the new value for the field
     * @throws IllegalAccessException if the field is not accesible
     * @throws NoSuchFieldException   if the field is not found
     */
    private static void setFieldValue(Object bean, String name, Object value) throws IllegalAccessException, NoSuchFieldException {
        findField(bean, name).set(bean, value);
    }

    /**
     * Performs the search of the field
     * with specified {@code name} in specified context.
     *
     * @param bean  the context bean that contains field
     * @param name  the name of the field
     * @return field object that represents found field
     * @throws NoSuchFieldException if the field is not found
     */
    private static Field findField(Object bean, String name) throws NoSuchFieldException {
        return (bean instanceof Class<?>)
                ? FieldFinder.findStaticField((Class<?>) bean, name)
                : FieldFinder.findField(bean.getClass(), name);
    }
}
