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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.test.lib.Utils;
import org.testng.Assert;
import org.testng.TestNG;
import org.testng.annotations.Test;

/*
 * @test
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          jdk.management
 * @build jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 * @run testng/othervm TreeTest
 * @summary Test counting and JavaChild.spawning and counting of Processes.
 * @author Roger Riggs
 */
public class TreeTest extends ProcessUtil {
    // Main can be used to run the tests from the command line with only testng.jar.
    @SuppressWarnings("raw_types")
    public static void main(String[] args) {
        Class<?>[] testclass = {TreeTest.class};
        TestNG testng = new TestNG();
        testng.setTestClasses(testclass);
        testng.run();
    }

    /**
     * Test counting and spawning and counting of Processes.
     */
    @Test
    public static void test1() {
        final int MAXCHILDREN = 2;
        List<JavaChild> spawned = new ArrayList<>();

        try {
            ProcessHandle self = ProcessHandle.current();

            printf("self pid: %d%n", self.pid());
            printDeep(self, "");

            for (int i = 0; i < MAXCHILDREN; i++) {
                // spawn and wait for instructions
                spawned.add(JavaChild.spawnJavaChild("pid", "stdin"));
            }

            // Verify spawned Process is in list of children
            final List<ProcessHandle> initialChildren = getChildren(self);
            spawned.stream()
                    .map(Process::toHandle)
                    .filter(p -> !initialChildren.contains(p))
                    .forEach(p -> Assert.fail("Spawned process missing from children: " + p));

            // Send exit command to each spawned Process
            spawned.forEach(p -> {
                    try {
                        p.sendAction("exit", "");
                    } catch (IOException ex) {
                        Assert.fail("IOException in sendAction", ex);
                    }
                });

            // Wait for each Process to exit
            spawned.forEach(p -> {
                    do {
                        try {
                            Assert.assertEquals(p.waitFor(), 0, "exit status incorrect");
                            break;
                        } catch (InterruptedException  ex) {
                            continue; // Retry
                        }
                    } while (true);
                });

            // Verify that ProcessHandle.isAlive sees each of them as not alive
            for (Process p : spawned) {
                ProcessHandle ph = p.toHandle();
                Assert.assertFalse(ph.isAlive(),
                        "ProcessHandle.isAlive for exited process: " + ph);
            }

            // Verify spawned processes are not visible as children
            final List<ProcessHandle> afterChildren = getChildren(self);
            spawned.stream()
                    .map(Process::toHandle)
                    .filter(p -> afterChildren.contains(p))
                    .forEach(p -> Assert.fail("Spawned process missing from children: " + p));

        } catch (IOException ioe) {
            Assert.fail("unable to spawn process", ioe);
        } finally {
            // Cleanup any left over processes
            spawned.stream()
                    .map(Process::toHandle)
                    .filter(ProcessHandle::isAlive)
                    .forEach(ph -> {
                        printDeep(ph, "test1 cleanup: ");
                        ph.destroyForcibly();
                    });
        }
    }

    /**
     * Test counting and spawning and counting of Processes.
     */
    @Test
    public static void test2() {
        try {
            ConcurrentHashMap<ProcessHandle, ProcessHandle> processes = new ConcurrentHashMap<>();

            ProcessHandle self = ProcessHandle.current();
            List<ProcessHandle> initialChildren = getChildren(self);
            long count = initialChildren.size();
            if (count > 0) {
                initialChildren.forEach(p -> printDeep(p, "test2 initial unexpected: "));
            }

            JavaChild p1 = JavaChild.spawnJavaChild("stdin");
            ProcessHandle p1Handle = p1.toHandle();
            printf("  p1 pid: %d%n", p1.pid());

            // Gather the PIDs from the output of the spawing process
            p1.forEachOutputLine((s) -> {
                String[] split = s.trim().split(" ");
                if (split.length == 3 && split[1].equals("spawn")) {
                    Long child = Long.valueOf(split[2]);
                    Long parent = Long.valueOf(split[0].split(":")[0]);
                    processes.put(ProcessHandle.of(child).get(), ProcessHandle.of(parent).get());
                }
            });

            int spawnNew = 3;
            p1.sendAction("spawn", spawnNew, "stdin");

            // Wait for direct children to be created and save the list
            List<ProcessHandle> subprocesses = waitForAllChildren(p1Handle, spawnNew);
            Optional<Instant> p1Start = p1Handle.info().startInstant();
            for (ProcessHandle ph : subprocesses) {
                Assert.assertTrue(ph.isAlive(), "Child should be alive: " + ph);
                // Verify each child was started after the parent
                ph.info().startInstant()
                        .ifPresent(childStart -> p1Start.ifPresent(parentStart -> {
                            Assert.assertFalse(childStart.isBefore(parentStart),
                                    String.format("Child process started before parent: child: %s, parent: %s",
                                            childStart, parentStart));
                        }));
            }

            // Each child spawns two processes and waits for commands
            int spawnNewSub = 2;
            p1.sendAction("child", "spawn", spawnNewSub, "stdin");

            // Poll until all 9 child processes exist or the timeout is reached
            int expected = 9;
            long timeout = Utils.adjustTimeout(60L);
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

            // show the complete list of children (for debug)
            List<ProcessHandle> descendants = getDescendants(p1Handle);
            printf(" descendants:  %s%n",
                    descendants.stream().map(p -> p.pid())
                           .collect(Collectors.toList()));

            // Verify that all spawned children show up in the descendants  List
            processes.forEach((p, parent) -> {
                Assert.assertEquals(p.isAlive(), true, "Child should be alive: " + p);
                Assert.assertTrue(descendants.contains(p), "Spawned child should be listed in descendants: " + p);
            });

            // Closing JavaChild's InputStream will cause all children to exit
            p1.getOutputStream().close();

            for (ProcessHandle p : descendants) {
                try {
                    p.onExit().get();       // wait for the child to exit
                } catch (ExecutionException e) {
                    Assert.fail("waiting for process to exit", e);
                }
            }
            p1.waitFor();           // wait for spawned process to exit

            // Verify spawned processes are no longer alive
            processes.forEach((ph, parent) -> Assert.assertFalse(ph.isAlive(),
                            "process should not be alive: " + ph));
        } catch (IOException | InterruptedException t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    /**
     * Test destroy of processes.
     * A JavaChild is started and it starts three children.
     * Each one is then checked to be alive and listed by descendants
     * and forcibly destroyed.
     * After they exit they should no longer be listed by descendants.
     */
    @Test
    public static void test3() {
        ConcurrentHashMap<ProcessHandle, ProcessHandle> processes = new ConcurrentHashMap<>();

        try {
            ProcessHandle self = ProcessHandle.current();

            JavaChild p1 = JavaChild.spawnJavaChild("stdin");
            ProcessHandle p1Handle = p1.toHandle();
            printf(" p1: %s%n", p1.pid());

            int newChildren = 3;
            CountDownLatch spawnCount = new CountDownLatch(newChildren);
            // Spawn children and have them wait
            p1.sendAction("spawn", newChildren, "stdin");

            // Gather the PIDs from the output of the spawning process
            p1.forEachOutputLine((s) -> {
                String[] split = s.trim().split(" ");
                if (split.length == 3 && split[1].equals("spawn")) {
                    Long child = Long.valueOf(split[2]);
                    Long parent = Long.valueOf(split[0].split(":")[0]);
                    processes.put(ProcessHandle.of(child).get(), ProcessHandle.of(parent).get());
                    spawnCount.countDown();
                }
            });

            // Wait for all the subprocesses to be listed as started
            Assert.assertTrue(spawnCount.await(Utils.adjustTimeout(30L), TimeUnit.SECONDS),
                    "Timeout waiting for processes to start");

            // Debugging; list descendants that are not expected in processes
            List<ProcessHandle> descendants = ProcessUtil.getDescendants(p1Handle);
            long count = descendants.stream()
                    .filter(ph -> !processes.containsKey(ph))
                    .count();
            if (count > 0) {
                descendants.stream()
                    .filter(ph -> !processes.containsKey(ph))
                    .forEach(ph1 -> ProcessUtil.printProcess(ph1, "Extra process: "));
                ProcessUtil.logTaskList();
                Assert.assertEquals(0, count, "Extra processes in descendants");
            }

            // Verify that all spawned children are alive, show up in the descendants list
            // then destroy them
            processes.forEach((p, parent) -> {
                Assert.assertEquals(p.isAlive(), true, "Child should be alive: " + p);
                Assert.assertTrue(descendants.contains(p), "Spawned child should be listed in descendants: " + p);
                p.destroyForcibly();
            });
            Assert.assertEquals(processes.size(), newChildren, "Wrong number of children");

            // Wait for each of the processes to exit
            processes.forEach((p, parent) ->  {
                for (long retries = Utils.adjustTimeout(100L); retries > 0 ; retries--) {
                    if (!p.isAlive()) {
                        return;                 // not alive, go on to the next
                    }
                    // Wait a bit and retry
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException ie) {
                        // try again
                    }
                }
                printf("Timeout waiting for exit of pid %s, parent: %s, info: %s%n",
                        p, parent, p.info());
                Assert.fail("Process still alive: " + p);
            });
            p1.destroyForcibly();
            p1.waitFor();

            // Verify that none of the spawned children are still listed by descendants
            List<ProcessHandle> remaining = getDescendants(self);
            Assert.assertFalse(remaining.remove(p1Handle), "Child p1 should have exited");
            remaining = remaining.stream().filter(processes::containsKey).collect(Collectors.toList());
            Assert.assertEquals(remaining.size(), 0, "Subprocess(es) should have exited: " + remaining);

        } catch (IOException ioe) {
            Assert.fail("Spawn of subprocess failed", ioe);
        } catch (InterruptedException inte) {
            Assert.fail("InterruptedException", inte);
        } finally {
            processes.forEach((p, parent) -> {
                if (p.isAlive()) {
                    ProcessUtil.printProcess(p);
                    p.destroyForcibly();
                }
            });
        }
    }

    /**
     * Test (Not really a test) that dumps the list of all Processes.
     */
    @Test
    public static void test4() {
        printf("    Parent     Child  Info%n");
        Stream<ProcessHandle> s = ProcessHandle.allProcesses();
        ProcessHandle[] processes = s.toArray(ProcessHandle[]::new);
        int len = processes.length;
        ProcessHandle[] parent = new ProcessHandle[len];
        Set<ProcessHandle> processesSet =
                Arrays.stream(processes).collect(Collectors.toSet());
        Integer[] sortindex = new Integer[len];
        for (int i = 0; i < len; i++) {
            sortindex[i] = i;
         }
        for (int i = 0; i < len; i++) {
            parent[sortindex[i]] = processes[sortindex[i]].parent().orElse(null);
        }
        Arrays.sort(sortindex, (i1, i2) -> {
            int cmp = Long.compare((parent[i1] == null ? 0L : parent[i1].pid()),
                    (parent[i2] == null ? 0L : parent[i2].pid()));
            if (cmp == 0) {
                cmp = Long.compare((processes[i1] == null ? 0L : processes[i1].pid()),
                        (processes[i2] == null ? 0L : processes[i2].pid()));
            }
            return cmp;
        });
        boolean fail = false;
        for (int i = 0; i < len; i++) {
            ProcessHandle p = processes[sortindex[i]];
            ProcessHandle p_parent = parent[sortindex[i]];
            ProcessHandle.Info info = p.info();
            String indent = "    ";
            if (p_parent != null) {
                if (!processesSet.contains(p_parent)) {
                    fail = true;
                    indent = "*** ";
                }
            }
            printf("%s %7s, %7s, %s%n", indent, p_parent, p, info);
        }
        Assert.assertFalse(fail, "Parents missing from all Processes");

    }

    /**
     * A test for scale; launch a large number (14) of subprocesses.
     */
    @Test
    public static void test5() {
        ConcurrentHashMap<ProcessHandle, ProcessHandle> processes = new ConcurrentHashMap<>();

        int factor = 2;
        JavaChild p1 = null;
        Instant start = Instant.now();
        try {
            p1 = JavaChild.spawnJavaChild("stdin");
            ProcessHandle p1Handle = p1.toHandle();

            printf("Spawning %d x %d x %d processes, pid: %d%n",
                    factor, factor, factor, p1.pid());

            // Start the first tier of subprocesses
            p1.sendAction("spawn", factor, "stdin");

            // Start the second tier of subprocesses
            p1.sendAction("child", "spawn", factor, "stdin");

            // Start the third tier of subprocesses
            p1.sendAction("child", "child", "spawn", factor, "stdin");

            int newChildren = factor * (1 + factor * (1 + factor));
            CountDownLatch spawnCount = new CountDownLatch(newChildren);

            // Gather the PIDs from the output of the spawning process
            p1.forEachOutputLine((s) -> {
                String[] split = s.trim().split(" ");
                if (split.length == 3 && split[1].equals("spawn")) {
                    Long child = Long.valueOf(split[2]);
                    Long parent = Long.valueOf(split[0].split(":")[0]);
                    processes.put(ProcessHandle.of(child).get(), ProcessHandle.of(parent).get());
                    spawnCount.countDown();
                }
            });

            // Wait for all the subprocesses to be listed as started
            Assert.assertTrue(spawnCount.await(Utils.adjustTimeout(30L), TimeUnit.SECONDS),
                    "Timeout waiting for processes to start");

            // Debugging; list descendants that are not expected in processes
            List<ProcessHandle> descendants = ProcessUtil.getDescendants(p1Handle);
            long count = descendants.stream()
                    .filter(ph -> !processes.containsKey(ph))
                    .count();
            if (count > 0) {
                descendants.stream()
                    .filter(ph -> !processes.containsKey(ph))
                    .forEach(ph1 -> ProcessUtil.printProcess(ph1, "Extra process: "));
                ProcessUtil.logTaskList();
                Assert.assertEquals(0, count, "Extra processes in descendants");
            }

            Assert.assertEquals(getChildren(p1Handle).size(),
                    factor, "expected direct children");
            count = getDescendants(p1Handle).size();
            long totalChildren = factor * factor * factor + factor * factor + factor;
            Assert.assertTrue(count >= totalChildren,
                    "expected at least " + totalChildren + ", actual: " + count);

            List<ProcessHandle> subprocesses = getDescendants(p1Handle);
            printf(" descendants:  %s%n",
                    subprocesses.stream().map(p -> p.pid())
                    .collect(Collectors.toList()));

            p1.getOutputStream().close();  // Close stdin for the controlling p1
            p1.waitFor();
        } catch (InterruptedException | IOException ex) {
            Assert.fail("Unexpected Exception", ex);
        } finally {
            printf("Duration: %s%n", Duration.between(start, Instant.now()));
            if (p1 != null) {
                p1.destroyForcibly();
            }
            processes.forEach((p, parent) -> {
                if (p.isAlive()) {
                    ProcessUtil.printProcess(p, "Process Cleanup: ");
                    p.destroyForcibly();
                }
            });
        }
    }

}
