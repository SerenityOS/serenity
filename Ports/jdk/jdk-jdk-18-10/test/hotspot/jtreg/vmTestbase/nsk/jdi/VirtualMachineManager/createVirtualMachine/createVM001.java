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
 * The test checks up that both createVirtualMachine(Connection) and   <BR>
 * createVirtualMachine(Connection, Process) methods throw             <BR>
 * IllegalStateException if the used Connection is not open.           <BR>
 *                                                                     <BR>
 */

public class createVM001 {

    static final int STATUS_PASSED = 0;
    static final int STATUS_FAILED = 2;
    static final int STATUS_TEMP = 95;

    static final String errorLogPrefixHead = "createVM001: ";
    static final String errorLogPrefix     = "             ";
    static final String infoLogPrefixHead = "--> createVM001: ";
    static final String infoLogPrefix     = "-->              ";
    static final String emptyString = "";


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
        int result =  new createVM001().runThis(argv, out);
        if ( result == STATUS_FAILED ) {
            logAlways("\n##> nsk/jdi/VirtualMachineManager/createVirtualMachine/createVM001 test FAILED");
        }
        else {
            logAlways("\n==> nsk/jdi/VirtualMachineManager/createVirtualMachine/createVM001 test PASSED");
        }
        return result;
    }


    private int runThis (String argv[], PrintStream out) {
        int testResult = STATUS_PASSED;

        argsHandler = new ArgumentHandler(argv);
        logHandler = new Log(out, argsHandler);
        logHandler.enableErrorsSummary(false);

        logAlways("==> nsk/jdi/VirtualMachineManager/createVirtualMachine/createVM001 test...");
        logOnVerbose
            ("==> Test checks that virtualMachineManager.createVirtualMachine(...) methods");
        logOnVerbose
            ("==> throw IllegalStateException for Connection that is not open.");

        VirtualMachineManager virtualMachineManager = Bootstrap.virtualMachineManager();
        if (virtualMachineManager == null) {
            logOnError(errorLogPrefixHead + "Bootstrap.virtualMachineManager() returns null.");
            return STATUS_FAILED;
        }

        // check method virtualMachineManager.createVirtualMachine(Connection)...
        Connection testConnection = new createVM001_Connection();
        try {
            VirtualMachine dummyVM = virtualMachineManager.createVirtualMachine(testConnection);
        } catch ( IllegalStateException illegStExcep ) {
            // OK - expected IllegalStateException because Connection is not open
        } catch ( Throwable thrown ) {
            logOnError(errorLogPrefixHead + "virtualMachineManager.createVirtualMachine(Connection) throws");
            logOnError(errorLogPrefix + "unexpected Exception for Connection that is not open:");
            logOnError(errorLogPrefix + "Expected Exception - IllegalStateException");
            logOnError(errorLogPrefix + "Actual Exception - '" + thrown + "'");
            testResult = STATUS_FAILED;
        }

        // check method virtualMachineManager.createVirtualMachine(Connection, Process)...
        testConnection = new createVM001_Connection();
        Process dummyProcess = new createVM001_Process();
        try {
            VirtualMachine dummyVM = virtualMachineManager.createVirtualMachine(testConnection, dummyProcess);
        } catch ( IllegalStateException illegStExcep ) {
            // OK - expected IllegalStateException because Connection is not open
        } catch ( Throwable thrown ) {
            logOnError(errorLogPrefixHead + "virtualMachineManager.createVirtualMachine(Connection, Process)");
            logOnError(errorLogPrefix + "throws unexpected Exception for Connection that is not open:");
            logOnError(errorLogPrefix + "Expected Exception - IllegalStateException");
            logOnError(errorLogPrefix + "Actual Exception - '" + thrown + "'");
            testResult = STATUS_FAILED;
        }

        return testResult;
    }

} // end of createVM001 class

class createVM001_Connection extends Connection {

    public void close() throws IOException {
        if ( true ) {
            throw new IOException("Dummy IOException in createVM001_Connection.close().");
        }
    }

    public boolean isOpen() {
        return false;
    }

    public byte[] readPacket() throws IOException {
        if ( true ) {
            throw new IOException("Dummy IOException in createVM001_Connection.readPacket().");
        }

        return new byte[11];
    }

    public void writePacket(byte b[]) throws IOException {
        if ( true ) {
            throw new IOException("Dummy IOException in createVM001_Connection.writePacket(byte b[]).");
        }

    }

} // end of createVM001_Connection class

class createVM001_Process extends Process {

    public OutputStream getOutputStream() {
        return null;
    }

    public InputStream getInputStream() {
        return null;
    }

    public InputStream getErrorStream() {
        return null;
    }

    public int waitFor() throws  InterruptedException {
        return 0;
    }

    public int exitValue() {
        return 0;
    }

    public void destroy() {
    }

} // end of createVM001_Process class
