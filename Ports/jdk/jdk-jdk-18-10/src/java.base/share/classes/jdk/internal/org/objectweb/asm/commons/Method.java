/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package jdk.internal.org.objectweb.asm.commons;

import java.util.HashMap;
import java.util.Map;
import jdk.internal.org.objectweb.asm.Type;

/**
 * A named method descriptor.
 *
 * @author Juozas Baliuka
 * @author Chris Nokleberg
 * @author Eric Bruneton
 */
public class Method {

    /** The method name. */
    private final String name;

    /** The method descriptor. */
    private final String descriptor;

    /** The descriptors of the primitive Java types (plus void). */
    private static final Map<String, String> PRIMITIVE_TYPE_DESCRIPTORS;

    static {
        HashMap<String, String> descriptors = new HashMap<>();
        descriptors.put("void", "V");
        descriptors.put("byte", "B");
        descriptors.put("char", "C");
        descriptors.put("double", "D");
        descriptors.put("float", "F");
        descriptors.put("int", "I");
        descriptors.put("long", "J");
        descriptors.put("short", "S");
        descriptors.put("boolean", "Z");
        PRIMITIVE_TYPE_DESCRIPTORS = descriptors;
    }

    /**
      * Constructs a new {@link Method}.
      *
      * @param name the method's name.
      * @param descriptor the method's descriptor.
      */
    public Method(final String name, final String descriptor) {
        this.name = name;
        this.descriptor = descriptor;
    }

    /**
      * Constructs a new {@link Method}.
      *
      * @param name the method's name.
      * @param returnType the method's return type.
      * @param argumentTypes the method's argument types.
      */
    public Method(final String name, final Type returnType, final Type[] argumentTypes) {
        this(name, Type.getMethodDescriptor(returnType, argumentTypes));
    }

    /**
      * Creates a new {@link Method}.
      *
      * @param method a java.lang.reflect method descriptor
      * @return a {@link Method} corresponding to the given Java method declaration.
      */
    public static Method getMethod(final java.lang.reflect.Method method) {
        return new Method(method.getName(), Type.getMethodDescriptor(method));
    }

    /**
      * Creates a new {@link Method}.
      *
      * @param constructor a java.lang.reflect constructor descriptor
      * @return a {@link Method} corresponding to the given Java constructor declaration.
      */
    public static Method getMethod(final java.lang.reflect.Constructor<?> constructor) {
        return new Method("<init>", Type.getConstructorDescriptor(constructor));
    }

    /**
      * Returns a {@link Method} corresponding to the given Java method declaration.
      *
      * @param method a Java method declaration, without argument names, of the form "returnType name
      *     (argumentType1, ... argumentTypeN)", where the types are in plain Java (e.g. "int",
      *     "float", "java.util.List", ...). Classes of the java.lang package can be specified by their
      *     unqualified name; all other classes names must be fully qualified.
      * @return a {@link Method} corresponding to the given Java method declaration.
      * @throws IllegalArgumentException if <code>method</code> could not get parsed.
      */
    public static Method getMethod(final String method) {
        return getMethod(method, false);
    }

    /**
      * Returns a {@link Method} corresponding to the given Java method declaration.
      *
      * @param method a Java method declaration, without argument names, of the form "returnType name
      *     (argumentType1, ... argumentTypeN)", where the types are in plain Java (e.g. "int",
      *     "float", "java.util.List", ...). Classes of the java.lang package may be specified by their
      *     unqualified name, depending on the defaultPackage argument; all other classes names must be
      *     fully qualified.
      * @param defaultPackage true if unqualified class names belong to the default package, or false
      *     if they correspond to java.lang classes. For instance "Object" means "Object" if this
      *     option is true, or "java.lang.Object" otherwise.
      * @return a {@link Method} corresponding to the given Java method declaration.
      * @throws IllegalArgumentException if <code>method</code> could not get parsed.
      */
    public static Method getMethod(final String method, final boolean defaultPackage) {
        final int spaceIndex = method.indexOf(' ');
        int currentArgumentStartIndex = method.indexOf('(', spaceIndex) + 1;
        final int endIndex = method.indexOf(')', currentArgumentStartIndex);
        if (spaceIndex == -1 || currentArgumentStartIndex == 0 || endIndex == -1) {
            throw new IllegalArgumentException();
        }
        final String returnType = method.substring(0, spaceIndex);
        final String methodName =
                method.substring(spaceIndex + 1, currentArgumentStartIndex - 1).trim();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('(');
        int currentArgumentEndIndex;
        do {
            String argumentDescriptor;
            currentArgumentEndIndex = method.indexOf(',', currentArgumentStartIndex);
            if (currentArgumentEndIndex == -1) {
                argumentDescriptor =
                        getDescriptorInternal(
                                method.substring(currentArgumentStartIndex, endIndex).trim(), defaultPackage);
            } else {
                argumentDescriptor =
                        getDescriptorInternal(
                                method.substring(currentArgumentStartIndex, currentArgumentEndIndex).trim(),
                                defaultPackage);
                currentArgumentStartIndex = currentArgumentEndIndex + 1;
            }
            stringBuilder.append(argumentDescriptor);
        } while (currentArgumentEndIndex != -1);
        stringBuilder.append(')').append(getDescriptorInternal(returnType, defaultPackage));
        return new Method(methodName, stringBuilder.toString());
    }

    /**
      * Returns the descriptor corresponding to the given type name.
      *
      * @param type a Java type name.
      * @param defaultPackage true if unqualified class names belong to the default package, or false
      *     if they correspond to java.lang classes. For instance "Object" means "Object" if this
      *     option is true, or "java.lang.Object" otherwise.
      * @return the descriptor corresponding to the given type name.
      */
    private static String getDescriptorInternal(final String type, final boolean defaultPackage) {
        if ("".equals(type)) {
            return type;
        }

        StringBuilder stringBuilder = new StringBuilder();
        int arrayBracketsIndex = 0;
        while ((arrayBracketsIndex = type.indexOf("[]", arrayBracketsIndex) + 1) > 0) {
            stringBuilder.append('[');
        }

        String elementType = type.substring(0, type.length() - stringBuilder.length() * 2);
        String descriptor = PRIMITIVE_TYPE_DESCRIPTORS.get(elementType);
        if (descriptor != null) {
            stringBuilder.append(descriptor);
        } else {
            stringBuilder.append('L');
            if (elementType.indexOf('.') < 0) {
                if (!defaultPackage) {
                    stringBuilder.append("java/lang/");
                }
                stringBuilder.append(elementType);
            } else {
                stringBuilder.append(elementType.replace('.', '/'));
            }
            stringBuilder.append(';');
        }
        return stringBuilder.toString();
    }

    /**
      * Returns the name of the method described by this object.
      *
      * @return the name of the method described by this object.
      */
    public String getName() {
        return name;
    }

    /**
      * Returns the descriptor of the method described by this object.
      *
      * @return the descriptor of the method described by this object.
      */
    public String getDescriptor() {
        return descriptor;
    }

    /**
      * Returns the return type of the method described by this object.
      *
      * @return the return type of the method described by this object.
      */
    public Type getReturnType() {
        return Type.getReturnType(descriptor);
    }

    /**
      * Returns the argument types of the method described by this object.
      *
      * @return the argument types of the method described by this object.
      */
    public Type[] getArgumentTypes() {
        return Type.getArgumentTypes(descriptor);
    }

    @Override
    public String toString() {
        return name + descriptor;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof Method)) {
            return false;
        }
        Method otherMethod = (Method) other;
        return name.equals(otherMethod.name) && descriptor.equals(otherMethod.descriptor);
    }

    @Override
    public int hashCode() {
        return name.hashCode() ^ descriptor.hashCode();
    }
}
