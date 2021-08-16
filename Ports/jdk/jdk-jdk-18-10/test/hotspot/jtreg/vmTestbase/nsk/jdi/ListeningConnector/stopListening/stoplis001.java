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

package nsk.jdi.ListeningConnector.stopListening;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.connect.*;

import java.io.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * The test exercises JDI function <code>ListeningConnector.stopListening</code>.
 * The <b>Socket Connector</b> is using as listening
 * connector.<br>
 * The test cases include:
 * <li> checking that <code>ListeningConnector.stopListening</code> throws
 * an Exception if it has been invoked with argument map different from
 * the one given for a previous <code>ListeningConnector.startListening()</code>
 * invocation;
 * <li> checking that listening can be successfully stopped if given
 * argument map is the same with the one given for the previous
 * <code>ListeningConnector.startListening()</code> invocation.
 */
public class stoplis001 {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;
    static final String CONNECTOR_NAME =
        "com.sun.jdi.SocketListen";

    private Log log;

    private ListeningConnector connector;
    private PrintStream out;

    boolean totalRes = true;

    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new stoplis001().runIt(argv, out);
    }

    private int runIt(String argv[], PrintStream out) {
        String addr;
        ArgumentHandler argHandler = new ArgumentHandler(argv);

// pass if CONNECTOR_NAME is not implemented
// on this platform
        if (argHandler.shouldPass(CONNECTOR_NAME))
            return PASSED;
        this.out = out;
        log = new Log(out, argHandler);

        Map<String,? extends com.sun.jdi.connect.Connector.Argument> cArgs1 = initConnector(argHandler.getTransportPort());
        Map<String,? extends com.sun.jdi.connect.Connector.Argument> cArgs2 = initConnector(null);
        if ((addr = startListen(cArgs2)) == null) {
            log.complain("FAILURE: unable to start listening the address " +
                addr);
            return FAILED;
        }
        else
            log.display("TEST: start listening the address " + addr);

/* Check that an Exception is thrown if ListeningConnector.stopListening
 has been invoked with argument map different from the one given for
 a previous ListeningConnector.startListening() invocation */
        if (!stopListen(cArgs1, true))
            log.display("Test case #1 PASSED: unable to stop listening");
        else {
            log.complain("Test case #1 FAILED: listening is successfully stopped without starting listening");
            totalRes = false;
        }

/* Check that listening can be successfully stopped if given argument map
   is the same with the one given for ListeningConnector.startListening() */
        if (!stopListen(cArgs2, false)) {
            log.complain("Test case #2 FAILED: unable to stop listening");
            return FAILED;
        }
        else
           log.display("Test case #2 PASSED: listening is successfully stopped");

        if (totalRes) return PASSED;
        else return FAILED;
    }

    private Map<java.lang.String,? extends com.sun.jdi.connect.Connector.Argument> initConnector(String port) {
        Connector.Argument arg;

        connector = (ListeningConnector)
            findConnector(CONNECTOR_NAME);
        Map<java.lang.String,? extends com.sun.jdi.connect.Connector.Argument> connArgs = connector.defaultArguments();
        Iterator cArgsValIter = connArgs.keySet().iterator();
        while (cArgsValIter.hasNext()) {
            String argKey = (String) cArgsValIter.next();
            String argVal = null;

            if ((arg = (Connector.Argument) connArgs.get(argKey)) == null) {
                log.complain("Argument " + argKey.toString() +
                    "is not defined for the connector: " +
                    connector.name());
            }
            if (arg.name().equals("port") && port != null)
                arg.setValue(port);

            log.display("\targument name=" + arg.name());
            if ((argVal = arg.value()) != null)
                log.display("\t\tvalue=" + argVal);
            else log.display("\t\tvalue=NULL");
        }

        return connArgs;
    }

    private String startListen(Map<java.lang.String,? extends com.sun.jdi.connect.Connector.Argument> cArgs) {
        try {
            return connector.startListening(cArgs);
        } catch (IOException e) {
            log.complain("FAILURE: caught IOException: " +
                e.getMessage());
            e.printStackTrace(out);
            return null;
        } catch (IllegalConnectorArgumentsException e) {
            log.complain("FAILURE: Illegal connector arguments: " +
                e.getMessage());
            e.printStackTrace(out);
            return null;
        } catch (Exception e) {
            log.complain("FAILURE: Exception: " + e.getMessage());
            e.printStackTrace(out);
            return null;
        }
    }

    private boolean stopListen(Map<String,? extends com.sun.jdi.connect.Connector.Argument> cArgs, boolean negative) {
        try {
            connector.stopListening(cArgs);
        } catch (IOException e) {
            if (negative)
                log.display("stopListen: caught IOException: " +
                    e.getMessage());
            else {
                log.complain("FAILURE: caught IOException: " +
                    e.getMessage());
                e.printStackTrace(out);
            }
            return false;
        } catch (IllegalConnectorArgumentsException e) {
            if (negative)
                log.display("stopListen: caught IllegalConnectorArgumentsException: " +
                    e.getMessage());
            else {
                log.complain("FAILURE: Illegal connector arguments: " +
                    e.getMessage());
                e.printStackTrace(out);
            }
            return false;
        } catch (Exception e) {
            if (negative)
                log.display("stopListen: caught Exception: " +
                    e.getMessage());
            else {
                log.complain("FAILURE: Exception: " + e.getMessage());
                e.printStackTrace(out);
            }
            return false;
        }

        return true;
    }

    private Connector findConnector(String connectorName) {
        List connectors = Bootstrap.virtualMachineManager().allConnectors();
        Iterator iter = connectors.iterator();

        while (iter.hasNext()) {
            Connector connector = (Connector) iter.next();
            if (connector.name().equals(connectorName)) {
                log.display("Connector name=" + connector.name() +
                    "\n\tdescription=" + connector.description() +
                    "\n\ttransport=" + connector.transport().name());
                return connector;
            }
        }
        throw new Error("No appropriate connector");
    }
}
