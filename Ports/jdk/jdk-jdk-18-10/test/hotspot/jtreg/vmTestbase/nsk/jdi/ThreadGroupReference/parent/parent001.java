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

package nsk.jdi.ThreadGroupReference.parent;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

//import com.sun.jdi.event.*;
//import com.sun.jdi.request.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * ThreadGroupReference.                                        <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.ThreadGroupReference.parent()</code>       <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The case for testing includes four ThreadGroup objects defined       <BR>
 * in two debuggee's classes and in an external class,                  <BR>
 * and their mirrors in a debugger.                                     <BR>
 * Parenthood relationships between the objects are as follows:         <BR>
 *   mainParent -> mainthreadGroup -> thread2Group1 -> thread2Group2    <BR>
 * The objects are created together with two debuggee's threads,        <BR>
 * mainthread and a thread2.                                            <BR>
 * The test works as follows. After being started up,                   <BR>                                            <BR>
 * the debuggee creates a 'lockingObject' for synchronizing threads,    <BR>
 * enters a synchronized block in which it creates new thread,          <BR>
 * the thread2, informs a debugger of the thread2 creation,             <BR>
 * and is waiting for reply.                                            <BR>
 * Since the thread2 uses the same locking object in its 'run' method   <BR>
 * it is locked up until main thread leaves the synchronized block.     <BR>
 * Upon the receiption a message from the debuggee,                     <BR>
 * the debugger gets the mirrors of all the ThreadGroup objects,        <BR>
 * and checks up the above mentioned parenthood relationships.          <BR>
 */

public class parent001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/ThreadGroupReference/parent/parent001  ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new parent001().runThis(argv, out);
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
        "nsk.jdi.ThreadGroupReference.parent.parent001a";

    private String testedClassName =
        "nsk.jdi.ThreadGroupReference.parent.Threadparent001a";

    //String mName = "nsk.jdi.ThreadGroupReference.parent";

    //====================================================== test program
    //------------------------------------------------------ common section

    static ArgumentHandler      argsHandler;

    static int waitTime;

    static VirtualMachine      vm            = null;
    //static EventRequestManager eventRManager = null;
    //static EventQueue          eventQueue    = null;
    //static EventSet            eventSet      = null;

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

            List            classes      = null;

            ThreadGroupReference group0 = null;
            ThreadGroupReference group1 = null;
            ThreadGroupReference group2 = null;
            ThreadGroupReference group3 = null;

            String obj0Name = "obj0Name";
            String obj1Name = "obj1Name";
            String obj2Name = "obj2Name";
            String obj3Name = "obj3Name";

            String str = null;

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



                log2("      getting a ThreadGroupReference object for a parent of 'mainthreadGroup'");
                str = ( (StringReference)
                       thread2Class.getValue(thread2Class.fieldByName(obj0Name)) ).value();
                group0 = (ThreadGroupReference)
                    mainthreadClass.getValue(mainthreadClass.fieldByName(str));


                log2("      getting a ThreadGroupReference object for 'mainthreadGroup'");
                str = ( (StringReference)
                       thread2Class.getValue(thread2Class.fieldByName(obj1Name)) ).value();
                group1 = (ThreadGroupReference)
                    mainthreadClass.getValue(mainthreadClass.fieldByName(str));


                log2("      getting a ThreadGroupReference object for 'thread2Group1'");
                str = ( (StringReference)
                       thread2Class.getValue(thread2Class.fieldByName(obj2Name)) ).value();
                group2 = (ThreadGroupReference)
                    thread2Class.getValue(thread2Class.fieldByName(str));

                log2("      getting a ThreadGroupReference object for 'thread2Group2'");
                str = ( (StringReference)
                       thread2Class.getValue(thread2Class.fieldByName(obj3Name)) ).value();
                group3 = (ThreadGroupReference)
                    thread2Class.getValue(thread2Class.fieldByName(str));


                log2("      checking up the parent of 'thread2Group2'; 'thread2Group1' is expected");
                if (!group3.parent().equals(group2)) {
                    log3("ERROR: !group3.parent().equals(group2)");
                    expresult = returnCode1;
                }
                log2("      checking up the parent of 'thread2Group1'; 'mainthreadGroup' is expected");
                if (!group2.parent().equals(group1)) {
                    log3("ERROR: !group2.parent().equals(group1)");
                    expresult = returnCode1;
                }
                log2("      checking up the parent of 'mainthreadGroup'; 'mainParent' is expected");
                if (!group1.parent().equals(group0)) {
                    log3("ERROR: group1.parent().equals(group0) : " + group1.parent().name());
                    expresult = returnCode1;
                }


                log2("     instructing mainThread to leave synchronized block");
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
