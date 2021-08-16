/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Provides classes and interfaces for obtaining reflective information about
 * classes and objects.  Reflection allows programmatic access to information
 * about the fields, methods, and constructors of loaded classes, and the use
 * of reflected fields, methods, and constructors to operate on their underlying
 * counterparts, within encapsulation and security restrictions.
 *
 * <p>Classes in this package, along with {@code java.lang.Class}
 * accommodate applications such as debuggers, interpreters, object
 * inspectors, class browsers, and services such as Object
 * Serialization and JavaBeans that need access to either the public
 * members of a target object (based on its runtime class) or the
 * members declared by a given class.
 *
 * <p>{@link AccessibleObject} allows suppression of access checks if
 * the necessary {@link ReflectPermission} is available.
 *
 * <p>{@link Array} provides static methods to dynamically create and
 * access arrays.
 *
 * <h2><a id="LanguageJvmModel">Java programming language and JVM modeling in core reflection</a></h2>
 *
 * The components of core reflection, which include types in this
 * package as well as {@link java.lang.Class Class}, {@link
 * java.lang.Package Package}, and {@link java.lang.Module Module},
 * fundamentally present a JVM model of the entities in question
 * rather than a Java programming language model.  A Java compiler,
 * such as {@code javac}, translates Java source code into executable
 * output that can be run on a JVM, primarily {@code class}
 * files. Compilers for source languages other than Java can and do
 * target the JVM as well.
 *
 * <p>The translation process, including from Java language sources,
 * to executable output for the JVM is not a one-to-one
 * mapping. Structures present in the source language may have no
 * representation in the output and structures <em>not</em> present in
 * the source language may be present in the output. The latter are
 * called <i>synthetic</i> structures. Synthetic structures can
 * include {@linkplain Method#isSynthetic() methods}, {@linkplain
 * Field#isSynthetic() fields}, {@linkplain Parameter#isSynthetic()
 * parameters}, {@linkplain Class#isSynthetic() classes and
 * interfaces}. One particular kind of synthetic method is a
 * {@linkplain Method#isBridge() bridge method}. It is possible a
 * synthetic structure may not be marked as such. In particular, not
 * all {@code class} file versions support marking a parameter as
 * synthetic. A source language compiler generally has multiple ways
 * to translate a source program into a {@code class} file
 * representation. The translation may also depend on the version of
 * the {@code class} file format being targeted as different {@code
 * class} file versions have different capabilities and features. In
 * some cases the modifiers present in the {@code class} file
 * representation may differ from the modifiers on the originating
 * element in the source language, including {@link Modifier#FINAL
 * final} on a {@linkplain Parameter#getModifiers() parameter} and
 * {@code protected}, {@code private}, and {@code static} on
 * {@linkplain java.lang.Class#getModifiers() classes and interfaces}.
 *
 * <p>Besides differences in structural representation between the
 * source language and the JVM representation, core reflection also
 * exposes runtime specific information. For example, the {@linkplain
 * java.lang.Class#getClassLoader() class loaders} and {@linkplain
 * java.lang.Class#getProtectionDomain() protection domains} of a
 * {@code Class} are runtime concepts without a direct analogue in
 * source code.
 *
 * @jls 13.1 The Form of a Binary
 * @jvms 1.2 The Java Virtual Machine
 * @jvms 4.7.8 The Synthetic Attribute
 * @jvms 5.3.1 Loading Using the Bootstrap Class Loader
 * @jvms 5.3.2 Loading Using a User-defined Class Loader
 * @since 1.1
 * @revised 9
 */
package java.lang.reflect;
