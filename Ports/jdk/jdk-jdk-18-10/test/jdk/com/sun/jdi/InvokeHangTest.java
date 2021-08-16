/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6293795
 * @summary  Backend hangs when invokeMethod is called from a JDI eventHandler
 * @author jjh
 *
 * @library /test/lib
 * @modules java.management
 *          jdk.jdi
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g InvokeHangTest.java
 * @run driver InvokeHangTest
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
class InvokeHangTarg extends Thread {
    static boolean one = false;
    static String name1 = "Thread 1";
    static String name2 = "Thread 2";
    static int count = 100;

    public static void main(String[] args) {
        System.out.println("Howdy!");
        InvokeHangTarg t1 = new InvokeHangTarg(name1);
        InvokeHangTarg t2 = new InvokeHangTarg(name2);

        t1.start();
        t2.start();
    }

    // This is called from the debugger via invokeMethod
    public double invokeee() {
        System.out.println("Debuggee: invokeee in thread "+Thread.currentThread().toString());
        Thread.yield();
        return longMethod(2);
    }
    public double longMethod(int n) {
        double a = 0;
        double s = 0;
        for (int i = 0; i < n; i++) {
            a += i;
            for (int j = -1000*i; j < 1000*i; j++) {
                a = a*(1 + i/(j + 0.5));
                s += Math.sin(a);
            }
        }
        System.out.println("Debuggee: invokeee finished");
        return s;
    }

    public InvokeHangTarg(String name) {
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
        System.out.println("Debuggee: " + Thread.currentThread() +" is running:" + i);
        try {
            Thread.currentThread().sleep(2);
        } catch (InterruptedException iex) {}
        //yield();
    }

    public void run1() {
        int i = 0;
        while (i < count) {
            i++;
            bkpt1(i);
        }
    }

    public void bkpt2(int i) {
        System.out.println("Debuggee: " + Thread.currentThread() +" is running:" + i);
        try {
            Thread.currentThread().sleep(2);
        } catch (InterruptedException iex) {}
            //yield();
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

public class InvokeHangTest extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;
    BreakpointRequest request1;
    BreakpointRequest request2;
    static volatile int bkpts = 0;
    Thread timerThread;
    static long waitTime = jdk.test.lib.Utils.adjustTimeout(20000);

    InvokeHangTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new InvokeHangTest(args).startTests();
    }

    void doInvoke(ThreadReference thread, ObjectReference ref, String methodName) {
        List methods = ref.referenceType().methodsByName(methodName);
        Method method = (Method) methods.get(0);
        try {
            System.err.println("  Debugger: Invoking in thread" + thread);
            ref.invokeMethod(thread, method, new ArrayList(), ref.INVOKE_NONVIRTUAL);
            System.err.println("  Debugger: Invoke done");
        } catch (Exception ex) {
            ex.printStackTrace();
            failure("failure: Exception");
        }
    }

    // BreakpointEvent handler
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
         * and then an invoke is done in the event handler.  In some cases
         * the other thread has hit a bkpt and the back-end is waiting
         * to send it.  When the back-end resumes the debuggee to do the
         * invokeMethod, this 2nd bkpt is released, the debuggee is suspended, including
         * the thread on which the invoke was done (because it is a SUSPEND_ALL bkpt),
         * the bkpt is sent to the front-end, but the client event handler is sitting
         * here waiting for the invoke to finish, so it doesn't get the 2nd bkpt and
         * do the resume for it.  Thus, the debuggee is suspended waiting for a resume
         * that never comes.
         */
        request1.disable();
        request2.disable();

        ThreadReference thread = event.thread();
        try {
            StackFrame sf = thread.frame(0);
            System.err.println("  Debugger: Breakpoint hit at "+sf.location());
            doInvoke(thread, sf.thisObject(), "invokeee");
        } catch (IncompatibleThreadStateException itsex) {
            itsex.printStackTrace();
            failure("failure: Exception");
        }
        request1.enable();
        request2.enable();

    }

    /********** test core **********/

    protected void runTests() throws Exception {

        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("InvokeHangTarg");
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
         * got any breakpoint events while it was asleep.  If not, then
         * we assume the debuggee is hung and fail the test.
         */
        timerThread = new Thread("test timer") {
                public void run() {
                    int myBkpts = bkpts;
                    while (true) {
                        try {
                            Thread.sleep(waitTime);
                            System.out.println("bkpts = " + bkpts);
                            if (myBkpts == bkpts) {
                                // no bkpt for 'waitTime' msecs
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
            println("InvokeHangTest: passed; bkpts = " + bkpts);
        } else {
            throw new Exception("InvokeHangTest: failed; bkpts = " + bkpts);
        }
    }
}
