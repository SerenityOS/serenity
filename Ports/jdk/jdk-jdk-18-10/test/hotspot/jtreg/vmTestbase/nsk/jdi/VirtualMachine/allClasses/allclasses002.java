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

package nsk.jdi.VirtualMachine.allClasses;

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
 * <code>com.sun.jdi.VirtualMachine.allClasses()</code>         <BR>
 * complies with its spec.                                      <BR>
 * The test check up that class array type, interface array     <BR>
 * type, and all primitive array types are in a List returned   <BR>
 * by the method.                                               <BR>
 * <BR>
 */

public class allclasses002 {

    //----------------------------------------------------- template section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- template parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/VirtualMachine/allClasses/allclasses002",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new allclasses002().runThis(argv, out);
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
        "nsk.jdi.VirtualMachine.allClasses.allclasses002a";

    private String namePrefix =
        "nsk.jdi.VirtualMachine.allClasses.";

    //====================================================== test program
    //------------------------------------------------------ common section

    static ArgumentHandler      argsHandler;

    static int waitTime;

    static VirtualMachine vm  = null;

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
        log2("allclasses002a debuggee launched");
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

        log1("     TESTING BEGINS");

        for (int i = 0; ; i++) {
            pipe.println("newcheck");
            line = pipe.readln();

            if (line.equals("checkend")) {
                log1("     : returned string is 'checkend'");
                break ;
            } else if (!line.equals("checkready")) {
                log3("ERROR: returned string is not 'checkready'");
                testExitCode = FAILED;
                break ;
            }


            log1("new check: #" + i);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part

            String name[] = { "allclasses002aClassForCheck[]",
                              "allclasses002aInterfaceForCheck[]",
                              "boolean[][][][]",
                              "byte[][][][]",
                              "char[][][][]",
                              "double[][][][]",
                              "float[][][][]",
                              "int[][][][]",
                              "long[][][][]",
                              "short[][][][]" };


            log2("        check for loaded " + namePrefix + name[0] + " type");
            if (refTypeYes(namePrefix + name[0]) == 0) {
                log3("ERROR: no " + namePrefix + name[0] + " type in the List");
                testExitCode = FAILED;
            }

            log2("        check for loaded " + namePrefix + name[1] + " type");
            if (refTypeYes(namePrefix + name[1]) == 0) {
                log3("ERROR: no " + namePrefix + name[1] + " type in the List");
                testExitCode = FAILED;
            }

            for (int i1 = 2; i1 < 10; i1++) {

                log2("......check for loaded " + name[i1] + " type");
                if (refTypeYes(name[i1]) == 0) {
                    log3("ERROR: no " + name[i1] + " type in the List");
                    testExitCode = FAILED;
                }
            }
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }

        log1("     TESTING ENDS");

    //--------------------------------------------------   test summary section
    //-------------------------------------------------    standard end section

        pipe.println("quit");
        log2("waiting for the debuggee finish ...");
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

    int refTypeYes ( String name ) {

        int retCode = 0;

        List vmClasses = vm.allClasses();
        ListIterator li = vmClasses.listIterator();

        log2("loaded '" + namePrefix + "' types:");
        for ( int i = 0; li.hasNext(); i++) {
            ReferenceType obj = (ReferenceType) li.next();

            if (obj.name().startsWith("nsk.jdi.VirtualMachine"))
                log2("        " + i + " : " + obj.name() );

            if (obj.name().endsWith("[][][][]"))
                log2("        " + i + " : " + obj.name() );

            if ( obj.name().equals(name) )
                retCode = 1;
        }
        return retCode;
    }

}
