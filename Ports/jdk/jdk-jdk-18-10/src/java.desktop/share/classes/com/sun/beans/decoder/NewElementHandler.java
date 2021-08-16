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

import com.sun.beans.finder.ConstructorFinder;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is intended to handle &lt;new&gt; element.
 * It describes instantiation of the object.
 * The {@code class} attribute denotes
 * the name of the class to instantiate.
 * The inner elements specifies the arguments of the constructor.
 * For example:<pre>
 * &lt;new class="java.lang.Long"&gt;
 *     &lt;string&gt;10&lt;/string&gt;
 * &lt;/new&gt;</pre>
 * is equivalent to {@code Long.valueOf("10")} in Java code.
 * <p>The following attributes are supported:
 * <dl>
 * <dt>class
 * <dd>the type of object for instantiation
 * <dt>id
 * <dd>the identifier of the variable that is intended to store the result
 * </dl>
 *
 * @since 1.7
 *
 * @author Sergey A. Malenkov
 */
class NewElementHandler extends ElementHandler {
    private List<Object> arguments = new ArrayList<Object>();
    private ValueObject value = ValueObjectImpl.VOID;

    private Class<?> type;

    /**
     * Parses attributes of the element.
     * The following attributes are supported:
     * <dl>
     * <dt>class
     * <dd>the type of object for instantiation
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
     * Adds the argument to the list of arguments
     * that is used to calculate the value of this element.
     *
     * @param argument  the value of the element that contained in this one
     */
    @Override
    protected final void addArgument(Object argument) {
        if (this.arguments == null) {
            throw new IllegalStateException("Could not add argument to evaluated element");
        }
        this.arguments.add(argument);
    }

    /**
     * Returns the context of the method.
     * The context of the static method is the class object.
     * The context of the non-static method is the value of the parent element.
     *
     * @return the context of the method
     */
    @Override
    protected final Object getContextBean() {
        return (this.type != null)
                ? this.type
                : super.getContextBean();
    }

    /**
     * Returns the value of this element.
     *
     * @return the value of this element
     */
    @Override
    protected final ValueObject getValueObject() {
        if (this.arguments != null) {
            try {
                this.value = getValueObject(this.type, this.arguments.toArray());
            }
            catch (Exception exception) {
                getOwner().handleException(exception);
            }
            finally {
                this.arguments = null;
            }
        }
        return this.value;
    }

    /**
     * Calculates the value of this element
     * using the base class and the array of arguments.
     * By default, it creates an instance of the base class.
     * This method should be overridden in those handlers
     * that extend behavior of this element.
     *
     * @param type  the base class
     * @param args  the array of arguments
     * @return the value of this element
     * @throws Exception if calculation is failed
     */
    ValueObject getValueObject(Class<?> type, Object[] args) throws Exception {
        if (type == null) {
            throw new IllegalArgumentException("Class name is not set");
        }
        Class<?>[] types = getArgumentTypes(args);
        Constructor<?> constructor = ConstructorFinder.findConstructor(type, types);
        if (constructor.isVarArgs()) {
            args = getArguments(args, constructor.getParameterTypes());
        }
        return ValueObjectImpl.create(constructor.newInstance(args));
    }

    /**
     * Converts the array of arguments to the array of corresponding classes.
     * If argument is {@code null} the class is {@code null} too.
     *
     * @param arguments  the array of arguments
     * @return the array of corresponding classes
     */
    static Class<?>[] getArgumentTypes(Object[] arguments) {
        Class<?>[] types = new Class<?>[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] != null) {
                types[i] = arguments[i].getClass();
            }
        }
        return types;
    }

    /**
     * Resolves variable arguments.
     *
     * @param arguments  the array of arguments
     * @param types      the array of parameter types
     * @return the resolved array of arguments
     */
    static Object[] getArguments(Object[] arguments, Class<?>[] types) {
        int index = types.length - 1;
        if (types.length == arguments.length) {
            Object argument = arguments[index];
            if (argument == null) {
                return arguments;
            }
            Class<?> type = types[index];
            if (type.isAssignableFrom(argument.getClass())) {
                return arguments;
            }
        }
        int length = arguments.length - index;
        Class<?> type = types[index].getComponentType();
        Object array = Array.newInstance(type, length);
        System.arraycopy(arguments, index, array, 0, length);

        Object[] args = new Object[types.length];
        System.arraycopy(arguments, 0, args, 0, index);
        args[index] = array;
        return args;
    }
}
