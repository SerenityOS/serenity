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

package jdk.dynalink.beans;

import java.lang.invoke.MethodHandles.Lookup;
import java.util.Collections;
import java.util.Set;
import jdk.dynalink.DynamicLinkerFactory;
import jdk.dynalink.StandardNamespace;
import jdk.dynalink.StandardOperation;
import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.GuardingDynamicLinker;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;
import jdk.dynalink.linker.TypeBasedGuardingDynamicLinker;

/**
 * A linker for ordinary Java objects. Normally used as the ultimate fallback
 * linker by the {@link DynamicLinkerFactory} so it is given the chance to link
 * calls to all objects that no other linker recognized. Specifically, this
 * linker will:
 * <ul>
 * <li>if the object is a {@link java.lang.Record record}, expose all public accessors of
 * record components as property getters for {@link StandardOperation#GET} operations
 * in the {@link StandardNamespace#PROPERTY} namespace;</li>
 * <li>expose all public methods of form {@code setXxx()}, {@code getXxx()},
 * and {@code isXxx()} as property setters and getters for
 * {@link StandardOperation#SET} and {@link StandardOperation#GET} operations in the
 * {@link StandardNamespace#PROPERTY} namespace, except for getters for properties
 * with names already handled by record component getters;</li>
 * <li>expose all public methods for retrieval for
 * {@link StandardOperation#GET} operation in the {@link StandardNamespace#METHOD} namespace;
 * the methods thus retrieved can then be invoked using {@link StandardOperation#CALL}.</li>
 * <li>expose all public fields as properties, unless there are getters or
 * setters for the properties of the same name;</li>
 * <li> expose elements of native Java arrays, {@link java.util.List} and {@link java.util.Map} objects as
 * {@link StandardOperation#GET} and {@link StandardOperation#SET} operations in the
 * {@link StandardNamespace#ELEMENT} namespace;</li>
 * <li> expose removal of elements of {@link java.util.List} and {@link java.util.Map} objects as
 * {@link StandardOperation#REMOVE} operation in the {@link StandardNamespace#ELEMENT} namespace;</li>
 * <li>expose a virtual property named {@code length} on Java arrays, {@link java.util.Collection} and
 * {@link java.util.Map} objects;</li>
 * <li>expose {@link StandardOperation#NEW} on instances of {@link StaticClass}
 * as calls to constructors, including those static class objects that represent
 * Java arrays (their constructors take a single {@code int} parameter
 * representing the length of the array to create);</li>
 * <li>expose static methods, fields, and properties of classes in a similar
 * manner to how instance method, fields, and properties are exposed, on
 * {@link StaticClass} objects.</li>
 * <li>expose a virtual property named {@code static} on instances of
 * {@link java.lang.Class} to access their {@link StaticClass}.</li>
 * </ul>
 * <p><strong>Overloaded method resolution</strong> is performed automatically
 * for property setters, methods, and constructors. Additionally, manual
 * overloaded method selection is supported by having a call site specify a name
 * for a method that contains an explicit signature, e.g.
 * {@code StandardOperation.GET.withNamespace(METHOD).named("parseInt(String,int)")}
 * You can use non-qualified class names in such signatures regardless of those
 * classes' packages, they will match any class with the same non-qualified name. You
 * only have to use a fully qualified class name in case non-qualified class
 * names would cause selection ambiguity (that is extremely rare). Overloaded
 * resolution for constructors is not automatic as there is no logical place to
 * attach that functionality to but if a language wishes to provide this
 * functionality, it can use {@link #getConstructorMethod(Class, String)} as a
 * useful building block for it.</p>
 * <p><strong>Variable argument invocation</strong> is handled for both methods
 * and constructors.</p>
 * <p><strong>Caller sensitive methods</strong> can be linked as long as they
 * are otherwise public and link requests have call site descriptors carrying
 * full-strength {@link Lookup} objects and not weakened lookups or the public
 * lookup.</p>
 * <p><strong>The behavior for handling missing members</strong> can be
 * customized by passing a {@link MissingMemberHandlerFactory} to the
 * {@link BeansLinker#BeansLinker(MissingMemberHandlerFactory) constructor}.
 * </p>
 * <p>The class also exposes various methods for discovery of available
 * property and method names on classes and class instances, as well as access
 * to per-class linkers using the {@link #getLinkerForClass(Class)}
 * method.</p>
 */
public class BeansLinker implements GuardingDynamicLinker {
    private static final ClassValue<TypeBasedGuardingDynamicLinker> linkers = new ClassValue<>() {
        @Override
        protected TypeBasedGuardingDynamicLinker computeValue(final Class<?> clazz) {
            // If ClassValue.put() were public, we could just pre-populate with these known mappings...
            return
                clazz == Class.class ? new ClassLinker() :
                clazz == StaticClass.class ? new StaticClassLinker() :
                DynamicMethod.class.isAssignableFrom(clazz) ? new DynamicMethodLinker() :
                new BeanLinker(clazz);
        }
    };

    private final MissingMemberHandlerFactory missingMemberHandlerFactory;

    /**
     * Creates a new beans linker. Equivalent to
     * {@link BeansLinker#BeansLinker(MissingMemberHandlerFactory)} with
     * {@code null} passed as the missing member handler factory, resulting in
     * the default behavior for linking and evaluating missing members.
     */
    public BeansLinker() {
        this(null);
    }

    /**
     * Creates a new beans linker with the specified factory for creating
     * missing member handlers. The passed factory can be null if the default
     * behavior is adequate. See {@link MissingMemberHandlerFactory} for details.
     * @param missingMemberHandlerFactory a factory for creating handlers for
     * operations on missing members.
     */
    public BeansLinker(final MissingMemberHandlerFactory missingMemberHandlerFactory) {
        this.missingMemberHandlerFactory = missingMemberHandlerFactory;
    }

    /**
     * Returns a bean linker for a particular single class. Useful when you need
     * to override or extend the behavior of linking for some classes in your
     * language runtime's linker, but still want to delegate to the default
     * behavior in some cases.
     * @param clazz the class
     * @return a bean linker for that class
     */
    public TypeBasedGuardingDynamicLinker getLinkerForClass(final Class<?> clazz) {
        final TypeBasedGuardingDynamicLinker staticLinker = getStaticLinkerForClass(clazz);
        if (missingMemberHandlerFactory == null) {
            return staticLinker;
        }
        return new NoSuchMemberHandlerBindingLinker(staticLinker, missingMemberHandlerFactory);
    }

    private static class NoSuchMemberHandlerBindingLinker implements TypeBasedGuardingDynamicLinker {
        private final TypeBasedGuardingDynamicLinker linker;
        private final MissingMemberHandlerFactory missingMemberHandlerFactory;

        NoSuchMemberHandlerBindingLinker(final TypeBasedGuardingDynamicLinker linker, final MissingMemberHandlerFactory missingMemberHandlerFactory) {
            this.linker = linker;
            this.missingMemberHandlerFactory = missingMemberHandlerFactory;
        }

        @Override
        public boolean canLinkType(final Class<?> type) {
            return linker.canLinkType(type);
        }

        @Override
        public GuardedInvocation getGuardedInvocation(final LinkRequest linkRequest, final LinkerServices linkerServices) throws Exception {
            return linker.getGuardedInvocation(linkRequest,
                    LinkerServicesWithMissingMemberHandlerFactory.get(
                            linkerServices, missingMemberHandlerFactory));
        }
    }

    static TypeBasedGuardingDynamicLinker getStaticLinkerForClass(final Class<?> clazz) {
        return linkers.get(clazz);
    }

    /**
     * Returns true if the object is a Java dynamic method (e.g., one
     * obtained through a {@code GET:METHOD} operation on a Java object or
     * {@link StaticClass} or through
     * {@link #getConstructorMethod(Class, String)}.
     *
     * @param obj the object we want to test for being a Java dynamic method.
     * @return true if it is a dynamic method, false otherwise.
     */
    public static boolean isDynamicMethod(final Object obj) {
        return obj instanceof DynamicMethod;
    }

    /**
     * Returns true if the object is a Java constructor (obtained through
     * {@link #getConstructorMethod(Class, String)}}.
     *
     * @param obj the object we want to test for being a Java constructor.
     * @return true if it is a constructor, false otherwise.
     */
    public static boolean isDynamicConstructor(final Object obj) {
        return obj instanceof DynamicMethod && ((DynamicMethod)obj).isConstructor();
    }

    /**
     * Return the dynamic method of constructor of the given class and the given
     * signature. This method is useful for exposing a functionality for
     * selecting an overloaded constructor based on an explicit signature, as
     * this functionality is not otherwise exposed by Dynalink as
     * {@link StaticClass} objects act as overloaded constructors without
     * explicit signature selection. Example usage would be:
     * {@code getConstructorMethod(java.awt.Color.class, "int, int, int")}.
     * @param clazz the class
     * @param signature full signature of the constructor. Note how you can use
     * names of primitive types, array names with normal Java notation (e.g.
     * {@code "int[]"}), and normally you can even use unqualified class names
     * (e.g. {@code "String, List"} instead of
     * {@code "java.lang.String, java.util.List"} as long as they don't cause
     * ambiguity in the specific parameter position.
     * @return dynamic method for the constructor or null if no constructor with
     * the specified signature exists.
     */
    public static Object getConstructorMethod(final Class<?> clazz, final String signature) {
        return StaticClassLinker.getConstructorMethod(clazz, signature);
    }

    /**
     * Returns a set of names of all readable instance properties of a class.
     * @param clazz the class
     * @return a set of names of all readable instance properties of a class.
     */
    public static Set<String> getReadableInstancePropertyNames(final Class<?> clazz) {
        final TypeBasedGuardingDynamicLinker linker = getStaticLinkerForClass(clazz);
        if(linker instanceof BeanLinker) {
            return ((BeanLinker)linker).getReadablePropertyNames();
        }
        return Collections.emptySet();
    }

    /**
     * Returns a set of names of all writable instance properties of a class.
     * @param clazz the class
     * @return a set of names of all writable instance properties of a class.
     */
    public static Set<String> getWritableInstancePropertyNames(final Class<?> clazz) {
        final TypeBasedGuardingDynamicLinker linker = getStaticLinkerForClass(clazz);
        if(linker instanceof BeanLinker) {
            return ((BeanLinker)linker).getWritablePropertyNames();
        }
        return Collections.emptySet();
    }

    /**
     * Returns a set of names of all instance methods of a class.
     * @param clazz the class
     * @return a set of names of all instance methods of a class.
     */
    public static Set<String> getInstanceMethodNames(final Class<?> clazz) {
        final TypeBasedGuardingDynamicLinker linker = getStaticLinkerForClass(clazz);
        if(linker instanceof BeanLinker) {
            return ((BeanLinker)linker).getMethodNames();
        }
        return Collections.emptySet();
    }

    /**
     * Returns a set of names of all readable static properties of a class.
     * @param clazz the class
     * @return a set of names of all readable static properties of a class.
     */
    public static Set<String> getReadableStaticPropertyNames(final Class<?> clazz) {
        return StaticClassLinker.getReadableStaticPropertyNames(clazz);
    }

    /**
     * Returns a set of names of all writable static properties of a class.
     * @param clazz the class
     * @return a set of names of all writable static properties of a class.
     */
    public static Set<String> getWritableStaticPropertyNames(final Class<?> clazz) {
        return StaticClassLinker.getWritableStaticPropertyNames(clazz);
    }

    /**
     * Returns a set of names of all static methods of a class.
     * @param clazz the class
     * @return a set of names of all static methods of a class.
     */
    public static Set<String> getStaticMethodNames(final Class<?> clazz) {
        return StaticClassLinker.getStaticMethodNames(clazz);
    }

    @Override
    public GuardedInvocation getGuardedInvocation(final LinkRequest request, final LinkerServices linkerServices)
            throws Exception {
        final Object receiver = request.getReceiver();
        if(receiver == null) {
            // Can't operate on null
            return null;
        }
        return getLinkerForClass(receiver.getClass()).getGuardedInvocation(request,
                LinkerServicesWithMissingMemberHandlerFactory.get(linkerServices,
                        missingMemberHandlerFactory));
    }
}
