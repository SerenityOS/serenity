/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

/**
 * A {@code VolatileCallSite} is a {@link CallSite} whose target acts like a volatile variable.
 * An {@code invokedynamic} instruction linked to a {@code VolatileCallSite} sees updates
 * to its call site target immediately, even if the update occurs in another thread.
 * There may be a performance penalty for such tight coupling between threads.
 * <p>
 * Unlike {@code MutableCallSite}, there is no
 * {@linkplain MutableCallSite#syncAll syncAll operation} on volatile
 * call sites, since every write to a volatile variable is implicitly
 * synchronized with reader threads.
 * <p>
 * In other respects, a {@code VolatileCallSite} is interchangeable
 * with {@code MutableCallSite}.
 * @see MutableCallSite
 * @author John Rose, JSR 292 EG
 * @since 1.7
 */
public class VolatileCallSite extends CallSite {
    /**
     * Creates a call site with a volatile binding to its target.
     * The initial target is set to a method handle
     * of the given type which will throw an {@code IllegalStateException} if called.
     * @param type the method type that this call site will have
     * @throws NullPointerException if the proposed type is null
     */
    public VolatileCallSite(MethodType type) {
        super(type);
    }

    /**
     * Creates a call site with a volatile binding to its target.
     * The target is set to the given value.
     * @param target the method handle that will be the initial target of the call site
     * @throws NullPointerException if the proposed target is null
     */
    public VolatileCallSite(MethodHandle target) {
        super(target);
    }

    /**
     * Returns the target method of the call site, which behaves
     * like a {@code volatile} field of the {@code VolatileCallSite}.
     * <p>
     * The interactions of {@code getTarget} with memory are the same
     * as of a read from a {@code volatile} field.
     * <p>
     * In particular, the current thread is required to issue a fresh
     * read of the target from memory, and must not fail to see
     * a recent update to the target by another thread.
     *
     * @return the linkage state of this call site, a method handle which can change over time
     * @see #setTarget
     */
    @Override public final MethodHandle getTarget() {
        return getTargetVolatile();
    }

    /**
     * Updates the target method of this call site, as a volatile variable.
     * The type of the new target must agree with the type of the old target.
     * <p>
     * The interactions with memory are the same as of a write to a volatile field.
     * In particular, any threads is guaranteed to see the updated target
     * the next time it calls {@code getTarget}.
     * @param newTarget the new target
     * @throws NullPointerException if the proposed new target is null
     * @throws WrongMethodTypeException if the proposed new target
     *         has a method type that differs from the previous target
     * @see #getTarget
     */
    @Override public void setTarget(MethodHandle newTarget) {
        setTargetVolatile(newTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final MethodHandle dynamicInvoker() {
        return makeDynamicInvoker();
    }
}
