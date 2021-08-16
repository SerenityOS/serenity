/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6751643
 * @key intermittent
 * @summary ThreadReference.ownedMonitors() can return null
 * @author jjh
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g SimulResumerTest.java
 * @run driver SimulResumerTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

/*
 * This debuggee basically runs two threads each of
 * which loop, hitting a bkpt in each iteration.
 *
 */
class SimulResumerTarg extends Thread {
    static boolean one = false;
    static String name1 = "Thread 1";
    static String name2 = "Thread 2";
    static int count = 10000;
    public static void main(String[] args) {
        System.out.println("Howdy!");
        SimulResumerTarg t1 = new SimulResumerTarg(name1);
        SimulResumerTarg t2 = new SimulResumerTarg(name2);

        t1.start();
        t2.start();
    }

    public SimulResumerTarg(String name) {
        super(name);
    }

    public void run() {
        if (getName().equals(name1)) {
            run1();
        } else {
            run2();
        }
    }

    public void bkpt1(int i) {
        synchronized(name1) {
            Thread.yield();
        }
    }

    public void run1() {
        int i = 0;
        while (i < count) {
            i++;
            bkpt1(i);
        }
    }

    public void bkpt2(int i) {
        synchronized(name2) {
            Thread.yield();
        }
    }

    public void run2() {
        int i = 0;
        while (i < count) {
            i++;
            bkpt2(i);
        }
    }
}

/********** test program **********/

public class SimulResumerTest extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;
    BreakpointRequest request1;
    BreakpointRequest request2;
    static volatile int bkpts = 0;
    static int iters = 0;
    Thread resumerThread;
    static int waitTime = 100;
    ThreadReference debuggeeThread1 = null;
    ThreadReference debuggeeThread2 = null;

    SimulResumerTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new SimulResumerTest(args).startTests();
    }

    /* BreakpointEvent handler */

    public void breakpointReached(BreakpointEvent event) {
        // save ThreadRefs for the two debuggee threads
        ThreadReference thr = event.thread();
        if (bkpts == 0) {
            resumerThread.start();
            debuggeeThread1 = thr;
            System.out.println("thr1 = " + debuggeeThread1);
        }

        if (debuggeeThread2 == null && thr != debuggeeThread1) {
            debuggeeThread2 = thr;
            System.out.println("thr2 = " + debuggeeThread2);
        }

        synchronized("abc") {
            bkpts++;
        }
        /**
        if (bkpts >= SimulResumerTarg.count * 2) {
            resumerThread.interrupt();
        }
        *****/

    }

    /********** test core **********/

    void check(ThreadReference thr) {
        // This calls each ThreadReference method that could fail due to the bug
        // that occurs if a resume is done while a call to the method is in process.
        String kind = "";
        if (thr != null) {
            try {
                kind = "ownedMonitors()";
                System.out.println("kind = " + kind);
                if (thr.ownedMonitors() == null) {
                    failure("failure: ownedMonitors = null");
                }

                kind = "ownedMonitorsAndFrames()";
                System.out.println("kind = " + kind);
                if (thr.ownedMonitorsAndFrames() == null) {
                    failure("failure: ownedMonitorsAndFrames = null");
                }

                kind = "currentContendedMonitor()";
                System.out.println("kind = " + kind);
                thr.currentContendedMonitor();
                // no failure return value here; could cause an NPE

                kind = "frames()";
                System.out.println("kind = " + kind);
                List<StackFrame> frames = thr.frames();
                // no failure return value here; could cause an NPE

                kind = "frames(0, size - 1)";
                System.out.println("kind = " + kind);
                int nframes = frames.size();
                while (nframes > 0) {
                    try {
                        thr.frames(0, nframes - 1);
                        break;
                    } catch (IndexOutOfBoundsException iobe) {
                        // 6815126. let's try to get less frames
                        iobe.printStackTrace();
                        nframes--;
                    }
                }

                kind = "frameCount()";
                System.out.println("kind = " + kind);
                if (thr.frameCount() == -1) {
                    failure("failure: frameCount = -1");
                }

                kind = "name()";
                System.out.println("kind = " + kind);
                if (thr.name() == null) {
                    failure("failure: name = null");
                }

                kind = "status()";
                System.out.println("kind = " + kind);
                if (thr.status() < 0) {
                    failure("failure: status < 0");
                }

            } catch (IncompatibleThreadStateException ee) {
                // ignore checks if thread was not suspended
            } catch (ObjectCollectedException ee) {
                // ignore ownedMonitors failure
            } catch (VMDisconnectedException ee) {
                // This is how we stop.  The debuggee runs to completion
                // and we get this exception.
                throw ee;
            } catch (Exception ee) {
                failure("failure: Got exception from " + kind + ": " + ee );
            }
        }
    }

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("SimulResumerTarg");
        targetClass = bpe.location().declaringType();
        mainThread = bpe.thread();
        EventRequestManager erm = vm().eventRequestManager();
        final Thread mainThread = Thread.currentThread();

        /*
         * Set event requests
         */
        Location loc1 = findMethod(targetClass, "bkpt1", "(I)V").location();
        Location loc2 = findMethod(targetClass, "bkpt2", "(I)V").location();
        request1 = erm.createBreakpointRequest(loc1);
        request2 = erm.createBreakpointRequest(loc2);
        request1.enable();
        request2.enable();

        /*
         * This thread will be started when we get the first bkpt.
         */
        resumerThread = new Thread("test resumer") {
                public void run() {
                    while (true) {
                        iters++;
                        // System.out.println("bkpts = " + bkpts + ", iters = " + iters);
                        try {
                            Thread.sleep(waitTime);
                            check(debuggeeThread1);
                            check(debuggeeThread2);
                        } catch (InterruptedException ee) {
                            // If the test completes, this occurs.
                            println("resumer Interrupted");
                            break;
                        } catch (VMDisconnectedException ee) {
                            println("VMDisconnectedException");
                            break;
                        }
                    }
                }
            };

        /*
         * resume the target, listening for events
         */
        listenUntilVMDisconnect();
        resumerThread.interrupt();
        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("SimulResumerTest: passed; bkpts = " + bkpts + ", iters = " + iters);
        } else {
            throw new Exception("SimulResumerTest: failed; bkpts = " + bkpts + ", iters = " + iters);
        }
    }
}
