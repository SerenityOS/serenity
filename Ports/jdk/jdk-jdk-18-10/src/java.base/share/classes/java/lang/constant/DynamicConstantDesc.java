/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package java.lang.constant;

import java.lang.Enum.EnumDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.invoke.VarHandle.VarHandleDesc;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.constant.ConstantDescs.CD_Class;
import static java.lang.constant.ConstantDescs.CD_VarHandle;
import static java.lang.constant.ConstantDescs.DEFAULT_NAME;
import static java.lang.constant.ConstantUtils.EMPTY_CONSTANTDESC;
import static java.lang.constant.ConstantUtils.validateMemberName;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * A <a href="package-summary.html#nominal">nominal descriptor</a> for a
 * dynamic constant (one described in the constant pool with
 * {@code Constant_Dynamic_info}.)
 *
 * <p>Concrete subtypes of {@linkplain DynamicConstantDesc} should be immutable
 * and their behavior should not rely on object identity.
 *
 * @param <T> the type of the dynamic constant
 *
 * @since 12
 */
public abstract non-sealed class DynamicConstantDesc<T>
        implements ConstantDesc {

    private final DirectMethodHandleDesc bootstrapMethod;
    private final ConstantDesc[] bootstrapArgs;
    private final String constantName;
    private final ClassDesc constantType;

    /**
     * Creates a nominal descriptor for a dynamic constant.
     *
     * @param bootstrapMethod a {@link DirectMethodHandleDesc} describing the
     *                        bootstrap method for the constant
     * @param constantName The unqualified name that would appear in the {@code NameAndType}
     *                     operand of the {@code LDC} for this constant
     * @param constantType a {@link ClassDesc} describing the type
     *                     that would appear in the {@code NameAndType} operand
     *                     of the {@code LDC} for this constant
     * @param bootstrapArgs {@link ConstantDesc}s describing the static arguments
     *                      to the bootstrap, that would appear in the
     *                      {@code BootstrapMethods} attribute
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if the {@code name} has the incorrect
     * format
     * @jvms 4.2.2 Unqualified Names
     */
    protected DynamicConstantDesc(DirectMethodHandleDesc bootstrapMethod,
                                  String constantName,
                                  ClassDesc constantType,
                                  ConstantDesc... bootstrapArgs) {
        this.bootstrapMethod = requireNonNull(bootstrapMethod);
        this.constantName = validateMemberName(requireNonNull(constantName), true);
        this.constantType = requireNonNull(constantType);
        this.bootstrapArgs = requireNonNull(bootstrapArgs).clone();

        if (constantName.length() == 0)
            throw new IllegalArgumentException("Illegal invocation name: " + constantName);
    }

    /**
     * Returns a nominal descriptor for a dynamic constant, transforming it into
     * a more specific type if the constant bootstrap is a well-known one and a
     * more specific nominal descriptor type (e.g., ClassDesc) is available.
     *
     * <p>Classes whose {@link Constable#describeConstable()} method produce
     * a {@linkplain DynamicConstantDesc} with a well-known bootstrap including
     * {@link Class} (for instances describing primitive types), {@link Enum},
     * and {@link VarHandle}.
     *
     * <p>Bytecode-reading APIs that process the constant pool and wish to expose
     * entries as {@link ConstantDesc} to their callers should generally use this
     * method in preference to {@link #ofNamed(DirectMethodHandleDesc, String, ClassDesc, ConstantDesc...)}
     * because this may result in a more specific type that can be provided to
     * callers.
     *
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod a {@link DirectMethodHandleDesc} describing the
     *                        bootstrap method for the constant
     * @param constantName The unqualified name that would appear in the {@code NameAndType}
     *                     operand of the {@code LDC} for this constant
     * @param constantType a {@link ClassDesc} describing the type
     *                     that would appear in the {@code NameAndType} operand
     *                     of the {@code LDC} for this constant
     * @param bootstrapArgs {@link ConstantDesc}s describing the static arguments
     *                      to the bootstrap, that would appear in the
     *                      {@code BootstrapMethods} attribute
     * @return the nominal descriptor
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if the {@code name} has the incorrect
     * format
     * @jvms 4.2.2 Unqualified Names
     */
    // Do not call this method from the static initialization of java.lang.constant.ConstantDescs
    // since that can lead to potential deadlock during multi-threaded concurrent execution
    public static<T> ConstantDesc ofCanonical(DirectMethodHandleDesc bootstrapMethod,
                                              String constantName,
                                              ClassDesc constantType,
                                              ConstantDesc[] bootstrapArgs) {
        return DynamicConstantDesc.<T>ofNamed(bootstrapMethod, constantName, constantType, bootstrapArgs)
                .tryCanonicalize();
    }

    /**
     * Returns a nominal descriptor for a dynamic constant.
     *
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod a {@link DirectMethodHandleDesc} describing the
     *                        bootstrap method for the constant
     * @param constantName The unqualified name that would appear in the {@code NameAndType}
     *                     operand of the {@code LDC} for this constant
     * @param constantType a {@link ClassDesc} describing the type
     *                     that would appear in the {@code NameAndType} operand
     *                     of the {@code LDC} for this constant
     * @param bootstrapArgs {@link ConstantDesc}s describing the static arguments
     *                      to the bootstrap, that would appear in the
     *                      {@code BootstrapMethods} attribute
     * @return the nominal descriptor
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if the {@code name} has the incorrect
     * format
     * @jvms 4.2.2 Unqualified Names
     */

    public static<T> DynamicConstantDesc<T> ofNamed(DirectMethodHandleDesc bootstrapMethod,
                                                    String constantName,
                                                    ClassDesc constantType,
                                                    ConstantDesc... bootstrapArgs) {
        return new AnonymousDynamicConstantDesc<>(bootstrapMethod, constantName, constantType, bootstrapArgs);
    }

    /**
     * Returns a nominal descriptor for a dynamic constant whose name parameter
     * is {@link ConstantDescs#DEFAULT_NAME}, and whose type parameter is always
     * the same as the bootstrap method return type.
     *
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod a {@link DirectMethodHandleDesc} describing the
     *                        bootstrap method for the constant
     * @param bootstrapArgs {@link ConstantDesc}s describing the static arguments
     *                      to the bootstrap, that would appear in the
     *                      {@code BootstrapMethods} attribute
     * @return the nominal descriptor
     * @throws NullPointerException if any argument is null
     * @jvms 4.2.2 Unqualified Names
     */
    public static<T> DynamicConstantDesc<T> of(DirectMethodHandleDesc bootstrapMethod,
                                               ConstantDesc... bootstrapArgs) {
        return ofNamed(bootstrapMethod, DEFAULT_NAME, bootstrapMethod.invocationType().returnType(), bootstrapArgs);
    }

    /**
     * Returns a nominal descriptor for a dynamic constant whose bootstrap has
     * no static arguments, whose name parameter is {@link ConstantDescs#DEFAULT_NAME},
     * and whose type parameter is always the same as the bootstrap method return type.
     *
     * @param <T> the type of the dynamic constant
     * @param bootstrapMethod a {@link DirectMethodHandleDesc} describing the
     *                        bootstrap method for the constant
     * @return the nominal descriptor
     * @throws NullPointerException if any argument is null
     */
    public static<T> DynamicConstantDesc<T> of(DirectMethodHandleDesc bootstrapMethod) {
        return of(bootstrapMethod, EMPTY_CONSTANTDESC);
    }

    /**
     * Returns the name that would appear in the {@code NameAndType} operand
     * of the {@code LDC} for this constant.
     *
     * @return the constant name
     */
    public String constantName() {
        return constantName;
    }

    /**
     * Returns a {@link ClassDesc} describing the type that would appear in the
     * {@code NameAndType} operand of the {@code LDC} for this constant.
     *
     * @return the constant type
     */
    public ClassDesc constantType() {
        return constantType;
    }

    /**
     * Returns a {@link MethodHandleDesc} describing the bootstrap method for
     * this constant.
     *
     * @return the bootstrap method
     */
    public DirectMethodHandleDesc bootstrapMethod() {
        return bootstrapMethod;
    }

    /**
     * Returns the bootstrap arguments for this constant.
     *
     * @return the bootstrap arguments
     */
    public ConstantDesc[] bootstrapArgs() {
        return bootstrapArgs.clone();
    }

    /**
     * Returns the bootstrap arguments for this constant as an immutable {@link List}.
     *
     * @return a {@link List} of the bootstrap arguments
     */
    public List<ConstantDesc> bootstrapArgsList() {
        return List.of(bootstrapArgs);
    }

    @SuppressWarnings("unchecked")
    public T resolveConstantDesc(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        try {
            MethodHandle bsm = (MethodHandle) bootstrapMethod.resolveConstantDesc(lookup);
            if (bsm.type().parameterCount() < 2 ||
                !MethodHandles.Lookup.class.isAssignableFrom(bsm.type().parameterType(0))) {
                throw new BootstrapMethodError(
                        "Invalid bootstrap method declared for resolving a dynamic constant: " + bootstrapMethod);
            }
            Object[] bsmArgs = new Object[3 + bootstrapArgs.length];
            bsmArgs[0] = lookup;
            bsmArgs[1] = constantName;
            bsmArgs[2] = constantType.resolveConstantDesc(lookup);
            for (int i = 0; i < bootstrapArgs.length; i++)
                bsmArgs[3 + i] = bootstrapArgs[i].resolveConstantDesc(lookup);

            return (T) bsm.invokeWithArguments(bsmArgs);
        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            throw new BootstrapMethodError(t);
        }
    }

    private ConstantDesc tryCanonicalize() {
        Function<DynamicConstantDesc<?>, ConstantDesc> f = CanonicalMapHolder.CANONICAL_MAP.get(bootstrapMethod);
        if (f != null) {
            try {
                return f.apply(this);
            }
            catch (Throwable t) {
                return this;
            }
        }
        return this;
    }

    private static ConstantDesc canonicalizeNull(DynamicConstantDesc<?> desc) {
        if (desc.bootstrapArgs.length != 0)
            return desc;
        return ConstantDescs.NULL;
    }

    private static ConstantDesc canonicalizeEnum(DynamicConstantDesc<?> desc) {
        if (desc.bootstrapArgs.length != 0
            || desc.constantName == null)
            return desc;
        return EnumDesc.of(desc.constantType, desc.constantName);
    }

    private static ConstantDesc canonicalizePrimitiveClass(DynamicConstantDesc<?> desc) {
        if (desc.bootstrapArgs.length != 0
            || !desc.constantType().equals(CD_Class)
            || desc.constantName == null)
            return desc;
        return ClassDesc.ofDescriptor(desc.constantName);
    }

    private static ConstantDesc canonicalizeStaticFieldVarHandle(DynamicConstantDesc<?> desc) {
        if (desc.bootstrapArgs.length != 2
                || !desc.constantType().equals(CD_VarHandle))
            return desc;
        return VarHandleDesc.ofStaticField((ClassDesc) desc.bootstrapArgs[0],
                                     desc.constantName,
                                     (ClassDesc) desc.bootstrapArgs[1]);
    }

    private static ConstantDesc canonicalizeFieldVarHandle(DynamicConstantDesc<?> desc) {
        if (desc.bootstrapArgs.length != 2
            || !desc.constantType().equals(CD_VarHandle))
            return desc;
        return VarHandleDesc.ofField((ClassDesc) desc.bootstrapArgs[0],
                                     desc.constantName,
                                     (ClassDesc) desc.bootstrapArgs[1]);
    }

    private static ConstantDesc canonicalizeArrayVarHandle(DynamicConstantDesc<?> desc) {
        if (desc.bootstrapArgs.length != 1
            || !desc.constantType().equals(CD_VarHandle))
            return desc;
        return VarHandleDesc.ofArray((ClassDesc) desc.bootstrapArgs[0]);
    }

    // @@@ To eventually support in canonicalization: DCR with BSM=MHR_METHODHANDLEDESC_ASTYPE becomes AsTypeMHDesc

    /**
     * Compares the specified object with this descriptor for equality.  Returns
     * {@code true} if and only if the specified object is also a
     * {@linkplain DynamicConstantDesc}, and both descriptors have equal
     * bootstrap methods, bootstrap argument lists, constant name, and
     * constant type.
     *
     * @param o the {@code DynamicConstantDesc} to compare to this
     *       {@code DynamicConstantDesc}
     * @return {@code true} if the specified {@code DynamicConstantDesc}
     *      is equal to this {@code DynamicConstantDesc}.
     *
     */
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        return (o instanceof DynamicConstantDesc<?> desc)
                && Objects.equals(bootstrapMethod, desc.bootstrapMethod)
                && Arrays.equals(bootstrapArgs, desc.bootstrapArgs)
                && Objects.equals(constantName, desc.constantName)
                && Objects.equals(constantType, desc.constantType);
    }

    @Override
    public final int hashCode() {
        int result = Objects.hash(bootstrapMethod, constantName, constantType);
        result = 31 * result + Arrays.hashCode(bootstrapArgs);
        return result;
    }

    /**
     * Returns a compact textual description of this constant description,
     * including the bootstrap method, the constant name and type, and
     * the static bootstrap arguments.
     *
     * @return A compact textual description of this call site descriptor
     */
    @Override
    public String toString() {
        return String.format("DynamicConstantDesc[%s::%s(%s%s)%s]",
                             bootstrapMethod.owner().displayName(),
                             bootstrapMethod.methodName(),
                             constantName.equals(ConstantDescs.DEFAULT_NAME) ? "" : constantName + "/",
                             Stream.of(bootstrapArgs).map(Object::toString).collect(joining(",")),
                             constantType.displayName());
    }

    private static class AnonymousDynamicConstantDesc<T> extends DynamicConstantDesc<T> {
        AnonymousDynamicConstantDesc(DirectMethodHandleDesc bootstrapMethod, String constantName, ClassDesc constantType, ConstantDesc... bootstrapArgs) {
            super(bootstrapMethod, constantName, constantType, bootstrapArgs);
        }
    }

    private static final class CanonicalMapHolder {
        static final Map<MethodHandleDesc, Function<DynamicConstantDesc<?>, ConstantDesc>> CANONICAL_MAP =
                Map.ofEntries(
                    Map.entry(ConstantDescs.BSM_PRIMITIVE_CLASS, DynamicConstantDesc::canonicalizePrimitiveClass),
                    Map.entry(ConstantDescs.BSM_ENUM_CONSTANT, DynamicConstantDesc::canonicalizeEnum),
                    Map.entry(ConstantDescs.BSM_NULL_CONSTANT, DynamicConstantDesc::canonicalizeNull),
                    Map.entry(ConstantDescs.BSM_VARHANDLE_STATIC_FIELD, DynamicConstantDesc::canonicalizeStaticFieldVarHandle),
                    Map.entry(ConstantDescs.BSM_VARHANDLE_FIELD, DynamicConstantDesc::canonicalizeFieldVarHandle),
                    Map.entry(ConstantDescs.BSM_VARHANDLE_ARRAY, DynamicConstantDesc::canonicalizeArrayVarHandle));
    }
}
