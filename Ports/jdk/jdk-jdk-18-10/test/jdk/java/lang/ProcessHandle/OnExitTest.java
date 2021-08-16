/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jdk.test.lib.Utils;

import org.testng.annotations.Test;
import org.testng.Assert;
import org.testng.TestNG;

/*
 * @test
 * @library /test/lib
 * @modules jdk.management
 * @build jdk.test.lib.Utils
 * @run testng OnExitTest
 * @summary Functions of Process.onExit and ProcessHandle.onExit
 * @author Roger Riggs
 */

public class OnExitTest extends ProcessUtil {

    @SuppressWarnings("raw_types")
    public static void main(String[] args) {
        Class<?>[] testclass = { OnExitTest.class};
        TestNG testng = new TestNG();
        testng.setTestClasses(testclass);
        testng.run();
    }

    /**
     * Basic test of exitValue and onExit.
     */
    @Test
    public static void test1() {
        try {
            int[] exitValues = {0, 1, 10};
            for (int value : exitValues) {
                Process p = JavaChild.spawn("exit", Integer.toString(value));
                CompletableFuture<Process> future = p.onExit();
                future.thenAccept( (ph) -> {
                    int actualExitValue = ph.exitValue();
                    printf(" javaChild done: %s, exitStatus: %d%n",
                            ph, actualExitValue);
                    Assert.assertEquals(actualExitValue, value, "actualExitValue incorrect");
                    Assert.assertEquals(ph, p, "Different Process passed to thenAccept");
                });

                Process h = future.get();
                Assert.assertEquals(h, p);
                Assert.assertEquals(p.exitValue(), value);
                Assert.assertFalse(p.isAlive(), "Process should not be alive");
                p.waitFor();
            }
        } catch (IOException | InterruptedException | ExecutionException ex) {
            Assert.fail(ex.getMessage(), ex);
        } finally {
            destroyProcessTree(ProcessHandle.current());
        }
    }

    /**
     * Test of Completion handler when parent is killed.
     * Spawn 1 child to spawn 3 children each with 2 children.
     */
    @Test
    public static void test2() {
        ProcessHandle procHandle = null;
        try {
            ConcurrentHashMap<ProcessHandle, ProcessHandle> processes = new ConcurrentHashMap<>();
            List<ProcessHandle> children = getChildren(ProcessHandle.current());
            children.forEach(ProcessUtil::printProcess);

            JavaChild proc = JavaChild.spawnJavaChild("stdin");
            procHandle = proc.toHandle();
            printf(" spawned: %d%n", proc.pid());

            proc.forEachOutputLine((s) -> {
                String[] split = s.trim().split(" ");
                if (split.length == 3 && split[1].equals("spawn")) {
                    Long child = Long.valueOf(split[2]);
                    Long parent = Long.valueOf(split[0].split(":")[0]);
                    processes.put(ProcessHandle.of(child).get(), ProcessHandle.of(parent).get());
                }
            });

            proc.sendAction("spawn", "3", "stdin");

            proc.sendAction("child", "spawn", "2", "stdin");

            // Poll until all 9 child processes exist or the timeout is reached
            int expected = 9;
            long timeout = Utils.adjustTimeout(10L);
            Instant endTimeout = Instant.now().plusSeconds(timeout);
            do {
                Thread.sleep(200L);
                printf(" subprocess count: %d, waiting for %d%n", processes.size(), expected);
            } while (processes.size() < expected &&
                    Instant.now().isBefore(endTimeout));

            if (processes.size() < expected) {
                printf("WARNING: not all children have been started. Can't complete test.%n");
                printf("         You can try to increase the timeout or%n");
                printf("         you can try to use a faster VM (i.e. not a debug version).%n");
            }
            children = getDescendants(procHandle);

            ConcurrentHashMap<ProcessHandle, CompletableFuture<ProcessHandle>> completions =
                    new ConcurrentHashMap<>();
            Instant startTime = Instant.now();
            // Create a future for each of the 9 children
            processes.forEach( (p, parent) -> {
                        CompletableFuture<ProcessHandle> cf = p.onExit().whenComplete((ph, ex) -> {
                            Duration elapsed = Duration.between(startTime, Instant.now());
                            printf("whenComplete: pid: %s, exception: %s, thread: %s, elapsed: %s%n",
                                    ph, ex, Thread.currentThread(), elapsed);
                        });
                        completions.put(p, cf);
                    });

            // Check that each of the spawned processes is included in the children
            List<ProcessHandle> remaining = new ArrayList<>(children);
            processes.forEach((p, parent) -> {
                Assert.assertTrue(remaining.remove(p), "spawned process should have been in children");
            });

            // Remove Win32 system spawned conhost.exe processes
            remaining.removeIf(ProcessUtil::isWindowsConsole);

            remaining.forEach(p -> printProcess(p, "unexpected: "));
            if (remaining.size() > 0) {
                // Show full list for debugging
                ProcessUtil.logTaskList();
            }

            proc.destroy();  // kill off the parent
            proc.waitFor();

            // Wait for all the processes and corresponding onExit CF to be completed
            processes.forEach((p, parent) -> {
                try {
                    p.onExit().get();
                    completions.get(p).join();
                } catch (InterruptedException | ExecutionException ex) {
                    // ignore
                }
            });

            // Verify that all 9 exit handlers were called with the correct ProcessHandle
            processes.forEach((p, parent) -> {
                ProcessHandle value = completions.get(p).getNow(null);
                Assert.assertEquals(p, value, "onExit.get value expected: " + p
                        + ", actual: " + value
                        + ": " + p.info());
            });

            // Show the status of the original children
            children.forEach(p -> printProcess(p, "after onExit:"));

            Assert.assertEquals(proc.isAlive(), false, "destroyed process is alive:: %s%n" + proc);
        } catch (IOException | InterruptedException ex) {
            Assert.fail(ex.getMessage());
        } finally {
            if (procHandle != null) {
                destroyProcessTree(procHandle);
            }
        }
    }

    /**
     * Verify that onExit completes for a non-child process only when
     * the process has exited.
     * Spawn a child (A) waiting to be commanded to exit.
     * Spawn a child (B) to wait for that process to exit.
     * Command (A) to exit.
     * Check that (B) does not complete until (A) has exited.
     */
    @Test
    public static void peerOnExitTest() {
        String line = null;
        ArrayBlockingQueue<String> alines = new ArrayBlockingQueue<>(100);
        ArrayBlockingQueue<String> blines = new ArrayBlockingQueue<>(100);
        JavaChild A = null;
        try {
            String[] split;
            A = JavaChild.spawnJavaChild("stdin");
            A.forEachOutputLine(l -> alines.add(l));

            // Verify A is running
            A.sendAction("pid");
            do {
                split = getSplitLine(alines);
            } while (!"pid".equals(split[1]));

            JavaChild B = null;
            try {
                B = JavaChild.spawnJavaChild("stdin");
                B.forEachOutputLine(l -> blines.add(l));

                // Verify B is running
                B.sendAction("pid");
                do {
                    split = getSplitLine(blines);
                } while (!"pid".equals(split[1]));

                // Tell B to wait for A's pid
                B.sendAction("waitpid", A.pid());

                // Wait a bit to see if B will prematurely report the termination of A
                try {
                    line = blines.poll(5L, TimeUnit.SECONDS);
                } catch (InterruptedException ie) {
                    Assert.fail("interrupted", ie);
                }
                Assert.assertNull(line, "waitpid didn't wait");

                A.toHandle().onExit().thenAccept(p -> {
                    System.out.printf(" A.toHandle().onExit().A info: %s, now: %s%n",
                            p.info(), Instant.now());
                });

                A.onExit().thenAccept(p -> {
                    System.out.printf(" A.onExit().A info: %s, now: %s%n",
                            p.info(), Instant.now());
                });

                ProcessHandle.Info A_info = A.info();

                A.sendAction("exit", 0L);

                // Look for B to report that A has exited
                do {
                    Instant start = Instant.now();
                    while (blines.isEmpty() && A.isAlive()) {
                        A_info = A.info(); // Spin
                    }
                    Instant end = Instant.now();
                    System.out.printf(" a.isAlive: %s, a.info: %s, @%s%n", A.isAlive(), A.info(),
                            Duration.between(start, end));

                    split = getSplitLine(blines);
                } while (!"waitpid".equals(split[1]));

                Assert.assertEquals(split[2], "false",  "Process A should not be alive");

                B.sendAction("exit", 0L);
            } catch (IOException ioe) {
                Assert.fail("unable to start JavaChild B", ioe);
            } finally {
                B.destroyForcibly();
            }
        } catch (IOException ioe2) {
            Assert.fail("unable to start JavaChild A", ioe2);
        } finally {
            A.destroyForcibly();
        }
    }

    private static boolean DEBUG = true;

    /**
     * Get a line from the queue and split into words on whitespace.
     * Log to stdout if requested.
     * @param queue a queue of strings
     * @return the words split from the line.
     */
    private static String[] getSplitLine(ArrayBlockingQueue<String> queue) {
        try {
            String line = queue.take();
            String[] split = line.split("\\s");
            if (DEBUG) {
                System.out.printf("  Child Output: %s%n", line);
            }
            return split;
        } catch (InterruptedException ie) {
            Assert.fail("interrupted", ie);
            return null;
        }
    }

}
