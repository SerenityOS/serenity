/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6296125
 * @summary  JDI: Disabling an EventRequest can cause a multi-threaded debuggee to hang
 * @author jjh
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g TwoThreadsTest.java
 * @run driver TwoThreadsTest
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
class TwoThreadsTarg extends Thread {
    static boolean one = false;
    static String name1 = "Thread 1";
    static String name2 = "Thread 2";
    static int count = 100;

    public static void main(String[] args) {
        System.out.println("Howdy!");
        TwoThreadsTarg t1 = new TwoThreadsTarg(name1);
        TwoThreadsTarg t2 = new TwoThreadsTarg(name2);

        t1.start();
        t2.start();
    }

    public TwoThreadsTarg(String name) {
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
        Thread.yield();
    }

    public void run1() {
        int i = 0;
        while (i < count) {
            i++;
            bkpt1(i);
        }
    }

    public void bkpt2(int i) {
        Thread.yield();
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

public class TwoThreadsTest extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;
    BreakpointRequest request1;
    BreakpointRequest request2;
    static volatile int bkpts = 0;
    Thread timerThread;
    static int waitTime = 20000;

    TwoThreadsTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new TwoThreadsTest(args).startTests();
    }

    /* BreakpointEvent handler */

    public void breakpointReached(BreakpointEvent event) {
        if (bkpts == 0) {
            /*
             * This thread will watch for n secs to go by with no
             * calls to this method.
             */
            timerThread.start();
        }

        synchronized("abc") {
            /*
             * Note that this will most likely never get to
             * the number of times the two bkpt lines in the debuggee
             * are hit because bkpts are lost while they are disabled.
             */
            bkpts++;
        }

        /*
         * The bug occurs when the requests are disabled
         * and then re-enabled during the event handler.
         */
        request1.disable();
        request2.disable();

        /*
         * This code between the disables and enables
         * is just filler that leaves the requests disabled
         * for awhile.  I suppose a sleep could be used instead
         */
        Method mmm = event.location().method();
        List lvlist;
        try {
            lvlist = mmm.variablesByName("i");
        } catch (AbsentInformationException ee) {
            failure("FAILED: can't get local var i");
            return;
        }
        LocalVariable ivar = (LocalVariable)lvlist.get(0);

        ThreadReference thr = event.thread();
        StackFrame sf;
        try {
            sf = thr.frame(0);
        } catch (IncompatibleThreadStateException ee) {
            failure("FAILED: bad thread state");
            return;
        }
        Value ival = sf.getValue(ivar);
        println("Got bkpt at: " + event.location() + ", i = " + ival);
        request1.enable();
        request2.enable();

    }

    /********** test core **********/

    protected void runTests() throws Exception {

        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("TwoThreadsTarg");
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
         * (Which we always expect to get).
         * It awakens every n seconds and checks to see if we
         * got any breakpoint events while it was asleep.
         */
        timerThread = new Thread("test timer") {
                public void run() {
                    int myBkpts = bkpts;
                    while (true) {
                        try {
                            Thread.sleep(waitTime);
                            System.out.println("bkpts = " + bkpts);
                            if (myBkpts == bkpts) {
                                // no bkpt for 'waitTime' secs
                                failure("failure: Debuggee appears to be hung");
                                vmDisconnected = true;
                                // This awakens the main thread which is
                                // waiting for a VMDisconnect.
                                mainThread.interrupt();
                                break;
                            }
                            myBkpts = bkpts;
                        } catch (InterruptedException ee) {
                            // If the test completes, this occurs.
                            println("timer Interrupted");
                            break;
                        }
                    }
                }
            };

        /*
         * resume the target, listening for events
         */
        listenUntilVMDisconnect();
        timerThread.interrupt();
        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("TwoThreadsTest: passed; bkpts = " + bkpts);
        } else {
            throw new Exception("TwoThreadsTest: failed; bkpts = " + bkpts);
        }
    }
}
