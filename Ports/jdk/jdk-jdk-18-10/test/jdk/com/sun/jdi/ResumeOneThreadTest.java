/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6700889
 * @summary  Thread resume invalidates all stack frames, even from other threads
 * @author jjh
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g ResumeOneThreadTest.java
 * @run driver ResumeOneThreadTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

class ResumeOneThreadTarg extends Thread {
    static String name1 = "Thread 1";
    static String name2 = "Thread 2";

    public ResumeOneThreadTarg(String name) {
        super(name);
    }

    public static void main(String[] args) {
        System.out.println("    Debuggee: Howdy!");
        ResumeOneThreadTarg t1 = new ResumeOneThreadTarg(name1);
        ResumeOneThreadTarg t2 = new ResumeOneThreadTarg(name2);

        t1.start();
        t2.start();
    }

    // This just starts two threads. Each runs to a bkpt.
    public void run() {
        if (getName().equals(name1)) {
            run1();
        } else {
            run2();
        }
    }

    public void bkpt1(String p1) {
        System.out.println("    Debuggee: bkpt 1");
    }

    public void run1() {
        bkpt1("Hello Alviso!");
    }



    public void bkpt2() {
        System.out.println("    Debuggee: bkpt 2");
    }

    public void run2() {
        bkpt2();
    }
}

/********** test program **********/

public class ResumeOneThreadTest extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;

    BreakpointRequest request1;
    BreakpointRequest request2;

    ThreadReference thread1 = null;
    ThreadReference thread2 = null;;
    boolean theVMisDead = false;

    ResumeOneThreadTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new ResumeOneThreadTest(args).startTests();
    }


    synchronized public void breakpointReached(BreakpointEvent event) {
        println("-- Got bkpt at: " + event.location());
        ThreadReference eventThread = event.thread();

        if (eventThread.name().equals(ResumeOneThreadTarg.name1)) {
            thread1 = eventThread;
        }

        if (eventThread.name().equals(ResumeOneThreadTarg.name2)) {
            thread2 = eventThread;
        }
    }

    public void vmDied(VMDeathEvent event) {
        theVMisDead = true;
    }

    synchronized public void eventSetComplete(EventSet set) {
        if (theVMisDead) {
            return;
        }
        if (thread1 == null || thread2 == null) {
            // Don't do a set.resume(), just let the other thread
            // keep running until it hits its bkpt.
            return;
        }

        // Both threads are stopped at their bkpts.  Get a StackFrame from
        // Thread 1 then resume Thread 2 and verify that the saved StackFrame is
        // still valid.

        // suspend everything.
        println("-- All threads suspended");
        vm().suspend();

        StackFrame t1sf0 = null;
        try {
            t1sf0 = thread1.frame(0);
        } catch (IncompatibleThreadStateException ee) {
            failure("FAILED: Exception: " + ee);
        }

        println("-- t1sf0 args: " + t1sf0.getArgumentValues());

        // Ok, we have a StackFrame for thread 1.  Resume just thread 2
        // Note that thread 2 has been suspended twice - by the SUSPEND_ALL
        // bkpt, and by the above vm().suspend(), so we have to resume
        // it twice.
        request2.disable();

        thread2.resume();
        thread2.resume();
        println("-- Did Resume on thread 2");

        // Can we get frames for thread1?
        try {
            StackFrame t1sf0_1 = thread1.frame(0);
            if (!t1sf0.equals(t1sf0_1)) {
                failure("FAILED: Got a different frame 0 for thread 1 after resuming thread 2");
            }
        } catch (IncompatibleThreadStateException ee) {
            failure("FAILED: Could not get frames for thread 1: Exception: " + ee);
        } catch (Exception ee) {
            failure("FAILED: Could not get frames for thread 1: Exception: " + ee);
        }


        try {
            println("-- t1sf0 args: " + t1sf0.getArgumentValues());
        } catch (InvalidStackFrameException ee) {
            // This is the failure.
            failure("FAILED Got InvalidStackFrameException");
            vm().dispose();
            throw(ee);
        }

        // Let the debuggee finish
        request1.disable();
        thread1.resume();
        vm().resume();
        println("--------------");
    }

    /********** test core **********/

    protected void runTests() throws Exception {

        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("ResumeOneThreadTarg");
        targetClass = bpe.location().declaringType();
        mainThread = bpe.thread();
        EventRequestManager erm = vm().eventRequestManager();
        final Thread mainThread = Thread.currentThread();

        /*
         * Set event requests
         */

        Location loc1 = findMethod(targetClass, "bkpt1", "(Ljava/lang/String;)V").location();
        request1 = erm.createBreakpointRequest(loc1);
        request1.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        request1.enable();

        Location loc2 = findMethod(targetClass, "bkpt2", "()V").location();
        request2 = erm.createBreakpointRequest(loc2);
        request2.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        request2.enable();

        /*
         * resume the target, listening for events
         */
        listenUntilVMDisconnect();
        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("ResumeOneThreadTest: passed");
        } else {
            throw new Exception("ResumeOneThreadTest: failed");
        }
    }
}
