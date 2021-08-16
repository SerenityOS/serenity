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
//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! templete parameters !!!!

// ThreadReference  Interface Name      ThreadReference
// threadGroup  Method Name             threadGroup
// threadgroup001  Test Name            threadgroup001
// ------------------------------------------------------

package nsk.jdi.ThreadReference.threadGroup;

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
 * <code>com.sun.jdi.ThreadReference.threadGroup()</code>       <BR>
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
 * the debugger gets mirrors of threadGroups and running threads,       <BR>
 * selects mirrors of Thread2,3,4, and checks up that :                 <BR>
 *  - object returned by Thread4.threadGroup(), is equal to threadGroup4;<BR>
 *  - object returned by Thread3.threadGroup(), is equal to threadGroup3;<BR>
 *  - object returned by Thread2.threadGroup(), is equal to threadGroup2.<BR>
 */

public class threadgroup001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/ThreadReference/threadGroup/threadgroup001  ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new threadgroup001().runThis(argv, out);
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
        "nsk.jdi.ThreadReference.threadGroup.threadgroup001a";

    private String testedClassName =
        "nsk.jdi.ThreadReference.threadGroup.Threadthreadgroup001a";

    //String mName = "nsk.jdi.ThreadReference.threadGroup";

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

            ThreadReference thread2 = null;
            ThreadReference thread3 = null;
            ThreadReference thread4 = null;

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


                log2("      getting Thread 2,3,4 mirror objects");

                iterator = threads.listIterator();
                for ( int i2 = 0; iterator.hasNext(); i2++ ) {
                    thread = (ThreadReference) iterator.next();
                    threadName = thread.name();
                    if (threadName.equals(threadNames[0]))
                         thread2 = thread;
                    else if (threadName.equals(threadNames[1]))
                         thread3 = thread;
                    else if (threadName.equals(threadNames[2]))
                         thread4 = thread;
                }


                log2("......checking up Thread2's group");
                if (!thread2.threadGroup().equals(groups[1])) {
                    log3("ERROR: !thread2.threadGroup.equals(groups[1])");
                    expresult = 1;
                }

                log2("......checking up Thread3's group");
                if (!thread3.threadGroup().equals(groups[2])) {
                    log3("ERROR: !thread3.threadGroup.equals(groups[2])");
                    expresult = 1;
                }


                log2("......checking up Thread4's group");
                if (!thread4.threadGroup().equals(groups[3])) {
                    log3("ERROR: !thread4.threadGroup.equals(groups[3])");
                    expresult = 1;
                }


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
