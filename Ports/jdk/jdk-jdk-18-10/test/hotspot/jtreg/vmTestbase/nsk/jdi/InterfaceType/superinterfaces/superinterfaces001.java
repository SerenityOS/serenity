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

package nsk.jdi.InterfaceType.superinterfaces;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * InterfaceType.                                               <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.InterfaceType.superinterfaces()</code>     <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The cases for testing are as follows.                        <BR>
 *                                                              <BR>
 * 1)    Iface1 has no a superinterface                         <BR>
 * 2)    Iface2 has the superinterface Iface1 and               <BR>
 *           no indirect interfaces                             <BR>
 * 3)    Iface3 has the superinterface Iface2 and               <BR>
 *           the indirect interface Iface1                      <BR>
 * 4)    Iface4 has the superinterfaces Iface0 and Iface1       <BR>
 * <BR>
 */

public class superinterfaces001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/InterfaceType/superinterfaces/superinterfaces001",
    sHeader2 = "--> superinterfaces001: ",
    sHeader3 = "##> superinterfaces001: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new superinterfaces001().runThis(argv, out);
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

    private String debuggeeName =
        "nsk.jdi.InterfaceType.superinterfaces.superinterfaces001a";

    String mName = "nsk.jdi.InterfaceType.superinterfaces";

    //====================================================== test program

    static ArgumentHandler      argsHandler;
    static int                  testExitCode = PASSED;

    //------------------------------------------------------ common section

    private int runThis (String argv[], PrintStream out) {

        Debugee debuggee;

        argsHandler     = new ArgumentHandler(argv);
        logHandler      = new Log(out, argsHandler);
        Binder binder   = new Binder(argsHandler, logHandler);

        if (argsHandler.verbose()) {
            debuggee = binder.bindToDebugee(debuggeeName + " -vbs");  // *** tp
        } else {
            debuggee = binder.bindToDebugee(debuggeeName);            // *** tp
        }

        IOPipe pipe     = new IOPipe(debuggee);

        debuggee.redirectStderr(out);
        log2("superinterfaces001a debuggee launched");
        debuggee.resume();

        String line = pipe.readln();
        if ((line == null) || !line.equals("ready")) {
            log3("signal received is not 'ready' but: " + line);
            return FAILED;
        } else {
            log2("'ready' recieved");
        }

        VirtualMachine vm = debuggee.VM();

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

            log1("new check: #" + i);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part

            ReferenceType classRefType = null;
            ReferenceType reftype      = null;
            List          l            = null;
            String        name         = null;
            InterfaceType ifaceType    = null;
            InterfaceType itype        = null;

            int i2;

            for (i2 = 0; ; i2++) {

                int expresult = 0;

                log2("new check: #" + i2);

                switch (i2) {

                case 0:         // Iface1 - 0 superinterfaces

                        List list0 = vm.classesByName(mName + ".Class1ForCheck");

                        classRefType  = (ReferenceType) list0.get(0);

                        List iface0list =
                             ((ClassType) classRefType).interfaces();

                        if (iface0list.size() != 1) {
                            log3("ERROR : iface0list.size() != 1  in case: Iface1");
                            expresult = 1;
                            break;
                        }

                        ifaceType  = (InterfaceType) iface0list.get(0);
                        if (ifaceType.superinterfaces().size() != 0) {
                            log3("ERROR : ifaceType.superinterfaces().size() != 0  in case: Iface1");
                            expresult = 1;
                            break;
                        }

                        break;

                case 1:         // Iface2 - 1 superifaces, 0 indirect ifaces

                        List list1 = vm.classesByName(mName + ".Class2ForCheck");

                        classRefType  = (ReferenceType) list1.get(0);

                        List iface1list =
                             ((ClassType) classRefType).interfaces();

                        if (iface1list.size() != 1) {
                            log3("ERROR : iface1list.size() != 1  in case: Iface2");
                            expresult = 1;
                            break;
                        }

                        ifaceType  = (InterfaceType) iface1list.get(0);
                        l = ifaceType.superinterfaces();
                        if (l.size() != 1) {
                            log3("ERROR : ifaceType.superinterfaces().size() != 1  in case: Iface2");
                            expresult = 1;
                            break;
                        }

                        itype = (InterfaceType) l.get(0);
                        name = itype.name();
                        if (!name.equals(mName + ".Iface1")) {
                            log3("ERROR : !name.equals(mName + '.Iface1') in case: Iface2");
                            expresult = 1;
                            break;
                        }

                        break;

                case 2:         // Iface3 - 1 superifaces, 1 indirect ifaces

                        List list3 = vm.classesByName(mName + ".Class3ForCheck");

                        classRefType  = (ReferenceType) list3.get(0);

                        List iface3list =
                             ((ClassType) classRefType).interfaces();

                        if (iface3list.size() != 1) {
                            log3("ERROR : iface3list.size() != 1  in case: Iface3");
                            expresult = 1;
                            break;
                        }

                        ifaceType  = (InterfaceType) iface3list.get(0);
                        l = ifaceType.superinterfaces();
                        if (l.size() != 1) {
                            log3("ERROR : ifaceType.superinterfaces().size() != 1  in case: Iface3");
                            expresult = 1;
                            break;
                        }

                        break;

                case 3:         // Iface4 - 2 superifaces, 0 indirect ifaces

                        List list4 = vm.classesByName(mName + ".Class4ForCheck");

                        classRefType  = (ReferenceType) list4.get(0);

                        List iface4list =
                             ((ClassType) classRefType).interfaces();

                        if (iface4list.size() != 1) {
                            log3("ERROR : iface4list.size() != 1  in case: Iface4");
                            expresult = 1;
                            break;
                        }

                        ifaceType  = (InterfaceType) iface4list.get(0);
                        l = ifaceType.superinterfaces();
                        if (l.size() != 2) {
                            log3("ERROR : ifaceType.superinterfaces().size() != 2  in case: Iface4");
                            expresult = 1;
                            break;
                        }

                        itype = (InterfaceType) l.get(0);
                        name = itype.name();
                        if (!name.equals(mName + ".Iface0")) {
                            if (!name.equals(mName + ".Iface1")) {
                                log3("ERROR : !name.equals('.Iface0' or '.Iface1') in case: Iface4");
                                expresult = 1;
                                break;
                            }
                        }
                        itype = (InterfaceType) l.get(1);
                        name = itype.name();
                        if (!name.equals(mName + ".Iface0")) {
                            if (!name.equals(mName + ".Iface1")) {
                                log3("ERROR : !name.equals('.Iface1' or '.Iface0') in case: Iface4");
                                expresult = 1;
                                break;
                            }
                        }

                        break;


                default: expresult = 2;
                         break ;
                }

                if (expresult == 2) {
                    log2("      test cases finished");
                    break ;
                } else if (expresult == 1) {
                    log3("ERROR: expresult != true;  check # = " + i);
                    testExitCode = FAILED;
                }
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
