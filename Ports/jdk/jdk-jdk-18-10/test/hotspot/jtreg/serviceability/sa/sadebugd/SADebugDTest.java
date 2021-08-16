/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8163805 8224252 8196751
 * @summary Checks that the jshdb debugd utility successfully starts
 *          and tries to attach to a running process
 * @requires vm.hasSA
 * @requires os.family != "windows"
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 *
 * @run driver SADebugDTest
 */

import java.util.concurrent.TimeUnit;

import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.SA.SATestUtils;
import jdk.test.lib.Utils;

import static jdk.test.lib.process.ProcessTools.startProcess;
import jtreg.SkippedException;

public class SADebugDTest {

    private static final String GOLDEN = "Debugger attached";
    private static final String RMI_CONNECTOR_IS_BOUND = "RMI connector is bound to port ";
    private static final String ADDRESS_ALREADY_IN_USE = "Address already in use";

    private static final int REGISTRY_DEFAULT_PORT = 1099;
    private static volatile boolean testResult = false;
    private static volatile boolean portInUse = false;

    public static void main(String[] args) throws Exception {
        SATestUtils.skipIfCannotAttach(); // throws SkippedException if attach not expected to work.
        SATestUtils.validateSADebugDPrivileges();
        runTests();
    }

    private static void runTests() throws Exception {
        boolean[] boolArray = {true, false};
        for (boolean useRmiPort : boolArray) {
            for (boolean useRegistryPort : boolArray) {
                for (boolean useHostname : boolArray) {
                    testWithPid(useRmiPort, useRegistryPort, useHostname);
                }
            }
        }
    }


    private static void testWithPid(final boolean useRmiPort, final boolean useRegistryPort, final boolean useHostName) throws Exception {
        LingeredApp app = null;

        try {
            app = LingeredApp.startApp();
            System.out.println("Started LingeredApp with pid " + app.getPid());

            do {
                testResult = false;
                portInUse = false;
                JDKToolLauncher jhsdbLauncher = JDKToolLauncher.createUsingTestJDK("jhsdb");
                jhsdbLauncher.addVMArgs(Utils.getFilteredTestJavaOpts("-Xcomp"));
                jhsdbLauncher.addToolArg("debugd");
                jhsdbLauncher.addToolArg("--pid");
                jhsdbLauncher.addToolArg(Long.toString(app.getPid()));

                int registryPort = REGISTRY_DEFAULT_PORT;
                if (useRegistryPort) {
                    registryPort = Utils.findUnreservedFreePort(REGISTRY_DEFAULT_PORT);
                    jhsdbLauncher.addToolArg("--registryport");
                    jhsdbLauncher.addToolArg(Integer.toString(registryPort));
                }

                int rmiPort = -1;
                if (useRmiPort) {
                    rmiPort = Utils.findUnreservedFreePort(REGISTRY_DEFAULT_PORT, registryPort);
                    jhsdbLauncher.addToolArg("--rmiport");
                    jhsdbLauncher.addToolArg(Integer.toString(rmiPort));
                }
                if (useHostName) {
                    jhsdbLauncher.addToolArg("--hostname");
                    jhsdbLauncher.addToolArg("testhost");
                }
                ProcessBuilder pb = SATestUtils.createProcessBuilder(jhsdbLauncher);

                final int finalRmiPort = rmiPort;

                // The startProcess will block until the 'golden' string appears in either process' stdout or stderr
                // In case of timeout startProcess kills the debugd process
                Process debugd = startProcess("debugd", pb, null,
                        l -> {
                            if (!useRmiPort && l.contains(GOLDEN)) {
                                testResult = true;
                            } else if (useRmiPort && l.contains(RMI_CONNECTOR_IS_BOUND + finalRmiPort)) {
                                testResult = true;
                            } else if (l.contains(ADDRESS_ALREADY_IN_USE)) {
                                portInUse = true;
                            }
                            return (l.contains(GOLDEN) || portInUse);
                        }, 20, TimeUnit.SECONDS);

                // If we are here, this means we have received the golden line and the test has passed
                // The debugd remains running, we have to kill it
                debugd.destroy();
                debugd.waitFor();

                if (!testResult) {
                    throw new RuntimeException("Expected message \"" +
                            RMI_CONNECTOR_IS_BOUND + rmiPort + "\" is not found in the output.");
                }

            } while (portInUse); // Repeat the test if the port is already in use
        } finally {
            LingeredApp.stopApp(app);
        }
    }
}
