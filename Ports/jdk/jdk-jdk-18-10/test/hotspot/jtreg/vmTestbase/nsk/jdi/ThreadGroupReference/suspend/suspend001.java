/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ThreadGroupReference.suspend;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * ThreadGroupReference.                                        <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.ThreadGroupReference.suspend()</code>      <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The case for testing includes four ThreadGroup objects defined       <BR>
 * in a debuggee and their mirrors in a debugger.                       <BR>
 * Parenthood relationships between the objects are as follows:         <BR>
 *    threadGroups 2&3 are subgroups of threadGroup1                    <BR>
 *    threadGroup4 is a subgroup of threadGroup3                        <BR>
 * The objects are created together with three threads, Thread 2,3,4,   <BR>
 * belonging to the corresponding subgroups.                            <BR>
 * The test works as follows. After being started up,                   <BR>                                            <BR>
 * the debuggee creates a 'lockingObject' for synchronizing threads,    <BR>
 * enters a synchronized block in which it creates the threads,         <BR>
 * informs the debugger of the threads creation,                        <BR>
 * and is waiting for reply.                                            <BR>
 * Since the threads use the same locking object in their 'run' methods <BR>
 * they are locked up until main thread leaves the synchronized block.  <BR>
 * Upon the receiption a message from the debuggee,                     <BR>
 * the debugger gets mirrors of threadGroups and checks up that:        <BR>
 *  - group4.suspend(); suspends only Thread4;                          <BR>
 *  - group3.suspend(); suspends Thread3 and Thread4;                   <BR>
 *  - group2.suspend(); suspends only Thread2;                          <BR>
 *  - group1.suspend(); suspends Thread2, Thread3 and Thread4.          <BR>
 */

public class suspend001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/ThreadGroupReference/suspend/suspend001  ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new suspend001().runThis(argv, out);
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

    private String debuggeeName =
        "nsk.jdi.ThreadGroupReference.suspend.suspend001a";

    private String testedClassName =
        "nsk.jdi.ThreadGroupReference.suspend.Threadsuspend001a";

    //String mName = "nsk.jdi.ThreadGroupReference.suspend";

    //====================================================== test program
    //------------------------------------------------------ common section

    static ArgumentHandler      argsHandler;

    static int waitTime;

    static VirtualMachine      vm  = null;

    ReferenceType     testedclass  = null;
    ThreadReference   thread2      = null;
    ThreadReference   mainThread   = null;

    static int  testExitCode = PASSED;

    static final int returnCode0 = 0;
    static final int returnCode1 = 1;
    static final int returnCode2 = 2;
    static final int returnCode3 = 3;
    static final int returnCode4 = 4;

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


        IOPipe pipe     = new IOPipe(debuggee);

        debuggee.redirectStderr(out);
        log2(debuggeeName + " debuggee launched");
        debuggee.resume();

        String line = pipe.readln();
        if ((line == null) || !line.equals("ready")) {
            log3("signal received is not 'ready' but: " + line);
            return FAILED;
        } else {
            log2("'ready' recieved");
        }

        vm = debuggee.VM();

    //------------------------------------------------------  testing section
        log1("      TESTING BEGINS");

        for (int i = 0; ; i++) {

            pipe.println("newcheck");
            line = pipe.readln();

            if (line.equals("checkend")) {
                log2("     : returned string is 'checkend'");
                break ;
            } else if (!line.equals("checkready")) {
                log3("ERROR: returned string is not 'checkready'");
                testExitCode = FAILED;
                break ;
            }

            log1("new checkready: #" + i);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part

            int expresult = returnCode0;

            List            classes      = null;

            ThreadGroupReference groups[] = { null, null, null, null };

            String groupNames [] = {  "threadGroup1Obj",
                                      "threadGroup2Obj",
                                      "threadGroup3Obj",
                                      "threadGroup4Obj"  };

            List            threads;
            ListIterator    iterator;
            int             flag;
            String          threadName;
            ThreadReference thread;

            String threadNames [] = { "Thread2", "Thread3", "Thread4" };

            ReferenceType mainthreadClass = null;
            ReferenceType thread2Class    = null;

            label0: {

                log2("getting ThreadReference objects");
                try {
                    classes         = vm.classesByName(testedClassName);
                    thread2Class    = (ReferenceType) classes.get(0);
                    classes         = vm.classesByName(debuggeeName);
                    mainthreadClass = (ReferenceType) classes.get(0);
                } catch ( Exception e) {
                    log3("ERROR: Exception at very beginning !? : " + e);
                    expresult = returnCode1;
                    break label0;
                }

                log2("      getting a List of all threadGroups");
                for (int i1 = 0; i1 < 4; i1++) {
                    groups[i1] = (ThreadGroupReference)
                             mainthreadClass.getValue(mainthreadClass.fieldByName(groupNames[i1]));
                }

                log2("      getting a List of all running threads");
                threads = vm.allThreads();


                log2("......checking up threads suspended with groups[3].suspend()");
                log2("         expected: 'Thread4'");
                groups[3].suspend();

                iterator = threads.listIterator();
                flag = 0;
                for ( int i2 = 0; iterator.hasNext(); i2++ ) {
                    thread = (ThreadReference) iterator.next();
                    if (!thread.isSuspended())
                        continue;
                    threadName = thread.name();
                    if (threadName.equals(threadNames[0]))
                         flag |= 1;
                    else if (threadName.equals(threadNames[1]))
                         flag |= 2;
                    else if (threadName.equals(threadNames[2]))
                         flag |= 4;
                    else flag |= 8;
                }
                if (flag != 4) {
                    expresult = returnCode1;
                    if (flag == 0)
                        log3("ERROR: no threads suspunded");
                    if ((flag & 4) == 0)
                        log3("ERROR: 'Thread4' is not among suspended threads");
                    if ((flag & 2) != 0)
                        log3("ERROR: 'Thread3' is also suspended");
                    if ((flag & 1) != 0)
                        log3("ERROR: 'Thread2' is also suspended");
                    if (flag >= 8)
                        log3("ERROR: some extra thread(s) is also suspended");
                }
                vm.resume();


                log2("......checking up threads suspended with groups[2].suspend()");
                log2("         expected: 'Thread3' and 'Thread4'");
                groups[2].suspend();

                iterator = threads.listIterator();
                flag = 0;
                for ( int i2 = 0; iterator.hasNext(); i2++ ) {
                    thread = (ThreadReference) iterator.next();
                    if (!thread.isSuspended())
                        continue;
                    threadName = thread.name();
                    if (threadName.equals(threadNames[0]))
                         flag |= 1;
                    else if (threadName.equals(threadNames[1]))
                         flag |= 2;
                    else if (threadName.equals(threadNames[2]))
                         flag |= 4;
                    else flag |= 8;
                }
                if (flag != 6) {
                    expresult = returnCode1;
                    if (flag == 0)
                        log3("ERROR: no threads suspunded");
                    if ((flag & 4) == 0)
                        log3("ERROR: 'Thread4' is not among suspended threads");
                    if ((flag & 2) == 0)
                        log3("ERROR: 'Thread3' is not among suspended threads");
                    if ((flag & 1) != 0)
                        log3("ERROR: 'Thread2' is also suspended");
                    if (flag >= 8)
                        log3("ERROR: some extra thread(s) is also suspended");
                }
                vm.resume();


                log2("......checking up threads suspended with groups[1].suspend()");
                log2("         expected: 'Thread2'");
                groups[1].suspend();

                iterator = threads.listIterator();
                flag = 0;
                for ( int i2 = 0; iterator.hasNext(); i2++ ) {
                    thread = (ThreadReference) iterator.next();
                    if (!thread.isSuspended())
                        continue;
                    threadName = thread.name();
                    if (threadName.equals(threadNames[0]))
                         flag |= 1;
                    else if (threadName.equals(threadNames[1]))
                         flag |= 2;
                    else if (threadName.equals(threadNames[2]))
                         flag |= 4;
                    else flag |= 8;
                }
                if (flag != 1) {
                    expresult = returnCode1;
                    if (flag == 0)
                        log3("ERROR: no threads suspunded");
                    if ((flag & 4) != 0)
                        log3("ERROR: 'Thread4' is among suspended threads");
                    if ((flag & 2) != 0)
                        log3("ERROR: 'Thread3' is among suspended threads");
                    if ((flag & 1) == 0)
                        log3("ERROR: 'Thread2' is not among suspended threads");
                    if (flag >= 8)
                        log3("ERROR: some extra thread(s) is also suspended");
                }
                vm.resume();

                log2("......checking up threads suspended with groups[0].suspend()");
                log2("         expected: 'Thread2', 'Thread3', and 'Thread4'");
                groups[0].suspend();

                iterator = threads.listIterator();
                flag = 0;
                for ( int i2 = 0; iterator.hasNext(); i2++ ) {
                    thread = (ThreadReference) iterator.next();
                    if (!thread.isSuspended())
                        continue;
                    threadName = thread.name();
                    if (threadName.equals(threadNames[0]))
                         flag |= 1;
                    else if (threadName.equals(threadNames[1]))
                         flag |= 2;
                    else if (threadName.equals(threadNames[2]))
                         flag |= 4;
                    else flag |= 8;
                }
                if (flag != 7) {
                    expresult = returnCode1;
                    if (flag == 0)
                        log3("ERROR: no threads suspunded");
                    if ((flag & 4) == 0)
                        log3("ERROR: 'Thread4' is not among suspended threads");
                    if ((flag & 2) == 0)
                        log3("ERROR: 'Thread3' is not among suspended threads");
                    if ((flag & 1) == 0)
                        log3("ERROR: 'Thread2' is not among suspended threads");
                    if (flag >= 8)
                        log3("ERROR: some extra thread(s) is also suspended");
                }
                vm.resume();




                log2("......instructing mainThread to leave synchronized block");
                pipe.println("continue");
                line = pipe.readln();
                if (!line.equals("docontinue")) {
                    log3("ERROR: returned string is not 'docontinue'");
                    expresult = returnCode4;
                }
            }
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            log2("     the end of testing");
            if (expresult != returnCode0)
                testExitCode = FAILED;
        }
        log1("      TESTING ENDS");

    //--------------------------------------------------   test summary section
    //-------------------------------------------------    standard end section

        pipe.println("quit");
        log2("waiting for the debuggee to finish ...");
        debuggee.waitFor();

        int status = debuggee.getStatus();
        if (status != PASSED + PASS_BASE) {
            log3("debuggee returned UNEXPECTED exit status: " +
                    status + " != PASS_BASE");
            testExitCode = FAILED;
        } else {
            log2("debuggee returned expected exit status: " +
                    status + " == PASS_BASE");
        }

        if (testExitCode != PASSED) {
            logHandler.complain("TEST FAILED");
        }
        return testExitCode;
    }
}
