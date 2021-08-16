/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.List;

import static jdk.test.lib.Asserts.*;
import jdk.test.lib.Utils;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.thread.ProcessThread;

/**
 * The base class for tests of jstatd.
 *
 * The test sequence for TestJstatdDefaults for example is:
 * <pre>
 * {@code
 * // start jstatd process
 * jstatd -J-XX:+UsePerfData -J-Djava.security.policy=all.policy
 *
 * // run jps and verify its output
 * jps -J-XX:+UsePerfData hostname
 *
 * // run jstat and verify its output
 * jstat -J-XX:+UsePerfData -J-Duser.language=en -gcutil pid@hostname 250 5
 *
 * // stop jstatd process and verify that no unexpected exceptions have been thrown
 * }
 * </pre>
 */
public final class JstatdTest {

    /**
     * jstat gcutil option: takes JSTAT_GCUTIL_SAMPLES samples at
     * JSTAT_GCUTIL_INTERVAL_MS millisecond intervals
     */
    private static final int JSTAT_GCUTIL_SAMPLES = 5;
    private static final int JSTAT_GCUTIL_INTERVAL_MS = 250;
    private static final String JPS_OUTPUT_REGEX = "^\\d+\\s*.*";

    private boolean useDefaultPort = true;
    private boolean useDefaultRmiPort = true;
    private String port;
    private String rmiPort;
    private String serverName;
    private Long jstatdPid;
    private boolean withExternalRegistry = false;
    private boolean useShortCommandSyntax = false;

    private volatile static boolean portInUse;

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void setUseDefaultPort(boolean useDefaultPort) {
        this.useDefaultPort = useDefaultPort;
    }

    public void setUseDefaultRmiPort(boolean useDefaultRmiPort) {
        this.useDefaultRmiPort = useDefaultRmiPort;
    }

    public void setWithExternalRegistry(boolean withExternalRegistry) {
        this.withExternalRegistry = withExternalRegistry;
    }

    private Long waitOnTool(ProcessThread thread) throws Throwable {
        long pid = thread.getPid();
        if (portInUse) {
            System.out.println("Port already in use. Trying to restart with a new one...");
            return null;
        }
        System.out.println(thread.getName() + " pid: " + pid);
        return pid;
    }

    private void log(String caption, String... cmd) {
        System.out.println(Utils.NEW_LINE + caption + ":");
        System.out.println(Arrays.toString(cmd).replace(",", ""));
    }

    private String getDestination() throws UnknownHostException {
        String option = Utils.getHostname();
        if (port != null) {
            option += ":" + port;
        }
        if (serverName != null) {
            option += "/" + serverName;
        }
        return option;
    }

    /**
     * Depending on test settings command line can look like:
     *
     * jps -J-XX:+UsePerfData hostname
     * jps -J-XX:+UsePerfData hostname:port
     * jps -J-XX:+UsePerfData hostname/serverName
     * jps -J-XX:+UsePerfData hostname:port/serverName
     */
    private OutputAnalyzer runJps() throws Exception {
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("jps");
        launcher.addVMArgs(Utils.getFilteredTestJavaOpts("-XX:+UsePerfData"));
        launcher.addVMArg("-XX:+UsePerfData");
        launcher.addToolArg(getDestination());

        String[] cmd = launcher.getCommand();
        log("Start jps", cmd);

        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        OutputAnalyzer output = ProcessTools.executeProcess(processBuilder);
        System.out.println(output.getOutput());

        return output;
    }

    /**
     * Verifies output form jps contains pids and programs' name information.
     * The function will discard any lines that come before the first line with pid.
     * This can happen if the JVM outputs a warning message for some reason
     * before running jps.
     *
     * The output can look like:
     * 35536 Jstatd
     * 35417 Main
     * 31103 org.eclipse.equinox.launcher_1.3.0.v20120522-1813.jar
     */
    private void verifyJpsOutput(OutputAnalyzer output) throws Exception {
        output.shouldHaveExitValue(0);
        assertFalse(output.getOutput().isEmpty(), "Output should not be empty");

        boolean foundFirstLineWithPid = false;
        List<String> lines = output.asLinesWithoutVMWarnings();
        for (String line : lines) {
            if (!foundFirstLineWithPid) {
                foundFirstLineWithPid = line.matches(JPS_OUTPUT_REGEX);
                continue;
            }
            assertTrue(line.matches(JPS_OUTPUT_REGEX),
                    "Output does not match the pattern" + Utils.NEW_LINE + line);
        }
        assertTrue(foundFirstLineWithPid, "Invalid output");
    }

    /**
     * Depending on test settings command line can look like:
     *
     * jstat -J-XX:+UsePerfData -J-Duser.language=en -gcutil pid@hostname 250 5
     * jstat -J-XX:+UsePerfData -J-Duser.language=en -gcutil pid@hostname:port 250 5
     * jstat -J-XX:+UsePerfData -J-Duser.language=en -gcutil pid@hostname/serverName 250 5
     * jstat -J-XX:+UsePerfData -J-Duser.language=en -gcutil pid@hostname:port/serverName 250 5
     */
    private OutputAnalyzer runJstat() throws Exception {
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("jstat");
        launcher.addVMArg("-XX:+UsePerfData");
        launcher.addVMArg("-Duser.language=en");
        launcher.addToolArg("-gcutil");
        launcher.addToolArg(jstatdPid + "@" + getDestination());
        launcher.addToolArg(Integer.toString(JSTAT_GCUTIL_INTERVAL_MS));
        launcher.addToolArg(Integer.toString(JSTAT_GCUTIL_SAMPLES));

        String[] cmd = launcher.getCommand();
        log("Start jstat", cmd);

        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        OutputAnalyzer output = ProcessTools.executeProcess(processBuilder);
        System.out.println(output.getOutput());

        return output;
    }

    private void verifyJstatOutput(OutputAnalyzer output)
            throws Exception {
        output.shouldHaveExitValue(0);
        assertFalse(output.getOutput().isEmpty(), "Output should not be empty");

        JstatGCUtilParser gcUtilParser = new JstatGCUtilParser(
                output.getOutput());
        gcUtilParser.parse(JSTAT_GCUTIL_SAMPLES);
    }

    private void runToolsAndVerify() throws Exception {
        OutputAnalyzer output = runJps();
        verifyJpsOutput(output);

        output = runJstat();
        verifyJstatOutput(output);
    }

    private Registry startRegistry()
            throws InterruptedException, RemoteException {
        Registry registry = null;
        try {
            System.out.println("Start rmiregistry on port " + port);
            registry = LocateRegistry
                    .createRegistry(Integer.parseInt(port));
        } catch (RemoteException e) {
            if (e.getMessage().contains("Port already in use")) {
                System.out.println("Port already in use. Trying to restart with a new one...");
                Thread.sleep(100);
                return null;
            } else {
                throw e;
            }
        }
        return registry;
    }

    private void cleanUpThread(ProcessThread thread) throws Throwable {
        if (thread != null) {
            thread.stopProcess();
            thread.joinAndThrow();
        }
    }

    /**
     * Depending on test settings command line can look like:
     *
     * jstatd -J-XX:+UsePerfData -J-Djava.security.policy=all.policy
     * jstatd -J-XX:+UsePerfData -J-Djava.security.policy=all.policy -p port
     * jstatd -J-XX:+UsePerfData -J-Djava.security.policy=all.policy -p port -r rmiport
     * jstatd -J-XX:+UsePerfData -J-Djava.security.policy=all.policy -n serverName
     * jstatd -J-XX:+UsePerfData -J-Djava.security.policy=all.policy -p port -n serverName
     */
    private String[] getJstatdCmd() throws Exception {
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("jstatd");
        launcher.addVMArg("-XX:+UsePerfData");
        launcher.addVMArg("-Djava.security.manager=allow");
        String testSrc = System.getProperty("test.src");
        File policy = new File(testSrc, "all.policy");
        assertTrue(policy.exists() && policy.isFile(),
                "Security policy " + policy.getAbsolutePath() + " does not exist or not a file");
        launcher.addVMArg("-Djava.security.policy=" + policy.getAbsolutePath());
        if (port != null) {
            addToolArg(launcher,"-p", port);
        }
        if (rmiPort != null) {
            addToolArg(launcher,"-r", rmiPort);
        }
        if (serverName != null) {
            addToolArg(launcher,"-n", serverName);
        }
        if (withExternalRegistry) {
            launcher.addToolArg("-nr");
        }
        String[] cmd = launcher.getCommand();
        log("Start jstatd", cmd);
        return cmd;
    }

    private void addToolArg(JDKToolLauncher launcher, String name, String value) {
        if (useShortCommandSyntax) {
            launcher.addToolArg(name + value);
        } else {
            launcher.addToolArg(name);
            launcher.addToolArg(value);
        }
    }

    private ProcessThread tryToSetupJstatdProcess() throws Throwable {
        portInUse = false;
        ProcessThread jstatdThread = new ProcessThread("Jstatd-Thread",
                JstatdTest::isJstadReady, getJstatdCmd());
        try {
            jstatdThread.start();
            // Make sure jstatd is up and running
            jstatdPid = waitOnTool(jstatdThread);
            if (jstatdPid == null) {
                // The port is already in use. Cancel and try with new one.
                jstatdThread.stopProcess();
                jstatdThread.join();
                return null;
            }
        } catch (Throwable t) {
            // Something went wrong in the product - clean up!
            cleanUpThread(jstatdThread);
            throw t;
        }

        return jstatdThread;
    }

    private static boolean isJstadReady(String line) {
        if (line.contains("Port already in use")) {
            portInUse = true;
            return true;
        }
        return line.startsWith("jstatd started (bound to ");
    }

    public void doTest() throws Throwable {
        runTest(false);
        runTest(true);
    }

    private void runTest(boolean useShortSyntax) throws Throwable {
        useShortCommandSyntax = useShortSyntax;
        if (useDefaultPort) {
            verifyNoRmiRegistryOnDefaultPort();
        }

        ProcessThread jstatdThread = null;
        try {
            while (jstatdThread == null) {
                if (!useDefaultPort) {
                    port = String.valueOf(Utils.getFreePort());
                }

                if (!useDefaultRmiPort) {
                    rmiPort = String.valueOf(Utils.getFreePort());
                }

                if (withExternalRegistry) {
                    Registry registry = startRegistry();
                    if (registry == null) {
                        // The port is already in use. Cancel and try with a new one.
                        continue;
                    }
                }

                jstatdThread = tryToSetupJstatdProcess();
            }

            runToolsAndVerify();
        } finally {
            cleanUpThread(jstatdThread);
        }

        // Verify output from jstatd
        OutputAnalyzer output = jstatdThread.getOutput();
        output.shouldBeEmptyIgnoreVMWarnings();
        assertNotEquals(output.getExitValue(), 0,
                "jstatd process exited with unexpected exit code");
    }

    private void verifyNoRmiRegistryOnDefaultPort() throws Exception {
        try {
            Registry registry = LocateRegistry.getRegistry();
            registry.list();
            throw new Exception("There is already RMI registry on the default port: " + registry);
        } catch (RemoteException e) {
            // No RMI registry on default port is detected
        }
    }

}
