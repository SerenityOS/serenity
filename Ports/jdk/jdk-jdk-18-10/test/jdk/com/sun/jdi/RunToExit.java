/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4997445
 * @summary Test that with server=y, when VM runs to System.exit() no error happens
 * @library /test/lib
 * @modules java.management
 *          jdk.jdi
 * @build VMConnection RunToExit Exit0
 * @run driver RunToExit
 */
import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.*;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.AttachingConnector;
import java.net.ConnectException;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jdk.test.lib.JDWP;
import jdk.test.lib.process.ProcessTools;

public class RunToExit {

    /* Increment this when ERROR: seen */
    static volatile int error_seen = 0;
    static volatile boolean ready = false;

    /* port the debuggee is listening on */
    private static String address;

    /*
     * Find a connector by name
     */
    private static Connector findConnector(String name) {
        List connectors = Bootstrap.virtualMachineManager().allConnectors();
        Iterator iter = connectors.iterator();
        while (iter.hasNext()) {
            Connector connector = (Connector)iter.next();
            if (connector.name().equals(name)) {
                return connector;
            }
        }
        return null;
    }

    /*
     * Launch a server debuggee, detect debuggee listening port
     */
    private static Process launch(String class_name) throws Exception {
        String args[] = new String[]{
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=0",
            class_name
        };
        args = VMConnection.insertDebuggeeVMOptions(args);

        ProcessBuilder launcher = ProcessTools.createJavaProcessBuilder(args);

        System.out.println(launcher.command().stream().collect(Collectors.joining(" ", "Starting: ", "")));

        Process p = ProcessTools.startProcess(
            class_name,
            launcher,
            RunToExit::checkForError,
            RunToExit::isTransportListening,
            0,
            TimeUnit.NANOSECONDS
        );

        return p;
    }

    private static boolean isTransportListening(String line) {
        JDWP.ListenAddress addr = JDWP.parseListenAddress(line);
        if (addr == null) {
            return false;
        }
        address = addr.address();
        return true;
    }

    private static void checkForError(String line) {
        if (line.contains("ERROR:")) {
            error_seen++;
        }
    }

    /*
     * - Launch a server debuggee: server=y,suspend=y,address=0
     * - detect the port debuggee is listening on
     * - run it to VM death
     * - verify we saw no error
     */
    public static void main(String args[]) throws Exception {
        // launch the server debuggee
        Process process = launch("Exit0");

        // attach to server debuggee and resume it so it can exit
        AttachingConnector conn = (AttachingConnector)findConnector("com.sun.jdi.SocketAttach");
        Map conn_args = conn.defaultArguments();
        Connector.IntegerArgument port_arg =
            (Connector.IntegerArgument)conn_args.get("port");
        port_arg.setValue(address);

        System.out.println("Connection arguments: " + conn_args);

        VirtualMachine vm = null;
        while (vm == null) {
            try {
                vm = conn.attach(conn_args);
            } catch (ConnectException e) {
                e.printStackTrace(System.out);
                System.out.println("--- Debugee not ready. Retrying in 500ms. ---");
                Thread.sleep(500);
            }
        }

        // The first event is always a VMStartEvent, and it is always in
        // an EventSet by itself.  Wait for it.
        EventSet evtSet = vm.eventQueue().remove();
        for (Event event: evtSet) {
            if (event instanceof VMStartEvent) {
                break;
            }
            throw new RuntimeException("Test failed - debuggee did not start properly");
        }
        vm.eventRequestManager().deleteAllBreakpoints();
        vm.resume();

        int exitCode = process.waitFor();

        // if the server debuggee ran cleanly, we assume we were clean
        if (exitCode == 0 && error_seen == 0) {
            System.out.println("Test passed - server debuggee cleanly terminated");
        } else {
            throw new RuntimeException("Test failed - server debuggee generated an error when it terminated, " +
                "exit code was " + exitCode + ", " + error_seen + " error(s) seen in debugee output.");
        }
    }
}
