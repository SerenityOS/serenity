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

package jdk.dynalink.beans;

import java.lang.invoke.MethodHandle;
import jdk.dynalink.DynamicLinkerFactory;
import jdk.dynalink.NamedOperation;
import jdk.dynalink.NoSuchDynamicMethodException;
import jdk.dynalink.StandardOperation;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;

/**
 * A factory for creating method handles for linking missing member behavior
 * in {@link BeansLinker}. BeansLinker links these method handles into guarded
 * invocations for link requests specifying {@code GET_*} and {@code SET_*}
 * {@link StandardOperation}s when it is either certain or possible that the
 * requested member (property, method, or element) is missing. They will be
 * linked both for {@link NamedOperation named} and unnamed operations. The
 * implementer must ensure that the parameter types of the returned method
 * handle match the parameter types of the call site described in the link
 * request. The return types can differ, though, to allow
 * {@link DynamicLinkerFactory#setPrelinkTransformer(jdk.dynalink.linker.GuardedInvocationTransformer)}
 * late return type transformations}. It is allowed to return {@code null} for a
 * method handle if the default behavior is sufficient.
 * <h2>Default missing member behavior</h2>
 * When a {@link BeansLinker} is configured without a missing member handler
 * factory, or the factory returns {@code null} for a particular handler
 * creation invocation, the default behavior is used. The default behavior is to
 * return {@code null} from
 * {@link BeansLinker#getGuardedInvocation(LinkRequest, LinkerServices)} when it
 * can be determined at link time that the linked operation will never address
 * an existing member. This lets the {@code DynamicLinker} attempt the next
 * linker if there is one, or ultimately fail the link request with
 * {@link NoSuchDynamicMethodException}. For other cases (typically all unnamed
 * member operations as well as most named operations on collection elements)
 * {@code BeansLinker} will produce a conditional linkage that will return
 * {@code null} when invoked at runtime with a name that does not match any
 * member for getters and silently ignore the passed values for setters.
 * <h2>Implementing exception-throwing behavior</h2>
 * Note that if the language-specific behavior for an operation on a missing
 * member is to throw an exception then the factory should produce a method
 * handle that throws the exception when invoked, and must not throw an
 * exception itself, as the linkage for the missing member is often conditional.
 *
 * @see BeansLinker#BeansLinker(MissingMemberHandlerFactory)
 */
@FunctionalInterface
public interface MissingMemberHandlerFactory {
    /**
     * Returns a method handle suitable for implementing missing member behavior
     * for a particular link request. See the class description for details.
     * @param linkRequest the current link request
     * @param linkerServices the current link services
     * @return a method handle that can be invoked if the property, element, or
     * method being addressed by an operation is missing. The return value can
     * be null.
     * @throws Exception if the operation fails for any reason. Please observe
     * the class documentation notes for implementing exception-throwing
     * missing member behavior.
     */
    public MethodHandle createMissingMemberHandler(LinkRequest linkRequest, LinkerServices linkerServices) throws Exception;
}
