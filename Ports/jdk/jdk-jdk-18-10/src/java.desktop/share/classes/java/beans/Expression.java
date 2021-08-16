/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.beans;

/**
 * An {@code Expression} object represents a primitive expression
 * in which a single method is applied to a target and a set of
 * arguments to return a result - as in {@code "a.getFoo()"}.
 * <p>
 * In addition to the properties of the super class, the
 * {@code Expression} object provides a <em>value</em> which
 * is the object returned when this expression is evaluated.
 * The return value is typically not provided by the caller and
 * is instead computed by dynamically finding the method and invoking
 * it when the first call to {@code getValue} is made.
 *
 * @see #getValue
 * @see #setValue
 *
 * @since 1.4
 *
 * @author Philip Milne
 */
public class Expression extends Statement {

    private static Object unbound = new Object();

    private Object value = unbound;

    /**
     * Creates a new {@link Expression} object
     * for the specified target object to invoke the method
     * specified by the name and by the array of arguments.
     * <p>
     * The {@code target} and the {@code methodName} values should not be {@code null}.
     * Otherwise an attempt to execute this {@code Expression}
     * will result in a {@code NullPointerException}.
     * If the {@code arguments} value is {@code null},
     * an empty array is used as the value of the {@code arguments} property.
     *
     * @param target  the target object of this expression
     * @param methodName  the name of the method to invoke on the specified target
     * @param arguments  the array of arguments to invoke the specified method
     *
     * @see #getValue
     */
    @ConstructorProperties({"target", "methodName", "arguments"})
    public Expression(Object target, String methodName, Object[] arguments) {
        super(target, methodName, arguments);
    }

    /**
     * Creates a new {@link Expression} object with the specified value
     * for the specified target object to invoke the  method
     * specified by the name and by the array of arguments.
     * The {@code value} value is used as the value of the {@code value} property,
     * so the {@link #getValue} method will return it
     * without executing this {@code Expression}.
     * <p>
     * The {@code target} and the {@code methodName} values should not be {@code null}.
     * Otherwise an attempt to execute this {@code Expression}
     * will result in a {@code NullPointerException}.
     * If the {@code arguments} value is {@code null},
     * an empty array is used as the value of the {@code arguments} property.
     *
     * @param value  the value of this expression
     * @param target  the target object of this expression
     * @param methodName  the name of the method to invoke on the specified target
     * @param arguments  the array of arguments to invoke the specified method
     *
     * @see #setValue
     */
    public Expression(Object value, Object target, String methodName, Object[] arguments) {
        this(target, methodName, arguments);
        setValue(value);
    }

    /**
     * {@inheritDoc}
     * <p>
     * If the invoked method completes normally,
     * the value it returns is copied in the {@code value} property.
     * Note that the {@code value} property is set to {@code null},
     * if the return type of the underlying method is {@code void}.
     *
     * @throws NullPointerException if the value of the {@code target} or
     *                              {@code methodName} property is {@code null}
     * @throws NoSuchMethodException if a matching method is not found
     * @throws SecurityException if a security manager exists and
     *                           it denies the method invocation
     * @throws Exception that is thrown by the invoked method
     *
     * @see java.lang.reflect.Method
     * @since 1.7
     */
    @Override
    public void execute() throws Exception {
        setValue(invoke());
    }

    /**
     * If the value property of this instance is not already set,
     * this method dynamically finds the method with the specified
     * methodName on this target with these arguments and calls it.
     * The result of the method invocation is first copied
     * into the value property of this expression and then returned
     * as the result of {@code getValue}. If the value property
     * was already set, either by a call to {@code setValue}
     * or a previous call to {@code getValue} then the value
     * property is returned without either looking up or calling the method.
     * <p>
     * The value property of an {@code Expression} is set to
     * a unique private (non-{@code null}) value by default and
     * this value is used as an internal indication that the method
     * has not yet been called. A return value of {@code null}
     * replaces this default value in the same way that any other value
     * would, ensuring that expressions are never evaluated more than once.
     * <p>
     * See the {@code execute} method for details on how
     * methods are chosen using the dynamic types of the target
     * and arguments.
     *
     * @see Statement#execute
     * @see #setValue
     *
     * @return The result of applying this method to these arguments.
     * @throws Exception if the method with the specified methodName
     * throws an exception
     */
    public Object getValue() throws Exception {
        if (value == unbound) {
            setValue(invoke());
        }
        return value;
    }

    /**
     * Sets the value of this expression to {@code value}.
     * This value will be returned by the getValue method
     * without calling the method associated with this
     * expression.
     *
     * @param value The value of this expression.
     *
     * @see #getValue
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /*pp*/ String instanceName(Object instance) {
        return instance == unbound ? "<unbound>" : super.instanceName(instance);
    }

    /**
     * Prints the value of this expression using a Java-style syntax.
     */
    public String toString() {
        return instanceName(value) + "=" + super.toString();
    }
}
