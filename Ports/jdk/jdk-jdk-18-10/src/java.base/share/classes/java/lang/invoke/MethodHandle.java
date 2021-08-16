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

package java.lang.invoke;


import jdk.internal.vm.annotation.IntrinsicCandidate;

import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.invoke.MethodHandleInfo.*;
import static java.lang.invoke.MethodHandleStatics.*;
import static java.lang.invoke.MethodHandles.Lookup.IMPL_LOOKUP;

/**
 * A method handle is a typed, directly executable reference to an underlying method,
 * constructor, field, or similar low-level operation, with optional
 * transformations of arguments or return values.
 * These transformations are quite general, and include such patterns as
 * {@linkplain #asType conversion},
 * {@linkplain #bindTo insertion},
 * {@linkplain java.lang.invoke.MethodHandles#dropArguments deletion},
 * and {@linkplain java.lang.invoke.MethodHandles#filterArguments substitution}.
 *
 * <h2>Method handle contents</h2>
 * Method handles are dynamically and strongly typed according to their parameter and return types.
 * They are not distinguished by the name or the defining class of their underlying methods.
 * A method handle must be invoked using a symbolic type descriptor which matches
 * the method handle's own {@linkplain #type() type descriptor}.
 * <p>
 * Every method handle reports its type descriptor via the {@link #type() type} accessor.
 * This type descriptor is a {@link java.lang.invoke.MethodType MethodType} object,
 * whose structure is a series of classes, one of which is
 * the return type of the method (or {@code void.class} if none).
 * <p>
 * A method handle's type controls the types of invocations it accepts,
 * and the kinds of transformations that apply to it.
 * <p>
 * A method handle contains a pair of special invoker methods
 * called {@link #invokeExact invokeExact} and {@link #invoke invoke}.
 * Both invoker methods provide direct access to the method handle's
 * underlying method, constructor, field, or other operation,
 * as modified by transformations of arguments and return values.
 * Both invokers accept calls which exactly match the method handle's own type.
 * The plain, inexact invoker also accepts a range of other call types.
 * <p>
 * Method handles are immutable and have no visible state.
 * Of course, they can be bound to underlying methods or data which exhibit state.
 * With respect to the Java Memory Model, any method handle will behave
 * as if all of its (internal) fields are final variables.  This means that any method
 * handle made visible to the application will always be fully formed.
 * This is true even if the method handle is published through a shared
 * variable in a data race.
 * <p>
 * Method handles cannot be subclassed by the user.
 * Implementations may (or may not) create internal subclasses of {@code MethodHandle}
 * which may be visible via the {@link java.lang.Object#getClass Object.getClass}
 * operation.  The programmer should not draw conclusions about a method handle
 * from its specific class, as the method handle class hierarchy (if any)
 * may change from time to time or across implementations from different vendors.
 *
 * <h2>Method handle compilation</h2>
 * A Java method call expression naming {@code invokeExact} or {@code invoke}
 * can invoke a method handle from Java source code.
 * From the viewpoint of source code, these methods can take any arguments
 * and their result can be cast to any return type.
 * Formally this is accomplished by giving the invoker methods
 * {@code Object} return types and variable arity {@code Object} arguments,
 * but they have an additional quality called <em>signature polymorphism</em>
 * which connects this freedom of invocation directly to the JVM execution stack.
 * <p>
 * As is usual with virtual methods, source-level calls to {@code invokeExact}
 * and {@code invoke} compile to an {@code invokevirtual} instruction.
 * More unusually, the compiler must record the actual argument types,
 * and may not perform method invocation conversions on the arguments.
 * Instead, it must generate instructions that push them on the stack according
 * to their own unconverted types.  The method handle object itself is pushed on
 * the stack before the arguments.
 * The compiler then generates an {@code invokevirtual} instruction that invokes
 * the method handle with a symbolic type descriptor which describes the argument
 * and return types.
 * <p>
 * To issue a complete symbolic type descriptor, the compiler must also determine
 * the return type.  This is based on a cast on the method invocation expression,
 * if there is one, or else {@code Object} if the invocation is an expression,
 * or else {@code void} if the invocation is a statement.
 * The cast may be to a primitive type (but not {@code void}).
 * <p>
 * As a corner case, an uncasted {@code null} argument is given
 * a symbolic type descriptor of {@code java.lang.Void}.
 * The ambiguity with the type {@code Void} is harmless, since there are no references of type
 * {@code Void} except the null reference.
 *
 * <h2>Method handle invocation</h2>
 * The first time an {@code invokevirtual} instruction is executed
 * it is linked by symbolically resolving the names in the instruction
 * and verifying that the method call is statically legal.
 * This also holds for calls to {@code invokeExact} and {@code invoke}.
 * In this case, the symbolic type descriptor emitted by the compiler is checked for
 * correct syntax, and names it contains are resolved.
 * Thus, an {@code invokevirtual} instruction which invokes
 * a method handle will always link, as long
 * as the symbolic type descriptor is syntactically well-formed
 * and the types exist.
 * <p>
 * When the {@code invokevirtual} is executed after linking,
 * the receiving method handle's type is first checked by the JVM
 * to ensure that it matches the symbolic type descriptor.
 * If the type match fails, it means that the method which the
 * caller is invoking is not present on the individual
 * method handle being invoked.
 * <p>
 * In the case of {@code invokeExact}, the type descriptor of the invocation
 * (after resolving symbolic type names) must exactly match the method type
 * of the receiving method handle.
 * In the case of plain, inexact {@code invoke}, the resolved type descriptor
 * must be a valid argument to the receiver's {@link #asType asType} method.
 * Thus, plain {@code invoke} is more permissive than {@code invokeExact}.
 * <p>
 * After type matching, a call to {@code invokeExact} directly
 * and immediately invoke the method handle's underlying method
 * (or other behavior, as the case may be).
 * <p>
 * A call to plain {@code invoke} works the same as a call to
 * {@code invokeExact}, if the symbolic type descriptor specified by the caller
 * exactly matches the method handle's own type.
 * If there is a type mismatch, {@code invoke} attempts
 * to adjust the type of the receiving method handle,
 * as if by a call to {@link #asType asType},
 * to obtain an exactly invokable method handle {@code M2}.
 * This allows a more powerful negotiation of method type
 * between caller and callee.
 * <p>
 * (<em>Note:</em> The adjusted method handle {@code M2} is not directly observable,
 * and implementations are therefore not required to materialize it.)
 *
 * <h2>Invocation checking</h2>
 * In typical programs, method handle type matching will usually succeed.
 * But if a match fails, the JVM will throw a {@link WrongMethodTypeException},
 * either directly (in the case of {@code invokeExact}) or indirectly as if
 * by a failed call to {@code asType} (in the case of {@code invoke}).
 * <p>
 * Thus, a method type mismatch which might show up as a linkage error
 * in a statically typed program can show up as
 * a dynamic {@code WrongMethodTypeException}
 * in a program which uses method handles.
 * <p>
 * Because method types contain "live" {@code Class} objects,
 * method type matching takes into account both type names and class loaders.
 * Thus, even if a method handle {@code M} is created in one
 * class loader {@code L1} and used in another {@code L2},
 * method handle calls are type-safe, because the caller's symbolic type
 * descriptor, as resolved in {@code L2},
 * is matched against the original callee method's symbolic type descriptor,
 * as resolved in {@code L1}.
 * The resolution in {@code L1} happens when {@code M} is created
 * and its type is assigned, while the resolution in {@code L2} happens
 * when the {@code invokevirtual} instruction is linked.
 * <p>
 * Apart from type descriptor checks,
 * a method handle's capability to call its underlying method is unrestricted.
 * If a method handle is formed on a non-public method by a class
 * that has access to that method, the resulting handle can be used
 * in any place by any caller who receives a reference to it.
 * <p>
 * Unlike with the Core Reflection API, where access is checked every time
 * a reflective method is invoked,
 * method handle access checking is performed
 * <a href="MethodHandles.Lookup.html#access">when the method handle is created</a>.
 * In the case of {@code ldc} (see below), access checking is performed as part of linking
 * the constant pool entry underlying the constant method handle.
 * <p>
 * Thus, handles to non-public methods, or to methods in non-public classes,
 * should generally be kept secret.
 * They should not be passed to untrusted code unless their use from
 * the untrusted code would be harmless.
 *
 * <h2>Method handle creation</h2>
 * Java code can create a method handle that directly accesses
 * any method, constructor, or field that is accessible to that code.
 * This is done via a reflective, capability-based API called
 * {@link java.lang.invoke.MethodHandles.Lookup MethodHandles.Lookup}.
 * For example, a static method handle can be obtained
 * from {@link java.lang.invoke.MethodHandles.Lookup#findStatic Lookup.findStatic}.
 * There are also conversion methods from Core Reflection API objects,
 * such as {@link java.lang.invoke.MethodHandles.Lookup#unreflect Lookup.unreflect}.
 * <p>
 * Like classes and strings, method handles that correspond to accessible
 * fields, methods, and constructors can also be represented directly
 * in a class file's constant pool as constants to be loaded by {@code ldc} bytecodes.
 * A new type of constant pool entry, {@code CONSTANT_MethodHandle},
 * refers directly to an associated {@code CONSTANT_Methodref},
 * {@code CONSTANT_InterfaceMethodref}, or {@code CONSTANT_Fieldref}
 * constant pool entry.
 * (For full details on method handle constants, see sections {@jvms
 * 4.4.8} and {@jvms 5.4.3.5} of the Java Virtual Machine
 * Specification.)
 * <p>
 * Method handles produced by lookups or constant loads from methods or
 * constructors with the variable arity modifier bit ({@code 0x0080})
 * have a corresponding variable arity, as if they were defined with
 * the help of {@link #asVarargsCollector asVarargsCollector}
 * or {@link #withVarargs withVarargs}.
 * <p>
 * A method reference may refer either to a static or non-static method.
 * In the non-static case, the method handle type includes an explicit
 * receiver argument, prepended before any other arguments.
 * In the method handle's type, the initial receiver argument is typed
 * according to the class under which the method was initially requested.
 * (E.g., if a non-static method handle is obtained via {@code ldc},
 * the type of the receiver is the class named in the constant pool entry.)
 * <p>
 * Method handle constants are subject to the same link-time access checks
 * their corresponding bytecode instructions, and the {@code ldc} instruction
 * will throw corresponding linkage errors if the bytecode behaviors would
 * throw such errors.
 * <p>
 * As a corollary of this, access to protected members is restricted
 * to receivers only of the accessing class, or one of its subclasses,
 * and the accessing class must in turn be a subclass (or package sibling)
 * of the protected member's defining class.
 * If a method reference refers to a protected non-static method or field
 * of a class outside the current package, the receiver argument will
 * be narrowed to the type of the accessing class.
 * <p>
 * When a method handle to a virtual method is invoked, the method is
 * always looked up in the receiver (that is, the first argument).
 * <p>
 * A non-virtual method handle to a specific virtual method implementation
 * can also be created.  These do not perform virtual lookup based on
 * receiver type.  Such a method handle simulates the effect of
 * an {@code invokespecial} instruction to the same method.
 * A non-virtual method handle can also be created to simulate the effect
 * of an {@code invokevirtual} or {@code invokeinterface} instruction on
 * a private method (as applicable).
 *
 * <h2>Usage examples</h2>
 * Here are some examples of usage:
 * <blockquote><pre>{@code
Object x, y; String s; int i;
MethodType mt; MethodHandle mh;
MethodHandles.Lookup lookup = MethodHandles.lookup();
// mt is (char,char)String
mt = MethodType.methodType(String.class, char.class, char.class);
mh = lookup.findVirtual(String.class, "replace", mt);
s = (String) mh.invokeExact("daddy",'d','n');
// invokeExact(Ljava/lang/String;CC)Ljava/lang/String;
assertEquals(s, "nanny");
// weakly typed invocation (using MHs.invoke)
s = (String) mh.invokeWithArguments("sappy", 'p', 'v');
assertEquals(s, "savvy");
// mt is (Object[])List
mt = MethodType.methodType(java.util.List.class, Object[].class);
mh = lookup.findStatic(java.util.Arrays.class, "asList", mt);
assert(mh.isVarargsCollector());
x = mh.invoke("one", "two");
// invoke(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;
assertEquals(x, java.util.Arrays.asList("one","two"));
// mt is (Object,Object,Object)Object
mt = MethodType.genericMethodType(3);
mh = mh.asType(mt);
x = mh.invokeExact((Object)1, (Object)2, (Object)3);
// invokeExact(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
assertEquals(x, java.util.Arrays.asList(1,2,3));
// mt is ()int
mt = MethodType.methodType(int.class);
mh = lookup.findVirtual(java.util.List.class, "size", mt);
i = (int) mh.invokeExact(java.util.Arrays.asList(1,2,3));
// invokeExact(Ljava/util/List;)I
assert(i == 3);
mt = MethodType.methodType(void.class, String.class);
mh = lookup.findVirtual(java.io.PrintStream.class, "println", mt);
mh.invokeExact(System.out, "Hello, world.");
// invokeExact(Ljava/io/PrintStream;Ljava/lang/String;)V
 * }</pre></blockquote>
 * Each of the above calls to {@code invokeExact} or plain {@code invoke}
 * generates a single invokevirtual instruction with
 * the symbolic type descriptor indicated in the following comment.
 * In these examples, the helper method {@code assertEquals} is assumed to
 * be a method which calls {@link java.util.Objects#equals(Object,Object) Objects.equals}
 * on its arguments, and asserts that the result is true.
 *
 * <h2>Exceptions</h2>
 * The methods {@code invokeExact} and {@code invoke} are declared
 * to throw {@link java.lang.Throwable Throwable},
 * which is to say that there is no static restriction on what a method handle
 * can throw.  Since the JVM does not distinguish between checked
 * and unchecked exceptions (other than by their class, of course),
 * there is no particular effect on bytecode shape from ascribing
 * checked exceptions to method handle invocations.  But in Java source
 * code, methods which perform method handle calls must either explicitly
 * throw {@code Throwable}, or else must catch all
 * throwables locally, rethrowing only those which are legal in the context,
 * and wrapping ones which are illegal.
 *
 * <h2><a id="sigpoly"></a>Signature polymorphism</h2>
 * The unusual compilation and linkage behavior of
 * {@code invokeExact} and plain {@code invoke}
 * is referenced by the term <em>signature polymorphism</em>.
 * As defined in the Java Language Specification,
 * a signature polymorphic method is one which can operate with
 * any of a wide range of call signatures and return types.
 * <p>
 * In source code, a call to a signature polymorphic method will
 * compile, regardless of the requested symbolic type descriptor.
 * As usual, the Java compiler emits an {@code invokevirtual}
 * instruction with the given symbolic type descriptor against the named method.
 * The unusual part is that the symbolic type descriptor is derived from
 * the actual argument and return types, not from the method declaration.
 * <p>
 * When the JVM processes bytecode containing signature polymorphic calls,
 * it will successfully link any such call, regardless of its symbolic type descriptor.
 * (In order to retain type safety, the JVM will guard such calls with suitable
 * dynamic type checks, as described elsewhere.)
 * <p>
 * Bytecode generators, including the compiler back end, are required to emit
 * untransformed symbolic type descriptors for these methods.
 * Tools which determine symbolic linkage are required to accept such
 * untransformed descriptors, without reporting linkage errors.
 *
 * <h2>Interoperation between method handles and the Core Reflection API</h2>
 * Using factory methods in the {@link java.lang.invoke.MethodHandles.Lookup Lookup} API,
 * any class member represented by a Core Reflection API object
 * can be converted to a behaviorally equivalent method handle.
 * For example, a reflective {@link java.lang.reflect.Method Method} can
 * be converted to a method handle using
 * {@link java.lang.invoke.MethodHandles.Lookup#unreflect Lookup.unreflect}.
 * The resulting method handles generally provide more direct and efficient
 * access to the underlying class members.
 * <p>
 * As a special case,
 * when the Core Reflection API is used to view the signature polymorphic
 * methods {@code invokeExact} or plain {@code invoke} in this class,
 * they appear as ordinary non-polymorphic methods.
 * Their reflective appearance, as viewed by
 * {@link java.lang.Class#getDeclaredMethod Class.getDeclaredMethod},
 * is unaffected by their special status in this API.
 * For example, {@link java.lang.reflect.Method#getModifiers Method.getModifiers}
 * will report exactly those modifier bits required for any similarly
 * declared method, including in this case {@code native} and {@code varargs} bits.
 * <p>
 * As with any reflected method, these methods (when reflected) may be
 * invoked via {@link java.lang.reflect.Method#invoke java.lang.reflect.Method.invoke}.
 * However, such reflective calls do not result in method handle invocations.
 * Such a call, if passed the required argument
 * (a single one, of type {@code Object[]}), will ignore the argument and
 * will throw an {@code UnsupportedOperationException}.
 * <p>
 * Since {@code invokevirtual} instructions can natively
 * invoke method handles under any symbolic type descriptor, this reflective view conflicts
 * with the normal presentation of these methods via bytecodes.
 * Thus, these two native methods, when reflectively viewed by
 * {@code Class.getDeclaredMethod}, may be regarded as placeholders only.
 * <p>
 * In order to obtain an invoker method for a particular type descriptor,
 * use {@link java.lang.invoke.MethodHandles#exactInvoker MethodHandles.exactInvoker},
 * or {@link java.lang.invoke.MethodHandles#invoker MethodHandles.invoker}.
 * The {@link java.lang.invoke.MethodHandles.Lookup#findVirtual Lookup.findVirtual}
 * API is also able to return a method handle
 * to call {@code invokeExact} or plain {@code invoke},
 * for any specified type descriptor .
 *
 * <h2>Interoperation between method handles and Java generics</h2>
 * A method handle can be obtained on a method, constructor, or field
 * which is declared with Java generic types.
 * As with the Core Reflection API, the type of the method handle
 * will be constructed from the erasure of the source-level type.
 * When a method handle is invoked, the types of its arguments
 * or the return value cast type may be generic types or type instances.
 * If this occurs, the compiler will replace those
 * types by their erasures when it constructs the symbolic type descriptor
 * for the {@code invokevirtual} instruction.
 * <p>
 * Method handles do not represent
 * their function-like types in terms of Java parameterized (generic) types,
 * because there are three mismatches between function-like types and parameterized
 * Java types.
 * <ul>
 * <li>Method types range over all possible arities,
 * from no arguments to up to the  <a href="MethodHandle.html#maxarity">maximum number</a> of allowed arguments.
 * Generics are not variadic, and so cannot represent this.</li>
 * <li>Method types can specify arguments of primitive types,
 * which Java generic types cannot range over.</li>
 * <li>Higher order functions over method handles (combinators) are
 * often generic across a wide range of function types, including
 * those of multiple arities.  It is impossible to represent such
 * genericity with a Java type parameter.</li>
 * </ul>
 *
 * <h2><a id="maxarity"></a>Arity limits</h2>
 * The JVM imposes on all methods and constructors of any kind an absolute
 * limit of 255 stacked arguments.  This limit can appear more restrictive
 * in certain cases:
 * <ul>
 * <li>A {@code long} or {@code double} argument counts (for purposes of arity limits) as two argument slots.
 * <li>A non-static method consumes an extra argument for the object on which the method is called.
 * <li>A constructor consumes an extra argument for the object which is being constructed.
 * <li>Since a method handle&rsquo;s {@code invoke} method (or other signature-polymorphic method) is non-virtual,
 *     it consumes an extra argument for the method handle itself, in addition to any non-virtual receiver object.
 * </ul>
 * These limits imply that certain method handles cannot be created, solely because of the JVM limit on stacked arguments.
 * For example, if a static JVM method accepts exactly 255 arguments, a method handle cannot be created for it.
 * Attempts to create method handles with impossible method types lead to an {@link IllegalArgumentException}.
 * In particular, a method handle&rsquo;s type must not have an arity of the exact maximum 255.
 *
 * @see MethodType
 * @see MethodHandles
 * @author John Rose, JSR 292 EG
 * @since 1.7
 */
public abstract class MethodHandle implements Constable {

    /**
     * Internal marker interface which distinguishes (to the Java compiler)
     * those methods which are <a href="MethodHandle.html#sigpoly">signature polymorphic</a>.
     */
    @java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @interface PolymorphicSignature { }

    private final MethodType type;
    /*private*/
    final LambdaForm form;
    // form is not private so that invokers can easily fetch it
    /*private*/
    MethodHandle asTypeCache;
    // asTypeCache is not private so that invokers can easily fetch it

    private byte customizationCount;

    /**
     * Reports the type of this method handle.
     * Every invocation of this method handle via {@code invokeExact} must exactly match this type.
     * @return the method handle type
     */
    public MethodType type() {
        return type;
    }

    /**
     * Package-private constructor for the method handle implementation hierarchy.
     * Method handle inheritance will be contained completely within
     * the {@code java.lang.invoke} package.
     */
    // @param type type (permanently assigned) of the new method handle
    /*non-public*/
    MethodHandle(MethodType type, LambdaForm form) {
        this.type = Objects.requireNonNull(type);
        this.form = Objects.requireNonNull(form).uncustomize();

        this.form.prepare();  // TO DO:  Try to delay this step until just before invocation.
    }

    /**
     * Invokes the method handle, allowing any caller type descriptor, but requiring an exact type match.
     * The symbolic type descriptor at the call site of {@code invokeExact} must
     * exactly match this method handle's {@link #type() type}.
     * No conversions are allowed on arguments or return values.
     * <p>
     * When this method is observed via the Core Reflection API,
     * it will appear as a single native method, taking an object array and returning an object.
     * If this native method is invoked directly via
     * {@link java.lang.reflect.Method#invoke java.lang.reflect.Method.invoke}, via JNI,
     * or indirectly via {@link java.lang.invoke.MethodHandles.Lookup#unreflect Lookup.unreflect},
     * it will throw an {@code UnsupportedOperationException}.
     * @param args the signature-polymorphic parameter list, statically represented using varargs
     * @return the signature-polymorphic result, statically represented using {@code Object}
     * @throws WrongMethodTypeException if the target's type is not identical with the caller's symbolic type descriptor
     * @throws Throwable anything thrown by the underlying method propagates unchanged through the method handle call
     */
    @IntrinsicCandidate
    public final native @PolymorphicSignature Object invokeExact(Object... args) throws Throwable;

    /**
     * Invokes the method handle, allowing any caller type descriptor,
     * and optionally performing conversions on arguments and return values.
     * <p>
     * If the call site's symbolic type descriptor exactly matches this method handle's {@link #type() type},
     * the call proceeds as if by {@link #invokeExact invokeExact}.
     * <p>
     * Otherwise, the call proceeds as if this method handle were first
     * adjusted by calling {@link #asType asType} to adjust this method handle
     * to the required type, and then the call proceeds as if by
     * {@link #invokeExact invokeExact} on the adjusted method handle.
     * <p>
     * There is no guarantee that the {@code asType} call is actually made.
     * If the JVM can predict the results of making the call, it may perform
     * adaptations directly on the caller's arguments,
     * and call the target method handle according to its own exact type.
     * <p>
     * The resolved type descriptor at the call site of {@code invoke} must
     * be a valid argument to the receivers {@code asType} method.
     * In particular, the caller must specify the same argument arity
     * as the callee's type,
     * if the callee is not a {@linkplain #asVarargsCollector variable arity collector}.
     * <p>
     * When this method is observed via the Core Reflection API,
     * it will appear as a single native method, taking an object array and returning an object.
     * If this native method is invoked directly via
     * {@link java.lang.reflect.Method#invoke java.lang.reflect.Method.invoke}, via JNI,
     * or indirectly via {@link java.lang.invoke.MethodHandles.Lookup#unreflect Lookup.unreflect},
     * it will throw an {@code UnsupportedOperationException}.
     * @param args the signature-polymorphic parameter list, statically represented using varargs
     * @return the signature-polymorphic result, statically represented using {@code Object}
     * @throws WrongMethodTypeException if the target's type cannot be adjusted to the caller's symbolic type descriptor
     * @throws ClassCastException if the target's type can be adjusted to the caller, but a reference cast fails
     * @throws Throwable anything thrown by the underlying method propagates unchanged through the method handle call
     */
    @IntrinsicCandidate
    public final native @PolymorphicSignature Object invoke(Object... args) throws Throwable;

    /**
     * Private method for trusted invocation of a method handle respecting simplified signatures.
     * Type mismatches will not throw {@code WrongMethodTypeException}, but could crash the JVM.
     * <p>
     * The caller signature is restricted to the following basic types:
     * Object, int, long, float, double, and void return.
     * <p>
     * The caller is responsible for maintaining type correctness by ensuring
     * that the each outgoing argument value is a member of the range of the corresponding
     * callee argument type.
     * (The caller should therefore issue appropriate casts and integer narrowing
     * operations on outgoing argument values.)
     * The caller can assume that the incoming result value is part of the range
     * of the callee's return type.
     * @param args the signature-polymorphic parameter list, statically represented using varargs
     * @return the signature-polymorphic result, statically represented using {@code Object}
     */
    @IntrinsicCandidate
    /*non-public*/
    final native @PolymorphicSignature Object invokeBasic(Object... args) throws Throwable;

    /**
     * Private method for trusted invocation of a MemberName of kind {@code REF_invokeVirtual}.
     * The caller signature is restricted to basic types as with {@code invokeBasic}.
     * The trailing (not leading) argument must be a MemberName.
     * @param args the signature-polymorphic parameter list, statically represented using varargs
     * @return the signature-polymorphic result, statically represented using {@code Object}
     */
    @IntrinsicCandidate
    /*non-public*/
    static native @PolymorphicSignature Object linkToVirtual(Object... args) throws Throwable;

    /**
     * Private method for trusted invocation of a MemberName of kind {@code REF_invokeStatic}.
     * The caller signature is restricted to basic types as with {@code invokeBasic}.
     * The trailing (not leading) argument must be a MemberName.
     * @param args the signature-polymorphic parameter list, statically represented using varargs
     * @return the signature-polymorphic result, statically represented using {@code Object}
     */
    @IntrinsicCandidate
    /*non-public*/
    static native @PolymorphicSignature Object linkToStatic(Object... args) throws Throwable;

    /**
     * Private method for trusted invocation of a MemberName of kind {@code REF_invokeSpecial}.
     * The caller signature is restricted to basic types as with {@code invokeBasic}.
     * The trailing (not leading) argument must be a MemberName.
     * @param args the signature-polymorphic parameter list, statically represented using varargs
     * @return the signature-polymorphic result, statically represented using {@code Object}
     */
    @IntrinsicCandidate
    /*non-public*/
    static native @PolymorphicSignature Object linkToSpecial(Object... args) throws Throwable;

    /**
     * Private method for trusted invocation of a MemberName of kind {@code REF_invokeInterface}.
     * The caller signature is restricted to basic types as with {@code invokeBasic}.
     * The trailing (not leading) argument must be a MemberName.
     * @param args the signature-polymorphic parameter list, statically represented using varargs
     * @return the signature-polymorphic result, statically represented using {@code Object}
     */
    @IntrinsicCandidate
    /*non-public*/
    static native @PolymorphicSignature Object linkToInterface(Object... args) throws Throwable;

    /** TODO */
    @IntrinsicCandidate
    /*non-public*/ static native @PolymorphicSignature Object linkToNative(Object... args) throws Throwable;

    /**
     * Performs a variable arity invocation, passing the arguments in the given array
     * to the method handle, as if via an inexact {@link #invoke invoke} from a call site
     * which mentions only the type {@code Object}, and whose actual argument count is the length
     * of the argument array.
     * <p>
     * Specifically, execution proceeds as if by the following steps,
     * although the methods are not guaranteed to be called if the JVM
     * can predict their effects.
     * <ul>
     * <li>Determine the length of the argument array as {@code N}.
     *     For a null reference, {@code N=0}. </li>
     * <li>Collect the {@code N} elements of the array as a logical
     *     argument list, each argument statically typed as an {@code Object}. </li>
     * <li>Determine, as {@code M}, the parameter count of the type of this
     *     method handle. </li>
     * <li>Determine the general type {@code TN} of {@code N} arguments or
     *     {@code M} arguments, if smaller than {@code N}, as
     *     {@code TN=MethodType.genericMethodType(Math.min(N, M))}.</li>
     * <li>If {@code N} is greater than {@code M}, perform the following
     *     checks and actions to shorten the logical argument list: <ul>
     *     <li>Check that this method handle has variable arity with a
     *         {@linkplain MethodType#lastParameterType trailing parameter}
     *         of some array type {@code A[]}.  If not, fail with a
     *         {@code WrongMethodTypeException}. </li>
     *     <li>Collect the trailing elements (there are {@code N-M+1} of them)
     *         from the logical argument list into a single array of
     *         type {@code A[]}, using {@code asType} conversions to
     *         convert each trailing argument to type {@code A}. </li>
     *     <li>If any of these conversions proves impossible, fail with either
     *         a {@code ClassCastException} if any trailing element cannot be
     *         cast to {@code A} or a {@code NullPointerException} if any
     *         trailing element is {@code null} and {@code A} is not a reference
     *         type. </li>
     *     <li>Replace the logical arguments gathered into the array of
     *         type {@code A[]} with the array itself, thus shortening
     *         the argument list to length {@code M}. This final argument
     *         retains the static type {@code A[]}.</li>
     *     <li>Adjust the type {@code TN} by changing the {@code N}th
     *         parameter type from {@code Object} to {@code A[]}.
     *     </ul>
     * <li>Force the original target method handle {@code MH0} to the
     *     required type, as {@code MH1 = MH0.asType(TN)}. </li>
     * <li>Spread the argument list into {@code N} separate arguments {@code A0, ...}. </li>
     * <li>Invoke the type-adjusted method handle on the unpacked arguments:
     *     MH1.invokeExact(A0, ...). </li>
     * <li>Take the return value as an {@code Object} reference. </li>
     * </ul>
     * <p>
     * If the target method handle has variable arity, and the argument list is longer
     * than that arity, the excess arguments, starting at the position of the trailing
     * array argument, will be gathered (if possible, as if by {@code asType} conversions)
     * into an array of the appropriate type, and invocation will proceed on the
     * shortened argument list.
     * In this way, <em>jumbo argument lists</em> which would spread into more
     * than 254 slots can still be processed uniformly.
     * <p>
     * Unlike the {@link #invoke(Object...) generic} invocation mode, which can
     * "recycle" an array argument, passing it directly to the target method,
     * this invocation mode <em>always</em> creates a new array parameter, even
     * if the original array passed to {@code invokeWithArguments} would have
     * been acceptable as a direct argument to the target method.
     * Even if the number {@code M} of actual arguments is the arity {@code N},
     * and the last argument is dynamically a suitable array of type {@code A[]},
     * it will still be boxed into a new one-element array, since the call
     * site statically types the argument as {@code Object}, not an array type.
     * This is not a special rule for this method, but rather a regular effect
     * of the {@linkplain #asVarargsCollector rules for variable-arity invocation}.
     * <p>
     * Because of the action of the {@code asType} step, the following argument
     * conversions are applied as necessary:
     * <ul>
     * <li>reference casting
     * <li>unboxing
     * <li>widening primitive conversions
     * <li>variable arity conversion
     * </ul>
     * <p>
     * The result returned by the call is boxed if it is a primitive,
     * or forced to null if the return type is void.
     * <p>
     * Unlike the signature polymorphic methods {@code invokeExact} and {@code invoke},
     * {@code invokeWithArguments} can be accessed normally via the Core Reflection API and JNI.
     * It can therefore be used as a bridge between native or reflective code and method handles.
     * @apiNote
     * This call is approximately equivalent to the following code:
     * <blockquote><pre>{@code
     * // for jumbo argument lists, adapt varargs explicitly:
     * int N = (arguments == null? 0: arguments.length);
     * int M = this.type.parameterCount();
     * int MAX_SAFE = 127;  // 127 longs require 254 slots, which is OK
     * if (N > MAX_SAFE && N > M && this.isVarargsCollector()) {
     *   Class<?> arrayType = this.type().lastParameterType();
     *   Class<?> elemType = arrayType.getComponentType();
     *   if (elemType != null) {
     *     Object args2 = Array.newInstance(elemType, M);
     *     MethodHandle arraySetter = MethodHandles.arrayElementSetter(arrayType);
     *     for (int i = 0; i < M; i++) {
     *       arraySetter.invoke(args2, i, arguments[M-1 + i]);
     *     }
     *     arguments = Arrays.copyOf(arguments, M);
     *     arguments[M-1] = args2;
     *     return this.asFixedArity().invokeWithArguments(arguments);
     *   }
     * } // done with explicit varargs processing
     *
     * // Handle fixed arity and non-jumbo variable arity invocation.
     * MethodHandle invoker = MethodHandles.spreadInvoker(this.type(), 0);
     * Object result = invoker.invokeExact(this, arguments);
     * }</pre></blockquote>
     *
     * @param arguments the arguments to pass to the target
     * @return the result returned by the target
     * @throws ClassCastException if an argument cannot be converted by reference casting
     * @throws WrongMethodTypeException if the target's type cannot be adjusted to take the given number of {@code Object} arguments
     * @throws Throwable anything thrown by the target method invocation
     * @see MethodHandles#spreadInvoker
     */
    public Object invokeWithArguments(Object... arguments) throws Throwable {
        // Note: Jumbo argument lists are handled in the variable-arity subclass.
        MethodType invocationType = MethodType.genericMethodType(arguments == null ? 0 : arguments.length);
        return invocationType.invokers().spreadInvoker(0).invokeExact(asType(invocationType), arguments);
    }

    /**
     * Performs a variable arity invocation, passing the arguments in the given list
     * to the method handle, as if via an inexact {@link #invoke invoke} from a call site
     * which mentions only the type {@code Object}, and whose actual argument count is the length
     * of the argument list.
     * <p>
     * This method is also equivalent to the following code:
     * <blockquote><pre>{@code
     *   invokeWithArguments(arguments.toArray())
     * }</pre></blockquote>
     * <p>
     * Jumbo-sized lists are acceptable if this method handle has variable arity.
     * See {@link #invokeWithArguments(Object[])} for details.
     *
     * @param arguments the arguments to pass to the target
     * @return the result returned by the target
     * @throws NullPointerException if {@code arguments} is a null reference
     * @throws ClassCastException if an argument cannot be converted by reference casting
     * @throws WrongMethodTypeException if the target's type cannot be adjusted to take the given number of {@code Object} arguments
     * @throws Throwable anything thrown by the target method invocation
     */
    public Object invokeWithArguments(java.util.List<?> arguments) throws Throwable {
        return invokeWithArguments(arguments.toArray());
    }

    /**
     * Produces an adapter method handle which adapts the type of the
     * current method handle to a new type.
     * The resulting method handle is guaranteed to report a type
     * which is equal to the desired new type.
     * <p>
     * If the original type and new type are equal, returns {@code this}.
     * <p>
     * The new method handle, when invoked, will perform the following
     * steps:
     * <ul>
     * <li>Convert the incoming argument list to match the original
     *     method handle's argument list.
     * <li>Invoke the original method handle on the converted argument list.
     * <li>Convert any result returned by the original method handle
     *     to the return type of new method handle.
     * </ul>
     * <p>
     * This method provides the crucial behavioral difference between
     * {@link #invokeExact invokeExact} and plain, inexact {@link #invoke invoke}.
     * The two methods
     * perform the same steps when the caller's type descriptor exactly matches
     * the callee's, but when the types differ, plain {@link #invoke invoke}
     * also calls {@code asType} (or some internal equivalent) in order
     * to match up the caller's and callee's types.
     * <p>
     * If the current method is a variable arity method handle
     * argument list conversion may involve the conversion and collection
     * of several arguments into an array, as
     * {@linkplain #asVarargsCollector described elsewhere}.
     * In every other case, all conversions are applied <em>pairwise</em>,
     * which means that each argument or return value is converted to
     * exactly one argument or return value (or no return value).
     * The applied conversions are defined by consulting
     * the corresponding component types of the old and new
     * method handle types.
     * <p>
     * Let <em>T0</em> and <em>T1</em> be corresponding new and old parameter types,
     * or old and new return types.  Specifically, for some valid index {@code i}, let
     * <em>T0</em>{@code =newType.parameterType(i)} and <em>T1</em>{@code =this.type().parameterType(i)}.
     * Or else, going the other way for return values, let
     * <em>T0</em>{@code =this.type().returnType()} and <em>T1</em>{@code =newType.returnType()}.
     * If the types are the same, the new method handle makes no change
     * to the corresponding argument or return value (if any).
     * Otherwise, one of the following conversions is applied
     * if possible:
     * <ul>
     * <li>If <em>T0</em> and <em>T1</em> are references, then a cast to <em>T1</em> is applied.
     *     (The types do not need to be related in any particular way.
     *     This is because a dynamic value of null can convert to any reference type.)
     * <li>If <em>T0</em> and <em>T1</em> are primitives, then a Java method invocation
     *     conversion (JLS 5.3) is applied, if one exists.
     *     (Specifically, <em>T0</em> must convert to <em>T1</em> by a widening primitive conversion.)
     * <li>If <em>T0</em> is a primitive and <em>T1</em> a reference,
     *     a Java casting conversion (JLS 5.5) is applied if one exists.
     *     (Specifically, the value is boxed from <em>T0</em> to its wrapper class,
     *     which is then widened as needed to <em>T1</em>.)
     * <li>If <em>T0</em> is a reference and <em>T1</em> a primitive, an unboxing
     *     conversion will be applied at runtime, possibly followed
     *     by a Java method invocation conversion (JLS 5.3)
     *     on the primitive value.  (These are the primitive widening conversions.)
     *     <em>T0</em> must be a wrapper class or a supertype of one.
     *     (In the case where <em>T0</em> is Object, these are the conversions
     *     allowed by {@link java.lang.reflect.Method#invoke java.lang.reflect.Method.invoke}.)
     *     The unboxing conversion must have a possibility of success, which means that
     *     if <em>T0</em> is not itself a wrapper class, there must exist at least one
     *     wrapper class <em>TW</em> which is a subtype of <em>T0</em> and whose unboxed
     *     primitive value can be widened to <em>T1</em>.
     * <li>If the return type <em>T1</em> is marked as void, any returned value is discarded
     * <li>If the return type <em>T0</em> is void and <em>T1</em> a reference, a null value is introduced.
     * <li>If the return type <em>T0</em> is void and <em>T1</em> a primitive,
     *     a zero value is introduced.
     * </ul>
     * (<em>Note:</em> Both <em>T0</em> and <em>T1</em> may be regarded as static types,
     * because neither corresponds specifically to the <em>dynamic type</em> of any
     * actual argument or return value.)
     * <p>
     * The method handle conversion cannot be made if any one of the required
     * pairwise conversions cannot be made.
     * <p>
     * At runtime, the conversions applied to reference arguments
     * or return values may require additional runtime checks which can fail.
     * An unboxing operation may fail because the original reference is null,
     * causing a {@link java.lang.NullPointerException NullPointerException}.
     * An unboxing operation or a reference cast may also fail on a reference
     * to an object of the wrong type,
     * causing a {@link java.lang.ClassCastException ClassCastException}.
     * Although an unboxing operation may accept several kinds of wrappers,
     * if none are available, a {@code ClassCastException} will be thrown.
     *
     * @param newType the expected type of the new method handle
     * @return a method handle which delegates to {@code this} after performing
     *           any necessary argument conversions, and arranges for any
     *           necessary return value conversions
     * @throws NullPointerException if {@code newType} is a null reference
     * @throws WrongMethodTypeException if the conversion cannot be made
     * @see MethodHandles#explicitCastArguments
     */
    public MethodHandle asType(MethodType newType) {
        // Fast path alternative to a heavyweight {@code asType} call.
        // Return 'this' if the conversion will be a no-op.
        if (newType == type) {
            return this;
        }
        // Return 'this.asTypeCache' if the conversion is already memoized.
        MethodHandle atc = asTypeCached(newType);
        if (atc != null) {
            return atc;
        }
        return asTypeUncached(newType);
    }

    private MethodHandle asTypeCached(MethodType newType) {
        MethodHandle atc = asTypeCache;
        if (atc != null && newType == atc.type) {
            return atc;
        }
        return null;
    }

    /** Override this to change asType behavior. */
    /*non-public*/
    MethodHandle asTypeUncached(MethodType newType) {
        if (!type.isConvertibleTo(newType))
            throw new WrongMethodTypeException("cannot convert "+this+" to "+newType);
        return asTypeCache = MethodHandleImpl.makePairwiseConvert(this, newType, true);
    }

    /**
     * Makes an <em>array-spreading</em> method handle, which accepts a trailing array argument
     * and spreads its elements as positional arguments.
     * The new method handle adapts, as its <i>target</i>,
     * the current method handle.  The type of the adapter will be
     * the same as the type of the target, except that the final
     * {@code arrayLength} parameters of the target's type are replaced
     * by a single array parameter of type {@code arrayType}.
     * <p>
     * If the array element type differs from any of the corresponding
     * argument types on the original target,
     * the original target is adapted to take the array elements directly,
     * as if by a call to {@link #asType asType}.
     * <p>
     * When called, the adapter replaces a trailing array argument
     * by the array's elements, each as its own argument to the target.
     * (The order of the arguments is preserved.)
     * They are converted pairwise by casting and/or unboxing
     * to the types of the trailing parameters of the target.
     * Finally the target is called.
     * What the target eventually returns is returned unchanged by the adapter.
     * <p>
     * Before calling the target, the adapter verifies that the array
     * contains exactly enough elements to provide a correct argument count
     * to the target method handle.
     * (The array may also be null when zero elements are required.)
     * <p>
     * When the adapter is called, the length of the supplied {@code array}
     * argument is queried as if by {@code array.length} or {@code arraylength}
     * bytecode. If the adapter accepts a zero-length trailing array argument,
     * the supplied {@code array} argument can either be a zero-length array or
     * {@code null}; otherwise, the adapter will throw a {@code NullPointerException}
     * if the array is {@code null} and throw an {@link IllegalArgumentException}
     * if the array does not have the correct number of elements.
     * <p>
     * Here are some simple examples of array-spreading method handles:
     * <blockquote><pre>{@code
MethodHandle equals = publicLookup()
  .findVirtual(String.class, "equals", methodType(boolean.class, Object.class));
assert( (boolean) equals.invokeExact("me", (Object)"me"));
assert(!(boolean) equals.invokeExact("me", (Object)"thee"));
// spread both arguments from a 2-array:
MethodHandle eq2 = equals.asSpreader(Object[].class, 2);
assert( (boolean) eq2.invokeExact(new Object[]{ "me", "me" }));
assert(!(boolean) eq2.invokeExact(new Object[]{ "me", "thee" }));
// try to spread from anything but a 2-array:
for (int n = 0; n <= 10; n++) {
  Object[] badArityArgs = (n == 2 ? new Object[0] : new Object[n]);
  try { assert((boolean) eq2.invokeExact(badArityArgs) && false); }
  catch (IllegalArgumentException ex) { } // OK
}
// spread both arguments from a String array:
MethodHandle eq2s = equals.asSpreader(String[].class, 2);
assert( (boolean) eq2s.invokeExact(new String[]{ "me", "me" }));
assert(!(boolean) eq2s.invokeExact(new String[]{ "me", "thee" }));
// spread second arguments from a 1-array:
MethodHandle eq1 = equals.asSpreader(Object[].class, 1);
assert( (boolean) eq1.invokeExact("me", new Object[]{ "me" }));
assert(!(boolean) eq1.invokeExact("me", new Object[]{ "thee" }));
// spread no arguments from a 0-array or null:
MethodHandle eq0 = equals.asSpreader(Object[].class, 0);
assert( (boolean) eq0.invokeExact("me", (Object)"me", new Object[0]));
assert(!(boolean) eq0.invokeExact("me", (Object)"thee", (Object[])null));
// asSpreader and asCollector are approximate inverses:
for (int n = 0; n <= 2; n++) {
    for (Class<?> a : new Class<?>[]{Object[].class, String[].class, CharSequence[].class}) {
        MethodHandle equals2 = equals.asSpreader(a, n).asCollector(a, n);
        assert( (boolean) equals2.invokeWithArguments("me", "me"));
        assert(!(boolean) equals2.invokeWithArguments("me", "thee"));
    }
}
MethodHandle caToString = publicLookup()
  .findStatic(Arrays.class, "toString", methodType(String.class, char[].class));
assertEquals("[A, B, C]", (String) caToString.invokeExact("ABC".toCharArray()));
MethodHandle caString3 = caToString.asCollector(char[].class, 3);
assertEquals("[A, B, C]", (String) caString3.invokeExact('A', 'B', 'C'));
MethodHandle caToString2 = caString3.asSpreader(char[].class, 2);
assertEquals("[A, B, C]", (String) caToString2.invokeExact('A', "BC".toCharArray()));
     * }</pre></blockquote>
     * @param arrayType usually {@code Object[]}, the type of the array argument from which to extract the spread arguments
     * @param arrayLength the number of arguments to spread from an incoming array argument
     * @return a new method handle which spreads its final array argument,
     *         before calling the original method handle
     * @throws NullPointerException if {@code arrayType} is a null reference
     * @throws IllegalArgumentException if {@code arrayType} is not an array type,
     *         or if target does not have at least
     *         {@code arrayLength} parameter types,
     *         or if {@code arrayLength} is negative,
     *         or if the resulting method handle's type would have
     *         <a href="MethodHandle.html#maxarity">too many parameters</a>
     * @throws WrongMethodTypeException if the implied {@code asType} call fails
     * @see #asCollector
     */
    public MethodHandle asSpreader(Class<?> arrayType, int arrayLength) {
        return asSpreader(type().parameterCount() - arrayLength, arrayType, arrayLength);
    }

    /**
     * Makes an <em>array-spreading</em> method handle, which accepts an array argument at a given position and spreads
     * its elements as positional arguments in place of the array. The new method handle adapts, as its <i>target</i>,
     * the current method handle. The type of the adapter will be the same as the type of the target, except that the
     * {@code arrayLength} parameters of the target's type, starting at the zero-based position {@code spreadArgPos},
     * are replaced by a single array parameter of type {@code arrayType}.
     * <p>
     * This method behaves very much like {@link #asSpreader(Class, int)}, but accepts an additional {@code spreadArgPos}
     * argument to indicate at which position in the parameter list the spreading should take place.
     *
     * @apiNote Example:
     * <blockquote><pre>{@code
    MethodHandle compare = LOOKUP.findStatic(Objects.class, "compare", methodType(int.class, Object.class, Object.class, Comparator.class));
    MethodHandle compare2FromArray = compare.asSpreader(0, Object[].class, 2);
    Object[] ints = new Object[]{3, 9, 7, 7};
    Comparator<Integer> cmp = (a, b) -> a - b;
    assertTrue((int) compare2FromArray.invoke(Arrays.copyOfRange(ints, 0, 2), cmp) < 0);
    assertTrue((int) compare2FromArray.invoke(Arrays.copyOfRange(ints, 1, 3), cmp) > 0);
    assertTrue((int) compare2FromArray.invoke(Arrays.copyOfRange(ints, 2, 4), cmp) == 0);
     * }</pre></blockquote>
     * @param spreadArgPos the position (zero-based index) in the argument list at which spreading should start.
     * @param arrayType usually {@code Object[]}, the type of the array argument from which to extract the spread arguments
     * @param arrayLength the number of arguments to spread from an incoming array argument
     * @return a new method handle which spreads an array argument at a given position,
     *         before calling the original method handle
     * @throws NullPointerException if {@code arrayType} is a null reference
     * @throws IllegalArgumentException if {@code arrayType} is not an array type,
     *         or if target does not have at least
     *         {@code arrayLength} parameter types,
     *         or if {@code arrayLength} is negative,
     *         or if {@code spreadArgPos} has an illegal value (negative, or together with arrayLength exceeding the
     *         number of arguments),
     *         or if the resulting method handle's type would have
     *         <a href="MethodHandle.html#maxarity">too many parameters</a>
     * @throws WrongMethodTypeException if the implied {@code asType} call fails
     *
     * @see #asSpreader(Class, int)
     * @since 9
     */
    public MethodHandle asSpreader(int spreadArgPos, Class<?> arrayType, int arrayLength) {
        MethodType postSpreadType = asSpreaderChecks(arrayType, spreadArgPos, arrayLength);
        MethodHandle afterSpread = this.asType(postSpreadType);
        BoundMethodHandle mh = afterSpread.rebind();
        LambdaForm lform = mh.editor().spreadArgumentsForm(1 + spreadArgPos, arrayType, arrayLength);
        MethodType preSpreadType = postSpreadType.replaceParameterTypes(spreadArgPos, spreadArgPos + arrayLength, arrayType);
        return mh.copyWith(preSpreadType, lform);
    }

    /**
     * See if {@code asSpreader} can be validly called with the given arguments.
     * Return the type of the method handle call after spreading but before conversions.
     */
    private MethodType asSpreaderChecks(Class<?> arrayType, int pos, int arrayLength) {
        spreadArrayChecks(arrayType, arrayLength);
        int nargs = type().parameterCount();
        if (nargs < arrayLength || arrayLength < 0)
            throw newIllegalArgumentException("bad spread array length");
        if (pos < 0 || pos + arrayLength > nargs) {
            throw newIllegalArgumentException("bad spread position");
        }
        Class<?> arrayElement = arrayType.getComponentType();
        MethodType mtype = type();
        boolean match = true, fail = false;
        for (int i = pos; i < pos + arrayLength; i++) {
            Class<?> ptype = mtype.parameterType(i);
            if (ptype != arrayElement) {
                match = false;
                if (!MethodType.canConvert(arrayElement, ptype)) {
                    fail = true;
                    break;
                }
            }
        }
        if (match)  return mtype;
        MethodType needType = mtype.asSpreaderType(arrayType, pos, arrayLength);
        if (!fail)  return needType;
        // elicit an error:
        this.asType(needType);
        throw newInternalError("should not return");
    }

    private void spreadArrayChecks(Class<?> arrayType, int arrayLength) {
        Class<?> arrayElement = arrayType.getComponentType();
        if (arrayElement == null)
            throw newIllegalArgumentException("not an array type", arrayType);
        if ((arrayLength & 0x7F) != arrayLength) {
            if ((arrayLength & 0xFF) != arrayLength)
                throw newIllegalArgumentException("array length is not legal", arrayLength);
            assert(arrayLength >= 128);
            if (arrayElement == long.class ||
                arrayElement == double.class)
                throw newIllegalArgumentException("array length is not legal for long[] or double[]", arrayLength);
        }
    }

    /**
     * Adapts this method handle to be {@linkplain #asVarargsCollector variable arity}
     * if the boolean flag is true, else {@linkplain #asFixedArity fixed arity}.
     * If the method handle is already of the proper arity mode, it is returned
     * unchanged.
     * @apiNote
     * <p>This method is sometimes useful when adapting a method handle that
     * may be variable arity, to ensure that the resulting adapter is also
     * variable arity if and only if the original handle was.  For example,
     * this code changes the first argument of a handle {@code mh} to {@code int} without
     * disturbing its variable arity property:
     * {@code mh.asType(mh.type().changeParameterType(0,int.class))
     *     .withVarargs(mh.isVarargsCollector())}
     * <p>
     * This call is approximately equivalent to the following code:
     * <blockquote><pre>{@code
     * if (makeVarargs == isVarargsCollector())
     *   return this;
     * else if (makeVarargs)
     *   return asVarargsCollector(type().lastParameterType());
     * else
     *   return asFixedArity();
     * }</pre></blockquote>
     * @param makeVarargs true if the return method handle should have variable arity behavior
     * @return a method handle of the same type, with possibly adjusted variable arity behavior
     * @throws IllegalArgumentException if {@code makeVarargs} is true and
     *         this method handle does not have a trailing array parameter
     * @since 9
     * @see #asVarargsCollector
     * @see #asFixedArity
     */
    public MethodHandle withVarargs(boolean makeVarargs) {
        assert(!isVarargsCollector());  // subclass responsibility
        if (makeVarargs) {
           return asVarargsCollector(type().lastParameterType());
        } else {
            return this;
        }
    }

    /**
     * Makes an <em>array-collecting</em> method handle, which accepts a given number of trailing
     * positional arguments and collects them into an array argument.
     * The new method handle adapts, as its <i>target</i>,
     * the current method handle.  The type of the adapter will be
     * the same as the type of the target, except that a single trailing
     * parameter (usually of type {@code arrayType}) is replaced by
     * {@code arrayLength} parameters whose type is element type of {@code arrayType}.
     * <p>
     * If the array type differs from the final argument type on the original target,
     * the original target is adapted to take the array type directly,
     * as if by a call to {@link #asType asType}.
     * <p>
     * When called, the adapter replaces its trailing {@code arrayLength}
     * arguments by a single new array of type {@code arrayType}, whose elements
     * comprise (in order) the replaced arguments.
     * Finally the target is called.
     * What the target eventually returns is returned unchanged by the adapter.
     * <p>
     * (The array may also be a shared constant when {@code arrayLength} is zero.)
     * <p>
     * (<em>Note:</em> The {@code arrayType} is often identical to the
     * {@linkplain MethodType#lastParameterType last parameter type}
     * of the original target.
     * It is an explicit argument for symmetry with {@code asSpreader}, and also
     * to allow the target to use a simple {@code Object} as its last parameter type.)
     * <p>
     * In order to create a collecting adapter which is not restricted to a particular
     * number of collected arguments, use {@link #asVarargsCollector asVarargsCollector}
     * or {@link #withVarargs withVarargs} instead.
     * <p>
     * Here are some examples of array-collecting method handles:
     * <blockquote><pre>{@code
MethodHandle deepToString = publicLookup()
  .findStatic(Arrays.class, "deepToString", methodType(String.class, Object[].class));
assertEquals("[won]",   (String) deepToString.invokeExact(new Object[]{"won"}));
MethodHandle ts1 = deepToString.asCollector(Object[].class, 1);
assertEquals(methodType(String.class, Object.class), ts1.type());
//assertEquals("[won]", (String) ts1.invokeExact(         new Object[]{"won"})); //FAIL
assertEquals("[[won]]", (String) ts1.invokeExact((Object) new Object[]{"won"}));
// arrayType can be a subtype of Object[]
MethodHandle ts2 = deepToString.asCollector(String[].class, 2);
assertEquals(methodType(String.class, String.class, String.class), ts2.type());
assertEquals("[two, too]", (String) ts2.invokeExact("two", "too"));
MethodHandle ts0 = deepToString.asCollector(Object[].class, 0);
assertEquals("[]", (String) ts0.invokeExact());
// collectors can be nested, Lisp-style
MethodHandle ts22 = deepToString.asCollector(Object[].class, 3).asCollector(String[].class, 2);
assertEquals("[A, B, [C, D]]", ((String) ts22.invokeExact((Object)'A', (Object)"B", "C", "D")));
// arrayType can be any primitive array type
MethodHandle bytesToString = publicLookup()
  .findStatic(Arrays.class, "toString", methodType(String.class, byte[].class))
  .asCollector(byte[].class, 3);
assertEquals("[1, 2, 3]", (String) bytesToString.invokeExact((byte)1, (byte)2, (byte)3));
MethodHandle longsToString = publicLookup()
  .findStatic(Arrays.class, "toString", methodType(String.class, long[].class))
  .asCollector(long[].class, 1);
assertEquals("[123]", (String) longsToString.invokeExact((long)123));
     * }</pre></blockquote>
     * <p>
     * <em>Note:</em> The resulting adapter is never a {@linkplain MethodHandle#asVarargsCollector
     * variable-arity method handle}, even if the original target method handle was.
     * @param arrayType often {@code Object[]}, the type of the array argument which will collect the arguments
     * @param arrayLength the number of arguments to collect into a new array argument
     * @return a new method handle which collects some trailing argument
     *         into an array, before calling the original method handle
     * @throws NullPointerException if {@code arrayType} is a null reference
     * @throws IllegalArgumentException if {@code arrayType} is not an array type
     *         or {@code arrayType} is not assignable to this method handle's trailing parameter type,
     *         or {@code arrayLength} is not a legal array size,
     *         or the resulting method handle's type would have
     *         <a href="MethodHandle.html#maxarity">too many parameters</a>
     * @throws WrongMethodTypeException if the implied {@code asType} call fails
     * @see #asSpreader
     * @see #asVarargsCollector
     */
    public MethodHandle asCollector(Class<?> arrayType, int arrayLength) {
        return asCollector(type().parameterCount() - 1, arrayType, arrayLength);
    }

    /**
     * Makes an <em>array-collecting</em> method handle, which accepts a given number of positional arguments starting
     * at a given position, and collects them into an array argument. The new method handle adapts, as its
     * <i>target</i>, the current method handle. The type of the adapter will be the same as the type of the target,
     * except that the parameter at the position indicated by {@code collectArgPos} (usually of type {@code arrayType})
     * is replaced by {@code arrayLength} parameters whose type is element type of {@code arrayType}.
     * <p>
     * This method behaves very much like {@link #asCollector(Class, int)}, but differs in that its {@code
     * collectArgPos} argument indicates at which position in the parameter list arguments should be collected. This
     * index is zero-based.
     *
     * @apiNote Examples:
     * <blockquote><pre>{@code
    StringWriter swr = new StringWriter();
    MethodHandle swWrite = LOOKUP.findVirtual(StringWriter.class, "write", methodType(void.class, char[].class, int.class, int.class)).bindTo(swr);
    MethodHandle swWrite4 = swWrite.asCollector(0, char[].class, 4);
    swWrite4.invoke('A', 'B', 'C', 'D', 1, 2);
    assertEquals("BC", swr.toString());
    swWrite4.invoke('P', 'Q', 'R', 'S', 0, 4);
    assertEquals("BCPQRS", swr.toString());
    swWrite4.invoke('W', 'X', 'Y', 'Z', 3, 1);
    assertEquals("BCPQRSZ", swr.toString());
     * }</pre></blockquote>
     * <p>
     * <em>Note:</em> The resulting adapter is never a {@linkplain MethodHandle#asVarargsCollector
     * variable-arity method handle}, even if the original target method handle was.
     * @param collectArgPos the zero-based position in the parameter list at which to start collecting.
     * @param arrayType often {@code Object[]}, the type of the array argument which will collect the arguments
     * @param arrayLength the number of arguments to collect into a new array argument
     * @return a new method handle which collects some arguments
     *         into an array, before calling the original method handle
     * @throws NullPointerException if {@code arrayType} is a null reference
     * @throws IllegalArgumentException if {@code arrayType} is not an array type
     *         or {@code arrayType} is not assignable to this method handle's array parameter type,
     *         or {@code arrayLength} is not a legal array size,
     *         or {@code collectArgPos} has an illegal value (negative, or greater than the number of arguments),
     *         or the resulting method handle's type would have
     *         <a href="MethodHandle.html#maxarity">too many parameters</a>
     * @throws WrongMethodTypeException if the implied {@code asType} call fails
     *
     * @see #asCollector(Class, int)
     * @since 9
     */
    public MethodHandle asCollector(int collectArgPos, Class<?> arrayType, int arrayLength) {
        asCollectorChecks(arrayType, collectArgPos, arrayLength);
        BoundMethodHandle mh = rebind();
        MethodType resultType = type().asCollectorType(arrayType, collectArgPos, arrayLength);
        MethodHandle collector = MethodHandleImpl.varargsArray(arrayType, arrayLength);
        LambdaForm lform = mh.editor().collectArgumentsForm(1 + collectArgPos, collector.type().basicType());
        return mh.copyWithExtendL(resultType, lform, collector);
    }

    /**
     * See if {@code asCollector} can be validly called with the given arguments.
     * Return false if the last parameter is not an exact match to arrayType.
     */
    /*non-public*/
    boolean asCollectorChecks(Class<?> arrayType, int pos, int arrayLength) {
        spreadArrayChecks(arrayType, arrayLength);
        int nargs = type().parameterCount();
        if (pos < 0 || pos >= nargs) {
            throw newIllegalArgumentException("bad collect position");
        }
        if (nargs != 0) {
            Class<?> param = type().parameterType(pos);
            if (param == arrayType)  return true;
            if (param.isAssignableFrom(arrayType))  return false;
        }
        throw newIllegalArgumentException("array type not assignable to argument", this, arrayType);
    }

    /**
     * Makes a <em>variable arity</em> adapter which is able to accept
     * any number of trailing positional arguments and collect them
     * into an array argument.
     * <p>
     * The type and behavior of the adapter will be the same as
     * the type and behavior of the target, except that certain
     * {@code invoke} and {@code asType} requests can lead to
     * trailing positional arguments being collected into target's
     * trailing parameter.
     * Also, the
     * {@linkplain MethodType#lastParameterType last parameter type}
     * of the adapter will be
     * {@code arrayType}, even if the target has a different
     * last parameter type.
     * <p>
     * This transformation may return {@code this} if the method handle is
     * already of variable arity and its trailing parameter type
     * is identical to {@code arrayType}.
     * <p>
     * When called with {@link #invokeExact invokeExact}, the adapter invokes
     * the target with no argument changes.
     * (<em>Note:</em> This behavior is different from a
     * {@linkplain #asCollector fixed arity collector},
     * since it accepts a whole array of indeterminate length,
     * rather than a fixed number of arguments.)
     * <p>
     * When called with plain, inexact {@link #invoke invoke}, if the caller
     * type is the same as the adapter, the adapter invokes the target as with
     * {@code invokeExact}.
     * (This is the normal behavior for {@code invoke} when types match.)
     * <p>
     * Otherwise, if the caller and adapter arity are the same, and the
     * trailing parameter type of the caller is a reference type identical to
     * or assignable to the trailing parameter type of the adapter,
     * the arguments and return values are converted pairwise,
     * as if by {@link #asType asType} on a fixed arity
     * method handle.
     * <p>
     * Otherwise, the arities differ, or the adapter's trailing parameter
     * type is not assignable from the corresponding caller type.
     * In this case, the adapter replaces all trailing arguments from
     * the original trailing argument position onward, by
     * a new array of type {@code arrayType}, whose elements
     * comprise (in order) the replaced arguments.
     * <p>
     * The caller type must provides as least enough arguments,
     * and of the correct type, to satisfy the target's requirement for
     * positional arguments before the trailing array argument.
     * Thus, the caller must supply, at a minimum, {@code N-1} arguments,
     * where {@code N} is the arity of the target.
     * Also, there must exist conversions from the incoming arguments
     * to the target's arguments.
     * As with other uses of plain {@code invoke}, if these basic
     * requirements are not fulfilled, a {@code WrongMethodTypeException}
     * may be thrown.
     * <p>
     * In all cases, what the target eventually returns is returned unchanged by the adapter.
     * <p>
     * In the final case, it is exactly as if the target method handle were
     * temporarily adapted with a {@linkplain #asCollector fixed arity collector}
     * to the arity required by the caller type.
     * (As with {@code asCollector}, if the array length is zero,
     * a shared constant may be used instead of a new array.
     * If the implied call to {@code asCollector} would throw
     * an {@code IllegalArgumentException} or {@code WrongMethodTypeException},
     * the call to the variable arity adapter must throw
     * {@code WrongMethodTypeException}.)
     * <p>
     * The behavior of {@link #asType asType} is also specialized for
     * variable arity adapters, to maintain the invariant that
     * plain, inexact {@code invoke} is always equivalent to an {@code asType}
     * call to adjust the target type, followed by {@code invokeExact}.
     * Therefore, a variable arity adapter responds
     * to an {@code asType} request by building a fixed arity collector,
     * if and only if the adapter and requested type differ either
     * in arity or trailing argument type.
     * The resulting fixed arity collector has its type further adjusted
     * (if necessary) to the requested type by pairwise conversion,
     * as if by another application of {@code asType}.
     * <p>
     * When a method handle is obtained by executing an {@code ldc} instruction
     * of a {@code CONSTANT_MethodHandle} constant, and the target method is marked
     * as a variable arity method (with the modifier bit {@code 0x0080}),
     * the method handle will accept multiple arities, as if the method handle
     * constant were created by means of a call to {@code asVarargsCollector}.
     * <p>
     * In order to create a collecting adapter which collects a predetermined
     * number of arguments, and whose type reflects this predetermined number,
     * use {@link #asCollector asCollector} instead.
     * <p>
     * No method handle transformations produce new method handles with
     * variable arity, unless they are documented as doing so.
     * Therefore, besides {@code asVarargsCollector} and {@code withVarargs},
     * all methods in {@code MethodHandle} and {@code MethodHandles}
     * will return a method handle with fixed arity,
     * except in the cases where they are specified to return their original
     * operand (e.g., {@code asType} of the method handle's own type).
     * <p>
     * Calling {@code asVarargsCollector} on a method handle which is already
     * of variable arity will produce a method handle with the same type and behavior.
     * It may (or may not) return the original variable arity method handle.
     * <p>
     * Here is an example, of a list-making variable arity method handle:
     * <blockquote><pre>{@code
MethodHandle deepToString = publicLookup()
  .findStatic(Arrays.class, "deepToString", methodType(String.class, Object[].class));
MethodHandle ts1 = deepToString.asVarargsCollector(Object[].class);
assertEquals("[won]",   (String) ts1.invokeExact(    new Object[]{"won"}));
assertEquals("[won]",   (String) ts1.invoke(         new Object[]{"won"}));
assertEquals("[won]",   (String) ts1.invoke(                      "won" ));
assertEquals("[[won]]", (String) ts1.invoke((Object) new Object[]{"won"}));
// findStatic of Arrays.asList(...) produces a variable arity method handle:
MethodHandle asList = publicLookup()
  .findStatic(Arrays.class, "asList", methodType(List.class, Object[].class));
assertEquals(methodType(List.class, Object[].class), asList.type());
assert(asList.isVarargsCollector());
assertEquals("[]", asList.invoke().toString());
assertEquals("[1]", asList.invoke(1).toString());
assertEquals("[two, too]", asList.invoke("two", "too").toString());
String[] argv = { "three", "thee", "tee" };
assertEquals("[three, thee, tee]", asList.invoke(argv).toString());
assertEquals("[three, thee, tee]", asList.invoke((Object[])argv).toString());
List ls = (List) asList.invoke((Object)argv);
assertEquals(1, ls.size());
assertEquals("[three, thee, tee]", Arrays.toString((Object[])ls.get(0)));
     * }</pre></blockquote>
     * <p style="font-size:smaller;">
     * <em>Discussion:</em>
     * These rules are designed as a dynamically-typed variation
     * of the Java rules for variable arity methods.
     * In both cases, callers to a variable arity method or method handle
     * can either pass zero or more positional arguments, or else pass
     * pre-collected arrays of any length.  Users should be aware of the
     * special role of the final argument, and of the effect of a
     * type match on that final argument, which determines whether
     * or not a single trailing argument is interpreted as a whole
     * array or a single element of an array to be collected.
     * Note that the dynamic type of the trailing argument has no
     * effect on this decision, only a comparison between the symbolic
     * type descriptor of the call site and the type descriptor of the method handle.)
     *
     * @param arrayType often {@code Object[]}, the type of the array argument which will collect the arguments
     * @return a new method handle which can collect any number of trailing arguments
     *         into an array, before calling the original method handle
     * @throws NullPointerException if {@code arrayType} is a null reference
     * @throws IllegalArgumentException if {@code arrayType} is not an array type
     *         or {@code arrayType} is not assignable to this method handle's trailing parameter type
     * @see #asCollector
     * @see #isVarargsCollector
     * @see #withVarargs
     * @see #asFixedArity
     */
    public MethodHandle asVarargsCollector(Class<?> arrayType) {
        Objects.requireNonNull(arrayType);
        boolean lastMatch = asCollectorChecks(arrayType, type().parameterCount() - 1, 0);
        if (isVarargsCollector() && lastMatch)
            return this;
        return MethodHandleImpl.makeVarargsCollector(this, arrayType);
    }

    /**
     * Determines if this method handle
     * supports {@linkplain #asVarargsCollector variable arity} calls.
     * Such method handles arise from the following sources:
     * <ul>
     * <li>a call to {@linkplain #asVarargsCollector asVarargsCollector}
     * <li>a call to a {@linkplain java.lang.invoke.MethodHandles.Lookup lookup method}
     *     which resolves to a variable arity Java method or constructor
     * <li>an {@code ldc} instruction of a {@code CONSTANT_MethodHandle}
     *     which resolves to a variable arity Java method or constructor
     * </ul>
     * @return true if this method handle accepts more than one arity of plain, inexact {@code invoke} calls
     * @see #asVarargsCollector
     * @see #asFixedArity
     */
    public boolean isVarargsCollector() {
        return false;
    }

    /**
     * Makes a <em>fixed arity</em> method handle which is otherwise
     * equivalent to the current method handle.
     * <p>
     * If the current method handle is not of
     * {@linkplain #asVarargsCollector variable arity},
     * the current method handle is returned.
     * This is true even if the current method handle
     * could not be a valid input to {@code asVarargsCollector}.
     * <p>
     * Otherwise, the resulting fixed-arity method handle has the same
     * type and behavior of the current method handle,
     * except that {@link #isVarargsCollector isVarargsCollector}
     * will be false.
     * The fixed-arity method handle may (or may not) be the
     * a previous argument to {@code asVarargsCollector}.
     * <p>
     * Here is an example, of a list-making variable arity method handle:
     * <blockquote><pre>{@code
MethodHandle asListVar = publicLookup()
  .findStatic(Arrays.class, "asList", methodType(List.class, Object[].class))
  .asVarargsCollector(Object[].class);
MethodHandle asListFix = asListVar.asFixedArity();
assertEquals("[1]", asListVar.invoke(1).toString());
Exception caught = null;
try { asListFix.invoke((Object)1); }
catch (Exception ex) { caught = ex; }
assert(caught instanceof ClassCastException);
assertEquals("[two, too]", asListVar.invoke("two", "too").toString());
try { asListFix.invoke("two", "too"); }
catch (Exception ex) { caught = ex; }
assert(caught instanceof WrongMethodTypeException);
Object[] argv = { "three", "thee", "tee" };
assertEquals("[three, thee, tee]", asListVar.invoke(argv).toString());
assertEquals("[three, thee, tee]", asListFix.invoke(argv).toString());
assertEquals(1, ((List) asListVar.invoke((Object)argv)).size());
assertEquals("[three, thee, tee]", asListFix.invoke((Object)argv).toString());
     * }</pre></blockquote>
     *
     * @return a new method handle which accepts only a fixed number of arguments
     * @see #asVarargsCollector
     * @see #isVarargsCollector
     * @see #withVarargs
     */
    public MethodHandle asFixedArity() {
        assert(!isVarargsCollector());
        return this;
    }

    /**
     * Binds a value {@code x} to the first argument of a method handle, without invoking it.
     * The new method handle adapts, as its <i>target</i>,
     * the current method handle by binding it to the given argument.
     * The type of the bound handle will be
     * the same as the type of the target, except that a single leading
     * reference parameter will be omitted.
     * <p>
     * When called, the bound handle inserts the given value {@code x}
     * as a new leading argument to the target.  The other arguments are
     * also passed unchanged.
     * What the target eventually returns is returned unchanged by the bound handle.
     * <p>
     * The reference {@code x} must be convertible to the first parameter
     * type of the target.
     * <p>
     * <em>Note:</em>  Because method handles are immutable, the target method handle
     * retains its original type and behavior.
     * <p>
     * <em>Note:</em> The resulting adapter is never a {@linkplain MethodHandle#asVarargsCollector
     * variable-arity method handle}, even if the original target method handle was.
     * @param x  the value to bind to the first argument of the target
     * @return a new method handle which prepends the given value to the incoming
     *         argument list, before calling the original method handle
     * @throws IllegalArgumentException if the target does not have a
     *         leading parameter type that is a reference type
     * @throws ClassCastException if {@code x} cannot be converted
     *         to the leading parameter type of the target
     * @see MethodHandles#insertArguments
     */
    public MethodHandle bindTo(Object x) {
        x = type.leadingReferenceParameter().cast(x);  // throw CCE if needed
        return bindArgumentL(0, x);
    }

    /**
     * Return a nominal descriptor for this instance, if one can be
     * constructed, or an empty {@link Optional} if one cannot be.
     *
     * @return An {@link Optional} containing the resulting nominal descriptor,
     * or an empty {@link Optional} if one cannot be constructed.
     * @since 12
     */
    @Override
    public Optional<MethodHandleDesc> describeConstable() {
        MethodHandleInfo info;
        ClassDesc owner;
        String name;
        MethodTypeDesc type;
        boolean isInterface;
        try {
            info = IMPL_LOOKUP.revealDirect(this);
            isInterface = info.getDeclaringClass().isInterface();
            owner = info.getDeclaringClass().describeConstable().orElseThrow();
            type = info.getMethodType().describeConstable().orElseThrow();
            name = info.getName();
        }
        catch (Exception e) {
            return Optional.empty();
        }

        return switch (info.getReferenceKind()) {
            case REF_getField         -> Optional.of(MethodHandleDesc.ofField(DirectMethodHandleDesc.Kind.GETTER, owner, name, type.returnType()));
            case REF_putField         -> Optional.of(MethodHandleDesc.ofField(DirectMethodHandleDesc.Kind.SETTER, owner, name, type.parameterType(0)));
            case REF_getStatic        -> Optional.of(MethodHandleDesc.ofField(DirectMethodHandleDesc.Kind.STATIC_GETTER, owner, name, type.returnType()));
            case REF_putStatic        -> Optional.of(MethodHandleDesc.ofField(DirectMethodHandleDesc.Kind.STATIC_SETTER, owner, name, type.parameterType(0)));
            case REF_invokeVirtual    -> Optional.of(MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.VIRTUAL, owner, name, type));
            case REF_invokeStatic     -> isInterface ?
                                          Optional.of(MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.INTERFACE_STATIC, owner, name, type)) :
                                          Optional.of(MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.STATIC, owner, name, type));
            case REF_invokeSpecial    -> isInterface ?
                                          Optional.of(MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.INTERFACE_SPECIAL, owner, name, type)) :
                                          Optional.of(MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.SPECIAL, owner, name, type));
            case REF_invokeInterface  -> Optional.of(MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.INTERFACE_VIRTUAL, owner, name, type));
            case REF_newInvokeSpecial -> Optional.of(MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.CONSTRUCTOR, owner, name, type));
            default -> Optional.empty();
        };
    }

    /**
     * Returns a string representation of the method handle,
     * starting with the string {@code "MethodHandle"} and
     * ending with the string representation of the method handle's type.
     * In other words, this method returns a string equal to the value of:
     * <blockquote><pre>{@code
     * "MethodHandle" + type().toString()
     * }</pre></blockquote>
     * <p>
     * (<em>Note:</em>  Future releases of this API may add further information
     * to the string representation.
     * Therefore, the present syntax should not be parsed by applications.)
     *
     * @return a string representation of the method handle
     */
    @Override
    public String toString() {
        if (DEBUG_METHOD_HANDLE_NAMES)  return "MethodHandle"+debugString();
        return standardString();
    }
    String standardString() {
        return "MethodHandle"+type;
    }
    /** Return a string with a several lines describing the method handle structure.
     *  This string would be suitable for display in an IDE debugger.
     */
    String debugString() {
        return type+" : "+internalForm()+internalProperties();
    }

    //// Implementation methods.
    //// Sub-classes can override these default implementations.
    //// All these methods assume arguments are already validated.

    // Other transforms to do:  convert, explicitCast, permute, drop, filter, fold, GWT, catch

    BoundMethodHandle bindArgumentL(int pos, Object value) {
        return rebind().bindArgumentL(pos, value);
    }

    /*non-public*/
    MethodHandle setVarargs(MemberName member) throws IllegalAccessException {
        if (!member.isVarargs())  return this;
        try {
            return this.withVarargs(true);
        } catch (IllegalArgumentException ex) {
            throw member.makeAccessException("cannot make variable arity", null);
        }
    }

    /*non-public*/
    MethodHandle viewAsType(MethodType newType, boolean strict) {
        // No actual conversions, just a new view of the same method.
        // Overridden in DMH, which has special rules
        assert(viewAsTypeChecks(newType, strict));
        return copyWith(newType, form);
    }

    /*non-public*/
    boolean viewAsTypeChecks(MethodType newType, boolean strict) {
        if (strict) {
            assert(type().isViewableAs(newType, true))
                : Arrays.asList(this, newType);
        } else {
            assert(type().basicType().isViewableAs(newType.basicType(), true))
                : Arrays.asList(this, newType);
        }
        return true;
    }

    // Decoding

    /*non-public*/
    LambdaForm internalForm() {
        return form;
    }

    /*non-public*/
    MemberName internalMemberName() {
        return null;  // DMH returns DMH.member
    }

    /*non-public*/
    Class<?> internalCallerClass() {
        return null;  // caller-bound MH for @CallerSensitive method returns caller
    }

    /*non-public*/
    MethodHandleImpl.Intrinsic intrinsicName() {
        // no special intrinsic meaning to most MHs
        return MethodHandleImpl.Intrinsic.NONE;
    }

    /*non-public*/
    Object intrinsicData() {
        return null;
    }

    /*non-public*/
    MethodHandle withInternalMemberName(MemberName member, boolean isInvokeSpecial) {
        if (member != null) {
            return MethodHandleImpl.makeWrappedMember(this, member, isInvokeSpecial);
        } else if (internalMemberName() == null) {
            // The required internalMemberName is null, and this MH (like most) doesn't have one.
            return this;
        } else {
            // The following case is rare. Mask the internalMemberName by wrapping the MH in a BMH.
            MethodHandle result = rebind();
            assert(result.internalMemberName() == null);
            return result;
        }
    }

    /*non-public*/
    boolean isInvokeSpecial() {
        return false;  // DMH.Special returns true
    }

    /*non-public*/
    boolean isCrackable() {
        return false;
    }

    /*non-public*/
    Object internalValues() {
        return null;
    }

    /*non-public*/
    Object internalProperties() {
        // Override to something to follow this.form, like "\n& FOO=bar"
        return "";
    }

    //// Method handle implementation methods.
    //// Sub-classes can override these default implementations.
    //// All these methods assume arguments are already validated.

    /*non-public*/
    abstract MethodHandle copyWith(MethodType mt, LambdaForm lf);

    /** Require this method handle to be a BMH, or else replace it with a "wrapper" BMH.
     *  Many transforms are implemented only for BMHs.
     *  @return a behaviorally equivalent BMH
     */
    abstract BoundMethodHandle rebind();

    /* non-public */
    void maybeCustomize() {
        if (form.customized == null) {
            byte count = customizationCount;
            if (count >= CUSTOMIZE_THRESHOLD) {
                customize();
            } else {
                customizationCount = (byte) (count + 1);
            }
        }
    }

    /** Craft a LambdaForm customized for this particular MethodHandle. */
    /*non-public*/
    void customize() {
        updateForm(new Function<>() {
                public LambdaForm apply(LambdaForm oldForm) {
                    return oldForm.customize(MethodHandle.this);
                }
            });
    }

    private volatile boolean updateInProgress; // = false;

    /**
     * Replace the old lambda form of this method handle with a new one.
     * The new one must be functionally equivalent to the old one.
     * Threads may continue running the old form indefinitely,
     * but it is likely that the new one will be preferred for new executions.
     * Use with discretion.
     */
    /*non-public*/
    void updateForm(Function<LambdaForm, LambdaForm> updater) {
        if (UNSAFE.compareAndSetBoolean(this, UPDATE_OFFSET, false, true)) { // updateInProgress = true
            // Only 1 thread wins the race and updates MH.form field.
            try {
                LambdaForm oldForm = form;
                LambdaForm newForm = updater.apply(oldForm);
                if (oldForm != newForm) {
                    assert (newForm.customized == null || newForm.customized == this);
                    newForm.prepare(); // as in MethodHandle.<init>
                    UNSAFE.putReference(this, FORM_OFFSET, newForm);
                    UNSAFE.fullFence();
                }
            } finally {
                updateInProgress = false;
            }
        } else {
            // Update got lost due to concurrent update. But callers don't care.
        }
    }

    private static final long   FORM_OFFSET = UNSAFE.objectFieldOffset(MethodHandle.class, "form");
    private static final long UPDATE_OFFSET = UNSAFE.objectFieldOffset(MethodHandle.class, "updateInProgress");
}
