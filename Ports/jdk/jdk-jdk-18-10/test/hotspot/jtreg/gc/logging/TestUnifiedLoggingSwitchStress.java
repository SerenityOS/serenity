/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc.logging;

import jdk.test.lib.Utils;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import static gc.testlibrary.Allocation.blackHole;

import java.lang.management.ManagementFactory;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;


/**
 * @test TestUnifiedLoggingSwitchStress
 * @key stress randomness
 * @summary Switches gc log level on fly while stressing memory/gc
 * @requires !vm.flightRecorder
 * @requires vm.gc != "Z"
 * @library /test/lib /
 * @modules java.management java.base/jdk.internal.misc
 *
 * @run main/othervm -Xmx256M -Xms256M
 *                   gc.logging.TestUnifiedLoggingSwitchStress 60
 */

class MemoryStresser implements Runnable {
    public static volatile boolean shouldStop = false;

    private final List<byte[]> liveObjects = new LinkedList<>();
    private final List<byte[]> liveHObjects = new LinkedList<>();
    private int maxSimpleAllocationMemory = 0;
    private int usedMemory = 0;

    /**
     * Maximum amount of huge allocations
     */
    private static int H_ALLOCATION_MAX_COUNT = 4;
    /**
     * Maximum regions in one huge allocation
     */
    private static int H_ALLOCATION_REGION_SIZE = 2;
    private static final int G1_REGION_SIZE = 1024 * 1024;
    /**
     * Maximum size of simple allocation
     */
    private static final int MAX_SIMPLE_ALLOCATION_SIZE = (int) (G1_REGION_SIZE / 2 * 0.9);

    /**
     * Maximum size of dead (i.e. one which is made unreachable right after allocation) object
     */
    private static final int DEAD_OBJECT_MAX_SIZE = G1_REGION_SIZE / 10;
    private final Random rnd = new Random(Utils.getRandomInstance().nextLong());

    /**
     * @param maxMemory maximum memory that could be allocated
     */
    public MemoryStresser(int maxMemory) {
        maxSimpleAllocationMemory = maxMemory - G1_REGION_SIZE * H_ALLOCATION_MAX_COUNT * H_ALLOCATION_REGION_SIZE;
    }

    public final Runnable[] actions = new Runnable[]{
            // Huge allocation
            () -> {
                if (liveHObjects.size() < H_ALLOCATION_MAX_COUNT) {
                    int allocationSize = rnd.nextInt((int) (G1_REGION_SIZE * (H_ALLOCATION_REGION_SIZE - 0.5)
                            * 0.9));
                    liveHObjects.add(new byte[allocationSize + G1_REGION_SIZE / 2]);
                }
            },

            // Huge deallocation
            () -> {
                if (liveHObjects.size() > 0) {
                    int elementNum = rnd.nextInt(liveHObjects.size());
                    liveHObjects.remove(elementNum);
                }
            },

            // Simple allocation
            () -> {
                if (maxSimpleAllocationMemory - usedMemory != 0) {
                    int arraySize = rnd.nextInt(Math.min(maxSimpleAllocationMemory - usedMemory,
                            MAX_SIMPLE_ALLOCATION_SIZE));
                    if (arraySize != 0) {
                        liveObjects.add(new byte[arraySize]);
                        usedMemory += arraySize;
                    }
                }
            },

            // Simple deallocation
            () -> {
                if (liveObjects.size() != 0) {
                    int elementNum = rnd.nextInt(liveObjects.size());
                    int shouldFree = liveObjects.get(elementNum).length;
                    liveObjects.remove(elementNum);
                    usedMemory -= shouldFree;
                }
            },

            // Dead object allocation
            () -> {
                int size = rnd.nextInt(DEAD_OBJECT_MAX_SIZE);
                blackHole(new byte[size]);
            }
    };

    @Override
    public void run() {
        while (!shouldStop) {
            actions[rnd.nextInt(actions.length)].run();
            Thread.yield();
        }

        System.out.println("Memory Stresser finished");
    }
}

class LogLevelSwitcher implements Runnable {

    public static volatile boolean shouldStop = false;
    private final int logCount; // how many various log files will be used
    private final String logFilePrefix; // name of log file will be logFilePrefix + index
    private final Random rnd;
    private final MBeanServer MBS = ManagementFactory.getPlatformMBeanServer();

    /**
     * @param logFilePrefix prefix for log files
     * @param logCount     amount of log files
     */
    public LogLevelSwitcher(String logFilePrefix, int logCount) {
        this.logCount = logCount;
        this.logFilePrefix = logFilePrefix;
        this.rnd = new Random(Utils.getRandomInstance().nextLong());
    }

    private static final String[] LOG_LEVELS = {"error", "warning", "info", "debug", "trace"};

    @Override
    public void run() {

        while (!shouldStop) {
            int fileNum = rnd.nextInt(logCount);
            int logLevel = rnd.nextInt(LOG_LEVELS.length);

            String outputCommand = String.format("output=%s_%d.log", logFilePrefix, fileNum);
            String logLevelCommand = "what='gc*=" + LOG_LEVELS[logLevel] + "'";

            try {
                Object out = MBS.invoke(new ObjectName("com.sun.management:type=DiagnosticCommand"),
                                        "vmLog",
                                        new Object[]{new String[]{outputCommand, logLevelCommand}},
                                        new String[]{String[].class.getName()});

                if (!out.toString().isEmpty()) {
                    System.out.format("WARNING: Diagnostic command vmLog with arguments %s,%s returned not empty"
                                    + " output %s\n",
                            outputCommand, logLevelCommand, out);
                }
            } catch (InstanceNotFoundException | MBeanException | ReflectionException | MalformedObjectNameException e) {
                System.out.println("Got exception trying to change log level:" + e);
                e.printStackTrace();
                throw new Error(e);
            }
            Thread.yield();
        }
        System.out.println("Log Switcher finished");
    }
}


public class TestUnifiedLoggingSwitchStress {
    /**
     * Count of memory stressing threads
     */
    private static final int MEMORY_STRESSERS_COUNT = 3;
    /**
     * Count of log switching threads
     */
    private static final int LOG_LEVEL_SWITCHERS_COUNT = 2;
    /**
     * Count of log files created by each log switching thread
     */
    private static final int LOG_FILES_COUNT = 2;
    /**
     * Maximum amount memory allocated by each stressing thread
     */
    private static final int MAX_MEMORY_PER_STRESSER = (int) (Runtime.getRuntime().freeMemory()
            / MEMORY_STRESSERS_COUNT * 0.7);

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 1) {
            throw new Error("Test Bug: Expected duration (in seconds) wasn't provided as command line argument");
        }
        long duration = Integer.parseInt(args[0]) * 1000;

        long startTime = System.currentTimeMillis();

        List<Thread> threads = new LinkedList<>();

        for (int i = 0; i < LOG_LEVEL_SWITCHERS_COUNT; i++) {
            threads.add(new Thread(new LogLevelSwitcher("Output_" + i, LOG_FILES_COUNT)));
        }

        for (int i = 0; i < MEMORY_STRESSERS_COUNT; i++) {
            threads.add(new Thread(new MemoryStresser(MAX_MEMORY_PER_STRESSER)));
        }

        threads.stream().forEach(Thread::start);

        while (System.currentTimeMillis() - startTime < duration) {
            Thread.yield();
        }

        MemoryStresser.shouldStop = true;
        LogLevelSwitcher.shouldStop = true;
    }
}
