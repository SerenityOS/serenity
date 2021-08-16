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

package java.lang.invoke;

/**
 * <p>
 * A {@code SwitchPoint} is an object which can publish state transitions to other threads.
 * A switch point is initially in the <em>valid</em> state, but may at any time be
 * changed to the <em>invalid</em> state.  Invalidation cannot be reversed.
 * A switch point can combine a <em>guarded pair</em> of method handles into a
 * <em>guarded delegator</em>.
 * The guarded delegator is a method handle which delegates to one of the old method handles.
 * The state of the switch point determines which of the two gets the delegation.
 * <p>
 * A single switch point may be used to control any number of method handles.
 * (Indirectly, therefore, it can control any number of call sites.)
 * This is done by using the single switch point as a factory for combining
 * any number of guarded method handle pairs into guarded delegators.
 * <p>
 * When a guarded delegator is created from a guarded pair, the pair
 * is wrapped in a new method handle {@code M},
 * which is permanently associated with the switch point that created it.
 * Each pair consists of a target {@code T} and a fallback {@code F}.
 * While the switch point is valid, invocations to {@code M} are delegated to {@code T}.
 * After it is invalidated, invocations are delegated to {@code F}.
 * <p>
 * Invalidation is global and immediate, as if the switch point contained a
 * volatile boolean variable consulted on every call to {@code M}.
 * The invalidation is also permanent, which means the switch point
 * can change state only once.
 * The switch point will always delegate to {@code F} after being invalidated.
 * At that point {@code guardWithTest} may ignore {@code T} and return {@code F}.
 * <p>
 * Here is an example of a switch point in action:
 * <pre>{@code
 * MethodHandle MH_strcat = MethodHandles.lookup()
 *     .findVirtual(String.class, "concat", MethodType.methodType(String.class, String.class));
 * SwitchPoint spt = new SwitchPoint();
 * assert(!spt.hasBeenInvalidated());
 * // the following steps may be repeated to re-use the same switch point:
 * MethodHandle worker1 = MH_strcat;
 * MethodHandle worker2 = MethodHandles.permuteArguments(MH_strcat, MH_strcat.type(), 1, 0);
 * MethodHandle worker = spt.guardWithTest(worker1, worker2);
 * assertEquals("method", (String) worker.invokeExact("met", "hod"));
 * SwitchPoint.invalidateAll(new SwitchPoint[]{ spt });
 * assert(spt.hasBeenInvalidated());
 * assertEquals("hodmet", (String) worker.invokeExact("met", "hod"));
 * }</pre>
 * <p style="font-size:smaller;">
 * <em>Discussion:</em>
 * Switch points are useful without subclassing.  They may also be subclassed.
 * This may be useful in order to associate application-specific invalidation logic
 * with the switch point.
 * Notice that there is no permanent association between a switch point and
 * the method handles it produces and consumes.
 * The garbage collector may collect method handles produced or consumed
 * by a switch point independently of the lifetime of the switch point itself.
 * <p style="font-size:smaller;">
 * <em>Implementation Note:</em>
 * A switch point behaves as if implemented on top of {@link MutableCallSite},
 * approximately as follows:
 * <pre>{@code
 * public class SwitchPoint {
 *     private static final MethodHandle
 *         K_true  = MethodHandles.constant(boolean.class, true),
 *         K_false = MethodHandles.constant(boolean.class, false);
 *     private final MutableCallSite mcs;
 *     private final MethodHandle mcsInvoker;
 *     public SwitchPoint() {
 *         this.mcs = new MutableCallSite(K_true);
 *         this.mcsInvoker = mcs.dynamicInvoker();
 *     }
 *     public MethodHandle guardWithTest(
 *             MethodHandle target, MethodHandle fallback) {
 *         // Note:  mcsInvoker is of type ()boolean.
 *         // Target and fallback may take any arguments, but must have the same type.
 *         return MethodHandles.guardWithTest(this.mcsInvoker, target, fallback);
 *     }
 *     public static void invalidateAll(SwitchPoint[] spts) {
 *         List<MutableCallSite> mcss = new ArrayList<>();
 *         for (SwitchPoint spt : spts)  mcss.add(spt.mcs);
 *         for (MutableCallSite mcs : mcss)  mcs.setTarget(K_false);
 *         MutableCallSite.syncAll(mcss.toArray(new MutableCallSite[0]));
 *     }
 * }
 * }</pre>
 * @author Remi Forax, JSR 292 EG
 * @since 1.7
 */
public class SwitchPoint {
    private static final MethodHandle
        K_true  = MethodHandles.constant(boolean.class, true),
        K_false = MethodHandles.constant(boolean.class, false);

    private final MutableCallSite mcs;
    private final MethodHandle mcsInvoker;

    /**
     * Creates a new switch point.
     */
    public SwitchPoint() {
        this.mcs = new MutableCallSite(K_true);
        this.mcsInvoker = mcs.dynamicInvoker();
    }

    /**
     * Determines if this switch point has been invalidated yet.
     *
     * <p style="font-size:smaller;">
     * <em>Discussion:</em>
     * Because of the one-way nature of invalidation, once a switch point begins
     * to return true for {@code hasBeenInvalidated},
     * it will always do so in the future.
     * On the other hand, a valid switch point visible to other threads may
     * be invalidated at any moment, due to a request by another thread.
     * <p style="font-size:smaller;">
     * Since invalidation is a global and immediate operation,
     * the execution of this query, on a valid switchpoint,
     * must be internally sequenced with any
     * other threads that could cause invalidation.
     * This query may therefore be expensive.
     * The recommended way to build a boolean-valued method handle
     * which queries the invalidation state of a switch point {@code s} is
     * to call {@code s.guardWithTest} on
     * {@link MethodHandles#constant constant} true and false method handles.
     *
     * @return true if this switch point has been invalidated
     */
    public boolean hasBeenInvalidated() {
        return (mcs.getTarget() != K_true);
    }

    /**
     * Returns a method handle which always delegates either to the target or the fallback.
     * The method handle will delegate to the target exactly as long as the switch point is valid.
     * After that, it will permanently delegate to the fallback.
     * <p>
     * The target and fallback must be of exactly the same method type,
     * and the resulting combined method handle will also be of this type.
     *
     * @param target the method handle selected by the switch point as long as it is valid
     * @param fallback the method handle selected by the switch point after it is invalidated
     * @return a combined method handle which always calls either the target or fallback
     * @throws NullPointerException if either argument is null
     * @throws IllegalArgumentException if the two method types do not match
     * @see MethodHandles#guardWithTest
     */
    public MethodHandle guardWithTest(MethodHandle target, MethodHandle fallback) {
        if (mcs.getTarget() == K_false)
            return fallback;  // already invalid
        return MethodHandles.guardWithTest(mcsInvoker, target, fallback);
    }

    /**
     * Sets all of the given switch points into the invalid state.
     * After this call executes, no thread will observe any of the
     * switch points to be in a valid state.
     * <p>
     * This operation is likely to be expensive and should be used sparingly.
     * If possible, it should be buffered for batch processing on sets of switch points.
     * <p>
     * If {@code switchPoints} contains a null element,
     * a {@code NullPointerException} will be raised.
     * In this case, some non-null elements in the array may be
     * processed before the method returns abnormally.
     * Which elements these are (if any) is implementation-dependent.
     *
     * <p style="font-size:smaller;">
     * <em>Discussion:</em>
     * For performance reasons, {@code invalidateAll} is not a virtual method
     * on a single switch point, but rather applies to a set of switch points.
     * Some implementations may incur a large fixed overhead cost
     * for processing one or more invalidation operations,
     * but a small incremental cost for each additional invalidation.
     * In any case, this operation is likely to be costly, since
     * other threads may have to be somehow interrupted
     * in order to make them notice the updated switch point state.
     * However, it may be observed that a single call to invalidate
     * several switch points has the same formal effect as many calls,
     * each on just one of the switch points.
     *
     * <p style="font-size:smaller;">
     * <em>Implementation Note:</em>
     * Simple implementations of {@code SwitchPoint} may use
     * a private {@link MutableCallSite} to publish the state of a switch point.
     * In such an implementation, the {@code invalidateAll} method can
     * simply change the call site's target, and issue one call to
     * {@linkplain MutableCallSite#syncAll synchronize} all the
     * private call sites.
     *
     * @param switchPoints an array of call sites to be synchronized
     * @throws NullPointerException if the {@code switchPoints} array reference is null
     *                              or the array contains a null
     */
    public static void invalidateAll(SwitchPoint[] switchPoints) {
        if (switchPoints.length == 0)  return;
        MutableCallSite[] sites = new MutableCallSite[switchPoints.length];
        for (int i = 0; i < switchPoints.length; i++) {
            SwitchPoint spt = switchPoints[i];
            if (spt == null)  break;  // MSC.syncAll will trigger a NPE
            sites[i] = spt.mcs;
            spt.mcs.setTarget(K_false);
        }
        MutableCallSite.syncAll(sites);
    }
}
