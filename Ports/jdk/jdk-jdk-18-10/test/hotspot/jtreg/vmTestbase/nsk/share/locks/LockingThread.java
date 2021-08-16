/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share.locks;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import nsk.share.Consts;
import nsk.share.Log;
import nsk.share.TestBug;
import nsk.share.TestJNIError;
import nsk.share.Wicket;

/*
 Thread with possibility acquiring monitors in different ways:
 - entering synchronized method
 - entering synchronized method for thread object itself
 - entering synchronized static method
 - entering synchronized method for thread class itself
 - entering synchronized block on non-static object
 - entering synchronized block on non-static on thread object itself
 - entering synchronized block on static object
 - entering synchronized block on static thread object itself
 - JNI MonitorEnter.

 Description of required thread stack should be passed to LockingThread in constructor.
 When started locking thread create required stack and sleep until not interrupted.

 LockingThread can relinquish acquired monitors in follows ways:
 - relinquish single monitor through Object.wait - relinquishMonitor(int monitorIndex),
 - relinquish single monitor through exiting from synchronized blocks/methods or through JNI MonitorExit  - exitSingleFrame(),
 - relinquish all monitors(exit from all synchronized blocks/methods) - stopLockingThread()

 Debug information about each acquired/relinquished monitor is stored and can be obtained through getMonitorsInfo().

 To be sure that LockingThread have reached required state call method LockingThread.waitState().

 Usage example:

 List<String> stackFramesDescription = new ArrayList<String>();
 stackFramesDescription.add(LockingThread.SYNCHRONIZED_METHOD);
 stackFramesDescription.add(LockingThread.SYNCHRONIZED_OBJECT_BLOCK);

 LockingThread lockingThread = new LockingThread(log, stackFramesDescription);

 lockingThread.start();

 // after calling waitState() LockingThread should complete stack creation
  lockingThread.waitState();

  lockingThread.exitSingleFrame();

  // after calling waitState() LockingThread should complete exit from stack frame
   lockingThread.waitState();
   */
public class LockingThread extends Thread {
    // native part uses TestJNIError class
    private static final Class<?> jniErrorKlass = TestJNIError.class;
    static {
        try {
            System.loadLibrary("LockingThread");
        } catch (UnsatisfiedLinkError e) {
            System.out.println("Unexpected UnsatisfiedLinkError on loading library 'LockingThread'");
            e.printStackTrace(System.out);
            System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
        }
    }

    /*
     *  Information about acquired monitor
     */
    public static class DebugMonitorInfo {
        public DebugMonitorInfo(Object monitor, int stackDepth, Thread thread, boolean isNative) {
            this.monitor = monitor;
            this.stackDepth = stackDepth;
            this.thread = thread;
            this.isNative = isNative;
        }

        public Object monitor;

        public int stackDepth;

        public Thread thread;

        boolean isNative;
    }

    // acquire JNI monitor through JNIMonitorEnter()
    public static final String JNI_MONITOR_ENTER = "JNI_MONITOR_ENTER";

    // entering synchronized static method
    public static final String SYNCHRONIZED_STATIC_METHOD = "SYNCHRONIZED_STATIC_METHOD";

    // entering synchronized static method for thread class itself
    public static final String SYNCHRONIZED_STATIC_THREAD_METHOD = "SYNCHRONIZED_STATIC_THREAD_METHOD";

    // entering synchronized method
    public static final String SYNCHRONIZED_METHOD = "SYNCHRONIZED_METHOD";

    // entering synchronized method for thread object itself
    public static final String SYNCHRONIZED_THREAD_METHOD = "SYNCHRONIZED_THREAD_METHOD";

    // entering synchronized block for thread object itself
    public static final String SYNCHRONIZED_THIS_BLOCK = "SYNCHRONIZED_THIS_BLOCK";

    // entering synchronized block
    public static final String SYNCHRONIZED_OBJECT_BLOCK = "SYNCHRONIZED_OBJECT_BLOCK";

    // entering synchronized block on static object
    public static final String SYNCHRONIZED_BLOCK_STATIC_OBJECT = "SYNCHRONIZED_BLOCK_STATIC_OBJECT";

    // entering synchronized block on static thread object itself
    public static final String SYNCHRONIZED_BLOCK_STATIC_THREAD_OBJECT = "SYNCHRONIZED_BLOCK_STATIC_THREAD_OBJECT";

    // entering frame without monitor acquiring
    public static final String FRAME_WITHOUT_LOCK = "FRAME_WITHOUT_LOCK";

    // all acquired monitors
    private List<DebugMonitorInfo> monitorsInfo = new ArrayList<DebugMonitorInfo>();

    // This parameter should be passed in constructor
    // It describe how many locks and in which way LockingThread should acquire
    private List<String> stackFramesDescription;

    private Log log;

    // is during LockingThread's operations any errors occurred
    private boolean executedWithErrors;

    public boolean isExecutedWithErrors() {
        return executedWithErrors;
    }

    public LockingThread(Log log, List<String> stackFramesDescription) {
        this.log = log;
        this.stackFramesDescription = stackFramesDescription;
    }

    // return array containing all acquired monitors
    public DebugMonitorInfo[] getMonitorsInfo(boolean returnJNIMonitors) {
        Map<Object, DebugMonitorInfo> result = new HashMap<Object, DebugMonitorInfo>();

        for (int i = monitorsInfo.size() - 1; i >= 0; i--) {
            DebugMonitorInfo monitorInfo = monitorsInfo.get(i);

            if ((returnJNIMonitors || !monitorInfo.isNative) &&

            // don't return relinquished monitors
                    (monitorInfo.monitor != relinquishedMonitor) &&

                    // return only last monitor occurrence
                    !result.containsKey(monitorInfo.monitor)) {
                result.put(monitorInfo.monitor, monitorInfo);
            }
        }

        return result.values().toArray(new DebugMonitorInfo[] {});
    }

    void log(String message) {
        log.display(Thread.currentThread().getName() + ": " + message);
    }

    // add debug information about acquired monitor
    void addMonitorInfo(DebugMonitorInfo monitorInfo) {
        monitorsInfo.add(monitorInfo);
    }

    // remove debug information about acquired monitor (also should update information about stack depth)
    void removeMonitorInfo(DebugMonitorInfo removedMonitor) {
        for (DebugMonitorInfo monitor : monitorsInfo) {
            if (monitor.stackDepth > removedMonitor.stackDepth)
                monitor.stackDepth -= 2;
        }

        monitorsInfo.remove(removedMonitor);
    }

    // used for stack frames creation
    private int currentIndex;

    // Recursive function used for stack frames creation

    // For example if LockingThread should acquire 1 monitor through synchronized block
    // and 1 monitor through synchronized method pass list with values SYNCHRONIZED_METHOD and SYNCHRONIZED_OBJECT_BLOCK
    // to the constructor and after running LockingThread will have following stack frames:

    // run()
    //  createStackFrame()
    //      ClassWithSynchronizedMethods().synchronizedMethod() // monitor for instance of ClassWithSynchronizedMethods is acquired here
    //          createStackFrame()
    //              synchronizedObjectBlock()   // monitor for instance of Object is acquired here
    //                  createStackFrame()
    //                      doWait()
    //                          sleep()

    // When LockingThread have created required stack frame it calls method doWait() and sleep(Long.MAX_VALUE)

    // If LockingThread should relinquish one of the acquired monitors it should be interrupted and after
    // interrupting should call 'wait()' for specified monitor, and for this example LockingThread will have
    // following stack frames:

    // run()
    //  createStackFrame()
    //      ClassWithSynchronizedMethods().synchronizedMethod() // monitor for instance of ClassWithSynchronizedMethods is acquired here
    //          createStackFrame()
    //              synchronizedObjectBlock()   // monitor for instance of Object is acquired here
    //                  createStackFrame()
    //                      doWait()
    //                          relinquishedMonitor.wait()

    // LockingThread still holds all other locks because of it didn't exit from corresponding synchronized methods and blocks.
    // To let LockingThread acquire relinquished monitor 'relinquishedMonitor.notifyAll()' should be called, after this
    // LockingThread will acquire this monitor again because of it still in corresponding synchronized method or block and
    // it will have again such stack frames:

    // run()
    //  createStackFrame()
    //      ClassWithSynchronizedMethods().synchronizedMethod() // monitor for instance of ClassWithSynchronizedMethods is acquired here
    //          createStackFrame()
    //              synchronizedObjectBlock()   // monitor for instance of Object is acquired here
    //                  createStackFrame()
    //                      doWait()
    //                          sleep()
    void createStackFrame() {
        if (currentIndex < stackFramesDescription.size()) {
            String frameDescription = stackFramesDescription.get(currentIndex);

            currentIndex++;

            if (frameDescription.equals(JNI_MONITOR_ENTER)) {
                // for JNI monitors -1 is returned as stack depth
                int currentStackDepth = -1;
                Object object = new Object();
                DebugMonitorInfo monitorInfo = new DebugMonitorInfo(object, currentStackDepth, this, true);
                addMonitorInfo(monitorInfo);
                log("Enter JNI monitor");
                nativeJNIMonitorEnter(object);
                log("Exit JNI monitor");
                removeMonitorInfo(monitorInfo);
            } else if (frameDescription.equals(SYNCHRONIZED_BLOCK_STATIC_OBJECT)) {
                synchronizedBlockStaticObject();
            } else if (frameDescription.equals(SYNCHRONIZED_BLOCK_STATIC_THREAD_OBJECT)) {
                synchronizedBlockStaticThreadObject();
            } else if (frameDescription.equals(SYNCHRONIZED_METHOD)) {
                new ClassWithSynchronizedMethods().synchronizedMethod(this);
            } else if (frameDescription.equals(SYNCHRONIZED_THREAD_METHOD)) {
                synchronizedMethod();
            } else if (frameDescription.equals(SYNCHRONIZED_STATIC_METHOD)) {
                ClassWithSynchronizedMethods.synchronizedStaticMethod(this);
            } else if (frameDescription.equals(SYNCHRONIZED_STATIC_THREAD_METHOD)) {
                synchronizedStaticMethod(this);
            } else if (frameDescription.equals(SYNCHRONIZED_THIS_BLOCK)) {
                synchronizedThisBlock();
            } else if (frameDescription.equals(SYNCHRONIZED_OBJECT_BLOCK)) {
                synchronizedObjectBlock();
            } else if (frameDescription.equals(FRAME_WITHOUT_LOCK)) {
                frameWithoutLock();
            } else
                throw new TestBug("Invalid stack frame description: " + frameDescription);
        } else {
            // required stack is created
            ready();
            doWait();
        }

        if (exitSingleFrame) {
            if (currentIndex-- < stackFramesDescription.size()) {
                // exit from single synchronized block/method
                ready();
                doWait();
            }
        }
    }

    public Object getRelinquishedMonitor() {
        return relinquishedMonitor;
    }

    private Object relinquishedMonitor;

    private Wicket waitStateWicket = new Wicket();

    private Thread.State requiredState;

    public void waitState() {
        // try wait with timeout to avoid possible hanging (if LockingThread have finished execution because of uncaught exception)
        int result = waitStateWicket.waitFor(60000);

        if (result != 0) {
            throw new TestBug("Locking thread can't reach required state (waitStateWicket wasn't unlocked)");
        }

        if (requiredState == null)
            throw new TestBug("Required state not specified");

        long startTime = System.currentTimeMillis();

        // additional check to be sure that LockingThread acquired state
        while (this.getState() != requiredState) {

            // try wait with timeout to avoid possible hanging if something will go wrong
            if ((System.currentTimeMillis() - startTime) > 60000) {
                throw new TestBug("Locking thread can't reach required state (state: " + requiredState + " wasn't reached) in 1 minute");
            }

            Thread.yield();
        }

        requiredState = null;

        Object relinquishedMonitor = getRelinquishedMonitor();
        /*
         * Changing thread state and release of lock is not single/atomic operation.
         * As result there is a potential race when thread state (LockingThread) has
         * been changed but the lock has not been released yet. To avoid this current
         * thread is trying to acquire the same monitor, so current thread proceeds
         * execution only when monitor has been really relinquished by LockingThread.
         */
        if (relinquishedMonitor != null) {
            synchronized (relinquishedMonitor) {
            }
        }
    }

    // LockingThread acquired required state
    private void ready() {
        waitStateWicket.unlockAll();
    }

    // is LockingThread should relinquish single monitor
    private volatile boolean relinquishMonitor;

    private int relinquishedMonitorIndex;

    // relinquish single monitor with given index through Object.wait()
    public void relinquishMonitor(int index) {
        if (index >= monitorsInfo.size()) {
            throw new TestBug("Invalid monitor index: " + index);
        }

        requiredState = Thread.State.WAITING;
        waitStateWicket = new Wicket();
        relinquishMonitor = true;
        relinquishedMonitorIndex = index;

        interrupt();

        DebugMonitorInfo monitorInfo = monitorsInfo.get(relinquishedMonitorIndex);

        if (monitorInfo == null)
            throw new TestBug("Invalid monitor index: " + relinquishedMonitorIndex);
    }

    public void acquireRelinquishedMonitor() {
        if (relinquishedMonitor == null) {
            throw new TestBug("There is no relinquished monitor");
        }

        // Set requiredState to 'Thread.State.TIMED_WAITING' because of LockingThread call
        // Thread.sleep(Long.MAX_VALUE) after monitor acquiring
        requiredState = Thread.State.TIMED_WAITING;

        waitStateWicket = new Wicket();
        relinquishMonitor = false;

        synchronized (relinquishedMonitor) {
            relinquishedMonitor.notifyAll();
        }
    }

    public void stopLockingThread() {
        requiredState = Thread.State.TIMED_WAITING;

        waitStateWicket = new Wicket();
        exitSingleFrame = false;

        interrupt();
    }

    // is thread should exit from single synchronized block/method
    private boolean exitSingleFrame;

    public void exitSingleFrame() {
        requiredState = Thread.State.TIMED_WAITING;

        waitStateWicket = new Wicket();
        exitSingleFrame = true;

        interrupt();
    }

    // LockingThread call this method when required state is reached
    private void doWait() {
        while (true) {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                // expected exception, LockingThread should be interrupted to change state
            }

            // if single monitor should be relinquished through Object.wait()
            if (relinquishMonitor) {
                try {
                    DebugMonitorInfo monitorInfo = monitorsInfo.get(relinquishedMonitorIndex);

                    if (monitorInfo == null)
                        throw new TestBug("Invalid monitor index: " + relinquishedMonitorIndex);

                    relinquishedMonitor = monitorInfo.monitor;

                    log("Relinquish monitor: " + relinquishedMonitor);

                    // monitor is relinquished
                    ready();

                    // Really monitor is relinquished only when LockingThread calls relinquishedMonitor.wait(0) below,
                    // but to be sure that LockingThread have reached required state method waitState() should be called
                    // and this method waits when LockingThred change state to 'Thread.State.WAITING'

                    while (relinquishMonitor)
                        relinquishedMonitor.wait(0);

                    log("Acquire relinquished monitor: " + relinquishedMonitor);
                } catch (Exception e) {
                    executedWithErrors = true;
                    log("Unexpected exception: " + e);
                    e.printStackTrace(log.getOutStream());
                }

                relinquishedMonitor = null;

                // monitor is acquired again
                //(becuase we still are located in the frame where lock was acquired before we relinquished it)
                ready();
            } else
                // exit from frame
                break;
        }
    }

    public void run() {
        // LockingThread call Thread.sleep() when required stack frame was created
        requiredState = Thread.State.TIMED_WAITING;

        createStackFrame();

        // thread relinquished all monitors
        ready();
        doWait();
    }

    static synchronized void synchronizedStaticMethod(LockingThread lockingThread) {
        int currentStackDepth = lockingThread.expectedDepth();

        lockingThread.log("Enter synchronized static thread method");

        DebugMonitorInfo monitorInfo = new DebugMonitorInfo(LockingThread.class, currentStackDepth, lockingThread, false);
        lockingThread.addMonitorInfo(monitorInfo);
        lockingThread.createStackFrame();
        lockingThread.removeMonitorInfo(monitorInfo);

        lockingThread.log("Exit synchronized static thread method");
    }

    // calculate stack depth at which monitor was acquired
    int expectedDepth() {
        // for each monitor call 2 methods: createStackFrame() and method which acquire monitor
        // + when stack creation is finished call 3 methods: createStackFrame()->doWait()->sleep()
        return (stackFramesDescription.size() - currentIndex) * 2 + 3;
    }

    private native void nativeJNIMonitorEnter(Object object);

    synchronized void synchronizedMethod() {
        int currentStackDepth = expectedDepth();

        log("Enter synchronized thread method");

        DebugMonitorInfo monitorInfo = new DebugMonitorInfo(this, currentStackDepth, this, false);
        addMonitorInfo(monitorInfo);
        createStackFrame();
        removeMonitorInfo(monitorInfo);

        log("Exit synchronized thread method");
    }

    void synchronizedThisBlock() {
        int currentStackDepth = expectedDepth();

        log("Enter synchronized(this) block");

        synchronized (this) {
            DebugMonitorInfo monitorInfo = new DebugMonitorInfo(this, currentStackDepth, this, false);
            addMonitorInfo(monitorInfo);
            createStackFrame();
            removeMonitorInfo(monitorInfo);
        }

        log("Exit synchronized(this) block");
    }

    private static Object staticObject;

    // 'staticObjectInitializingLock' is used in synchronizedBlockStaticObject() and synchronizedBlockStaticThreadObject().
    // In this methods LockingThread initializes static object and enters in synchronized block
    // for this static object, this actions are not thread safe(because of static fields are used) and 'staticObjectInitializingLock'
    // is used to prevent races
    private static ReentrantLock staticObjectInitializingLock = new ReentrantLock();

    void synchronizedBlockStaticObject() {
        int currentStackDepth = expectedDepth();

        // initializing of 'staticObject' and entering to the synchronized(staticObject) block should be thread safe
        staticObjectInitializingLock.lock();

        staticObject = new Object();

        log("Enter synchronized(static object) block");

        synchronized (staticObject) {
            // thread unsafe actions was done
            staticObjectInitializingLock.unlock();

            DebugMonitorInfo monitorInfo = new DebugMonitorInfo(staticObject, currentStackDepth, this, false);
            addMonitorInfo(monitorInfo);
            createStackFrame();
            removeMonitorInfo(monitorInfo);
        }

        log("Exit synchronized(static object) block");
    }

    private static LockingThread staticLockingThread;

    void synchronizedBlockStaticThreadObject() {
        int currentStackDepth = expectedDepth();

        // initializing of 'staticLockingThread' and entering to the synchronized(staticLockingThread) block should be thread safe
        staticObjectInitializingLock.lock();

        staticLockingThread = this;

        log("Enter synchronized(static thread object) block");

        synchronized (staticLockingThread) {
            // thread unsafe actions was done
            staticObjectInitializingLock.unlock();

            DebugMonitorInfo monitorInfo = new DebugMonitorInfo(staticLockingThread, currentStackDepth, this, false);
            addMonitorInfo(monitorInfo);
            createStackFrame();
            removeMonitorInfo(monitorInfo);
        }

        log("Exit synchronized(static thread object) block");
    }

    void synchronizedObjectBlock() {
        int currentStackDepth = expectedDepth();

        Object object = new Object();

        log("Enter synchronized(object) block");

        synchronized (object) {
            DebugMonitorInfo monitorInfo = new DebugMonitorInfo(object, currentStackDepth, this, false);
            addMonitorInfo(monitorInfo);
            createStackFrame();
            removeMonitorInfo(monitorInfo);
        }

        log("Exit synchronized(object) block");
    }

    private void frameWithoutLock() {
        log("Enter frameWithoutLock");

        createStackFrame();

        for (DebugMonitorInfo monitor : monitorsInfo)
            monitor.stackDepth -= 2;

        log("Exit frameWithoutLock");
    }
}

//class containing synchronized method and synchronized static method, used by LockingThread
class ClassWithSynchronizedMethods {
    public synchronized void synchronizedMethod(LockingThread lockingThread) {
        int currentStackDepth = lockingThread.expectedDepth();

        lockingThread.log("Enter synchronized method");

        LockingThread.DebugMonitorInfo monitorInfo = new LockingThread.DebugMonitorInfo(this, currentStackDepth, lockingThread, false);
        lockingThread.addMonitorInfo(monitorInfo);
        lockingThread.createStackFrame();
        lockingThread.removeMonitorInfo(monitorInfo);

        lockingThread.log("Exit synchronized method");
    }

    public static synchronized void synchronizedStaticMethod(LockingThread lockingThread) {
        int currentStackDepth = lockingThread.expectedDepth();

        lockingThread.log("Enter synchronized static method");

        LockingThread.DebugMonitorInfo monitorInfo = new LockingThread.DebugMonitorInfo(ClassWithSynchronizedMethods.class, currentStackDepth,
                lockingThread, false);
        lockingThread.addMonitorInfo(monitorInfo);
        lockingThread.createStackFrame();
        lockingThread.removeMonitorInfo(monitorInfo);

        lockingThread.log("Exit synchronized static method");
    }
}
