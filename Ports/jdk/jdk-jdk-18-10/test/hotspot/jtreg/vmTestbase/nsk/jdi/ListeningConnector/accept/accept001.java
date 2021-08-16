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

package nsk.jdi.ListeningConnector.accept;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.connect.*;
import com.sun.jdi.VirtualMachine;

import java.io.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * The test checks that debugger may establish connection with
 * a debugee VM via <code>com.sun.jdi.SocketListen</code> connector.
 */
public class accept001 {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;
    static final String DEBUGEE_CLASS = "nsk.jdi.ListeningConnector.accept.accept001t";

    private Log log;

    private VirtualMachine vm;
    private ListeningConnector connector;
    private Map<java.lang.String,? extends com.sun.jdi.connect.Connector.Argument> connArgs;

    IORedirector outRedirector;
    IORedirector errRedirector;

    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new accept001().runIt(argv, out);
    }

    private int runIt(String argv[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);

// pass if "com.sun.jdi.SocketListen" is not implemented
// on this platform
        if (argHandler.shouldPass("com.sun.jdi.SocketListen"))
            return PASSED;

        long timeout = argHandler.getWaitTime() * 60 * 1000;
        log = new Log(out, argHandler);

        String connAddr = startListen(argHandler.getTransportPortIfNotDynamic());

        String java = argHandler.getLaunchExecPath()
                          + " " + argHandler.getLaunchOptions();
        String cmd = java +
            " -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=n,address=" +
            connAddr + " " + DEBUGEE_CLASS;

        Binder binder = new Binder(argHandler, log);
        log.display("command: " + cmd);
        Debugee debugee = binder.startLocalDebugee(cmd);
        debugee.redirectOutput(log);

        if ((vm = attachTarget()) == null) {
            log.complain("TEST: Unable to attach the debugee VM");
            debugee.close();
            return FAILED;
        }

        if (!stopListen()) {
            log.complain("TEST: Unable to stop listen");
            debugee.close();
            return FAILED;
        }

        log.display("Debugee VM: name=" + vm.name() + " JRE version=" +
            vm.version() + "\n\tdescription=" + vm.description());

        debugee.setupVM(vm);
        debugee.waitForVMInit(timeout);

        log.display("\nResuming debugee VM");
        debugee.resume();

        log.display("\nWaiting for debugee VM exit");
        int code = debugee.waitFor();
        if (code != (JCK_STATUS_BASE+PASSED)) {
            log.complain("Debugee VM has crashed: exit code=" +
                code);
            return FAILED;
        }
        log.display("Debugee VM: exit code=" + code);
        return PASSED;
    }

    private VirtualMachine attachTarget() {
        try {
            return connector.accept(connArgs);
        } catch (IOException e) {
            log.complain("TEST: caught IOException: " +
                e.getMessage());
            return null;
        } catch (IllegalConnectorArgumentsException e) {
            log.complain("TEST: Illegal connector arguments: " +
                e.getMessage());
            return null;
        } catch (Exception e) {
            log.complain("TEST: Internal error: " + e.getMessage());
            return null;
        }
    }

    private String startListen(String port) {
        Connector.Argument arg;

        connector = (ListeningConnector)
            findConnector("com.sun.jdi.SocketListen");

        connArgs = connector.defaultArguments();
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

        try {
            return connector.startListening(connArgs);
        } catch (IOException e) {
            throw new Error("TEST: Unable to start listening to the debugee VM: " +
                e.getMessage());
        } catch (IllegalConnectorArgumentsException e) {
            throw new Error("TEST: Illegal connector arguments: " +
                e.getMessage());
        } catch (Exception e) {
            throw new Error("TEST: Internal error: " + e.getMessage());
        }
    }

    private boolean stopListen() {
        try {
            connector.stopListening(connArgs);
        } catch (IOException e) {
            log.complain("TEST: Unable to stop listening to the debugee VM: " +
                e.getMessage());
            return false;
        } catch (IllegalConnectorArgumentsException e) {
            log.complain("TEST: Illegal connector arguments: " +
                e.getMessage());
            return false;
        } catch (Exception e) {
            log.complain("TEST: Internal error: " + e.getMessage());
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
