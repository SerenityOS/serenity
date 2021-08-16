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
import java.util.logging.Level;
import java.util.logging.Logger;
import jdk.dynalink.DynamicLinker;
import jdk.dynalink.linker.LinkerServices;

/**
 * Utility methods for creating typical guards for
 * {@link MethodHandles#guardWithTest(MethodHandle, MethodHandle, MethodHandle)}
 * and for adjusting their method types.
 */
public final class Guards {
    private static final Logger LOG = Logger
            .getLogger(Guards.class.getName(), "jdk.dynalink.support.messages");

    private Guards() {
    }

    /**
     * Creates a guard method handle with arguments of a specified type, but with boolean return value. When invoked, it
     * returns true if the first argument is of the specified class (exactly of it, not a subclass). The rest of the
     * arguments will be ignored.
     *
     * @param clazz the class of the first argument to test for
     * @param type the method type
     * @return a method handle testing whether its first argument is of the specified class.
     */
    @SuppressWarnings("boxing")
    public static MethodHandle isOfClass(final Class<?> clazz, final MethodType type) {
        final Class<?> declaredType = type.parameterType(0);
        if(clazz == declaredType) {
            LOG.log(Level.WARNING, "isOfClassGuardAlwaysTrue", new Object[] { clazz.getName(), 0, type, DynamicLinker.getLinkedCallSiteLocation() });
            return constantTrue(type);
        }
        if(!declaredType.isAssignableFrom(clazz)) {
            LOG.log(Level.WARNING, "isOfClassGuardAlwaysFalse", new Object[] { clazz.getName(), 0, type, DynamicLinker.getLinkedCallSiteLocation() });
            return constantFalse(type);
        }
        return getClassBoundArgumentTest(IS_OF_CLASS, clazz, 0, type);
    }

    /**
     * Creates a method handle with arguments of a specified type, but with boolean return value. When invoked, it
     * returns true if the first argument is instance of the specified class or its subclass). The rest of the arguments
     * will be ignored.
     *
     * @param clazz the class of the first argument to test for
     * @param type the method type
     * @return a method handle testing whether its first argument is of the specified class or subclass.
     */
    public static MethodHandle isInstance(final Class<?> clazz, final MethodType type) {
        return isInstance(clazz, 0, type);
    }

    /**
     * Creates a method handle with arguments of a specified type, but with boolean return value. When invoked, it
     * returns true if the n'th argument is instance of the specified class or its subclass). The rest of the arguments
     * will be ignored.
     *
     * @param clazz the class of the first argument to test for
     * @param pos the position on the argument list to test
     * @param type the method type
     * @return a method handle testing whether its first argument is of the specified class or subclass.
     */
    @SuppressWarnings("boxing")
    public static MethodHandle isInstance(final Class<?> clazz, final int pos, final MethodType type) {
        final Class<?> declaredType = type.parameterType(pos);
        if(clazz.isAssignableFrom(declaredType)) {
            LOG.log(Level.WARNING, "isInstanceGuardAlwaysTrue", new Object[] { clazz.getName(), pos, type, DynamicLinker.getLinkedCallSiteLocation() });
            return constantTrue(type);
        }
        if(!declaredType.isAssignableFrom(clazz)) {
            LOG.log(Level.WARNING, "isInstanceGuardAlwaysFalse", new Object[] { clazz.getName(), pos, type, DynamicLinker.getLinkedCallSiteLocation() });
            return constantFalse(type);
        }
        return getClassBoundArgumentTest(IS_INSTANCE, clazz, pos, type);
    }

    /**
     * Creates a method handle that returns true if the argument in the specified position is a Java array.
     *
     * @param pos the position in the argument lit
     * @param type the method type of the handle
     * @return a method handle that returns true if the argument in the specified position is a Java array; the rest of
     * the arguments are ignored.
     */
    @SuppressWarnings("boxing")
    public static MethodHandle isArray(final int pos, final MethodType type) {
        final Class<?> declaredType = type.parameterType(pos);
        if(declaredType.isArray()) {
            LOG.log(Level.WARNING, "isArrayGuardAlwaysTrue", new Object[] { pos, type, DynamicLinker.getLinkedCallSiteLocation() });
            return constantTrue(type);
        }
        if(!declaredType.isAssignableFrom(Object[].class)) {
            LOG.log(Level.WARNING, "isArrayGuardAlwaysFalse", new Object[] { pos, type, DynamicLinker.getLinkedCallSiteLocation() });
            return constantFalse(type);
        }
        return asType(IS_ARRAY, pos, type);
    }

    private static MethodHandle getClassBoundArgumentTest(final MethodHandle test, final Class<?> clazz, final int pos, final MethodType type) {
        // Bind the class to the first argument of the test
        return asType(test.bindTo(clazz), pos, type);
    }

    /**
     * Takes a method handle intended to be used as a guard, and adapts it to
     * the requested type, but returning a boolean. Applies
     * {@link MethodHandle#asType(MethodType)} to convert types and uses
     * {@link MethodHandles#dropArguments(MethodHandle, int, Class...)} to match
     * the requested type arity.
     * @param test the test method handle
     * @param type the type to adapt the method handle to
     * @return the adapted method handle
     */
    public static MethodHandle asType(final MethodHandle test, final MethodType type) {
        return test.asType(getTestType(test, type));
    }

    /**
     * Takes a method handle intended to be used as a guard, and adapts it to
     * the requested type, but returning a boolean. Applies
     * {@link LinkerServices#asType(MethodHandle, MethodType)} to convert types
     * and uses
     * {@link MethodHandles#dropArguments(MethodHandle, int, Class...)} to match
     * the requested type arity.
     * @param linkerServices the linker services to use for type conversions
     * @param test the test method handle
     * @param type the type to adapt the method handle to
     * @return the adapted method handle
     */
    public static MethodHandle asType(final LinkerServices linkerServices, final MethodHandle test, final MethodType type) {
        return linkerServices.asType(test, getTestType(test, type));
    }

    private static MethodType getTestType(final MethodHandle test, final MethodType type) {
        return type.dropParameterTypes(test.type().parameterCount(),
                type.parameterCount()).changeReturnType(boolean.class);
    }

    private static MethodHandle asType(final MethodHandle test, final int pos, final MethodType type) {
        assert test != null;
        assert type != null;
        assert type.parameterCount() > 0;
        assert pos >= 0 && pos < type.parameterCount();
        assert test.type().parameterCount() == 1;
        assert test.type().returnType() == Boolean.TYPE;
        return MethodHandles.permuteArguments(test.asType(test.type().changeParameterType(0, type.parameterType(pos))),
                type.changeReturnType(Boolean.TYPE), pos);
    }

    private static final MethodHandle IS_INSTANCE = Lookup.PUBLIC.findVirtual(Class.class, "isInstance",
            MethodType.methodType(Boolean.TYPE, Object.class));

    private static final MethodHandle IS_OF_CLASS;
    private static final MethodHandle IS_ARRAY;
    private static final MethodHandle IS_IDENTICAL;
    private static final MethodHandle IS_NULL;
    private static final MethodHandle IS_NOT_NULL;

    static {
        final Lookup lookup = new Lookup(MethodHandles.lookup());

        IS_OF_CLASS  = lookup.findOwnStatic("isOfClass",   Boolean.TYPE, Class.class, Object.class);
        IS_ARRAY     = lookup.findOwnStatic("isArray",     Boolean.TYPE, Object.class);
        IS_IDENTICAL = lookup.findOwnStatic("isIdentical", Boolean.TYPE, Object.class, Object.class);
        IS_NULL      = lookup.findOwnStatic("isNull",      Boolean.TYPE, Object.class);
        IS_NOT_NULL  = lookup.findOwnStatic("isNotNull",   Boolean.TYPE, Object.class);
    }

    /**
     * Creates a guard method that tests its only argument for being of an exact particular class.
     * @param clazz the class to test for.
     * @return the desired guard method.
     */
    public static MethodHandle getClassGuard(final Class<?> clazz) {
        return IS_OF_CLASS.bindTo(clazz);
    }

    /**
     * Creates a guard method that tests its only argument for being an instance of a particular class.
     * @param clazz the class to test for.
     * @return the desired guard method.
     */
    public static MethodHandle getInstanceOfGuard(final Class<?> clazz) {
        return IS_INSTANCE.bindTo(clazz);
    }

    /**
     * Creates a guard method that tests its only argument for being referentially identical to another object
     * @param obj the object used as referential identity test
     * @return the desired guard method.
     */
    public static MethodHandle getIdentityGuard(final Object obj) {
        return IS_IDENTICAL.bindTo(obj);
    }

    /**
     * Returns a guard that tests whether the first argument is null.
     * @return a guard that tests whether the first argument is null.
     */
    public static MethodHandle isNull() {
        return IS_NULL;
    }

    /**
     * Returns a guard that tests whether the first argument is not null.
     * @return a guard that tests whether the first argument is not null.
     */
    public static MethodHandle isNotNull() {
        return IS_NOT_NULL;
    }

    @SuppressWarnings("unused")
    private static boolean isNull(final Object obj) {
        return obj == null;
    }

    @SuppressWarnings("unused")
    private static boolean isNotNull(final Object obj) {
        return obj != null;
    }

    @SuppressWarnings("unused")
    private static boolean isArray(final Object o) {
        return o != null && o.getClass().isArray();
    }

    @SuppressWarnings("unused")
    private static boolean isOfClass(final Class<?> c, final Object o) {
        return o != null && o.getClass() == c;
    }

    @SuppressWarnings("unused")
    private static boolean isIdentical(final Object o1, final Object o2) {
        return o1 == o2;
    }

    private static MethodHandle constantTrue(final MethodType type) {
        return constantBoolean(Boolean.TRUE, type);
    }

    private static MethodHandle constantFalse(final MethodType type) {
        return constantBoolean(Boolean.FALSE, type);
    }

    private static MethodHandle constantBoolean(final Boolean value, final MethodType type) {
        return MethodHandles.permuteArguments(MethodHandles.constant(Boolean.TYPE, value),
                type.changeReturnType(Boolean.TYPE));
    }
}
