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
 * The test checks up that the                                         <BR>
 * createVirtualMachine(Connection, Process) method throws             <BR>
 * IOException when I/O error occurs in used Connection during         <BR>
 * creating of Virtual Mashine.                                        <BR>
 *                                                                     <BR>
 */
public class createVM002 {

    static final int STATUS_PASSED = 0;
    static final int STATUS_FAILED = 2;
    static final int STATUS_TEMP = 95;

    static final String errorLogPrefixHead = "createVM002: ";
    static final String errorLogPrefix     = "             ";
    static final String infoLogPrefixHead = "--> createVM002: ";
    static final String infoLogPrefix     = "-->              ";
    static final String emptyString = "";
    static final String packagePrefix = "nsk.jdi.VirtualMachineManager.createVirtualMachine.";
//    static final String packagePrefix = emptyString;
    static final String targetVMClassName = packagePrefix + "CreateVM002_TargetVM";

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
        int result =  new createVM002().runThis(argv, out);
        if ( result == STATUS_FAILED ) {
            logAlways("\n##> nsk/jdi/VirtualMachineManager/createVirtualMachine/createVM002 test FAILED");
        }
        else {
            logAlways("\n==> nsk/jdi/VirtualMachineManager/createVirtualMachine/createVM002 test PASSED");
        }
        return result;
    }


    private int runThis (String argv[], PrintStream out) {
        int testResult = STATUS_FAILED;

        argsHandler = new ArgumentHandler(argv);
        logHandler = new Log(out, argsHandler);

        logAlways("==> nsk/jdi/VirtualMachineManager/createVirtualMachine/createVM002 test...");
        logOnVerbose
            ("==> Test checks that virtualMachineManager.createVirtualMachine(Connection, Process) method");
        logOnVerbose
            ("==> throw IOException when I/O error occurs in Connection.");

        VirtualMachineManager virtualMachineManager = Bootstrap.virtualMachineManager();
        if (virtualMachineManager == null) {
            logOnError(errorLogPrefixHead + "Bootstrap.virtualMachineManager() returns null.");
            return STATUS_FAILED;
        }

        TransportService testTransportService = new CreateVM002_TranspServ();

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
                testTransportServiceListenKey.address() +
                // Addind "suspend=n' option helps avoid debugee hang. See 6803636.
                ",suspend=n" +
                " " + targetVMClassName;

        Binder binder = new Binder(argsHandler, logHandler);
        Debugee debugee = null;
        Process processToRun = null;
        Connection testTransportServiceConnection = null;
        VirtualMachine testVirtualMachine = null;

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
//            ( (CreateVM002_Connection)testTransportServiceConnection).toPrintPacket(true);
            boolean wasIOExceptionInConnection = false;
            try {
                testVirtualMachine
                    = virtualMachineManager.createVirtualMachine(testTransportServiceConnection,
                                                                            processToRun);
                wasIOExceptionInConnection
                    =( (CreateVM002_Connection)testTransportServiceConnection).wasIOException;
                if ( wasIOExceptionInConnection ) {
                    logOnError(errorLogPrefixHead + "VirtualMachineManager.createVirtualMachine(Connection, Process) does");
                    logOnError(errorLogPrefix + "NOT throw IOException when I/O error occurs in Connection.");
                    testResult = STATUS_FAILED;
                }
                debugee.setupVM(testVirtualMachine);
            } catch ( IOException ioExcept ) {
                // OK. It is expected Exception
                logOnVerbose(infoLogPrefixHead
                    + "VirtualMachineManager.createVirtualMachine(Connection, Process) throws IOException.");
                logOnVerbose(infoLogPrefix + "IOException - '" + ioExcept + "'");
                testResult = STATUS_PASSED;
            } catch ( Throwable thrown ) {
                wasIOExceptionInConnection
                    =( (CreateVM002_Connection)testTransportServiceConnection).wasIOException;
                logOnError(errorLogPrefixHead + "VirtualMachineManager.createVirtualMachine(Connection, Process) throws");
                if ( wasIOExceptionInConnection ) {
                    logOnError(errorLogPrefix + "unexpected Exception when I/O error occurs in Connection:");
                    logOnError(errorLogPrefix + "Expected Exception - 'IOException'");
                    logOnError(errorLogPrefix + "Actual Exception - '" + thrown + "'");
                } else {
                    logOnError(errorLogPrefix + "unexpected Exception:");
                    logOnError(errorLogPrefix + "Exception - '" + thrown + "'");
                }
                testResult = STATUS_FAILED;
            }
//            ( (CreateVM002_Connection)testTransportServiceConnection).toPrintPacket(false);

        } finally {

            logOnVerbose(infoLogPrefixHead + "Closing connection and destroying target VM...");
            if (testVirtualMachine != null) {
                logOnVerbose(infoLogPrefix + "Disposing target VM mirror");
                testVirtualMachine.dispose();
            }
            if (testTransportServiceConnection != null) {
                logOnVerbose(infoLogPrefix + "Closing transport connection");
                try {
                    testTransportServiceConnection.close();
                } catch (IOException e) {
                    logAlways("# WARNING: IOException while closing connection:\n\t" + e);
                }
            }
            if (debugee != null) {
                logOnVerbose(infoLogPrefix + "Waiting for target VM exit");
                int status = debugee.waitFor();
                logOnVerbose(infoLogPrefix + "debuggee exit status: " + status);
            }
        }

        return testResult;
    }

} // end of createVM002 class
