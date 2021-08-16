/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * Note: What seems to be an excessive use of System.xxx.flush();
 * is actually necessary to combat lost output on win32 systems.
 *
 * @test
 * @bug 4291701 4376819 4422312 4522770
 * @summary Test JIT debugging -
 * assure that launching on uncaught exception works
 *
 * @library /test/lib
 *
 * @author Robert Field
 * @run main/othervm JITDebug
 */

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;

import java.util.*;

/*
 * This class implements three separate small programs, each
 * of which (directly or indirectly) invokes the next.  These
 * programs are:
 *    test launcher -
 *        Runs the debug target.  It exists to work around a
 *        bug in the test tools which do not allow quoted spaces
 *        in command lines.  It launchs the debug target in
 *        such a way that when it encounters an uncaught exception
 *        it (in turn) will launch the trivial debugger.
 *    debug target -
 *        A one line program which throws an uncaught exception.
 *    trivial debugger -
 *        A debugger which attachs to the debug target and shuts
 *        it down with a zero exit code.
 * These programs are differentiated by their command line arguments:
 *    test launcher - (no args)
 *    debug target - ("TARGET")
 *    trivial debugger - ("DEBUGGER", host and port)
 */
public class JITDebug {

    public static void main(String[] args) {
        if (!new JITDebug().parseArgs(args)) {
            throw new RuntimeException("Unexpected command line arguments: "
                                       + args);
        }
    }

    boolean parseArgs(String[] args) {
        switch (args.length) {
        case 0:
            testLaunch();
            return true;
        case 1:
            if (args[0].equals("TARGET")) {
                debugTarget();
                return true;
            } else {
                return false;
            }
        case 3:
            if (args[0].equals("DEBUGGER")) {
                // launched by using "-agentlib:" "launch" sub-option:
                // The following strings are appended to the string given in this argument (space-delimited).
                // They can aid the launched debugger in establishing a connection with this VM.
                // The resulting string is executed.
                // - The value of the transport sub-option.
                // - The value of the address sub-option (or the generated address if one is not given)
                trivialDebugger(args[1], args[2]);
                return true;
            } else {
                return false;
            }
        default:
            return false;
        }
    }

    void testLaunch() {
        ProcessBuilder pb = ProcessTools.createTestJvm();
        List largs = pb.command();
        largs.add("-classpath");
        largs.add(Utils.TEST_CLASSES);
        String javaExe = JDKToolFinder.getJDKTool("java");
        largs.add("-agentlib:jdwp=transport=dt_socket,server=y,onuncaught=y," +
                  "launch=" + javaExe + " " + this.getClass().getName() + " DEBUGGER ");
        largs.add(this.getClass().getName());
        largs.add("TARGET");
        try {
            ProcessTools.executeCommand(pb)
                    .shouldHaveExitValue(0);
        } catch (Throwable exc) {
            throw new RuntimeException("Unexpected exception: " + exc);
        }
        System.out.println("JIT Debugging test PASSED");
    }

    // Target VM code
    void debugTarget() {
        System.out.flush();
        System.out.println("trigger onuncaught launch");
        System.out.flush();
        throw new RuntimeException("Start-up onuncaught handling");
    }

    void trivialDebugger(String transportMethod, String transportAddress) {
        System.out.println("trivial debugger started");
        String connectorName = null;
        if ("dt_shmem".equals(transportMethod)) {
            connectorName = "com.sun.jdi.SharedMemoryAttach";
        } else if ("dt_socket".equals(transportMethod)) {
            connectorName = "com.sun.jdi.SocketAttach";
        } else {
            System.err.flush();
            System.err.println("Unknown transportMethod: " + transportMethod + " - hanging");
            System.err.flush();
            hang();
        }
        List conns = Bootstrap.virtualMachineManager().attachingConnectors();
        for (Iterator it = conns.iterator(); it.hasNext(); ) {
            AttachingConnector conn = (AttachingConnector)it.next();
            if (conn.name().equals(connectorName)) {
                doAttach(connectorName, conn, transportAddress);
                return;
            }
        }
        System.err.flush();
        System.err.println("No attaching connector matching: " + connectorName + " - hanging");
        System.err.flush();
        hang();
    }

    void doAttach(String connectorName, AttachingConnector conn, String transportAddress) {
        Map<String, Connector.Argument> connArgs = conn.defaultArguments();
        if ("com.sun.jdi.SharedMemoryAttach".equals(connectorName)) {
            Connector.Argument portArg = (Connector.Argument)connArgs.get("name");
            portArg.setValue(transportAddress);
        } else {
            Connector.Argument portArg = (Connector.Argument)connArgs.get("port");
            portArg.setValue(transportAddress);
        }
        try {
            VirtualMachine vm = conn.attach(connArgs);
            System.out.println("attached to: " + transportAddress);
            vm.exit(0); // we are happy - terminate VM with no error
            System.out.println("we are happy - terminated VM with no error");
        } catch (Exception exc) {
            System.err.flush();
            System.err.println("Exception: " + exc + " - hanging");
            System.err.flush();
            hang();
        }
    }

    /** Hang so that test fails */
    void hang() {
        try {
            // ten minute nap
            Thread.sleep(10 * 60 * 1000);
        } catch (InterruptedException exc) {
            // shouldn't happen
        }
    }
}
