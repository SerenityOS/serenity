/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package sun.java2d;

import java.lang.ref.Reference;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This ReentrantContextProvider implementation uses one ConcurrentLinkedQueue
 * to store all ReentrantContext instances (thread and its child contexts)
 *
 * Note: this implementation keeps less contexts in memory depending on the
 * concurrent active threads in contrary to a ThreadLocal provider. However,
 * it is slower in highly concurrent workloads.
 *
 * @param <K> ReentrantContext subclass
 */
public abstract class ReentrantContextProviderCLQ<K extends ReentrantContext>
    extends ReentrantContextProvider<K>
{
    // ReentrantContext queue to store all contexts
    private final ConcurrentLinkedQueue<Reference<K>> ctxQueue
        = new ConcurrentLinkedQueue<Reference<K>>();

    /**
     * Create a new ReentrantContext provider using the given reference type
     * among hard, soft or weak based using a ConcurrentLinkedQueue storage
     *
     * @param refType reference type
     */
    public ReentrantContextProviderCLQ(final int refType) {
        super(refType);
    }

    /**
     * Give a ReentrantContext instance for the current thread
     *
     * @return ReentrantContext instance
     */
    @Override
    public final K acquire() {
        K ctx = null;
        // Drain queue if all referent are null:
        Reference<K> ref = null;
        while ((ctx == null) && ((ref = ctxQueue.poll()) != null)) {
            ctx = ref.get();
        }
        if (ctx == null) {
            // create a new ReentrantContext if none is available
            ctx = newContext();
            ctx.usage = USAGE_CLQ;
        }
        return ctx;
    }

    /**
     * Restore the given ReentrantContext instance for reuse
     *
     * @param ctx ReentrantContext instance
     */
    @Override
    public final void release(final K ctx) {
        if (ctx.usage == USAGE_CLQ) {
            ctxQueue.offer(getOrCreateReference(ctx));
        }
    }
}
