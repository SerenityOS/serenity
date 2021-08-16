/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.LaunchingConnector.launchnosuspend;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.event.*;

import java.io.*;
import java.util.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * This tiny debugger-like application starts debugee VM
 * so that debugger/debugee connection be correctly
 * established via connector <code>com.sun.jdi.CommandLineLaunch</code>
 * and <code>dt_socket</code> transport.
 *
 * <p>Note, that unlike most of JDI tests, this test
 * will always fail if launching of other JVM is prohibited
 * by security restrictions.
 */
public class launchnosuspend001 {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    private final static String CONNECTOR_NAME =
        "com.sun.jdi.RawCommandLineLaunch";
    private final static String TRANSPORT_NAME = "dt_socket";

    private final static String DEBUGEE_CLASS =
        "nsk.jdi.LaunchingConnector.launchnosuspend.launchnosuspend001o";

    public static void main (String args[]) {
        System.exit(run(args,System.out) + JCK_STATUS_BASE);
        // JCK-style exit status.
    }

    public static int run (String args[], PrintStream out) {
        return new launchnosuspend001(args,out).run();
    }

    private launchnosuspend001 (String args[], PrintStream out) {
        this.out  = out;

        argHandler = new ArgumentHandler(args);
        log = new Log(this.out, argHandler);
        //log.enableVerbose(true);
    }

    private PrintStream out;
    private ArgumentHandler argHandler;
    private Log log;

    private int run() {

// pass if CONNECTOR_NAME with TRANSPORT_NAME is not implemented
// on this platform
        if (argHandler.shouldPass(CONNECTOR_NAME, TRANSPORT_NAME))
            return PASSED;

        LaunchingConnector lc =
            getLaunchingConnector(CONNECTOR_NAME, TRANSPORT_NAME);
        if (lc == null) {
            log.complain("FAILURE: cannot get LaunchingConnector");
            return FAILED;
        };
        Map<java.lang.String,? extends com.sun.jdi.connect.Connector.Argument> carg =
            setupConnectorArguments(lc, argHandler.getTestHost(),
                                    argHandler.getTransportPort());
        log.display("Starting debugee VM");
        VirtualMachine vm;
        try {
            vm = lc.launch(carg);
        } catch (Exception e) {
            log.complain("FAILURE: cannot launch debugee VM: " +
                e.getMessage());
            e.printStackTrace(out);
            return FAILED;
        };

        log.display("target VM: name=" + vm.name() + " JRE version=" +
            vm.version() + "\n\tdescription=" + vm.description());

        //try {EventSet set = vm.eventQueue().remove();
        //for(Event event : set)
        //    System.out.println("==DEFSTAN: " + event.toString());
        //} catch (Throwable e) {}

        //vm.setDebugTraceMode(VirtualMachine.TRACE_ALL);
        Binder binder = new Binder(argHandler, log);
        Debugee debugee = binder.makeLocalDebugee(vm.process());
        debugee.redirectOutput(log);
        debugee.setupVM(vm);
        //debugee.waitForVMInit(1000);

        //log.display("Resuming debugee VM");
        //debugee.resume();

        log.display("Waiting for debugee VM exit");
        int code = debugee.waitFor();
        if (code != (JCK_STATUS_BASE+PASSED)) {
            log.complain("FAILURE: debugee VM exitCode=" + code);
            return FAILED;
        };
        log.display("Debugee VM exitCode=" + code);
        log.display("Test PASSED!");
        return PASSED;
    }

    private Map<java.lang.String,? extends com.sun.jdi.connect.Connector.Argument> setupConnectorArguments(
        LaunchingConnector lc, String host, String port) {

        String java = argHandler.getLaunchExecPath()
                          + " " + argHandler.getLaunchOptions();
        Map<java.lang.String,? extends com.sun.jdi.connect.Connector.Argument> carg = lc.defaultArguments();
        log.display("Connector's arguments:");
        Object cava[] = carg.values().toArray();
        for (int i=0; i<cava.length; i++) {
            Connector.Argument a = (Connector.Argument) cava[i];
            if (a.name().equals("command"))
                a.setValue(java +
                " -agentlib:jdwp=suspend=n,transport=" +
                TRANSPORT_NAME +
                ",address=" + host + ":" + port +
                " " + DEBUGEE_CLASS);
            if (a.name().equals("address"))
                a.setValue(port);
            log.display("    " + a.name() + "=" + a.value());
        };
        return carg;
    }

    /**
     * Find launching connector having the given name and intended for
     * the given transport. Return <code>null</code>, if there is no such
     * socket found supported by the tested JDI implementation. Print caution
     * and also return <code>null</code>, if there is more than one launching
     * connector of such kind.
     */
    private LaunchingConnector getLaunchingConnector(
        String connectorName, String transportName) {

        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();

        List lcl = vmm.launchingConnectors();
        log.display("Number of launching connectors: " + lcl.size());

        int found = 0;
        LaunchingConnector lc = null;

        Iterator lci = lcl.iterator();
        for (int i=1; lci.hasNext(); i++) {
            LaunchingConnector c = (LaunchingConnector) lci.next();
            log.display("Launching connector #" + i + ":");
            log.display("\tname=" + c.name());
            log.display("\tdescription=" + c.description());
            log.display("\ttransport=" + c.transport().name());
            if (!c.name().equals(connectorName))
                continue;
            if (c.transport().name().equals(transportName)) {
                log.display("\t-- Connector " + c.name() +
                    " is appropriate.");
                if (lc == null)
                    lc = c;
                found++;
            }
        };

        if (lc == null) {
            log.complain("FAILURE: no "
            + connectorName + " connector found for "
            + transportName + " transport!");
            return null;
        };
        if (found > 1) {
            log.complain("TEST_BUG: more than one "
            + connectorName + " connector found for "
            + transportName + " transport!");
            return null;
        };
        return lc;
    }

}
