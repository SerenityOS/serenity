/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

import sun.invoke.util.Wrapper;

import static java.lang.invoke.MethodHandleNatives.mapLookupExceptionToError;
import static java.util.Objects.requireNonNull;

/**
 * Bootstrap methods for dynamically-computed constants.
 *
 * <p>The bootstrap methods in this class will throw a
 * {@code NullPointerException} for any reference argument that is {@code null},
 * unless the argument is specified to be unused or specified to accept a
 * {@code null} value.
 *
 * @since 11
 */
public final class ConstantBootstraps {
    /**
     * Do not call.
     */
    private ConstantBootstraps() {throw new AssertionError();}

    // implements the upcall from the JVM, MethodHandleNatives.linkDynamicConstant:
    /*non-public*/
    static Object makeConstant(MethodHandle bootstrapMethod,
                               // Callee information:
                               String name, Class<?> type,
                               // Extra arguments for BSM, if any:
                               Object info,
                               // Caller information:
                               Class<?> callerClass) {
        // Restrict bootstrap methods to those whose first parameter is Lookup
        // The motivation here is, in the future, to possibly support BSMs
        // that do not accept the meta-data of lookup/name/type, thereby
        // allowing the co-opting of existing methods to be used as BSMs as
        // long as the static arguments can be passed as method arguments
        MethodType mt = bootstrapMethod.type();
        if (mt.parameterCount() < 2 ||
            !MethodHandles.Lookup.class.isAssignableFrom(mt.parameterType(0))) {
            throw new BootstrapMethodError(
                    "Invalid bootstrap method declared for resolving a dynamic constant: " + bootstrapMethod);
        }

        // BSMI.invoke handles all type checking and exception translation.
        // If type is not a reference type, the JVM is expecting a boxed
        // version, and will manage unboxing on the other side.
        return BootstrapMethodInvoker.invoke(
                type, bootstrapMethod, name, type, info, callerClass);
    }

    /**
     * Returns a {@code null} object reference for the reference type specified
     * by {@code type}.
     *
     * @param lookup unused
     * @param name unused
     * @param type a reference type
     * @return a {@code null} value
     * @throws IllegalArgumentException if {@code type} is not a reference type
     */
    public static Object nullConstant(MethodHandles.Lookup lookup, String name, Class<?> type) {
        if (requireNonNull(type).isPrimitive()) {
            throw new IllegalArgumentException(String.format("not reference: %s", type));
        }

        return null;
    }

    /**
     * Returns a {@link Class} mirror for the primitive type whose type
     * descriptor is specified by {@code name}.
     *
     * @param lookup unused
     * @param name the descriptor (JVMS 4.3) of the desired primitive type
     * @param type the required result type (must be {@code Class.class})
     * @return the {@link Class} mirror
     * @throws IllegalArgumentException if the name is not a descriptor for a
     * primitive type or the type is not {@code Class.class}
     */
    public static Class<?> primitiveClass(MethodHandles.Lookup lookup, String name, Class<?> type) {
        requireNonNull(name);
        requireNonNull(type);
        if (type != Class.class) {
            throw new IllegalArgumentException();
        }
        if (name.length() != 1) {
            throw new IllegalArgumentException(String.format("not primitive: %s", name));
        }

        return Wrapper.forPrimitiveType(name.charAt(0)).primitiveType();
    }

    /**
     * Returns an {@code enum} constant of the type specified by {@code type}
     * with the name specified by {@code name}.
     *
     * @param lookup the lookup context describing the class performing the
     * operation (normally stacked by the JVM)
     * @param name the name of the constant to return, which must exactly match
     * an enum constant in the specified type.
     * @param type the {@code Class} object describing the enum type for which
     * a constant is to be returned
     * @param <E> The enum type for which a constant value is to be returned
     * @return the enum constant of the specified enum type with the
     * specified name
     * @throws IllegalAccessError if the declaring class or the field is not
     * accessible to the class performing the operation
     * @throws IllegalArgumentException if the specified enum type has
     * no constant with the specified name, or the specified
     * class object does not represent an enum type
     * @see Enum#valueOf(Class, String)
     */
    public static <E extends Enum<E>> E enumConstant(MethodHandles.Lookup lookup, String name, Class<E> type) {
        requireNonNull(lookup);
        requireNonNull(name);
        requireNonNull(type);
        validateClassAccess(lookup, type);

        return Enum.valueOf(type, name);
    }

    /**
     * Returns the value of a static final field.
     *
     * @param lookup the lookup context describing the class performing the
     * operation (normally stacked by the JVM)
     * @param name the name of the field
     * @param type the type of the field
     * @param declaringClass the class in which the field is declared
     * @return the value of the field
     * @throws IllegalAccessError if the declaring class or the field is not
     * accessible to the class performing the operation
     * @throws NoSuchFieldError if the specified field does not exist
     * @throws IncompatibleClassChangeError if the specified field is not
     * {@code final}
     */
    public static Object getStaticFinal(MethodHandles.Lookup lookup, String name, Class<?> type,
                                        Class<?> declaringClass) {
        requireNonNull(lookup);
        requireNonNull(name);
        requireNonNull(type);
        requireNonNull(declaringClass);

        MethodHandle mh;
        try {
            mh = lookup.findStaticGetter(declaringClass, name, type);
            MemberName member = mh.internalMemberName();
            if (!member.isFinal()) {
                throw new IncompatibleClassChangeError("not a final field: " + name);
            }
        }
        catch (ReflectiveOperationException ex) {
            throw mapLookupExceptionToError(ex);
        }

        // Since mh is a handle to a static field only instances of
        // VirtualMachineError are anticipated to be thrown, such as a
        // StackOverflowError or an InternalError from the j.l.invoke code
        try {
            return mh.invoke();
        }
        catch (RuntimeException | Error e) {
            throw e;
        }
        catch (Throwable e) {
            throw new LinkageError("Unexpected throwable", e);
        }
    }

    /**
     * Returns the value of a static final field declared in the class which
     * is the same as the field's type (or, for primitive-valued fields,
     * declared in the wrapper class.)  This is a simplified form of
     * {@link #getStaticFinal(MethodHandles.Lookup, String, Class, Class)}
     * for the case where a class declares distinguished constant instances of
     * itself.
     *
     * @param lookup the lookup context describing the class performing the
     * operation (normally stacked by the JVM)
     * @param name the name of the field
     * @param type the type of the field
     * @return the value of the field
     * @throws IllegalAccessError if the declaring class or the field is not
     * accessible to the class performing the operation
     * @throws NoSuchFieldError if the specified field does not exist
     * @throws IncompatibleClassChangeError if the specified field is not
     * {@code final}
     * @see #getStaticFinal(MethodHandles.Lookup, String, Class, Class)
     */
    public static Object getStaticFinal(MethodHandles.Lookup lookup, String name, Class<?> type) {
        requireNonNull(type);

        Class<?> declaring = type.isPrimitive()
                             ? Wrapper.forPrimitiveType(type).wrapperType()
                             : type;
        return getStaticFinal(lookup, name, type, declaring);
    }


    /**
     * Returns the result of invoking a method handle with the provided
     * arguments.
     * <p>
     * This method behaves as if the method handle to be invoked is the result
     * of adapting the given method handle, via {@link MethodHandle#asType}, to
     * adjust the return type to the desired type.
     *
     * @param lookup unused
     * @param name unused
     * @param type the desired type of the value to be returned, which must be
     * compatible with the return type of the method handle
     * @param handle the method handle to be invoked
     * @param args the arguments to pass to the method handle, as if with
     * {@link MethodHandle#invokeWithArguments}.  Each argument may be
     * {@code null}.
     * @return the result of invoking the method handle
     * @throws WrongMethodTypeException if the handle's method type cannot be
     * adjusted to take the given number of arguments, or if the handle's return
     * type cannot be adjusted to the desired type
     * @throws ClassCastException if an argument or the result produced by
     * invoking the handle cannot be converted by reference casting
     * @throws Throwable anything thrown by the method handle invocation
     */
    public static Object invoke(MethodHandles.Lookup lookup, String name, Class<?> type,
                                MethodHandle handle, Object... args) throws Throwable {
        requireNonNull(type);
        requireNonNull(handle);
        requireNonNull(args);

        if (type != handle.type().returnType()) {
            // Adjust the return type of the handle to be invoked while
            // preserving variable arity if present
            handle = handle.asType(handle.type().changeReturnType(type)).
                    withVarargs(handle.isVarargsCollector());
        }

        return handle.invokeWithArguments(args);
    }

    /**
     * Finds a {@link VarHandle} for an instance field.
     *
     * @param lookup the lookup context describing the class performing the
     * operation (normally stacked by the JVM)
     * @param name the name of the field
     * @param type the required result type (must be {@code Class<VarHandle>})
     * @param declaringClass the class in which the field is declared
     * @param fieldType the type of the field
     * @return the {@link VarHandle}
     * @throws IllegalAccessError if the declaring class or the field is not
     * accessible to the class performing the operation
     * @throws NoSuchFieldError if the specified field does not exist
     * @throws IllegalArgumentException if the type is not {@code VarHandle}
     */
    public static VarHandle fieldVarHandle(MethodHandles.Lookup lookup, String name, Class<VarHandle> type,
                                           Class<?> declaringClass, Class<?> fieldType) {
        requireNonNull(lookup);
        requireNonNull(name);
        requireNonNull(type);
        requireNonNull(declaringClass);
        requireNonNull(fieldType);
        if (type != VarHandle.class) {
            throw new IllegalArgumentException();
        }

        try {
            return lookup.findVarHandle(declaringClass, name, fieldType);
        }
        catch (ReflectiveOperationException e) {
            throw mapLookupExceptionToError(e);
        }
    }

    /**
     * Finds a {@link VarHandle} for a static field.
     *
     * @param lookup the lookup context describing the class performing the
     * operation (normally stacked by the JVM)
     * @param name the name of the field
     * @param type the required result type (must be {@code Class<VarHandle>})
     * @param declaringClass the class in which the field is declared
     * @param fieldType the type of the field
     * @return the {@link VarHandle}
     * @throws IllegalAccessError if the declaring class or the field is not
     * accessible to the class performing the operation
     * @throws NoSuchFieldError if the specified field does not exist
     * @throws IllegalArgumentException if the type is not {@code VarHandle}
     */
    public static VarHandle staticFieldVarHandle(MethodHandles.Lookup lookup, String name, Class<VarHandle> type,
                                                 Class<?> declaringClass, Class<?> fieldType) {
        requireNonNull(lookup);
        requireNonNull(name);
        requireNonNull(type);
        requireNonNull(declaringClass);
        requireNonNull(fieldType);
        if (type != VarHandle.class) {
            throw new IllegalArgumentException();
        }

        try {
            return lookup.findStaticVarHandle(declaringClass, name, fieldType);
        }
        catch (ReflectiveOperationException e) {
            throw mapLookupExceptionToError(e);
        }
    }

    /**
     * Finds a {@link VarHandle} for an array type.
     *
     * @param lookup the lookup context describing the class performing the
     * operation (normally stacked by the JVM)
     * @param name unused
     * @param type the required result type (must be {@code Class<VarHandle>})
     * @param arrayClass the type of the array
     * @return the {@link VarHandle}
     * @throws IllegalAccessError if the component type of the array is not
     * accessible to the class performing the operation
     * @throws IllegalArgumentException if the type is not {@code VarHandle}
     */
    public static VarHandle arrayVarHandle(MethodHandles.Lookup lookup, String name, Class<VarHandle> type,
                                           Class<?> arrayClass) {
        requireNonNull(lookup);
        requireNonNull(type);
        requireNonNull(arrayClass);
        if (type != VarHandle.class) {
            throw new IllegalArgumentException();
        }

        return MethodHandles.arrayElementVarHandle(validateClassAccess(lookup, arrayClass));
    }

    /**
     * Applies a conversion from a source type to a destination type.
     * <p>
     * Given a destination type {@code dstType} and an input
     * value {@code value}, one of the following will happen:
     * <ul>
     * <li>If {@code dstType} is {@code void.class},
     *     a {@link ClassCastException} is thrown.
     * <li>If {@code dstType} is {@code Object.class}, {@code value} is returned as is.
     * </ul>
     * <p>
     * Otherwise one of the following conversions is applied to {@code value}:
     * <ol>
     * <li>If {@code dstType} is a reference type, a reference cast
     *     is applied to {@code value} as if by calling {@code dstType.cast(value)}.
     * <li>If {@code dstType} is a primitive type, then, if the runtime type
     *     of {@code value} is a primitive wrapper type (such as {@link Integer}),
     *     a Java unboxing conversion is applied {@jls 5.1.8} followed by a
     *     Java casting conversion {@jls 5.5} converting either directly to
     *     {@code dstType}, or, if {@code dstType} is {@code boolean},
     *     to {@code int}, which is then converted to either {@code true}
     *     or {@code false} depending on whether the least-significant-bit
     *     is 1 or 0 respectively. If the runtime type of {@code value} is
     *     not a primitive wrapper type a {@link ClassCastException} is thrown.
     * </ol>
     * <p>
     * The result is the same as when using the following code:
     * <blockquote><pre>{@code
     * MethodHandle id = MethodHandles.identity(dstType);
     * MethodType mt = MethodType.methodType(dstType, Object.class);
     * MethodHandle conv = MethodHandles.explicitCastArguments(id, mt);
     * return conv.invoke(value);
     * }</pre></blockquote>
     *
     * @param lookup unused
     * @param name unused
     * @param dstType the destination type of the conversion
     * @param value the value to be converted
     * @return the converted value
     * @throws ClassCastException when {@code dstType} is {@code void},
     *         when a cast per (1) fails, or when {@code dstType} is a primitive type
     *         and the runtime type of {@code value} is not a primitive wrapper type
     *         (such as {@link Integer})
     *
     * @since 15
     */
    public static Object explicitCast(MethodHandles.Lookup lookup, String name, Class<?> dstType, Object value)
            throws ClassCastException {
        if (dstType == void.class)
            throw new ClassCastException("Can not convert to void");
        if (dstType == Object.class)
            return value;

        MethodHandle id = MethodHandles.identity(dstType);
        MethodType mt = MethodType.methodType(dstType, Object.class);
        MethodHandle conv = MethodHandles.explicitCastArguments(id, mt);
        try {
            return conv.invoke(value);
        } catch (RuntimeException|Error e) {
            throw e; // let specified CCE and other runtime exceptions/errors through
        } catch (Throwable throwable) {
            throw new InternalError(throwable); // Not specified, throw InternalError
        }
    }

    private static <T> Class<T> validateClassAccess(MethodHandles.Lookup lookup, Class<T> type) {
        try {
            lookup.accessClass(type);
            return type;
        }
        catch (ReflectiveOperationException ex) {
            throw mapLookupExceptionToError(ex);
        }
    }
}
