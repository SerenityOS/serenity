/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.incubator.foreign;

import jdk.internal.foreign.ResourceScopeImpl;

import java.lang.invoke.MethodHandle;
import java.lang.ref.Cleaner;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Spliterator;

/**
 * A resource scope manages the lifecycle of one or more resources. Resources (e.g. {@link MemorySegment}) associated
 * with a resource scope can only be accessed while the resource scope is <em>alive</em> (see {@link #isAlive()}),
 * and by the thread associated with the resource scope (if any).
 *
 * <h2>Explicit resource scopes</h2>
 *
 * Resource scopes obtained from {@link #newConfinedScope()}, {@link #newSharedScope()} support <em>deterministic deallocation</em>;
 * We call these resource scopes <em>explicit scopes</em>. Explicit resource scopes can be closed explicitly (see {@link ResourceScope#close()}).
 * When a resource scope is closed, it is no longer <em>alive</em> (see {@link #isAlive()}, and subsequent operations on
 * resources associated with that scope (e.g. attempting to access a {@link MemorySegment} instance) will fail with {@link IllegalStateException}.
 * <p>
 * Closing a resource scope will cause all the cleanup actions associated with that scope (see {@link #addCloseAction(Runnable)}) to be called.
 * Moreover, closing a resource scope might trigger the releasing of the underlying memory resources associated with said scope; for instance:
 * <ul>
 *     <li>closing the scope associated with a native memory segment results in <em>freeing</em> the native memory associated with it
 *     (see {@link MemorySegment#allocateNative(long, ResourceScope)}, or {@link SegmentAllocator#arenaAllocator(ResourceScope)})</li>
 *     <li>closing the scope associated with a mapped memory segment results in the backing memory-mapped file to be unmapped
 *     (see {@link MemorySegment#mapFile(Path, long, long, FileChannel.MapMode, ResourceScope)})</li>
 *     <li>closing the scope associated with an upcall stub results in releasing the stub
 *     (see {@link CLinker#upcallStub(MethodHandle, FunctionDescriptor, ResourceScope)}</li>
 * </ul>
 * <p>
 * Sometimes, explicit scopes can be associated with a {@link Cleaner} instance (see {@link #newConfinedScope(Cleaner)} and
 * {@link #newSharedScope(Cleaner)}). We call these resource scopes <em>managed</em> resource scopes. A managed resource scope
 * is closed automatically once the scope instance becomes <a href="../../../java/lang/ref/package.html#reachability">unreachable</a>.
 * <p>
 * Managed scopes can be useful to allow for predictable, deterministic resource deallocation, while still prevent accidental native memory leaks.
 * In case a managed resource scope is closed explicitly, no further action will be taken when the scope becomes unreachable;
 * that is, cleanup actions (see {@link #addCloseAction(Runnable)}) associated with a resource scope, whether managed or not,
 * are called <em>exactly once</em>.
 *
 * <h2>Implicit resource scopes</h2>
 *
 * Resource scopes obtained from {@link #newImplicitScope()} cannot be closed explicitly. We call these resource scopes
 * <em>implicit scopes</em>. Calling {@link #close()} on an implicit resource scope always results in an exception.
 * Resources associated with implicit scopes are released once the scope instance becomes
 * <a href="../../../java/lang/ref/package.html#reachability">unreachable</a>.
 * <p>
 * An important implicit resource scope is the so called {@linkplain #globalScope() global scope}; the global scope is
 * an implicit scope that is guaranteed to never become <a href="../../../java/lang/ref/package.html#reachability">unreachable</a>.
 * As a results, the global scope will never attempt to release resources associated with it. Such resources must, where
 * needed, be managed independently by clients.
 *
 * <h2><a id = "thread-confinement">Thread confinement</a></h2>
 *
 * Resource scopes can be further divided into two categories: <em>thread-confined</em> resource scopes, and <em>shared</em>
 * resource scopes.
 * <p>
 * Confined resource scopes (see {@link #newConfinedScope()}), support strong thread-confinement guarantees. Upon creation,
 * they are assigned an <em>owner thread</em>, typically the thread which initiated the creation operation (see {@link #ownerThread()}).
 * After creating a confined resource scope, only the owner thread will be allowed to directly manipulate the resources
 * associated with this resource scope. Any attempt to perform resource access from a thread other than the
 * owner thread will result in a runtime failure.
 * <p>
 * Shared resource scopes (see {@link #newSharedScope()} and {@link #newImplicitScope()}), on the other hand, have no owner thread;
 * as such resources associated with this shared resource scopes can be accessed by multiple threads.
 * This might be useful when multiple threads need to access the same resource concurrently (e.g. in the case of parallel processing).
 * For instance, a client might obtain a {@link Spliterator} from a shared segment, which can then be used to slice the
 * segment and allow multiple threads to work in parallel on disjoint segment slices. The following code can be used to sum
 * all int values in a memory segment in parallel:
 *
 * <blockquote><pre>{@code
SequenceLayout SEQUENCE_LAYOUT = MemoryLayout.sequenceLayout(1024, MemoryLayouts.JAVA_INT);
try (ResourceScope scope = ResourceScope.newSharedScope()) {
    MemorySegment segment = MemorySegment.allocateNative(SEQUENCE_LAYOUT, scope);
    VarHandle VH_int = SEQUENCE_LAYOUT.elementLayout().varHandle(int.class);
    int sum = StreamSupport.stream(segment.spliterator(SEQUENCE_LAYOUT), true)
        .mapToInt(s -> (int)VH_int.get(s.address()))
        .sum();
}
 * }</pre></blockquote>
 *
 * <p>
 * Explicit shared resource scopes, while powerful, must be used with caution: if one or more threads accesses
 * a resource associated with a shared scope while the scope is being closed from another thread, an exception might occur on both
 * the accessing and the closing threads. Clients should refrain from attempting to close a shared resource scope repeatedly
 * (e.g. keep calling {@link #close()} until no exception is thrown). Instead, clients of shared resource scopes
 * should always ensure that proper synchronization mechanisms (e.g. using resource scope handles, see below) are put in place
 * so that threads closing shared resource scopes can never race against threads accessing resources managed by same scopes.
 *
 * <h2>Resource scope handles</h2>
 *
 * Resource scopes can be made <em>non-closeable</em> by acquiring one or more resource scope <em>handles</em> (see
 * {@link #acquire()}. A resource scope handle can be used to make sure that resources associated with a given resource scope
 * (either explicit or implicit) cannot be released for a certain period of time - e.g. during a critical region of code
 * involving one or more resources associated with the scope. For instance, an explicit resource scope can only be closed
 * <em>after</em> all the handles acquired against that scope have been closed (see {@link Handle#close()}).
 * This can be useful when clients need to perform a critical operation on a memory segment, during which they have
 * to ensure that the segment will not be released; this can be done as follows:
 *
 * <blockquote><pre>{@code
MemorySegment segment = ...
ResourceScope.Handle segmentHandle = segment.scope().acquire()
try {
   <critical operation on segment>
} finally {
   segment.scope().release(segmentHandle);
}
 * }</pre></blockquote>
 *
 * Acquiring implicit resource scopes is also possible, but it is often unnecessary: since resources associated with
 * an implicit scope will only be released when the scope becomes <a href="../../../java/lang/ref/package.html#reachability">unreachable</a>,
 * clients can use e.g. {@link java.lang.ref.Reference#reachabilityFence(Object)} to make sure that resources associated
 * with implicit scopes are not released prematurely. That said, the above code snippet works (trivially) for implicit scopes too.
 *
 * @implSpec
 * Implementations of this interface are immutable, thread-safe and <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>.
 */
public sealed interface ResourceScope extends AutoCloseable permits ResourceScopeImpl {
    /**
     * Is this resource scope alive?
     * @return true, if this resource scope is alive.
     * @see ResourceScope#close()
     */
    boolean isAlive();

    /**
     * The thread owning this resource scope.
     * @return the thread owning this resource scope, or {@code null} if this resource scope is shared.
     */
    Thread ownerThread();

    /**
     * Is this resource scope an <em>implicit scope</em>?
     * @return true if this scope is an <em>implicit scope</em>.
     * @see #newImplicitScope()
     * @see #globalScope()
     */
    boolean isImplicit();

    /**
     * Closes this resource scope. As a side-effect, if this operation completes without exceptions, this scope will be marked
     * as <em>not alive</em>, and subsequent operations on resources associated with this scope will fail with {@link IllegalStateException}.
     * Additionally, upon successful closure, all native resources associated with this resource scope will be released.
     *
     * @apiNote This operation is not idempotent; that is, closing an already closed resource scope <em>always</em> results in an
     * exception being thrown. This reflects a deliberate design choice: resource scope state transitions should be
     * manifest in the client code; a failure in any of these transitions reveals a bug in the underlying application
     * logic.
     *
     * @throws IllegalStateException if one of the following condition is met:
     * <ul>
     *     <li>this resource scope is not <em>alive</em>
     *     <li>this resource scope is confined, and this method is called from a thread other than the thread owning this resource scope</li>
     *     <li>this resource scope is shared and a resource associated with this scope is accessed while this method is called</li>
     *     <li>one or more handles (see {@link #acquire()}) associated with this resource scope have not been {@linkplain #release(Handle) released}</li>
     * </ul>
     * @throws UnsupportedOperationException if this resource scope is {@linkplain #isImplicit() implicit}.
     */
    void close();

    /**
     * Add a custom cleanup action which will be executed when the resource scope is closed.
     * The order in which custom cleanup actions are invoked once the scope is closed is unspecified.
     * @param runnable the custom cleanup action to be associated with this scope.
     * @throws IllegalStateException if this scope has already been closed.
     */
    void addCloseAction(Runnable runnable);

    /**
     * Acquires a resource scope handle associated with this resource scope. An explicit resource scope cannot be
     * {@linkplain #close() closed} until all the resource scope handles acquired from it have been {@linkplain #release(Handle)} released}.
     * @return a resource scope handle.
     */
    Handle acquire();

    /**
     * Release the provided resource scope handle. This method is idempotent, that is, releasing the same handle
     * multiple times has no effect.
     * @param handle the resource scope handle to be released.
     * @throws IllegalArgumentException if the provided handle is not associated with this scope.
     */
    void release(Handle handle);

    /**
     * An abstraction modelling a resource scope handle. A resource scope handle is typically {@linkplain #acquire() acquired} by clients
     * in order to prevent an explicit resource scope from being closed while executing a certain operation.
     * Once obtained, resource scope handles can be {@linkplain #release(Handle)} released}; an explicit resource scope can
     * be closed only <em>after</em> all the resource scope handles acquired from it have been released.
     */
    sealed interface Handle permits ResourceScopeImpl.HandleImpl {

        /**
         * Returns the resource scope associated with this handle.
         * @return the resource scope associated with this handle.
         */
        ResourceScope scope();
    }

    /**
     * Create a new confined scope. The resulting scope is closeable, and is not managed by a {@link Cleaner}.
     * @return a new confined scope.
     */
    static ResourceScope newConfinedScope() {
        return ResourceScopeImpl.createConfined( null);
    }

    /**
     * Create a new confined scope managed by a {@link Cleaner}.
     * @param cleaner the cleaner to be associated with the returned scope.
     * @return a new confined scope, managed by {@code cleaner}.
     * @throws NullPointerException if {@code cleaner == null}.
     */
    static ResourceScope newConfinedScope(Cleaner cleaner) {
        Objects.requireNonNull(cleaner);
        return ResourceScopeImpl.createConfined( cleaner);
    }

    /**
     * Create a new shared scope. The resulting scope is closeable, and is not managed by a {@link Cleaner}.
     * @return a new shared scope.
     */
    static ResourceScope newSharedScope() {
        return ResourceScopeImpl.createShared(null);
    }

    /**
     * Create a new shared scope managed by a {@link Cleaner}.
     * @param cleaner the cleaner to be associated with the returned scope.
     * @return a new shared scope, managed by {@code cleaner}.
     * @throws NullPointerException if {@code cleaner == null}.
     */
    static ResourceScope newSharedScope(Cleaner cleaner) {
        Objects.requireNonNull(cleaner);
        return ResourceScopeImpl.createShared(cleaner);
    }

    /**
     * Create a new <em>implicit scope</em>. The implicit scope is a managed, shared, and non-closeable scope which only features
     * <a href="ResourceScope.html#implicit-closure"><em>implicit closure</em></a>.
     * Since implicit scopes can only be closed implicitly by the garbage collector, it is recommended that implicit
     * scopes are only used in cases where deallocation performance is not a critical concern, to avoid unnecessary
     * memory pressure.
     *
     * @return a new implicit scope.
     */
    static ResourceScope newImplicitScope() {
        return ResourceScopeImpl.createImplicitScope();
    }

    /**
     * Returns an implicit scope which is assumed to be always alive.
     * @return the global scope.
     */
    static ResourceScope globalScope() {
        return ResourceScopeImpl.GLOBAL;
    }
}
