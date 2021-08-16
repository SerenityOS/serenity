/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.OutputStream;
import java.io.IOException;

/**
 * Class to run and control registry/rmiregistry in a sub-process.
 * The behaviour changes when use different runner, currently
 * there are 2 built-in runners, RegistryRunner and RMIRegistryRunner.
 *
 * We can't kill a registry if we have too-close control
 * over it.  We must make it in a subprocess, and then kill the
 * subprocess when it has served our needs.
 */
public class RegistryVM extends JavaVM {

    private static final double START_TIMEOUT =
            20_000 * TestLibrary.getTimeoutFactor();
    private static final String DEFAULT_RUNNER = "RegistryRunner";

    private int port = -1;

    private RegistryVM(String runner, OutputStream out, OutputStream err,
                    String options, int port) {
        super(runner, options, Integer.toString(port), out, err);
        try {
            Class runnerClass = Class.forName(runner);
            if (!RegistryRunner.class.isAssignableFrom(runnerClass)) {
                throw new RuntimeException("runner class must be RegistryRunner"
                        + " or its sub class");
            }
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
        this.port = port;
    }

    /**
     * Create a RegistryVM instance on an ephemeral port.
     *
     * @return a RegistryVM instance
     */
    public static RegistryVM createRegistryVM() {
        return createRegistryVMWithRunner(DEFAULT_RUNNER, System.out, System.err, "", 0);
    }

    /**
     * Create a RegistryVM instance on an ephemeral port with additional
     * command line options.
     *
     * @param options command line options
     * @return a RegistryVM instance
     */
    public static RegistryVM createRegistryVM(String options) {
        return createRegistryVMWithRunner(
                DEFAULT_RUNNER, System.out, System.err, options, 0);
    }

    /**
     * Create a RegistryVM instance on a specified port capturing stdout and
     * stderr with additional command line options.
     *
     * @param out the OutputStream where the normal output of the
     *            registry subprocess goes
     * @param err the OutputStream where the error output of the
     *            registry subprocess goes
     * @param options the command line options
     * @param port the port on which Registry accepts requests
     * @return a RegistryVM instance
     */
    public static RegistryVM createRegistryVM(OutputStream out, OutputStream err,
                                              String options, int port) {
        return createRegistryVMWithRunner(DEFAULT_RUNNER, out, err, options, port);
    }

    /**
     * Create a RegistryVM instance on an ephemeral port with additional
     * command line options and a specified runner.
     *
     * @param runner the runner class name
     * @param options command line options
     * @return a RegistryVM instance
     */
    public static RegistryVM createRegistryVMWithRunner(String runner, String options) {
        return createRegistryVMWithRunner(runner, System.out, System.err, options, 0);
    }

    /**
     * Create a RegistryVM instance on a specified port capturing stdout and
     * stderr with additional command line options and a specified runner.
     *
     * @param runner the runner class name
     * @param out the OutputStream where the normal output of the
     *            registry subprocess goes
     * @param err the OutputStream where the error output of the
     *            registry subprocess goes
     * @param options the command line options
     * @param port the port on which Registry accepts requests
     * @return a RegistryVM instance
     */
    public static RegistryVM createRegistryVMWithRunner(String runner, OutputStream out,
                                        OutputStream err, String options, int port) {
        options += " --add-exports=java.rmi/sun.rmi.registry=ALL-UNNAMED"
                + " --add-exports=java.rmi/sun.rmi.server=ALL-UNNAMED"
                + " --add-exports=java.rmi/sun.rmi.transport=ALL-UNNAMED"
                + " --add-exports=java.rmi/sun.rmi.transport.tcp=ALL-UNNAMED";
        RegistryVM reg = new RegistryVM(runner, out, err, options, port);
        reg.setPolicyFile(TestParams.defaultRegistryPolicy);
        return reg;
    }

    /**
     * Starts the registry in a sub-process and waits up to
     * the given timeout period to confirm that it's running,
     * and get the port where it's running.
     *
     * @throws IOException if fails to start subprocess
     */
    public void start() throws IOException {
        super.start();
        long startTime = System.currentTimeMillis();
        long deadline = TestLibrary.computeDeadline(startTime, (long)START_TIMEOUT);
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) { }

            String output = outputStream.ba.toString();
            port = RegistryRunner.getRegistryPort(output);
            if (port != -1) {
                break;
            }
            try {
                int exit = vm.exitValue();
                TestLibrary.bomb("[RegistryVM] registry sub-process exited with status "
                        + exit + ".");
            } catch (IllegalThreadStateException ignore) { }

            if (System.currentTimeMillis() > deadline) {
                TestLibrary.bomb("Failed to start registry, giving up after " +
                    (System.currentTimeMillis() - startTime) + "ms.", null);
            }
        }
    }

    /**
     * Shuts down the registry.
     */
    @Override
    public void cleanup() {
        RegistryRunner.requestExit(port);
        super.destroy();
    }

    /**
     * Gets the port where the registry is serving.
     *
     * @return the port where the registry is serving
     */
    public int getPort() {
        return port;
    }
}
