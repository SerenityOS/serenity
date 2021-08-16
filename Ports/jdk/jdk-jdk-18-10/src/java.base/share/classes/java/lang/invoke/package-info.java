/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * The {@code java.lang.invoke} package provides low-level primitives for interacting
 * with the Java Virtual Machine.
 *
 * <p>
 * As described in the Java Virtual Machine Specification, certain types in this package
 * are given special treatment by the virtual machine:
 * <ul>
 * <li>The classes {@link java.lang.invoke.MethodHandle MethodHandle} and
 * {@link java.lang.invoke.VarHandle VarHandle} contain
 * <a href="MethodHandle.html#sigpoly">signature polymorphic methods</a>
 * which can be linked regardless of their type descriptor.
 * Normally, method linkage requires exact matching of type descriptors.
 * </li>
 *
 * <li>The JVM bytecode format supports immediate constants of
 * the classes {@link java.lang.invoke.MethodHandle MethodHandle} and
 * {@link java.lang.invoke.MethodType MethodType}.
 * </li>
 *
 * <li>The {@code invokedynamic} instruction makes use of bootstrap {@code MethodHandle}
 * constants to dynamically resolve {@code CallSite} objects for custom method invocation
 * behavior.
 * </li>
 *
 * <li>The {@code ldc} instruction makes use of bootstrap {@code MethodHandle} constants
 * to dynamically resolve custom constant values.
 * </li>
 * </ul>
 *
 * <h2><a id="jvm_mods"></a>Dynamic resolution of call sites and constants</h2>
 * The following low-level information summarizes relevant parts of the
 * Java Virtual Machine specification.  For full details, please see the
 * current version of that specification.
 *
 * <h3><a id="indyinsn"></a>Dynamically-computed call sites</h3>
 * An {@code invokedynamic} instruction is originally in an unlinked state.
 * In this state, there is no target method for the instruction to invoke.
 * <p>
 * Before the JVM can execute an {@code invokedynamic} instruction,
 * the instruction must first be <em>linked</em>.
 * Linking is accomplished by calling a <em>bootstrap method</em>
 * which is given the static information content of the call,
 * and which must produce a {@link java.lang.invoke.CallSite}
 * that gives the behavior of the invocation.
 * <p>
 * Each {@code invokedynamic} instruction statically specifies its own
 * bootstrap method as a constant pool reference.
 * The constant pool reference also specifies the invocation's name and method type descriptor,
 * just like {@code invokestatic} and the other invoke instructions.
 *
 * <h3><a id="condycon"></a>Dynamically-computed constants</h3>
 * The constant pool may contain constants tagged {@code CONSTANT_Dynamic},
 * equipped with bootstrap methods which perform their resolution.
 * Such a <em>dynamic constant</em> is originally in an unresolved state.
 * Before the JVM can use a dynamically-computed constant, it must first be <em>resolved</em>.
 * Dynamically-computed constant resolution is accomplished by calling a <em>bootstrap method</em>
 * which is given the static information content of the constant,
 * and which must produce a value of the constant's statically declared type.
 * <p>
 * Each dynamically-computed constant statically specifies its own
 * bootstrap method as a constant pool reference.
 * The constant pool reference also specifies the constant's name and field type descriptor,
 * just like {@code getstatic} and the other field reference instructions.
 * (Roughly speaking, a dynamically-computed constant is to a dynamically-computed call site
 * as a {@code CONSTANT_Fieldref} is to a {@code CONSTANT_Methodref}.)
 *
 * <h3><a id="bsm"></a>Execution of bootstrap methods</h3>
 * Resolving a dynamically-computed call site or constant
 * starts with resolving constants from the constant pool for the
 * following items:
 * <ul>
 * <li>the bootstrap method, a {@code CONSTANT_MethodHandle}</li>
 * <li>the {@code Class} or {@code MethodType} derived from
 * type component of the {@code CONSTANT_NameAndType} descriptor</li>
 * <li>static arguments, if any (note that static arguments can themselves be
 * dynamically-computed constants)</li>
 * </ul>
 * <p>
 * The bootstrap method is then invoked, as if by
 * {@link java.lang.invoke.MethodHandle#invoke MethodHandle.invoke},
 * with the following arguments:
 * <ul>
 * <li>a {@code MethodHandles.Lookup}, which is a lookup object on the <em>caller class</em>
 * in which dynamically-computed constant or call site occurs</li>
 * <li>a {@code String}, the name mentioned in the {@code CONSTANT_NameAndType}</li>
 * <li>a {@code MethodType} or {@code Class}, the resolved type descriptor of the {@code CONSTANT_NameAndType}</li>
 * <li>a {@code Class}, the resolved type descriptor of the constant, if it is a dynamic constant </li>
 * <li>the additional resolved static arguments, if any</li>
 * </ul>
 * <p>
 * For a dynamically-computed call site, the returned result must be a non-null reference to a
 * {@link java.lang.invoke.CallSite CallSite}.
 * The type of the call site's target must be exactly equal to the type
 * derived from the invocation's type descriptor and passed to
 * the bootstrap method. If these conditions are not met, a {@code BootstrapMethodError} is thrown.
 * On success the call site then becomes permanently linked to the {@code invokedynamic}
 * instruction.
 * <p>
 * For a dynamically-computed constant, the first parameter of the bootstrap
 * method must be assignable to {@code MethodHandles.Lookup}. If this condition
 * is not met, a {@code BootstrapMethodError} is thrown.
 * On success the result of the bootstrap method is cached as the resolved
 * constant value.
 * <p>
 * If an exception, {@code E} say, occurs during execution of the bootstrap method, then
 * resolution fails and terminates abnormally. {@code E} is rethrown if the type of
 * {@code E} is {@code Error} or a subclass, otherwise a
 * {@code BootstrapMethodError} that wraps {@code E} is thrown.
 * If this happens, the same error will be thrown for all
 * subsequent attempts to execute the {@code invokedynamic} instruction or load the
 * dynamically-computed constant.
 *
 * <h3>Timing of resolution</h3>
 * An {@code invokedynamic} instruction is linked just before its first execution.
 * A dynamically-computed constant is resolved just before the first time it is used
 * (by pushing it on the stack or linking it as a bootstrap method parameter).
 * The bootstrap method call implementing the linkage occurs within
 * a thread that is attempting a first execution or first use.
 * <p>
 * If there are several such threads, the bootstrap method may be
 * invoked in several threads concurrently.
 * Therefore, bootstrap methods which access global application
 * data must take the usual precautions against race conditions.
 * In any case, every {@code invokedynamic} instruction is either
 * unlinked or linked to a unique {@code CallSite} object.
 * <p>
 * In an application which requires {@code invokedynamic} instructions with individually
 * mutable behaviors, their bootstrap methods should produce distinct
 * {@link java.lang.invoke.CallSite CallSite} objects, one for each linkage request.
 * Alternatively, an application can link a single {@code CallSite} object
 * to several {@code invokedynamic} instructions, in which case
 * a change to the target method will become visible at each of
 * the instructions.
 * <p>
 * If several threads simultaneously execute a bootstrap method for a single dynamically-computed
 * call site or constant, the JVM must choose one bootstrap method result and install it visibly to
 * all threads.  Any other bootstrap method calls are allowed to complete, but their
 * results are ignored.
 * <p style="font-size:smaller;">
 * <em>Discussion:</em>
 * These rules do not enable the JVM to share call sites,
 * or to issue &ldquo;causeless&rdquo; bootstrap method calls.
 * Every {@code invokedynamic} instruction transitions at most once from unlinked to linked,
 * just before its first invocation.
 * There is no way to undo the effect of a completed bootstrap method call.
 *
 * <h3>Types of bootstrap methods</h3>
 * For a dynamically-computed call site, the bootstrap method is invoked with parameter
 * types {@code MethodHandles.Lookup}, {@code String}, {@code MethodType}, and the types
 * of any static arguments; the return type is {@code CallSite}.
 * <p>
 * For a dynamically-computed constant, the bootstrap method is invoked with parameter types
 * {@code MethodHandles.Lookup}, {@code String}, {@code Class}, and the types of any
 * static arguments; the return type is the type represented by the {@code Class}.
 * <p>
 * Because {@link java.lang.invoke.MethodHandle#invoke MethodHandle.invoke} allows for
 * adaptations between the invoked method type and the bootstrap method handle's method type,
 * there is flexibility in the declaration of the bootstrap method.
 * For a dynamically-computed constant the first parameter type of the bootstrap method handle
 * must be assignable to {@code MethodHandles.Lookup}, other than that constraint the same degree
 * of flexibility applies to bootstrap methods of dynamically-computed call sites and
 * dynamically-computed constants.
 * Note: this constraint allows for the future possibility where the bootstrap method is
 * invoked with just the parameter types of static arguments, thereby supporting a wider
 * range of methods compatible with the static arguments (such as methods that don't declare
 * or require the lookup, name, and type meta-data parameters).
 * <p> For example, for dynamically-computed call site, the first argument
 * could be {@code Object} instead of {@code MethodHandles.Lookup}, and the return type
 * could also be {@code Object} instead of {@code CallSite}.
 * (Note that the types and number of the stacked arguments limit
 * the legal kinds of bootstrap methods to appropriately typed
 * static methods and constructors.)
 * <p>
 * If a pushed value is a primitive type, it may be converted to a reference by boxing conversion.
 * If the bootstrap method is a variable arity method (its modifier bit {@code 0x0080} is set),
 * then some or all of the arguments specified here may be collected into a trailing array parameter.
 * (This is not a special rule, but rather a useful consequence of the interaction
 * between {@code CONSTANT_MethodHandle} constants, the modifier bit for variable arity methods,
 * and the {@link java.lang.invoke.MethodHandle#asVarargsCollector asVarargsCollector} transformation.)
 * <p>
 * Given these rules, here are examples of legal bootstrap method declarations for
 * dynamically-computed call sites, given various numbers {@code N} of extra arguments.
 * The first row (marked {@code *}) will work for any number of extra arguments.
 * <table class="plain" style="vertical-align:top">
 * <caption style="display:none">Static argument types</caption>
 * <thead>
 * <tr><th scope="col">N</th><th scope="col">Sample bootstrap method</th></tr>
 * </thead>
 * <tbody>
 * <tr><th scope="row" style="font-weight:normal; vertical-align:top">*</th><td>
 *     <ul style="list-style:none; padding-left: 0; margin:0">
 *     <li>{@code CallSite bootstrap(Lookup caller, String name, MethodType type, Object... args)}
 *     <li>{@code CallSite bootstrap(Object... args)}
 *     <li>{@code CallSite bootstrap(Object caller, Object... nameAndTypeWithArgs)}
 *     </ul></td></tr>
 * <tr><th scope="row" style="font-weight:normal; vertical-align:top">0</th><td>
 *     <ul style="list-style:none; padding-left: 0; margin:0">
 *     <li>{@code CallSite bootstrap(Lookup caller, String name, MethodType type)}
 *     <li>{@code CallSite bootstrap(Lookup caller, Object... nameAndType)}
 *     </ul></td></tr>
 * <tr><th scope="row" style="font-weight:normal; vertical-align:top">1</th><td>
 *     {@code CallSite bootstrap(Lookup caller, String name, MethodType type, Object arg)}</td></tr>
 * <tr><th scope="row" style="font-weight:normal; vertical-align:top">2</th><td>
 *     <ul style="list-style:none; padding-left: 0; margin:0">
 *     <li>{@code CallSite bootstrap(Lookup caller, String name, MethodType type, Object... args)}
 *     <li>{@code CallSite bootstrap(Lookup caller, String name, MethodType type, String... args)}
 *     <li>{@code CallSite bootstrap(Lookup caller, String name, MethodType type, String x, int y)}
 *     </ul></td></tr>
 * </tbody>
 * </table>
 * The last example assumes that the extra arguments are of type
 * {@code String} and {@code Integer} (or {@code int}), respectively.
 * The second-to-last example assumes that all extra arguments are of type
 * {@code String}.
 * The other examples work with all types of extra arguments.  Note that all
 * the examples except the second and third also work with dynamically-computed
 * constants if the return type is changed to be compatible with the
 * constant's declared type (such as {@code Object}, which is always compatible).
 * <p>
 * Since dynamically-computed constants can be provided as static arguments to bootstrap
 * methods, there are no limitations on the types of bootstrap arguments.
 * However, arguments of type {@code boolean}, {@code byte}, {@code short}, or {@code char}
 * cannot be <em>directly</em> supplied by {@code CONSTANT_Integer}
 * constant pool entries, since the {@code asType} conversions do
 * not perform the necessary narrowing primitive conversions.
 * <p>
 * In the above examples, the return type is always {@code CallSite},
 * but that is not a necessary feature of bootstrap methods.
 * In the case of a dynamically-computed call site, the only requirement is that
 * the return type of the bootstrap method must be convertible
 * (using the {@code asType} conversions) to {@code CallSite}, which
 * means the bootstrap method return type might be {@code Object} or
 * {@code ConstantCallSite}.
 * In the case of a dynamically-resolved constant, the return type of the bootstrap
 * method must be convertible to the type of the constant, as
 * represented by its field type descriptor.  For example, if the
 * dynamic constant has a field type descriptor of {@code "C"}
 * ({@code char}) then the bootstrap method return type could be
 * {@code Object}, {@code Character}, or {@code char}, but not
 * {@code int} or {@code Integer}.
 *
 * @author John Rose, JSR 292 EG
 * @since 1.7
 */

package java.lang.invoke;
