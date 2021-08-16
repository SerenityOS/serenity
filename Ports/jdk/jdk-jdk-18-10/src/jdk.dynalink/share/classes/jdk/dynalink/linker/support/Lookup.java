/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * file, and Oracle licenses the original version of this file under the BSD
 * license:
 */
/*
   Copyright 2009-2013 Attila Szegedi

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions are
   met:
   * Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
   * Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
   * Neither the name of the copyright holder nor the names of
     contributors may be used to endorse or promote products derived from
     this software without specific prior written permission.

   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
   IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL COPYRIGHT HOLDER
   BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
   CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
   SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
   BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
   WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
   OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
   ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package jdk.dynalink.linker.support;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * A wrapper around {@link java.lang.invoke.MethodHandles.Lookup} that masks
 * checked exceptions. It is useful in those cases when you're looking up
 * methods within your own codebase (therefore it is an error if they are not
 * present).
 */
public final class Lookup {
    private final MethodHandles.Lookup lookup;

    /**
     * Creates a new instance, bound to an instance of
     * {@link java.lang.invoke.MethodHandles.Lookup}.
     *
     * @param lookup the {@link java.lang.invoke.MethodHandles.Lookup} it delegates to.
     */
    public Lookup(final MethodHandles.Lookup lookup) {
        this.lookup = lookup;
    }

    /**
     * A canonical Lookup object that wraps {@link MethodHandles#publicLookup()}.
     */
    public static final Lookup PUBLIC = new Lookup(MethodHandles.publicLookup());

    /**
     * Performs a {@link java.lang.invoke.MethodHandles.Lookup#unreflect(Method)},
     * converting any encountered {@link IllegalAccessException} into an
     * {@link IllegalAccessError}.
     *
     * @param m the method to unreflect
     * @return the unreflected method handle.
     * @throws IllegalAccessError if the method is inaccessible.
     */
    public MethodHandle unreflect(final Method m) {
        return unreflect(lookup, m);
    }

    /**
     * Performs a {@link java.lang.invoke.MethodHandles.Lookup#unreflect(Method)},
     * converting any encountered {@link IllegalAccessException} into an
     * {@link IllegalAccessError}.
     *
     * @param lookup the lookup used to unreflect
     * @param m the method to unreflect
     * @return the unreflected method handle.
     * @throws IllegalAccessError if the method is inaccessible.
     */
    public static MethodHandle unreflect(final MethodHandles.Lookup lookup, final Method m) {
        try {
            return lookup.unreflect(m);
        } catch(final IllegalAccessException e) {
            final IllegalAccessError ee = new IllegalAccessError("Failed to unreflect method " + m);
            ee.initCause(e);
            throw ee;
        }
    }

    /**
     * Performs a {@link java.lang.invoke.MethodHandles.Lookup#unreflectGetter(Field)},
     * converting any encountered {@link IllegalAccessException} into an {@link IllegalAccessError}.
     *
     * @param f the field for which a getter is unreflected
     * @return the unreflected field getter handle.
     * @throws IllegalAccessError if the getter is inaccessible.
     */
    public MethodHandle unreflectGetter(final Field f) {
        try {
            return lookup.unreflectGetter(f);
        } catch(final IllegalAccessException e) {
            final IllegalAccessError ee = new IllegalAccessError("Failed to unreflect getter for field " + f);
            ee.initCause(e);
            throw ee;
        }
    }

    /**
     * Performs a {@link java.lang.invoke.MethodHandles.Lookup#findGetter(Class, String, Class)},
     * converting any encountered {@link IllegalAccessException} into an
     * {@link IllegalAccessError} and {@link NoSuchFieldException} into a
     * {@link NoSuchFieldError}.
     *
     * @param refc the class declaring the field
     * @param name the name of the field
     * @param type the type of the field
     * @return the unreflected field getter handle.
     * @throws IllegalAccessError if the field is inaccessible.
     * @throws NoSuchFieldError if the field does not exist.
     */
    public MethodHandle findGetter(final Class<?>refc, final String name, final Class<?> type) {
        try {
            return lookup.findGetter(refc, name, type);
        } catch(final IllegalAccessException e) {
            final IllegalAccessError ee = new IllegalAccessError("Failed to access getter for field " + refc.getName() +
                    "." + name + " of type " + type.getName());
            ee.initCause(e);
            throw ee;
        } catch(final NoSuchFieldException e) {
            final NoSuchFieldError ee = new NoSuchFieldError("Failed to find getter for field " + refc.getName() +
                    "." + name + " of type " + type.getName());
            ee.initCause(e);
            throw ee;
        }
    }

    /**
     * Performs a {@link java.lang.invoke.MethodHandles.Lookup#unreflectSetter(Field)},
     * converting any encountered {@link IllegalAccessException} into an
     * {@link IllegalAccessError}.
     *
     * @param f the field for which a setter is unreflected
     * @return the unreflected field setter handle.
     * @throws IllegalAccessError if the field is inaccessible.
     * @throws NoSuchFieldError if the field does not exist.
     */
    public MethodHandle unreflectSetter(final Field f) {
        try {
            return lookup.unreflectSetter(f);
        } catch(final IllegalAccessException e) {
            final IllegalAccessError ee = new IllegalAccessError("Failed to unreflect setter for field " + f);
            ee.initCause(e);
            throw ee;
        }
    }

    /**
     * Performs a {@link java.lang.invoke.MethodHandles.Lookup#unreflectConstructor(Constructor)},
     * converting any encountered {@link IllegalAccessException} into an
     * {@link IllegalAccessError}.
     *
     * @param c the constructor to unreflect
     * @return the unreflected constructor handle.
     * @throws IllegalAccessError if the constructor is inaccessible.
     */
    public MethodHandle unreflectConstructor(final Constructor<?> c) {
        return unreflectConstructor(lookup, c);
    }

    /**
     * Performs a {@link java.lang.invoke.MethodHandles.Lookup#unreflectConstructor(Constructor)},
     * converting any encountered {@link IllegalAccessException} into an
     * {@link IllegalAccessError}.
     *
     * @param lookup the lookup used to unreflect
     * @param c the constructor to unreflect
     * @return the unreflected constructor handle.
     * @throws IllegalAccessError if the constructor is inaccessible.
     */
    public static MethodHandle unreflectConstructor(final MethodHandles.Lookup lookup, final Constructor<?> c) {
        try {
            return lookup.unreflectConstructor(c);
        } catch(final IllegalAccessException e) {
            final IllegalAccessError ee = new IllegalAccessError("Failed to unreflect constructor " + c);
            ee.initCause(e);
            throw ee;
        }
    }

    /**
     * Performs a {@link java.lang.invoke.MethodHandles.Lookup#findSpecial(Class, String, MethodType, Class)}
     * on the underlying lookup. Converts any encountered
     * {@link IllegalAccessException} into an {@link IllegalAccessError} and
     * {@link NoSuchMethodException} into a {@link NoSuchMethodError}.
     *
     * @param declaringClass class declaring the method
     * @param name the name of the method
     * @param type the type of the method
     * @return a method handle for the method
     * @throws IllegalAccessError if the method is inaccessible.
     * @throws NoSuchMethodError if the method does not exist.
     */
    public MethodHandle findSpecial(final Class<?> declaringClass, final String name, final MethodType type) {
        try {
            return lookup.findSpecial(declaringClass, name, type, declaringClass);
        } catch(final IllegalAccessException e) {
            final IllegalAccessError ee = new IllegalAccessError("Failed to access special method " + methodDescription(
                    declaringClass, name, type));
            ee.initCause(e);
            throw ee;
        } catch(final NoSuchMethodException e) {
            final NoSuchMethodError ee = new NoSuchMethodError("Failed to find special method " + methodDescription(
                    declaringClass, name, type));
            ee.initCause(e);
            throw ee;
        }
    }

    private static String methodDescription(final Class<?> declaringClass, final String name, final MethodType type) {
        return declaringClass.getName() + "#" + name + type;
    }

    /**
     * Performs a {@link java.lang.invoke.MethodHandles.Lookup#findStatic(Class, String, MethodType)}
     * on the underlying lookup. Converts any encountered
     * {@link IllegalAccessException} into an {@link IllegalAccessError} and
     * {@link NoSuchMethodException} into a {@link NoSuchMethodError}.
     *
     * @param declaringClass class declaring the method
     * @param name the name of the method
     * @param type the type of the method
     * @return a method handle for the method
     * @throws IllegalAccessError if the method is inaccessible.
     * @throws NoSuchMethodError if the method does not exist.
     */
    public MethodHandle findStatic(final Class<?> declaringClass, final String name, final MethodType type) {
        try {
            return lookup.findStatic(declaringClass, name, type);
        } catch(final IllegalAccessException e) {
            final IllegalAccessError ee = new IllegalAccessError("Failed to access static method " + methodDescription(
                    declaringClass, name, type));
            ee.initCause(e);
            throw ee;
        } catch(final NoSuchMethodException e) {
            final NoSuchMethodError ee = new NoSuchMethodError("Failed to find static method " + methodDescription(
                    declaringClass, name, type));
            ee.initCause(e);
            throw ee;
        }
    }

    /**
     * Performs a {@link java.lang.invoke.MethodHandles.Lookup#findVirtual(Class, String, MethodType)}
     * on the underlying lookup. Converts any encountered
     * {@link IllegalAccessException} into an {@link IllegalAccessError} and
     * {@link NoSuchMethodException} into a {@link NoSuchMethodError}.
     *
     * @param declaringClass class declaring the method
     * @param name the name of the method
     * @param type the type of the method
     * @return a method handle for the method
     * @throws IllegalAccessError if the method is inaccessible.
     * @throws NoSuchMethodError if the method does not exist.
     */
    public MethodHandle findVirtual(final Class<?> declaringClass, final String name, final MethodType type) {
        try {
            return lookup.findVirtual(declaringClass, name, type);
        } catch(final IllegalAccessException e) {
            final IllegalAccessError ee = new IllegalAccessError("Failed to access virtual method " + methodDescription(
                    declaringClass, name, type));
            ee.initCause(e);
            throw ee;
        } catch(final NoSuchMethodException e) {
            final NoSuchMethodError ee = new NoSuchMethodError("Failed to find virtual method " + methodDescription(
                    declaringClass, name, type));
            ee.initCause(e);
            throw ee;
        }
    }

    /**
     * Given a lookup, finds using {@link #findSpecial(Class, String, MethodType)}
     * a method on that lookup's class. Useful in classes' code for convenient
     * linking to their own privates.
     * @param lookup the lookup for the class
     * @param name the name of the method
     * @param rtype the return type of the method
     * @param ptypes the parameter types of the method
     * @return the method handle for the method
     */
    public static MethodHandle findOwnSpecial(final MethodHandles.Lookup lookup, final String name, final Class<?> rtype, final Class<?>... ptypes) {
        return new Lookup(lookup).findOwnSpecial(name, rtype, ptypes);
    }


    /**
     * Finds using {@link #findSpecial(Class, String, MethodType)} a method on
     * that lookup's class. Useful in classes' code for convenient linking to
     * their own privates. It's also more convenient than {@code findSpecial}
     * in that you can just list the parameter types, and don't have to specify
     * lookup class.
     * @param name the name of the method
     * @param rtype the return type of the method
     * @param ptypes the parameter types of the method
     * @return the method handle for the method
     */
    public MethodHandle findOwnSpecial(final String name, final Class<?> rtype, final Class<?>... ptypes) {
        return findSpecial(lookup.lookupClass(), name, MethodType.methodType(rtype, ptypes));
    }

    /**
     * Given a lookup, finds using {@link #findStatic(Class, String, MethodType)}
     * a method on that lookup's class. Useful in classes' code for convenient
     * linking to their own privates. It's easier to use than {@code findStatic}
     * in that you can just list the parameter types, and don't have to specify
     * lookup class.
     * @param lookup the lookup for the class
     * @param name the name of the method
     * @param rtype the return type of the method
     * @param ptypes the parameter types of the method
     * @return the method handle for the method
     */
    public static MethodHandle findOwnStatic(final MethodHandles.Lookup lookup, final String name, final Class<?> rtype, final Class<?>... ptypes) {
        return new Lookup(lookup).findOwnStatic(name, rtype, ptypes);
    }

    /**
     * Finds using {@link #findStatic(Class, String, MethodType)} a method on
     * that lookup's class. Useful in classes' code for convenient linking to
     * their own privates. It's easier to use than {@code findStatic}
     * in that you can just list the parameter types, and don't have to specify
     * lookup class.
     * @param name the name of the method
     * @param rtype the return type of the method
     * @param ptypes the parameter types of the method
     * @return the method handle for the method
     */
    public MethodHandle findOwnStatic(final String name, final Class<?> rtype, final Class<?>... ptypes) {
        return findStatic(lookup.lookupClass(), name, MethodType.methodType(rtype, ptypes));
    }
}
