/*
 *  Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

package java.lang.invoke;

import jdk.internal.vm.annotation.ForceInline;
import jdk.internal.vm.annotation.Stable;

import java.util.List;
import java.util.function.BiFunction;

/**
 * An indirect var handle can be thought of as an aggregate of the method handles implementing its supported access modes.
 * Its varform contains no method name table (given that some of the method handles composing a bound var handle might
 * not be direct). The set of method handles constituting an indirect var handle are retrieved lazily, to minimize
 * code spinning (since not all the access modes will be used anyway).
 * Indirect var handles are useful when constructing var handle adapters - that is, an adapter var handle
 * can be constructed by extracting the method handles constituting the target var handle, adapting them
 * (using the method handle combinator API) and then repackaging the adapted method handles into a new, indirect
 * var handle.
 */
/* package */ class IndirectVarHandle extends VarHandle {

    @Stable
    private final MethodHandle[] handleMap = new MethodHandle[AccessMode.COUNT];
    private final VarHandle directTarget; // cache, for performance reasons
    private final VarHandle target;
    private final BiFunction<AccessMode, MethodHandle, MethodHandle> handleFactory;
    private final Class<?> value;
    private final Class<?>[] coordinates;

    IndirectVarHandle(VarHandle target, Class<?> value, Class<?>[] coordinates, BiFunction<AccessMode, MethodHandle, MethodHandle> handleFactory) {
        this(target, value, coordinates, handleFactory, new VarForm(value, coordinates), false);
    }

    private IndirectVarHandle(VarHandle target, Class<?> value, Class<?>[] coordinates,
                      BiFunction<AccessMode, MethodHandle, MethodHandle> handleFactory, VarForm form, boolean exact) {
        super(form, exact);
        this.handleFactory = handleFactory;
        this.target = target;
        this.directTarget = target.asDirect();
        this.value = value;
        this.coordinates = coordinates;
    }

    @Override
    public Class<?> varType() {
        return value;
    }

    @Override
    public List<Class<?>> coordinateTypes() {
        return List.of(coordinates);
    }

    @Override
    MethodType accessModeTypeUncached(AccessType at) {
        return at.accessModeType(null, value, coordinates);
    }

    @Override
    boolean isDirect() {
        return false;
    }

    @Override
    VarHandle asDirect() {
        return directTarget;
    }

    VarHandle target() {
        return target;
    }

    @Override
    public VarHandle withInvokeExactBehavior() {
        return hasInvokeExactBehavior()
            ? this
            : new IndirectVarHandle(target, value, coordinates, handleFactory, vform, true);
    }

    @Override
    public VarHandle withInvokeBehavior() {
        return !hasInvokeExactBehavior()
            ? this
            : new IndirectVarHandle(target, value, coordinates, handleFactory, vform, false);
    }

    @Override
    @ForceInline
    MethodHandle getMethodHandle(int mode) {
        MethodHandle handle = handleMap[mode];
        if (handle == null) {
            MethodHandle targetHandle = target.getMethodHandle(mode); // might throw UOE of access mode is not supported, which is ok
            handle = handleMap[mode] = handleFactory.apply(AccessMode.values()[mode], targetHandle);
        }
        return handle;
    }

    @Override
    public MethodHandle toMethodHandle(AccessMode accessMode) {
        return getMethodHandle(accessMode.ordinal()).bindTo(directTarget);
    }
}
