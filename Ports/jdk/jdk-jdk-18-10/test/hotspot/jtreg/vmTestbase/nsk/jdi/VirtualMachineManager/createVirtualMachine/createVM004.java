/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.VirtualMachineManager.createVirtualMachine;

import nsk.share.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.connect.spi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the                                                    <BR>
 * virtualMachineManager.createVirtualMachine(...) methods.            <BR>
 *                                                                     <BR>
 * The test checks up that createVirtualMachine(Connection, Process)   <BR>
 * method creates the VirtualMachine properly when 'Process' argument  <BR>
 * is null.                                                            <BR>
 *                                                                     <BR>
 * After the VirtualMachine is created the test checks that:           <BR>
 *   - VirtualMachine.process() method returns the null as             <BR>
 *     the specification says about it.                                <BR>
 *   - nevertheless after VirtualMachine.resume() the target VM should <BR>
 *     finish for specified time with expected Exit Status value;      <BR>
 *                                                                     <BR>
 */
public class createVM004 {

    static final int STATUS_PASSED = 0;
    static final int STATUS_FAILED = 2;
    static final int STATUS_TEMP = 95;

    static final String errorLogPrefixHead = "createVM004: ";
    static final String errorLogPrefix     = "             ";
    static final String infoLogPrefixHead = "--> createVM004: ";
    static final String infoLogPrefix     = "-->              ";
    static final String emptyString = "";
    static final String packagePrefix = "nsk.jdi.VirtualMachineManager.createVirtualMachine.";
//    static final String packagePrefix = emptyString;
    static final String targetVMClassName = packagePrefix + "CreateVM004_TargetVM";

    static ArgumentHandler  argsHandler;
    static Log logHandler;

    private static void logOnVerbose(String message) {
        logHandler.display(message);
    }

    private static void logOnError(String message) {
        logHandler.complain(message);
    }

    private static void logAlways(String message) {
        logHandler.println(message);
    }

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + STATUS_TEMP);
    }

    public static int run (String argv[], PrintStream out) {
        int result =  new createVM004().runThis(argv, out);
        if ( result == STATUS_FAILED ) {
            logAlways("\n##> nsk/jdi/VirtualMachineManager/createVirtualMachine/createVM004 test FAILED");
        }
        else {
            logAlways("\n==> nsk/jdi/VirtualMachineManager/createVirtualMachine/createVM004 test PASSED");
        }
        return result;
    }


    private int runThis (String argv[], PrintStream out) {
        int testResult = STATUS_PASSED;

        argsHandler = new ArgumentHandler(argv);
        logHandler = new Log(out, argsHandler);

        logAlways("==> nsk/jdi/VirtualMachineManager/createVirtualMachine/createVM004 test...");
        logOnVerbose
            ("==> Test checks that virtualMachineManager.createVirtualMachine(Connection, Process) method");
        logOnVerbose
            ("==> creates Virtual Machine properly when 'Process' argument is null.");

        VirtualMachineManager virtualMachineManager = Bootstrap.virtualMachineManager();
        if (virtualMachineManager == null) {
            logOnError(errorLogPrefixHead + "Bootstrap.virtualMachineManager() returns null.");
            return STATUS_FAILED;
        }

        String targetJavaKey = "targetJava=";
        String[] testArgs = argsHandler.getArguments();

        TransportService testTransportService = new CreateVM004_TranspServ();

        TransportService.ListenKey testTransportServiceListenKey = null;
        logOnVerbose(infoLogPrefixHead + "Start Listening for target VM...");
        try {
            testTransportServiceListenKey = testTransportService.startListening();
        } catch ( IOException ioExcept ) {
            // OK. It is possible Exception
            logOnVerbose(infoLogPrefixHead
                + "testTransportService.startListening() throws IOException.");
            logOnVerbose(infoLogPrefix + "testTransportService = '" + testTransportService + "'");
            logOnVerbose(infoLogPrefix + "IOException - '" + ioExcept + "'");
            logOnVerbose(infoLogPrefix + "The test is stopped.");
            return STATUS_PASSED;
        } catch ( Throwable thrown ) {
            logOnError(errorLogPrefixHead + "testTransportService.startListening() throws");
            logOnError(errorLogPrefix + "unexpected Exception:");
            logOnError(errorLogPrefix + "testTransportService = '" + testTransportService + "'");
            logOnError(errorLogPrefix + "Exception - '" + thrown + "'");
            return STATUS_FAILED;
        }

        String targetJava = argsHandler.getLaunchExecPath()
                                    + " " + argsHandler.getLaunchOptions();
        String commandToRun = targetJava + " -Xdebug -Xrunjdwp:transport=dt_socket,address=" +
            testTransportServiceListenKey.address() + " " + targetVMClassName;

        Binder binder = new Binder(argsHandler, logHandler);
        Debugee debugee = null;
        Process processToRun = null;
        Connection testTransportServiceConnection = null;

        try {

            logOnVerbose(infoLogPrefixHead + "PROCESS is being created:");
            logOnVerbose(infoLogPrefix + "Command to run: " + commandToRun);

            debugee = binder.startLocalDebugee(commandToRun);
            debugee.redirectOutput(logHandler);
            processToRun = debugee.getProcess();

            logOnVerbose(infoLogPrefixHead + "Accepting launched target VM...");
            try {
                testTransportServiceConnection
                    = testTransportService.accept(testTransportServiceListenKey, 0, 0);
            } catch ( IOException ioExcept ) {
                // OK. It is possible Exception
                logOnVerbose(infoLogPrefixHead
                    + "testTransportService.accept(testTransportServiceListenKey, 0, 0) throws IOException.");
                logOnVerbose(infoLogPrefix + "testTransportService = '" + testTransportService + "'");
                logOnVerbose(infoLogPrefix + "testTransportServiceListenKey = '" + testTransportServiceListenKey + "'");
                logOnVerbose(infoLogPrefix + "IOException - '" + ioExcept + "'");
                logOnVerbose(infoLogPrefix + "The test is stopped.");
                return STATUS_PASSED;
            } catch ( Throwable thrown ) {
                logOnError(errorLogPrefixHead + "testTransportService.accept(testTransportServiceListenKey) throws");
                logOnError(errorLogPrefix + "unexpected Exception:");
                logOnError(errorLogPrefix + "testTransportService = '" + testTransportService + "'");
                logOnError(errorLogPrefix + "testTransportServiceListenKey = '" + testTransportServiceListenKey + "'");
                logOnError(errorLogPrefix + "Exception - '" + thrown + "'");
                return STATUS_FAILED;
            }

            try {
                testTransportService.stopListening(testTransportServiceListenKey);
            } catch ( IOException ioExcept ) {
                // OK. It is possible Exception
                logOnVerbose(infoLogPrefixHead
                    + "testTransportService.stopListening(testTransportServiceListenKey) throws IOException.");
                logOnVerbose(infoLogPrefix + "testTransportService = '" + testTransportService + "'");
                logOnVerbose(infoLogPrefix + "testTransportServiceListenKey = '" + testTransportServiceListenKey + "'");
                logOnVerbose(infoLogPrefix + "IOException - '" + ioExcept + "'");
                logOnVerbose(infoLogPrefix + "The test is stopped.");
                return STATUS_PASSED;
            } catch ( Throwable thrown ) {
                logOnError(errorLogPrefixHead
                    + "testTransportService.stopListening(testTransportServiceListenKey) throws");
                logOnError(errorLogPrefix + "unexpected Exception:");
                logOnError(errorLogPrefix + "testTransportService = '" + testTransportService + "'");
                logOnError(errorLogPrefix + "testTransportServiceListenKey = '" + testTransportServiceListenKey + "'");
                logOnError(errorLogPrefix + "Exception - '" + thrown + "'");
                return STATUS_FAILED;
            }

            logOnVerbose(infoLogPrefixHead + "Creating VirtualMachine for target VM...");
            VirtualMachine testVirtualMachine = null;
            try {
                testVirtualMachine
                    = virtualMachineManager.createVirtualMachine(testTransportServiceConnection, null);
            } catch ( IOException ioExcept ) {
                // OK. It is possible Exception
                logOnVerbose(infoLogPrefixHead
                    + "VirtualMachineManager.createVirtualMachine(Connection, null) throws IOException.");
                logOnVerbose(infoLogPrefix + "IOException - '" + ioExcept + "'");
                logOnVerbose(infoLogPrefix + "The test is stopped.");
                return STATUS_PASSED;
            } catch ( Throwable thrown ) {
                logOnError(errorLogPrefixHead + "VirtualMachineManager.createVirtualMachine(Connection, null) throws");
                logOnError(errorLogPrefix + "unexpected Exception:");
                logOnError(errorLogPrefix + "Connection = '" + testTransportServiceConnection + "'");
                logOnError(errorLogPrefix + "Exception - '" + thrown + "'");
                return STATUS_FAILED;
            }

            Process  testVirtualMachineProcess = testVirtualMachine.process();
            if ( testVirtualMachineProcess != null ) {
                logOnError(errorLogPrefixHead
                    + "Created VirtualMachine with null 'Process' argument returns unexpected Process:");
                logOnError(errorLogPrefix + "Expected Process = null");
                logOnError(errorLogPrefix + "Returned Process = '" + testVirtualMachineProcess + "'");
                testResult = STATUS_FAILED;
            }

            long timeout = argsHandler.getWaitTime() * 60 * 1000; // milliseconds

            logOnVerbose(infoLogPrefixHead + "Managing target VM");
            debugee.setupVM(testVirtualMachine);
            logOnVerbose(infoLogPrefix + "Waiting for VM initialized");
            debugee.waitForVMInit(timeout);
            logOnVerbose(infoLogPrefix + "Resuming VM");
            debugee.resume();
            logOnVerbose(infoLogPrefix + "Waiting for VM exit");
            int procExitValue = debugee.waitFor();
            logOnVerbose(infoLogPrefix + "VM exit status: " + procExitValue);

            if ( procExitValue != (STATUS_PASSED + STATUS_TEMP) ) {
                logOnError(errorLogPrefixHead + "Target VM finished unexpectedly:");
                logOnError(errorLogPrefix + "Expected Exit status = " + (STATUS_PASSED + STATUS_TEMP));
                logOnError(errorLogPrefix + "Actual Exit status = " + procExitValue);
                testResult = STATUS_FAILED;
            } else {
                logOnVerbose(infoLogPrefixHead + "Target VM finished succesfully!");
            }

        } finally {

            logOnVerbose(infoLogPrefixHead + "Closing connection and destroying target VM...");
            if (testTransportServiceConnection != null) {
                try {
                    testTransportServiceConnection.close();
                } catch (IOException e) {
                    logAlways("# WARNING: IOException while closing connection:\n\t" + e);
                }
            }
            if (debugee != null) {
                debugee.close();
            }
        }

        return testResult;
    }

} // end of createVM004 class
