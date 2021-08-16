/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@code MutableCallSite} is a {@link CallSite} whose target variable
 * behaves like an ordinary field.
 * An {@code invokedynamic} instruction linked to a {@code MutableCallSite} delegates
 * all calls to the site's current target.
 * The {@linkplain CallSite#dynamicInvoker dynamic invoker} of a mutable call site
 * also delegates each call to the site's current target.
 * <p>
 * Here is an example of a mutable call site which introduces a
 * state variable into a method handle chain.
 * <!-- JavaDocExamplesTest.testMutableCallSite -->
 * <blockquote><pre>{@code
MutableCallSite name = new MutableCallSite(MethodType.methodType(String.class));
MethodHandle MH_name = name.dynamicInvoker();
MethodType MT_str1 = MethodType.methodType(String.class);
MethodHandle MH_upcase = MethodHandles.lookup()
    .findVirtual(String.class, "toUpperCase", MT_str1);
MethodHandle worker1 = MethodHandles.filterReturnValue(MH_name, MH_upcase);
name.setTarget(MethodHandles.constant(String.class, "Rocky"));
assertEquals("ROCKY", (String) worker1.invokeExact());
name.setTarget(MethodHandles.constant(String.class, "Fred"));
assertEquals("FRED", (String) worker1.invokeExact());
// (mutation can be continued indefinitely)
 * }</pre></blockquote>
 * <p>
 * The same call site may be used in several places at once.
 * <blockquote><pre>{@code
MethodType MT_str2 = MethodType.methodType(String.class, String.class);
MethodHandle MH_cat = lookup().findVirtual(String.class,
  "concat", methodType(String.class, String.class));
MethodHandle MH_dear = MethodHandles.insertArguments(MH_cat, 1, ", dear?");
MethodHandle worker2 = MethodHandles.filterReturnValue(MH_name, MH_dear);
assertEquals("Fred, dear?", (String) worker2.invokeExact());
name.setTarget(MethodHandles.constant(String.class, "Wilma"));
assertEquals("WILMA", (String) worker1.invokeExact());
assertEquals("Wilma, dear?", (String) worker2.invokeExact());
 * }</pre></blockquote>
 * <p>
 * <em>Non-synchronization of target values:</em>
 * A write to a mutable call site's target does not force other threads
 * to become aware of the updated value.  Threads which do not perform
 * suitable synchronization actions relative to the updated call site
 * may cache the old target value and delay their use of the new target
 * value indefinitely.
 * (This is a normal consequence of the Java Memory Model as applied
 * to object fields.)
 * <p>
 * The {@link #syncAll syncAll} operation provides a way to force threads
 * to accept a new target value, even if there is no other synchronization.
 * <p>
 * For target values which will be frequently updated, consider using
 * a {@linkplain VolatileCallSite volatile call site} instead.
 * @author John Rose, JSR 292 EG
 * @since 1.7
 */
public class MutableCallSite extends CallSite {
    /**
     * Creates a blank call site object with the given method type.
     * The initial target is set to a method handle of the given type
     * which will throw an {@link IllegalStateException} if called.
     * <p>
     * The type of the call site is permanently set to the given type.
     * <p>
     * Before this {@code CallSite} object is returned from a bootstrap method,
     * or invoked in some other manner,
     * it is usually provided with a more useful target method,
     * via a call to {@link CallSite#setTarget(MethodHandle) setTarget}.
     * @param type the method type that this call site will have
     * @throws NullPointerException if the proposed type is null
     */
    public MutableCallSite(MethodType type) {
        super(type);
    }

    /**
     * Creates a call site object with an initial target method handle.
     * The type of the call site is permanently set to the initial target's type.
     * @param target the method handle that will be the initial target of the call site
     * @throws NullPointerException if the proposed target is null
     */
    public MutableCallSite(MethodHandle target) {
        super(target);
    }

    /**
     * Returns the target method of the call site, which behaves
     * like a normal field of the {@code MutableCallSite}.
     * <p>
     * The interactions of {@code getTarget} with memory are the same
     * as of a read from an ordinary variable, such as an array element or a
     * non-volatile, non-final field.
     * <p>
     * In particular, the current thread may choose to reuse the result
     * of a previous read of the target from memory, and may fail to see
     * a recent update to the target by another thread.
     *
     * @return the linkage state of this call site, a method handle which can change over time
     * @see #setTarget
     */
    @Override public final MethodHandle getTarget() {
        return target;
    }

    /**
     * Updates the target method of this call site, as a normal variable.
     * The type of the new target must agree with the type of the old target.
     * <p>
     * The interactions with memory are the same
     * as of a write to an ordinary variable, such as an array element or a
     * non-volatile, non-final field.
     * <p>
     * In particular, unrelated threads may fail to see the updated target
     * until they perform a read from memory.
     * Stronger guarantees can be created by putting appropriate operations
     * into the bootstrap method and/or the target methods used
     * at any given call site.
     *
     * @param newTarget the new target
     * @throws NullPointerException if the proposed new target is null
     * @throws WrongMethodTypeException if the proposed new target
     *         has a method type that differs from the previous target
     * @see #getTarget
     */
    @Override public void setTarget(MethodHandle newTarget) {
        setTargetNormal(newTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final MethodHandle dynamicInvoker() {
        return makeDynamicInvoker();
    }

    /**
     * Performs a synchronization operation on each call site in the given array,
     * forcing all other threads to throw away any cached values previously
     * loaded from the target of any of the call sites.
     * <p>
     * This operation does not reverse any calls that have already started
     * on an old target value.
     * (Java supports {@linkplain java.lang.Object#wait() forward time travel} only.)
     * <p>
     * The overall effect is to force all future readers of each call site's target
     * to accept the most recently stored value.
     * ("Most recently" is reckoned relative to the {@code syncAll} itself.)
     * Conversely, the {@code syncAll} call may block until all readers have
     * (somehow) decached all previous versions of each call site's target.
     * <p>
     * To avoid race conditions, calls to {@code setTarget} and {@code syncAll}
     * should generally be performed under some sort of mutual exclusion.
     * Note that reader threads may observe an updated target as early
     * as the {@code setTarget} call that install the value
     * (and before the {@code syncAll} that confirms the value).
     * On the other hand, reader threads may observe previous versions of
     * the target until the {@code syncAll} call returns
     * (and after the {@code setTarget} that attempts to convey the updated version).
     * <p>
     * This operation is likely to be expensive and should be used sparingly.
     * If possible, it should be buffered for batch processing on sets of call sites.
     * <p>
     * If {@code sites} contains a null element,
     * a {@code NullPointerException} will be raised.
     * In this case, some non-null elements in the array may be
     * processed before the method returns abnormally.
     * Which elements these are (if any) is implementation-dependent.
     *
     * <h4>Java Memory Model details</h4>
     * In terms of the Java Memory Model, this operation performs a synchronization
     * action which is comparable in effect to the writing of a volatile variable
     * by the current thread, and an eventual volatile read by every other thread
     * that may access one of the affected call sites.
     * <p>
     * The following effects are apparent, for each individual call site {@code S}:
     * <ul>
     * <li>A new volatile variable {@code V} is created, and written by the current thread.
     *     As defined by the JMM, this write is a global synchronization event.
     * <li>As is normal with thread-local ordering of write events,
     *     every action already performed by the current thread is
     *     taken to happen before the volatile write to {@code V}.
     *     (In some implementations, this means that the current thread
     *     performs a global release operation.)
     * <li>Specifically, the write to the current target of {@code S} is
     *     taken to happen before the volatile write to {@code V}.
     * <li>The volatile write to {@code V} is placed
     *     (in an implementation specific manner)
     *     in the global synchronization order.
     * <li>Consider an arbitrary thread {@code T} (other than the current thread).
     *     If {@code T} executes a synchronization action {@code A}
     *     after the volatile write to {@code V} (in the global synchronization order),
     *     it is therefore required to see either the current target
     *     of {@code S}, or a later write to that target,
     *     if it executes a read on the target of {@code S}.
     *     (This constraint is called "synchronization-order consistency".)
     * <li>The JMM specifically allows optimizing compilers to elide
     *     reads or writes of variables that are known to be useless.
     *     Such elided reads and writes have no effect on the happens-before
     *     relation.  Regardless of this fact, the volatile {@code V}
     *     will not be elided, even though its written value is
     *     indeterminate and its read value is not used.
     * </ul>
     * Because of the last point, the implementation behaves as if a
     * volatile read of {@code V} were performed by {@code T}
     * immediately after its action {@code A}.  In the local ordering
     * of actions in {@code T}, this read happens before any future
     * read of the target of {@code S}.  It is as if the
     * implementation arbitrarily picked a read of {@code S}'s target
     * by {@code T}, and forced a read of {@code V} to precede it,
     * thereby ensuring communication of the new target value.
     * <p>
     * As long as the constraints of the Java Memory Model are obeyed,
     * implementations may delay the completion of a {@code syncAll}
     * operation while other threads ({@code T} above) continue to
     * use previous values of {@code S}'s target.
     * However, implementations are (as always) encouraged to avoid
     * livelock, and to eventually require all threads to take account
     * of the updated target.
     *
     * <p style="font-size:smaller;">
     * <em>Discussion:</em>
     * For performance reasons, {@code syncAll} is not a virtual method
     * on a single call site, but rather applies to a set of call sites.
     * Some implementations may incur a large fixed overhead cost
     * for processing one or more synchronization operations,
     * but a small incremental cost for each additional call site.
     * In any case, this operation is likely to be costly, since
     * other threads may have to be somehow interrupted
     * in order to make them notice the updated target value.
     * However, it may be observed that a single call to synchronize
     * several sites has the same formal effect as many calls,
     * each on just one of the sites.
     *
     * <p style="font-size:smaller;">
     * <em>Implementation Note:</em>
     * Simple implementations of {@code MutableCallSite} may use
     * a volatile variable for the target of a mutable call site.
     * In such an implementation, the {@code syncAll} method can be a no-op,
     * and yet it will conform to the JMM behavior documented above.
     *
     * @param sites an array of call sites to be synchronized
     * @throws NullPointerException if the {@code sites} array reference is null
     *                              or the array contains a null
     */
    public static void syncAll(MutableCallSite[] sites) {
        if (sites.length == 0)  return;
        STORE_BARRIER.lazySet(0);
        for (MutableCallSite site : sites) {
            Objects.requireNonNull(site); // trigger NPE on first null
        }
        // FIXME: NYI
    }
    private static final AtomicInteger STORE_BARRIER = new AtomicInteger();
}
