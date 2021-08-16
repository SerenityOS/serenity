/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http.common;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Objects.requireNonNull;

/**
 * A scheduler of ( repeatable ) tasks that MUST be run sequentially.
 *
 * <p> This class can be used as a synchronization aid that assists a number of
 * parties in running a task in a mutually exclusive fashion.
 *
 * <p> To run the task, a party invokes {@code runOrSchedule}. To permanently
 * prevent the task from subsequent runs, the party invokes {@code stop}.
 *
 * <p> The parties can, but do not have to, operate in different threads.
 *
 * <p> The task can be either synchronous ( completes when its {@code run}
 * method returns ), or asynchronous ( completed when its
 * {@code DeferredCompleter} is explicitly completed ).
 *
 * <p> The next run of the task will not begin until the previous run has
 * finished.
 *
 * <p> The task may invoke {@code runOrSchedule} itself, which may be a normal
 * situation.
 */
public final class SequentialScheduler {

    /*
       Since the task is fixed and known beforehand, no blocking synchronization
       (locks, queues, etc.) is required. The job can be done solely using
       nonblocking primitives.

       The machinery below addresses two problems:

         1. Running the task in a sequential order (no concurrent runs):

                begin, end, begin, end...

         2. Avoiding indefinite recursion:

                begin
                  end
                    begin
                      end
                        ...

       Problem #1 is solved with a finite state machine with 4 states:

           BEGIN, AGAIN, END, and STOP.

       Problem #2 is solved with a "state modifier" OFFLOAD.

       Parties invoke `runOrSchedule()` to signal the task must run. A party
       that has invoked `runOrSchedule()` either begins the task or exploits the
       party that is either beginning the task or ending it.

       The party that is trying to end the task either ends it or begins it
       again.

       To avoid indefinite recursion, before re-running the task the
       TryEndDeferredCompleter sets the OFFLOAD bit, signalling to its "child"
       TryEndDeferredCompleter that this ("parent") TryEndDeferredCompleter is
       available and the "child" must offload the task on to the "parent". Then
       a race begins. Whichever invocation of TryEndDeferredCompleter.complete
       manages to unset OFFLOAD bit first does not do the work.

       There is at most 1 thread that is beginning the task and at most 2
       threads that are trying to end it: "parent" and "child". In case of a
       synchronous task "parent" and "child" are the same thread.
     */

    /**
     * An interface to signal the completion of a {@link RestartableTask}.
     *
     * <p> The invocation of {@code complete} completes the task. The invocation
     * of {@code complete} may restart the task, if an attempt has previously
     * been made to run the task while it was already running.
     *
     * @apiNote {@code DeferredCompleter} is useful when a task is not necessary
     * complete when its {@code run} method returns, but will complete at a
     * later time, and maybe in different thread. This type exists for
     * readability purposes at use-sites only.
     */
    public static abstract class DeferredCompleter {

        /** Extensible from this (outer) class ONLY. */
        private DeferredCompleter() { }

        /** Completes the task. Must be called once, and once only. */
        public abstract void complete();
    }

    /**
     * A restartable task.
     */
    @FunctionalInterface
    public interface RestartableTask {

        /**
         * The body of the task.
         *
         * @param taskCompleter
         *         A completer that must be invoked once, and only once,
         *         when this task is logically finished
         */
        void run(DeferredCompleter taskCompleter);
    }

    /**
     * A simple and self-contained task that completes once its {@code run}
     * method returns.
     */
    public static abstract class CompleteRestartableTask
        implements RestartableTask
    {
        @Override
        public final void run(DeferredCompleter taskCompleter) {
            try {
                run();
            } finally {
                taskCompleter.complete();
            }
        }

        /** The body of the task. */
        protected abstract void run();
    }

    /**
     * A task that runs its main loop within a synchronized block to provide
     * memory visibility between runs. Since the main loop can't run concurrently,
     * the lock shouldn't be contended and no deadlock should ever be possible.
     */
    public static final class SynchronizedRestartableTask
            extends CompleteRestartableTask {

        private final Runnable mainLoop;
        private final Object lock = new Object();

        public SynchronizedRestartableTask(Runnable mainLoop) {
            this.mainLoop = mainLoop;
        }

        @Override
        protected void run() {
            synchronized(lock) {
                mainLoop.run();
            }
        }
    }

    /**
     * A task that runs its main loop within a  block protected by a lock to provide
     * memory visibility between runs. Since the main loop can't run concurrently,
     * the lock shouldn't be contended and no deadlock should ever be possible.
     */
    public static final class LockingRestartableTask
            extends CompleteRestartableTask {

        private final Runnable mainLoop;
        private final Lock lock = new ReentrantLock();

        public LockingRestartableTask(Runnable mainLoop) {
            this.mainLoop = mainLoop;
        }

        @Override
        protected void run() {
            // The logics of the sequential scheduler should ensure that
            // the restartable task is running in only one thread at
            // a given time: there should never be contention.
            boolean locked = lock.tryLock();
            assert locked : "contention detected in SequentialScheduler";
            try {
                mainLoop.run();
            } finally {
                if (locked) lock.unlock();
            }
        }
    }

    private static final int OFFLOAD =  1;
    private static final int AGAIN   =  2;
    private static final int BEGIN   =  4;
    private static final int STOP    =  8;
    private static final int END     = 16;

    private final AtomicInteger state = new AtomicInteger(END);
    private final RestartableTask restartableTask;
    private final DeferredCompleter completer;
    private final SchedulableTask schedulableTask;

    /**
     * An auxiliary task that starts the restartable task:
     * {@code restartableTask.run(completer)}.
     */
    private final class SchedulableTask implements Runnable {
        @Override
        public void run() {
            restartableTask.run(completer);
        }
    }

    public SequentialScheduler(RestartableTask restartableTask) {
        this.restartableTask = requireNonNull(restartableTask);
        this.completer = new TryEndDeferredCompleter();
        this.schedulableTask = new SchedulableTask();
    }

    /**
     * Runs or schedules the task to be run.
     *
     * @implSpec The recursion which is possible here must be bounded:
     *
     *  <pre>{@code
     *     this.runOrSchedule()
     *         completer.complete()
     *             this.runOrSchedule()
     *                 ...
     * }</pre>
     *
     * @implNote The recursion in this implementation has the maximum
     * depth of 1.
     */
    public void runOrSchedule() {
        runOrSchedule(schedulableTask, null);
    }

    /**
     * Executes or schedules the task to be executed in the provided executor.
     *
     * <p> This method can be used when potential executing from a calling
     * thread is not desirable.
     *
     * @param executor
     *         An executor in which to execute the task, if the task needs
     *         to be executed.
     *
     * @apiNote The given executor can be {@code null} in which case calling
     * {@code runOrSchedule(null)} is strictly equivalent to calling
     * {@code runOrSchedule()}.
     */
    public void runOrSchedule(Executor executor) {
        runOrSchedule(schedulableTask, executor);
    }

    private void runOrSchedule(SchedulableTask task, Executor executor) {
        while (true) {
            int s = state.get();
            if (s == END) {
                if (state.compareAndSet(END, BEGIN)) {
                    break;
                }
            } else if ((s & BEGIN) != 0) {
                // Tries to change the state to AGAIN, preserving OFFLOAD bit
                if (state.compareAndSet(s, AGAIN | (s & OFFLOAD))) {
                    return;
                }
            } else if ((s & AGAIN) != 0 || s == STOP) {
                /* In the case of AGAIN the scheduler does not provide
                   happens-before relationship between actions prior to
                   runOrSchedule() and actions that happen in task.run().
                   The reason is that no volatile write is done in this case,
                   and the call piggybacks on the call that has actually set
                   AGAIN state. */
                return;
            } else {
                // Non-existent state, or the one that cannot be offloaded
                throw new InternalError(String.valueOf(s));
            }
        }
        if (executor == null) {
            task.run();
        } else {
            executor.execute(task);
        }
    }

    /** The only concrete {@code DeferredCompleter} implementation. */
    private class TryEndDeferredCompleter extends DeferredCompleter {

        @Override
        public void complete() {
            while (true) {
                int s;
                while (((s = state.get()) & OFFLOAD) != 0) {
                    // Tries to offload ending of the task to the parent
                    if (state.compareAndSet(s, s & ~OFFLOAD)) {
                        return;
                    }
                }
                while (true) {
                    if ((s & OFFLOAD) != 0) {
                        /* OFFLOAD bit can never be observed here. Otherwise
                           it would mean there is another invocation of
                           "complete" that can run the task. */
                        throw new InternalError(String.valueOf(s));
                    }
                    if (s == BEGIN) {
                        if (state.compareAndSet(BEGIN, END)) {
                            return;
                        }
                    } else if (s == AGAIN) {
                        if (state.compareAndSet(AGAIN, BEGIN | OFFLOAD)) {
                            break;
                        }
                    } else if (s == STOP) {
                        return;
                    } else if (s == END) {
                        throw new IllegalStateException("Duplicate completion");
                    } else {
                        // Non-existent state
                        throw new InternalError(String.valueOf(s));
                    }
                    s = state.get();
                }
                restartableTask.run(completer);
            }
        }
    }

    /**
     * Tells whether, or not, this scheduler has been permanently stopped.
     *
     * <p> Should be used from inside the task to poll the status of the
     * scheduler, pretty much the same way as it is done for threads:
     * <pre>{@code
     *     if (!Thread.currentThread().isInterrupted()) {
     *         ...
     *     }
     * }</pre>
     */
    public boolean isStopped() {
        return state.get() == STOP;
    }

    /**
     * Stops this scheduler.  Subsequent invocations of {@code runOrSchedule}
     * are effectively no-ops.
     *
     * <p> If the task has already begun, this invocation will not affect it,
     * unless the task itself uses {@code isStopped()} method to check the state
     * of the handler.
     */
    public void stop() {
        state.set(STOP);
    }

    /**
     * Returns a new {@code SequentialScheduler} that executes the provided
     * {@code mainLoop} from within a {@link SynchronizedRestartableTask}.
     *
     * @apiNote This is equivalent to calling
     * {@code new SequentialScheduler(new SynchronizedRestartableTask(mainLoop))}
     * The main loop must not perform any blocking operation.
     *
     * @param mainLoop The main loop of the new sequential scheduler
     * @return a new {@code SequentialScheduler} that executes the provided
     * {@code mainLoop} from within a {@link SynchronizedRestartableTask}.
     */
    public static SequentialScheduler synchronizedScheduler(Runnable mainLoop) {
        return new SequentialScheduler(new SynchronizedRestartableTask(mainLoop));
    }

    /**
     * Returns a new {@code SequentialScheduler} that executes the provided
     * {@code mainLoop} from within a {@link LockingRestartableTask}.
     *
     * @apiNote This is equivalent to calling
     * {@code new SequentialScheduler(new LockingRestartableTask(mainLoop))}
     * The main loop must not perform any blocking operation.
     *
     * @param mainLoop The main loop of the new sequential scheduler
     * @return a new {@code SequentialScheduler} that executes the provided
     * {@code mainLoop} from within a {@link LockingRestartableTask}.
     */
    public static SequentialScheduler lockingScheduler(Runnable mainLoop) {
        return new SequentialScheduler(new LockingRestartableTask(mainLoop));
    }
}
