/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import com.sun.beans.finder.ClassFinder;
import com.sun.beans.finder.ConstructorFinder;
import com.sun.beans.finder.MethodFinder;
import sun.reflect.misc.MethodUtil;

import static sun.reflect.misc.ReflectUtil.checkPackageAccess;

/**
 * A {@code Statement} object represents a primitive statement
 * in which a single method is applied to a target and
 * a set of arguments - as in {@code "a.setFoo(b)"}.
 * Note that where this example uses names
 * to denote the target and its argument, a statement
 * object does not require a name space and is constructed with
 * the values themselves.
 * The statement object associates the named method
 * with its environment as a simple set of values:
 * the target and an array of argument values.
 *
 * @since 1.4
 *
 * @author Philip Milne
 */
public class Statement {

    private static Object[] emptyArray = new Object[]{};

    static ExceptionListener defaultExceptionListener = new ExceptionListener() {
        public void exceptionThrown(Exception e) {
            System.err.println(e);
            // e.printStackTrace();
            System.err.println("Continuing ...");
        }
    };

    @SuppressWarnings("removal")
    private final AccessControlContext acc = AccessController.getContext();
    private final Object target;
    private final String methodName;
    private final Object[] arguments;
    ClassLoader loader;

    /**
     * Creates a new {@link Statement} object
     * for the specified target object to invoke the method
     * specified by the name and by the array of arguments.
     * <p>
     * The {@code target} and the {@code methodName} values should not be {@code null}.
     * Otherwise an attempt to execute this {@code Expression}
     * will result in a {@code NullPointerException}.
     * If the {@code arguments} value is {@code null},
     * an empty array is used as the value of the {@code arguments} property.
     *
     * @param target  the target object of this statement
     * @param methodName  the name of the method to invoke on the specified target
     * @param arguments  the array of arguments to invoke the specified method
     */
    @ConstructorProperties({"target", "methodName", "arguments"})
    public Statement(Object target, String methodName, Object[] arguments) {
        this.target = target;
        this.methodName = methodName;
        this.arguments = (arguments == null) ? emptyArray : arguments.clone();
    }

    /**
     * Returns the target object of this statement.
     * If this method returns {@code null},
     * the {@link #execute} method
     * throws a {@code NullPointerException}.
     *
     * @return the target object of this statement
     */
    public Object getTarget() {
        return target;
    }

    /**
     * Returns the name of the method to invoke.
     * If this method returns {@code null},
     * the {@link #execute} method
     * throws a {@code NullPointerException}.
     *
     * @return the name of the method
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Returns the arguments for the method to invoke.
     * The number of arguments and their types
     * must match the method being  called.
     * {@code null} can be used as a synonym of an empty array.
     *
     * @return the array of arguments
     */
    public Object[] getArguments() {
        return this.arguments.clone();
    }

    /**
     * The {@code execute} method finds a method whose name is the same
     * as the {@code methodName} property, and invokes the method on
     * the target.
     *
     * When the target's class defines many methods with the given name
     * the implementation should choose the most specific method using
     * the algorithm specified in the Java Language Specification
     * (15.11). The dynamic class of the target and arguments are used
     * in place of the compile-time type information and, like the
     * {@link java.lang.reflect.Method} class itself, conversion between
     * primitive values and their associated wrapper classes is handled
     * internally.
     * <p>
     * The following method types are handled as special cases:
     * <ul>
     * <li>
     * Static methods may be called by using a class object as the target.
     * <li>
     * The reserved method name "new" may be used to call a class's constructor
     * as if all classes defined static "new" methods. Constructor invocations
     * are typically considered {@code Expression}s rather than {@code Statement}s
     * as they return a value.
     * <li>
     * The method names "get" and "set" defined in the {@link java.util.List}
     * interface may also be applied to array instances, mapping to
     * the static methods of the same name in the {@code Array} class.
     * </ul>
     *
     * @throws NullPointerException if the value of the {@code target} or
     *                              {@code methodName} property is {@code null}
     * @throws NoSuchMethodException if a matching method is not found
     * @throws SecurityException if a security manager exists and
     *                           it denies the method invocation
     * @throws Exception that is thrown by the invoked method
     *
     * @see java.lang.reflect.Method
     */
    public void execute() throws Exception {
        invoke();
    }

    @SuppressWarnings("removal")
    Object invoke() throws Exception {
        AccessControlContext acc = this.acc;
        if ((acc == null) && (System.getSecurityManager() != null)) {
            throw new SecurityException("AccessControlContext is not set");
        }
        try {
            return AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Object>() {
                        public Object run() throws Exception {
                            return invokeInternal();
                        }
                    },
                    acc
            );
        }
        catch (PrivilegedActionException exception) {
            throw exception.getException();
        }
    }

    private Object invokeInternal() throws Exception {
        Object target = getTarget();
        String methodName = getMethodName();

        if (target == null || methodName == null) {
            throw new NullPointerException((target == null ? "target" :
                                            "methodName") + " should not be null");
        }

        Object[] arguments = getArguments();
        if (arguments == null) {
            arguments = emptyArray;
        } else {
            arguments = arguments.clone();
        }
        if (target == Class.class && methodName.equals("forName")) {
            final String name = (String) arguments[0];
            if (arguments.length == 1) {
                // Class.forName(String className) won't load classes outside
                // of core from a class inside core. Special
                // case this method.
                // checkPackageAccess(name) will be called by ClassFinder
                return ClassFinder.resolveClass(name, this.loader);
            }
            // The 3 args Class.forName(String className, boolean, classloader)
            // requires getClassLoader permission, but we will be stricter and
            // will require access to the package as well.
            checkPackageAccess(name);
        }
        Class<?>[] argClasses = new Class<?>[arguments.length];
        for(int i = 0; i < arguments.length; i++) {
            argClasses[i] = (arguments[i] == null) ? null : arguments[i].getClass();
        }

        AccessibleObject m = null;
        if (target instanceof Class) {
            /*
            For class methods, simluate the effect of a meta class
            by taking the union of the static methods of the
            actual class, with the instance methods of "Class.class"
            and the overloaded "newInstance" methods defined by the
            constructors.
            This way "System.class", for example, will perform both
            the static method getProperties() and the instance method
            getSuperclass() defined in "Class.class".
            */
            if (methodName.equals("new")) {
                methodName = "newInstance";
            }
            // Provide a short form for array instantiation by faking an nary-constructor.
            if (methodName.equals("newInstance") && ((Class)target).isArray()) {
                Object result = Array.newInstance(((Class)target).getComponentType(), arguments.length);
                for(int i = 0; i < arguments.length; i++) {
                    Array.set(result, i, arguments[i]);
                }
                return result;
            }
            if (methodName.equals("newInstance") && arguments.length != 0) {
                // The Character class, as of 1.4, does not have a constructor
                // which takes a String. All of the other "wrapper" classes
                // for Java's primitive types have a String constructor so we
                // fake such a constructor here so that this special case can be
                // ignored elsewhere.
                if (target == Character.class && arguments.length == 1 &&
                    argClasses[0] == String.class) {
                    return ((String)arguments[0]).charAt(0);
                }
                try {
                    m = ConstructorFinder.findConstructor((Class)target, argClasses);
                }
                catch (NoSuchMethodException exception) {
                    m = null;
                }
            }
            if (m == null && target != Class.class) {
                m = getMethod((Class)target, methodName, argClasses);
            }
            if (m == null) {
                m = getMethod(Class.class, methodName, argClasses);
            }
        }
        else {
            /*
            This special casing of arrays is not necessary, but makes files
            involving arrays much shorter and simplifies the archiving infrastrcure.
            The Array.set() method introduces an unusual idea - that of a static method
            changing the state of an instance. Normally statements with side
            effects on objects are instance methods of the objects themselves
            and we reinstate this rule (perhaps temporarily) by special-casing arrays.
            */
            if (target.getClass().isArray() &&
                (methodName.equals("set") || methodName.equals("get"))) {
                int index = ((Integer)arguments[0]).intValue();
                if (methodName.equals("get")) {
                    return Array.get(target, index);
                }
                else {
                    Array.set(target, index, arguments[1]);
                    return null;
                }
            }
            m = getMethod(target.getClass(), methodName, argClasses);
        }
        if (m != null) {
            try {
                if (m instanceof Method) {
                    return MethodUtil.invoke((Method)m, target, arguments);
                }
                else {
                    return ((Constructor)m).newInstance(arguments);
                }
            }
            catch (IllegalAccessException iae) {
                throw new Exception("Statement cannot invoke: " +
                                    methodName + " on " + target.getClass(),
                                    iae);
            }
            catch (InvocationTargetException ite) {
                Throwable te = ite.getCause();
                if (te instanceof Exception) {
                    throw (Exception)te;
                }
                else {
                    throw ite;
                }
            }
        }
        throw new NoSuchMethodException(toString());
    }

    String instanceName(Object instance) {
        if (instance == null) {
            return "null";
        } else if (instance.getClass() == String.class) {
            return "\""+(String)instance + "\"";
        } else {
            // Note: there is a minor problem with using the non-caching
            // NameGenerator method. The return value will not have
            // specific information about the inner class name. For example,
            // In 1.4.2 an inner class would be represented as JList$1 now
            // would be named Class.

            return NameGenerator.unqualifiedClassName(instance.getClass());
        }
    }

    /**
     * Prints the value of this statement using a Java-style syntax.
     */
    public String toString() {
        // Respect a subclass's implementation here.
        Object target = getTarget();
        String methodName = getMethodName();
        Object[] arguments = getArguments();
        if (arguments == null) {
            arguments = emptyArray;
        }
        StringBuilder result = new StringBuilder(instanceName(target) + "." + methodName + "(");
        int n = arguments.length;
        for(int i = 0; i < n; i++) {
            result.append(instanceName(arguments[i]));
            if (i != n -1) {
                result.append(", ");
            }
        }
        result.append(");");
        return result.toString();
    }

    static Method getMethod(Class<?> type, String name, Class<?>... args) {
        try {
            return MethodFinder.findMethod(type, name, args);
        }
        catch (NoSuchMethodException exception) {
            return null;
        }
    }
}
