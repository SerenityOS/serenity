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

package nsk.jdi.ThreadReference.stop;

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
 * <code>com.sun.jdi.ThreadReference.stop()</code>              <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The test works as follows. After being started up,                   <BR>                                            <BR>
 * the debuggee creates a 'lockingObject' for synchronizing threads,    <BR>
 * enters a synchronized block in which it creates new thread, thread2, <BR>
 * informs the debugger of the thread2 creation,                        <BR>
 * and is waiting for reply.                                            <BR>
 * Since the thread2 uses the same locking object in its 'run' method   <BR>
 * it is locked up until getting Exception from the debugger.           <BR>
 * Upon the receiption a message from the debuggee,                     <BR>
 * the debugger gets a ThreadReference thread2 mirroring the thread2 and <BR>
 * an ObjectReference throwableObj mirroring a special Throwable object <BR>
 * prepared by the main thread in the debuggee,                         <BR>
 * (note: for identification, throwableObj.getMessage() returns         <BR>
 *        the string "testException")                                   <BR>
 * and invokes thread2.stop(throwableObj) which should throw exception  <BR>
 * in the locked thread2 in the debugger.                               <BR>
 * In catch subclouse, the thread2 assigns Exception it got to a special<BR>
 * variable tObj of the Throwable type and ends its running.            <BR>
 * The main thread reads tObj.message(), compares to "testException",   <BR>
 * and informs the debugger of strings match or mismatch.               <BR>
 */

public class stop001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/ThreadReference/stop/stop001  ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new stop001().runThis(argv, out);
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
        "nsk.jdi.ThreadReference.stop.stop001a";

    private String testedClassName =
        "nsk.jdi.ThreadReference.stop.Threadstop001a";

    //String mName = "nsk.jdi.ThreadReference.stop";

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

            String threadName = "Thread2";

            List            allThreads    = null;
            ListIterator    listIterator  = null;
            List            classes       = null;
            ReferenceType mainthreadClass = null;
            ObjectReference throwableObj  = null;


            label0: {

                log2("getting ThreadReference objects and setting up breakponts");
                try {
                    allThreads  = vm.allThreads();
                    classes     = vm.classesByName(debuggeeName);
                    mainthreadClass = (ReferenceType) classes.get(0);
                } catch ( Exception e) {
                    log3("ERROR: Exception at very beginning !? : " + e);
                    expresult = returnCode1;
                    break label0;
                }

                listIterator = allThreads.listIterator();
                for (;;) {
                    try {
                        thread2 = (ThreadReference) listIterator.next();
                        if (thread2.name().equals(threadName))
                            break ;
                    } catch ( NoSuchElementException e ) {
                        log3("ERROR: NoSuchElementException for listIterator.next()");
                        log3("ERROR: NO THREAD2 ?????????!!!!!!!");
                        expresult = returnCode1;
                        break label0;
                    }
                }
            }


            label1: {
                if (expresult != returnCode0)
                    break label1;


                try {
                    log2("      getting a mirror of the throwableObj");
                    throwableObj = (ObjectReference)
                        mainthreadClass.getValue(mainthreadClass.fieldByName("throwableObj"));

                    log2("      stopping the thread2");
                    thread2.stop(throwableObj);

                } catch ( InvalidTypeException e1 ) {
                    log3("ERROR: InvalidTypeException ???");
                    expresult = returnCode1;
                } catch ( Exception e2 ) {
                    log3("ERROR: unexpected exception: " + e2);
                    expresult = returnCode1;
                }

                log2("......instructing mainThread to leave synchronized block");
                pipe.println("continue");

                log2("......getting result from mainThread:");
                line = pipe.readln();
                log2("       returned string is: " + line);
                if (line.equals("null")) {
                    log3("ERROR: 'stop001a.tObj = e1;' was not assigned");
                    expresult = returnCode1;
                } else if (line.equals("equal")) {
                } else if (line.equals("NOT_equal")) {
                    log3("ERROR: in the debugee, e1 is not 'LineUnavailableException'");
                    expresult = returnCode1;
                } else {
                    log3("ERROR: returned string is unexpected");
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
