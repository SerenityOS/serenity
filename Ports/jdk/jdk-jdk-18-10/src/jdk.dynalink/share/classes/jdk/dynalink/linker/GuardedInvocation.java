/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file, and Oracle licenses the original version of this file under the BSD
 * license:
 */
/*
   Copyright 2009-2013 Attila Szegedi

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions are
   met:
   * Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
   * Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
   * Neither the name of the copyright holder nor the names of
     contributors may be used to endorse or promote products derived from
     this software without specific prior written permission.

   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
   IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL COPYRIGHT HOLDER
   BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
   CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
   SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
   BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
   WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
   OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
   ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package jdk.dynalink.linker;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.SwitchPoint;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import jdk.dynalink.CallSiteDescriptor;
import jdk.dynalink.linker.support.Guards;

/**
 * Represents a conditionally valid method handle. Usually produced as a return
 * value of
 * {@link GuardingDynamicLinker#getGuardedInvocation(LinkRequest, LinkerServices)}
 * and
 * {@link GuardingTypeConverterFactory#convertToType(Class, Class, Supplier)}.
 * It is an immutable tuple of an invocation method handle, a guard method
 * handle that defines the applicability of the invocation handle, zero or more
 * switch points that can be used for external invalidation of the invocation
 * handle, and an exception type that if thrown during an invocation of the
 * method handle also invalidates it. The invocation handle is suitable for
 * invocation if the guard handle returns true for its arguments, and as long
 * as any of the switch points are not invalidated, and as long as it does not
 * throw an exception of the designated type. The guard, the switch points, and
 * the exception type are all optional (a guarded invocation having none of them
 * is unconditionally valid).
 */
public class GuardedInvocation {
    private final MethodHandle invocation;
    private final MethodHandle guard;
    private final Class<? extends Throwable> exception;
    private final SwitchPoint[] switchPoints;

    /**
     * Creates a new unconditional guarded invocation. It is unconditional as it
     * has no invalidations.
     *
     * @param invocation the method handle representing the invocation. Must not
     * be null.
     * @throws NullPointerException if invocation is null.
     */
    public GuardedInvocation(final MethodHandle invocation) {
        this(invocation, null, (SwitchPoint)null, null);
    }

    /**
     * Creates a new guarded invocation, with a guard method handle.
     *
     * @param invocation the method handle representing the invocation. Must not
     * be null.
     * @param guard the method handle representing the guard. Must have be
     * compatible with the {@code invocation} handle as per
     * {@link MethodHandles#guardWithTest(MethodHandle, MethodHandle, MethodHandle)}.
     * For some useful guards, check out the {@link Guards} class. It can be
     * null to represent an unconditional invocation.
     * @throws NullPointerException if invocation is null.
     */
    public GuardedInvocation(final MethodHandle invocation, final MethodHandle guard) {
        this(invocation, guard, (SwitchPoint)null, null);
    }

    /**
     * Creates a new guarded invocation that can be invalidated by a switch
     * point.
     *
     * @param invocation the method handle representing the invocation. Must
     * not be null.
     * @param switchPoint the optional switch point that can be used to
     * invalidate this linkage. It can be null. If it is null, this represents
     * an unconditional invocation.
     * @throws NullPointerException if invocation is null.
     */
    public GuardedInvocation(final MethodHandle invocation, final SwitchPoint switchPoint) {
        this(invocation, null, switchPoint, null);
    }

    /**
     * Creates a new guarded invocation, with both a guard method handle and a
     * switch point that can be used to invalidate it.
     *
     * @param invocation the method handle representing the invocation. Must
     * not be null.
     * @param guard the method handle representing the guard. Must have be
     * compatible with the {@code invocation} handle as per
     * {@link MethodHandles#guardWithTest(MethodHandle, MethodHandle, MethodHandle)}.
     * For some useful guards, check out the {@link Guards} class. It can be
     * null. If both it and the switch point are null, this represents an
     * unconditional invocation.
     * @param switchPoint the optional switch point that can be used to
     * invalidate this linkage.
     * @throws NullPointerException if invocation is null.
     */
    public GuardedInvocation(final MethodHandle invocation, final MethodHandle guard, final SwitchPoint switchPoint) {
        this(invocation, guard, switchPoint, null);
    }

    /**
     * Creates a new guarded invocation, with a guard method handle, a
     * switch point that can be used to invalidate it, and an exception that if
     * thrown when invoked also invalidates it.
     *
     * @param invocation the method handle representing the invocation. Must not
     * be null.
     * @param guard the method handle representing the guard. Must have be
     * compatible with the {@code invocation} handle as per
     * {@link MethodHandles#guardWithTest(MethodHandle, MethodHandle, MethodHandle)}.
     * For some useful guards, check out the {@link Guards} class. It can be
     * null. If it and the switch point and the exception are all null, this
     * represents an unconditional invocation.
     * @param switchPoint the optional switch point that can be used to
     * invalidate this linkage.
     * @param exception the optional exception type that is when thrown by the
     * invocation also invalidates it.
     * @throws NullPointerException if invocation is null.
     */
    public GuardedInvocation(final MethodHandle invocation, final MethodHandle guard, final SwitchPoint switchPoint, final Class<? extends Throwable> exception) {
        this.invocation = Objects.requireNonNull(invocation);
        this.guard = guard;
        this.switchPoints = switchPoint == null ? null : new SwitchPoint[] { switchPoint };
        if (exception != null && !Throwable.class.isAssignableFrom(exception)) {
            throw new IllegalArgumentException(exception.getName() + " is not assignable from Throwable");
        }
        this.exception = exception;
    }

    /**
     * Creates a new guarded invocation, with a guard method handle, any number
     * of switch points that can be used to invalidate it, and an exception that
     * if thrown when invoked also invalidates it.
     *
     * @param invocation the method handle representing the invocation. Must not
     * be null.
     * @param guard the method handle representing the guard. Must have be
     * compatible with the {@code invocation} handle as per
     * {@link MethodHandles#guardWithTest(MethodHandle, MethodHandle, MethodHandle)}.
     * For some useful guards, check out the {@link Guards} class. It can be
     * null. If it and the exception are both null, and no switch points were
     * specified, this represents an unconditional invocation.
     * @param switchPoints optional switch points that can be used to
     * invalidate this linkage.
     * @param exception the optional exception type that is when thrown by the
     * invocation also invalidates it.
     * @throws NullPointerException if invocation is null.
     */
    public GuardedInvocation(final MethodHandle invocation, final MethodHandle guard, final SwitchPoint[] switchPoints, final Class<? extends Throwable> exception) {
        this.invocation = Objects.requireNonNull(invocation);
        this.guard = guard;
        this.switchPoints = switchPoints == null ? null : switchPoints.clone();
        if (exception != null && !Throwable.class.isAssignableFrom(exception)) {
            throw new IllegalArgumentException(exception.getName() + " is not assignable from Throwable");
        }
        this.exception = exception;
    }

    /**
     * Returns the invocation method handle.
     *
     * @return the invocation method handle. It will never be null.
     */
    public MethodHandle getInvocation() {
        return invocation;
    }

    /**
     * Returns the guard method handle.
     *
     * @return the guard method handle. Can be null.
     */
    public MethodHandle getGuard() {
        return guard;
    }

    /**
     * Returns the switch points that can be used to invalidate the linkage of
     * this invocation handle.
     *
     * @return the switch points that can be used to invalidate the linkage of
     * this invocation handle. Can be null.
     */
    public SwitchPoint[] getSwitchPoints() {
        return switchPoints == null ? null : switchPoints.clone();
    }

    /**
     * Returns the exception type that if thrown by the invocation should
     * invalidate the linkage of this guarded invocation.
     *
     * @return the exception type that if thrown should be used to invalidate
     * the linkage. Can be null.
     */
    public Class<? extends Throwable> getException() {
        return exception;
    }

    /**
     * Returns true if and only if this guarded invocation has at least one
     * invalidated switch point.
     * @return true if and only if this guarded invocation has at least one
     * invalidated switch point.
     */
    public boolean hasBeenInvalidated() {
        if (switchPoints == null) {
            return false;
        }
        for (final SwitchPoint sp : switchPoints) {
            if (sp.hasBeenInvalidated()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a new guarded invocation with different methods, preserving the switch point.
     *
     * @param newInvocation the new invocation
     * @param newGuard the new guard
     * @return a new guarded invocation with the replaced methods and the same switch point as this invocation.
     */
    public GuardedInvocation replaceMethods(final MethodHandle newInvocation, final MethodHandle newGuard) {
        return new GuardedInvocation(newInvocation, newGuard, switchPoints, exception);
    }

    /**
     * Create a new guarded invocation with an added switch point.
     * @param newSwitchPoint new switch point. Can be null in which case this
     * method return the current guarded invocation with no changes.
     * @return a guarded invocation with the added switch point.
     */
    public GuardedInvocation addSwitchPoint(final SwitchPoint newSwitchPoint) {
        if (newSwitchPoint == null) {
            return this;
        }

        final SwitchPoint[] newSwitchPoints;
        if (switchPoints != null) {
            newSwitchPoints = new SwitchPoint[switchPoints.length + 1];
            System.arraycopy(switchPoints, 0, newSwitchPoints, 0, switchPoints.length);
            newSwitchPoints[switchPoints.length] = newSwitchPoint;
        } else {
            newSwitchPoints = new SwitchPoint[] { newSwitchPoint };
        }

        return new GuardedInvocation(invocation, guard, newSwitchPoints, exception);
    }

    private GuardedInvocation replaceMethodsOrThis(final MethodHandle newInvocation, final MethodHandle newGuard) {
        if (newInvocation == invocation && newGuard == guard) {
            return this;
        }
        return replaceMethods(newInvocation, newGuard);
    }

    /**
     * Changes the type of the invocation, as if
     * {@link MethodHandle#asType(MethodType)} was applied to its invocation
     * and its guard, if it has one (with return type changed to boolean, and
     * parameter count potentially truncated for the guard). If the invocation
     * already is of the required type, returns this object.
     * @param newType the new type of the invocation.
     * @return a guarded invocation with the new type applied to it.
     */
    public GuardedInvocation asType(final MethodType newType) {
        return replaceMethodsOrThis(invocation.asType(newType), guard == null ? null : Guards.asType(guard, newType));
    }

    /**
     * Changes the type of the invocation, as if
     * {@link LinkerServices#asType(MethodHandle, MethodType)} was applied to
     * its invocation and its guard, if it has one (with return type changed to
     * boolean, and parameter count potentially truncated for the guard). If the
     * invocation already is of the required type, returns this object.
     * @param linkerServices the linker services to use for the conversion
     * @param newType the new type of the invocation.
     * @return a guarded invocation with the new type applied to it.
     */
    public GuardedInvocation asType(final LinkerServices linkerServices, final MethodType newType) {
        return replaceMethodsOrThis(linkerServices.asType(invocation, newType), guard == null ? null :
            Guards.asType(linkerServices, guard, newType));
    }

    /**
     * Changes the type of the invocation, as if
     * {@link LinkerServices#asTypeLosslessReturn(MethodHandle, MethodType)} was
     * applied to its invocation and
     * {@link LinkerServices#asType(MethodHandle, MethodType)} applied to its
     * guard, if it has one (with return type changed to boolean, and parameter
     * count potentially truncated for the guard). If the invocation doesn't
     * change its type, returns this object.
     * @param linkerServices the linker services to use for the conversion
     * @param newType the new type of the invocation.
     * @return a guarded invocation with the new type applied to it.
     */
    public GuardedInvocation asTypeSafeReturn(final LinkerServices linkerServices, final MethodType newType) {
        return replaceMethodsOrThis(linkerServices.asTypeLosslessReturn(invocation, newType), guard == null ? null :
            Guards.asType(linkerServices, guard, newType));
    }

    /**
     * Changes the type of the invocation, as if
     * {@link MethodHandle#asType(MethodType)} was applied to its invocation
     * and its guard, if it has one (with return type changed to boolean for
     * guard). If the invocation already is of the required type, returns this
     * object.
     * @param desc a call descriptor whose method type is adapted.
     * @return a guarded invocation with the new type applied to it.
     */
    public GuardedInvocation asType(final CallSiteDescriptor desc) {
        return asType(desc.getMethodType());
    }

    /**
     * Applies argument filters to both the invocation and the guard
     * (if it exists and has at least {@code pos + 1} parameters) with
     * {@link MethodHandles#filterArguments(MethodHandle, int, MethodHandle...)}.
     * @param pos the position of the first argument being filtered
     * @param filters the argument filters
     * @return a filtered invocation
     */
    public GuardedInvocation filterArguments(final int pos, final MethodHandle... filters) {
        return replaceMethods(MethodHandles.filterArguments(invocation, pos, filters),
                guard == null || pos >= guard.type().parameterCount() ?
                        guard : MethodHandles.filterArguments(guard, pos, filters));
    }

    /**
     * Makes an invocation that drops arguments in both the invocation and the
     * guard (if it exists and has at least {@code pos} parameters) with
     * {@link MethodHandles#dropArguments(MethodHandle, int, List)}.
     * @param pos the position of the first argument being dropped
     * @param valueTypes the types of the values being dropped
     * @return an invocation that drops arguments
     */
    public GuardedInvocation dropArguments(final int pos, final List<Class<?>> valueTypes) {
        return replaceMethods(MethodHandles.dropArguments(invocation, pos, valueTypes),
                guard == null || pos > guard.type().parameterCount() ?
                    guard : MethodHandles.dropArguments(guard, pos, valueTypes));
    }

    /**
     * Makes an invocation that drops arguments in both the invocation and the
     * guard (if it exists and has at least {@code pos} parameters) with
     * {@link MethodHandles#dropArguments(MethodHandle, int, Class...)}.
     * @param pos the position of the first argument being dropped
     * @param valueTypes the types of the values being dropped
     * @return an invocation that drops arguments
     */
    public GuardedInvocation dropArguments(final int pos, final Class<?>... valueTypes) {
        return replaceMethods(MethodHandles.dropArguments(invocation, pos, valueTypes),
                guard == null || pos > guard.type().parameterCount() ?
                        guard : MethodHandles.dropArguments(guard, pos, valueTypes));
    }


    /**
     * Composes the invocation, guard, switch points, and the exception into a
     * composite method handle that knows how to fall back when the guard fails
     * or the invocation is invalidated.
     * @param fallback the fallback method handle for when a switch point is
     * invalidated, a guard returns false, or invalidating exception is thrown.
     * @return a composite method handle.
     */
    public MethodHandle compose(final MethodHandle fallback) {
        return compose(fallback, fallback, fallback);
    }

    /**
     * Composes the invocation, guard, switch points, and the exception into a
     * composite method handle that knows how to fall back when the guard fails
     * or the invocation is invalidated.
     * @param switchpointFallback the fallback method handle in case a switch
     * point is invalidated.
     * @param guardFallback the fallback method handle in case guard returns
     * false.
     * @param catchFallback the fallback method in case the exception handler
     * triggers.
     * @return a composite method handle.
     */
    public MethodHandle compose(final MethodHandle guardFallback, final MethodHandle switchpointFallback, final MethodHandle catchFallback) {
        final MethodHandle guarded =
                guard == null ?
                        invocation :
                        MethodHandles.guardWithTest(
                                guard,
                                invocation,
                                guardFallback);

        final MethodHandle catchGuarded =
                exception == null ?
                        guarded :
                        MethodHandles.catchException(
                                guarded,
                                exception,
                                MethodHandles.dropArguments(
                                    catchFallback,
                                    0,
                                    exception));

        if (switchPoints == null) {
            return catchGuarded;
        }

        MethodHandle spGuarded = catchGuarded;
        for (final SwitchPoint sp : switchPoints) {
            spGuarded = sp.guardWithTest(spGuarded, switchpointFallback);
        }

        return spGuarded;
    }
}
