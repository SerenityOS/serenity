/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserPrincipal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import jdk.test.lib.Platform;
import jdk.test.lib.Utils;
import org.testng.Assert;
import org.testng.TestNG;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8077350 8081566 8081567 8098852 8136597
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          jdk.management
 * @build jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 * @run testng InfoTest
 * @summary Functions of ProcessHandle.Info
 * @author Roger Riggs
 */

public class InfoTest {

    static String whoami;

    static {
        try {
            // Create a file and take the username from the file
            Path p = Paths.get("OwnerName.tmp");
            Files.createFile(p);
            UserPrincipal owner = Files.getOwner(p);
            whoami = owner.getName();
            Files.delete(p);
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new UncheckedIOException("tmp file", ex);
        }
    }

    // Main can be used to run the tests from the command line with only testng.jar.
    @SuppressWarnings("raw_types")
    public static void main(String[] args) {
        Class<?>[] testclass = {InfoTest.class};
        TestNG testng = new TestNG();
        testng.setTestClasses(testclass);
        testng.run();
    }

    /**
     * Test that cputime used shows up in ProcessHandle.info
     */
    @Test
    public static void test1() {
        System.out.println("Note: when run in samevm mode the cputime of the " +
                "test runner is included.");
        ProcessHandle self = ProcessHandle.current();

        Duration someCPU = Duration.ofMillis(200L);
        Instant end = Instant.now().plus(someCPU);
        while (Instant.now().isBefore(end)) {
            // waste the cpu
        }
        ProcessHandle.Info info = self.info();
        System.out.printf(" info: %s%n", info);
        Optional<Duration> totalCpu = info.totalCpuDuration();
        if (totalCpu.isPresent() && (totalCpu.get().compareTo(someCPU) < 0)) {
            Assert.fail("reported cputime less than expected: " + someCPU + ", " +
                    "actual: " + info.totalCpuDuration());
        }
    }

    /**
     * Spawn a child with arguments and check they are visible via the ProcessHandle.
     */
    @Test
    public static void test2() {
        try {
            long cpuLoopTime = 100;             // 100 ms
            String[] extraArgs = {"pid", "parent", "stdin"};
            JavaChild p1 = JavaChild.spawnJavaChild((Object[])extraArgs);
            Instant afterStart = null;

            try (BufferedReader lines = p1.outputReader()) {
                // Read the args line to know the subprocess has started
                lines.readLine();
                afterStart = Instant.now();

                Duration lastCpu = Duration.ofMillis(0L);
                for (int j = 0; j < 10; j++) {

                    p1.sendAction("cpuloop", cpuLoopTime);
                    p1.sendAction("cputime", "");

                    // Read cputime from child
                    Duration childCpuTime = null;
                    // Read lines from the child until the result from cputime is returned
                    for (String s; (s = lines.readLine()) != null;) {
                        String[] split = s.trim().split(" ");
                        if (split.length == 3 && split[1].equals("cputime")) {
                            long nanos = Long.valueOf(split[2]);
                            childCpuTime = Duration.ofNanos(nanos);
                            break;      // found the result we're looking for
                        }
                    }

                    if (Platform.isAix()) {
                        // Unfortunately, on AIX the usr/sys times reported through
                        // /proc/<pid>/psinfo which are used by ProcessHandle.Info
                        // are running slow compared to the corresponding times reported
                        // by the times()/getrusage() system calls which are used by
                        // OperatingSystemMXBean.getProcessCpuTime() and returned by
                        // the JavaChild for the "cputime" command.
                        // This is because /proc/<pid>/status is only updated once a second.
                        // So we better wait a little bit to get plausible values here.
                        Thread.sleep(1000);
                    }
                    ProcessHandle.Info info = p1.info();
                    System.out.printf(" info: %s%n", info);

                    if (info.user().isPresent()) {
                        String user = info.user().get();
                        Assert.assertNotNull(user, "User name");
                        Assert.assertEquals(user, whoami, "User name");
                    }

                    Optional<String> command = info.command();
                    if (command.isPresent()) {
                        String javaExe = System.getProperty("test.jdk") +
                                File.separator + "bin" + File.separator + "java";
                        String expected = Platform.isWindows() ? javaExe + ".exe" : javaExe;
                        Path expectedPath = Paths.get(expected);
                        Path actualPath = Paths.get(command.get());
                        Assert.assertTrue(Files.isSameFile(expectedPath, actualPath),
                                "Command: expected: " + javaExe + ", actual: " + command.get());
                    }

                    if (info.arguments().isPresent()) {
                        String[] args = info.arguments().get();

                        int offset = args.length - extraArgs.length;
                        for (int i = 0; i < extraArgs.length; i++) {
                            Assert.assertEquals(args[offset + i], extraArgs[i],
                                                "Actual argument mismatch, index: " + i);
                        }

                        // Now check that the first argument is not the same as the executed command
                        if (args.length > 0) {
                            Assert.assertNotEquals(args[0], command,
                                    "First argument should not be the executable: args[0]: "
                                            + args[0] + ", command: " + command);
                        }
                    }

                    if (command.isPresent() && info.arguments().isPresent()) {
                        // If both, 'command' and 'arguments' are present,
                        // 'commandLine' is just the concatenation of the two.
                        Assert.assertTrue(info.commandLine().isPresent(),
                                          "commandLine() must be available");

                        String javaExe = System.getProperty("test.jdk") +
                                File.separator + "bin" + File.separator + "java";
                        String expected = Platform.isWindows() ? javaExe + ".exe" : javaExe;
                        Path expectedPath = Paths.get(expected);
                        String commandLine = info.commandLine().get();
                        String commandLineCmd = commandLine.split(" ")[0];
                        Path commandLineCmdPath = Paths.get(commandLineCmd);
                        Assert.assertTrue(Files.isSameFile(commandLineCmdPath, expectedPath),
                                          "commandLine() should start with: " + expectedPath +
                                          " but starts with " + commandLineCmdPath);

                        Assert.assertTrue(commandLine.contains(command.get()),
                                "commandLine() must contain the command: " + command.get());
                        List<String> allArgs = p1.getArgs();
                        for (int i = 1; i < allArgs.size(); i++) {
                            Assert.assertTrue(commandLine.contains(allArgs.get(i)),
                                              "commandLine() must contain argument: " + allArgs.get(i));
                        }
                    } else if (info.commandLine().isPresent() &&
                            command.isPresent() &&
                            command.get().length() < info.commandLine().get().length()) {
                        // If we only have the commandLine() we can only do some basic checks...
                        String commandLine = info.commandLine().get();
                        String javaExe = "java" + (Platform.isWindows() ? ".exe" : "");
                        int pos = commandLine.indexOf(javaExe);
                        Assert.assertTrue(pos > 0, "commandLine() should at least contain 'java'");

                        pos += javaExe.length() + 1; // +1 for the space after the command
                        List<String> allArgs = p1.getArgs();
                        // First argument is the command - skip it here as we've already checked that.
                        for (int i = 1; (i < allArgs.size()) &&
                                        (pos + allArgs.get(i).length() < commandLine.length()); i++) {
                            Assert.assertTrue(commandLine.contains(allArgs.get(i)),
                                              "commandLine() must contain argument: " + allArgs.get(i));
                            pos += allArgs.get(i).length() + 1;
                        }
                    }

                    if (info.totalCpuDuration().isPresent()) {
                        Duration totalCPU = info.totalCpuDuration().get();
                        Duration epsilon = Duration.ofMillis(200L);
                        if (childCpuTime != null) {
                            System.out.printf(" info.totalCPU: %s, childCpuTime: %s, diff: %s%n",
                                    totalCPU.toNanos(), childCpuTime.toNanos(),
                                    childCpuTime.toNanos() - totalCPU.toNanos());
                            Assert.assertTrue(checkEpsilon(childCpuTime, totalCPU, epsilon),
                                    childCpuTime + " should be within " +
                                            epsilon + " of " + totalCPU);
                        }
                        Assert.assertTrue(totalCPU.toNanos() > 0L,
                                "total cpu time expected > 0ms, actual: " + totalCPU);
                        long t = Utils.adjustTimeout(10L);  // Adjusted timeout seconds
                        Assert.assertTrue(totalCPU.toNanos() < lastCpu.toNanos() + t * 1_000_000_000L,
                                "total cpu time expected < " + t
                                        + " seconds more than previous iteration, actual: "
                                        + (totalCPU.toNanos() - lastCpu.toNanos()));
                        lastCpu = totalCPU;
                    }

                    if (info.startInstant().isPresent()) {
                        Instant startTime = info.startInstant().get();
                        Assert.assertTrue(startTime.isBefore(afterStart),
                                "startTime after process spawn completed"
                                        + startTime + " + > " + afterStart);
                    }
                }
            }
            p1.sendAction("exit");
            Assert.assertTrue(p1.waitFor(Utils.adjustTimeout(30L), TimeUnit.SECONDS),
                    "timeout waiting for process to terminate");
        } catch (IOException | InterruptedException ie) {
            ie.printStackTrace(System.out);
            Assert.fail("unexpected exception", ie);
        } finally {
            // Destroy any children that still exist
            ProcessUtil.destroyProcessTree(ProcessHandle.current());
        }
    }

    /**
     * Spawn a child with arguments and check they are visible via the ProcessHandle.
     */
    @Test
    public static void test3() {
        try {
            for (long sleepTime : Arrays.asList(Utils.adjustTimeout(30), Utils.adjustTimeout(32))) {
                Process p = spawn("sleep", String.valueOf(sleepTime));

                ProcessHandle.Info info = p.info();
                System.out.printf(" info: %s%n", info);

                if (info.user().isPresent()) {
                    String user = info.user().get();
                    Assert.assertNotNull(user);
                    Assert.assertEquals(user, whoami);
                }
                if (info.command().isPresent()) {
                    String command = info.command().get();
                    String expected = "sleep";
                    if (Platform.isWindows()) {
                        expected = "sleep.exe";
                    } else if (Platform.isBusybox("/bin/sleep")) {
                        // With busybox sleep is just a sym link to busybox.
                        // The busbox executable is seen as ProcessHandle.Info command.
                        expected = "busybox";
                    }
                    Assert.assertTrue(command.endsWith(expected), "Command: expected: \'" +
                            expected + "\', actual: " + command);

                    // Verify the command exists and is executable
                    File exe = new File(command);
                    Assert.assertTrue(exe.exists(), "command must exist: " + exe);
                    Assert.assertTrue(exe.canExecute(), "command must be executable: " + exe);
                }
                if (info.arguments().isPresent()) {
                    String[] args = info.arguments().get();
                    if (args.length > 0) {
                        Assert.assertEquals(args[0], String.valueOf(sleepTime));
                    }
                }
                p.destroy();
                Assert.assertTrue(p.waitFor(Utils.adjustTimeout(30), TimeUnit.SECONDS),
                        "timeout waiting for process to terminate");
            }
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace(System.out);
        } finally {
            // Destroy any children that still exist
            ProcessUtil.destroyProcessTree(ProcessHandle.current());
        }
    }

    /**
     * Cross check the cputime reported from java.management with that for the current process.
     */
    @Test
    public static void test4() {
        Duration myCputime1 = ProcessUtil.MXBeanCpuTime();

        if (Platform.isAix()) {
            // Unfortunately, on AIX the usr/sys times reported through
            // /proc/<pid>/psinfo which are used by ProcessHandle.Info
            // are running slow compared to the corresponding times reported
            // by the times()/getrusage() system calls which are used by
            // OperatingSystemMXBean.getProcessCpuTime() and returned by
            // the JavaChild for the "cputime" command.
            // So we better wait a little bit to get plausible values here.
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {}
        }
        Optional<Duration> dur1 = ProcessHandle.current().info().totalCpuDuration();

        Duration myCputime2 = ProcessUtil.MXBeanCpuTime();

        if (Platform.isAix()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {}
        }
        Optional<Duration> dur2 = ProcessHandle.current().info().totalCpuDuration();

        if (dur1.isPresent() && dur2.isPresent()) {
            Duration total1 = dur1.get();
            Duration total2 = dur2.get();
            System.out.printf(" total1 vs. mbean: %s, getProcessCpuTime: %s, diff: %s%n",
                    Objects.toString(total1), myCputime1, myCputime1.minus(total1));
            System.out.printf(" total2 vs. mbean: %s, getProcessCpuTime: %s, diff: %s%n",
                    Objects.toString(total2), myCputime2, myCputime2.minus(total2));

            Duration epsilon = Duration.ofMillis(200L);      // Epsilon is 200ms.
            Assert.assertTrue(checkEpsilon(myCputime1, myCputime2, epsilon),
                    myCputime1.toNanos() + " should be within " + epsilon
                            + " of " + myCputime2.toNanos());
            Assert.assertTrue(checkEpsilon(total1, total2, epsilon),
                    total1.toNanos() + " should be within " + epsilon
                            + " of " + total2.toNanos());
            Assert.assertTrue(checkEpsilon(myCputime1, total1, epsilon),
                    myCputime1.toNanos() + " should be within " + epsilon
                            + " of " + total1.toNanos());
            Assert.assertTrue(checkEpsilon(total1, myCputime2, epsilon),
                    total1.toNanos() + " should be within " + epsilon
                            + " of " + myCputime2.toNanos());
            Assert.assertTrue(checkEpsilon(myCputime2, total2, epsilon),
                    myCputime2.toNanos() + " should be within " + epsilon
                            + " of " + total2.toNanos());
        }
    }

    @Test
    public static void test5() {
        ProcessHandle self = ProcessHandle.current();
        Random r = new Random();
        for (int i = 0; i < 30; i++) {
            Instant end = Instant.now().plusMillis(500L);
            while (end.isBefore(Instant.now())) {
                // burn the cpu time checking the time
                long x = r.nextLong();
            }
            if (self.info().totalCpuDuration().isPresent()) {
                Duration totalCpu = self.info().totalCpuDuration().get();
                long infoTotalCputime = totalCpu.toNanos();
                long beanCputime = ProcessUtil.MXBeanCpuTime().toNanos();
                System.out.printf(" infoTotal: %12d, beanCpu: %12d, diff: %12d%n",
                        infoTotalCputime, beanCputime, beanCputime - infoTotalCputime);
            } else {
                break;  // nothing to compare; continue
            }
        }
    }
    /**
     * Check two Durations, the second should be greater than the first or
     * within the supplied Epsilon.
     * @param d1 a Duration - presumed to be shorter
     * @param d2 a 2nd Duration - presumed to be greater (or within Epsilon)
     * @param epsilon Epsilon the amount of overlap allowed
     * @return true if d2 is greater than d1 or within epsilon, false otherwise
     */
    static boolean checkEpsilon(Duration d1, Duration d2, Duration epsilon) {
        if (d1.toNanos() <= d2.toNanos()) {
            return true;
        }
        Duration diff = d1.minus(d2).abs();
        return diff.compareTo(epsilon) <= 0;
    }

    /**
     * Spawn a native process with the provided arguments.
     * @param command the executable of native process
     * @param args to start a new process
     * @return the Process that was started
     * @throws IOException thrown by ProcessBuilder.start
     */
    static Process spawn(String command, String... args) throws IOException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.inheritIO();
        List<String> list = new ArrayList<>();
        list.add(command);
        for (String arg : args)
            list.add(arg);
        pb.command(list);
        return pb.start();
    }
}
