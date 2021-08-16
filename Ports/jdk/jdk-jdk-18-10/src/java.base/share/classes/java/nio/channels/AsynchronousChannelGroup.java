/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.channels;

import java.nio.channels.spi.AsynchronousChannelProvider;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * A grouping of asynchronous channels for the purpose of resource sharing.
 *
 * <p> An asynchronous channel group encapsulates the mechanics required to
 * handle the completion of I/O operations initiated by {@link AsynchronousChannel
 * asynchronous channels} that are bound to the group. A group has an associated
 * thread pool to which tasks are submitted to handle I/O events and dispatch to
 * {@link CompletionHandler completion-handlers} that consume the result of
 * asynchronous operations performed on channels in the group. In addition to
 * handling I/O events, the pooled threads may also execute other tasks required
 * to support the execution of asynchronous I/O operations.
 *
 * <p> An asynchronous channel group is created by invoking the {@link
 * #withFixedThreadPool withFixedThreadPool} or {@link #withCachedThreadPool
 * withCachedThreadPool} methods defined here. Channels are bound to a group by
 * specifying the group when constructing the channel. The associated thread
 * pool is <em>owned</em> by the group; termination of the group results in the
 * shutdown of the associated thread pool.
 *
 * <p> In addition to groups created explicitly, the Java virtual machine
 * maintains a system-wide <em>default group</em> that is constructed
 * automatically. Asynchronous channels that do not specify a group at
 * construction time are bound to the default group. The default group has an
 * associated thread pool that creates new threads as needed. The default group
 * may be configured by means of system properties defined in the table below.
 * Where the {@link java.util.concurrent.ThreadFactory ThreadFactory} for the
 * default group is not configured then the pooled threads of the default group
 * are {@link Thread#isDaemon daemon} threads.
 *
 * <table class="striped">
 * <caption style="display:none:">System properties</caption>
 *   <thead>
 *   <tr>
 *     <th scope="col">System property</th>
 *     <th scope="col">Description</th>
 *   </tr>
 *   </thead>
 *   <tbody>
 *   <tr>
 *     <th scope="row">
 *       {@systemProperty java.nio.channels.DefaultThreadPool.threadFactory}
 *     </th>
 *     <td> The value of this property is taken to be the fully-qualified name
 *     of a concrete {@link java.util.concurrent.ThreadFactory ThreadFactory}
 *     class. The class is loaded using the system class loader and instantiated.
 *     The factory's {@link java.util.concurrent.ThreadFactory#newThread
 *     newThread} method is invoked to create each thread for the default
 *     group's thread pool. If the process to load and instantiate the value
 *     of the property fails then an unspecified error is thrown during the
 *     construction of the default group. </td>
 *   </tr>
 *   <tr>
 *     <th scope="row">
 *       {@systemProperty java.nio.channels.DefaultThreadPool.initialSize}
 *     </th>
 *     <td> The value of the {@code initialSize} parameter for the default
 *     group (see {@link #withCachedThreadPool withCachedThreadPool}).
 *     The value of the property is taken to be the {@code String}
 *     representation of an {@code Integer} that is the initial size parameter.
 *     If the value cannot be parsed as an {@code Integer} it causes an
 *     unspecified error to be thrown during the construction of the default
 *     group. </td>
 *   </tr>
 *   </tbody>
 * </table>
 *
 * <a id="threading"></a><h2>Threading</h2>
 *
 * <p> The completion handler for an I/O operation initiated on a channel bound
 * to a group is guaranteed to be invoked by one of the pooled threads in the
 * group. This ensures that the completion handler is run by a thread with the
 * expected <em>identity</em>.
 *
 * <p> Where an I/O operation completes immediately, and the initiating thread
 * is one of the pooled threads in the group then the completion handler may
 * be invoked directly by the initiating thread. To avoid stack overflow, an
 * implementation may impose a limit as to the number of activations on the
 * thread stack. Some I/O operations may prohibit invoking the completion
 * handler directly by the initiating thread (see {@link
 * AsynchronousServerSocketChannel#accept(Object,CompletionHandler) accept}).
 *
 * <a id="shutdown"></a><h2>Shutdown and Termination</h2>
 *
 * <p> The {@link #shutdown() shutdown} method is used to initiate an <em>orderly
 * shutdown</em> of a group. An orderly shutdown marks the group as shutdown;
 * further attempts to construct a channel that binds to the group will throw
 * {@link ShutdownChannelGroupException}. Whether or not a group is shutdown can
 * be tested using the {@link #isShutdown() isShutdown} method. Once shutdown,
 * the group <em>terminates</em> when all asynchronous channels that are bound to
 * the group are closed, all actively executing completion handlers have run to
 * completion, and resources used by the group are released. No attempt is made
 * to stop or interrupt threads that are executing completion handlers. The
 * {@link #isTerminated() isTerminated} method is used to test if the group has
 * terminated, and the {@link #awaitTermination awaitTermination} method can be
 * used to block until the group has terminated.
 *
 * <p> The {@link #shutdownNow() shutdownNow} method can be used to initiate a
 * <em>forceful shutdown</em> of the group. In addition to the actions performed
 * by an orderly shutdown, the {@code shutdownNow} method closes all open channels
 * in the group as if by invoking the {@link AsynchronousChannel#close close}
 * method.
 *
 * @since 1.7
 *
 * @see AsynchronousSocketChannel#open(AsynchronousChannelGroup)
 * @see AsynchronousServerSocketChannel#open(AsynchronousChannelGroup)
 */

public abstract class AsynchronousChannelGroup {
    private final AsynchronousChannelProvider provider;

    /**
     * Initialize a new instance of this class.
     *
     * @param   provider
     *          The asynchronous channel provider for this group
     */
    protected AsynchronousChannelGroup(AsynchronousChannelProvider provider) {
        this.provider = provider;
    }

    /**
     * Returns the provider that created this channel group.
     *
     * @return  The provider that created this channel group
     */
    public final AsynchronousChannelProvider provider() {
        return provider;
    }

    /**
     * Creates an asynchronous channel group with a fixed thread pool.
     *
     * <p> The resulting asynchronous channel group reuses a fixed number of
     * threads. At any point, at most {@code nThreads} threads will be active
     * processing tasks that are submitted to handle I/O events and dispatch
     * completion results for operations initiated on asynchronous channels in
     * the group.
     *
     * <p> The group is created by invoking the {@link
     * AsynchronousChannelProvider#openAsynchronousChannelGroup(int,ThreadFactory)
     * openAsynchronousChannelGroup(int,ThreadFactory)} method of the system-wide
     * default {@link AsynchronousChannelProvider} object.
     *
     * @param   nThreads
     *          The number of threads in the pool
     * @param   threadFactory
     *          The factory to use when creating new threads
     *
     * @return  A new asynchronous channel group
     *
     * @throws  IllegalArgumentException
     *          If {@code nThreads <= 0}
     * @throws  IOException
     *          If an I/O error occurs
     */
    public static AsynchronousChannelGroup withFixedThreadPool(int nThreads,
                                                               ThreadFactory threadFactory)
        throws IOException
    {
        return AsynchronousChannelProvider.provider()
            .openAsynchronousChannelGroup(nThreads, threadFactory);
    }

    /**
     * Creates an asynchronous channel group with a given thread pool that
     * creates new threads as needed.
     *
     * <p> The {@code executor} parameter is an {@code ExecutorService} that
     * creates new threads as needed to execute tasks that are submitted to
     * handle I/O events and dispatch completion results for operations initiated
     * on asynchronous channels in the group. It may reuse previously constructed
     * threads when they are available.
     *
     * <p> The {@code initialSize} parameter may be used by the implementation
     * as a <em>hint</em> as to the initial number of tasks it may submit. For
     * example, it may be used to indicate the initial number of threads that
     * wait on I/O events.
     *
     * <p> The executor is intended to be used exclusively by the resulting
     * asynchronous channel group. Termination of the group results in the
     * orderly  {@link ExecutorService#shutdown shutdown} of the executor
     * service. Shutting down the executor service by other means results in
     * unspecified behavior.
     *
     * <p> The group is created by invoking the {@link
     * AsynchronousChannelProvider#openAsynchronousChannelGroup(ExecutorService,int)
     * openAsynchronousChannelGroup(ExecutorService,int)} method of the system-wide
     * default {@link AsynchronousChannelProvider} object.
     *
     * @param   executor
     *          The thread pool for the resulting group
     * @param   initialSize
     *          A value {@code >=0} or a negative value for implementation
     *          specific default
     *
     * @return  A new asynchronous channel group
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @see java.util.concurrent.Executors#newCachedThreadPool
     */
    public static AsynchronousChannelGroup withCachedThreadPool(ExecutorService executor,
                                                                int initialSize)
        throws IOException
    {
        return AsynchronousChannelProvider.provider()
            .openAsynchronousChannelGroup(executor, initialSize);
    }

    /**
     * Creates an asynchronous channel group with a given thread pool.
     *
     * <p> The {@code executor} parameter is an {@code ExecutorService} that
     * executes tasks submitted to dispatch completion results for operations
     * initiated on asynchronous channels in the group.
     *
     * <p> Care should be taken when configuring the executor service. It
     * should support <em>direct handoff</em> or <em>unbounded queuing</em> of
     * submitted tasks, and the thread that invokes the {@link
     * ExecutorService#execute execute} method should never invoke the task
     * directly. An implementation may mandate additional constraints.
     *
     * <p> The executor is intended to be used exclusively by the resulting
     * asynchronous channel group. Termination of the group results in the
     * orderly  {@link ExecutorService#shutdown shutdown} of the executor
     * service. Shutting down the executor service by other means results in
     * unspecified behavior.
     *
     * <p> The group is created by invoking the {@link
     * AsynchronousChannelProvider#openAsynchronousChannelGroup(ExecutorService,int)
     * openAsynchronousChannelGroup(ExecutorService,int)} method of the system-wide
     * default {@link AsynchronousChannelProvider} object with an {@code
     * initialSize} of {@code 0}.
     *
     * @param   executor
     *          The thread pool for the resulting group
     *
     * @return  A new asynchronous channel group
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    public static AsynchronousChannelGroup withThreadPool(ExecutorService executor)
        throws IOException
    {
        return AsynchronousChannelProvider.provider()
            .openAsynchronousChannelGroup(executor, 0);
    }

    /**
     * Tells whether or not this asynchronous channel group is shutdown.
     *
     * @return  {@code true} if this asynchronous channel group is shutdown or
     *          has been marked for shutdown.
     */
    public abstract boolean isShutdown();

    /**
     * Tells whether or not this group has terminated.
     *
     * <p> Where this method returns {@code true}, then the associated thread
     * pool has also {@link ExecutorService#isTerminated terminated}.
     *
     * @return  {@code true} if this group has terminated
     */
    public abstract boolean isTerminated();

    /**
     * Initiates an orderly shutdown of the group.
     *
     * <p> This method marks the group as shutdown. Further attempts to construct
     * channel that binds to this group will throw {@link ShutdownChannelGroupException}.
     * The group terminates when all asynchronous channels in the group are
     * closed, all actively executing completion handlers have run to completion,
     * and all resources have been released. This method has no effect if the
     * group is already shutdown.
     */
    public abstract void shutdown();

    /**
     * Shuts down the group and closes all open channels in the group.
     *
     * <p> In addition to the actions performed by the {@link #shutdown() shutdown}
     * method, this method invokes the {@link AsynchronousChannel#close close}
     * method on all open channels in the group. This method does not attempt to
     * stop or interrupt threads that are executing completion handlers. The
     * group terminates when all actively executing completion handlers have run
     * to completion and all resources have been released. This method may be
     * invoked at any time. If some other thread has already invoked it, then
     * another invocation will block until the first invocation is complete,
     * after which it will return without effect.
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    public abstract void shutdownNow() throws IOException;

    /**
     * Awaits termination of the group.
     *
     * <p> This method blocks until the group has terminated, or the timeout
     * occurs, or the current thread is interrupted, whichever happens first.
     *
     * @param   timeout
     *          The maximum time to wait, or zero or less to not wait
     * @param   unit
     *          The time unit of the timeout argument
     *
     * @return  {@code true} if the group has terminated; {@code false} if the
     *          timeout elapsed before termination
     *
     * @throws  InterruptedException
     *          If interrupted while waiting
     */
    public abstract boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException;
}
