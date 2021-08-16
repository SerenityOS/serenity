/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ObjectReference.waitingThreads;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import java.io.*;
import java.util.*;
import java.util.jar.*;

/**
 */
public class waitingthreads003 {

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

    private final static String prefix = "nsk.jdi.ObjectReference.waitingThreads.";
    private final static String className = "waitingthreads003";
    private final static String debuggerName = prefix + className;
    private final static String debuggeeName = debuggerName + "a";

    //------------------------------------------------------- test specific fields

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

        try {
            debuggee = Debugee.prepareDebugee(argHandler, log, debuggeeName);
            debuggeeClass = debuggee.classByName(debuggeeName);
            if ( debuggeeClass == null ) {
                throw new Failure("Class '" + debuggeeName + "' not found.");
            } else {
                execTest();
            }
        } catch (Exception e) {
            exitStatus = Consts.TEST_FAILED;
            complain("Unexpected Exception: " + e.getMessage());
            e.printStackTrace(out);
        } finally {
            debuggee.quit();
        }

        return exitStatus;
    }

    //------------------------------------------------------ mutable common method

    private static void execTest() {

        debuggee.receiveExpectedSignal(SIGNAL_GO);

        if (debuggee.VM().canGetMonitorInfo()) {

            // Wait up to waitTime until all MyThreads will be blocked on entering in monitor
            int waitingCount = 0;
            long oldTime = System.currentTimeMillis();
            while ((System.currentTimeMillis() - oldTime) <= waitTime && waitingCount < waitingthreads003a.threadCount) {
                Iterator threads = debuggee.VM().allThreads().iterator();
                waitingCount = 0;
                while (threads.hasNext()) {
                    ThreadReference thread = (ThreadReference)threads.next();
                    if (thread.name().indexOf(waitingthreads003a.threadNamePrefix) >= 0 &&
                           thread.status() == ThreadReference.THREAD_STATUS_MONITOR ) {
                        waitingCount++;
                    }
                }
            }

            if (waitingCount < waitingthreads003a.threadCount) {
                exitStatus = Consts.TEST_FAILED;
                complain("After " + waitTime + " ms only " + waitingCount + " of " + waitingthreads003a.threadNamePrefix + "s " +
                         "\n\t are blocked on entering monitor. Expected count: " + waitingthreads003a.threadCount);
            } else {
                try {
                    debuggee.VM().suspend();

                    // check for waitingthreads003a.waitnotifyObj - no waiting threads expected
                    String fieldName = "waitnotifyObj";
                    display("CHECK1 : checking waitingThreads method for ObjectReference of waitingthreads003a." + fieldName);
                    ObjectReference objRef = (ObjectReference) debuggeeClass.getValue(debuggeeClass.fieldByName(fieldName));
                    try {
                        List waitingThreads = objRef.waitingThreads();
                        if (waitingThreads.size() != 0) {
                            exitStatus = Consts.TEST_FAILED;
                            complain("waitingThreads method returned non-zero size list for " + fieldName);
                        } else {
                            display("waitingThreads method returned expected list of zero size for " + fieldName);
                        }
                        if (exitStatus == Consts.TEST_PASSED) {
                            display("CHECK1 PASSED");
                        }
                    } catch (Exception e) {
                        throw new Failure("Unexpected exception while getting waitingThreads method's result for " + fieldName + " : " + e);
                    }

                    // check for waitingthreads003a.lockingObject - waiting threads expected
                    fieldName = "lockingObject";
                    display("CHECK2: checking waitingThreads method for ObjectReference of waitingthreads003a." + fieldName);
                    objRef = (ObjectReference) debuggeeClass.getValue(debuggeeClass.fieldByName(fieldName));
                    try {
                        List waitingThreads = objRef.waitingThreads();
                        if (waitingThreads.size() != waitingthreads003a.threadCount) {
                            exitStatus = Consts.TEST_FAILED;
                            complain("waitingThreads method returned list with unexpected size for " + fieldName +
                                "\n\t expected value : " + waitingthreads003a.threadCount + "; got one : " + waitingThreads.size());
                        } else {
                            // check waitingThreads list
                            Iterator itr = waitingThreads.iterator();
                            while (itr.hasNext()) {
                                ThreadReference thread = (ThreadReference)itr.next();
                                if (thread.name().indexOf(waitingthreads003a.threadNamePrefix) < 0) {
                                    exitStatus = Consts.TEST_FAILED;
                                    complain("waitingThreads returned list containing ThreadReference with unexpected name: " + thread.name());
                                } else {
                                    display("Expected ThreadReference is found in the returned list: " + thread.name());
                                }
                                if (!thread.currentContendedMonitor().equals(objRef)) {
                                    exitStatus = Consts.TEST_FAILED;
                                    complain("waitingThreads returned list contained ThreadReference with unexpected monitor: " + thread.currentContendedMonitor());
                                }
                            }
                        }
                        if (exitStatus == Consts.TEST_PASSED) {
                            display("CHECK2 PASSED");
                        }
                    } catch (Exception e) {
                        throw new Failure("Unexpected exception while getting waitingThreads method's result for " + fieldName + " : " + e);
                    }
                } finally {
                    debuggee.VM().resume();
                }
            }
        }
    }

    //--------------------------------------------------------- test specific methods

}
//--------------------------------------------------------- test specific classes
