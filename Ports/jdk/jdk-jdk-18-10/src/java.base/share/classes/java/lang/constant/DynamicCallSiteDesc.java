/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.constant.ConstantDescs.CD_String;
import static java.lang.constant.ConstantUtils.EMPTY_CONSTANTDESC;
import static java.lang.constant.ConstantUtils.validateMemberName;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * A <a href="package-summary.html#nominal">nominal descriptor</a> for an
 * {@code invokedynamic} call site.
 *
 * <p>Concrete subtypes of {@linkplain DynamicCallSiteDesc} should be immutable
 * and their behavior should not rely on object identity.
 *
 * @since 12
 */
public class DynamicCallSiteDesc {

    private final DirectMethodHandleDesc bootstrapMethod;
    private final ConstantDesc[] bootstrapArgs;
    private final String invocationName;
    private final MethodTypeDesc invocationType;

    /**
     * Creates a nominal descriptor for an {@code invokedynamic} call site.
     *
     * @param bootstrapMethod a {@link DirectMethodHandleDesc} describing the
     *                        bootstrap method for the {@code invokedynamic}
     * @param invocationName The unqualified name that would appear in the {@code NameAndType}
     *                       operand of the {@code invokedynamic}
     * @param invocationType a {@link MethodTypeDesc} describing the invocation
     *                       type that would appear in the {@code NameAndType}
     *                       operand of the {@code invokedynamic}
     * @param bootstrapArgs {@link ConstantDesc}s describing the static arguments
     *                      to the bootstrap, that would appear in the
     *                      {@code BootstrapMethods} attribute
     * @throws NullPointerException if any parameter or its contents are {@code null}
     * @throws IllegalArgumentException if the invocation name has the incorrect
     * format
     * @jvms 4.2.2 Unqualified Names
     */
    private DynamicCallSiteDesc(DirectMethodHandleDesc bootstrapMethod,
                                String invocationName,
                                MethodTypeDesc invocationType,
                                ConstantDesc[] bootstrapArgs) {
        this.invocationName = validateMemberName(requireNonNull(invocationName), true);
        this.invocationType = requireNonNull(invocationType);
        this.bootstrapMethod = requireNonNull(bootstrapMethod);
        this.bootstrapArgs = requireNonNull(bootstrapArgs.clone());
        for (int i = 0; i < this.bootstrapArgs.length; i++) {
            requireNonNull(this.bootstrapArgs[i]);
        }
        if (invocationName.length() == 0)
            throw new IllegalArgumentException("Illegal invocation name: " + invocationName);
    }

    /**
     * Creates a nominal descriptor for an {@code invokedynamic} call site.
     *
     * @param bootstrapMethod a {@link DirectMethodHandleDesc} describing the
     *                        bootstrap method for the {@code invokedynamic}
     * @param invocationName The unqualified name that would appear in the {@code NameAndType}
     *                       operand of the {@code invokedynamic}
     * @param invocationType a {@link MethodTypeDesc} describing the invocation
     *                       type that would appear in the {@code NameAndType}
     *                       operand of the {@code invokedynamic}
     * @param bootstrapArgs {@link ConstantDesc}s describing the static arguments
     *                      to the bootstrap, that would appear in the
     *                      {@code BootstrapMethods} attribute
     * @return the nominal descriptor
     * @throws NullPointerException if any parameter or its contents are {@code null}
     * @throws IllegalArgumentException if the invocation name has the incorrect
     * format
     * @jvms 4.2.2 Unqualified Names
     */
    public static DynamicCallSiteDesc of(DirectMethodHandleDesc bootstrapMethod,
                                         String invocationName,
                                         MethodTypeDesc invocationType,
                                         ConstantDesc... bootstrapArgs) {
        return new DynamicCallSiteDesc(bootstrapMethod, invocationName, invocationType, bootstrapArgs);
    }

    /**
     * Creates a nominal descriptor for an {@code invokedynamic} call site whose
     * bootstrap method has no static arguments.
     *
     * @param bootstrapMethod The bootstrap method for the {@code invokedynamic}
     * @param invocationName The invocationName that would appear in the
     * {@code NameAndType} operand of the {@code invokedynamic}
     * @param invocationType The invocation invocationType that would appear
     * in the {@code NameAndType} operand of the {@code invokedynamic}
     * @return the nominal descriptor
     * @throws NullPointerException if any parameter is null
     * @throws IllegalArgumentException if the invocation name has the incorrect
     * format
     */
    public static DynamicCallSiteDesc of(DirectMethodHandleDesc bootstrapMethod,
                                         String invocationName,
                                         MethodTypeDesc invocationType) {
        return new DynamicCallSiteDesc(bootstrapMethod, invocationName, invocationType, EMPTY_CONSTANTDESC);
    }

    /**
     * Creates a nominal descriptor for an {@code invokedynamic} call site whose
     * bootstrap method has no static arguments and for which the name parameter
     * is {@link ConstantDescs#DEFAULT_NAME}.
     *
     * @param bootstrapMethod a {@link DirectMethodHandleDesc} describing the
     *                        bootstrap method for the {@code invokedynamic}
     * @param invocationType a {@link MethodTypeDesc} describing the invocation
     *                       type that would appear in the {@code NameAndType}
     *                       operand of the {@code invokedynamic}
     * @return the nominal descriptor
     * @throws NullPointerException if any parameter is null
     */
    public static DynamicCallSiteDesc of(DirectMethodHandleDesc bootstrapMethod,
                                         MethodTypeDesc invocationType) {
        return of(bootstrapMethod, ConstantDescs.DEFAULT_NAME, invocationType);
    }

    /**
     * Returns a nominal descriptor for an {@code invokedynamic} call site whose
     * bootstrap method, name, and invocation type are the same as this one, but
     * with the specified bootstrap arguments.
     *
     * @param bootstrapArgs {@link ConstantDesc}s describing the static arguments
     *                      to the bootstrap, that would appear in the
     *                      {@code BootstrapMethods} attribute
     * @return the nominal descriptor
     * @throws NullPointerException if the argument or its contents are {@code null}
     */
    public DynamicCallSiteDesc withArgs(ConstantDesc... bootstrapArgs) {
        return new DynamicCallSiteDesc(bootstrapMethod, invocationName, invocationType, bootstrapArgs);
    }

    /**
     * Returns a nominal descriptor for an {@code invokedynamic} call site whose
     * bootstrap and bootstrap arguments are the same as this one, but with the
     * specified invocationName and invocation invocationType
     *
     * @param invocationName The unqualified name that would appear in the {@code NameAndType}
     *                       operand of the {@code invokedynamic}
     * @param invocationType a {@link MethodTypeDesc} describing the invocation
     *                       type that would appear in the {@code NameAndType}
     *                       operand of the {@code invokedynamic}
     * @return the nominal descriptor
     * @throws NullPointerException if any parameter is null
     * @throws IllegalArgumentException if the invocation name has the incorrect
     * format
     * @jvms 4.2.2 Unqualified Names
     */
    public DynamicCallSiteDesc withNameAndType(String invocationName,
                                               MethodTypeDesc invocationType) {
        return new DynamicCallSiteDesc(bootstrapMethod, invocationName, invocationType, bootstrapArgs);
    }

    /**
     * Returns the invocation name that would appear in the {@code NameAndType}
     * operand of the {@code invokedynamic}.
     *
     * @return the invocation name
     */
    public String invocationName() {
        return invocationName;
    }

    /**
     * Returns a {@link MethodTypeDesc} describing the invocation type that
     * would appear in the {@code NameAndType} operand of the {@code invokedynamic}.
     *
     * @return the invocation type
     */
    public MethodTypeDesc invocationType() {
        return invocationType;
    }

    /**
     * Returns a {@link MethodHandleDesc} describing the bootstrap method for
     * the {@code invokedynamic}.
     *
     * @return the bootstrap method for the {@code invokedynamic}
     */
    public MethodHandleDesc bootstrapMethod() { return bootstrapMethod; }

    /**
     * Returns {@link ConstantDesc}s describing the bootstrap arguments for the
     * {@code invokedynamic}. The returned array is always non-null. A zero
     * length array is returned if this {@linkplain DynamicCallSiteDesc} has no
     * bootstrap arguments.
     *
     * @return the bootstrap arguments for the {@code invokedynamic}
     */
    public ConstantDesc[] bootstrapArgs() { return bootstrapArgs.clone(); }

    /**
     * Reflectively invokes the bootstrap method with the specified arguments,
     * and return the resulting {@link CallSite}
     *
     * @param lookup The {@link MethodHandles.Lookup} used to resolve class names
     * @return the {@link CallSite}
     * @throws Throwable if any exception is thrown by the bootstrap method
     */
    public CallSite resolveCallSiteDesc(MethodHandles.Lookup lookup) throws Throwable {
        assert bootstrapMethod.invocationType().parameterType(1).equals(CD_String);
        MethodHandle bsm = (MethodHandle) bootstrapMethod.resolveConstantDesc(lookup);
        Object[] args = new Object[bootstrapArgs.length + 3];
        args[0] = lookup;
        args[1] = invocationName;
        args[2] = invocationType.resolveConstantDesc(lookup);
        System.arraycopy(bootstrapArgs, 0, args, 3, bootstrapArgs.length);
        return (CallSite) bsm.invokeWithArguments(args);
    }

    /**
     * Compares the specified object with this descriptor for equality.  Returns
     * {@code true} if and only if the specified object is also a
     * {@linkplain DynamicCallSiteDesc}, and both descriptors have equal
     * bootstrap methods, bootstrap argument lists, invocation name, and
     * invocation type.
     *
     * @param o the {@code DynamicCallSiteDesc} to compare to this
     *       {@code DynamicCallSiteDesc}
     * @return {@code true} if the specified {@code DynamicCallSiteDesc}
     *      is equal to this {@code DynamicCallSiteDesc}.
     */
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DynamicCallSiteDesc specifier = (DynamicCallSiteDesc) o;
        return Objects.equals(bootstrapMethod, specifier.bootstrapMethod) &&
               Arrays.equals(bootstrapArgs, specifier.bootstrapArgs) &&
               Objects.equals(invocationName, specifier.invocationName) &&
               Objects.equals(invocationType, specifier.invocationType);
    }

    @Override
    public final int hashCode() {
        int result = Objects.hash(bootstrapMethod, invocationName, invocationType);
        result = 31 * result + Arrays.hashCode(bootstrapArgs);
        return result;
    }

    /**
     * Returns a compact textual description of this call site description,
     * including the bootstrap method, the invocation name and type, and
     * the static bootstrap arguments.
     *
     * @return A compact textual description of this call site descriptor
     */
    @Override
    public String toString() {
        return String.format("DynamicCallSiteDesc[%s::%s(%s%s):%s]",
                             bootstrapMethod.owner().displayName(),
                             bootstrapMethod.methodName(),
                             invocationName.equals(ConstantDescs.DEFAULT_NAME) ? "" : invocationName + "/",
                             Stream.of(bootstrapArgs).map(Object::toString).collect(joining(",")),
                             invocationType.displayDescriptor());
    }
}
