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

package java.util.concurrent;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.security.AccessController;
import java.security.AccessControlContext;
import java.security.Permission;
import java.security.Permissions;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

/**
 * An {@link ExecutorService} for running {@link ForkJoinTask}s.
 * A {@code ForkJoinPool} provides the entry point for submissions
 * from non-{@code ForkJoinTask} clients, as well as management and
 * monitoring operations.
 *
 * <p>A {@code ForkJoinPool} differs from other kinds of {@link
 * ExecutorService} mainly by virtue of employing
 * <em>work-stealing</em>: all threads in the pool attempt to find and
 * execute tasks submitted to the pool and/or created by other active
 * tasks (eventually blocking waiting for work if none exist). This
 * enables efficient processing when most tasks spawn other subtasks
 * (as do most {@code ForkJoinTask}s), as well as when many small
 * tasks are submitted to the pool from external clients.  Especially
 * when setting <em>asyncMode</em> to true in constructors, {@code
 * ForkJoinPool}s may also be appropriate for use with event-style
 * tasks that are never joined. All worker threads are initialized
 * with {@link Thread#isDaemon} set {@code true}.
 *
 * <p>A static {@link #commonPool()} is available and appropriate for
 * most applications. The common pool is used by any ForkJoinTask that
 * is not explicitly submitted to a specified pool. Using the common
 * pool normally reduces resource usage (its threads are slowly
 * reclaimed during periods of non-use, and reinstated upon subsequent
 * use).
 *
 * <p>For applications that require separate or custom pools, a {@code
 * ForkJoinPool} may be constructed with a given target parallelism
 * level; by default, equal to the number of available processors.
 * The pool attempts to maintain enough active (or available) threads
 * by dynamically adding, suspending, or resuming internal worker
 * threads, even if some tasks are stalled waiting to join others.
 * However, no such adjustments are guaranteed in the face of blocked
 * I/O or other unmanaged synchronization. The nested {@link
 * ManagedBlocker} interface enables extension of the kinds of
 * synchronization accommodated. The default policies may be
 * overridden using a constructor with parameters corresponding to
 * those documented in class {@link ThreadPoolExecutor}.
 *
 * <p>In addition to execution and lifecycle control methods, this
 * class provides status check methods (for example
 * {@link #getStealCount}) that are intended to aid in developing,
 * tuning, and monitoring fork/join applications. Also, method
 * {@link #toString} returns indications of pool state in a
 * convenient form for informal monitoring.
 *
 * <p>As is the case with other ExecutorServices, there are three
 * main task execution methods summarized in the following table.
 * These are designed to be used primarily by clients not already
 * engaged in fork/join computations in the current pool.  The main
 * forms of these methods accept instances of {@code ForkJoinTask},
 * but overloaded forms also allow mixed execution of plain {@code
 * Runnable}- or {@code Callable}- based activities as well.  However,
 * tasks that are already executing in a pool should normally instead
 * use the within-computation forms listed in the table unless using
 * async event-style tasks that are not usually joined, in which case
 * there is little difference among choice of methods.
 *
 * <table class="plain">
 * <caption>Summary of task execution methods</caption>
 *  <tr>
 *    <td></td>
 *    <th scope="col"> Call from non-fork/join clients</th>
 *    <th scope="col"> Call from within fork/join computations</th>
 *  </tr>
 *  <tr>
 *    <th scope="row" style="text-align:left"> Arrange async execution</th>
 *    <td> {@link #execute(ForkJoinTask)}</td>
 *    <td> {@link ForkJoinTask#fork}</td>
 *  </tr>
 *  <tr>
 *    <th scope="row" style="text-align:left"> Await and obtain result</th>
 *    <td> {@link #invoke(ForkJoinTask)}</td>
 *    <td> {@link ForkJoinTask#invoke}</td>
 *  </tr>
 *  <tr>
 *    <th scope="row" style="text-align:left"> Arrange exec and obtain Future</th>
 *    <td> {@link #submit(ForkJoinTask)}</td>
 *    <td> {@link ForkJoinTask#fork} (ForkJoinTasks <em>are</em> Futures)</td>
 *  </tr>
 * </table>
 *
 * <p>The parameters used to construct the common pool may be controlled by
 * setting the following {@linkplain System#getProperty system properties}:
 * <ul>
 * <li>{@systemProperty java.util.concurrent.ForkJoinPool.common.parallelism}
 * - the parallelism level, a non-negative integer
 * <li>{@systemProperty java.util.concurrent.ForkJoinPool.common.threadFactory}
 * - the class name of a {@link ForkJoinWorkerThreadFactory}.
 * The {@linkplain ClassLoader#getSystemClassLoader() system class loader}
 * is used to load this class.
 * <li>{@systemProperty java.util.concurrent.ForkJoinPool.common.exceptionHandler}
 * - the class name of a {@link UncaughtExceptionHandler}.
 * The {@linkplain ClassLoader#getSystemClassLoader() system class loader}
 * is used to load this class.
 * <li>{@systemProperty java.util.concurrent.ForkJoinPool.common.maximumSpares}
 * - the maximum number of allowed extra threads to maintain target
 * parallelism (default 256).
 * </ul>
 * If no thread factory is supplied via a system property, then the
 * common pool uses a factory that uses the system class loader as the
 * {@linkplain Thread#getContextClassLoader() thread context class loader}.
 * In addition, if a {@link SecurityManager} is present, then
 * the common pool uses a factory supplying threads that have no
 * {@link Permissions} enabled.
 *
 * Upon any error in establishing these settings, default parameters
 * are used. It is possible to disable or limit the use of threads in
 * the common pool by setting the parallelism property to zero, and/or
 * using a factory that may return {@code null}. However doing so may
 * cause unjoined tasks to never be executed.
 *
 * <p><b>Implementation notes:</b> This implementation restricts the
 * maximum number of running threads to 32767. Attempts to create
 * pools with greater than the maximum number result in
 * {@code IllegalArgumentException}.
 *
 * <p>This implementation rejects submitted tasks (that is, by throwing
 * {@link RejectedExecutionException}) only when the pool is shut down
 * or internal resources have been exhausted.
 *
 * @since 1.7
 * @author Doug Lea
 */
public class ForkJoinPool extends AbstractExecutorService {

    /*
     * Implementation Overview
     *
     * This class and its nested classes provide the main
     * functionality and control for a set of worker threads:
     * Submissions from non-FJ threads enter into submission queues.
     * Workers take these tasks and typically split them into subtasks
     * that may be stolen by other workers. Work-stealing based on
     * randomized scans generally leads to better throughput than
     * "work dealing" in which producers assign tasks to idle threads,
     * in part because threads that have finished other tasks before
     * the signalled thread wakes up (which can be a long time) can
     * take the task instead.  Preference rules give first priority to
     * processing tasks from their own queues (LIFO or FIFO, depending
     * on mode), then to randomized FIFO steals of tasks in other
     * queues.  This framework began as vehicle for supporting
     * tree-structured parallelism using work-stealing.  Over time,
     * its scalability advantages led to extensions and changes to
     * better support more diverse usage contexts.  Because most
     * internal methods and nested classes are interrelated, their
     * main rationale and descriptions are presented here; individual
     * methods and nested classes contain only brief comments about
     * details.
     *
     * WorkQueues
     * ==========
     *
     * Most operations occur within work-stealing queues (in nested
     * class WorkQueue).  These are special forms of Deques that
     * support only three of the four possible end-operations -- push,
     * pop, and poll (aka steal), under the further constraints that
     * push and pop are called only from the owning thread (or, as
     * extended here, under a lock), while poll may be called from
     * other threads.  (If you are unfamiliar with them, you probably
     * want to read Herlihy and Shavit's book "The Art of
     * Multiprocessor programming", chapter 16 describing these in
     * more detail before proceeding.)  The main work-stealing queue
     * design is roughly similar to those in the papers "Dynamic
     * Circular Work-Stealing Deque" by Chase and Lev, SPAA 2005
     * (http://research.sun.com/scalable/pubs/index.html) and
     * "Idempotent work stealing" by Michael, Saraswat, and Vechev,
     * PPoPP 2009 (http://portal.acm.org/citation.cfm?id=1504186).
     * The main differences ultimately stem from GC requirements that
     * we null out taken slots as soon as we can, to maintain as small
     * a footprint as possible even in programs generating huge
     * numbers of tasks. To accomplish this, we shift the CAS
     * arbitrating pop vs poll (steal) from being on the indices
     * ("base" and "top") to the slots themselves.
     *
     * Adding tasks then takes the form of a classic array push(task)
     * in a circular buffer:
     *    q.array[q.top++ % length] = task;
     *
     * The actual code needs to null-check and size-check the array,
     * uses masking, not mod, for indexing a power-of-two-sized array,
     * enforces memory ordering, supports resizing, and possibly
     * signals waiting workers to start scanning -- see below.
     *
     * The pop operation (always performed by owner) is of the form:
     *   if ((task = getAndSet(q.array, (q.top-1) % length, null)) != null)
     *        decrement top and return task;
     * If this fails, the queue is empty.
     *
     * The poll operation by another stealer thread is, basically:
     *   if (CAS nonnull task at q.array[q.base % length] to null)
     *       increment base and return task;
     *
     * This may fail due to contention, and may be retried.
     * Implementations must ensure a consistent snapshot of the base
     * index and the task (by looping or trying elsewhere) before
     * trying CAS.  There isn't actually a method of this form,
     * because failure due to inconsistency or contention is handled
     * in different ways in different contexts, normally by first
     * trying other queues. (For the most straightforward example, see
     * method pollScan.) There are further variants for cases
     * requiring inspection of elements before extracting them, so
     * must interleave these with variants of this code.  Also, a more
     * efficient version (nextLocalTask) is used for polls by owners.
     * It avoids some overhead because the queue cannot be growing
     * during call.
     *
     * Memory ordering.  See "Correct and Efficient Work-Stealing for
     * Weak Memory Models" by Le, Pop, Cohen, and Nardelli, PPoPP 2013
     * (http://www.di.ens.fr/~zappa/readings/ppopp13.pdf) for an
     * analysis of memory ordering requirements in work-stealing
     * algorithms similar to the one used here.  Inserting and
     * extracting tasks in array slots via volatile or atomic accesses
     * or explicit fences provides primary synchronization.
     *
     * Operations on deque elements require reads and writes of both
     * indices and slots. When possible, we allow these to occur in
     * any order.  Because the base and top indices (along with other
     * pool or array fields accessed in many methods) only imprecisely
     * guide where to extract from, we let accesses other than the
     * element getAndSet/CAS/setVolatile appear in any order, using
     * plain mode. But we must still preface some methods (mainly
     * those that may be accessed externally) with an acquireFence to
     * avoid unbounded staleness. This is equivalent to acting as if
     * callers use an acquiring read of the reference to the pool or
     * queue when invoking the method, even when they do not. We use
     * explicit acquiring reads (getSlot) rather than plain array
     * access when acquire mode is required but not otherwise ensured
     * by context. To reduce stalls by other stealers, we encourage
     * timely writes to the base index by immediately following
     * updates with a write of a volatile field that must be updated
     * anyway, or an Opaque-mode write if there is no such
     * opportunity.
     *
     * Because indices and slot contents cannot always be consistent,
     * the emptiness check base == top is only quiescently accurate
     * (and so used where this suffices). Otherwise, it may err on the
     * side of possibly making the queue appear nonempty when a push,
     * pop, or poll have not fully committed, or making it appear
     * empty when an update of top or base has not yet been seen.
     * Similarly, the check in push for the queue array being full may
     * trigger when not completely full, causing a resize earlier than
     * required.
     *
     * Mainly because of these potential inconsistencies among slots
     * vs indices, the poll operation, considered individually, is not
     * wait-free. One thief cannot successfully continue until another
     * in-progress one (or, if previously empty, a push) visibly
     * completes.  This can stall threads when required to consume
     * from a given queue (which may spin).  However, in the
     * aggregate, we ensure probabilistic non-blockingness at least
     * until checking quiescence (which is intrinsically blocking):
     * If an attempted steal fails, a scanning thief chooses a
     * different victim target to try next. So, in order for one thief
     * to progress, it suffices for any in-progress poll or new push
     * on any empty queue to complete. The worst cases occur when many
     * threads are looking for tasks being produced by a stalled
     * producer.
     *
     * This approach also enables support of a user mode in which
     * local task processing is in FIFO, not LIFO order, simply by
     * using poll rather than pop.  This can be useful in
     * message-passing frameworks in which tasks are never joined,
     * although with increased contention among task producers and
     * consumers.
     *
     * WorkQueues are also used in a similar way for tasks submitted
     * to the pool. We cannot mix these tasks in the same queues used
     * by workers. Instead, we randomly associate submission queues
     * with submitting threads, using a form of hashing.  The
     * ThreadLocalRandom probe value serves as a hash code for
     * choosing existing queues, and may be randomly repositioned upon
     * contention with other submitters.  In essence, submitters act
     * like workers except that they are restricted to executing local
     * tasks that they submitted (or when known, subtasks thereof).
     * Insertion of tasks in shared mode requires a lock. We use only
     * a simple spinlock (using field "source"), because submitters
     * encountering a busy queue move to a different position to use
     * or create other queues. They block only when registering new
     * queues.
     *
     * Management
     * ==========
     *
     * The main throughput advantages of work-stealing stem from
     * decentralized control -- workers mostly take tasks from
     * themselves or each other, at rates that can exceed a billion
     * per second.  Most non-atomic control is performed by some form
     * of scanning across or within queues.  The pool itself creates,
     * activates (enables scanning for and running tasks),
     * deactivates, blocks, and terminates threads, all with minimal
     * central information.  There are only a few properties that we
     * can globally track or maintain, so we pack them into a small
     * number of variables, often maintaining atomicity without
     * blocking or locking.  Nearly all essentially atomic control
     * state is held in a few volatile variables that are by far most
     * often read (not written) as status and consistency checks. We
     * pack as much information into them as we can.
     *
     * Field "ctl" contains 64 bits holding information needed to
     * atomically decide to add, enqueue (on an event queue), and
     * dequeue and release workers.  To enable this packing, we
     * restrict maximum parallelism to (1<<15)-1 (which is far in
     * excess of normal operating range) to allow ids, counts, and
     * their negations (used for thresholding) to fit into 16bit
     * subfields.
     *
     * Field "mode" holds configuration parameters as well as lifetime
     * status, atomically and monotonically setting SHUTDOWN, STOP,
     * and finally TERMINATED bits. It is updated only via bitwise
     * atomics (getAndBitwiseOr).
     *
     * Array "queues" holds references to WorkQueues.  It is updated
     * (only during worker creation and termination) under the
     * registrationLock, but is otherwise concurrently readable, and
     * accessed directly (although always prefaced by acquireFences or
     * other acquiring reads). To simplify index-based operations, the
     * array size is always a power of two, and all readers must
     * tolerate null slots.  Worker queues are at odd indices. Worker
     * ids masked with SMASK match their index. Shared (submission)
     * queues are at even indices. Grouping them together in this way
     * simplifies and speeds up task scanning.
     *
     * All worker thread creation is on-demand, triggered by task
     * submissions, replacement of terminated workers, and/or
     * compensation for blocked workers. However, all other support
     * code is set up to work with other policies.  To ensure that we
     * do not hold on to worker or task references that would prevent
     * GC, all accesses to workQueues are via indices into the
     * queues array (which is one source of some of the messy code
     * constructions here). In essence, the queues array serves as
     * a weak reference mechanism. Thus for example the stack top
     * subfield of ctl stores indices, not references.
     *
     * Queuing Idle Workers. Unlike HPC work-stealing frameworks, we
     * cannot let workers spin indefinitely scanning for tasks when
     * none can be found immediately, and we cannot start/resume
     * workers unless there appear to be tasks available.  On the
     * other hand, we must quickly prod them into action when new
     * tasks are submitted or generated. These latencies are mainly a
     * function of JVM park/unpark (and underlying OS) performance,
     * which can be slow and variable.  In many usages, ramp-up time
     * is the main limiting factor in overall performance, which is
     * compounded at program start-up by JIT compilation and
     * allocation. On the other hand, throughput degrades when too
     * many threads poll for too few tasks.
     *
     * The "ctl" field atomically maintains total and "released"
     * worker counts, plus the head of the available worker queue
     * (actually stack, represented by the lower 32bit subfield of
     * ctl).  Released workers are those known to be scanning for
     * and/or running tasks. Unreleased ("available") workers are
     * recorded in the ctl stack. These workers are made available for
     * signalling by enqueuing in ctl (see method awaitWork).  The
     * "queue" is a form of Treiber stack. This is ideal for
     * activating threads in most-recently used order, and improves
     * performance and locality, outweighing the disadvantages of
     * being prone to contention and inability to release a worker
     * unless it is topmost on stack. The top stack state holds the
     * value of the "phase" field of the worker: its index and status,
     * plus a version counter that, in addition to the count subfields
     * (also serving as version stamps) provide protection against
     * Treiber stack ABA effects.
     *
     * Creating workers. To create a worker, we pre-increment counts
     * (serving as a reservation), and attempt to construct a
     * ForkJoinWorkerThread via its factory. On starting, the new
     * thread first invokes registerWorker, where it constructs a
     * WorkQueue and is assigned an index in the queues array
     * (expanding the array if necessary).  Upon any exception across
     * these steps, or null return from factory, deregisterWorker
     * adjusts counts and records accordingly.  If a null return, the
     * pool continues running with fewer than the target number
     * workers. If exceptional, the exception is propagated, generally
     * to some external caller.
     *
     * WorkQueue field "phase" is used by both workers and the pool to
     * manage and track whether a worker is UNSIGNALLED (possibly
     * blocked waiting for a signal).  When a worker is enqueued its
     * phase field is set negative. Note that phase field updates lag
     * queue CAS releases; seeing a negative phase does not guarantee
     * that the worker is available. When queued, the lower 16 bits of
     * its phase must hold its pool index. So we place the index there
     * upon initialization and never modify these bits.
     *
     * The ctl field also serves as the basis for memory
     * synchronization surrounding activation. This uses a more
     * efficient version of a Dekker-like rule that task producers and
     * consumers sync with each other by both writing/CASing ctl (even
     * if to its current value).  However, rather than CASing ctl to
     * its current value in the common case where no action is
     * required, we reduce write contention by ensuring that
     * signalWork invocations are prefaced with a full-volatile memory
     * access (which is usually needed anyway).
     *
     * Signalling. Signals (in signalWork) cause new or reactivated
     * workers to scan for tasks.  Method signalWork and its callers
     * try to approximate the unattainable goal of having the right
     * number of workers activated for the tasks at hand, but must err
     * on the side of too many workers vs too few to avoid stalls.  If
     * computations are purely tree structured, it suffices for every
     * worker to activate another when it pushes a task into an empty
     * queue, resulting in O(log(#threads)) steps to full activation.
     * If instead, tasks come in serially from only a single producer,
     * each worker taking its first (since the last quiescence) task
     * from a queue should signal another if there are more tasks in
     * that queue. This is equivalent to, but generally faster than,
     * arranging the stealer take two tasks, re-pushing one on its own
     * queue, and signalling (because its queue is empty), also
     * resulting in logarithmic full activation time. Because we don't
     * know about usage patterns (or most commonly, mixtures), we use
     * both approaches.  We approximate the second rule by arranging
     * that workers in scan() do not repeat signals when repeatedly
     * taking tasks from any given queue, by remembering the previous
     * one. There are narrow windows in which both rules may apply,
     * leading to duplicate or unnecessary signals. Despite such
     * limitations, these rules usually avoid slowdowns that otherwise
     * occur when too many workers contend to take too few tasks, or
     * when producers waste most of their time resignalling.  However,
     * contention and overhead effects may still occur during ramp-up,
     * ramp-down, and small computations involving only a few workers.
     *
     * Scanning. Method scan performs top-level scanning for (and
     * execution of) tasks.  Scans by different workers and/or at
     * different times are unlikely to poll queues in the same
     * order. Each scan traverses and tries to poll from each queue in
     * a pseudorandom permutation order by starting at a random index,
     * and using a constant cyclically exhaustive stride; restarting
     * upon contention.  (Non-top-level scans; for example in
     * helpJoin, use simpler linear probes because they do not
     * systematically contend with top-level scans.)  The pseudorandom
     * generator need not have high-quality statistical properties in
     * the long term. We use Marsaglia XorShifts, seeded with the Weyl
     * sequence from ThreadLocalRandom probes, which are cheap and
     * suffice. Scans do not otherwise explicitly take into account
     * core affinities, loads, cache localities, etc, However, they do
     * exploit temporal locality (which usually approximates these) by
     * preferring to re-poll from the same queue after a successful
     * poll before trying others (see method topLevelExec).  This
     * reduces fairness, which is partially counteracted by using a
     * one-shot form of poll (tryPoll) that may lose to other workers.
     *
     * Deactivation. Method scan returns a sentinel when no tasks are
     * found, leading to deactivation (see awaitWork). The count
     * fields in ctl allow accurate discovery of quiescent states
     * (i.e., when all workers are idle) after deactivation. However,
     * this may also race with new (external) submissions, so a
     * recheck is also needed to determine quiescence. Upon apparently
     * triggering quiescence, awaitWork re-scans and self-signals if
     * it may have missed a signal. In other cases, a missed signal
     * may transiently lower parallelism because deactivation does not
     * necessarily mean that there is no more work, only that that
     * there were no tasks not taken by other workers.  But more
     * signals are generated (see above) to eventually reactivate if
     * needed.
     *
     * Trimming workers. To release resources after periods of lack of
     * use, a worker starting to wait when the pool is quiescent will
     * time out and terminate if the pool has remained quiescent for
     * period given by field keepAlive.
     *
     * Shutdown and Termination. A call to shutdownNow invokes
     * tryTerminate to atomically set a mode bit. The calling thread,
     * as well as every other worker thereafter terminating, helps
     * terminate others by cancelling their unprocessed tasks, and
     * waking them up. Calls to non-abrupt shutdown() preface this by
     * checking isQuiescent before triggering the "STOP" phase of
     * termination. To conform to ExecutorService invoke, invokeAll,
     * and invokeAny specs, we must track pool status while waiting,
     * and interrupt interruptible callers on termination (see
     * ForkJoinTask.joinForPoolInvoke etc).
     *
     * Joining Tasks
     * =============
     *
     * Normally, the first option when joining a task that is not done
     * is to try to unfork it from local queue and run it.  Otherwise,
     * any of several actions may be taken when one worker is waiting
     * to join a task stolen (or always held) by another.  Because we
     * are multiplexing many tasks on to a pool of workers, we can't
     * always just let them block (as in Thread.join).  We also cannot
     * just reassign the joiner's run-time stack with another and
     * replace it later, which would be a form of "continuation", that
     * even if possible is not necessarily a good idea since we may
     * need both an unblocked task and its continuation to progress.
     * Instead we combine two tactics:
     *
     *   Helping: Arranging for the joiner to execute some task that it
     *      could be running if the steal had not occurred.
     *
     *   Compensating: Unless there are already enough live threads,
     *      method tryCompensate() may create or re-activate a spare
     *      thread to compensate for blocked joiners until they unblock.
     *
     * A third form (implemented via tryRemove) amounts to helping a
     * hypothetical compensator: If we can readily tell that a
     * possible action of a compensator is to steal and execute the
     * task being joined, the joining thread can do so directly,
     * without the need for a compensation thread; although with a
     * (rare) possibility of reduced parallelism because of a
     * transient gap in the queue array.
     *
     * Other intermediate forms available for specific task types (for
     * example helpAsyncBlocker) often avoid or postpone the need for
     * blocking or compensation.
     *
     * The ManagedBlocker extension API can't use helping so relies
     * only on compensation in method awaitBlocker.
     *
     * The algorithm in helpJoin entails a form of "linear helping".
     * Each worker records (in field "source") the id of the queue
     * from which it last stole a task.  The scan in method helpJoin
     * uses these markers to try to find a worker to help (i.e., steal
     * back a task from and execute it) that could hasten completion
     * of the actively joined task.  Thus, the joiner executes a task
     * that would be on its own local deque if the to-be-joined task
     * had not been stolen. This is a conservative variant of the
     * approach described in Wagner & Calder "Leapfrogging: a portable
     * technique for implementing efficient futures" SIGPLAN Notices,
     * 1993 (http://portal.acm.org/citation.cfm?id=155354). It differs
     * mainly in that we only record queue ids, not full dependency
     * links.  This requires a linear scan of the queues array to
     * locate stealers, but isolates cost to when it is needed, rather
     * than adding to per-task overhead. Also, searches are limited to
     * direct and at most two levels of indirect stealers, after which
     * there are rapidly diminishing returns on increased overhead.
     * Searches can fail to locate stealers when stalls delay
     * recording sources.  Further, even when accurately identified,
     * stealers might not ever produce a task that the joiner can in
     * turn help with. So, compensation is tried upon failure to find
     * tasks to run.
     *
     * Joining CountedCompleters (see helpComplete) differs from (and
     * is generally more efficient than) other cases because task
     * eligibility is determined by checking completion chains rather
     * than tracking stealers.
     *
     * Joining under timeouts (ForkJoinTask timed get) uses a
     * constrained mixture of helping and compensating in part because
     * pools (actually, only the common pool) may not have any
     * available threads: If the pool is saturated (all available
     * workers are busy), the caller tries to remove and otherwise
     * help; else it blocks under compensation so that it may time out
     * independently of any tasks.
     *
     * Compensation does not by default aim to keep exactly the target
     * parallelism number of unblocked threads running at any given
     * time. Some previous versions of this class employed immediate
     * compensations for any blocked join. However, in practice, the
     * vast majority of blockages are transient byproducts of GC and
     * other JVM or OS activities that are made worse by replacement
     * when they cause longer-term oversubscription.  Rather than
     * impose arbitrary policies, we allow users to override the
     * default of only adding threads upon apparent starvation.  The
     * compensation mechanism may also be bounded.  Bounds for the
     * commonPool (see COMMON_MAX_SPARES) better enable JVMs to cope
     * with programming errors and abuse before running out of
     * resources to do so.
     *
     * Common Pool
     * ===========
     *
     * The static common pool always exists after static
     * initialization.  Since it (or any other created pool) need
     * never be used, we minimize initial construction overhead and
     * footprint to the setup of about a dozen fields.
     *
     * When external threads submit to the common pool, they can
     * perform subtask processing (see helpComplete and related
     * methods) upon joins.  This caller-helps policy makes it
     * sensible to set common pool parallelism level to one (or more)
     * less than the total number of available cores, or even zero for
     * pure caller-runs.  We do not need to record whether external
     * submissions are to the common pool -- if not, external help
     * methods return quickly. These submitters would otherwise be
     * blocked waiting for completion, so the extra effort (with
     * liberally sprinkled task status checks) in inapplicable cases
     * amounts to an odd form of limited spin-wait before blocking in
     * ForkJoinTask.join.
     *
     * Guarantees for common pool parallelism zero are limited to
     * tasks that are joined by their callers in a tree-structured
     * fashion or use CountedCompleters (as is true for jdk
     * parallelStreams). Support infiltrates several methods,
     * including those that retry helping steps until we are sure that
     * none apply if there are no workers.
     *
     * As a more appropriate default in managed environments, unless
     * overridden by system properties, we use workers of subclass
     * InnocuousForkJoinWorkerThread when there is a SecurityManager
     * present. These workers have no permissions set, do not belong
     * to any user-defined ThreadGroup, and erase all ThreadLocals
     * after executing any top-level task.  The associated mechanics
     * may be JVM-dependent and must access particular Thread class
     * fields to achieve this effect.
     *
     * Interrupt handling
     * ==================
     *
     * The framework is designed to manage task cancellation
     * (ForkJoinTask.cancel) independently from the interrupt status
     * of threads running tasks. (See the public ForkJoinTask
     * documentation for rationale.)  Interrupts are issued only in
     * tryTerminate, when workers should be terminating and tasks
     * should be cancelled anyway. Interrupts are cleared only when
     * necessary to ensure that calls to LockSupport.park do not loop
     * indefinitely (park returns immediately if the current thread is
     * interrupted). If so, interruption is reinstated after blocking
     * if status could be visible during the scope of any task.  For
     * cases in which task bodies are specified or desired to
     * interrupt upon cancellation, ForkJoinTask.cancel can be
     * overridden to do so (as is done for invoke{Any,All}).
     *
     * Memory placement
     * ================
     *
     * Performance can be very sensitive to placement of instances of
     * ForkJoinPool and WorkQueues and their queue arrays. To reduce
     * false-sharing impact, the @Contended annotation isolates the
     * ForkJoinPool.ctl field as well as the most heavily written
     * WorkQueue fields. These mainly reduce cache traffic by scanners.
     * WorkQueue arrays are presized large enough to avoid resizing
     * (which transiently reduces throughput) in most tree-like
     * computations, although not in some streaming usages. Initial
     * sizes are not large enough to avoid secondary contention
     * effects (especially for GC cardmarks) when queues are placed
     * near each other in memory. This is common, but has different
     * impact in different collectors and remains incompletely
     * addressed.
     *
     * Style notes
     * ===========
     *
     * Memory ordering relies mainly on atomic operations (CAS,
     * getAndSet, getAndAdd) along with explicit fences.  This can be
     * awkward and ugly, but also reflects the need to control
     * outcomes across the unusual cases that arise in very racy code
     * with very few invariants. All fields are read into locals
     * before use, and null-checked if they are references, even if
     * they can never be null under current usages.  Array accesses
     * using masked indices include checks (that are always true) that
     * the array length is non-zero to avoid compilers inserting more
     * expensive traps.  This is usually done in a "C"-like style of
     * listing declarations at the heads of methods or blocks, and
     * using inline assignments on first encounter.  Nearly all
     * explicit checks lead to bypass/return, not exception throws,
     * because they may legitimately arise during shutdown.
     *
     * There is a lot of representation-level coupling among classes
     * ForkJoinPool, ForkJoinWorkerThread, and ForkJoinTask.  The
     * fields of WorkQueue maintain data structures managed by
     * ForkJoinPool, so are directly accessed.  There is little point
     * trying to reduce this, since any associated future changes in
     * representations will need to be accompanied by algorithmic
     * changes anyway. Several methods intrinsically sprawl because
     * they must accumulate sets of consistent reads of fields held in
     * local variables. Some others are artificially broken up to
     * reduce producer/consumer imbalances due to dynamic compilation.
     * There are also other coding oddities (including several
     * unnecessary-looking hoisted null checks) that help some methods
     * perform reasonably even when interpreted (not compiled).
     *
     * The order of declarations in this file is (with a few exceptions):
     * (1) Static utility functions
     * (2) Nested (static) classes
     * (3) Static fields
     * (4) Fields, along with constants used when unpacking some of them
     * (5) Internal control methods
     * (6) Callbacks and other support for ForkJoinTask methods
     * (7) Exported methods
     * (8) Static block initializing statics in minimally dependent order
     *
     * Revision notes
     * ==============
     *
     * The main sources of differences of January 2020 ForkJoin
     * classes from previous version are:
     *
     * * ForkJoinTask now uses field "aux" to support blocking joins
     *   and/or record exceptions, replacing reliance on builtin
     *   monitors and side tables.
     * * Scans probe slots (vs compare indices), along with related
     *   changes that reduce performance differences across most
     *   garbage collectors, and reduce contention.
     * * Refactoring for better integration of special task types and
     *   other capabilities that had been incrementally tacked on. Plus
     *   many minor reworkings to improve consistency.
     */

    // Static utilities

    /**
     * If there is a security manager, makes sure caller has
     * permission to modify threads.
     */
    private static void checkPermission() {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null)
            security.checkPermission(modifyThreadPermission);
    }

    @SuppressWarnings("removal")
    static AccessControlContext contextWithPermissions(Permission ... perms) {
        Permissions permissions = new Permissions();
        for (Permission perm : perms)
            permissions.add(perm);
        return new AccessControlContext(
            new ProtectionDomain[] { new ProtectionDomain(null, permissions) });
    }

    // Nested classes

    /**
     * Factory for creating new {@link ForkJoinWorkerThread}s.
     * A {@code ForkJoinWorkerThreadFactory} must be defined and used
     * for {@code ForkJoinWorkerThread} subclasses that extend base
     * functionality or initialize threads with different contexts.
     */
    public static interface ForkJoinWorkerThreadFactory {
        /**
         * Returns a new worker thread operating in the given pool.
         * Returning null or throwing an exception may result in tasks
         * never being executed.  If this method throws an exception,
         * it is relayed to the caller of the method (for example
         * {@code execute}) causing attempted thread creation. If this
         * method returns null or throws an exception, it is not
         * retried until the next attempted creation (for example
         * another call to {@code execute}).
         *
         * @param pool the pool this thread works in
         * @return the new worker thread, or {@code null} if the request
         *         to create a thread is rejected
         * @throws NullPointerException if the pool is null
         */
        public ForkJoinWorkerThread newThread(ForkJoinPool pool);
    }

    /**
     * Default ForkJoinWorkerThreadFactory implementation; creates a
     * new ForkJoinWorkerThread using the system class loader as the
     * thread context class loader.
     */
    static final class DefaultForkJoinWorkerThreadFactory
        implements ForkJoinWorkerThreadFactory {
        // ACC for access to the factory
        @SuppressWarnings("removal")
        private static final AccessControlContext ACC = contextWithPermissions(
            new RuntimePermission("getClassLoader"),
            new RuntimePermission("setContextClassLoader"));
        @SuppressWarnings("removal")
        public final ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            return AccessController.doPrivileged(
                new PrivilegedAction<>() {
                    public ForkJoinWorkerThread run() {
                        return new ForkJoinWorkerThread(null, pool, true, false);
                    }},
                ACC);
        }
    }

    /**
     * Factory for CommonPool unless overridden by System property.
     * Creates InnocuousForkJoinWorkerThreads if a security manager is
     * present at time of invocation.  Support requires that we break
     * quite a lot of encapsulation (some via helper methods in
     * ThreadLocalRandom) to access and set Thread fields.
     */
    static final class DefaultCommonPoolForkJoinWorkerThreadFactory
        implements ForkJoinWorkerThreadFactory {
        @SuppressWarnings("removal")
        private static final AccessControlContext ACC = contextWithPermissions(
            modifyThreadPermission,
            new RuntimePermission("enableContextClassLoaderOverride"),
            new RuntimePermission("modifyThreadGroup"),
            new RuntimePermission("getClassLoader"),
            new RuntimePermission("setContextClassLoader"));

        @SuppressWarnings("removal")
        public final ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            return AccessController.doPrivileged(
                 new PrivilegedAction<>() {
                     public ForkJoinWorkerThread run() {
                         return System.getSecurityManager() == null ?
                             new ForkJoinWorkerThread(null, pool, true, true):
                             new ForkJoinWorkerThread.
                             InnocuousForkJoinWorkerThread(pool); }},
                 ACC);
        }
    }

    // Constants shared across ForkJoinPool and WorkQueue

    // Bounds
    static final int SWIDTH       = 16;            // width of short
    static final int SMASK        = 0xffff;        // short bits == max index
    static final int MAX_CAP      = 0x7fff;        // max #workers - 1

    // Masks and units for WorkQueue.phase and ctl sp subfield
    static final int UNSIGNALLED  = 1 << 31;       // must be negative
    static final int SS_SEQ       = 1 << 16;       // version count

    // Mode bits and sentinels, some also used in WorkQueue fields
    static final int FIFO         = 1 << 16;       // fifo queue or access mode
    static final int SRC          = 1 << 17;       // set for valid queue ids
    static final int INNOCUOUS    = 1 << 18;       // set for Innocuous workers
    static final int QUIET        = 1 << 19;       // quiescing phase or source
    static final int SHUTDOWN     = 1 << 24;
    static final int TERMINATED   = 1 << 25;
    static final int STOP         = 1 << 31;       // must be negative
    static final int UNCOMPENSATE = 1 << 16;       // tryCompensate return

    /**
     * Initial capacity of work-stealing queue array.  Must be a power
     * of two, at least 2. See above.
     */
    static final int INITIAL_QUEUE_CAPACITY = 1 << 8;

    /**
     * Queues supporting work-stealing as well as external task
     * submission. See above for descriptions and algorithms.
     */
    static final class WorkQueue {
        volatile int phase;        // versioned, negative if inactive
        int stackPred;             // pool stack (ctl) predecessor link
        int config;                // index, mode, ORed with SRC after init
        int base;                  // index of next slot for poll
        ForkJoinTask<?>[] array;   // the queued tasks; power of 2 size
        final ForkJoinWorkerThread owner; // owning thread or null if shared

        // segregate fields frequently updated but not read by scans or steals
        @jdk.internal.vm.annotation.Contended("w")
        int top;                   // index of next slot for push
        @jdk.internal.vm.annotation.Contended("w")
        volatile int source;       // source queue id, lock, or sentinel
        @jdk.internal.vm.annotation.Contended("w")
        int nsteals;               // number of steals from other queues

        // Support for atomic operations
        private static final VarHandle QA; // for array slots
        private static final VarHandle SOURCE;
        private static final VarHandle BASE;
        static final ForkJoinTask<?> getSlot(ForkJoinTask<?>[] a, int i) {
            return (ForkJoinTask<?>)QA.getAcquire(a, i);
        }
        static final ForkJoinTask<?> getAndClearSlot(ForkJoinTask<?>[] a,
                                                     int i) {
            return (ForkJoinTask<?>)QA.getAndSet(a, i, null);
        }
        static final void setSlotVolatile(ForkJoinTask<?>[] a, int i,
                                          ForkJoinTask<?> v) {
            QA.setVolatile(a, i, v);
        }
        static final boolean casSlotToNull(ForkJoinTask<?>[] a, int i,
                                          ForkJoinTask<?> c) {
            return QA.compareAndSet(a, i, c, null);
        }
        final boolean tryLock() {
            return SOURCE.compareAndSet(this, 0, 1);
        }
        final void setBaseOpaque(int b) {
            BASE.setOpaque(this, b);
        }

        /**
         * Constructor used by ForkJoinWorkerThreads. Most fields
         * are initialized upon thread start, in pool.registerWorker.
         */
        WorkQueue(ForkJoinWorkerThread owner, boolean isInnocuous) {
            this.config = (isInnocuous) ? INNOCUOUS : 0;
            this.owner = owner;
        }

        /**
         * Constructor used for external queues.
         */
        WorkQueue(int config) {
            array = new ForkJoinTask<?>[INITIAL_QUEUE_CAPACITY];
            this.config = config;
            owner = null;
            phase = -1;
        }

        /**
         * Returns an exportable index (used by ForkJoinWorkerThread).
         */
        final int getPoolIndex() {
            return (config & 0xffff) >>> 1; // ignore odd/even tag bit
        }

        /**
         * Returns the approximate number of tasks in the queue.
         */
        final int queueSize() {
            VarHandle.acquireFence(); // ensure fresh reads by external callers
            int n = top - base;
            return (n < 0) ? 0 : n;   // ignore transient negative
        }

        /**
         * Provides a more conservative estimate of whether this queue
         * has any tasks than does queueSize.
         */
        final boolean isEmpty() {
            return !((source != 0 && owner == null) || top - base > 0);
        }

        /**
         * Pushes a task. Call only by owner in unshared queues.
         *
         * @param task the task. Caller must ensure non-null.
         * @param pool (no-op if null)
         * @throws RejectedExecutionException if array cannot be resized
         */
        final void push(ForkJoinTask<?> task, ForkJoinPool pool) {
            ForkJoinTask<?>[] a = array;
            int s = top++, d = s - base, cap, m; // skip insert if disabled
            if (a != null && pool != null && (cap = a.length) > 0) {
                setSlotVolatile(a, (m = cap - 1) & s, task);
                if (d == m)
                    growArray();
                if (d == m || a[m & (s - 1)] == null)
                    pool.signalWork(); // signal if was empty or resized
            }
        }

        /**
         * Pushes task to a shared queue with lock already held, and unlocks.
         *
         * @return true if caller should signal work
         */
        final boolean lockedPush(ForkJoinTask<?> task) {
            ForkJoinTask<?>[] a = array;
            int s = top++, d = s - base, cap, m;
            if (a != null && (cap = a.length) > 0) {
                a[(m = cap - 1) & s] = task;
                if (d == m)
                    growArray();
                source = 0; // unlock
                if (d == m || a[m & (s - 1)] == null)
                    return true;
            }
            return false;
        }

        /**
         * Doubles the capacity of array. Called by owner or with lock
         * held after pre-incrementing top, which is reverted on
         * allocation failure.
         */
        final void growArray() {
            ForkJoinTask<?>[] oldArray = array, newArray;
            int s = top - 1, oldCap, newCap;
            if (oldArray != null && (oldCap = oldArray.length) > 0 &&
                (newCap = oldCap << 1) > 0) { // skip if disabled
                try {
                    newArray = new ForkJoinTask<?>[newCap];
                } catch (Throwable ex) {
                    top = s;
                    if (owner == null)
                        source = 0; // unlock
                    throw new RejectedExecutionException(
                        "Queue capacity exceeded");
                }
                int newMask = newCap - 1, oldMask = oldCap - 1;
                for (int k = oldCap; k > 0; --k, --s) {
                    ForkJoinTask<?> x;        // poll old, push to new
                    if ((x = getAndClearSlot(oldArray, s & oldMask)) == null)
                        break;                // others already taken
                    newArray[s & newMask] = x;
                }
                VarHandle.releaseFence();     // fill before publish
                array = newArray;
            }
        }

        // Variants of pop

        /**
         * Pops and returns task, or null if empty. Called only by owner.
         */
        private ForkJoinTask<?> pop() {
            ForkJoinTask<?> t = null;
            int s = top, cap; ForkJoinTask<?>[] a;
            if ((a = array) != null && (cap = a.length) > 0 && base != s-- &&
                (t = getAndClearSlot(a, (cap - 1) & s)) != null)
                top = s;
            return t;
        }

        /**
         * Pops the given task for owner only if it is at the current top.
         */
        final boolean tryUnpush(ForkJoinTask<?> task) {
            int s = top, cap; ForkJoinTask<?>[] a;
            if ((a = array) != null && (cap = a.length) > 0 && base != s-- &&
                casSlotToNull(a, (cap - 1) & s, task)) {
                top = s;
                return true;
            }
            return false;
        }

        /**
         * Locking version of tryUnpush.
         */
        final boolean externalTryUnpush(ForkJoinTask<?> task) {
            boolean taken = false;
            for (;;) {
                int s = top, cap, k; ForkJoinTask<?>[] a;
                if ((a = array) == null || (cap = a.length) <= 0 ||
                    a[k = (cap - 1) & (s - 1)] != task)
                    break;
                if (tryLock()) {
                    if (top == s && array == a) {
                        if (taken = casSlotToNull(a, k, task)) {
                            top = s - 1;
                            source = 0;
                            break;
                        }
                    }
                    source = 0; // release lock for retry
                }
                Thread.yield(); // trylock failure
            }
            return taken;
        }

        /**
         * Deep form of tryUnpush: Traverses from top and removes task if
         * present, shifting others to fill gap.
         */
        final boolean tryRemove(ForkJoinTask<?> task, boolean owned) {
            boolean taken = false;
            int p = top, cap; ForkJoinTask<?>[] a; ForkJoinTask<?> t;
            if ((a = array) != null && task != null && (cap = a.length) > 0) {
                int m = cap - 1, s = p - 1, d = p - base;
                for (int i = s, k; d > 0; --i, --d) {
                    if ((t = a[k = i & m]) == task) {
                        if (owned || tryLock()) {
                            if ((owned || (array == a && top == p)) &&
                                (taken = casSlotToNull(a, k, t))) {
                                for (int j = i; j != s; ) // shift down
                                    a[j & m] = getAndClearSlot(a, ++j & m);
                                top = s;
                            }
                            if (!owned)
                                source = 0;
                        }
                        break;
                    }
                }
            }
            return taken;
        }

        // variants of poll

        /**
         * Tries once to poll next task in FIFO order, failing on
         * inconsistency or contention.
         */
        final ForkJoinTask<?> tryPoll() {
            int cap, b, k; ForkJoinTask<?>[] a;
            if ((a = array) != null && (cap = a.length) > 0) {
                ForkJoinTask<?> t = getSlot(a, k = (cap - 1) & (b = base));
                if (base == b++ && t != null && casSlotToNull(a, k, t)) {
                    setBaseOpaque(b);
                    return t;
                }
            }
            return null;
        }

        /**
         * Takes next task, if one exists, in order specified by mode.
         */
        final ForkJoinTask<?> nextLocalTask(int cfg) {
            ForkJoinTask<?> t = null;
            int s = top, cap; ForkJoinTask<?>[] a;
            if ((a = array) != null && (cap = a.length) > 0) {
                for (int b, d;;) {
                    if ((d = s - (b = base)) <= 0)
                        break;
                    if (d == 1 || (cfg & FIFO) == 0) {
                        if ((t = getAndClearSlot(a, --s & (cap - 1))) != null)
                            top = s;
                        break;
                    }
                    if ((t = getAndClearSlot(a, b++ & (cap - 1))) != null) {
                        setBaseOpaque(b);
                        break;
                    }
                }
            }
            return t;
        }

        /**
         * Takes next task, if one exists, using configured mode.
         */
        final ForkJoinTask<?> nextLocalTask() {
            return nextLocalTask(config);
        }

        /**
         * Returns next task, if one exists, in order specified by mode.
         */
        final ForkJoinTask<?> peek() {
            VarHandle.acquireFence();
            int cap; ForkJoinTask<?>[] a;
            return ((a = array) != null && (cap = a.length) > 0) ?
                a[(cap - 1) & ((config & FIFO) != 0 ? base : top - 1)] : null;
        }

        // specialized execution methods

        /**
         * Runs the given (stolen) task if nonnull, as well as
         * remaining local tasks and/or others available from the
         * given queue.
         */
        final void topLevelExec(ForkJoinTask<?> task, WorkQueue q) {
            int cfg = config, nstolen = 1;
            while (task != null) {
                task.doExec();
                if ((task = nextLocalTask(cfg)) == null &&
                    q != null && (task = q.tryPoll()) != null)
                    ++nstolen;
            }
            nsteals += nstolen;
            source = 0;
            if ((cfg & INNOCUOUS) != 0)
                ThreadLocalRandom.eraseThreadLocals(Thread.currentThread());
        }

        /**
         * Tries to pop and run tasks within the target's computation
         * until done, not found, or limit exceeded.
         *
         * @param task root of CountedCompleter computation
         * @param owned true if owned by a ForkJoinWorkerThread
         * @param limit max runs, or zero for no limit
         * @return task status on exit
         */
        final int helpComplete(ForkJoinTask<?> task, boolean owned, int limit) {
            int status = 0, cap, k, p, s; ForkJoinTask<?>[] a; ForkJoinTask<?> t;
            while (task != null && (status = task.status) >= 0 &&
                   (a = array) != null && (cap = a.length) > 0 &&
                   (t = a[k = (cap - 1) & (s = (p = top) - 1)])
                   instanceof CountedCompleter) {
                CountedCompleter<?> f = (CountedCompleter<?>)t;
                boolean taken = false;
                for (;;) {     // exec if root task is a completer of t
                    if (f == task) {
                        if (owned) {
                            if ((taken = casSlotToNull(a, k, t)))
                                top = s;
                        }
                        else if (tryLock()) {
                            if (top == p && array == a &&
                                (taken = casSlotToNull(a, k, t)))
                                top = s;
                            source = 0;
                        }
                        if (taken)
                            t.doExec();
                        else if (!owned)
                            Thread.yield(); // tryLock failure
                        break;
                    }
                    else if ((f = f.completer) == null)
                        break;
                }
                if (taken && limit != 0 && --limit == 0)
                    break;
            }
            return status;
        }

        /**
         * Tries to poll and run AsynchronousCompletionTasks until
         * none found or blocker is released.
         *
         * @param blocker the blocker
         */
        final void helpAsyncBlocker(ManagedBlocker blocker) {
            int cap, b, d, k; ForkJoinTask<?>[] a; ForkJoinTask<?> t;
            while (blocker != null && (d = top - (b = base)) > 0 &&
                   (a = array) != null && (cap = a.length) > 0 &&
                   (((t = getSlot(a, k = (cap - 1) & b)) == null && d > 1) ||
                    t instanceof
                    CompletableFuture.AsynchronousCompletionTask) &&
                   !blocker.isReleasable()) {
                if (t != null && base == b++ && casSlotToNull(a, k, t)) {
                    setBaseOpaque(b);
                    t.doExec();
                }
            }
        }

        // misc

        /** AccessControlContext for innocuous workers, created on 1st use. */
        @SuppressWarnings("removal")
        private static AccessControlContext INNOCUOUS_ACC;

        /**
         * Initializes (upon registration) InnocuousForkJoinWorkerThreads.
         */
        @SuppressWarnings("removal")
        final void initializeInnocuousWorker() {
            AccessControlContext acc; // racy construction OK
            if ((acc = INNOCUOUS_ACC) == null)
                INNOCUOUS_ACC = acc = new AccessControlContext(
                    new ProtectionDomain[] { new ProtectionDomain(null, null) });
            Thread t = Thread.currentThread();
            ThreadLocalRandom.setInheritedAccessControlContext(t, acc);
            ThreadLocalRandom.eraseThreadLocals(t);
        }

        /**
         * Returns true if owned by a worker thread and not known to be blocked.
         */
        final boolean isApparentlyUnblocked() {
            Thread wt; Thread.State s;
            return ((wt = owner) != null &&
                    (s = wt.getState()) != Thread.State.BLOCKED &&
                    s != Thread.State.WAITING &&
                    s != Thread.State.TIMED_WAITING);
        }

        static {
            try {
                QA = MethodHandles.arrayElementVarHandle(ForkJoinTask[].class);
                MethodHandles.Lookup l = MethodHandles.lookup();
                SOURCE = l.findVarHandle(WorkQueue.class, "source", int.class);
                BASE = l.findVarHandle(WorkQueue.class, "base", int.class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    // static fields (initialized in static initializer below)

    /**
     * Creates a new ForkJoinWorkerThread. This factory is used unless
     * overridden in ForkJoinPool constructors.
     */
    public static final ForkJoinWorkerThreadFactory
        defaultForkJoinWorkerThreadFactory;

    /**
     * Permission required for callers of methods that may start or
     * kill threads.
     */
    static final RuntimePermission modifyThreadPermission;

    /**
     * Common (static) pool. Non-null for public use unless a static
     * construction exception, but internal usages null-check on use
     * to paranoically avoid potential initialization circularities
     * as well as to simplify generated code.
     */
    static final ForkJoinPool common;

    /**
     * Common pool parallelism. To allow simpler use and management
     * when common pool threads are disabled, we allow the underlying
     * common.parallelism field to be zero, but in that case still report
     * parallelism as 1 to reflect resulting caller-runs mechanics.
     */
    static final int COMMON_PARALLELISM;

    /**
     * Limit on spare thread construction in tryCompensate.
     */
    private static final int COMMON_MAX_SPARES;

    /**
     * Sequence number for creating worker names
     */
    private static volatile int poolIds;

    // static configuration constants

    /**
     * Default idle timeout value (in milliseconds) for the thread
     * triggering quiescence to park waiting for new work
     */
    private static final long DEFAULT_KEEPALIVE = 60_000L;

    /**
     * Undershoot tolerance for idle timeouts
     */
    private static final long TIMEOUT_SLOP = 20L;

    /**
     * The default value for COMMON_MAX_SPARES.  Overridable using the
     * "java.util.concurrent.ForkJoinPool.common.maximumSpares" system
     * property.  The default value is far in excess of normal
     * requirements, but also far short of MAX_CAP and typical OS
     * thread limits, so allows JVMs to catch misuse/abuse before
     * running out of resources needed to do so.
     */
    private static final int DEFAULT_COMMON_MAX_SPARES = 256;

    /*
     * Bits and masks for field ctl, packed with 4 16 bit subfields:
     * RC: Number of released (unqueued) workers minus target parallelism
     * TC: Number of total workers minus target parallelism
     * SS: version count and status of top waiting thread
     * ID: poolIndex of top of Treiber stack of waiters
     *
     * When convenient, we can extract the lower 32 stack top bits
     * (including version bits) as sp=(int)ctl.  The offsets of counts
     * by the target parallelism and the positionings of fields makes
     * it possible to perform the most common checks via sign tests of
     * fields: When ac is negative, there are not enough unqueued
     * workers, when tc is negative, there are not enough total
     * workers.  When sp is non-zero, there are waiting workers.  To
     * deal with possibly negative fields, we use casts in and out of
     * "short" and/or signed shifts to maintain signedness.
     *
     * Because it occupies uppermost bits, we can add one release
     * count using getAndAdd of RC_UNIT, rather than CAS, when
     * returning from a blocked join.  Other updates entail multiple
     * subfields and masking, requiring CAS.
     *
     * The limits packed in field "bounds" are also offset by the
     * parallelism level to make them comparable to the ctl rc and tc
     * fields.
     */

    // Lower and upper word masks
    private static final long SP_MASK    = 0xffffffffL;
    private static final long UC_MASK    = ~SP_MASK;

    // Release counts
    private static final int  RC_SHIFT   = 48;
    private static final long RC_UNIT    = 0x0001L << RC_SHIFT;
    private static final long RC_MASK    = 0xffffL << RC_SHIFT;

    // Total counts
    private static final int  TC_SHIFT   = 32;
    private static final long TC_UNIT    = 0x0001L << TC_SHIFT;
    private static final long TC_MASK    = 0xffffL << TC_SHIFT;
    private static final long ADD_WORKER = 0x0001L << (TC_SHIFT + 15); // sign

    // Instance fields

    final long keepAlive;                // milliseconds before dropping if idle
    volatile long stealCount;            // collects worker nsteals
    int scanRover;                       // advances across pollScan calls
    volatile int threadIds;              // for worker thread names
    final int bounds;                    // min, max threads packed as shorts
    volatile int mode;                   // parallelism, runstate, queue mode
    WorkQueue[] queues;                  // main registry
    final ReentrantLock registrationLock;
    Condition termination;               // lazily constructed
    final String workerNamePrefix;       // null for common pool
    final ForkJoinWorkerThreadFactory factory;
    final UncaughtExceptionHandler ueh;  // per-worker UEH
    final Predicate<? super ForkJoinPool> saturate;

    @jdk.internal.vm.annotation.Contended("fjpctl") // segregate
    volatile long ctl;                   // main pool control

    // Support for atomic operations
    private static final VarHandle CTL;
    private static final VarHandle MODE;
    private static final VarHandle THREADIDS;
    private static final VarHandle POOLIDS;
    private boolean compareAndSetCtl(long c, long v) {
        return CTL.compareAndSet(this, c, v);
    }
    private long compareAndExchangeCtl(long c, long v) {
        return (long)CTL.compareAndExchange(this, c, v);
    }
    private long getAndAddCtl(long v) {
        return (long)CTL.getAndAdd(this, v);
    }
    private int getAndBitwiseOrMode(int v) {
        return (int)MODE.getAndBitwiseOr(this, v);
    }
    private int getAndAddThreadIds(int x) {
        return (int)THREADIDS.getAndAdd(this, x);
    }
    private static int getAndAddPoolIds(int x) {
        return (int)POOLIDS.getAndAdd(x);
    }

    // Creating, registering and deregistering workers

    /**
     * Tries to construct and start one worker. Assumes that total
     * count has already been incremented as a reservation.  Invokes
     * deregisterWorker on any failure.
     *
     * @return true if successful
     */
    private boolean createWorker() {
        ForkJoinWorkerThreadFactory fac = factory;
        Throwable ex = null;
        ForkJoinWorkerThread wt = null;
        try {
            if (fac != null && (wt = fac.newThread(this)) != null) {
                wt.start();
                return true;
            }
        } catch (Throwable rex) {
            ex = rex;
        }
        deregisterWorker(wt, ex);
        return false;
    }

    /**
     * Provides a name for ForkJoinWorkerThread constructor.
     */
    final String nextWorkerThreadName() {
        String prefix = workerNamePrefix;
        int tid = getAndAddThreadIds(1) + 1;
        if (prefix == null) // commonPool has no prefix
            prefix = "ForkJoinPool.commonPool-worker-";
        return prefix.concat(Integer.toString(tid));
    }

    /**
     * Finishes initializing and records owned queue.
     *
     * @param w caller's WorkQueue
     */
    final void registerWorker(WorkQueue w) {
        ReentrantLock lock = registrationLock;
        ThreadLocalRandom.localInit();
        int seed = ThreadLocalRandom.getProbe();
        if (w != null && lock != null) {
            int modebits = (mode & FIFO) | w.config;
            w.array = new ForkJoinTask<?>[INITIAL_QUEUE_CAPACITY];
            w.stackPred = seed;                         // stash for runWorker
            if ((modebits & INNOCUOUS) != 0)
                w.initializeInnocuousWorker();
            int id = (seed << 1) | 1;                   // initial index guess
            lock.lock();
            try {
                WorkQueue[] qs; int n;                  // find queue index
                if ((qs = queues) != null && (n = qs.length) > 0) {
                    int k = n, m = n - 1;
                    for (; qs[id &= m] != null && k > 0; id -= 2, k -= 2);
                    if (k == 0)
                        id = n | 1;                     // resize below
                    w.phase = w.config = id | modebits; // now publishable

                    if (id < n)
                        qs[id] = w;
                    else {                              // expand array
                        int an = n << 1, am = an - 1;
                        WorkQueue[] as = new WorkQueue[an];
                        as[id & am] = w;
                        for (int j = 1; j < n; j += 2)
                            as[j] = qs[j];
                        for (int j = 0; j < n; j += 2) {
                            WorkQueue q;
                            if ((q = qs[j]) != null)    // shared queues may move
                                as[q.config & am] = q;
                        }
                        VarHandle.releaseFence();       // fill before publish
                        queues = as;
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Final callback from terminating worker, as well as upon failure
     * to construct or start a worker.  Removes record of worker from
     * array, and adjusts counts. If pool is shutting down, tries to
     * complete termination.
     *
     * @param wt the worker thread, or null if construction failed
     * @param ex the exception causing failure, or null if none
     */
    final void deregisterWorker(ForkJoinWorkerThread wt, Throwable ex) {
        ReentrantLock lock = registrationLock;
        WorkQueue w = null;
        int cfg = 0;
        if (wt != null && (w = wt.workQueue) != null && lock != null) {
            WorkQueue[] qs; int n, i;
            cfg = w.config;
            long ns = w.nsteals & 0xffffffffL;
            lock.lock();                             // remove index from array
            if ((qs = queues) != null && (n = qs.length) > 0 &&
                qs[i = cfg & (n - 1)] == w)
                qs[i] = null;
            stealCount += ns;                        // accumulate steals
            lock.unlock();
            long c = ctl;
            if ((cfg & QUIET) == 0) // unless self-signalled, decrement counts
                do {} while (c != (c = compareAndExchangeCtl(
                                       c, ((RC_MASK & (c - RC_UNIT)) |
                                           (TC_MASK & (c - TC_UNIT)) |
                                           (SP_MASK & c)))));
            else if ((int)c == 0)                    // was dropped on timeout
                cfg = 0;                             // suppress signal if last
            for (ForkJoinTask<?> t; (t = w.pop()) != null; )
                ForkJoinTask.cancelIgnoringExceptions(t); // cancel tasks
        }

        if (!tryTerminate(false, false) && w != null && (cfg & SRC) != 0)
            signalWork();                            // possibly replace worker
        if (ex != null)
            ForkJoinTask.rethrow(ex);
    }

    /*
     * Tries to create or release a worker if too few are running.
     */
    final void signalWork() {
        for (long c = ctl; c < 0L;) {
            int sp, i; WorkQueue[] qs; WorkQueue v;
            if ((sp = (int)c & ~UNSIGNALLED) == 0) {  // no idle workers
                if ((c & ADD_WORKER) == 0L)           // enough total workers
                    break;
                if (c == (c = compareAndExchangeCtl(
                              c, ((RC_MASK & (c + RC_UNIT)) |
                                  (TC_MASK & (c + TC_UNIT)))))) {
                    createWorker();
                    break;
                }
            }
            else if ((qs = queues) == null)
                break;                                // unstarted/terminated
            else if (qs.length <= (i = sp & SMASK))
                break;                                // terminated
            else if ((v = qs[i]) == null)
                break;                                // terminating
            else {
                long nc = (v.stackPred & SP_MASK) | (UC_MASK & (c + RC_UNIT));
                Thread vt = v.owner;
                if (c == (c = compareAndExchangeCtl(c, nc))) {
                    v.phase = sp;
                    LockSupport.unpark(vt);           // release idle worker
                    break;
                }
            }
        }
    }

    /**
     * Top-level runloop for workers, called by ForkJoinWorkerThread.run.
     * See above for explanation.
     *
     * @param w caller's WorkQueue (may be null on failed initialization)
     */
    final void runWorker(WorkQueue w) {
        if (mode >= 0 && w != null) {           // skip on failed init
            w.config |= SRC;                    // mark as valid source
            int r = w.stackPred, src = 0;       // use seed from registerWorker
            do {
                r ^= r << 13; r ^= r >>> 17; r ^= r << 5; // xorshift
            } while ((src = scan(w, src, r)) >= 0 ||
                     (src = awaitWork(w)) == 0);
        }
    }

    /**
     * Scans for and if found executes top-level tasks: Tries to poll
     * each queue starting at a random index with random stride,
     * returning source id or retry indicator if contended or
     * inconsistent.
     *
     * @param w caller's WorkQueue
     * @param prevSrc the previous queue stolen from in current phase, or 0
     * @param r random seed
     * @return id of queue if taken, negative if none found, prevSrc for retry
     */
    private int scan(WorkQueue w, int prevSrc, int r) {
        WorkQueue[] qs = queues;
        int n = (w == null || qs == null) ? 0 : qs.length;
        for (int step = (r >>> 16) | 1, i = n; i > 0; --i, r += step) {
            int j, cap, b; WorkQueue q; ForkJoinTask<?>[] a;
            if ((q = qs[j = r & (n - 1)]) != null && // poll at qs[j].array[k]
                (a = q.array) != null && (cap = a.length) > 0) {
                int k = (cap - 1) & (b = q.base), nextBase = b + 1;
                int nextIndex = (cap - 1) & nextBase, src = j | SRC;
                ForkJoinTask<?> t = WorkQueue.getSlot(a, k);
                if (q.base != b)                // inconsistent
                    return prevSrc;
                else if (t != null && WorkQueue.casSlotToNull(a, k, t)) {
                    q.base = nextBase;
                    ForkJoinTask<?> next = a[nextIndex];
                    if ((w.source = src) != prevSrc && next != null)
                        signalWork();           // propagate
                    w.topLevelExec(t, q);
                    return src;
                }
                else if (a[nextIndex] != null)  // revisit
                    return prevSrc;
            }
        }
        return (queues != qs) ? prevSrc: -1;    // possibly resized
    }

    /**
     * Advances worker phase, pushes onto ctl stack, and awaits signal
     * or reports termination.
     *
     * @return negative if terminated, else 0
     */
    private int awaitWork(WorkQueue w) {
        if (w == null)
            return -1;                       // already terminated
        int phase = (w.phase + SS_SEQ) & ~UNSIGNALLED;
        w.phase = phase | UNSIGNALLED;       // advance phase
        long prevCtl = ctl, c;               // enqueue
        do {
            w.stackPred = (int)prevCtl;
            c = ((prevCtl - RC_UNIT) & UC_MASK) | (phase & SP_MASK);
        } while (prevCtl != (prevCtl = compareAndExchangeCtl(prevCtl, c)));

        Thread.interrupted();                // clear status
        LockSupport.setCurrentBlocker(this); // prepare to block (exit also OK)
        long deadline = 0L;                  // nonzero if possibly quiescent
        int ac = (int)(c >> RC_SHIFT), md;
        if ((md = mode) < 0)                 // pool is terminating
            return -1;
        else if ((md & SMASK) + ac <= 0) {
            boolean checkTermination = (md & SHUTDOWN) != 0;
            if ((deadline = System.currentTimeMillis() + keepAlive) == 0L)
                deadline = 1L;               // avoid zero
            WorkQueue[] qs = queues;         // check for racing submission
            int n = (qs == null) ? 0 : qs.length;
            for (int i = 0; i < n; i += 2) {
                WorkQueue q; ForkJoinTask<?>[] a; int cap, b;
                if (ctl != c) {              // already signalled
                    checkTermination = false;
                    break;
                }
                else if ((q = qs[i]) != null &&
                         (a = q.array) != null && (cap = a.length) > 0 &&
                         ((b = q.base) != q.top || a[(cap - 1) & b] != null ||
                          q.source != 0)) {
                    if (compareAndSetCtl(c, prevCtl))
                        w.phase = phase;     // self-signal
                    checkTermination = false;
                    break;
                }
            }
            if (checkTermination && tryTerminate(false, false))
                return -1;                   // trigger quiescent termination
        }

        for (boolean alt = false;;) {        // await activation or termination
            if (w.phase >= 0)
                break;
            else if (mode < 0)
                return -1;
            else if ((c = ctl) == prevCtl)
                Thread.onSpinWait();         // signal in progress
            else if (!(alt = !alt))          // check between park calls
                Thread.interrupted();
            else if (deadline == 0L)
                LockSupport.park();
            else if (deadline - System.currentTimeMillis() > TIMEOUT_SLOP)
                LockSupport.parkUntil(deadline);
            else if (((int)c & SMASK) == (w.config & SMASK) &&
                     compareAndSetCtl(c, ((UC_MASK & (c - TC_UNIT)) |
                                          (prevCtl & SP_MASK)))) {
                w.config |= QUIET;           // sentinel for deregisterWorker
                return -1;                   // drop on timeout
            }
            else if ((deadline += keepAlive) == 0L)
                deadline = 1L;               // not at head; restart timer
        }
        return 0;
    }

    // Utilities used by ForkJoinTask

    /**
     * Returns true if can start terminating if enabled, or already terminated
     */
    final boolean canStop() {
        outer: for (long oldSum = 0L;;) { // repeat until stable
            int md; WorkQueue[] qs;  long c;
            if ((qs = queues) == null || ((md = mode) & STOP) != 0)
                return true;
            if ((md & SMASK) + (int)((c = ctl) >> RC_SHIFT) > 0)
                break;
            long checkSum = c;
            for (int i = 1; i < qs.length; i += 2) { // scan submitters
                WorkQueue q; ForkJoinTask<?>[] a; int s = 0, cap;
                if ((q = qs[i]) != null && (a = q.array) != null &&
                    (cap = a.length) > 0 &&
                    ((s = q.top) != q.base || a[(cap - 1) & s] != null ||
                     q.source != 0))
                    break outer;
                checkSum += (((long)i) << 32) ^ s;
            }
            if (oldSum == (oldSum = checkSum) && queues == qs)
                return true;
        }
        return (mode & STOP) != 0; // recheck mode on false return
    }

    /**
     * Tries to decrement counts (sometimes implicitly) and possibly
     * arrange for a compensating worker in preparation for
     * blocking. May fail due to interference, in which case -1 is
     * returned so caller may retry. A zero return value indicates
     * that the caller doesn't need to re-adjust counts when later
     * unblocked.
     *
     * @param c incoming ctl value
     * @return UNCOMPENSATE: block then adjust, 0: block, -1 : retry
     */
    private int tryCompensate(long c) {
        Predicate<? super ForkJoinPool> sat;
        int md = mode, b = bounds;
        // counts are signed; centered at parallelism level == 0
        int minActive = (short)(b & SMASK),
            maxTotal  = b >>> SWIDTH,
            active    = (int)(c >> RC_SHIFT),
            total     = (short)(c >>> TC_SHIFT),
            sp        = (int)c & ~UNSIGNALLED;
        if ((md & SMASK) == 0)
            return 0;                  // cannot compensate if parallelism zero
        else if (total >= 0) {
            if (sp != 0) {                        // activate idle worker
                WorkQueue[] qs; int n; WorkQueue v;
                if ((qs = queues) != null && (n = qs.length) > 0 &&
                    (v = qs[sp & (n - 1)]) != null) {
                    Thread vt = v.owner;
                    long nc = ((long)v.stackPred & SP_MASK) | (UC_MASK & c);
                    if (compareAndSetCtl(c, nc)) {
                        v.phase = sp;
                        LockSupport.unpark(vt);
                        return UNCOMPENSATE;
                    }
                }
                return -1;                        // retry
            }
            else if (active > minActive) {        // reduce parallelism
                long nc = ((RC_MASK & (c - RC_UNIT)) | (~RC_MASK & c));
                return compareAndSetCtl(c, nc) ? UNCOMPENSATE : -1;
            }
        }
        if (total < maxTotal) {                   // expand pool
            long nc = ((c + TC_UNIT) & TC_MASK) | (c & ~TC_MASK);
            return (!compareAndSetCtl(c, nc) ? -1 :
                    !createWorker() ? 0 : UNCOMPENSATE);
        }
        else if (!compareAndSetCtl(c, c))         // validate
            return -1;
        else if ((sat = saturate) != null && sat.test(this))
            return 0;
        else
            throw new RejectedExecutionException(
                "Thread limit exceeded replacing blocked worker");
    }

    /**
     * Readjusts RC count; called from ForkJoinTask after blocking.
     */
    final void uncompensate() {
        getAndAddCtl(RC_UNIT);
    }

    /**
     * Helps if possible until the given task is done.  Scans other
     * queues for a task produced by one of w's stealers; returning
     * compensated blocking sentinel if none are found.
     *
     * @param task the task
     * @param w caller's WorkQueue
     * @param canHelp if false, compensate only
     * @return task status on exit, or UNCOMPENSATE for compensated blocking
     */
    final int helpJoin(ForkJoinTask<?> task, WorkQueue w, boolean canHelp) {
        int s = 0;
        if (task != null && w != null) {
            int wsrc = w.source, wid = w.config & SMASK, r = wid + 2;
            boolean scan = true;
            long c = 0L;                          // track ctl stability
            outer: for (;;) {
                if ((s = task.status) < 0)
                    break;
                else if (scan = !scan) {          // previous scan was empty
                    if (mode < 0)
                        ForkJoinTask.cancelIgnoringExceptions(task);
                    else if (c == (c = ctl) && (s = tryCompensate(c)) >= 0)
                        break;                    // block
                }
                else if (canHelp) {               // scan for subtasks
                    WorkQueue[] qs = queues;
                    int n = (qs == null) ? 0 : qs.length, m = n - 1;
                    for (int i = n; i > 0; i -= 2, r += 2) {
                        int j; WorkQueue q, x, y; ForkJoinTask<?>[] a;
                        if ((q = qs[j = r & m]) != null) {
                            int sq = q.source & SMASK, cap, b;
                            if ((a = q.array) != null && (cap = a.length) > 0) {
                                int k = (cap - 1) & (b = q.base);
                                int nextBase = b + 1, src = j | SRC, sx;
                                ForkJoinTask<?> t = WorkQueue.getSlot(a, k);
                                boolean eligible = sq == wid ||
                                    ((x = qs[sq & m]) != null &&   // indirect
                                     ((sx = (x.source & SMASK)) == wid ||
                                      ((y = qs[sx & m]) != null && // 2-indirect
                                       (y.source & SMASK) == wid)));
                                if ((s = task.status) < 0)
                                    break outer;
                                else if ((q.source & SMASK) != sq ||
                                         q.base != b)
                                    scan = true;          // inconsistent
                                else if (t == null)
                                    scan |= (a[nextBase & (cap - 1)] != null ||
                                             q.top != b); // lagging
                                else if (eligible) {
                                    if (WorkQueue.casSlotToNull(a, k, t)) {
                                        q.base = nextBase;
                                        w.source = src;
                                        t.doExec();
                                        w.source = wsrc;
                                    }
                                    scan = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return s;
    }

    /**
     * Extra helpJoin steps for CountedCompleters.  Scans for and runs
     * subtasks of the given root task, returning if none are found.
     *
     * @param task root of CountedCompleter computation
     * @param w caller's WorkQueue
     * @param owned true if owned by a ForkJoinWorkerThread
     * @return task status on exit
     */
    final int helpComplete(ForkJoinTask<?> task, WorkQueue w, boolean owned) {
        int s = 0;
        if (task != null && w != null) {
            int r = w.config;
            boolean scan = true, locals = true;
            long c = 0L;
            outer: for (;;) {
                if (locals) {                     // try locals before scanning
                    if ((s = w.helpComplete(task, owned, 0)) < 0)
                        break;
                    locals = false;
                }
                else if ((s = task.status) < 0)
                    break;
                else if (scan = !scan) {
                    if (c == (c = ctl))
                        break;
                }
                else {                            // scan for subtasks
                    WorkQueue[] qs = queues;
                    int n = (qs == null) ? 0 : qs.length;
                    for (int i = n; i > 0; --i, ++r) {
                        int j, cap, b; WorkQueue q; ForkJoinTask<?>[] a;
                        boolean eligible = false;
                        if ((q = qs[j = r & (n - 1)]) != null &&
                            (a = q.array) != null && (cap = a.length) > 0) {
                            int k = (cap - 1) & (b = q.base), nextBase = b + 1;
                            ForkJoinTask<?> t = WorkQueue.getSlot(a, k);
                            if (t instanceof CountedCompleter) {
                                CountedCompleter<?> f = (CountedCompleter<?>)t;
                                do {} while (!(eligible = (f == task)) &&
                                             (f = f.completer) != null);
                            }
                            if ((s = task.status) < 0)
                                break outer;
                            else if (q.base != b)
                                scan = true;       // inconsistent
                            else if (t == null)
                                scan |= (a[nextBase & (cap - 1)] != null ||
                                         q.top != b);
                            else if (eligible) {
                                if (WorkQueue.casSlotToNull(a, k, t)) {
                                    q.setBaseOpaque(nextBase);
                                    t.doExec();
                                    locals = true;
                                }
                                scan = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return s;
    }

    /**
     * Scans for and returns a polled task, if available.  Used only
     * for untracked polls. Begins scan at an index (scanRover)
     * advanced on each call, to avoid systematic unfairness.
     *
     * @param submissionsOnly if true, only scan submission queues
     */
    private ForkJoinTask<?> pollScan(boolean submissionsOnly) {
        VarHandle.acquireFence();
        int r = scanRover += 0x61c88647; // Weyl increment; raciness OK
        if (submissionsOnly)             // even indices only
            r &= ~1;
        int step = (submissionsOnly) ? 2 : 1;
        WorkQueue[] qs; int n;
        while ((qs = queues) != null && (n = qs.length) > 0) {
            boolean scan = false;
            for (int i = 0; i < n; i += step) {
                int j, cap, b; WorkQueue q; ForkJoinTask<?>[] a;
                if ((q = qs[j = (n - 1) & (r + i)]) != null &&
                    (a = q.array) != null && (cap = a.length) > 0) {
                    int k = (cap - 1) & (b = q.base), nextBase = b + 1;
                    ForkJoinTask<?> t = WorkQueue.getSlot(a, k);
                    if (q.base != b)
                        scan = true;
                    else if (t == null)
                        scan |= (q.top != b || a[nextBase & (cap - 1)] != null);
                    else if (!WorkQueue.casSlotToNull(a, k, t))
                        scan = true;
                    else {
                        q.setBaseOpaque(nextBase);
                        return t;
                    }
                }
            }
            if (!scan && queues == qs)
                break;
        }
        return null;
    }

    /**
     * Runs tasks until {@code isQuiescent()}. Rather than blocking
     * when tasks cannot be found, rescans until all others cannot
     * find tasks either.
     *
     * @param nanos max wait time (Long.MAX_VALUE if effectively untimed)
     * @param interruptible true if return on interrupt
     * @return positive if quiescent, negative if interrupted, else 0
     */
    final int helpQuiescePool(WorkQueue w, long nanos, boolean interruptible) {
        if (w == null)
            return 0;
        long startTime = System.nanoTime(), parkTime = 0L;
        int prevSrc = w.source, wsrc = prevSrc, cfg = w.config, r = cfg + 1;
        for (boolean active = true, locals = true;;) {
            boolean busy = false, scan = false;
            if (locals) {  // run local tasks before (re)polling
                locals = false;
                for (ForkJoinTask<?> u; (u = w.nextLocalTask(cfg)) != null;)
                    u.doExec();
            }
            WorkQueue[] qs = queues;
            int n = (qs == null) ? 0 : qs.length;
            for (int i = n; i > 0; --i, ++r) {
                int j, b, cap; WorkQueue q; ForkJoinTask<?>[] a;
                if ((q = qs[j = (n - 1) & r]) != null && q != w &&
                    (a = q.array) != null && (cap = a.length) > 0) {
                    int k = (cap - 1) & (b = q.base);
                    int nextBase = b + 1, src = j | SRC;
                    ForkJoinTask<?> t = WorkQueue.getSlot(a, k);
                    if (q.base != b)
                        busy = scan = true;
                    else if (t != null) {
                        busy = scan = true;
                        if (!active) {    // increment before taking
                            active = true;
                            getAndAddCtl(RC_UNIT);
                        }
                        if (WorkQueue.casSlotToNull(a, k, t)) {
                            q.base = nextBase;
                            w.source = src;
                            t.doExec();
                            w.source = wsrc = prevSrc;
                            locals = true;
                        }
                        break;
                    }
                    else if (!busy) {
                        if (q.top != b || a[nextBase & (cap - 1)] != null)
                            busy = scan = true;
                        else if (q.source != QUIET && q.phase >= 0)
                            busy = true;
                    }
                }
            }
            VarHandle.acquireFence();
            if (!scan && queues == qs) {
                boolean interrupted;
                if (!busy) {
                    w.source = prevSrc;
                    if (!active)
                        getAndAddCtl(RC_UNIT);
                    return 1;
                }
                if (wsrc != QUIET)
                    w.source = wsrc = QUIET;
                if (active) {                 // decrement
                    active = false;
                    parkTime = 0L;
                    getAndAddCtl(RC_MASK & -RC_UNIT);
                }
                else if (parkTime == 0L) {
                    parkTime = 1L << 10; // initially about 1 usec
                    Thread.yield();
                }
                else if ((interrupted = interruptible && Thread.interrupted()) ||
                         System.nanoTime() - startTime > nanos) {
                    getAndAddCtl(RC_UNIT);
                    return interrupted ? -1 : 0;
                }
                else {
                    LockSupport.parkNanos(this, parkTime);
                    if (parkTime < nanos >>> 8 && parkTime < 1L << 20)
                        parkTime <<= 1;  // max sleep approx 1 sec or 1% nanos
                }
            }
        }
    }

    /**
     * Helps quiesce from external caller until done, interrupted, or timeout
     *
     * @param nanos max wait time (Long.MAX_VALUE if effectively untimed)
     * @param interruptible true if return on interrupt
     * @return positive if quiescent, negative if interrupted, else 0
     */
    final int externalHelpQuiescePool(long nanos, boolean interruptible) {
        for (long startTime = System.nanoTime(), parkTime = 0L;;) {
            ForkJoinTask<?> t;
            if ((t = pollScan(false)) != null) {
                t.doExec();
                parkTime = 0L;
            }
            else if (canStop())
                return 1;
            else if (parkTime == 0L) {
                parkTime = 1L << 10;
                Thread.yield();
            }
            else if ((System.nanoTime() - startTime) > nanos)
                return 0;
            else if (interruptible && Thread.interrupted())
                return -1;
            else {
                LockSupport.parkNanos(this, parkTime);
                if (parkTime < nanos >>> 8 && parkTime < 1L << 20)
                    parkTime <<= 1;
            }
        }
    }

    /**
     * Gets and removes a local or stolen task for the given worker.
     *
     * @return a task, if available
     */
    final ForkJoinTask<?> nextTaskFor(WorkQueue w) {
        ForkJoinTask<?> t;
        if (w == null || (t = w.nextLocalTask(w.config)) == null)
            t = pollScan(false);
        return t;
    }

    // External operations

    /**
     * Finds and locks a WorkQueue for an external submitter, or
     * returns null if shutdown or terminating.
     */
    final WorkQueue submissionQueue() {
        int r;
        if ((r = ThreadLocalRandom.getProbe()) == 0) {
            ThreadLocalRandom.localInit();           // initialize caller's probe
            r = ThreadLocalRandom.getProbe();
        }
        for (int id = r << 1;;) {                    // even indices only
            int md = mode, n, i; WorkQueue q; ReentrantLock lock;
            WorkQueue[] qs = queues;
            if ((md & SHUTDOWN) != 0 || qs == null || (n = qs.length) <= 0)
                return null;
            else if ((q = qs[i = (n - 1) & id]) == null) {
                if ((lock = registrationLock) != null) {
                    WorkQueue w = new WorkQueue(id | SRC);
                    lock.lock();                    // install under lock
                    if (qs[i] == null)
                        qs[i] = w;                  // else lost race; discard
                    lock.unlock();
                }
            }
            else if (!q.tryLock())                  // move and restart
                id = (r = ThreadLocalRandom.advanceProbe(r)) << 1;
            else
                return q;
        }
    }

    /**
     * Adds the given task to an external submission queue, or throws
     * exception if shutdown or terminating.
     *
     * @param task the task. Caller must ensure non-null.
     */
    final void externalPush(ForkJoinTask<?> task) {
        WorkQueue q;
        if ((q = submissionQueue()) == null)
            throw new RejectedExecutionException(); // shutdown or disabled
        else if (q.lockedPush(task))
            signalWork();
    }

    /**
     * Pushes a possibly-external submission.
     */
    private <T> ForkJoinTask<T> externalSubmit(ForkJoinTask<T> task) {
        Thread t; ForkJoinWorkerThread wt; WorkQueue q;
        if (task == null)
            throw new NullPointerException();
        if (((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) &&
            (q = (wt = (ForkJoinWorkerThread)t).workQueue) != null &&
            wt.pool == this)
            q.push(task, this);
        else
            externalPush(task);
        return task;
    }

    /**
     * Returns common pool queue for an external thread that has
     * possibly ever submitted a common pool task (nonzero probe), or
     * null if none.
     */
    static WorkQueue commonQueue() {
        ForkJoinPool p; WorkQueue[] qs;
        int r = ThreadLocalRandom.getProbe(), n;
        return ((p = common) != null && (qs = p.queues) != null &&
                (n = qs.length) > 0 && r != 0) ?
            qs[(n - 1) & (r << 1)] : null;
    }

    /**
     * Returns queue for an external thread, if one exists
     */
    final WorkQueue externalQueue() {
        WorkQueue[] qs;
        int r = ThreadLocalRandom.getProbe(), n;
        return ((qs = queues) != null && (n = qs.length) > 0 && r != 0) ?
            qs[(n - 1) & (r << 1)] : null;
    }

    /**
     * If the given executor is a ForkJoinPool, poll and execute
     * AsynchronousCompletionTasks from worker's queue until none are
     * available or blocker is released.
     */
    static void helpAsyncBlocker(Executor e, ManagedBlocker blocker) {
        WorkQueue w = null; Thread t; ForkJoinWorkerThread wt;
        if ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) {
            if ((wt = (ForkJoinWorkerThread)t).pool == e)
                w = wt.workQueue;
        }
        else if (e instanceof ForkJoinPool)
            w = ((ForkJoinPool)e).externalQueue();
        if (w != null)
            w.helpAsyncBlocker(blocker);
    }

    /**
     * Returns a cheap heuristic guide for task partitioning when
     * programmers, frameworks, tools, or languages have little or no
     * idea about task granularity.  In essence, by offering this
     * method, we ask users only about tradeoffs in overhead vs
     * expected throughput and its variance, rather than how finely to
     * partition tasks.
     *
     * In a steady state strict (tree-structured) computation, each
     * thread makes available for stealing enough tasks for other
     * threads to remain active. Inductively, if all threads play by
     * the same rules, each thread should make available only a
     * constant number of tasks.
     *
     * The minimum useful constant is just 1. But using a value of 1
     * would require immediate replenishment upon each steal to
     * maintain enough tasks, which is infeasible.  Further,
     * partitionings/granularities of offered tasks should minimize
     * steal rates, which in general means that threads nearer the top
     * of computation tree should generate more than those nearer the
     * bottom. In perfect steady state, each thread is at
     * approximately the same level of computation tree. However,
     * producing extra tasks amortizes the uncertainty of progress and
     * diffusion assumptions.
     *
     * So, users will want to use values larger (but not much larger)
     * than 1 to both smooth over transient shortages and hedge
     * against uneven progress; as traded off against the cost of
     * extra task overhead. We leave the user to pick a threshold
     * value to compare with the results of this call to guide
     * decisions, but recommend values such as 3.
     *
     * When all threads are active, it is on average OK to estimate
     * surplus strictly locally. In steady-state, if one thread is
     * maintaining say 2 surplus tasks, then so are others. So we can
     * just use estimated queue length.  However, this strategy alone
     * leads to serious mis-estimates in some non-steady-state
     * conditions (ramp-up, ramp-down, other stalls). We can detect
     * many of these by further considering the number of "idle"
     * threads, that are known to have zero queued tasks, so
     * compensate by a factor of (#idle/#active) threads.
     */
    static int getSurplusQueuedTaskCount() {
        Thread t; ForkJoinWorkerThread wt; ForkJoinPool pool; WorkQueue q;
        if (((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) &&
            (pool = (wt = (ForkJoinWorkerThread)t).pool) != null &&
            (q = wt.workQueue) != null) {
            int p = pool.mode & SMASK;
            int a = p + (int)(pool.ctl >> RC_SHIFT);
            int n = q.top - q.base;
            return n - (a > (p >>>= 1) ? 0 :
                        a > (p >>>= 1) ? 1 :
                        a > (p >>>= 1) ? 2 :
                        a > (p >>>= 1) ? 4 :
                        8);
        }
        return 0;
    }

    // Termination

    /**
     * Possibly initiates and/or completes termination.
     *
     * @param now if true, unconditionally terminate, else only
     * if no work and no active workers
     * @param enable if true, terminate when next possible
     * @return true if terminating or terminated
     */
    private boolean tryTerminate(boolean now, boolean enable) {
        int md; // try to set SHUTDOWN, then STOP, then help terminate
        if (((md = mode) & SHUTDOWN) == 0) {
            if (!enable)
                return false;
            md = getAndBitwiseOrMode(SHUTDOWN);
        }
        if ((md & STOP) == 0) {
            if (!now && !canStop())
                return false;
            md = getAndBitwiseOrMode(STOP);
        }
        for (boolean rescan = true;;) { // repeat until no changes
            boolean changed = false;
            for (ForkJoinTask<?> t; (t = pollScan(false)) != null; ) {
                changed = true;
                ForkJoinTask.cancelIgnoringExceptions(t); // help cancel
            }
            WorkQueue[] qs; int n; WorkQueue q; Thread thread;
            if ((qs = queues) != null && (n = qs.length) > 0) {
                for (int j = 1; j < n; j += 2) { // unblock other workers
                    if ((q = qs[j]) != null && (thread = q.owner) != null &&
                        !thread.isInterrupted()) {
                        changed = true;
                        try {
                            thread.interrupt();
                        } catch (Throwable ignore) {
                        }
                    }
                }
            }
            ReentrantLock lock; Condition cond; // signal when no workers
            if (((md = mode) & TERMINATED) == 0 &&
                (md & SMASK) + (short)(ctl >>> TC_SHIFT) <= 0 &&
                (getAndBitwiseOrMode(TERMINATED) & TERMINATED) == 0 &&
                (lock = registrationLock) != null) {
                lock.lock();
                if ((cond = termination) != null)
                    cond.signalAll();
                lock.unlock();
            }
            if (changed)
                rescan = true;
            else if (rescan)
                rescan = false;
            else
                break;
        }
        return true;
    }

    // Exported methods

    // Constructors

    /**
     * Creates a {@code ForkJoinPool} with parallelism equal to {@link
     * java.lang.Runtime#availableProcessors}, using defaults for all
     * other parameters (see {@link #ForkJoinPool(int,
     * ForkJoinWorkerThreadFactory, UncaughtExceptionHandler, boolean,
     * int, int, int, Predicate, long, TimeUnit)}).
     *
     * @throws SecurityException if a security manager exists and
     *         the caller is not permitted to modify threads
     *         because it does not hold {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")}
     */
    public ForkJoinPool() {
        this(Math.min(MAX_CAP, Runtime.getRuntime().availableProcessors()),
             defaultForkJoinWorkerThreadFactory, null, false,
             0, MAX_CAP, 1, null, DEFAULT_KEEPALIVE, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates a {@code ForkJoinPool} with the indicated parallelism
     * level, using defaults for all other parameters (see {@link
     * #ForkJoinPool(int, ForkJoinWorkerThreadFactory,
     * UncaughtExceptionHandler, boolean, int, int, int, Predicate,
     * long, TimeUnit)}).
     *
     * @param parallelism the parallelism level
     * @throws IllegalArgumentException if parallelism less than or
     *         equal to zero, or greater than implementation limit
     * @throws SecurityException if a security manager exists and
     *         the caller is not permitted to modify threads
     *         because it does not hold {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")}
     */
    public ForkJoinPool(int parallelism) {
        this(parallelism, defaultForkJoinWorkerThreadFactory, null, false,
             0, MAX_CAP, 1, null, DEFAULT_KEEPALIVE, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates a {@code ForkJoinPool} with the given parameters (using
     * defaults for others -- see {@link #ForkJoinPool(int,
     * ForkJoinWorkerThreadFactory, UncaughtExceptionHandler, boolean,
     * int, int, int, Predicate, long, TimeUnit)}).
     *
     * @param parallelism the parallelism level. For default value,
     * use {@link java.lang.Runtime#availableProcessors}.
     * @param factory the factory for creating new threads. For default value,
     * use {@link #defaultForkJoinWorkerThreadFactory}.
     * @param handler the handler for internal worker threads that
     * terminate due to unrecoverable errors encountered while executing
     * tasks. For default value, use {@code null}.
     * @param asyncMode if true,
     * establishes local first-in-first-out scheduling mode for forked
     * tasks that are never joined. This mode may be more appropriate
     * than default locally stack-based mode in applications in which
     * worker threads only process event-style asynchronous tasks.
     * For default value, use {@code false}.
     * @throws IllegalArgumentException if parallelism less than or
     *         equal to zero, or greater than implementation limit
     * @throws NullPointerException if the factory is null
     * @throws SecurityException if a security manager exists and
     *         the caller is not permitted to modify threads
     *         because it does not hold {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")}
     */
    public ForkJoinPool(int parallelism,
                        ForkJoinWorkerThreadFactory factory,
                        UncaughtExceptionHandler handler,
                        boolean asyncMode) {
        this(parallelism, factory, handler, asyncMode,
             0, MAX_CAP, 1, null, DEFAULT_KEEPALIVE, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates a {@code ForkJoinPool} with the given parameters.
     *
     * @param parallelism the parallelism level. For default value,
     * use {@link java.lang.Runtime#availableProcessors}.
     *
     * @param factory the factory for creating new threads. For
     * default value, use {@link #defaultForkJoinWorkerThreadFactory}.
     *
     * @param handler the handler for internal worker threads that
     * terminate due to unrecoverable errors encountered while
     * executing tasks. For default value, use {@code null}.
     *
     * @param asyncMode if true, establishes local first-in-first-out
     * scheduling mode for forked tasks that are never joined. This
     * mode may be more appropriate than default locally stack-based
     * mode in applications in which worker threads only process
     * event-style asynchronous tasks.  For default value, use {@code
     * false}.
     *
     * @param corePoolSize the number of threads to keep in the pool
     * (unless timed out after an elapsed keep-alive). Normally (and
     * by default) this is the same value as the parallelism level,
     * but may be set to a larger value to reduce dynamic overhead if
     * tasks regularly block. Using a smaller value (for example
     * {@code 0}) has the same effect as the default.
     *
     * @param maximumPoolSize the maximum number of threads allowed.
     * When the maximum is reached, attempts to replace blocked
     * threads fail.  (However, because creation and termination of
     * different threads may overlap, and may be managed by the given
     * thread factory, this value may be transiently exceeded.)  To
     * arrange the same value as is used by default for the common
     * pool, use {@code 256} plus the {@code parallelism} level. (By
     * default, the common pool allows a maximum of 256 spare
     * threads.)  Using a value (for example {@code
     * Integer.MAX_VALUE}) larger than the implementation's total
     * thread limit has the same effect as using this limit (which is
     * the default).
     *
     * @param minimumRunnable the minimum allowed number of core
     * threads not blocked by a join or {@link ManagedBlocker}.  To
     * ensure progress, when too few unblocked threads exist and
     * unexecuted tasks may exist, new threads are constructed, up to
     * the given maximumPoolSize.  For the default value, use {@code
     * 1}, that ensures liveness.  A larger value might improve
     * throughput in the presence of blocked activities, but might
     * not, due to increased overhead.  A value of zero may be
     * acceptable when submitted tasks cannot have dependencies
     * requiring additional threads.
     *
     * @param saturate if non-null, a predicate invoked upon attempts
     * to create more than the maximum total allowed threads.  By
     * default, when a thread is about to block on a join or {@link
     * ManagedBlocker}, but cannot be replaced because the
     * maximumPoolSize would be exceeded, a {@link
     * RejectedExecutionException} is thrown.  But if this predicate
     * returns {@code true}, then no exception is thrown, so the pool
     * continues to operate with fewer than the target number of
     * runnable threads, which might not ensure progress.
     *
     * @param keepAliveTime the elapsed time since last use before
     * a thread is terminated (and then later replaced if needed).
     * For the default value, use {@code 60, TimeUnit.SECONDS}.
     *
     * @param unit the time unit for the {@code keepAliveTime} argument
     *
     * @throws IllegalArgumentException if parallelism is less than or
     *         equal to zero, or is greater than implementation limit,
     *         or if maximumPoolSize is less than parallelism,
     *         of if the keepAliveTime is less than or equal to zero.
     * @throws NullPointerException if the factory is null
     * @throws SecurityException if a security manager exists and
     *         the caller is not permitted to modify threads
     *         because it does not hold {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")}
     * @since 9
     */
    public ForkJoinPool(int parallelism,
                        ForkJoinWorkerThreadFactory factory,
                        UncaughtExceptionHandler handler,
                        boolean asyncMode,
                        int corePoolSize,
                        int maximumPoolSize,
                        int minimumRunnable,
                        Predicate<? super ForkJoinPool> saturate,
                        long keepAliveTime,
                        TimeUnit unit) {
        checkPermission();
        int p = parallelism;
        if (p <= 0 || p > MAX_CAP || p > maximumPoolSize || keepAliveTime <= 0L)
            throw new IllegalArgumentException();
        if (factory == null || unit == null)
            throw new NullPointerException();
        this.factory = factory;
        this.ueh = handler;
        this.saturate = saturate;
        this.keepAlive = Math.max(unit.toMillis(keepAliveTime), TIMEOUT_SLOP);
        int size = 1 << (33 - Integer.numberOfLeadingZeros(p - 1));
        int corep = Math.min(Math.max(corePoolSize, p), MAX_CAP);
        int maxSpares = Math.min(maximumPoolSize, MAX_CAP) - p;
        int minAvail = Math.min(Math.max(minimumRunnable, 0), MAX_CAP);
        this.bounds = ((minAvail - p) & SMASK) | (maxSpares << SWIDTH);
        this.mode = p | (asyncMode ? FIFO : 0);
        this.ctl = ((((long)(-corep) << TC_SHIFT) & TC_MASK) |
                    (((long)(-p)     << RC_SHIFT) & RC_MASK));
        this.registrationLock = new ReentrantLock();
        this.queues = new WorkQueue[size];
        String pid = Integer.toString(getAndAddPoolIds(1) + 1);
        this.workerNamePrefix = "ForkJoinPool-" + pid + "-worker-";
    }

    // helper method for commonPool constructor
    private static Object newInstanceFromSystemProperty(String property)
        throws ReflectiveOperationException {
        String className = System.getProperty(property);
        return (className == null)
            ? null
            : ClassLoader.getSystemClassLoader().loadClass(className)
            .getConstructor().newInstance();
    }

    /**
     * Constructor for common pool using parameters possibly
     * overridden by system properties
     */
    private ForkJoinPool(byte forCommonPoolOnly) {
        int parallelism = Runtime.getRuntime().availableProcessors() - 1;
        ForkJoinWorkerThreadFactory fac = null;
        UncaughtExceptionHandler handler = null;
        try {  // ignore exceptions in accessing/parsing properties
            fac = (ForkJoinWorkerThreadFactory) newInstanceFromSystemProperty(
                "java.util.concurrent.ForkJoinPool.common.threadFactory");
            handler = (UncaughtExceptionHandler) newInstanceFromSystemProperty(
                "java.util.concurrent.ForkJoinPool.common.exceptionHandler");
            String pp = System.getProperty
                ("java.util.concurrent.ForkJoinPool.common.parallelism");
            if (pp != null)
                parallelism = Integer.parseInt(pp);
        } catch (Exception ignore) {
        }
        this.ueh = handler;
        this.keepAlive = DEFAULT_KEEPALIVE;
        this.saturate = null;
        this.workerNamePrefix = null;
        int p = Math.min(Math.max(parallelism, 0), MAX_CAP), size;
        this.mode = p;
        if (p > 0) {
            size = 1 << (33 - Integer.numberOfLeadingZeros(p - 1));
            this.bounds = ((1 - p) & SMASK) | (COMMON_MAX_SPARES << SWIDTH);
            this.ctl = ((((long)(-p) << TC_SHIFT) & TC_MASK) |
                        (((long)(-p) << RC_SHIFT) & RC_MASK));
        } else {  // zero min, max, spare counts, 1 slot
            size = 1;
            this.bounds = 0;
            this.ctl = 0L;
        }
        this.factory = (fac != null) ? fac :
            new DefaultCommonPoolForkJoinWorkerThreadFactory();
        this.queues = new WorkQueue[size];
        this.registrationLock = new ReentrantLock();
    }

    /**
     * Returns the common pool instance. This pool is statically
     * constructed; its run state is unaffected by attempts to {@link
     * #shutdown} or {@link #shutdownNow}. However this pool and any
     * ongoing processing are automatically terminated upon program
     * {@link System#exit}.  Any program that relies on asynchronous
     * task processing to complete before program termination should
     * invoke {@code commonPool().}{@link #awaitQuiescence awaitQuiescence},
     * before exit.
     *
     * @return the common pool instance
     * @since 1.8
     */
    public static ForkJoinPool commonPool() {
        // assert common != null : "static init error";
        return common;
    }

    // Execution methods

    /**
     * Performs the given task, returning its result upon completion.
     * If the computation encounters an unchecked Exception or Error,
     * it is rethrown as the outcome of this invocation.  Rethrown
     * exceptions behave in the same way as regular exceptions, but,
     * when possible, contain stack traces (as displayed for example
     * using {@code ex.printStackTrace()}) of both the current thread
     * as well as the thread actually encountering the exception;
     * minimally only the latter.
     *
     * @param task the task
     * @param <T> the type of the task's result
     * @return the task's result
     * @throws NullPointerException if the task is null
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     */
    public <T> T invoke(ForkJoinTask<T> task) {
        externalSubmit(task);
        return task.joinForPoolInvoke(this);
    }

    /**
     * Arranges for (asynchronous) execution of the given task.
     *
     * @param task the task
     * @throws NullPointerException if the task is null
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     */
    public void execute(ForkJoinTask<?> task) {
        externalSubmit(task);
    }

    // AbstractExecutorService methods

    /**
     * @throws NullPointerException if the task is null
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     */
    @Override
    @SuppressWarnings("unchecked")
    public void execute(Runnable task) {
        externalSubmit((task instanceof ForkJoinTask<?>)
                       ? (ForkJoinTask<Void>) task // avoid re-wrap
                       : new ForkJoinTask.RunnableExecuteAction(task));
    }

    /**
     * Submits a ForkJoinTask for execution.
     *
     * @param task the task to submit
     * @param <T> the type of the task's result
     * @return the task
     * @throws NullPointerException if the task is null
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     */
    public <T> ForkJoinTask<T> submit(ForkJoinTask<T> task) {
        return externalSubmit(task);
    }

    /**
     * @throws NullPointerException if the task is null
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     */
    @Override
    public <T> ForkJoinTask<T> submit(Callable<T> task) {
        return externalSubmit(new ForkJoinTask.AdaptedCallable<T>(task));
    }

    /**
     * @throws NullPointerException if the task is null
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     */
    @Override
    public <T> ForkJoinTask<T> submit(Runnable task, T result) {
        return externalSubmit(new ForkJoinTask.AdaptedRunnable<T>(task, result));
    }

    /**
     * @throws NullPointerException if the task is null
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     */
    @Override
    @SuppressWarnings("unchecked")
    public ForkJoinTask<?> submit(Runnable task) {
        return externalSubmit((task instanceof ForkJoinTask<?>)
            ? (ForkJoinTask<Void>) task // avoid re-wrap
            : new ForkJoinTask.AdaptedRunnableAction(task));
    }

    /**
     * @throws NullPointerException       {@inheritDoc}
     * @throws RejectedExecutionException {@inheritDoc}
     */
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
        ArrayList<Future<T>> futures = new ArrayList<>(tasks.size());
        try {
            for (Callable<T> t : tasks) {
                ForkJoinTask<T> f =
                    new ForkJoinTask.AdaptedInterruptibleCallable<T>(t);
                futures.add(f);
                externalSubmit(f);
            }
            for (int i = futures.size() - 1; i >= 0; --i)
                ((ForkJoinTask<?>)futures.get(i)).awaitPoolInvoke(this);
            return futures;
        } catch (Throwable t) {
            for (Future<T> e : futures)
                ForkJoinTask.cancelIgnoringExceptions(e);
            throw t;
        }
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                         long timeout, TimeUnit unit)
        throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        ArrayList<Future<T>> futures = new ArrayList<>(tasks.size());
        try {
            for (Callable<T> t : tasks) {
                ForkJoinTask<T> f =
                    new ForkJoinTask.AdaptedInterruptibleCallable<T>(t);
                futures.add(f);
                externalSubmit(f);
            }
            long startTime = System.nanoTime(), ns = nanos;
            boolean timedOut = (ns < 0L);
            for (int i = futures.size() - 1; i >= 0; --i) {
                Future<T> f = futures.get(i);
                if (!f.isDone()) {
                    if (timedOut)
                        ForkJoinTask.cancelIgnoringExceptions(f);
                    else {
                        ((ForkJoinTask<T>)f).awaitPoolInvoke(this, ns);
                        if ((ns = nanos - (System.nanoTime() - startTime)) < 0L)
                            timedOut = true;
                    }
                }
            }
            return futures;
        } catch (Throwable t) {
            for (Future<T> e : futures)
                ForkJoinTask.cancelIgnoringExceptions(e);
            throw t;
        }
    }

    // Task to hold results from InvokeAnyTasks
    static final class InvokeAnyRoot<E> extends ForkJoinTask<E> {
        private static final long serialVersionUID = 2838392045355241008L;
        @SuppressWarnings("serial") // Conditionally serializable
        volatile E result;
        final AtomicInteger count;  // in case all throw
        final ForkJoinPool pool;    // to check shutdown while collecting
        InvokeAnyRoot(int n, ForkJoinPool p) {
            pool = p;
            count = new AtomicInteger(n);
        }
        final void tryComplete(Callable<E> c) { // called by InvokeAnyTasks
            Throwable ex = null;
            boolean failed;
            if (c == null || Thread.interrupted() ||
                (pool != null && pool.mode < 0))
                failed = true;
            else if (isDone())
                failed = false;
            else {
                try {
                    complete(c.call());
                    failed = false;
                } catch (Throwable tx) {
                    ex = tx;
                    failed = true;
                }
            }
            if ((pool != null && pool.mode < 0) ||
                (failed && count.getAndDecrement() <= 1))
                trySetThrown(ex != null ? ex : new CancellationException());
        }
        public final boolean exec()         { return false; } // never forked
        public final E getRawResult()       { return result; }
        public final void setRawResult(E v) { result = v; }
    }

    // Variant of AdaptedInterruptibleCallable with results in InvokeAnyRoot
    static final class InvokeAnyTask<E> extends ForkJoinTask<E> {
        private static final long serialVersionUID = 2838392045355241008L;
        final InvokeAnyRoot<E> root;
        @SuppressWarnings("serial") // Conditionally serializable
        final Callable<E> callable;
        transient volatile Thread runner;
        InvokeAnyTask(InvokeAnyRoot<E> root, Callable<E> callable) {
            this.root = root;
            this.callable = callable;
        }
        public final boolean exec() {
            Thread.interrupted();
            runner = Thread.currentThread();
            root.tryComplete(callable);
            runner = null;
            Thread.interrupted();
            return true;
        }
        public final boolean cancel(boolean mayInterruptIfRunning) {
            Thread t;
            boolean stat = super.cancel(false);
            if (mayInterruptIfRunning && (t = runner) != null) {
                try {
                    t.interrupt();
                } catch (Throwable ignore) {
                }
            }
            return stat;
        }
        public final void setRawResult(E v) {} // unused
        public final E getRawResult()       { return null; }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException {
        int n = tasks.size();
        if (n <= 0)
            throw new IllegalArgumentException();
        InvokeAnyRoot<T> root = new InvokeAnyRoot<T>(n, this);
        ArrayList<InvokeAnyTask<T>> fs = new ArrayList<>(n);
        try {
            for (Callable<T> c : tasks) {
                if (c == null)
                    throw new NullPointerException();
                InvokeAnyTask<T> f = new InvokeAnyTask<T>(root, c);
                fs.add(f);
                externalSubmit(f);
                if (root.isDone())
                    break;
            }
            return root.getForPoolInvoke(this);
        } finally {
            for (InvokeAnyTask<T> f : fs)
                ForkJoinTask.cancelIgnoringExceptions(f);
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                           long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        long nanos = unit.toNanos(timeout);
        int n = tasks.size();
        if (n <= 0)
            throw new IllegalArgumentException();
        InvokeAnyRoot<T> root = new InvokeAnyRoot<T>(n, this);
        ArrayList<InvokeAnyTask<T>> fs = new ArrayList<>(n);
        try {
            for (Callable<T> c : tasks) {
                if (c == null)
                    throw new NullPointerException();
                InvokeAnyTask<T> f = new InvokeAnyTask<T>(root, c);
                fs.add(f);
                externalSubmit(f);
                if (root.isDone())
                    break;
            }
            return root.getForPoolInvoke(this, nanos);
        } finally {
            for (InvokeAnyTask<T> f : fs)
                ForkJoinTask.cancelIgnoringExceptions(f);
        }
    }

    /**
     * Returns the factory used for constructing new workers.
     *
     * @return the factory used for constructing new workers
     */
    public ForkJoinWorkerThreadFactory getFactory() {
        return factory;
    }

    /**
     * Returns the handler for internal worker threads that terminate
     * due to unrecoverable errors encountered while executing tasks.
     *
     * @return the handler, or {@code null} if none
     */
    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return ueh;
    }

    /**
     * Returns the targeted parallelism level of this pool.
     *
     * @return the targeted parallelism level of this pool
     */
    public int getParallelism() {
        int par = mode & SMASK;
        return (par > 0) ? par : 1;
    }

    /**
     * Returns the targeted parallelism level of the common pool.
     *
     * @return the targeted parallelism level of the common pool
     * @since 1.8
     */
    public static int getCommonPoolParallelism() {
        return COMMON_PARALLELISM;
    }

    /**
     * Returns the number of worker threads that have started but not
     * yet terminated.  The result returned by this method may differ
     * from {@link #getParallelism} when threads are created to
     * maintain parallelism when others are cooperatively blocked.
     *
     * @return the number of worker threads
     */
    public int getPoolSize() {
        return ((mode & SMASK) + (short)(ctl >>> TC_SHIFT));
    }

    /**
     * Returns {@code true} if this pool uses local first-in-first-out
     * scheduling mode for forked tasks that are never joined.
     *
     * @return {@code true} if this pool uses async mode
     */
    public boolean getAsyncMode() {
        return (mode & FIFO) != 0;
    }

    /**
     * Returns an estimate of the number of worker threads that are
     * not blocked waiting to join tasks or for other managed
     * synchronization. This method may overestimate the
     * number of running threads.
     *
     * @return the number of worker threads
     */
    public int getRunningThreadCount() {
        VarHandle.acquireFence();
        WorkQueue[] qs; WorkQueue q;
        int rc = 0;
        if ((qs = queues) != null) {
            for (int i = 1; i < qs.length; i += 2) {
                if ((q = qs[i]) != null && q.isApparentlyUnblocked())
                    ++rc;
            }
        }
        return rc;
    }

    /**
     * Returns an estimate of the number of threads that are currently
     * stealing or executing tasks. This method may overestimate the
     * number of active threads.
     *
     * @return the number of active threads
     */
    public int getActiveThreadCount() {
        int r = (mode & SMASK) + (int)(ctl >> RC_SHIFT);
        return (r <= 0) ? 0 : r; // suppress momentarily negative values
    }

    /**
     * Returns {@code true} if all worker threads are currently idle.
     * An idle worker is one that cannot obtain a task to execute
     * because none are available to steal from other threads, and
     * there are no pending submissions to the pool. This method is
     * conservative; it might not return {@code true} immediately upon
     * idleness of all threads, but will eventually become true if
     * threads remain inactive.
     *
     * @return {@code true} if all threads are currently idle
     */
    public boolean isQuiescent() {
        return canStop();
    }

    /**
     * Returns an estimate of the total number of completed tasks that
     * were executed by a thread other than their submitter. The
     * reported value underestimates the actual total number of steals
     * when the pool is not quiescent. This value may be useful for
     * monitoring and tuning fork/join programs: in general, steal
     * counts should be high enough to keep threads busy, but low
     * enough to avoid overhead and contention across threads.
     *
     * @return the number of steals
     */
    public long getStealCount() {
        long count = stealCount;
        WorkQueue[] qs; WorkQueue q;
        if ((qs = queues) != null) {
            for (int i = 1; i < qs.length; i += 2) {
                if ((q = qs[i]) != null)
                    count += (long)q.nsteals & 0xffffffffL;
            }
        }
        return count;
    }

    /**
     * Returns an estimate of the total number of tasks currently held
     * in queues by worker threads (but not including tasks submitted
     * to the pool that have not begun executing). This value is only
     * an approximation, obtained by iterating across all threads in
     * the pool. This method may be useful for tuning task
     * granularities.
     *
     * @return the number of queued tasks
     */
    public long getQueuedTaskCount() {
        VarHandle.acquireFence();
        WorkQueue[] qs; WorkQueue q;
        int count = 0;
        if ((qs = queues) != null) {
            for (int i = 1; i < qs.length; i += 2) {
                if ((q = qs[i]) != null)
                    count += q.queueSize();
            }
        }
        return count;
    }

    /**
     * Returns an estimate of the number of tasks submitted to this
     * pool that have not yet begun executing.  This method may take
     * time proportional to the number of submissions.
     *
     * @return the number of queued submissions
     */
    public int getQueuedSubmissionCount() {
        VarHandle.acquireFence();
        WorkQueue[] qs; WorkQueue q;
        int count = 0;
        if ((qs = queues) != null) {
            for (int i = 0; i < qs.length; i += 2) {
                if ((q = qs[i]) != null)
                    count += q.queueSize();
            }
        }
        return count;
    }

    /**
     * Returns {@code true} if there are any tasks submitted to this
     * pool that have not yet begun executing.
     *
     * @return {@code true} if there are any queued submissions
     */
    public boolean hasQueuedSubmissions() {
        VarHandle.acquireFence();
        WorkQueue[] qs; WorkQueue q;
        if ((qs = queues) != null) {
            for (int i = 0; i < qs.length; i += 2) {
                if ((q = qs[i]) != null && !q.isEmpty())
                    return true;
            }
        }
        return false;
    }

    /**
     * Removes and returns the next unexecuted submission if one is
     * available.  This method may be useful in extensions to this
     * class that re-assign work in systems with multiple pools.
     *
     * @return the next submission, or {@code null} if none
     */
    protected ForkJoinTask<?> pollSubmission() {
        return pollScan(true);
    }

    /**
     * Removes all available unexecuted submitted and forked tasks
     * from scheduling queues and adds them to the given collection,
     * without altering their execution status. These may include
     * artificially generated or wrapped tasks. This method is
     * designed to be invoked only when the pool is known to be
     * quiescent. Invocations at other times may not remove all
     * tasks. A failure encountered while attempting to add elements
     * to collection {@code c} may result in elements being in
     * neither, either or both collections when the associated
     * exception is thrown.  The behavior of this operation is
     * undefined if the specified collection is modified while the
     * operation is in progress.
     *
     * @param c the collection to transfer elements into
     * @return the number of elements transferred
     */
    protected int drainTasksTo(Collection<? super ForkJoinTask<?>> c) {
        int count = 0;
        for (ForkJoinTask<?> t; (t = pollScan(false)) != null; ) {
            c.add(t);
            ++count;
        }
        return count;
    }

    /**
     * Returns a string identifying this pool, as well as its state,
     * including indications of run state, parallelism level, and
     * worker and task counts.
     *
     * @return a string identifying this pool, as well as its state
     */
    public String toString() {
        // Use a single pass through queues to collect counts
        int md = mode; // read volatile fields first
        long c = ctl;
        long st = stealCount;
        long qt = 0L, ss = 0L; int rc = 0;
        WorkQueue[] qs; WorkQueue q;
        if ((qs = queues) != null) {
            for (int i = 0; i < qs.length; ++i) {
                if ((q = qs[i]) != null) {
                    int size = q.queueSize();
                    if ((i & 1) == 0)
                        ss += size;
                    else {
                        qt += size;
                        st += (long)q.nsteals & 0xffffffffL;
                        if (q.isApparentlyUnblocked())
                            ++rc;
                    }
                }
            }
        }

        int pc = (md & SMASK);
        int tc = pc + (short)(c >>> TC_SHIFT);
        int ac = pc + (int)(c >> RC_SHIFT);
        if (ac < 0) // ignore transient negative
            ac = 0;
        String level = ((md & TERMINATED) != 0 ? "Terminated" :
                        (md & STOP)       != 0 ? "Terminating" :
                        (md & SHUTDOWN)   != 0 ? "Shutting down" :
                        "Running");
        return super.toString() +
            "[" + level +
            ", parallelism = " + pc +
            ", size = " + tc +
            ", active = " + ac +
            ", running = " + rc +
            ", steals = " + st +
            ", tasks = " + qt +
            ", submissions = " + ss +
            "]";
    }

    /**
     * Possibly initiates an orderly shutdown in which previously
     * submitted tasks are executed, but no new tasks will be
     * accepted. Invocation has no effect on execution state if this
     * is the {@link #commonPool()}, and no additional effect if
     * already shut down.  Tasks that are in the process of being
     * submitted concurrently during the course of this method may or
     * may not be rejected.
     *
     * @throws SecurityException if a security manager exists and
     *         the caller is not permitted to modify threads
     *         because it does not hold {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")}
     */
    public void shutdown() {
        checkPermission();
        if (this != common)
            tryTerminate(false, true);
    }

    /**
     * Possibly attempts to cancel and/or stop all tasks, and reject
     * all subsequently submitted tasks.  Invocation has no effect on
     * execution state if this is the {@link #commonPool()}, and no
     * additional effect if already shut down. Otherwise, tasks that
     * are in the process of being submitted or executed concurrently
     * during the course of this method may or may not be
     * rejected. This method cancels both existing and unexecuted
     * tasks, in order to permit termination in the presence of task
     * dependencies. So the method always returns an empty list
     * (unlike the case for some other Executors).
     *
     * @return an empty list
     * @throws SecurityException if a security manager exists and
     *         the caller is not permitted to modify threads
     *         because it does not hold {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")}
     */
    public List<Runnable> shutdownNow() {
        checkPermission();
        if (this != common)
            tryTerminate(true, true);
        return Collections.emptyList();
    }

    /**
     * Returns {@code true} if all tasks have completed following shut down.
     *
     * @return {@code true} if all tasks have completed following shut down
     */
    public boolean isTerminated() {
        return (mode & TERMINATED) != 0;
    }

    /**
     * Returns {@code true} if the process of termination has
     * commenced but not yet completed.  This method may be useful for
     * debugging. A return of {@code true} reported a sufficient
     * period after shutdown may indicate that submitted tasks have
     * ignored or suppressed interruption, or are waiting for I/O,
     * causing this executor not to properly terminate. (See the
     * advisory notes for class {@link ForkJoinTask} stating that
     * tasks should not normally entail blocking operations.  But if
     * they do, they must abort them on interrupt.)
     *
     * @return {@code true} if terminating but not yet terminated
     */
    public boolean isTerminating() {
        return (mode & (STOP | TERMINATED)) == STOP;
    }

    /**
     * Returns {@code true} if this pool has been shut down.
     *
     * @return {@code true} if this pool has been shut down
     */
    public boolean isShutdown() {
        return (mode & SHUTDOWN) != 0;
    }

    /**
     * Blocks until all tasks have completed execution after a
     * shutdown request, or the timeout occurs, or the current thread
     * is interrupted, whichever happens first. Because the {@link
     * #commonPool()} never terminates until program shutdown, when
     * applied to the common pool, this method is equivalent to {@link
     * #awaitQuiescence(long, TimeUnit)} but always returns {@code false}.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return {@code true} if this executor terminated and
     *         {@code false} if the timeout elapsed before termination
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException {
        ReentrantLock lock; Condition cond;
        long nanos = unit.toNanos(timeout);
        boolean terminated = false;
        if (this == common) {
            Thread t; ForkJoinWorkerThread wt; int q;
            if ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread &&
                (wt = (ForkJoinWorkerThread)t).pool == this)
                q = helpQuiescePool(wt.workQueue, nanos, true);
            else
                q = externalHelpQuiescePool(nanos, true);
            if (q < 0)
                throw new InterruptedException();
        }
        else if (!(terminated = ((mode & TERMINATED) != 0)) &&
                 (lock = registrationLock) != null) {
            lock.lock();
            try {
                if ((cond = termination) == null)
                    termination = cond = lock.newCondition();
                while (!(terminated = ((mode & TERMINATED) != 0)) && nanos > 0L)
                    nanos = cond.awaitNanos(nanos);
            } finally {
                lock.unlock();
            }
        }
        return terminated;
    }

    /**
     * If called by a ForkJoinTask operating in this pool, equivalent
     * in effect to {@link ForkJoinTask#helpQuiesce}. Otherwise,
     * waits and/or attempts to assist performing tasks until this
     * pool {@link #isQuiescent} or the indicated timeout elapses.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return {@code true} if quiescent; {@code false} if the
     * timeout elapsed.
     */
    public boolean awaitQuiescence(long timeout, TimeUnit unit) {
        Thread t; ForkJoinWorkerThread wt; int q;
        long nanos = unit.toNanos(timeout);
        if ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread &&
            (wt = (ForkJoinWorkerThread)t).pool == this)
            q = helpQuiescePool(wt.workQueue, nanos, false);
        else
            q = externalHelpQuiescePool(nanos, false);
        return (q > 0);
    }

    /**
     * Interface for extending managed parallelism for tasks running
     * in {@link ForkJoinPool}s.
     *
     * <p>A {@code ManagedBlocker} provides two methods.  Method
     * {@link #isReleasable} must return {@code true} if blocking is
     * not necessary. Method {@link #block} blocks the current thread
     * if necessary (perhaps internally invoking {@code isReleasable}
     * before actually blocking). These actions are performed by any
     * thread invoking {@link
     * ForkJoinPool#managedBlock(ManagedBlocker)}.  The unusual
     * methods in this API accommodate synchronizers that may, but
     * don't usually, block for long periods. Similarly, they allow
     * more efficient internal handling of cases in which additional
     * workers may be, but usually are not, needed to ensure
     * sufficient parallelism.  Toward this end, implementations of
     * method {@code isReleasable} must be amenable to repeated
     * invocation. Neither method is invoked after a prior invocation
     * of {@code isReleasable} or {@code block} returns {@code true}.
     *
     * <p>For example, here is a ManagedBlocker based on a
     * ReentrantLock:
     * <pre> {@code
     * class ManagedLocker implements ManagedBlocker {
     *   final ReentrantLock lock;
     *   boolean hasLock = false;
     *   ManagedLocker(ReentrantLock lock) { this.lock = lock; }
     *   public boolean block() {
     *     if (!hasLock)
     *       lock.lock();
     *     return true;
     *   }
     *   public boolean isReleasable() {
     *     return hasLock || (hasLock = lock.tryLock());
     *   }
     * }}</pre>
     *
     * <p>Here is a class that possibly blocks waiting for an
     * item on a given queue:
     * <pre> {@code
     * class QueueTaker<E> implements ManagedBlocker {
     *   final BlockingQueue<E> queue;
     *   volatile E item = null;
     *   QueueTaker(BlockingQueue<E> q) { this.queue = q; }
     *   public boolean block() throws InterruptedException {
     *     if (item == null)
     *       item = queue.take();
     *     return true;
     *   }
     *   public boolean isReleasable() {
     *     return item != null || (item = queue.poll()) != null;
     *   }
     *   public E getItem() { // call after pool.managedBlock completes
     *     return item;
     *   }
     * }}</pre>
     */
    public static interface ManagedBlocker {
        /**
         * Possibly blocks the current thread, for example waiting for
         * a lock or condition.
         *
         * @return {@code true} if no additional blocking is necessary
         * (i.e., if isReleasable would return true)
         * @throws InterruptedException if interrupted while waiting
         * (the method is not required to do so, but is allowed to)
         */
        boolean block() throws InterruptedException;

        /**
         * Returns {@code true} if blocking is unnecessary.
         * @return {@code true} if blocking is unnecessary
         */
        boolean isReleasable();
    }

    /**
     * Runs the given possibly blocking task.  When {@linkplain
     * ForkJoinTask#inForkJoinPool() running in a ForkJoinPool}, this
     * method possibly arranges for a spare thread to be activated if
     * necessary to ensure sufficient parallelism while the current
     * thread is blocked in {@link ManagedBlocker#block blocker.block()}.
     *
     * <p>This method repeatedly calls {@code blocker.isReleasable()} and
     * {@code blocker.block()} until either method returns {@code true}.
     * Every call to {@code blocker.block()} is preceded by a call to
     * {@code blocker.isReleasable()} that returned {@code false}.
     *
     * <p>If not running in a ForkJoinPool, this method is
     * behaviorally equivalent to
     * <pre> {@code
     * while (!blocker.isReleasable())
     *   if (blocker.block())
     *     break;}</pre>
     *
     * If running in a ForkJoinPool, the pool may first be expanded to
     * ensure sufficient parallelism available during the call to
     * {@code blocker.block()}.
     *
     * @param blocker the blocker task
     * @throws InterruptedException if {@code blocker.block()} did so
     */
    public static void managedBlock(ManagedBlocker blocker)
        throws InterruptedException {
        Thread t; ForkJoinPool p;
        if ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread &&
            (p = ((ForkJoinWorkerThread)t).pool) != null)
            p.compensatedBlock(blocker);
        else
            unmanagedBlock(blocker);
    }

    /** ManagedBlock for ForkJoinWorkerThreads */
    private void compensatedBlock(ManagedBlocker blocker)
        throws InterruptedException {
        if (blocker == null) throw new NullPointerException();
        for (;;) {
            int comp; boolean done;
            long c = ctl;
            if (blocker.isReleasable())
                break;
            if ((comp = tryCompensate(c)) >= 0) {
                long post = (comp == 0) ? 0L : RC_UNIT;
                try {
                    done = blocker.block();
                } finally {
                    getAndAddCtl(post);
                }
                if (done)
                    break;
            }
        }
    }

    /** ManagedBlock for external threads */
    private static void unmanagedBlock(ManagedBlocker blocker)
        throws InterruptedException {
        if (blocker == null) throw new NullPointerException();
        do {} while (!blocker.isReleasable() && !blocker.block());
    }

    // AbstractExecutorService.newTaskFor overrides rely on
    // undocumented fact that ForkJoinTask.adapt returns ForkJoinTasks
    // that also implement RunnableFuture.

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new ForkJoinTask.AdaptedRunnable<T>(runnable, value);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new ForkJoinTask.AdaptedCallable<T>(callable);
    }

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            CTL = l.findVarHandle(ForkJoinPool.class, "ctl", long.class);
            MODE = l.findVarHandle(ForkJoinPool.class, "mode", int.class);
            THREADIDS = l.findVarHandle(ForkJoinPool.class, "threadIds", int.class);
            POOLIDS = l.findStaticVarHandle(ForkJoinPool.class, "poolIds", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }

        // Reduce the risk of rare disastrous classloading in first call to
        // LockSupport.park: https://bugs.openjdk.java.net/browse/JDK-8074773
        Class<?> ensureLoaded = LockSupport.class;

        int commonMaxSpares = DEFAULT_COMMON_MAX_SPARES;
        try {
            String p = System.getProperty
                ("java.util.concurrent.ForkJoinPool.common.maximumSpares");
            if (p != null)
                commonMaxSpares = Integer.parseInt(p);
        } catch (Exception ignore) {}
        COMMON_MAX_SPARES = commonMaxSpares;

        defaultForkJoinWorkerThreadFactory =
            new DefaultForkJoinWorkerThreadFactory();
        modifyThreadPermission = new RuntimePermission("modifyThread");
        @SuppressWarnings("removal")
        ForkJoinPool tmp = AccessController.doPrivileged(new PrivilegedAction<>() {
            public ForkJoinPool run() {
                return new ForkJoinPool((byte)0); }});
        common = tmp;

        COMMON_PARALLELISM = Math.max(common.mode & SMASK, 1);
    }
}
