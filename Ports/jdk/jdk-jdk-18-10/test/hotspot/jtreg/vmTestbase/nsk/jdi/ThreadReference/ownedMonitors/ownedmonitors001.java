/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ThreadReference.ownedMonitors;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * ThreadReference.                                             <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.ThreadReference.ownedMonitors()</code>     <BR>
 * complies with its spec.                                      <BR>
 */

public class ownedmonitors001 {

    //----------------------------------------------------- template section
    private static final int PASSED = 0;
    private static final int FAILED = 2;
    private static final int PASS_BASE = 95;

    private static final String sHeader1 = "==> debugger:      ";
    private static final String  sHeader2 = "--> debugger: ";
    private static final String  sHeader3 = "ERROR ##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new ownedmonitors001().runThis(argv, out);
    }

    //--------------------------------------------------   log procedures

    private static Log  logHandler;

    private static void log1(String message) {
        logHandler.display(sHeader1 + message);
    }
    private static void log2(String message) {
        logHandler.display(sHeader2 + message);
    }
    private static void log3(String message) {
        logHandler.complain(sHeader3 + message);
    }

    //  ************************************************    test parameters

    private static final String debuggeeName = "nsk.jdi.ThreadReference.ownedMonitors.ownedmonitors001a";
//    private static String testedClassName = "nsk.jdi.ThreadReference.ownedMonitors.Threadownedmonitors001a";

    //====================================================== test program
    //------------------------------------------------------ common section

    private static ArgumentHandler argsHandler;
    private static int waitTime;
    private static VirtualMachine vm = null;
    private static int testExitCode = PASSED;
    private static ReferenceType debuggeeRef;

    private static Vector<ObjectReference> expMonitors = new Vector<ObjectReference>();
    private static Vector<String> monitorNames = new Vector<String>();
    private static final int monitorCount = 2;

    //------------------------------------------------------ methods

    private int runThis (String argv[], PrintStream out) {

        Debugee debuggee;

        argsHandler     = new ArgumentHandler(argv);
        logHandler      = new Log(out, argsHandler);
        Binder binder   = new Binder(argsHandler, logHandler);

        if (argsHandler.verbose()) {
            debuggee = binder.bindToDebugee(debuggeeName + " -vbs");
        } else {
            debuggee = binder.bindToDebugee(debuggeeName);
        }

        waitTime = argsHandler.getWaitTime();
        IOPipe pipe     = debuggee.createIOPipe();

        debuggee.redirectStderr(logHandler, "debuggee.err > ");
        log2("debuggee launched");
        debuggee.resume();

        String line = pipe.readln();
        if ((line == null) || !line.equals("ready")) {
            log3("Signal received is not 'ready' but: " + line);
            return FAILED;
        } else {
            log2("'ready' recieved");
        }

        vm = debuggee.VM();

        log2("TESTING BEGINS");
        label1: {
            debuggeeRef = debuggee.classByName(debuggeeName);
            if (debuggeeRef == null) {
                log3("Cannot find ReferenceType for " + debuggeeName);
                testExitCode = FAILED;
                break label1;
            }

            for (int i2 = 0; ; i2++) {
                if (!vm.canGetOwnedMonitorInfo()) {
                    log1("TEST ABORTED: vm.canGetOwnedMonitorInfo() returned false.");
                    break label1;
                }

                pipe.println("newcheck");
                line = pipe.readln();

                if (line.equals("checkend")) {
                    log2("'checkend' received");
                    break ;
                } else if (!line.equals("checkready")) {
                    log3("Returned string is not 'checkready'");
                    testExitCode = FAILED;
                    break ;
                }

                log2("Case #" + i2);
                switch (i2) {

                    case 0 :
                        checkMonitors("main", 0);
                        break;

                    case 1 :
                        checkMonitors("main", monitorCount);
                        break;

                    default:
                        log3("default:");
                        break label1;
                }

                pipe.println("continue");
                line = pipe.readln();
                if (!line.equals("docontinue")) {
                    log3("Returned string is not 'docontinue'");
                    testExitCode = FAILED;
                    break label1;
                }

            }  // for
        }
        log2("TESTING ENDS");

    //--------------------------------------------------   test summary section
    //-------------------------------------------------    standard end section

        pipe.println("quit");
        log1("waiting for the debuggee to finish ...");
        debuggee.waitFor();

        int status = debuggee.getStatus();
        if (status != PASSED + PASS_BASE) {
            log3("debuggee returned UNEXPECTED exit status: " +
                    status + " != PASS_BASE");
            testExitCode = FAILED;
        } else {
            log1("debuggee returned expected exit status: " +
                    status + " == PASS_BASE");
        }

        if (testExitCode != PASSED) {
            logHandler.complain("TEST FAILED");
        }
        return testExitCode;
    }

    private void checkMonitors(String threadName, int expSize) {
        log1("Getting ThreadReference for " + threadName + " thread");
        ThreadReference checkedThread = null;
        Iterator itr = vm.allThreads().listIterator();
        while (itr.hasNext()) {
             ThreadReference thread = (ThreadReference) itr.next();
             if (thread.name().equals(threadName)) {
                  checkedThread = thread;
             }
        }
        if (checkedThread == null) {
            log3("Cannot find  " + threadName + "thread in the debuggee");
            testExitCode = FAILED;
            return;
        }
        log1("Checking up throwing an IncompatibleThreadStateException for not suspended thread");
        List monitors;
        try {
            monitors = checkedThread.ownedMonitors();
            log3("No IncompatibleThreadStateException for " + threadName + " thread");
            testExitCode = FAILED;
        } catch ( IncompatibleThreadStateException e1 ) {
            log1("Expected IncompatibleThreadStateException is thrown");
        }

        log1("Suspending the " + threadName + " thread");
        checkedThread.suspend();

        log1("Checking up ownedMonitors() list");
        try {
            monitors = checkedThread.ownedMonitors();
            int monSize = monitors.size();
            if (monSize < expSize) {
                log3("Got unexpected ownedMonitors() list size : " + monSize +
                      "\n\t expected minimal value : " + expSize);
                testExitCode = FAILED;
            } else {
                log1("Got size of ownedMonitors() list: " + monSize);

                if (expSize > 0) {
                    log1("Checking up items in ownedMonitors() list");
                    getMonitorRefs();
                    itr = expMonitors.iterator();
                    while (itr.hasNext()) {
                        ObjectReference mon = (ObjectReference) itr.next();
                        if (monitors.contains(mon)) {
                            log1("Found expected item in ownedMonitors() list : " + mon);
                        } else {
                            log3("Did not found expected item in ownedMonitors() list : " + mon);
                            testExitCode = FAILED;
                        }
                    }
                }
            }
        } catch ( IncompatibleThreadStateException e1 ) {
            log3("Unexpected IncompatibleThreadStateException is thrown");
            testExitCode = FAILED;
        }
        log1("Resuming the main thread");
        checkedThread.resume();
    }

    private void getMonitorRefs () {
        monitorNames.add("waitnotifyObj");
        monitorNames.add("lockingObject");
        Iterator itr = monitorNames.iterator();
        while (itr.hasNext()) {
            try {
                String monName = (String)itr.next();
                Field field = debuggeeRef.fieldByName(monName);
                Value value = debuggeeRef.getValue(field);
                expMonitors.add((ObjectReference)value);
            } catch (Exception e) {
                log3("Unexpected excption while getting ObjectReference for monitors");
                testExitCode = FAILED;
            }
        }
    }
}
