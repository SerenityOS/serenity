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


package nsk.jvmti.GetThreadState;

import java.io.PrintStream;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class thrstat005 {
    final static int JCK_STATUS_BASE = 95;

    public static final int TS_NEW        = 0;
    public static final int TS_TERMINATED = 1;

    public static final int TS_RUN_RUNNING           = 2;
    public static final int TS_RUN_BLOCKED           = 3;
    public static final int TS_RUN_WAIT_TIMED        = 4;
    public static final int TS_RUN_WAIT_INDEF        = 5;
    public static final int TS_RUN_WAIT_PARKED_TIMED = 6;
    public static final int TS_RUN_WAIT_PARKED_INDEF = 7;
    public static final int TS_RUN_WAIT_SLEEP        = 8; /* assumes _TIMED */

    public static final int WAIT_TIME = 250;

    public PrintStream _out;
    public Thread      _thrMain;
    public TestThread  _thrDummy;
    public int         _passCnt, _failCnt;

    /**
     * Set waiting time for checkThreadState
     */
    native static void setWaitTime(int sec);

    /**
     * Check that thread state (TS_xxx) is what we expect
     * (for TS_xxx -> JVMTI_THREAD_STATE_xxx mapping see table in thrstat005.c)
     */
    native static boolean checkThreadState(Thread t, int stateIdx);

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        return new thrstat005(out).run();
    }

    thrstat005(PrintStream out) {
        _out = out;
        _thrMain = Thread.currentThread();
        setWaitTime(WAIT_TIME * 23 / 11);
    }

    public int run() {
        _failCnt = 0;
        _passCnt = 0;

        testAndPrint("New", TS_NEW);
        testAndPrint("Running", TS_RUN_RUNNING);
        testAndPrint("Blocked on monitor", TS_RUN_BLOCKED);
        testAndPrint("Waiting with timeout", TS_RUN_WAIT_TIMED);
        testAndPrint("Waiting forever", TS_RUN_WAIT_INDEF);
        testAndPrint("Parking forever", TS_RUN_WAIT_PARKED_TIMED);
        testAndPrint("Parking with timeout", TS_RUN_WAIT_PARKED_INDEF);
        testAndPrint("Sleeping", TS_RUN_WAIT_SLEEP);
        testAndPrint("Terminating", TS_TERMINATED);

        log(">>> PASS/FAIL: " + _passCnt + "/" + _failCnt);

        return _failCnt > 0 ? 2 : 0;
    }

    public void testAndPrint(String name, int state) {
        boolean fPassed;

        try {
            log(">>> Testing state: " + name);
            fPassed = test(state);
        } catch ( BrokenBarrierException e ) {
            log("Main: broken barrier exception");
            fPassed = false;
        } catch ( InterruptedException e ) {
            log("Main: interrupted exception");
            fPassed = false;
        }

        log(">>> " + (fPassed ? "PASSED" : "FAILED") + " testing state: " + name);
        if ( fPassed )
            _passCnt++;
        else
            _failCnt++;
    }

    public boolean test(int state) throws BrokenBarrierException, InterruptedException {
        boolean fRes;

        switch ( state ) {
            case TS_NEW:
                log("Main: Creating new thread");
                _thrDummy = new TestThread();
                fRes = checkThreadState(_thrDummy, state);
                _thrDummy.start();
                return fRes;

            case TS_RUN_RUNNING:
                log("Main: Running thread");
                _thrDummy._fRun = true;
                fRes = sendStateAndCheckIt(state);
                _thrDummy._fRun = false;
                return fRes;

            case TS_RUN_BLOCKED:
                log("Main: Blocking thread");
                synchronized ( _thrDummy._mon ) {
                    return sendStateAndCheckIt(state);
                }

            case TS_RUN_WAIT_TIMED:
            case TS_RUN_WAIT_INDEF:
                log("Main: Thread will wait");
                _thrDummy._fRun = true;
                fRes = sendStateAndCheckIt(state);

                _thrDummy._fRun = false;
                do {
                    log("Main: Notifying the thread");
                    synchronized ( _thrDummy._mon ) {
                        _thrDummy._mon.notify();
                    }

                    if ( ! _thrDummy._fInTest ) {
                        break;
                    }

                    Thread.sleep(WAIT_TIME / 4);
                } while ( true );

                return fRes;

            case TS_RUN_WAIT_PARKED_TIMED:
            case TS_RUN_WAIT_PARKED_INDEF:
                log("Main: Thread will park");
                _thrDummy._fRun = true;
                fRes = sendStateAndCheckIt(state);

                _thrDummy._fRun = false;
                do {
                    log("Main: Unparking the thread");
                    LockSupport.unpark(_thrDummy);

                    if ( ! _thrDummy._fInTest ) {
                        break;
                    }

                    Thread.sleep(WAIT_TIME);
                } while ( true );

                return fRes;

            case TS_RUN_WAIT_SLEEP:
                log("Main: Thread will sleep");
                _thrDummy._fRun = true;
                fRes = sendStateAndCheckIt(state);
                _thrDummy._fRun = false;
                return fRes;

            case TS_TERMINATED:
                log("Main: Terminating thread");
                _thrDummy.sendTestState(state);

                log("Main: Waiting for join");
                _thrDummy.join();
                return checkThreadState(_thrDummy, state);
        }

        return false;
    }

    public boolean sendStateAndCheckIt(int state) throws BrokenBarrierException, InterruptedException {
        _thrDummy.sendTestState(state);
        while ( ! _thrDummy._fInTest ) {
            log("Main: Waiting for the thread to start the test");
            Thread.sleep(WAIT_TIME * 29 / 7); // Wait time should not be a multiple of WAIT_TIME
        }
        return checkThreadState(_thrDummy, state);
    }

    synchronized void log(String s) {
        _out.println(s);
        _out.flush();
    }

    class TestThread extends Thread {

        SynchronousQueue<Integer> _taskQueue = new SynchronousQueue<Integer>();

        public volatile boolean _fRun = true;
        public volatile boolean _fInTest = false;
        public Object _mon = new Object();

        public void sendTestState(int state) throws BrokenBarrierException, InterruptedException {
            _taskQueue.put(state);
        }

        public int recvTestState() {
            int state = TS_NEW;
            try {
                state = _taskQueue.take();
            } catch ( InterruptedException e ) {
                log("Thread: interrupted exception " + e);
            }
            return state;
        }

        public void run() {
            log("Thread: started");

            while ( true ) {
                int state = recvTestState();
                switch ( state ) {
                    case TS_NEW:
                        log("Thread: ERROR IN TEST: TS_NEW");
                        break;

                    case TS_RUN_RUNNING:
                        int i = 0;
                        log("Thread: Running...");
                        _fInTest = true;
                        while ( _fRun ) i++;
                        log("Thread: Running: done");
                        _fInTest = false;
                        break;

                    case TS_RUN_BLOCKED:
                        log("Thread: Blocking...");
                        _fInTest = true;
                        synchronized ( _mon ) {}
                        log("Thread: Blocking: done");
                        _fInTest = false;
                        break;

                    case TS_RUN_WAIT_TIMED:
                        log("Thread: Waiting with timeout...");
                        while ( _fRun ) {
                            synchronized ( _mon ) {
                                _fInTest = true;
                                try {
                                    _mon.wait(WAIT_TIME);
                                } catch ( InterruptedException e ) {
                                    log("Thread: Interrupted exception");
                                }
                            }
                        }
                        log("Thread: Waiting: done");
                        _fInTest = false;
                        break;

                    case TS_RUN_WAIT_INDEF:
                        log("Thread: Waiting indefinitely...");
                        _fInTest = true;
                        synchronized ( _mon ) {
                            try {
                                _mon.wait();
                            } catch ( InterruptedException e ) {
                                log("Thread: Interrupted exception");
                            }
                            log("Thread: Waiting: done");
                            _fInTest = false;
                        }
                        break;

                    case TS_RUN_WAIT_SLEEP:
                        log("Thread: Sleeping...");
                        while ( _fRun ) {
                            try {
                                _fInTest = true;
                                Thread.sleep(WAIT_TIME);
                            } catch ( InterruptedException e ) {
                                log("Thread: Interrupted exception");
                            }
                        }
                        log("Thread: Sleeping: done");
                        _fInTest = false;
                        break;

                    case TS_RUN_WAIT_PARKED_TIMED:
                        log("Thread: Parking indefinitely...");
                        _fInTest = true;
                        while ( _fRun ) {
                            LockSupport.park();
                        }
                        log("Thread: Parking: done");
                        _fInTest = false;
                        break;

                    case TS_RUN_WAIT_PARKED_INDEF:
                        log("Thread: Parking with timeout...");
                        _fInTest = true;
                        while ( _fRun ) {
                            LockSupport.parkUntil(System.currentTimeMillis() + WAIT_TIME);
                        }
                        log("Thread: Parking: done");
                        _fInTest = false;
                        break;

                    case TS_TERMINATED:
                        log("Thread: terminating");
                        return;
                }
            }
        }
    }
}
