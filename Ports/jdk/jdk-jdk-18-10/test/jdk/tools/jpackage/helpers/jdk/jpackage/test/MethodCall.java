/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package jdk.jpackage.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.jpackage.test.Functional.ThrowingConsumer;
import jdk.jpackage.test.TestInstance.TestDesc;

class MethodCall implements ThrowingConsumer {

    MethodCall(Object[] instanceCtorArgs, Method method) {
        this.ctorArgs = Optional.ofNullable(instanceCtorArgs).orElse(
                DEFAULT_CTOR_ARGS);
        this.method = method;
        this.methodArgs = new Object[0];
    }

    MethodCall(Object[] instanceCtorArgs, Method method, Object arg) {
        this.ctorArgs = Optional.ofNullable(instanceCtorArgs).orElse(
                DEFAULT_CTOR_ARGS);
        this.method = method;
        this.methodArgs = new Object[]{arg};
    }

    TestDesc createDescription() {
        var descBuilder = TestDesc.createBuilder().method(method);
        if (methodArgs.length != 0) {
            descBuilder.methodArgs(methodArgs);
        }

        if (ctorArgs.length != 0) {
            descBuilder.ctorArgs(ctorArgs);
        }

        return descBuilder.get();
    }

    Method getMethod() {
        return method;
    }

    Object newInstance() throws NoSuchMethodException, InstantiationException,
            IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        if ((method.getModifiers() & Modifier.STATIC) != 0) {
            return null;
        }

        Constructor ctor = findRequiredConstructor(method.getDeclaringClass(),
                ctorArgs);
        if (ctor.isVarArgs()) {
            // Assume constructor doesn't have fixed, only variable parameters.
            return ctor.newInstance(new Object[]{ctorArgs});
        }

        return ctor.newInstance(ctorArgs);
    }

    void checkRequiredConstructor() throws NoSuchMethodException {
        if ((method.getModifiers() & Modifier.STATIC) == 0) {
            findRequiredConstructor(method.getDeclaringClass(), ctorArgs);
        }
    }

    private static Constructor findVarArgConstructor(Class type) {
        return Stream.of(type.getConstructors()).filter(
                Constructor::isVarArgs).findFirst().orElse(null);
    }

    private Constructor findRequiredConstructor(Class type, Object... ctorArgs)
            throws NoSuchMethodException {

        Supplier<NoSuchMethodException> notFoundException = () -> {
            return new NoSuchMethodException(String.format(
                    "No public contructor in %s for %s arguments", type,
                    Arrays.deepToString(ctorArgs)));
        };

        if (Stream.of(ctorArgs).allMatch(Objects::nonNull)) {
            // No `null` in constructor args, take easy path
            try {
                return type.getConstructor(Stream.of(ctorArgs).map(
                        Object::getClass).collect(Collectors.toList()).toArray(
                        Class[]::new));
            } catch (NoSuchMethodException ex) {
                // Failed to find ctor that can take the given arguments.
                Constructor varArgCtor = findVarArgConstructor(type);
                if (varArgCtor != null) {
                    // There is one with variable number of arguments. Use it.
                    return varArgCtor;
                }
                throw notFoundException.get();
            }
        }

        List<Constructor> ctors = Stream.of(type.getConstructors())
                .filter(ctor -> ctor.getParameterCount() == ctorArgs.length)
                .collect(Collectors.toList());

        if (ctors.isEmpty()) {
            // No public constructors that can handle the given arguments.
            throw notFoundException.get();
        }

        if (ctors.size() == 1) {
            return ctors.iterator().next();
        }

        // Revisit this tricky case when it will start bothering.
        throw notFoundException.get();
    }

    @Override
    public void accept(Object thiz) throws Throwable {
        method.invoke(thiz, methodArgs);
    }

    private final Object[] methodArgs;
    private final Method method;
    private final Object[] ctorArgs;

    final static Object[] DEFAULT_CTOR_ARGS = new Object[0];
}
