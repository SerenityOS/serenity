/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package jdk.jshell.execution;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.ListeningConnector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;

/**
 * Sets up a JDI connection, providing the resulting JDI {@link VirtualMachine}
 * and the {@link Process} the remote agent is running in.
 *
 * @since 9
 */
public class JdiInitiator {

    // factor for the timeout on all of connect
    private static final double CONNECT_TIMEOUT_FACTOR = 1.5;

    // Over-all connect time-out
    private final int connectTimeout;

    private VirtualMachine vm;
    private Process process = null;
    private final Connector connector;
    private final String remoteAgent;
    private final Map<String, com.sun.jdi.connect.Connector.Argument> connectorArgs;

    /**
     * Start the remote agent and establish a JDI connection to it.
     *
     * @param port the socket port for (non-JDI) commands
     * @param remoteVMOptions any user requested VM command-line options
     * @param remoteAgent full class name of remote agent to launch
     * @param isLaunch does JDI do the launch? That is, LaunchingConnector,
     * otherwise we start explicitly and use ListeningConnector
     * @param host explicit hostname to use, if null use discovered
     * hostname, applies to listening only (!isLaunch)
     * @param timeout the start-up time-out in milliseconds. If zero or negative,
     * will not wait thus will timeout immediately if not already started.
     * @param customConnectorArgs custom arguments passed to the connector.
     * These are JDI com.sun.jdi.connect.Connector arguments.
     */
    public JdiInitiator(int port, List<String> remoteVMOptions, String remoteAgent,
            boolean isLaunch, String host, int timeout,
            Map<String, String> customConnectorArgs) {
        this.remoteAgent = remoteAgent;
        this.connectTimeout = (int) (timeout * CONNECT_TIMEOUT_FACTOR);
        String connectorName
                = isLaunch
                        ? "com.sun.jdi.CommandLineLaunch"
                        : "com.sun.jdi.SocketListen";
        this.connector = findConnector(connectorName);
        if (connector == null) {
            throw new IllegalArgumentException("No connector named: " + connectorName);
        }
        Map<String, String> argumentName2Value
                = isLaunch
                        ? launchArgs(port, String.join(" ", remoteVMOptions))
                        : new HashMap<>();
        if (!isLaunch) {
            argumentName2Value.put("timeout", ""+timeout);
            if (host != null && !isLaunch) {
                argumentName2Value.put("localAddress", host);
            }
        }
        argumentName2Value.putAll(customConnectorArgs);
        this.connectorArgs = mergeConnectorArgs(connector, argumentName2Value);
        this.vm = isLaunch
                ? launchTarget()
                : listenTarget(port, remoteVMOptions);

    }

    /**
     * Returns the resulting {@code VirtualMachine} instance.
     *
     * @return the virtual machine
     */
    public VirtualMachine vm() {
        return vm;
    }

    /**
     * Returns the launched process.
     *
     * @return the remote agent process
     */
    public Process process() {
        return process;
    }

    /* launch child target vm */
    private VirtualMachine launchTarget() {
        LaunchingConnector launcher = (LaunchingConnector) connector;
        try {
            VirtualMachine new_vm = timedVirtualMachineCreation(() -> launcher.launch(connectorArgs), null);
            process = new_vm.process();
            return new_vm;
        } catch (Throwable ex) {
            throw reportLaunchFail(ex, "launch");
        }
    }

    /**
     * Directly launch the remote agent and connect JDI to it with a
     * ListeningConnector.
     */
    private VirtualMachine listenTarget(int port, List<String> remoteVMOptions) {
        ListeningConnector listener = (ListeningConnector) connector;
        // Files to collection to output of a start-up failure
        File crashErrorFile = createTempFile("error");
        File crashOutputFile = createTempFile("output");
        try {
            // Start listening, get the JDI connection address
            String addr = listener.startListening(connectorArgs);
            debug("Listening at address: " + addr);

            // Launch the RemoteAgent requesting a connection on that address
            String javaHome = System.getProperty("java.home");
            List<String> args = new ArrayList<>();
            args.add(javaHome == null
                    ? "java"
                    : javaHome + File.separator + "bin" + File.separator + "java");
            args.add("-agentlib:jdwp=transport=" + connector.transport().name() +
                    ",address=" + addr);
            args.addAll(remoteVMOptions);
            args.add(remoteAgent);
            args.add("" + port);
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectError(crashErrorFile);
            pb.redirectOutput(crashOutputFile);
            process = pb.start();

            // Accept the connection from the remote agent
            vm = timedVirtualMachineCreation(() -> listener.accept(connectorArgs),
                    () -> process.waitFor());
            try {
                listener.stopListening(connectorArgs);
            } catch (IOException | IllegalConnectorArgumentsException ex) {
                // ignore
            }
            crashErrorFile.delete();
            crashOutputFile.delete();
            return vm;
        } catch (Throwable ex) {
            if (process != null) {
                process.destroyForcibly();
            }
            try {
                listener.stopListening(connectorArgs);
            } catch (IOException | IllegalConnectorArgumentsException iex) {
                // ignore
            }
            String text = readFile(crashErrorFile) + readFile(crashOutputFile);
            crashErrorFile.delete();
            crashOutputFile.delete();
            if (text.isEmpty()) {
                throw reportLaunchFail(ex, "listen");
            } else {
                throw new IllegalArgumentException(text);
            }
        }
    }

    private File createTempFile(String label) {
        try {
            File f = File.createTempFile("remote", label);
            f.deleteOnExit();
            return f;
        } catch (IOException ex) {
            throw new InternalError("Failed create temp ", ex);
        }
    }

    private String readFile(File f) {
        try {
            return new String(Files.readAllBytes(f.toPath()),
                    StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "error reading " + f + " : " + ex.toString();
        }
    }

    VirtualMachine timedVirtualMachineCreation(Callable<VirtualMachine> creator,
            Callable<Integer> processComplete) throws Exception {
        VirtualMachine result;
        ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setDaemon(true);
            return thread;
        });
        try {
            Future<VirtualMachine> future = executor.submit(creator);
            if (processComplete != null) {
                executor.submit(() -> {
                    Integer i = processComplete.call();
                    future.cancel(true);
                    return i;
                });
            }

            try {
                result = future.get(connectTimeout, TimeUnit.MILLISECONDS);
            } catch (TimeoutException ex) {
                future.cancel(true);
                throw ex;
            }
        } finally {
            executor.shutdownNow();
        }
        return result;
    }

    private Connector findConnector(String name) {
        for (Connector cntor
                : Bootstrap.virtualMachineManager().allConnectors()) {
            if (cntor.name().equals(name)) {
                return cntor;
            }
        }
        return null;
    }

    private Map<String, Connector.Argument> mergeConnectorArgs(Connector connector, Map<String, String> argumentName2Value) {
        Map<String, Connector.Argument> arguments = connector.defaultArguments();

        for (Entry<String, String> argumentEntry : argumentName2Value.entrySet()) {
            String name = argumentEntry.getKey();
            String value = argumentEntry.getValue();
            Connector.Argument argument = arguments.get(name);

            if (argument == null) {
                throw new IllegalArgumentException("Argument is not defined for connector:" +
                        name + " -- " + connector.name());
            }

            argument.setValue(value);
        }

        return arguments;
    }

    /**
     * The JShell specific Connector args for the LaunchingConnector.
     *
     * @param portthe socket port for (non-JDI) commands
     * @param remoteVMOptions any user requested VM options
     * @return the argument map
     */
    private Map<String, String> launchArgs(int port, String remoteVMOptions) {
        Map<String, String> argumentName2Value = new HashMap<>();
        argumentName2Value.put("main", remoteAgent + " " + port);
        argumentName2Value.put("options", remoteVMOptions);
        return argumentName2Value;
    }

    private InternalError reportLaunchFail(Throwable ex, String context) {
        return new InternalError("Failed remote " + context + ": "
                + ex.toString()
                + " @ " + connector +
                " -- " + connectorArgs, ex);
    }

    /**
     * Log debugging information. Arguments as for {@code printf}.
     *
     * @param format a format string as described in Format string syntax
     * @param args arguments referenced by the format specifiers in the format
     * string.
     */
    private void debug(String format, Object... args) {
        // Reserved for future logging
    }

    /**
     * Log a serious unexpected internal exception.
     *
     * @param ex the exception
     * @param where a description of the context of the exception
     */
    private void debug(Throwable ex, String where) {
        // Reserved for future logging
    }

}
