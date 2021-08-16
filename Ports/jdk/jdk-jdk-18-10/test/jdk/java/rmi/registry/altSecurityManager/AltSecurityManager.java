/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4183202
 * @summary rmiregistry could allow alternate security manager
 * @author Laird Dornin
 *
 * @library ../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 *          java.base/sun.nio.ch
 * @build TestLibrary RegistryVM RMIRegistryRunner TestSecurityManager
 * @run main/othervm AltSecurityManager
 */

/**
 * Ensure that a user is able to specify alternate security managers to
 * be used in rmiregistry.  Test specifies a security manager
 * that throws a runtime exception in its checkListen method, this
 * will cause rmiregistry to exit early because those
 * utilities will be unable to export any remote objects; test fails
 * if registry takes too long to exit.
 */
public class AltSecurityManager implements Runnable {
    // variable to hold registry child
    static JavaVM vm = null;

    // names of utilities
    static String utilityToStart = null;
    static final String REGISTRY_IMPL = "sun.rmi.registry.RegistryImpl";

    // children should exit in at least this time.
    private static final long TIME_OUT =
            (long)(15000 * TestLibrary.getTimeoutFactor());

    public void run() {
        try {
            if (utilityToStart.equals(REGISTRY_IMPL)) {
                vm = RegistryVM.createRegistryVMWithRunner(
                        "RMIRegistryRunner",
                        "-Djava.security.manager=TestSecurityManager");
            } else {
                TestLibrary.bomb("Utility to start must be " + REGISTRY_IMPL);
            }

            System.err.println("starting " + utilityToStart);
            try {
                vm.start();
                throw new RuntimeException("Expected exception did not occur!");
            } catch (Exception expected) {
                int exit = vm.waitFor();
                if (exit != TestSecurityManager.EXIT_VALUE) {
                    throw new RuntimeException(utilityToStart
                            + " exit with an unexpected value "
                            + exit + ".");
                }
                System.err.format("Success: starting %s exited with status %d%n",
                                  utilityToStart, TestSecurityManager.EXIT_VALUE);
            }

        } catch (Exception e) {
            TestLibrary.bomb(e);
        }
    }

    /**
     * Wait to make sure that the registry exits after
     * their security manager is set.
     */
    public static void ensureExit(String utility) throws Exception {
        utilityToStart = utility;

        try {
            Thread thread = new Thread(new AltSecurityManager());
            System.err.println("expecting RuntimeException for " +
                               "checkListen in child process");
            long start = System.currentTimeMillis();
            thread.start();
            thread.join(TIME_OUT);

            long time = System.currentTimeMillis() - start;
            System.err.println("waited " + time + " millis for " +
                               utilityToStart + " to die");

            if (time >= TIME_OUT) {
                TestLibrary.bomb(utilityToStart +
                                 " took too long to die...");
            } else {
                System.err.println(utilityToStart +
                                   " terminated on time");
            }
        } finally {
            vm.cleanup();
            vm = null;
        }
    }

    public static void main(String[] args) {
        try {
            System.err.println("\nRegression test for bug 4183202\n");

            // make sure the registry exits early.
            ensureExit(REGISTRY_IMPL);

            System.err.println("test passed");

        } catch (Exception e) {
            TestLibrary.bomb(e);
        }
    }
}
