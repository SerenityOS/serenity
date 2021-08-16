/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * Defines the API for dynamic linking of high-level operations on objects.
 * <p>
 * Dynalink is a library for dynamic linking of high-level operations on objects.
 * These operations include "read a property",
 * "write a property", "invoke a function" and so on. Dynalink is primarily
 * useful for implementing programming languages where at least some expressions
 * have dynamic types (that is, types that can not be decided statically), and
 * the operations on dynamic types are expressed as
 * {@linkplain java.lang.invoke.CallSite call sites}. These call sites will be
 * linked to appropriate target {@linkplain java.lang.invoke.MethodHandle method handles}
 * at run time based on actual types of the values the expressions evaluated to.
 * These can change between invocations, necessitating relinking the call site
 * multiple times to accommodate new types; Dynalink handles all that and more.
 * <p>
 * Dynalink supports implementation of programming languages with object models
 * that differ (even radically) from the JVM's class-based model and have their
 * custom type conversions.
 * <p>
 * Dynalink is closely related to, and relies on, the {@link java.lang.invoke}
 * package.
 * <p>
 *
 * While {@link java.lang.invoke} provides a low level API for dynamic linking
 * of {@code invokedynamic} call sites, it does not provide a way to express
 * higher level operations on objects, nor methods that implement them. These
 * operations are the usual ones in object-oriented environments: property
 * access, access of elements of collections, invocation of methods and
 * constructors (potentially with multiple dispatch, e.g. link- and run-time
 * equivalents of Java overloaded method resolution). These are all functions
 * that are normally desired in a language on the JVM. If a language is
 * statically typed and its type system matches that of the JVM, it can
 * accomplish this with use of the usual invocation, field access, etc.
 * instructions (e.g. {@code invokevirtual}, {@code getfield}). However, if the
 * language is dynamic (hence, types of some expressions are not known until
 * evaluated at run time), or its object model or type system don't match
 * closely that of the JVM, then it should use {@code invokedynamic} call sites
 * instead and let Dynalink manage them.
 * <h2>Example</h2>
 * Dynalink is probably best explained by an example showing its use. Let's
 * suppose you have a program in a language where you don't have to declare the
 * type of an object and you want to access a property on it:
 * <pre>
 * var color = obj.color;
 * </pre>
 * If you generated a Java class to represent the above one-line program, its
 * bytecode would look something like this:
 * <pre>
 * aload 2 // load "obj" on stack
 * invokedynamic "GET:PROPERTY:color"(Object)Object // invoke property getter on object of unknown type
 * astore 3 // store the return value into local variable "color"
 * </pre>
 * In order to link the {@code invokedynamic} instruction, we need a bootstrap
 * method. A minimalist bootstrap method with Dynalink could look like this:
 * <pre>
 * import java.lang.invoke.*;
 * import jdk.dynalink.*;
 * import jdk.dynalink.support.*;
 *
 * class MyLanguageRuntime {
 *     private static final DynamicLinker dynamicLinker = new DynamicLinkerFactory().createLinker();
 *
 *     public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type) {
 *         return dynamicLinker.link(
 *             new SimpleRelinkableCallSite(
 *                 new CallSiteDescriptor(lookup, parseOperation(name), type)));
 *     }
 *
 *     private static Operation parseOperation(String name) {
 *         ...
 *     }
 * }
 * </pre>
 * There are several objects of significance in the above code snippet:
 * <ul>
 * <li>{@link jdk.dynalink.DynamicLinker} is the main object in Dynalink, it
 * coordinates the linking of call sites to method handles that implement the
 * operations named in them. It is configured and created using a
 * {@link jdk.dynalink.DynamicLinkerFactory}.</li>
 * <li>When the bootstrap method is invoked, it needs to create a
 * {@link java.lang.invoke.CallSite} object. In Dynalink, these call sites need
 * to additionally implement the {@link jdk.dynalink.RelinkableCallSite}
 * interface. "Relinkable" here alludes to the fact that if the call site
 * encounters objects of different types at run time, its target will be changed
 * to a method handle that can perform the operation on the newly encountered
 * type. {@link jdk.dynalink.support.SimpleRelinkableCallSite} and
 * {@link jdk.dynalink.support.ChainedCallSite} (not used in the above example)
 * are two implementations already provided by the library.</li>
 * <li>Dynalink uses {@link jdk.dynalink.CallSiteDescriptor} objects to
 * preserve the parameters to the bootstrap method: the lookup and the method type,
 * as it will need them whenever it needs to relink a call site.</li>
 * <li>Dynalink uses {@link jdk.dynalink.Operation} objects to express
 * dynamic operations. It does not prescribe how would you encode the operations
 * in your call site, though. That is why in the above example the
 * {@code parseOperation} function is left empty, and you would be expected to
 * provide the code to parse the string {@code "GET:PROPERTY:color"}
 * in the call site's name into a named property getter operation object as
 * {@code StandardOperation.GET.withNamespace(StandardNamespace.PROPERTY).named("color")}.
 * </ul>
 * <p>What can you already do with the above setup? {@code DynamicLinkerFactory}
 * by default creates a {@code DynamicLinker} that can link Java objects with the
 * usual Java semantics. If you have these three simple classes:
 * <pre>
 * public class A {
 *     public String color;
 *     public A(String color) { this.color = color; }
 * }
 *
 * public class B {
 *     private String color;
 *     public B(String color) { this.color = color; }
 *     public String getColor() { return color; }
 * }
 *
 * public class C {
 *     private int color;
 *     public C(int color) { this.color = color; }
 *     public int getColor() { return color; }
 * }
 * </pre>
 * and you somehow create their instances and pass them to your call site in your
 * programming language:
 * <pre>
 * for each(var obj in [new A("red"), new B("green"), new C(0x0000ff)]) {
 *     print(obj.color);
 * }
 * </pre>
 * then on first invocation, Dynalink will link the {@code .color} getter
 * operation to a field getter for {@code A.color}, on second invocation it will
 * relink it to {@code B.getColor()} returning a {@code String}, and finally on
 * third invocation it will relink it to {@code C.getColor()} returning an {@code int}.
 * The {@code SimpleRelinkableCallSite} we used above only remembers the linkage
 * for the last encountered type (it implements what is known as a <i>monomorphic
 * inline cache</i>). Another already provided implementation,
 * {@link jdk.dynalink.support.ChainedCallSite} will remember linkages for
 * several different types (it is a <i>polymorphic inline cache</i>) and is
 * probably a better choice in serious applications.
 * <h2>Dynalink and bytecode creation</h2>
 * {@code CallSite} objects are usually created as part of bootstrapping
 * {@code invokedynamic} instructions in bytecode. Hence, Dynalink is typically
 * used as part of language runtimes that compile programs into Java
 * {@code .class} bytecode format. Dynalink does not address the aspects of
 * either creating bytecode classes or loading them into the JVM. That said,
 * Dynalink can also be used without bytecode compilation (e.g. in language
 * interpreters) by creating {@code CallSite} objects explicitly and associating
 * them with representations of dynamic operations in the interpreted program
 * (e.g. a typical representation would be some node objects in a syntax tree).
 * <h2>Available operations</h2>
 * Dynalink defines several standard operations in its
 * {@link jdk.dynalink.StandardOperation} class. The linker for Java
 * objects can link all of these operations, and you are encouraged to at
 * minimum support and use these operations in your language too. The
 * standard operations {@code GET} and {@code SET} need to be combined with
 * at least one {@link jdk.dynalink.Namespace} to be useful, e.g. to express a
 * property getter, you'd use {@code StandardOperation.GET.withNamespace(StandardNamespace.PROPERTY)}.
 * Dynalink defines three standard namespaces in the {@link jdk.dynalink.StandardNamespace} class.
 * To associate a fixed name with an operation, you can use
 * {@link jdk.dynalink.NamedOperation} as in the previous example:
 * {@code StandardOperation.GET.withNamespace(StandardNamespace.PROPERTY).named("color")}
 * expresses a getter for the property named "color".
 * <h2>Operations on multiple namespaces</h2>
 * Some languages might not have separate namespaces on objects for
 * properties, elements, and methods, and a source language construct might
 * address several of them at once. Dynalink supports specifying multiple
 * {@link jdk.dynalink.Namespace} objects with {@link jdk.dynalink.NamespaceOperation}.
 * <h2>Language-specific linkers</h2>
 * Languages that define their own object model different than the JVM
 * class-based model and/or use their own type conversions will need to create
 * their own language-specific linkers. See the {@link jdk.dynalink.linker}
 * package and specifically the {@link jdk.dynalink.linker.GuardingDynamicLinker}
 * interface to get started.
 * <h2>Dynalink and Java objects</h2>
 * The {@code DynamicLinker} objects created by {@code DynamicLinkerFactory} by
 * default contain an internal instance of
 * {@code BeansLinker}, which is a language-specific linker
 * that implements the usual Java semantics for all of the above operations and
 * can link any Java object that no other language-specific linker has managed
 * to link. This way, all language runtimes have built-in interoperability with
 * ordinary Java objects. See {@link jdk.dynalink.beans.BeansLinker} for details
 * on how it links the various operations.
 * <h2>Cross-language interoperability</h2>
 * A {@code DynamicLinkerFactory} can be configured with a
 * {@linkplain jdk.dynalink.DynamicLinkerFactory#setClassLoader(ClassLoader) class
 * loader}. It will try to instantiate all
 * {@link jdk.dynalink.linker.GuardingDynamicLinkerExporter} classes visible to
 * that class loader and compose the linkers they provide into the
 * {@code DynamicLinker} it creates. This allows for interoperability between
 * languages: if you have two language runtimes A and B deployed in your JVM and
 * they export their linkers through the above mechanism, language runtime A
 * will have a language-specific linker instance from B and vice versa inside
 * their {@code DynamicLinker} objects. This means that if an object from
 * language runtime B gets passed to code from language runtime A, the linker
 * from B will get a chance to link the call site in A when it encounters the
 * object from B.
 *
 * @uses jdk.dynalink.linker.GuardingDynamicLinkerExporter
 *
 * @moduleGraph
 * @since 9
 */
module jdk.dynalink {
    requires java.logging;

    exports jdk.dynalink;
    exports jdk.dynalink.beans;
    exports jdk.dynalink.linker;
    exports jdk.dynalink.linker.support;
    exports jdk.dynalink.support;

    uses jdk.dynalink.linker.GuardingDynamicLinkerExporter;
}

