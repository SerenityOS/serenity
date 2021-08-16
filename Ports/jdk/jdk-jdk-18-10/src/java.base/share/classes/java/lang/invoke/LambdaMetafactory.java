/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.Serializable;
import java.util.Arrays;
import java.lang.reflect.Array;
import java.util.Objects;

/**
 * <p>Methods to facilitate the creation of simple "function objects" that
 * implement one or more interfaces by delegation to a provided {@link MethodHandle},
 * possibly after type adaptation and partial evaluation of arguments.  These
 * methods are typically used as <em>bootstrap methods</em> for {@code invokedynamic}
 * call sites, to support the <em>lambda expression</em> and <em>method
 * reference expression</em> features of the Java Programming Language.
 *
 * <p>Indirect access to the behavior specified by the provided {@code MethodHandle}
 * proceeds in order through three phases:
 * <ul>
 *     <li><p><em>Linkage</em> occurs when the methods in this class are invoked.
 *     They take as arguments an interface to be implemented (typically a
 *     <em>functional interface</em>, one with a single abstract method), a
 *     name and signature of a method from that interface to be implemented, a
 *     {@linkplain MethodHandleInfo direct method handle} describing the desired
 *     implementation behavior for that method, and possibly other additional
 *     metadata, and produce a {@link CallSite} whose target can be used to
 *     create suitable function objects.
 *
 *     <p>Linkage may involve dynamically loading a new class that implements
 *     the target interface, or re-using a suitable existing class.
 *
 *     <p>The {@code CallSite} can be considered a "factory" for function
 *     objects and so these linkage methods are referred to as
 *     "metafactories".</li>
 *
 *     <li><p><em>Capture</em> occurs when the {@code CallSite}'s target is
 *     invoked, typically through an {@code invokedynamic} call site,
 *     producing a function object. This may occur many times for
 *     a single factory {@code CallSite}.
 *
 *     <p>If the behavior {@code MethodHandle} has additional parameters beyond
 *     those of the specified interface method, these are referred to as
 *     <em>captured parameters</em>, which must be provided as arguments to the
 *     {@code CallSite} target. The expected number and types of captured
 *     parameters are determined during linkage.
 *
 *     <p>Capture may involve allocation of a new function object, or may return
 *     a suitable existing function object. The identity of a function object
 *     produced by capture is unpredictable, and therefore identity-sensitive
 *     operations (such as reference equality, object locking, and {@code
 *     System.identityHashCode()}) may produce different results in different
 *     implementations, or even upon different invocations in the same
 *     implementation.</li>
 *
 *     <li><p><em>Invocation</em> occurs when an implemented interface method is
 *     invoked on a function object. This may occur many times for a single
 *     function object. The method referenced by the implementation
 *     {@code MethodHandle} is invoked, passing to it the captured arguments and
 *     the invocation arguments. The result of the method is returned.
 *     </li>
 * </ul>
 *
 * <p>It is sometimes useful to restrict the set of inputs or results permitted
 * at invocation.  For example, when the generic interface {@code Predicate<T>}
 * is parameterized as {@code Predicate<String>}, the input must be a
 * {@code String}, even though the method to implement allows any {@code Object}.
 * At linkage time, an additional {@link MethodType} parameter describes the
 * "dynamic" method type; on invocation, the arguments and eventual result
 * are checked against this {@code MethodType}.
 *
 * <p>This class provides two forms of linkage methods: a standard version
 * ({@link #metafactory(MethodHandles.Lookup, String, MethodType, MethodType, MethodHandle, MethodType)})
 * using an optimized protocol, and an alternate version
 * {@link #altMetafactory(MethodHandles.Lookup, String, MethodType, Object...)}).
 * The alternate version is a generalization of the standard version, providing
 * additional control over the behavior of the generated function objects via
 * flags and additional arguments.  The alternate version adds the ability to
 * manage the following attributes of function objects:
 *
 * <ul>
 *     <li><em>Multiple methods.</em>  It is sometimes useful to implement multiple
 *     variations of the method signature, involving argument or return type
 *     adaptation.  This occurs when multiple distinct VM signatures for a method
 *     are logically considered to be the same method by the language.  The
 *     flag {@code FLAG_BRIDGES} indicates that a list of additional
 *     {@code MethodType}s will be provided, each of which will be implemented
 *     by the resulting function object.  These methods will share the same
 *     name and instantiated type.</li>
 *
 *     <li><em>Multiple interfaces.</em>  If needed, more than one interface
 *     can be implemented by the function object.  (These additional interfaces
 *     are typically marker interfaces with no methods.)  The flag {@code FLAG_MARKERS}
 *     indicates that a list of additional interfaces will be provided, each of
 *     which should be implemented by the resulting function object.</li>
 *
 *     <li><em>Serializability.</em>  The generated function objects do not
 *     generally support serialization.  If desired, {@code FLAG_SERIALIZABLE}
 *     can be used to indicate that the function objects should be serializable.
 *     Serializable function objects will use, as their serialized form,
 *     instances of the class {@code SerializedLambda}, which requires additional
 *     assistance from the capturing class (the class described by the
 *     {@link MethodHandles.Lookup} parameter {@code caller}); see
 *     {@link SerializedLambda} for details.</li>
 * </ul>
 *
 * <p>Assume the linkage arguments are as follows:
 * <ul>
 *      <li>{@code factoryType} (describing the {@code CallSite} signature) has
 *      K parameters of types (D1..Dk) and return type Rd;</li>
 *      <li>{@code interfaceMethodType} (describing the implemented method type) has N
 *      parameters, of types (U1..Un) and return type Ru;</li>
 *      <li>{@code implementation} (the {@code MethodHandle} providing the
 *      implementation) has M parameters, of types (A1..Am) and return type Ra
 *      (if the method describes an instance method, the method type of this
 *      method handle already includes an extra first argument corresponding to
 *      the receiver);</li>
 *      <li>{@code dynamicMethodType} (allowing restrictions on invocation)
 *      has N parameters, of types (T1..Tn) and return type Rt.</li>
 * </ul>
 *
 * <p>Then the following linkage invariants must hold:
 * <ul>
 *     <li>{@code interfaceMethodType} and {@code dynamicMethodType} have the same
 *     arity N, and for i=1..N, Ti and Ui are the same type, or Ti and Ui are
 *     both reference types and Ti is a subtype of Ui</li>
 *     <li>Either Rt and Ru are the same type, or both are reference types and
 *     Rt is a subtype of Ru</li>
 *     <li>K + N = M</li>
 *     <li>For i=1..K, Di = Ai</li>
 *     <li>For i=1..N, Ti is adaptable to Aj, where j=i+k</li>
 *     <li>The return type Rt is void, or the return type Ra is not void and is
 *     adaptable to Rt</li>
 * </ul>
 *
 * <p>Further, at capture time, if {@code implementation} corresponds to an instance
 * method, and there are any capture arguments ({@code K > 0}), then the first
 * capture argument (corresponding to the receiver) must be non-null.
 *
 * <p>A type Q is considered adaptable to S as follows:
 * <table class="striped">
 *   <caption style="display:none">adaptable types</caption>
 *   <thead>
 *     <tr><th scope="col">Q</th><th scope="col">S</th><th scope="col">Link-time checks</th><th scope="col">Invocation-time checks</th></tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *         <th scope="row">Primitive</th><th scope="row">Primitive</th>
 *         <td>Q can be converted to S via a primitive widening conversion</td>
 *         <td>None</td>
 *     </tr>
 *     <tr>
 *         <th scope="row">Primitive</th><th scope="row">Reference</th>
 *         <td>S is a supertype of the Wrapper(Q)</td>
 *         <td>Cast from Wrapper(Q) to S</td>
 *     </tr>
 *     <tr>
 *         <th scope="row">Reference</th><th scope="row">Primitive</th>
 *         <td>for parameter types: Q is a primitive wrapper and Primitive(Q)
 *         can be widened to S
 *         <br>for return types: If Q is a primitive wrapper, check that
 *         Primitive(Q) can be widened to S</td>
 *         <td>If Q is not a primitive wrapper, cast Q to the base Wrapper(S);
 *         for example Number for numeric types</td>
 *     </tr>
 *     <tr>
 *         <th scope="row">Reference</th><th scope="row">Reference</th>
 *         <td>for parameter types: S is a supertype of Q
 *         <br>for return types: none</td>
 *         <td>Cast from Q to S</td>
 *     </tr>
 *   </tbody>
 * </table>
 *
 * @apiNote These linkage methods are designed to support the evaluation
 * of <em>lambda expressions</em> and <em>method references</em> in the Java
 * Language.  For every lambda expressions or method reference in the source code,
 * there is a target type which is a functional interface.  Evaluating a lambda
 * expression produces an object of its target type. The recommended mechanism
 * for evaluating lambda expressions is to desugar the lambda body to a method,
 * invoke an invokedynamic call site whose static argument list describes the
 * sole method of the functional interface and the desugared implementation
 * method, and returns an object (the lambda object) that implements the target
 * type. (For method references, the implementation method is simply the
 * referenced method; no desugaring is needed.)
 *
 * <p>The argument list of the implementation method and the argument list of
 * the interface method(s) may differ in several ways.  The implementation
 * methods may have additional arguments to accommodate arguments captured by
 * the lambda expression; there may also be differences resulting from permitted
 * adaptations of arguments, such as casting, boxing, unboxing, and primitive
 * widening. (Varargs adaptations are not handled by the metafactories; these are
 * expected to be handled by the caller.)
 *
 * <p>Invokedynamic call sites have two argument lists: a static argument list
 * and a dynamic argument list.  The static argument list is stored in the
 * constant pool; the dynamic argument is pushed on the operand stack at capture
 * time.  The bootstrap method has access to the entire static argument list
 * (which in this case, includes information describing the implementation method,
 * the target interface, and the target interface method(s)), as well as a
 * method signature describing the number and static types (but not the values)
 * of the dynamic arguments and the static return type of the invokedynamic site.
 *
 * <p>The implementation method is described with a direct method handle
 * referencing a method or constructor. In theory, any method handle could be
 * used, but this is not compatible with some implementation techniques and
 * would complicate the work implementations must do.
 *
 * @since 1.8
 */
public final class LambdaMetafactory {

    private LambdaMetafactory() {}

    /** Flag for {@link #altMetafactory} indicating the lambda object
     * must be serializable */
    public static final int FLAG_SERIALIZABLE = 1 << 0;

    /**
     * Flag for {@link #altMetafactory} indicating the lambda object implements
     * other interfaces besides {@code Serializable}
     */
    public static final int FLAG_MARKERS = 1 << 1;

    /**
     * Flag for alternate metafactories indicating the lambda object requires
     * additional methods that invoke the {@code implementation}
     */
    public static final int FLAG_BRIDGES = 1 << 2;

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];
    private static final MethodType[] EMPTY_MT_ARRAY = new MethodType[0];

    // LambdaMetafactory bootstrap methods are startup sensitive, and may be
    // special cased in java.lang.invoke.BootstrapMethodInvoker to ensure
    // methods are invoked with exact type information to avoid generating
    // code for runtime checks. Take care any changes or additions here are
    // reflected there as appropriate.

    /**
     * Facilitates the creation of simple "function objects" that implement one
     * or more interfaces by delegation to a provided {@link MethodHandle},
     * after appropriate type adaptation and partial evaluation of arguments.
     * Typically used as a <em>bootstrap method</em> for {@code invokedynamic}
     * call sites, to support the <em>lambda expression</em> and <em>method
     * reference expression</em> features of the Java Programming Language.
     *
     * <p>This is the standard, streamlined metafactory; additional flexibility
     * is provided by {@link #altMetafactory(MethodHandles.Lookup, String, MethodType, Object...)}.
     * A general description of the behavior of this method is provided
     * {@link LambdaMetafactory above}.
     *
     * <p>When the target of the {@code CallSite} returned from this method is
     * invoked, the resulting function objects are instances of a class which
     * implements the interface named by the return type of {@code factoryType},
     * declares a method with the name given by {@code interfaceMethodName} and the
     * signature given by {@code interfaceMethodType}.  It may also override additional
     * methods from {@code Object}.
     *
     * @param caller Represents a lookup context with the accessibility
     *               privileges of the caller.  Specifically, the lookup context
     *               must have {@linkplain MethodHandles.Lookup#hasFullPrivilegeAccess()
     *               full privilege access}.
     *               When used with {@code invokedynamic}, this is stacked
     *               automatically by the VM.
     * @param interfaceMethodName The name of the method to implement.  When used with
     *                            {@code invokedynamic}, this is provided by the
     *                            {@code NameAndType} of the {@code InvokeDynamic}
     *                            structure and is stacked automatically by the VM.
     * @param factoryType The expected signature of the {@code CallSite}.  The
     *                    parameter types represent the types of capture variables;
     *                    the return type is the interface to implement.   When
     *                    used with {@code invokedynamic}, this is provided by
     *                    the {@code NameAndType} of the {@code InvokeDynamic}
     *                    structure and is stacked automatically by the VM.
     * @param interfaceMethodType Signature and return type of method to be
     *                            implemented by the function object.
     * @param implementation A direct method handle describing the implementation
     *                       method which should be called (with suitable adaptation
     *                       of argument types and return types, and with captured
     *                       arguments prepended to the invocation arguments) at
     *                       invocation time.
     * @param dynamicMethodType The signature and return type that should
     *                          be enforced dynamically at invocation time.
     *                          In simple use cases this is the same as
     *                          {@code interfaceMethodType}.
     * @return a CallSite whose target can be used to perform capture, generating
     *         instances of the interface named by {@code factoryType}
     * @throws LambdaConversionException If {@code caller} does not have full privilege
     *         access, or if {@code interfaceMethodName} is not a valid JVM
     *         method name, or if the return type of {@code factoryType} is not
     *         an interface, or if {@code implementation} is not a direct method
     *         handle referencing a method or constructor, or if the linkage
     *         invariants are violated, as defined {@link LambdaMetafactory above}.
     * @throws NullPointerException If any argument is {@code null}.
     * @throws SecurityException If a security manager is present, and it
     *         <a href="MethodHandles.Lookup.html#secmgr">refuses access</a>
     *         from {@code caller} to the package of {@code implementation}.
     */
    public static CallSite metafactory(MethodHandles.Lookup caller,
                                       String interfaceMethodName,
                                       MethodType factoryType,
                                       MethodType interfaceMethodType,
                                       MethodHandle implementation,
                                       MethodType dynamicMethodType)
            throws LambdaConversionException {
        AbstractValidatingLambdaMetafactory mf;
        mf = new InnerClassLambdaMetafactory(Objects.requireNonNull(caller),
                                             Objects.requireNonNull(factoryType),
                                             Objects.requireNonNull(interfaceMethodName),
                                             Objects.requireNonNull(interfaceMethodType),
                                             Objects.requireNonNull(implementation),
                                             Objects.requireNonNull(dynamicMethodType),
                                             false,
                                             EMPTY_CLASS_ARRAY,
                                             EMPTY_MT_ARRAY);
        mf.validateMetafactoryArgs();
        return mf.buildCallSite();
    }

    /**
     * Facilitates the creation of simple "function objects" that implement one
     * or more interfaces by delegation to a provided {@link MethodHandle},
     * after appropriate type adaptation and partial evaluation of arguments.
     * Typically used as a <em>bootstrap method</em> for {@code invokedynamic}
     * call sites, to support the <em>lambda expression</em> and <em>method
     * reference expression</em> features of the Java Programming Language.
     *
     * <p>This is the general, more flexible metafactory; a streamlined version
     * is provided by {@link #metafactory(java.lang.invoke.MethodHandles.Lookup,
     * String, MethodType, MethodType, MethodHandle, MethodType)}.
     * A general description of the behavior of this method is provided
     * {@link LambdaMetafactory above}.
     *
     * <p>The argument list for this method includes three fixed parameters,
     * corresponding to the parameters automatically stacked by the VM for the
     * bootstrap method in an {@code invokedynamic} invocation, and an {@code Object[]}
     * parameter that contains additional parameters.  The declared argument
     * list for this method is:
     *
     * <pre>{@code
     *  CallSite altMetafactory(MethodHandles.Lookup caller,
     *                          String interfaceMethodName,
     *                          MethodType factoryType,
     *                          Object... args)
     * }</pre>
     *
     * <p>but it behaves as if the argument list is as follows:
     *
     * <pre>{@code
     *  CallSite altMetafactory(MethodHandles.Lookup caller,
     *                          String interfaceMethodName,
     *                          MethodType factoryType,
     *                          MethodType interfaceMethodType,
     *                          MethodHandle implementation,
     *                          MethodType dynamicMethodType,
     *                          int flags,
     *                          int altInterfaceCount,        // IF flags has MARKERS set
     *                          Class... altInterfaces,       // IF flags has MARKERS set
     *                          int altMethodCount,           // IF flags has BRIDGES set
     *                          MethodType... altMethods      // IF flags has BRIDGES set
     *                          )
     * }</pre>
     *
     * <p>Arguments that appear in the argument list for
     * {@link #metafactory(MethodHandles.Lookup, String, MethodType, MethodType, MethodHandle, MethodType)}
     * have the same specification as in that method.  The additional arguments
     * are interpreted as follows:
     * <ul>
     *     <li>{@code flags} indicates additional options; this is a bitwise
     *     OR of desired flags.  Defined flags are {@link #FLAG_BRIDGES},
     *     {@link #FLAG_MARKERS}, and {@link #FLAG_SERIALIZABLE}.</li>
     *     <li>{@code altInterfaceCount} is the number of additional interfaces
     *     the function object should implement, and is present if and only if the
     *     {@code FLAG_MARKERS} flag is set.</li>
     *     <li>{@code altInterfaces} is a variable-length list of additional
     *     interfaces to implement, whose length equals {@code altInterfaceCount},
     *     and is present if and only if the {@code FLAG_MARKERS} flag is set.</li>
     *     <li>{@code altMethodCount} is the number of additional method signatures
     *     the function object should implement, and is present if and only if
     *     the {@code FLAG_BRIDGES} flag is set.</li>
     *     <li>{@code altMethods} is a variable-length list of additional
     *     methods signatures to implement, whose length equals {@code altMethodCount},
     *     and is present if and only if the {@code FLAG_BRIDGES} flag is set.</li>
     * </ul>
     *
     * <p>Each class named by {@code altInterfaces} is subject to the same
     * restrictions as {@code Rd}, the return type of {@code factoryType},
     * as described {@link LambdaMetafactory above}.  Each {@code MethodType}
     * named by {@code altMethods} is subject to the same restrictions as
     * {@code interfaceMethodType}, as described {@link LambdaMetafactory above}.
     *
     * <p>When FLAG_SERIALIZABLE is set in {@code flags}, the function objects
     * will implement {@code Serializable}, and will have a {@code writeReplace}
     * method that returns an appropriate {@link SerializedLambda}.  The
     * {@code caller} class must have an appropriate {@code $deserializeLambda$}
     * method, as described in {@link SerializedLambda}.
     *
     * <p>When the target of the {@code CallSite} returned from this method is
     * invoked, the resulting function objects are instances of a class with
     * the following properties:
     * <ul>
     *     <li>The class implements the interface named by the return type
     *     of {@code factoryType} and any interfaces named by {@code altInterfaces}</li>
     *     <li>The class declares methods with the name given by {@code interfaceMethodName},
     *     and the signature given by {@code interfaceMethodType} and additional signatures
     *     given by {@code altMethods}</li>
     *     <li>The class may override methods from {@code Object}, and may
     *     implement methods related to serialization.</li>
     * </ul>
     *
     * @param caller Represents a lookup context with the accessibility
     *               privileges of the caller.  Specifically, the lookup context
     *               must have {@linkplain MethodHandles.Lookup#hasFullPrivilegeAccess()
     *               full privilege access}.
     *               When used with {@code invokedynamic}, this is stacked
     *               automatically by the VM.
     * @param interfaceMethodName The name of the method to implement.  When used with
     *                            {@code invokedynamic}, this is provided by the
     *                            {@code NameAndType} of the {@code InvokeDynamic}
     *                            structure and is stacked automatically by the VM.
     * @param factoryType The expected signature of the {@code CallSite}.  The
     *                    parameter types represent the types of capture variables;
     *                    the return type is the interface to implement.   When
     *                    used with {@code invokedynamic}, this is provided by
     *                    the {@code NameAndType} of the {@code InvokeDynamic}
     *                    structure and is stacked automatically by the VM.
     * @param  args An array of {@code Object} containing the required
     *              arguments {@code interfaceMethodType}, {@code implementation},
     *              {@code dynamicMethodType}, {@code flags}, and any
     *              optional arguments, as described above
     * @return a CallSite whose target can be used to perform capture, generating
     *         instances of the interface named by {@code factoryType}
     * @throws LambdaConversionException If {@code caller} does not have full privilege
     *         access, or if {@code interfaceMethodName} is not a valid JVM
     *         method name, or if the return type of {@code factoryType} is not
     *         an interface, or if any of {@code altInterfaces} is not an
     *         interface, or if {@code implementation} is not a direct method
     *         handle referencing a method or constructor, or if the linkage
     *         invariants are violated, as defined {@link LambdaMetafactory above}.
     * @throws NullPointerException If any argument, or any component of {@code args},
     *         is {@code null}.
     * @throws IllegalArgumentException If the number or types of the components
     *         of {@code args} do not follow the above rules, or if
     *         {@code altInterfaceCount} or {@code altMethodCount} are negative
     *         integers.
     * @throws SecurityException If a security manager is present, and it
     *         <a href="MethodHandles.Lookup.html#secmgr">refuses access</a>
     *         from {@code caller} to the package of {@code implementation}.
     */
    public static CallSite altMetafactory(MethodHandles.Lookup caller,
                                          String interfaceMethodName,
                                          MethodType factoryType,
                                          Object... args)
            throws LambdaConversionException {
        Objects.requireNonNull(caller);
        Objects.requireNonNull(interfaceMethodName);
        Objects.requireNonNull(factoryType);
        Objects.requireNonNull(args);
        int argIndex = 0;
        MethodType interfaceMethodType = extractArg(args, argIndex++, MethodType.class);
        MethodHandle implementation = extractArg(args, argIndex++, MethodHandle.class);
        MethodType dynamicMethodType = extractArg(args, argIndex++, MethodType.class);
        int flags = extractArg(args, argIndex++, Integer.class);
        Class<?>[] altInterfaces = EMPTY_CLASS_ARRAY;
        MethodType[] altMethods = EMPTY_MT_ARRAY;
        if ((flags & FLAG_MARKERS) != 0) {
            int altInterfaceCount = extractArg(args, argIndex++, Integer.class);
            if (altInterfaceCount < 0) {
                throw new IllegalArgumentException("negative argument count");
            }
            if (altInterfaceCount > 0) {
                altInterfaces = extractArgs(args, argIndex, Class.class, altInterfaceCount);
                argIndex += altInterfaceCount;
            }
        }
        if ((flags & FLAG_BRIDGES) != 0) {
            int altMethodCount = extractArg(args, argIndex++, Integer.class);
            if (altMethodCount < 0) {
                throw new IllegalArgumentException("negative argument count");
            }
            if (altMethodCount > 0) {
                altMethods = extractArgs(args, argIndex, MethodType.class, altMethodCount);
                argIndex += altMethodCount;
            }
        }
        if (argIndex < args.length) {
            throw new IllegalArgumentException("too many arguments");
        }

        boolean isSerializable = ((flags & FLAG_SERIALIZABLE) != 0);
        if (isSerializable) {
            boolean foundSerializableSupertype = Serializable.class.isAssignableFrom(factoryType.returnType());
            for (Class<?> c : altInterfaces)
                foundSerializableSupertype |= Serializable.class.isAssignableFrom(c);
            if (!foundSerializableSupertype) {
                altInterfaces = Arrays.copyOf(altInterfaces, altInterfaces.length + 1);
                altInterfaces[altInterfaces.length-1] = Serializable.class;
            }
        }

        AbstractValidatingLambdaMetafactory mf
                = new InnerClassLambdaMetafactory(caller,
                                                  factoryType,
                                                  interfaceMethodName,
                                                  interfaceMethodType,
                                                  implementation,
                                                  dynamicMethodType,
                                                  isSerializable,
                                                  altInterfaces,
                                                  altMethods);
        mf.validateMetafactoryArgs();
        return mf.buildCallSite();
    }

    private static <T> T extractArg(Object[] args, int index, Class<T> type) {
        if (index >= args.length) {
            throw new IllegalArgumentException("missing argument");
        }
        Object result = Objects.requireNonNull(args[index]);
        if (!type.isInstance(result)) {
            throw new IllegalArgumentException("argument has wrong type");
        }
        return type.cast(result);
    }

    private static <T> T[] extractArgs(Object[] args, int index, Class<T> type, int count) {
        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(type, count);
        for (int i = 0; i < count; i++) {
            result[i] = extractArg(args, index + i, type);
        }
        return result;
    }

}
