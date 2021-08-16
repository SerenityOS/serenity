/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package jdk.vm.ci.meta;

import java.lang.invoke.MethodHandle;

/**
 * Interface to access the internals of the {@link MethodHandle} implementation of the VM. An
 * implementation of this interface is usually required to access non-public classes, methods, and
 * fields of {@link MethodHandle}, i.e., data that is not standardized by the Java specification.
 */
public interface MethodHandleAccessProvider {

    /**
     * Identification for methods defined on the class {@link MethodHandle} that are processed by
     * the {@link MethodHandleAccessProvider}.
     */
    enum IntrinsicMethod {
        /** The method {@code MethodHandle.invokeBasic}. */
        INVOKE_BASIC,
        /** The method {@code MethodHandle.linkToStatic}. */
        LINK_TO_STATIC,
        /** The method {@code MethodHandle.linkToSpecial}. */
        LINK_TO_SPECIAL,
        /** The method {@code MethodHandle.linkToVirtual}. */
        LINK_TO_VIRTUAL,
        /** The method {@code MethodHandle.linkToInterface}. */
        LINK_TO_INTERFACE
    }

    /**
     * Returns the method handle method intrinsic identifier for the provided method, or
     * {@code null} if the method is not an intrinsic processed by this interface.
     *
     * @throws NullPointerException if {@code method} is null
     */
    IntrinsicMethod lookupMethodHandleIntrinsic(ResolvedJavaMethod method);

    /**
     * Resolves the invocation target for an invocation of {@link IntrinsicMethod#INVOKE_BASIC
     * MethodHandle.invokeBasic} with the given constant receiver {@link MethodHandle}. Returns
     * {@code null} if the invocation target is not available at this time.
     *
     * The first invocations of a method handle can use an interpreter to lookup the actual invoked
     * method; frequently executed method handles can use Java bytecode generation to avoid the
     * interpreter overhead. If the parameter forceBytecodeGeneration is set to true, the VM should
     * try to generate bytecodes before this method returns.
     *
     * @returns {@code null} if {@code methodHandle} is not a {@link MethodHandle} or the invocation
     *          target is not available at this time
     * @throws NullPointerException if {@code methodHandle} is null
     */
    ResolvedJavaMethod resolveInvokeBasicTarget(JavaConstant methodHandle, boolean forceBytecodeGeneration);

    /**
     * Resolves the invocation target for an invocation of a {@code MethodHandle.linkTo*} method
     * with the given constant member name. The member name is the last parameter of the
     * {@code linkTo*} method.
     *
     * @returns {@code null} if the invocation target is not available at this time
     * @throws NullPointerException if {@code memberName} is null
     * @throws IllegalArgumentException if {@code memberName} is not a
     *             {@code java.lang.invoke.MemberName}
     */
    ResolvedJavaMethod resolveLinkToTarget(JavaConstant memberName);
}
