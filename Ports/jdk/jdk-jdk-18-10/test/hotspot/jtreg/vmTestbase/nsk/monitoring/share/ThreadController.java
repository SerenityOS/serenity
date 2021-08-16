/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.monitoring.share;

import java.lang.management.*;
import java.util.*;

import nsk.share.*;

/**
 * The <code>ThreadController</code> class allows to operate with threads.
 */
public class ThreadController extends StateController {

    /**
     * Type of threads: pure java.
     */
    static public final int JAVA_TYPE = 0;

    /**
     * Type of threads: native.
     */
    static public final int NATIVE_TYPE = 1;

    /**
     * Type of threads: both java and native.
     */
    static public final int MIXED_TYPE = 2;

    /**
     * Result code: no errors.
     */
    static public final int NO_ERROR = 0;

    /**
     * Result code: wrong state of the thread.
     */
    static public final int ERR_STATE = 1;

    /**
     * Result code: error in stack trace.
     */
    static public final int ERR_STACKTRACE = 2;

    /**
     * Result code: thread not found.
     */
    static public final int ERR_THREAD_NOTFOUND = 3;


    // Prefix to print while logging
    static final String LOG_PREFIX = "ThreadController> ";

    // Internal trace levels
    static final int THREAD_TRACE_LEVEL = 50;

    /**
     * Suffix of all started threads.
     */
    static final String THREAD_SUFFIX = "_ThreadMM";

    // Number of tested kinds of threads
    public static final int THREAD_KIND_COUNT = 4;

    /**
     * Index of blocked threads.
     */
    static public final int BLOCKED_IDX = 0;

    /**
     * Index of waiting threads.
     */
    static public final int WAITING_IDX = 1;

    /**
     * Index of sleeping threads.
     */
    static public final int SLEEPING_IDX = 2;

    /**
     * Index of running threads.
     */
    static public final int RUNNING_IDX = 3;

    public static final String[] THREAD_KIND_NAMES =  {"BLOCKED","WAITING","SLEEPING","RUNNABLE"};
    public static final Thread.State[] THREAD_KINDS = {Thread.State.BLOCKED, Thread.State.WAITING, Thread.State.TIMED_WAITING, Thread.State.RUNNABLE};

    private Map<Thread.State, Integer> threadsCount = new HashMap<Thread.State, Integer>();
    private Map<Thread.State, List<BaseThread>> threadsClusters = new HashMap<Thread.State, List<BaseThread>>();

    private ThreadsGroupLocks threadsGroupLocks;


    int maxDepth;
    static int invocationType;

    static {
        try {
            System.loadLibrary("ThreadController");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Could not load \"ThreadController\" "
                    + "library");
            System.err.println("java.library.path:"
                    + System.getProperty("java.library.path"));
            throw e;
        }
    }

    /**
     * Creates a new <code>ThreadController</code> object with defined
     * arguments..
     *
     * @param log            <code>Log</code> object to print info to.
     * @param threadCount    number of threads to start.
     * @param maxDepth       depth of recursion.
     * @param invocationType type of threads to start (java, native, or mixed).
     */
    public ThreadController(Log log, int threadCount, int maxDepth,
                            String invocationType) {
        logPrefix = LOG_PREFIX;
        setLog(log);
        setThreadCount(threadCount);
        setDepth(maxDepth);
        setInvocationType(invocationType);
    }


    // Calculate how many threads of each kind to start
    private void setThreadCount(int threadCount) {
        int total = 0;
        int kinds = THREAD_KIND_COUNT;
        int tmp = threadCount / kinds;
        int rest = threadCount % kinds;
        int increased = kinds - rest;
        for (int i = 0; i < kinds; i++) {
            if (i >= increased) {
                threadsCount.put(THREAD_KINDS[i], tmp + 1);
            } else {
                threadsCount.put(THREAD_KINDS[i], tmp);
            }
        }
        display("number of created threads:\t" + threadCount);
    }

    // Print thread count
    private void printThreadCount() {
        for (Thread.State state : THREAD_KINDS) {
            display("\t" + state + " threads ("
                    + threadsCount.get(state) + ")");
        }
    }

    // Set recursion depth
    private void setDepth(int depth) {
        maxDepth = depth;
        display("depth for all threads:\t" + maxDepth);
    }

    // Set invocation type
    private void setInvocationType(String value) {
        display("invocation type:\t" + value);
        if (value.equals(ArgumentHandler.JAVA_TYPE)) {
            invocationType = JAVA_TYPE;
        } else if (value.equals(ArgumentHandler.NATIVE_TYPE)) {
            invocationType = NATIVE_TYPE;
        } else if (value.equals(ArgumentHandler.MIXED_TYPE)) {
            invocationType = MIXED_TYPE;
        } else {
            throw new Failure("UNKNOWN invocation type");
        }
    }

    /**
     * Returns invocation type.
     *
     * @return invocation type.
     */
    public int getInvocationType() {
        return invocationType;
    }

    /**
     * Returns thread count.
     *
     * @param state kind of thread state
     * @return thread count.
     */
    public int getThreadCount(Thread.State state) {
        return threadsCount.get(state);
    }

      /**
     * Returns thread count.
     *
     * @param kindIndex of thread state
     * @return thread count.
     */
    public int getThreadCount(int kindIndex) {
        return threadsCount.get(THREAD_KINDS[kindIndex]);
    }

    public int getThreadKindCount() {
        return THREAD_KINDS.length;
    }


    /**
     * Brings out VM into defined state.
     * <p/>
     * The method starts all threads.
     */
    public void run() {
        long startTime = System.currentTimeMillis() / 1000;
        startThreads();
        display("locking threads");
        waitForThreads();
    }

    /**
     * Tries to return VM into initial state
     * <p/>
     * The method interrupts all threads.
     */
    public void reset() {
        for (Thread.State state : THREAD_KINDS) {
            threadsGroupLocks.releaseGroup(state);
        }
    }

    // Get thread state via JVMTI
    private native Thread.State getThreadState(Thread thread);

    // Start all threads
    private void startThreads() throws Failure {

        String tmp_name;
        BaseThread thread = null;

        threadsGroupLocks = new ThreadsGroupLocks(threadsCount, logger);
        for (Thread.State state : THREAD_KINDS) {
            threadsClusters.put(state, new ArrayList<BaseThread>());
            for (int j = 0; j < threadsCount.get(state); j++) {
                tmp_name = state + THREAD_SUFFIX + int2Str(j);
                switch (state) {
                    case BLOCKED:
                        thread = new BlockedThread(this, tmp_name, logger.getLog(), threadsGroupLocks);
                        break;
                    case WAITING:
                        thread = new WaitingThread(this, tmp_name, logger.getLog(), threadsGroupLocks);
                        break;
                    case TIMED_WAITING:
                        thread = new SleepingThread(this, tmp_name, logger.getLog(), threadsGroupLocks);
                        break;
                    case RUNNABLE:
                        thread = new RunningThread(this, tmp_name, logger.getLog(), threadsGroupLocks);
                        break;
                    default:
                        throw new TestBug("Unknow thread kind");
                }
                threadsClusters.get(state).add(thread);
                thread.start();
            }
        }
        waitForThreads();
    }

    private boolean checkState(Thread.State expectedState) {
        for (Thread thread : threadsClusters.get(expectedState)) {
            if (getThreadState(thread) != expectedState) {

                return false;
            }
        }
        return true;
    }

    private void waitForThreads() {
        for (Thread.State state : THREAD_KINDS) {
            threadsGroupLocks.waitForGroup(state);
            while (!checkState(state)) {
                Thread.yield();
            }
        }
    }


    /**
     * Finds a thread with defined id.
     *
     * @param id ID of the thread.
     * @return a thread with defined id.
     */
    public BaseThread findThread(long id) {
        for(Thread.State state:THREAD_KINDS){
            for(BaseThread thread:threadsClusters.get(state)){
                 if (id==thread.getId()) {
                     return thread;
                }
            }
        }
        return null;
    }

    /**
     * Finds a thread by name.
     *
     * @param name name of the thread.
     * @return a thread with defined name.
     */
    public BaseThread findThread(String name) {
        for(Thread.State state:THREAD_KINDS){
            for(BaseThread thread:threadsClusters.get(state)){
                 if (name.equals(thread.getName())) {
                     return thread;
                }
            }
        }
        return null;
    }

    /**
     * Checks the thread's <code>ThreadInfo</code>.
     *
     * @param info <code>ThreadInfo</code> object to test.
     * @return result code.
     * @see #NO_ERROR
     * @see #ERR_THREAD_NOTFOUND
     * @see #ERR_STATE
     * @see #ERR_STACKTRACE
     */
    public int checkThreadInfo(ThreadInfo info) {
        String name = info.getThreadName();

        if (name.indexOf(THREAD_SUFFIX) == -1) {
            return NO_ERROR;
        }

        long id = info.getThreadId();
        Thread.State state = info.getThreadState();
        StackTraceElement[] stackTrace = info.getStackTrace();

        BaseThread thrd = findThread(id);
        if (thrd == null) {
            return ERR_THREAD_NOTFOUND;
        }

        if (!thrd.checkState(state))
            return ERR_STATE;

        if (!thrd.checkStackTrace(stackTrace))
            return ERR_STACKTRACE;

        return NO_ERROR;
    }
}

abstract class BaseThread extends Thread {

    private int currentDepth = 0;
    private String logPrefix;
    protected Log.Logger logger;

    protected ThreadController controller;

    protected List<String> expectedMethods = new ArrayList<String>();
    protected int expectedLength;

    protected ThreadsGroupLocks threadsGroupLocks;

    static {
        if (ThreadController.invocationType == ThreadController.NATIVE_TYPE ||
                ThreadController.invocationType == ThreadController.MIXED_TYPE) {
            try {
                System.loadLibrary("ThreadController");
            } catch (UnsatisfiedLinkError e) {
                System.err.println("Could not load \"ThreadController\" "
                        + "library");
                System.err.println("java.library.path:"
                        + System.getProperty("java.library.path"));
                throw e;
            }
        }
    }

    public BaseThread(ThreadController controller, String name, Log log, ThreadsGroupLocks threadsGroupLocks) {
        super(name);
        this.controller = controller;
        int pos = controller.LOG_PREFIX.indexOf('>');
        logPrefix = controller.LOG_PREFIX.substring(0, pos) + "::"
                + name + "> ";
        setLog(log);
        this.threadsGroupLocks = threadsGroupLocks;

        expectedLength = 1 + controller.maxDepth + 1;
        if(controller.invocationType == ThreadController.MIXED_TYPE) {
             //nativeRecursiveMethod
             expectedLength ++;
        }

        expectedMethods.add(BaseThread.class.getName() + ".run");

        switch (controller.invocationType) {
            case ThreadController.JAVA_TYPE:
                expectedMethods.add(BaseThread.class.getName() + ".recursiveMethod");
                break;
            case ThreadController.NATIVE_TYPE:
                expectedMethods.add(BaseThread.class.getName() + ".nativeRecursiveMethod");
                break;
            case ThreadController.MIXED_TYPE:
                expectedMethods.add(BaseThread.class.getName() + ".recursiveMethod");
                expectedMethods.add(BaseThread.class.getName() + ".nativeRecursiveMethod");
        }

        expectedMethods.add(ThreadsGroupLocks.PlainCountDownLatch.class.getName() + ".countDown");
    }

    public void run() {
        try {
            switch (controller.invocationType) {
                case ThreadController.JAVA_TYPE:
                case ThreadController.MIXED_TYPE:
                    recursiveMethod();
                    break;
                case ThreadController.NATIVE_TYPE:
                    nativeRecursiveMethod();
                    break;
                default:
                    throw new Failure("unknown invocationType:"
                            + controller.invocationType);
            }
        } catch (StackOverflowError e) {
            logger.complain(e.toString());
            throw new RuntimeException(e);
        }
        logger.trace(controller.THREAD_TRACE_LEVEL, "thread finished");
    }

    protected abstract void bringState();

    public abstract State getState();

    protected abstract void nativeBringState();

    public abstract boolean checkState(Thread.State state);

    public boolean checkStackTrace(StackTraceElement[] elements) {
        boolean res = true;

        logger.trace(controller.THREAD_TRACE_LEVEL, "trace elements: "
                + elements.length);

        if (elements.length > expectedLength) {
            res = false;
            logger.complain("Contains " + elements.length + ", more then "
                    + expectedLength + " elements");
        }

        for (int j = 0; j < elements.length; j++) {
            if (!checkElement(elements[j])) {
                logger.complain("Unexpected method name: "
                                + elements[j].getMethodName()
                                + " at " + j + " position");
                if (elements[j].isNativeMethod()) {
                    logger.complain("\tline number: (native method)");
                    logger.complain("\tclass name: " + elements[j].getClassName());
                } else {
                    logger.complain("\tline number: " + elements[j].getLineNumber());
                    logger.complain("\tclass name: " + elements[j].getClassName());
                    logger.complain("\tfile name: " + elements[j].getFileName());
                }
                res = false;
            }
        }
        return res;
    }

    protected boolean checkElement(StackTraceElement element) {
        String name = element.getClassName() + "." + element.getMethodName();
        if (expectedMethods.contains(name)) {
            return true;
        }

        logger.trace(controller.THREAD_TRACE_LEVEL, "\"" + name + "\""
                + " is not expected method name");
        return false;
    }

    protected void recursiveMethod() {
        currentDepth++;

        if (controller.maxDepth - currentDepth > 0) {

            Thread.yield();
            try {
                if (ThreadController.invocationType
                        == ThreadController.MIXED_TYPE) {
                    nativeRecursiveMethod();
                } else {
                    recursiveMethod();
                }

            } catch (StackOverflowError e) {
                logger.display(getName() + "> " + e);
            }

        } else if (controller.maxDepth == currentDepth) {
            logger.trace(controller.THREAD_TRACE_LEVEL, "state has been "
                    + "reached");
            bringState();
        }
        currentDepth--;
    }

    protected native void nativeRecursiveMethod();

    /**
     * Defines <code>Log.Logger</code> object
     */
    public void setLog(Log log) {
        logger = new Log.Logger(log, logPrefix);
    }
}

class BlockedThread extends BaseThread {

    private static final Thread.State STATE = Thread.State.BLOCKED;

    public State getState() {
        return STATE;
    }

    public BlockedThread(ThreadController controller, String name, Log log, ThreadsGroupLocks threadsGroupLocks) {
        super(controller, name, log, threadsGroupLocks);

        this.threadsGroupLocks = threadsGroupLocks;

        expectedLength += 2;

        expectedMethods.add(ThreadsGroupLocks.Blocker.class.getName() + ".block");

        switch (controller.invocationType) {
            case ThreadController.JAVA_TYPE:
                expectedMethods.add(BlockedThread.class.getName() + ".bringState");
                break;
            case ThreadController.NATIVE_TYPE:
                expectedMethods.add(BlockedThread.class.getName() + ".nativeBringState");
                break;
            case ThreadController.MIXED_TYPE:
                expectedMethods.add(BlockedThread.class.getName() + ".bringState");

        }
    }

    protected void bringState() {
        logger.trace(controller.THREAD_TRACE_LEVEL, "entering to monitor");
        threadsGroupLocks.getBarrier(getState()).countDown();
        threadsGroupLocks.blocker.block();
        logger.trace(controller.THREAD_TRACE_LEVEL, "exiting from monitor");
    }

    protected native void nativeBringState();

    public boolean checkState(Thread.State state) {
        return state == Thread.State.BLOCKED;
    }
}

class WaitingThread extends BaseThread {

    private static final Thread.State STATE = Thread.State.WAITING;
    public State getState() {
        return STATE;
    }

    private ThreadsGroupLocks threadsGroupLocks;

    public WaitingThread(ThreadController controller, String name, Log log, ThreadsGroupLocks threadsGroupLocks) {
        super(controller, name, log, threadsGroupLocks);

        this.threadsGroupLocks = threadsGroupLocks;

        expectedLength += 4;

        expectedMethods.add(ThreadsGroupLocks.PlainCountDownLatch.class.getName() + ".await");
        expectedMethods.add(Object.class.getName() + ".wait");

        switch (controller.invocationType) {
            case ThreadController.JAVA_TYPE:
                expectedMethods.add(WaitingThread.class.getName() + ".bringState");
                break;
            case ThreadController.NATIVE_TYPE:
                expectedMethods.add(WaitingThread.class.getName() + ".nativeBringState");
                break;
            case ThreadController.MIXED_TYPE:
                expectedMethods.add(WaitingThread.class.getName() + ".bringState");

        }
    }


    protected void bringState() {
        ThreadsGroupLocks.PlainCountDownLatch barrier = threadsGroupLocks.getBarrier(STATE);
        try {
            logger.trace(controller.THREAD_TRACE_LEVEL, "waiting on a monitor");
            threadsGroupLocks.getBarrier(getState()).countDown();
            barrier.await();
        } catch (InterruptedException e) {
            logger.display(e.toString());
        }
    }

    protected native void nativeBringState();

    public boolean checkState(Thread.State state) {
        return state == STATE;
    }

}

class SleepingThread extends BaseThread {
    private static final Thread.State STATE = State.TIMED_WAITING;

    public State getState() {
        return STATE;
    }

    private ThreadsGroupLocks threadsGroupLocks;

    public SleepingThread(ThreadController controller, String name, Log log, ThreadsGroupLocks threadsGroupLocks) {
        super(controller, name, log, threadsGroupLocks);

        this.threadsGroupLocks = threadsGroupLocks;

        expectedLength += 3;

        expectedMethods.add(Thread.class.getName() + ".sleep");
        expectedMethods.add(SleepingThread.class.getName() + ".run");

        switch (controller.invocationType) {
            case ThreadController.JAVA_TYPE:
                expectedMethods.add(SleepingThread.class.getName() + ".bringState");
                break;
            case ThreadController.NATIVE_TYPE:
                 expectedMethods.add(SleepingThread.class.getName() + ".nativeBringState");
                break;
            case ThreadController.MIXED_TYPE:
                 expectedMethods.add(SleepingThread.class.getName() + ".bringState");
        }

    }

    protected void bringState() {
        try {
            threadsGroupLocks.getBarrier(getState()).countDown();
            Thread.sleep(3600 * 1000);
        } catch (InterruptedException e) {
            logger.display(e.toString());
        }
    }

    protected native void nativeBringState();

    public boolean checkState(Thread.State state) {
        return state == Thread.State.TIMED_WAITING;
    }

    public void run() {
        try {
            switch (controller.invocationType) {
                case ThreadController.JAVA_TYPE:
                case ThreadController.MIXED_TYPE:
                    recursiveMethod();
                    break;
                case ThreadController.NATIVE_TYPE:
                    nativeRecursiveMethod();
                    break;
                default:
                    throw new Failure("unknown invocationType:"
                            + controller.invocationType);
            }
            logger.trace(controller.THREAD_TRACE_LEVEL, "thread finished");
        } catch (StackOverflowError e) {
            logger.complain(e.toString());
            throw new RuntimeException(e);
        }
    }
}

class RunningThread extends BaseThread {
    public State getState() {
        return STATE;
    }

    private static final Thread.State STATE = Thread.State.RUNNABLE;
    private ThreadsGroupLocks threadsGroupLocks;

    public RunningThread(ThreadController controller, String name, Log log, ThreadsGroupLocks threadsGroupLocks) {
        super(controller, name, log, threadsGroupLocks);
        this.threadsGroupLocks = threadsGroupLocks;

        expectedLength += 2;

        expectedMethods.add(Thread.class.getName() + ".yield");

        switch (controller.invocationType) {
            case ThreadController.JAVA_TYPE:
                expectedMethods.add(RunningThread.class.getName() + ".bringState");
                break;
            case ThreadController.NATIVE_TYPE:
                expectedMethods.add(RunningThread.class.getName() + ".nativeBringState");
                break;
            case ThreadController.MIXED_TYPE:
                expectedMethods.add(RunningThread.class.getName() + ".bringState");
        }
    }

    protected void bringState() {
        logger.trace(controller.THREAD_TRACE_LEVEL, "running loop");
        threadsGroupLocks.getBarrier(getState()).countDown();
        while (!threadsGroupLocks.runnableCanExit) {
            Thread.yield();
        }
    }

    protected native void nativeBringState();

    public boolean checkState(Thread.State state) {
        return state == Thread.State.RUNNABLE;
    }
}


class ThreadsGroupLocks {

    private Log.Logger logger;

    //for all
    private Map<Thread.State, PlainCountDownLatch> barriers = new HashMap<Thread.State, PlainCountDownLatch>();

    //for Blocked
    public final Blocker blocker = new Blocker();

    //for Runnable
    public volatile boolean runnableCanExit = false;

    public ThreadsGroupLocks(Map<Thread.State, Integer> threadsCount, Log.Logger logger) {
        this.logger = logger;
        for (Thread.State state : threadsCount.keySet()) {
            if (state == Thread.State.WAITING) {
                barriers.put(state, new PlainCountDownLatch(threadsCount.get(state) + 1));
            } else {
                barriers.put(state, new PlainCountDownLatch(threadsCount.get(state)));
            }
        }
        blocker.startBlocker();
    }

    public PlainCountDownLatch getBarrier(Thread.State state) {
        return barriers.get(state);
    }

    public void waitForGroup(Thread.State stateGroup) {
        switch (stateGroup) {
            case BLOCKED:
            case RUNNABLE:
            case TIMED_WAITING:
                try {
                    barriers.get(stateGroup).await();
                } catch (InterruptedException e) {
                    logger.display(e.toString());
                }
                break;

            case WAITING:
                while (barriers.get(stateGroup).getCount() != 1) {
                    Thread.yield();
                }
                break;
        }
    }

    public void releaseGroup(Thread.State stateGroup) {
        switch (stateGroup) {
            case BLOCKED:
                blocker.unBlock();
                break;
            case RUNNABLE:
                runnableCanExit = true;
                break;
            case TIMED_WAITING:
            case WAITING:
                barriers.get(stateGroup).countDown();
                break;
        }
    }

    public class Blocker {

        private Object monitor = new Object();
        private PlainCountDownLatch blockerCanExit = new PlainCountDownLatch(1);
        private PlainCountDownLatch blockerStart = new PlainCountDownLatch(1);

        private Runnable blockerThread = new Runnable() {
            public void run() {
                synchronized (monitor) {
                    blockerStart.countDown();
                    try {
                        blockerCanExit.await();
                    } catch (InterruptedException e) {
                        logger.display(e.toString());
                    }

                }
            }
        };

        public void startBlocker() {
            new Thread(blockerThread, "Blocker").start();
        }

        public void block() {
            try {
                blockerStart.await();
            } catch (InterruptedException e) {
                logger.complain(e.toString());
            }
            synchronized (monitor) {
            }
        }

        public void unBlock() {
            blockerCanExit.countDown();
        }
    }

     public static class PlainCountDownLatch {
        private volatile int counter;
        private Object counterMonitor = new Object();

        public PlainCountDownLatch(int counter){
            this.counter = counter;
        }

        public void countDown(){
            synchronized (counterMonitor) {
                counter--;
                if(counter==0) {
                    counterMonitor.notifyAll();
                }
            }
        }

        public void await() throws InterruptedException{
            synchronized (counterMonitor){
                while(counter != 0){
                    counterMonitor.wait();
                }
            }
        }

         public int getCount(){
             synchronized (counterMonitor) {
                 return counter;
             }
         }
    }

}
