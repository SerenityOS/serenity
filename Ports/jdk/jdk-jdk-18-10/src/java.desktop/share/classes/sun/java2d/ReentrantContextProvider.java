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
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

/**
 * This abstract ReentrantContextProvider helper class manages the creation,
 * storage, and retrieval of concrete ReentrantContext instances which can be
 * subclassed to hold cached contextual data.
 *
 * It supports reentrancy as every call to acquire() provides a new unique context
 * instance that must later be returned for reuse by a call to release(ctx)
 * (typically in a try/finally block).
 *
 * It has a couple of abstract implementations which store references in a queue
 * and/or thread-local storage.
 * The Providers can be configured to hold ReentrantContext instances in memory
 * using hard, soft or weak references.
 *
 * The acquire() and release() methods are used to retrieve and return the contexts.
 *
 * The {@code newContext()} method remains abstract in all implementations and
 * must be provided by the module to create a new subclass of ReentrantContext
 * with the appropriate contextual data in it.
 *
 * Sample Usage:
 * - create a subclass ReentrantContextImpl to hold the thread state:
 *
 * static final class ReentrantContextImpl extends ReentrantContext {
 *     // specific cached data
 * }
 *
 * - create the appropriate ReentrantContextProvider:
 *
 * private static final ReentrantContextProvider<ReentrantContextImpl> contextProvider =
 *     new ReentrantContextProviderTL<ReentrantContextImpl>(ReentrantContextProvider.REF_WEAK)
 *     {
 *         @Override
 *         protected ReentrantContextImpl newContext() {
 *             return new ReentrantContextImpl();
 *         }
 *     };
 * ...
 * void someMethod() {
 *     ReentrantContextImpl ctx = contextProvider.acquire();
 *     try {
 *         // use the context
 *     } finally {
 *         contextProvider.release(ctx);
 *     }
 * }
 *
 * @param <K> ReentrantContext subclass
 *
 * @see ReentrantContext
 */
public abstract class ReentrantContextProvider<K extends ReentrantContext>
{
    // thread-local storage: inactive
    static final byte USAGE_TL_INACTIVE = 0;
    // thread-local storage: in use
    static final byte USAGE_TL_IN_USE = 1;
    // CLQ storage
    static final byte USAGE_CLQ = 2;

    // hard reference
    public static final int REF_HARD = 0;
    // soft reference
    public static final int REF_SOFT = 1;
    // weak reference
    public static final int REF_WEAK = 2;

    /* members */
    // internal reference type
    private final int refType;

    /**
     * Create a new ReentrantContext provider using the given reference type
     * among hard, soft or weak
     *
     * @param refType reference type
     */
    protected ReentrantContextProvider(final int refType) {
        this.refType = refType;
    }

    /**
     * Create a new ReentrantContext instance
     *
     * @return new ReentrantContext instance
     */
    protected abstract K newContext();

    /**
     * Give a ReentrantContext instance for the current thread
     *
     * @return ReentrantContext instance
     */
    public abstract K acquire();

    /**
     * Restore the given ReentrantContext instance for reuse
     *
     * @param ctx ReentrantContext instance
     */
    public abstract void release(K ctx);

    @SuppressWarnings("unchecked")
    protected final Reference<K> getOrCreateReference(final K ctx) {
        if (ctx.reference == null) {
            // Create the reference:
            switch (refType) {
                case REF_HARD:
                    ctx.reference = new HardReference<K>(ctx);
                    break;
                case REF_SOFT:
                    ctx.reference = new SoftReference<K>(ctx);
                    break;
                default:
                case REF_WEAK:
                    ctx.reference = new WeakReference<K>(ctx);
                    break;
            }
        }
        return (Reference<K>) ctx.reference;
    }

    /* Missing HardReference implementation */
    static final class HardReference<V> extends WeakReference<V> {
        // kept strong reference:
        private final V strongRef;

        HardReference(final V referent) {
            // no referent needed for the parent WeakReference:
            super(null);
            this.strongRef = referent;
        }

        @Override
        public V get() {
            return strongRef;
        }
    }
}
