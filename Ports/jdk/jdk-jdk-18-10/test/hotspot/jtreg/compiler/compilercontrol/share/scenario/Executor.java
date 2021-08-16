/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

package compiler.compilercontrol.share.scenario;

import compiler.compilercontrol.share.actions.BaseAction;
import jdk.test.lib.Asserts;
import jdk.test.lib.management.InputArguments;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.PidJcmdExecutor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Executable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Executor {
    private final boolean isValid;
    private final List<String> vmOptions;
    private final Map<Executable, State> states;
    private final List<String> jcmdCommands;
    private final String execClass = System.getProperty("compiler."
            + "compilercontrol.share.executor.executeClass",
            BaseAction.class.getName());
    private OutputAnalyzer[] jcmdOutputAnalyzers;

    /**
     * Constructor
     *
     * @param isValid      shows that the input given to the VM is valid and
     *                     VM shouldn't fail
     * @param vmOptions    a list of VM input options
     * @param states       a state map, or null for the non-checking execution
     * @param jcmdCommands a list of diagnostic commands to be preformed
     *                     on test VM
     */
    public Executor(boolean isValid, List<String> vmOptions,
            Map<Executable, State> states, List<String> jcmdCommands) {
        this.isValid = isValid;
        if (vmOptions == null) {
            this.vmOptions = new ArrayList<>();
        } else {
            this.vmOptions = vmOptions;
        }
        this.states = states;
        this.jcmdCommands = jcmdCommands;
    }

    /**
     * Executes separate VM a gets an OutputAnalyzer instance with the results
     * of execution
     */
    public List<OutputAnalyzer> execute() {
        // Add class name that would be executed in a separate VM
        vmOptions.add(execClass);
        OutputAnalyzer output;
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            {
                // Get port test VM will connect to
                int port = serverSocket.getLocalPort();
                if (port == -1) {
                    throw new Error("Socket is not bound: " + port);
                }
                vmOptions.add(String.valueOf(port));
                if (states != null) {
                    // add flag to show that there should be state map passed
                    vmOptions.add("states");
                }
                // Start separate thread to connect with test VM
                new Thread(() -> connectTestVM(serverSocket)).start();
            }
            // Start a test VM using vm flags from @run and from vm options
            String[] vmInputArgs = InputArguments.getVmInputArgs();
            String[] cmds = Arrays.copyOf(vmInputArgs,
                    vmInputArgs.length + vmOptions.size());
            System.arraycopy(vmOptions.toArray(), 0, cmds, vmInputArgs.length,
                    vmOptions.size());
            output = ProcessTools.executeTestJvm(cmds);
        } catch (Throwable thr) {
            throw new Error("Execution failed: " + thr.getMessage(), thr);
        }

        List<OutputAnalyzer> outputList = new ArrayList<>();
        outputList.add(output);
        if (jcmdOutputAnalyzers != null) {
            Collections.addAll(outputList, jcmdOutputAnalyzers);
        }
        return outputList;
    }

    /*
     * Performs connection with a test VM, sends method states and performs
     * JCMD operations on a test VM.
     */
    private void connectTestVM(ServerSocket serverSocket) {
        /*
         * There are no way to prove that accept was invoked before we started
         * test VM that connects to this serverSocket. Connection timeout is
         * enough
         */
        try (
                Socket socket = serverSocket.accept();
                PrintWriter pw = new PrintWriter(socket.getOutputStream(),
                        true);
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()))) {
            // Get pid of the executed process
            int pid = Integer.parseInt(in.readLine());
            Asserts.assertNE(pid, 0, "Got incorrect pid");
            jcmdOutputAnalyzers = executeJCMD(pid);
            if (states != null) {
                // serialize and send state map
                states.forEach((executable, state) -> {
                    pw.println("{");
                    pw.println(executable.toGenericString());
                    pw.println(state.toString());
                    pw.println("}");
                });
            } else {
                pw.println();
            }
        } catch (IOException e) {
            // hotspot process may exit because of invalid directives,
            // suppress the IOException of closed socket.
            if (!e.getMessage().equals("Socket closed")) {
              throw new Error("Failed to write data: " + e.getMessage(), e);
            }
        }
    }

    // Executes all diagnostic commands
    protected OutputAnalyzer[] executeJCMD(int pid) {
        int size = jcmdCommands.size();
        OutputAnalyzer[] outputArray = new OutputAnalyzer[size];
        CommandExecutor jcmdExecutor = new PidJcmdExecutor(String.valueOf(pid));
        for (int i = 0; i < size; i++) {
            outputArray[i] = jcmdExecutor.execute(jcmdCommands.get(i));
        }
        return outputArray;
    }
}
