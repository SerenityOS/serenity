/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share.jdi;

import com.sun.jdi.connect.*;
import com.sun.jdi.*;
import java.io.*;
import java.util.*;

import nsk.share.*;
import nsk.share.jpda.*;

/*
 * This class contains several common methods used by connector tests.
 */
public abstract class ConnectorTest {
    protected Log log;

    protected VirtualMachine vm;

    protected int attempts; // attempts to connect to the debuggee VM

    protected int delay; // delay between connection attempts

    protected IORedirector outRedirector;

    protected IORedirector errRedirector;

    protected ArgHandler argHandler;

    protected boolean isTestFailed;

    // set test status 'FAILED'
    protected void testFailed() {
        isTestFailed = true;
    }

    // check if tested functionality implemented on current platform
    protected boolean shouldPass() {
        return argHandler.shouldPass(getConnectorName());
    }

    abstract protected void doTest();

    abstract protected String getConnectorName();

    abstract protected String getDebuggeeClass();

    static public class ArgHandler extends ArgumentHandler {
        public ArgHandler(String[] args) {
            super(args);

        }

        protected boolean checkOption(String option, String value) {
            if (super.checkOption(option, value))
                return true;

            if (option.equals("testWorkDir"))
                return true;

            if (option.equals("waitVMStartEvent"))
                return true;

            return false;
        }

        public String getTestWorkDir() {
            String dir = options.getProperty("testWorkDir");

            if (dir.endsWith(File.separator)) {
                dir = dir.substring(0, dir.length() - 1);
            }

            return dir;
        }

        public boolean waitVMStartEvent() {
            return options.containsKey("waitVMStartEvent");
        }
    }

    /*
     * Subclasses can provide another ArgumentHandlers
     */
    protected ArgHandler createArgumentHandler(String[] args) {
        return new ArgHandler(args);
    }

    protected void init(String[] args, PrintStream out) {
        argHandler = createArgumentHandler(args);

        log = new Log(out, argHandler);

        delay = argHandler.getConnectionDelay();

        // calculate number of connection attempts to not exceed WAITTIME
        long timeout = argHandler.getWaitTime() * 60 * 1000;
        attempts = (int) (timeout / delay);
    }

    protected int runIt(String argv[], PrintStream out) {
        try {
            init(argv, out);

            if (shouldPass()) {
                log.display("Tested functionality isn't implemented on this platform. Treat test as passed.");
                return Consts.TEST_PASSED;
            }

            doTest();

            if (isTestFailed)
                return Consts.TEST_FAILED;
            else
                return Consts.TEST_PASSED;

        } catch (Throwable t) {
            out.println("Unexpected exception: " + t);
            t.printStackTrace(out);
            return Consts.TEST_FAILED;
        }
    }

    protected void waitForVMInit(VirtualMachine vm) {
        Debugee.waitForVMInit(vm, log, argHandler.getWaitTime() * 60 * 1000);
    }

    // set connector argument value with given name
    protected void setConnectorArg(Map<String, Connector.Argument> args, String argName, String value) {
        for (String key : args.keySet()) {
            Connector.Argument arg = args.get(key);
            if (arg.name().equals(argName)) {
                arg.setValue(value);
                return;
            }
        }

        throw new Error("There is no argument '" + argName + "'");
    }

    // try attach to target VM using attaching connector
    protected VirtualMachine tryAttach(AttachingConnector connector, Map<String, Connector.Argument> cArgs) {
        // make several attempts to connect to the debuggee VM until WAITTIME exceeds
        for (int i = 0; i < attempts; i++) {
            try {
                return connector.attach(cArgs);
            } catch (IOException e) {
                // could not connect; sleep a few and make new attempt
                log.display("Connection attempt #" + i + " failed: " + e);
                e.printStackTrace(log.getOutStream());
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    testFailed();
                    log.complain("TEST INCOMPLETE: interrupted sleep: " + ie);
                    ie.printStackTrace(log.getOutStream());
                }
            } catch (IllegalConnectorArgumentsException e) {
                testFailed();
                log.complain("TEST: Illegal connector arguments: " + e.getMessage());
                return null;
            } catch (Exception e) {
                testFailed();
                log.complain("TEST: Internal error: " + e.getMessage());
                e.printStackTrace(log.getOutStream());
                return null;
            }
        }

        testFailed();
        // return null after all attempts failed
        log.complain("FAILURE: all attempts to connect to the debuggee VM failed");
        return null;
    }

    // try find connector with given name
    protected Connector findConnector(String connectorName) {
        List<Connector> connectors = Bootstrap.virtualMachineManager().allConnectors();
        Iterator<Connector> iter = connectors.iterator();

        while (iter.hasNext()) {
            Connector connector = (Connector) iter.next();
            if (connector.name().equals(connectorName)) {
                log.display("Connector name=" + connector.name() + "\n\tdescription=" + connector.description() + "\n\ttransport="
                        + connector.transport().name());
                return connector;
            }
        }
        throw new Error("No appropriate connector");
    }

    // wait when debuggee process finishes and check exit code
    protected void waitDebuggeeExit(Debugee debuggee) {
        log.display("\nwaiting for debuggee VM exit");
        int code = debuggee.waitFor();
        if (code != (Consts.JCK_STATUS_BASE + Consts.TEST_PASSED)) {
            testFailed();
            log.complain("Debuggee VM has crashed: exit code=" + code);
            return;
        }
        log.display("debuggee VM: exit code=" + code);
    }

    // wait 'READY' command from debuggee VM (this method is used by debuggers establishing socket connection with debuggee VM)
    protected boolean waitReadyCommand(IOPipe pipe) {
        String command = pipe.readln();
        log.display("Command: " + command);

        if (!command.equals(AbstractDebuggeeTest.COMMAND_READY)) {
            testFailed();
            log.complain("Unexpected debuggee answer: " + command + ", expected is " + AbstractDebuggeeTest.COMMAND_READY);
            return false;
        }

        return true;
    }
}
