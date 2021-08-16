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

package nsk.jdi.VirtualMachine.dispose;

import jdk.test.lib.Utils;
import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * VirtualMachine.                                              <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.VirtualMachine.dispose()</code>            <BR>
 * complies with its specification.                             <BR>
 * The test checks up that after call to VirtualMachine.dispose(),<BR>
 * debuggee's thread suspended before the call by a debugger,   <BR>
 * with method ThreadReference.suspend(), is resumed and runs.  <BR>
 * <BR>
 * The test work as follows.                                    <BR>
 * Upon launch the debuggee creates new thread which is waiting <BR>
 * until a main thread leaves a synchronized block, and informs <BR>
 * the debugger of the thread creation. The debugger suspends   <BR>
 * the tested thread, performs vm.dispose() and sleeps          <BR>
 * for a predefined time-test parameter. Then the debugger      <BR>
 * asks the debuggee to check up on the tested thread state     <BR>
 * which should be "not alive" if the thread was resumed.       <BR>
 */

public class dispose003 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/VirtualMachine/dispose/dispose003  ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new dispose003().runThis(argv, out);
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
        "nsk.jdi.VirtualMachine.dispose.dispose003a";

    private String testedClassName =
        "nsk.jdi.VirtualMachine.dispose.Threaddispose003a";

    //String mName = "nsk.jdi.VirtualMachine.dispose";

    //====================================================== test program
    //------------------------------------------------------ common section

    static ArgumentHandler      argsHandler;

    static int waitTime;

    static VirtualMachine      vm            = null;

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


            String threadName = "testedThread";

            List            allThreads   = null;
            ListIterator    listIterator = null;
            List            classes      = null;


            label0: {

                log2("getting ThreadReference object");
                try {
                    allThreads  = vm.allThreads();
                    classes     = vm.classesByName(testedClassName);
                    testedclass = (ReferenceType) classes.get(0);
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

                log2("      suspending thread2");
                thread2.suspend();


                log2("      vm.dispose()");
                vm.dispose();

                log2("......forcing the main thread to leave synchronized block");
                pipe.println("continue");
                line = pipe.readln();
                if (!line.equals("docontinue")) {
                    log3("ERROR: returned string is not 'docontinue'");
                    expresult = returnCode4;
                }

                if (expresult != returnCode0)
                    break label1;

                log2("      Waiting for thread2 is not alive");

                Utils.waitForCondition(
                        () -> {
                            log2("......sending to the debuggee: 'check_alive'");
                            log2("       expected reply: 'not_alive'");
                            pipe.println("check_alive");
                            String reply = pipe.readln();
                            if (reply.equals("alive")) {
                                log3("ERROR: thread2 is alive");
                                return false;
                            } else if (reply.equals("not_alive")) {
                                log2("     thread2 is not alive");
                                return true;
                            } else {
                                log3("ERROR: unexpected reply: " + reply);
                                throw new RuntimeException("ERROR: unexpected reply: " + reply);
                            }
                        },
                        Utils.adjustTimeout(waitTime * 60000),
                        1000);
                pipe.println("check_done");

            }

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
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
