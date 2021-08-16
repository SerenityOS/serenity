/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @summary Tests that the breakpoint in the notification listener is hit when the
 * notification thread is enabled and is not hit when the notification thread is disabled
 * (the service thread delivers the notifications in this case).
 *
 * @library /test/lib
 * @run compile -g JdbStopInNotificationThreadTest.java
 * @run main/othervm JdbStopInNotificationThreadTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import java.lang.management.*;
import java.util.Collection;
import java.util.LinkedList;

class JdbStopInNotificationThreadTestTarg {

    private static volatile boolean done = false;

    private static final MemoryPoolMXBean tenuredGenPool =
            findTenuredGenPool();

    public static void main(String[] args) throws Exception {
        test(); // @1 breakpoint
    }

    private static void test() throws Exception {
        setPercentageUsageThreshold(0.1);
        MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
        NotificationEmitter emitter = (NotificationEmitter) mbean;
        emitter.addNotificationListener(new NotificationListener() {
            public void handleNotification(Notification n, Object hb) {
                System.out.println("Notification received:" + n.getType());
                if (n.getType().equals(
                        MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED)) {
                    done = true;
                    System.out.println("Notification MEMORY_THRESHOLD_EXCEEDED received:");
                    long maxMemory = tenuredGenPool.getUsage().getMax();
                    long usedMemory = tenuredGenPool.getUsage().getUsed();
                    System.out.println("Memory usage low!!!"); // @2 breakpoint
                    double percentageUsed = ((double) usedMemory) / maxMemory;
                    System.out.println("percentageUsed = " + percentageUsed);
                }
            }
        }, null, null);

        Collection<Object[]> numbers = new LinkedList();
        long counter = 0;
        while (!done) {
            numbers.add(new Object[1000]);
            counter++;
            if (counter % 1000 == 0) {
                Thread.sleep(100);
            }
        }
        System.out.println("Done");
    }

    private static MemoryPoolMXBean findTenuredGenPool() {
        for (MemoryPoolMXBean pool :
                ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == MemoryType.HEAP &&
                    pool.isUsageThresholdSupported()) {
                return pool;
            }
        }
        throw new RuntimeException("Could not find tenured space");
    }

    public static void setPercentageUsageThreshold(double percentage) {
        if (percentage <= 0.0 || percentage > 1.0) {
            throw new IllegalArgumentException("Percentage not in range");
        }
        System.out.println("Setting threashold for pool " + tenuredGenPool.getName() + " percentage:" + percentage);
        long maxMemory = tenuredGenPool.getUsage().getMax();
        long warningThreshold = (long) (maxMemory * percentage);
        tenuredGenPool.setUsageThreshold(warningThreshold);
    }
}

public class JdbStopInNotificationThreadTest extends JdbTest {

    private static final String DEBUGGEE_CLASS = JdbStopInNotificationThreadTestTarg.class.getName();
    private static final String PATTERN1_TEMPLATE = "^Breakpoint hit: \"thread=Notification Thread\", " +
            "JdbStopInNotificationThreadTestTarg\\$1\\.handleNotification\\(\\), line=%LINE_NUMBER.*\\R%LINE_NUMBER\\s+System\\.out\\.println\\(\"Memory usage low!!!\"\\);.*";

    private JdbStopInNotificationThreadTest() {
        super(DEBUGGEE_CLASS);
    }

    public static void main(String argv[]) {
        new JdbStopInNotificationThreadTest().run();
    }

    @Override
    protected void runCases() {
        if (isNotificationThreadDisabled()) {
            System.out.println("Notification Thread is disabled. Skipping the test");
            return;
        }
        int bpLine2 = parseBreakpoints(getTestSourcePath("JdbStopInNotificationThreadTest.java"), 2).get(0);
        jdb.command(JdbCommand.stopAt(DEBUGGEE_CLASS + "$1", bpLine2));
        String pattern = PATTERN1_TEMPLATE.replaceAll("%LINE_NUMBER", String.valueOf(bpLine2));
        jdb.command(JdbCommand.cont());
        new OutputAnalyzer(jdb.getJdbOutput()).shouldMatch(pattern);
    }

    private boolean isNotificationThreadDisabled() {
        int bpLine1 = parseBreakpoints(getTestSourcePath("JdbStopInNotificationThreadTest.java"), 1).get(0);
        jdb.command(JdbCommand.stopAt(DEBUGGEE_CLASS, bpLine1));
        jdb.command(JdbCommand.run());
        jdb.command(JdbCommand.threads());
        if (new OutputAnalyzer(jdb.getJdbOutput()).getOutput().contains("Notification Thread")) {
            return false;
        }
        return true;
    }
}
