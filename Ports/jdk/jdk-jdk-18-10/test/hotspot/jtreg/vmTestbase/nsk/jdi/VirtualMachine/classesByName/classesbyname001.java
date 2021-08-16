/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.VirtualMachine.classesByName;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * VirtualMachine.                                              <BR>
 * <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.VirtualMachine.classesByName()</code>      <BR>
 * complies with the spec for                                   <BR>
 * <code>com.sun.jdi.classesByName</code> methods               <BR>
 * <BR>
 * The cases for testing are as follows :                       <BR>
 *                                                                      <BR>
 * debuggee             debugger                                        <BR>
 * classes loaded                                                       <BR>
 *                                                                      <BR>
 * no                   vm.classesByName(".Class1ForCheck")    ==0      <BR>
 *                                                                      <BR>
 * ".ClassForCheck"     vm.classesByName(".Class1ForCheck")    ==1      <BR>
 *                      vm.classesByName(".Class2ForCheck")    ==0      <BR>
 *                      vm.classesByName(".InterfaceForCheck") ==0      <BR>
 *                                                                      <BR>
 * ".Class2ForCheck"    vm.classesByName(".Class2ForCheck")    ==1      <BR>
 *                      vm.classesByName(".InterfaceForCheck") ==1      <BR>
 *                                                                      <BR>
 * "Class2ForCheck"     vm.classesByName(".Class2ForCheck")    ==0      <BR>
 */

public class classesbyname001 {

    //----------------------------------------------------- template section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- template parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/VirtualMachine/classesByName/classesbyname001",
    sHeader2 = "--> classesbyname001: ",
    sHeader3 = "##> classesbyname001: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new classesbyname001().runThis(argv, out);
    }

     //--------------------------------------------------   log procedures

    //private static boolean verbMode = false;

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

    private String debugeeName =
        "nsk.jdi.VirtualMachine.classesByName.classesbyname001a";

    //====================================================== test program

    static ArgumentHandler      argsHandler;
    static int                  testExitCode = PASSED;

    //------------------------------------------------------ common section

    private int runThis (String argv[], PrintStream out) {

        Debugee debugee;

        argsHandler     = new ArgumentHandler(argv);
        logHandler      = new Log(out, argsHandler);
        Binder binder   = new Binder(argsHandler, logHandler);

        if (argsHandler.verbose()) {
            debugee = binder.bindToDebugee(debugeeName + " -vbs");  // *** tp
        } else {
            debugee = binder.bindToDebugee(debugeeName);            // *** tp
        }

        IOPipe pipe     = new IOPipe(debugee);

        debugee.redirectStderr(out);
        log2("classesbyname001a debugee launched");
        debugee.resume();

        String line = pipe.readln();
        if ((line == null) || !line.equals("ready")) {
            log3("signal received is not 'ready' but: " + line);
            return FAILED;
        } else {
            log2("'ready' recieved");
        }

        VirtualMachine vm = debugee.VM();

    //------------------------------------------------------  testing section
        log1("TESTING BEGINS");

        for (int i = 0; ; i++) {
            pipe.println("newcheck");
            line = pipe.readln();

            if (line.equals("checkend")) {
                log3("     : returned string is 'checkend'");
                break ;
            } else if (!line.equals("checkready")) {
                log3("ERROR: returned string is not 'checkready'");
                testExitCode = FAILED;
                break ;
            }

            String mName =
                "nsk.jdi.VirtualMachine.classesByName";

            log2("new check: #" + i);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part

            List listVMClasses = null;
            int expN = 0;

            switch (i) {

                // no attempt is made to load a class of given name

                case 0:
                        listVMClasses = vm.classesByName(mName +
                                                        ".Class1ForCheck");
                        expN = 0;       // O#=0; I#=0;
                        break;

                case 1:
                        listVMClasses = vm.classesByName(mName +
                                                        ".Class1ForCheck");
                        expN = 0;       // O#=0; I#=0;
                        break;


                // Returns the loaded reference types that match a given name

                case 2:
                        listVMClasses = vm.classesByName(mName +
                                                        ".Class1ForCheck");
                        expN = 1;       // O#=1; I#=0;
                        break;
                case 3:
                        listVMClasses = vm.classesByName(mName +
                                                        ".Class2ForCheck");
                        expN = 0;       // O#=0; I#=0;
                        break;
                case 4:
                        listVMClasses = vm.classesByName(mName +
                                                        ".InterfaceForCheck");
                        expN = 0;       // O#=0; I#=0;
                        break;
                case 5:
                        listVMClasses = vm.classesByName(mName +
                                                        ".Class2ForCheck");
                        expN = 1;       // O#=1; I#=1;
                        break;
                case 6:
                        listVMClasses = vm.classesByName(mName +
                                                        ".InterfaceForCheck");
                        expN = 1;       // O#=1; I#=1;
                        break;


                // not a fully qualified name

                case 7:
                        listVMClasses = vm.classesByName("Class2ForCheck");
                        expN = 0;       // O#=2; I#=3?;
                        break;


                default:
                        break ;
            }

            if (listVMClasses.size() != expN) {
                log3("ERROR: size of returned list != expN but == " +
                        listVMClasses.size() + "; check # = " + i);
                testExitCode = FAILED;
            } else {
                log2("size of returned list == expN ; check # = " + i);
            }

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
        log1("TESTING ENDS");

    //--------------------------------------------------   test summary section



    //-------------------------------------------------    standard end section

        pipe.println("quit");
        log2("waiting for the debugee finish ...");
        debugee.waitFor();

        int status = debugee.getStatus();
        if (status != PASSED + PASS_BASE) {
            log3("ERROR: debugee returned UNEXPECTED exit status: " +
                   status + " != PASS_BASE");
            testExitCode = FAILED;
        } else {
            log2("debugee returned expected exit status: " +
                   status + " == PASS_BASE");
        }

        if (testExitCode != PASSED) {
            logHandler.complain("TEST FAILED");
        }
        return testExitCode;
    }
}
