/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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


/*
 * @test
 *
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/status/status005.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *  A test for status() method of ThreadReference interface.
 *  The test checks if the method returns
 *  ThreadReference.THREAD_STATUS_WAIT status for
 *  debuggee's thread when it performs Object.wait() call.
 *  The test has two cases:
 *    - for Object.wait() call,
 *    - for Object.wait(long) call.
 *  The test checks status of unsuspended and suspended thread in
 *  both cases.
 *  The test consists of a debugger program (status005.java)
 *  and debuggee application (status005a.java).
 *  Package name is nsk.jdi.ThreadReference.status.
 *  The test works as follows.
 *  The debugger uses nsk.jdi.share framework classes to
 *  establish connection with debuggee. The debugger and debuggee
 *  synchronize with each other using special commands over
 *  communication channel provided by framework classes.
 *  In both test cases the debuggee starts special axiliary threads.
 *  After notification of readiness from debuggee, the debugger
 *  checks if ThreadReference.status() returns expected status
 *  for checked thread. Then the debugger suspends the checked
 *  thread and checks its status again.
 *  The test fails if status() method returned a status code
 *  other then expected THREAD_STATUS_WAIT one.
 * COMMENTS:
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.status.status005.status005
 *        nsk.jdi.ThreadReference.status.status005.status005a
 * @run main/othervm
 *      nsk.jdi.ThreadReference.status.status005.status005
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.ThreadReference.status.status005;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.ThreadReference;

//import com.sun.jdi.request.*;
//import com.sun.jdi.event.*;
//import com.sun.jdi.connect.*;
import java.io.*;
import java.util.*;

/**
 * The debugger application of the test.
 */
public class status005 {

    //------------------------------------------------------- immutable common fields

    final static String SIGNAL_READY = "ready";
    final static String SIGNAL_GO    = "go";
    final static String SIGNAL_QUIT  = "quit";

    private static int waitTime;
    private static int exitStatus;
    private static ArgumentHandler     argHandler;
    private static Log                 log;
    private static Debugee             debuggee;
    private static ReferenceType       debuggeeClass;

    //------------------------------------------------------- mutable common fields

    private final static String prefix = "nsk.jdi.ThreadReference.status.status005.";
    private final static String className = "status005";
    private final static String debuggerName = prefix + className;
    private final static String debuggeeName = debuggerName + "a";

    //------------------------------------------------------- test specific fields

    private final static String testedThreadName = prefix + "status005aThread";

    //------------------------------------------------------- immutable common methods

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    private static void display(String msg) {
        log.display("debugger > " + msg);
    }

    private static void complain(String msg) {
        log.complain("debugger FAILURE > " + msg);
    }

    public static int run(String argv[], PrintStream out) {

        exitStatus = Consts.TEST_PASSED;

        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        waitTime = argHandler.getWaitTime() * 60000;

        debuggee = Debugee.prepareDebugee(argHandler, log, debuggeeName);

        debuggeeClass = debuggee.classByName(debuggeeName);
        if ( debuggeeClass == null ) {
            complain("Class '" + debuggeeName + "' not found.");
            exitStatus = Consts.TEST_FAILED;
        }

        execTest();

        debuggee.quit();

        return exitStatus;
    }

    //------------------------------------------------------ mutable common method

    private static void execTest() {
        debuggee.receiveExpectedSignal(SIGNAL_GO);
        check(status005a.testedThreadNames[0], "CHECK1", "CHECK2");
        check(status005a.testedThreadNames[1], "CHECK3", "CHECK4");
        display("Checking completed!");
    }

    //--------------------------------------------------------- test specific methods

    private static String getThreadStatusName (int status) {
        switch (status) {
            case ThreadReference.THREAD_STATUS_UNKNOWN:
                return "THREAD_STATUS_UNKNOWN";
            case ThreadReference.THREAD_STATUS_ZOMBIE:
                return "THREAD_STATUS_ZOMBIE";
            case ThreadReference.THREAD_STATUS_RUNNING:
                return "THREAD_STATUS_RUNNING";
            case ThreadReference.THREAD_STATUS_SLEEPING:
                return "THREAD_STATUS_SLEEPING";
            case ThreadReference.THREAD_STATUS_MONITOR:
                return "THREAD_STATUS_MONITOR";
            case ThreadReference.THREAD_STATUS_WAIT:
                return "THREAD_STATUS_WAIT";
            case ThreadReference.THREAD_STATUS_NOT_STARTED:
                return "THREAD_STATUS_NOT_STARTED";
            default:
                return "ERROR: Unknown ThreadReference status " + status;
        }
    }

    private static void check (String threadName, String check1, String check2) {
        ThreadReference testedThread = (ThreadReference)debuggeeClass.getValue(debuggeeClass.fieldByName(threadName));
        ObjectReference lockRef = (ObjectReference)debuggeeClass.getValue(debuggeeClass.fieldByName("lock"));

        // wait during waitTime until testedThread becomes waiting notification
        boolean waitingNotify = false;
        if (debuggee.VM().canGetMonitorInfo()) {
            try {
                long oldTime = System.currentTimeMillis();
                while (!waitingNotify && (System.currentTimeMillis() - oldTime) <= status005.waitTime) {

                    debuggee.VM().suspend();

                    Iterator waitingThreads = lockRef.waitingThreads().iterator();
                    while (waitingThreads.hasNext()) {
                        ThreadReference threadRef = (ThreadReference)waitingThreads.next();
                        if (threadRef.equals(testedThread)) {
                            waitingNotify = true;
                            display("ObjectReference.waitingThreads() contains expected reference to " + threadName);
                        }
                    }

                    debuggee.VM().resume();

                    if (!waitingNotify) {
                        try {
                            Thread.currentThread().sleep(1000);
                        } catch (InterruptedException e) {
                            display("Debugger's main thread was interrupted while sleeping ");
                        }
                    }
                }
            } catch (Exception e) {
                complain("Unexpected exception while waiting until " + threadName + " set on waiting state: " + e);
                exitStatus = Consts.TEST_FAILED;
            }
        } else {
            display("WARNING: ObjectReference.waitingThreads() was not checked for " + threadName);
            waitingNotify = true;
        }
        if (!waitingNotify) {
            complain("Tested " + threadName + " did not wait ");
            exitStatus = Consts.TEST_FAILED;

        } else {
            int threadStatus = testedThread.status();

            if (threadStatus == ThreadReference.THREAD_STATUS_WAIT) {
                display(check1 + " PASSED");
                display("\t ThreadReference.status() returned expected status THREAD_STATUS_WAIT for unsuspended " + threadName);
            } else {
                complain(check1 + " FAILED");
                complain("\t ThreadReference.status() returned unexpected status " +
                    getThreadStatusName(threadStatus) + " for unsuspended " + threadName);
                complain("\t Expected status : " + getThreadStatusName(ThreadReference.THREAD_STATUS_WAIT));
                exitStatus = Consts.TEST_FAILED;
            }

            try {
                testedThread.suspend();
                threadStatus = testedThread.status();
                if (threadStatus == ThreadReference.THREAD_STATUS_WAIT) {
                    display(check2 + " PASSED");
                    display("\t ThreadReference.status() returned expected status THREAD_STATUS_WAIT for suspended " + threadName);
                } else {
                    complain(check2 + " FAILED");
                    complain("\t ThreadReference.status() returned unexpected status " +
                        getThreadStatusName(threadStatus)+ " for suspended " + threadName);
                    complain("\t Expected status : " + getThreadStatusName(ThreadReference.THREAD_STATUS_WAIT));
                    exitStatus = Consts.TEST_FAILED;
                }
                testedThread.resume();
            } catch (Exception e) {
                complain(check2 + " FAILED");
                complain("\t Unexpected exception while calling ThreadReference.suspend for " + threadName + " : " + e);
                exitStatus = Consts.TEST_FAILED;
            }
        }
    }
}
//--------------------------------------------------------- test specific classes
