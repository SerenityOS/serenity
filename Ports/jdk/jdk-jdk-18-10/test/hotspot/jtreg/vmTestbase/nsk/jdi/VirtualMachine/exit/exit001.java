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

package nsk.jdi.VirtualMachine.exit;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;
import com.sun.jdi.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * VirtualMachine.                                              <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.VirtualMachine.exit()</code>               <BR>
 * complies with its specification.                             <BR>
 * The test checks up that after call to VirtualMachine.exit(), <BR>
 * the communication channel is closed, hence,                  <BR>
 * the VirtualMachine object becomes invalid.                   <BR>
 * <BR>
 * The test works as follows.                                   <BR>
 * After launching a debuggee, a debugger gets its VirtualMachine<BR>
 * object (vm), invokes the method vm.exit(0); and performs     <BR>
 * checking call to the method vm.allClasses().                 <BR>
 * An expected correct reaction to the method allClasses() is   <BR>
 * throwing VMDisconnectedException.                            <BR>
 */

public class exit001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;

    static final int FAILED = 2;

    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String sHeader1 = "\n==> nsk/jdi/VirtualMachine/exit/exit001  ", sHeader2 = "--> debugger: ",
            sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main(String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new exit001().runThis(argv, out);
    }

    //--------------------------------------------------   log procedures

    private static Log logHandler;

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

    private String debuggeeName = "nsk.jdi.VirtualMachine.exit.exit001a";

    //String mName = "nsk.jdi.VirtualMachine.exit";

    //====================================================== test program
    //------------------------------------------------------ common section

    static ArgumentHandler argsHandler;

    static int waitTime;

    static VirtualMachine vm = null;

    static int testExitCode = PASSED;

    static final int returnCode0 = 0;

    static final int returnCode1 = 1;

    static final int returnCode2 = 2;

    static final int returnCode3 = 3;

    static final int returnCode4 = 4;

    //------------------------------------------------------ methods

    private int runThis(String argv[], PrintStream out) {

        Debugee debuggee;

        argsHandler = new ArgumentHandler(argv);
        logHandler = new Log(out, argsHandler);
        Binder binder = new Binder(argsHandler, logHandler);

        if (argsHandler.verbose()) {
            debuggee = binder.bindToDebugee(debuggeeName + " -vbs");
        } else {
            debuggee = binder.bindToDebugee(debuggeeName);
        }

        waitTime = argsHandler.getWaitTime();

        IOPipe pipe = new IOPipe(debuggee);

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

        log1("      TESTING BEGINS");

        log2("......vm.exit(0);");
        vm.exit(0);

        try {
            log2("...... try: vm.allClasses();  VMDisconnectedException is expected");
            vm.allClasses();
            log3("ERROR: no VMDisconnectedException");
            testExitCode = FAILED;
        } catch (VMDisconnectedException e1) {
            log2("     :    VMDisconnectedException");
        } catch (Exception e2) {
            log3("ERROR:  unexpected Exception : " + e2);
            testExitCode = FAILED;
        }
        log1("      TESTING ENDS");

        if (testExitCode != PASSED) {
            logHandler.complain("TEST FAILED");
        }

        return testExitCode;
    }
}
