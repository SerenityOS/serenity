/*
 * Copyright (c) 2001, 2016, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4402649
 * @summary RMI should use new logging APIs. Unit test to exercise
 * RMI's use of the java.util.logging API.
 * @author Laird Dornin
 *
 * @library ../../../../../java/rmi/testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary
 * @run main/othervm -Djava.security.manager=allow CheckLogging
 */

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.OutputStream;

import java.rmi.RemoteException;
import java.rmi.Remote;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.LogStream;
import java.rmi.server.RemoteServer;

import java.rmi.registry.Registry;

/**
 * Perform following checks:
 *
 * 1. If using java.util.logging, turn on client call logger using
 * system property, "sun.rmi.client.logCalls".  Collect client call
 * output using a custom stream handler. Verify client call output is
 * generated and contains the string "outbound call".
 *
 * 2. Turn on server call using
 * RemoteServer.setLog(ByteArrayOutputStream). Invoke some remote
 * method calls verify logger output is non-null.
 *
 * Turn off server call log by doing setLog(null), verify output is
 * zero length.  Verify that RemoteServer.getLog == null
 *
 * Use setLog to turn call log back on.  Invoke remote method that
 * throws an exception and contains the string "exception".
 *
 * 3. Print directly to return value of RemoteServer.getLog(), verify
 * logger output is non-null.
 */
public class CheckLogging {
    private static int REGISTRY_PORT = -1;
    private static String LOCATION;
    private static Logger logger;

    private static final ByteArrayOutputStream clientCallOut =
        new ByteArrayOutputStream();

    private static final boolean usingOld =
        Boolean.getBoolean("sun.rmi.log.useOld");

    static {
        System.setProperty("sun.rmi.client.logCalls", "true");
        if (usingOld) {
            System.err.println("set default stream");
            LogStream.setDefaultStream(new PrintStream(clientCallOut));
        } else {
            logger = Logger.getLogger("sun.rmi.client.call");
            logger.addHandler(new InternalStreamHandler(clientCallOut));
        }
    }

    /* use registry to generate client & server call log info */
    private static Registry registry;
    static {
        try {
            registry = TestLibrary.createRegistryOnEphemeralPort();
            REGISTRY_PORT = TestLibrary.getRegistryPort(registry);
            LOCATION = "rmi://localhost:" + REGISTRY_PORT + "/";
        } catch (Exception e) {
            TestLibrary.bomb("could not create registry");
        }
    }

    /**
     * Used to collect output from specific loggers
     */
    private static class InternalStreamHandler extends StreamHandler {
        private InternalStreamHandler(OutputStream out) {
            super(out, new SimpleFormatter());
            setLevel(Level.ALL);
        }

        public void publish(LogRecord record) {
            super.publish(record);
            flush();
        }

        public void close() {
            flush();
        }
    }

    /**
     * Ensure that a log has some output and that it contains a
     * certain string
     */
    private static void verifyLog(ByteArrayOutputStream bout,
                                  String mustContain)
    {
        byte[] bytes = bout.toByteArray();
        if (bytes.length == 0) {
            TestLibrary.bomb("log data length is zero");
        } else if ((mustContain != null) &&
                   (bout.toString().indexOf(mustContain) < 0))
        {
            TestLibrary.bomb("log output did not contain: " + mustContain);
        }
    }

    /**
     * Check serverCallLog output
     */
    private static void checkServerCallLog() throws Exception {
        ByteArrayOutputStream serverCallLog = new ByteArrayOutputStream();
        RemoteServer.setLog(serverCallLog);
        Naming.list(LOCATION);
        verifyLog(serverCallLog, "list");

        serverCallLog.reset();
        RemoteServer.setLog(null);
        PrintStream callStream = RemoteServer.getLog();
        if (callStream != null) {
            TestLibrary.bomb("call stream not null after calling " +
                             "setLog(null)");
        } else {
            System.err.println("call stream should be null and it is");
        }
        Naming.list(LOCATION);

        if (usingOld) {
            if (serverCallLog.toString().indexOf("UnicastServerRef") >= 0) {
                TestLibrary.bomb("server call logging not turned off");
            }
        } else if (serverCallLog.toByteArray().length != 0) {
            TestLibrary.bomb("call log contains output but it " +
                             "should be empty");
        }

        serverCallLog.reset();
        RemoteServer.setLog(serverCallLog);
        try {
            // generates a notbound exception
            Naming.lookup(LOCATION + "notthere");
        } catch (Exception e) {
        }
        verifyLog(serverCallLog, "exception");

        serverCallLog.reset();
        RemoteServer.setLog(serverCallLog);
        callStream = RemoteServer.getLog();
        callStream.println("bingo, this is a getLog test");
        verifyLog(serverCallLog, "bingo");
    }

    private static void checkPermissions() {
        SecurityException ex = null;
        try {
            // should fail for lack of LoggingPermission "control"
            RemoteServer.setLog(System.err);
        } catch (SecurityException e) {
            System.err.println("security excepton caught correctly");
            ex = e;
        }
        if (ex == null) {
            TestLibrary.bomb("able to set log without permission");
        }
    }

    public static void main(String[] args) {
        try {
            checkServerCallLog();

            if (!usingOld) {
                verifyLog(clientCallOut, "outbound call");
                System.setSecurityManager(new java.lang.SecurityManager());
                checkPermissions();
            }
            System.err.println("TEST PASSED");

        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            TestLibrary.bomb("unexpected exception", e);
        } finally {
            TestLibrary.unexport(registry);
        }
    }
}
