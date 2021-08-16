/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.dynalink.support;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.LinkedList;
import jdk.dynalink.CallSiteDescriptor;
import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.support.Lookup;

/**
 * A relinkable call site that implements a polymorphic inline caching strategy.
 * It remembers up to 8 {@link GuardedInvocation}s it was linked with, and on
 * each relink request builds a cascading chain of method handles of one
 * invocation falling back to the next one. The number of remembered invocations
 * can be customized by overriding {@link #getMaxChainLength()} in a subclass.
 * When this call site is relinked with a new invocation and the length of the
 * chain is already at the maximum, it will throw away the oldest invocation.
 * Invocations with invalidated switch points and ones for which their
 * invalidating exception triggered are removed eagerly from the chain. The
 * invocations are never reordered; the most recently linked method handle is
 * always at the start of the chain and the least recently linked at its end.
 * The call site can be safely relinked on more than one thread concurrently.
 * Race conditions in linking are resolved by throwing away the
 * {@link GuardedInvocation} produced on the losing thread without incorporating
 * it into the chain, so it can lead to repeated linking for the same arguments.
 */
public class ChainedCallSite extends AbstractRelinkableCallSite {
    private static final MethodHandle PRUNE_CATCHES;
    private static final MethodHandle PRUNE_SWITCHPOINTS;
    static {
        final MethodHandle PRUNE = Lookup.findOwnSpecial(MethodHandles.lookup(), "prune", MethodHandle.class,
                MethodHandle.class, boolean.class);
        PRUNE_CATCHES      = MethodHandles.insertArguments(PRUNE, 2, true);
        PRUNE_SWITCHPOINTS = MethodHandles.insertArguments(PRUNE, 2, false);
    }

    /**
     * Contains the invocations currently linked into this call site's target. They are used when we are
     * relinking to rebuild the guardWithTest chain. Valid values for this field are: {@code null} if there's
     * no linked invocations, or an instance of {@link GuardedInvocation} if there is exactly one previous
     * invocation, or an instance of {@code GuardedInvocation[]} if there is more than one previous
     * invocation.
     */
    private Object invocations;

    /**
     * Creates a new chained call site.
     * @param descriptor the descriptor for the call site.
     */
    public ChainedCallSite(final CallSiteDescriptor descriptor) {
        super(descriptor);
    }

    /**
     * The maximum number of method handles in the chain. Defaults to 8. You can
     * override it in a subclass if you need to change the value.
     * @return the maximum number of method handles in the chain. The return
     * value is checked, and if your override returns a value less than 1, a
     * {@link RuntimeException} will be thrown.
     */
    protected int getMaxChainLength() {
        return 8;
    }

    @Override
    public void relink(final GuardedInvocation guardedInvocation, final MethodHandle relinkAndInvoke) {
        relinkInternal(guardedInvocation, relinkAndInvoke, false, false);
    }

    @Override
    public void resetAndRelink(final GuardedInvocation guardedInvocation, final MethodHandle relinkAndInvoke) {
        relinkInternal(guardedInvocation, relinkAndInvoke, true, false);
    }

    private MethodHandle relinkInternal(final GuardedInvocation invocation, final MethodHandle relink, final boolean reset, final boolean removeCatches) {
        final Object currentInvocations = invocations;
        final LinkedList<GuardedInvocation> newInvocations;
        if (currentInvocations == null || reset) {
            newInvocations = new LinkedList<>();
        } else if (currentInvocations instanceof GuardedInvocation) {
            newInvocations = new LinkedList<>();
            newInvocations.add((GuardedInvocation)currentInvocations);
        } else if (currentInvocations instanceof GuardedInvocation[]) {
            newInvocations = new LinkedList<>(Arrays.asList(((GuardedInvocation[])currentInvocations)));
        } else {
            throw new AssertionError();
        }

        // First, prune the chain of invalidated switchpoints, we always do this
        // We also remove any catches if the remove catches flag is set
        newInvocations.removeIf(inv ->
            inv.hasBeenInvalidated() || (removeCatches && inv.getException() != null)
        );

        // prune() is allowed to invoke this method with invocation == null meaning we're just pruning the chain and not
        // adding any new invocations to it.
        if(invocation != null) {
            // Remove oldest entry if we're at max length
            if(newInvocations.size() == checkMaxChainLength(getMaxChainLength())) {
                newInvocations.removeFirst();
            }
            newInvocations.addLast(invocation);
        }

        // prune-and-invoke is used as the fallback for invalidated switchpoints. If a switchpoint gets invalidated, we
        // rebuild the chain and get rid of all invalidated switchpoints instead of letting them linger.
        final MethodHandle pruneAndInvokeSwitchPoints = makePruneAndInvokeMethod(relink, PRUNE_SWITCHPOINTS);
        final MethodHandle pruneAndInvokeCatches      = makePruneAndInvokeMethod(relink, PRUNE_CATCHES);

        // Fold the new chain
        MethodHandle target = relink;
        for(final GuardedInvocation inv: newInvocations) {
            target = inv.compose(target, pruneAndInvokeSwitchPoints, pruneAndInvokeCatches);
        }

        switch (newInvocations.size()) {
            case 0:
                invocations = null;
                break;
            case 1:
                invocations = newInvocations.getFirst();
                break;
            default:
                invocations = newInvocations.toArray(new GuardedInvocation[0]);
        }
        setTarget(target);
        return target;
    }

    private static int checkMaxChainLength(final int maxChainLength) {
        if (maxChainLength > 0) {
            return maxChainLength;
        }
        throw new RuntimeException("getMaxChainLength() returned a non-positive value");

    }
    /**
     * Creates a method that rebuilds our call chain, pruning it of any invalidated switchpoints, and then invokes that
     * chain.
     * @param relinkAndInvoke the ultimate fallback for the chain passed from the dynamic linker.
     * @return a method handle for prune-and-invoke
     */
    private MethodHandle makePruneAndInvokeMethod(final MethodHandle relinkAndInvoke, final MethodHandle prune) {
        // Bind prune to (this, relink)
        final MethodHandle boundPrune = MethodHandles.insertArguments(prune, 0, this, relinkAndInvoke);
        // Make it ignore all incoming arguments
        final MethodHandle ignoreArgsPrune = MethodHandles.dropArguments(boundPrune, 0, type().parameterList());
        // Invoke prune, then invoke the call site target with original arguments
        return MethodHandles.foldArguments(MethodHandles.exactInvoker(type()), ignoreArgsPrune);
    }

    @SuppressWarnings("unused")
    private MethodHandle prune(final MethodHandle relink, final boolean catches) {
        return relinkInternal(null, relink, false, catches);
    }
}
