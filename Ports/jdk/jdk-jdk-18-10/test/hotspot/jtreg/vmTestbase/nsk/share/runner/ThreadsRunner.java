/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package nsk.share.runner;

import nsk.share.gc.OOMStress;
import nsk.share.log.*;
import nsk.share.test.Stresser;
import nsk.share.test.ExecutionController;
import nsk.share.TestBug;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *  Helper to assist in running threads.
 *
 *  This class starts a number of threads which run some tasks in cycle.
 *  They exit after some time or after some iterations as
 *  determined by RunParams.
 */
public class ThreadsRunner implements MultiRunner, LogAware, RunParamsAware {

    private Log log;
    private RunParams runParams;
    private List<Runnable> runnables = new ArrayList<Runnable>();
    private List<ManagedThread> threads = new ArrayList<ManagedThread>();
    private AtomicInteger notStarted;
    private AtomicInteger finished;
    private boolean started = false;
    private boolean successful = true;

    public ThreadsRunner() {
        this(RunParams.getInstance());
    }

    public ThreadsRunner(RunParams runParams) {
        setRunParams(runParams);
    }

    public final void setLog(Log log) {
        this.log = log;
    }

    private static class ManagedThreadFactory {

        private RunParams params;

        static ManagedThreadFactory createFactory(RunParams params) {
            return new ManagedThreadFactory(params);
        }

        private ManagedThreadFactory(RunParams params) {
            this.params = params;
        }

        public Thread newThread(Runnable runnable, String name, int num) {
            return new Thread(runnable, name);
        }
    }

    private class ManagedThread implements Runnable {

        private Stresser stresser;
        private Throwable exception;
        private Runnable test;
        private boolean shouldWait;
        private Thread thread;


        public ManagedThread(ManagedThreadFactory threadFactory, Runnable test, int num) {
            this.test = test;
            this.shouldWait = true;
            this.thread = threadFactory.newThread(this, test.toString(), num);
            this.stresser = new Stresser(thread.getName(), runParams.getStressOptions());
        }

        @Override
        public void run() {
            notStarted.decrementAndGet();
            while (notStarted.get() != 0) {
                Thread.onSpinWait();
            }
            try {
                stresser.start(runParams.getIterations());
                while (!this.thread.isInterrupted() && stresser.iteration()) {
                    test.run();
                    Thread.yield();
                }
            } catch (OutOfMemoryError oom) {
                if (test instanceof OOMStress) {
                    // Test stressing OOM, not a failure.
                    log.info("Caught OutOfMemoryError in OOM stress test, omitting exception.");
                } else {
                    failWithException(oom);
                }
            } catch (Throwable t) {
                failWithException(t);
            } finally {
                waitForOtherThreads();
                stresser.finish();
            }
        }

        private void waitForOtherThreads() {
            if (shouldWait) {
                shouldWait = false;
                finished.decrementAndGet();
                while (finished.get() != 0) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                    }
                }
            } else {
                throw new TestBug("Waiting a second time is not premitted");
            }
        }

        private void failWithException(Throwable t) {
            log.debug("Exception in ");
            log.debug(test);
            log.debug(t);
            exception = t;
        }

        public void forceFinish() {
            stresser.forceFinish();
            if (runParams.isInterruptThreads()) {
                log.debug("Interrupting: " + this);
                this.thread.interrupt();
            }
        }

        public final Throwable getException() {
            return exception;
        }

        public final ExecutionController getExecutionController() {
            return stresser;
        }
    }

    public void add(Runnable runnable) {
        runnables.add(runnable);
    }

    public void remove(Runnable runnable) {
        runnables.remove(runnable);
    }

    public void removeAll() {
        runnables.clear();
    }

    private Runnable get(int index) {
        return (Runnable) runnables.get(index);
    }

    public Thread getThread(int index) {
        return threads.get(index).thread;
    }

    private int getCount() {
        return runnables.size();
    }

    private void prepare() {
    }

    private void create() {
        int threadCount = runnables.size();
        notStarted = new AtomicInteger(threadCount);
        finished = new AtomicInteger(threadCount);
        ManagedThreadFactory factory = ManagedThreadFactory.createFactory(runParams);
        for (int i = 0; i < threadCount; ++i) {
            threads.add(new ManagedThread(factory, get(i), i));
        }
    }

    /**
     * Start threads that run the tasks.
     */
    public void start() {
        if (started) {
            return;
        }
        create();
        prepare();
        for (int i = 0; i < threads.size(); ++i) {
            Thread t = threads.get(i).thread;
            log.debug("Starting " + t);
            t.start();
        }
        started = true;
    }

    /**
     * Stop threads that run the tasks.
     */
    public void forceFinish() {
        log.info("Forcing threads to finish");
        for (int i = 0; i < threads.size(); i++) {
            ManagedThread thread = threads.get(i);
            thread.forceFinish();
        }
    }

    /**
     * Join threads that run the tasks.
     */
    public void join() throws InterruptedException {
        for (int i = 0; i < threads.size(); ++i) {
            Thread t = threads.get(i).thread;
            //log.debug("Joining " + t);
            t.join();
        }
    }

    private int dumpFailures() {
        int n = 0;
        for (int i = 0; i < threads.size(); i++) {
            ManagedThread thread = threads.get(i);
            Throwable exception = thread.getException();
            if (exception != null) {
                if (n == 0) {
                    log.error("Failures summary:");
                }
                ++n;
                log.error(exception);
            }
        }
        if (n == 0) {
            log.info("No unexpected exceptions/errors are thrown");
        }
        return n;
    }

    private ManagedThread findManagedThread(Thread t) {
        for (int i = 0; i < threads.size(); i++) {
            ManagedThread mt = threads.get(i);
            if (mt.thread == t) {
                return mt;
            }
        }
        return null;
    }

    /**
     * Run threads as determined by RunParams.
     *
     * Start threads, run for some time or for some number of iterations,
     * then join and report if there were any exceptions.
     *
     * This method may additionally run other threads (as determined by RunParams):
     * - thread that does System.gc() in cycle, @see GCRunner
     * - thread that prints memory information in cycle, @see MemDiag
     * - thread that prints information about FinMemoryObject's in cycle, @see FinDiag
     * - thread that prints information about AllMemoryObject's in cycle, @see AllDiag
     *
     * @return true if there were no exceptions, false otherwise
     */
    public void run() {
        if (runParams.isRunGCThread()) {
            add(new GCRunner());
        }
        if (runParams.isRunFinThread()) {
            add(new FinRunner());
        }
        if (runParams.isRunMemDiagThread()) {
            add(new MemDiag());
        }
        try {
            start();
            join();
            successful = dumpFailures() == 0;
        } catch (Throwable t) {
            log.info("Unexpected exception during the run.");
            log.info(t);
            successful = false;
        }
    }

    public boolean isSuccessful() {
        return successful;
    }

    public ExecutionController getExecutionController() {
        Thread ct = Thread.currentThread();
        ManagedThread t = findManagedThread(ct);
        if (t != null) {
            return t.getExecutionController();
        } else {
            throw new TestBug("Unable to find managed thread for thread (this method should be called from one of managed threads): " + ct);
        }
    }

    public void runForever() {
        start();
    }

    public final void setRunParams(RunParams runParams) {
        this.runParams = runParams;
    }
}
