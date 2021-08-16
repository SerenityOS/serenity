/*
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
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/**
 * Utility classes commonly useful in concurrent programming.  This
 * package includes a few small standardized extensible frameworks, as
 * well as some classes that provide useful functionality and are
 * otherwise tedious or difficult to implement.  Here are brief
 * descriptions of the main components.  See also the
 * {@link java.util.concurrent.locks} and
 * {@link java.util.concurrent.atomic} packages.
 *
 * <h2>Executors</h2>
 *
 * <b>Interfaces.</b>
 *
 * {@link java.util.concurrent.Executor} is a simple standardized
 * interface for defining custom thread-like subsystems, including
 * thread pools, asynchronous I/O, and lightweight task frameworks.
 * Depending on which concrete Executor class is being used, tasks may
 * execute in a newly created thread, an existing task-execution thread,
 * or the thread calling {@link java.util.concurrent.Executor#execute
 * execute}, and may execute sequentially or concurrently.
 *
 * {@link java.util.concurrent.ExecutorService} provides a more
 * complete asynchronous task execution framework.  An
 * ExecutorService manages queuing and scheduling of tasks,
 * and allows controlled shutdown.
 *
 * The {@link java.util.concurrent.ScheduledExecutorService}
 * subinterface and associated interfaces add support for
 * delayed and periodic task execution.  ExecutorServices
 * provide methods arranging asynchronous execution of any
 * function expressed as {@link java.util.concurrent.Callable},
 * the result-bearing analog of {@link java.lang.Runnable}.
 *
 * A {@link java.util.concurrent.Future} returns the results of
 * a function, allows determination of whether execution has
 * completed, and provides a means to cancel execution.
 *
 * A {@link java.util.concurrent.RunnableFuture} is a {@code Future}
 * that possesses a {@code run} method that upon execution,
 * sets its results.
 *
 * <p>
 *
 * <b>Implementations.</b>
 *
 * Classes {@link java.util.concurrent.ThreadPoolExecutor} and
 * {@link java.util.concurrent.ScheduledThreadPoolExecutor}
 * provide tunable, flexible thread pools.
 *
 * The {@link java.util.concurrent.Executors} class provides
 * factory methods for the most common kinds and configurations
 * of Executors, as well as a few utility methods for using
 * them.  Other utilities based on {@code Executors} include the
 * concrete class {@link java.util.concurrent.FutureTask}
 * providing a common extensible implementation of Futures, and
 * {@link java.util.concurrent.ExecutorCompletionService}, that
 * assists in coordinating the processing of groups of
 * asynchronous tasks.
 *
 * <p>Class {@link java.util.concurrent.ForkJoinPool} provides an
 * Executor primarily designed for processing instances of {@link
 * java.util.concurrent.ForkJoinTask} and its subclasses.  These
 * classes employ a work-stealing scheduler that attains high
 * throughput for tasks conforming to restrictions that often hold in
 * computation-intensive parallel processing.
 *
 * <h2>Queues</h2>
 *
 * The {@link java.util.concurrent.ConcurrentLinkedQueue} class
 * supplies an efficient scalable thread-safe non-blocking FIFO queue.
 * The {@link java.util.concurrent.ConcurrentLinkedDeque} class is
 * similar, but additionally supports the {@link java.util.Deque}
 * interface.
 *
 * <p>Five implementations in {@code java.util.concurrent} support
 * the extended {@link java.util.concurrent.BlockingQueue}
 * interface, that defines blocking versions of put and take:
 * {@link java.util.concurrent.LinkedBlockingQueue},
 * {@link java.util.concurrent.ArrayBlockingQueue},
 * {@link java.util.concurrent.SynchronousQueue},
 * {@link java.util.concurrent.PriorityBlockingQueue}, and
 * {@link java.util.concurrent.DelayQueue}.
 * The different classes cover the most common usage contexts
 * for producer-consumer, messaging, parallel tasking, and
 * related concurrent designs.
 *
 * <p>Extended interface {@link java.util.concurrent.TransferQueue},
 * and implementation {@link java.util.concurrent.LinkedTransferQueue}
 * introduce a synchronous {@code transfer} method (along with related
 * features) in which a producer may optionally block awaiting its
 * consumer.
 *
 * <p>The {@link java.util.concurrent.BlockingDeque} interface
 * extends {@code BlockingQueue} to support both FIFO and LIFO
 * (stack-based) operations.
 * Class {@link java.util.concurrent.LinkedBlockingDeque}
 * provides an implementation.
 *
 * <h2>Timing</h2>
 *
 * The {@link java.util.concurrent.TimeUnit} class provides
 * multiple granularities (including nanoseconds) for
 * specifying and controlling time-out based operations.  Most
 * classes in the package contain operations based on time-outs
 * in addition to indefinite waits.  In all cases that
 * time-outs are used, the time-out specifies the minimum time
 * that the method should wait before indicating that it
 * timed-out.  Implementations make a &quot;best effort&quot;
 * to detect time-outs as soon as possible after they occur.
 * However, an indefinite amount of time may elapse between a
 * time-out being detected and a thread actually executing
 * again after that time-out.  All methods that accept timeout
 * parameters treat values less than or equal to zero to mean
 * not to wait at all.  To wait "forever", you can use a value
 * of {@code Long.MAX_VALUE}.
 *
 * <h2>Synchronizers</h2>
 *
 * Five classes aid common special-purpose synchronization idioms.
 * <ul>
 *
 * <li>{@link java.util.concurrent.Semaphore} is a classic concurrency tool.
 *
 * <li>{@link java.util.concurrent.CountDownLatch} is a very simple yet
 * very common utility for blocking until a given number of signals,
 * events, or conditions hold.
 *
 * <li>A {@link java.util.concurrent.CyclicBarrier} is a resettable
 * multiway synchronization point useful in some styles of parallel
 * programming.
 *
 * <li>A {@link java.util.concurrent.Phaser} provides
 * a more flexible form of barrier that may be used to control phased
 * computation among multiple threads.
 *
 * <li>An {@link java.util.concurrent.Exchanger} allows two threads to
 * exchange objects at a rendezvous point, and is useful in several
 * pipeline designs.
 *
 * </ul>
 *
 * <h2>Concurrent Collections</h2>
 *
 * Besides Queues, this package supplies Collection implementations
 * designed for use in multithreaded contexts:
 * {@link java.util.concurrent.ConcurrentHashMap},
 * {@link java.util.concurrent.ConcurrentSkipListMap},
 * {@link java.util.concurrent.ConcurrentSkipListSet},
 * {@link java.util.concurrent.CopyOnWriteArrayList}, and
 * {@link java.util.concurrent.CopyOnWriteArraySet}.
 * When many threads are expected to access a given collection, a
 * {@code ConcurrentHashMap} is normally preferable to a synchronized
 * {@code HashMap}, and a {@code ConcurrentSkipListMap} is normally
 * preferable to a synchronized {@code TreeMap}.
 * A {@code CopyOnWriteArrayList} is preferable to a synchronized
 * {@code ArrayList} when the expected number of reads and traversals
 * greatly outnumber the number of updates to a list.
 *
 * <p>The "Concurrent" prefix used with some classes in this package
 * is a shorthand indicating several differences from similar
 * "synchronized" classes.  For example {@code java.util.Hashtable} and
 * {@code Collections.synchronizedMap(new HashMap())} are
 * synchronized.  But {@link
 * java.util.concurrent.ConcurrentHashMap} is "concurrent".  A
 * concurrent collection is thread-safe, but not governed by a
 * single exclusion lock.  In the particular case of
 * ConcurrentHashMap, it safely permits any number of
 * concurrent reads as well as a large number of concurrent
 * writes.  "Synchronized" classes can be useful when you need
 * to prevent all access to a collection via a single lock, at
 * the expense of poorer scalability.  In other cases in which
 * multiple threads are expected to access a common collection,
 * "concurrent" versions are normally preferable.  And
 * unsynchronized collections are preferable when either
 * collections are unshared, or are accessible only when
 * holding other locks.
 *
 * <p id="Weakly">Most concurrent Collection implementations
 * (including most Queues) also differ from the usual {@code java.util}
 * conventions in that their {@linkplain java.util.Iterator Iterators}
 * and {@linkplain java.util.Spliterator Spliterators} provide
 * <em>weakly consistent</em> rather than fast-fail traversal:
 * <ul>
 * <li>they may proceed concurrently with other operations
 * <li>they will never throw {@link java.util.ConcurrentModificationException
 * ConcurrentModificationException}
 * <li>they are guaranteed to traverse elements as they existed upon
 * construction exactly once, and may (but are not guaranteed to)
 * reflect any modifications subsequent to construction.
 * </ul>
 *
 * <h2 id="MemoryVisibility">Memory Consistency Properties</h2>
 *
 * Chapter 17 of
 * <cite>The Java Language Specification</cite> defines the
 * <i>happens-before</i> relation on memory operations such as reads and
 * writes of shared variables.  The results of a write by one thread are
 * guaranteed to be visible to a read by another thread only if the write
 * operation <i>happens-before</i> the read operation.  The
 * {@code synchronized} and {@code volatile} constructs, as well as the
 * {@code Thread.start()} and {@code Thread.join()} methods, can form
 * <i>happens-before</i> relationships.  In particular:
 *
 * <ul>
 *   <li>Each action in a thread <i>happens-before</i> every action in that
 *   thread that comes later in the program's order.
 *
 *   <li>An unlock ({@code synchronized} block or method exit) of a
 *   monitor <i>happens-before</i> every subsequent lock ({@code synchronized}
 *   block or method entry) of that same monitor.  And because
 *   the <i>happens-before</i> relation is transitive, all actions
 *   of a thread prior to unlocking <i>happen-before</i> all actions
 *   subsequent to any thread locking that monitor.
 *
 *   <li>A write to a {@code volatile} field <i>happens-before</i> every
 *   subsequent read of that same field.  Writes and reads of
 *   {@code volatile} fields have similar memory consistency effects
 *   as entering and exiting monitors, but do <em>not</em> entail
 *   mutual exclusion locking.
 *
 *   <li>A call to {@code start} on a thread <i>happens-before</i> any
 *   action in the started thread.
 *
 *   <li>All actions in a thread <i>happen-before</i> any other thread
 *   successfully returns from a {@code join} on that thread.
 *
 * </ul>
 *
 * The methods of all classes in {@code java.util.concurrent} and its
 * subpackages extend these guarantees to higher-level
 * synchronization.  In particular:
 *
 * <ul>
 *
 *   <li>Actions in a thread prior to placing an object into any concurrent
 *   collection <i>happen-before</i> actions subsequent to the access or
 *   removal of that element from the collection in another thread.
 *
 *   <li>Actions in a thread prior to the submission of a {@code Runnable}
 *   to an {@code Executor} <i>happen-before</i> its execution begins.
 *   Similarly for {@code Callables} submitted to an {@code ExecutorService}.
 *
 *   <li>Actions taken by the asynchronous computation represented by a
 *   {@code Future} <i>happen-before</i> actions subsequent to the
 *   retrieval of the result via {@code Future.get()} in another thread.
 *
 *   <li>Actions prior to "releasing" synchronizer methods such as
 *   {@code Lock.unlock}, {@code Semaphore.release}, and
 *   {@code CountDownLatch.countDown} <i>happen-before</i> actions
 *   subsequent to a successful "acquiring" method such as
 *   {@code Lock.lock}, {@code Semaphore.acquire},
 *   {@code Condition.await}, and {@code CountDownLatch.await} on the
 *   same synchronizer object in another thread.
 *
 *   <li>For each pair of threads that successfully exchange objects via
 *   an {@code Exchanger}, actions prior to the {@code exchange()}
 *   in each thread <i>happen-before</i> those subsequent to the
 *   corresponding {@code exchange()} in another thread.
 *
 *   <li>Actions prior to calling {@code CyclicBarrier.await} and
 *   {@code Phaser.awaitAdvance} (as well as its variants)
 *   <i>happen-before</i> actions performed by the barrier action, and
 *   actions performed by the barrier action <i>happen-before</i> actions
 *   subsequent to a successful return from the corresponding {@code await}
 *   in other threads.
 *
 * </ul>
 *
 * @jls 17.4.5 Happens-before Order
 *
 * @since 1.5
 */
package java.util.concurrent;
