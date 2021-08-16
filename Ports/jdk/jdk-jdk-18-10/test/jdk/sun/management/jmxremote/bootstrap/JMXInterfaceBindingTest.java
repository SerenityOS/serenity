/*
 * Copyright (c) 2015, Red Hat Inc
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
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/**
 * @test
 * @bug     6425769
 * @summary Test JMX agent host address binding. Same ports but different
 *          interfaces to bind to (selecting plain or SSL sockets at random)
 *
 * @library /test/lib
 * @modules java.management.rmi
 *
 * @build JMXAgentInterfaceBinding
 * @run main/timeout=60 JMXInterfaceBindingTest
 */
public class JMXInterfaceBindingTest {

    public static final int COMMUNICATION_ERROR_EXIT_VAL = 1;
    public static final int STOP_PROCESS_EXIT_VAL = 10;
    public static final int JMX_PORT_RANGE_LOWER = 9100;
    public static final int JMX_PORT_RANGE_UPPER = 9200;
    public static final int JMX_PORT_RANGE_LOWER_SSL = 9201; // 9200 might be RMI Port
    public static final int JMX_PORT_RANGE_UPPER_SSL = 9300;
    private static final int MAX_RETRY_ATTEMTS = 10;
    public static final String READY_MSG = "MainThread: Ready for connections";
    public static final String TEST_CLASS = JMXAgentInterfaceBinding.class.getSimpleName();
    public static final String KEYSTORE_LOC = System.getProperty("test.src", ".") +
                                              File.separator +
                                              "ssl" +
                                              File.separator +
                                              "keystore";
    public static final String TRUSTSTORE_LOC = System.getProperty("test.src", ".") +
                                                File.separator +
                                                "ssl" +
                                                File.separator +
                                                "truststore";
    public static final String TEST_CLASSPATH = System.getProperty("test.classes", ".");

    public void run(List<InetAddress> addrs) {
        System.out.println("DEBUG: Running tests with plain sockets.");
        runTests(addrs, false);
        System.out.println("DEBUG: Running tests with SSL sockets.");
        runTests(addrs, true);
    }

    private void runTests(List<InetAddress> addrs, boolean useSSL) {
        List<TestProcessThread> testThreads = new ArrayList<>(addrs.size());
        CountDownLatch latch = new CountDownLatch(addrs.size());
        for (InetAddress addr : addrs) {
            String address = JMXAgentInterfaceBinding.wrapAddress(addr.getHostAddress());
            TestProcessThread t = new TestProcessThread(address, useSSL, latch);
            testThreads.add(t);
            t.start();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            System.err.println("Failed to wait for the test threads to complete");
            throw new RuntimeException("Test failed", e);
        }

        long failedProcesses = testThreads.stream().filter(TestProcessThread::isTestFailed).count();
        if (failedProcesses > 0) {
            throw new RuntimeException("Test FAILED. " + failedProcesses + " out of " + addrs.size() +
                    " process(es) failed to start the JMX agent.");
        }
    }

    private static int getRandomPortInRange(int lower, int upper) {
        if (upper <= lower) {
            throw new IllegalArgumentException("upper <= lower");
        }
        int range = upper - lower;
        int randPort = lower + (int)(Math.random() * range);
        return randPort;
    }

    public static void main(String[] args) {
        List<InetAddress> addrs = getNonLoopbackAddressesForLocalHost();
        if (addrs.isEmpty()) {
            System.out.println("Ignoring test since no non-loopback IPs are available to bind to " +
                               "in addition to the loopback interface.");
            return;
        }
        JMXInterfaceBindingTest test = new JMXInterfaceBindingTest();
        // Add loopback interface too as we'd like to verify whether it's
        // possible to bind to multiple addresses on the same host. This
        // wasn't possible prior JDK-6425769. It used to bind to *all* local
        // interfaces. We add loopback here, since that eases test setup.
        addrs.add(InetAddress.getLoopbackAddress());
        test.run(addrs);
        System.out.println("All tests PASSED.");
    }

    private static List<InetAddress> getNonLoopbackAddressesForLocalHost() {
        List<InetAddress> addrs = new ArrayList<>();
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            if (!localHost.isLoopbackAddress()) {
                addrs.add(localHost);
            }
            return addrs;
        } catch (UnknownHostException e) {
            throw new RuntimeException("Test failed", e);
        }
    }

    private static class TestProcessThread extends Thread {
        private final String name;
        private final String address;
        private final boolean useSSL;
        private final CountDownLatch latch;
        private volatile boolean testFailed = false;
        private OutputAnalyzer output;

        public TestProcessThread(String address, boolean useSSL, CountDownLatch latch) {
            this.address = address;
            this.useSSL = useSSL;
            this.name = "JMX-Tester-" + address;
            this.latch = latch;
        }

        @Override
        public void run() {
            int attempts = 0;
            boolean needRetry = false;
            do {
                if (needRetry) {
                    System.err.println("Retrying the test for " + name);
                }
                needRetry = runTest();
            } while (needRetry && (attempts++ < MAX_RETRY_ATTEMTS));

            if (testFailed) {
                int exitValue = output.getExitValue();
                if (needRetry) {
                    System.err.println("Test FAILURE on " + name + " reason: run out of retries to to pick free ports");
                } else if (exitValue == COMMUNICATION_ERROR_EXIT_VAL) {
                    // Failure case since the java processes should still be
                    // running.
                    System.err.println("Test FAILURE on " + name);
                } else if (exitValue == STOP_PROCESS_EXIT_VAL) {
                    System.out.println("Test FAILURE on " + name + " reason: The expected line \"" + READY_MSG
                            + "\" is not present in the process output");
                } else {
                    System.err.println("Test FAILURE on " + name + " reason: Unexpected exit code => " + exitValue);
                }
                output.reportDiagnosticSummary();
            }
            latch.countDown();
        }

        public boolean isTestFailed() {
            return testFailed;
        }

        private int getJMXPort() {
            return useSSL ?
                    getRandomPortInRange(JMX_PORT_RANGE_LOWER_SSL, JMX_PORT_RANGE_UPPER_SSL) :
                    getRandomPortInRange(JMX_PORT_RANGE_LOWER, JMX_PORT_RANGE_UPPER);
        }

        private Process createTestProcess() {
            int jmxPort = getJMXPort();
            int rmiPort = jmxPort + 1;
            String msg = String.format("DEBUG: Launching java tester for triplet (HOSTNAME,JMX_PORT,RMI_PORT)" +
                            " == (%s,%d,%d)", address, jmxPort, rmiPort);
            System.out.println(msg);
            List<String> args = new ArrayList<>();
            args.add("-classpath");
            args.add(TEST_CLASSPATH);
            args.add("-Dcom.sun.management.jmxremote.host=" + address);
            args.add("-Dcom.sun.management.jmxremote.port=" + jmxPort);
            args.add("-Dcom.sun.management.jmxremote.rmi.port=" + rmiPort);
            args.add("-Dcom.sun.management.jmxremote.authenticate=false");
            args.add("-Dcom.sun.management.jmxremote.ssl=" + Boolean.toString(useSSL));
            // This is needed for testing on loopback
            args.add("-Djava.rmi.server.hostname=" + address);
            if (useSSL) {
                args.add("-Dcom.sun.management.jmxremote.registry.ssl=true");
                args.add("-Djavax.net.ssl.keyStore=" + KEYSTORE_LOC);
                args.add("-Djavax.net.ssl.trustStore=" + TRUSTSTORE_LOC);
                args.add("-Djavax.net.ssl.keyStorePassword=password");
                args.add("-Djavax.net.ssl.trustStorePassword=trustword");
            }
            args.add(TEST_CLASS);
            args.add(address);
            args.add(Integer.toString(jmxPort));
            args.add(Integer.toString(rmiPort));
            args.add(Boolean.toString(useSSL));

            try {
                ProcessBuilder builder = ProcessTools.createJavaProcessBuilder(args.toArray(new String[]{}));
                System.out.println(ProcessTools.getCommandLine(builder));
                Process process = builder.start();
                output = new OutputAnalyzer(process);
                return process;
            } catch (Exception e) {
                throw new RuntimeException("Test failed", e);
            }
        }

        // Returns true if the test failed due to "Port already in use" error.
        private boolean runTest() {
            testFailed = true;
            Process process = createTestProcess();
            try {
                sendMessageToProcess(process, "Exit: " + STOP_PROCESS_EXIT_VAL);
                process.waitFor();
            } catch (Throwable e) {
                System.err.println("Failed to stop process: " + name);
                throw new RuntimeException("Test failed", e);
            }
            if (output.getExitValue() == STOP_PROCESS_EXIT_VAL && output.getStdout().contains(READY_MSG)) {
                testFailed = false;
            } else if (output.getStderr().contains("Port already in use")) {
                System.out.println("The test attempt for the test " + name +" failed due to the bind error");
                // Need to retry
                return true;
            }
            return false;
        }

        private static void sendMessageToProcess(Process process, String message) {
            try (PrintWriter pw = new PrintWriter(process.getOutputStream())) {
                pw.println(message);
                pw.flush();
            }
        }
    }
}
