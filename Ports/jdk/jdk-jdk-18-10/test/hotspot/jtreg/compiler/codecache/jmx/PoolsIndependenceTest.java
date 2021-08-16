/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test PoolsIndependenceTest
 * @summary testing of getUsageThreshold()
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @library /test/lib /
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *     -XX:+WhiteBoxAPI -XX:-UseCodeCacheFlushing -XX:-MethodFlushing
 *     -XX:+SegmentedCodeCache
 *     compiler.codecache.jmx.PoolsIndependenceTest
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *     -XX:+WhiteBoxAPI -XX:-UseCodeCacheFlushing -XX:-MethodFlushing
 *     -XX:-SegmentedCodeCache
 *     compiler.codecache.jmx.PoolsIndependenceTest
 */

package compiler.codecache.jmx;

import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import sun.hotspot.code.BlobType;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jtreg.SkippedException;

public class PoolsIndependenceTest implements NotificationListener {

    private final Map<String, AtomicInteger> counters;
    private final BlobType btype;
    private volatile long lastEventTimestamp;
    private volatile long maxUsageRegistered;
    private final long TEST_TIMEOUT_LIMIT = System.currentTimeMillis() +
        Utils.adjustTimeout(Utils.DEFAULT_TEST_TIMEOUT) - 5_000; // 5 seconds allowance is arbitrary

    public PoolsIndependenceTest(BlobType btype) {
        counters = new HashMap<>();
        for (BlobType bt : BlobType.getAvailable()) {
            counters.put(bt.getMemoryPool().getName(), new AtomicInteger(0));
        }
        this.btype = btype;
        lastEventTimestamp = 0;
        maxUsageRegistered = 0;
        CodeCacheUtils.disableCollectionUsageThresholds();
    }

    public static void main(String[] args) {
        for (BlobType bt : BlobType.getAvailable()) {
            new PoolsIndependenceTest(bt).runTest();
        }
    }

    protected void runTest() {
        MemoryPoolMXBean bean = btype.getMemoryPool();
        System.out.printf("INFO: Starting scenario with %s%n", bean.getName());
        ((NotificationEmitter) ManagementFactory.getMemoryMXBean()).
                addNotificationListener(this, null, null);
        final long usageThresholdLimit = bean.getUsage().getUsed() + 1;
        bean.setUsageThreshold(usageThresholdLimit);

        long beginTimestamp = System.currentTimeMillis();
        final long phaseTimeout = Math.min(TEST_TIMEOUT_LIMIT,
                beginTimestamp + 20_000); // 20 seconds is enought for everybody.

        CodeCacheUtils.WB.allocateCodeBlob(
                CodeCacheUtils.ALLOCATION_SIZE, btype.id);
        CodeCacheUtils.WB.fullGC();

        /* waiting for expected event to be received plus double the time took
         to receive expected event(for possible unexpected) and
         plus 1 second in case expected event received (almost)immediately */
        Utils.waitForCondition(() -> {
            maxUsageRegistered = Math.max(bean.getUsage().getUsed(), maxUsageRegistered);
            long currentTimestamp = System.currentTimeMillis();
            int eventsCount
                    = counters.get(btype.getMemoryPool().getName()).get();
            if (eventsCount > 0) {
                if (eventsCount > 1) {
                    return true;
                }
                long timeLastEventTook
                        = lastEventTimestamp - beginTimestamp;

                long awaitForUnexpectedTimeout
                        = 1000L + beginTimestamp + 3L * timeLastEventTook;

                return currentTimestamp > Math.min(phaseTimeout, awaitForUnexpectedTimeout);
            };

            if (currentTimestamp > phaseTimeout) {
                if (maxUsageRegistered < usageThresholdLimit) {
                    throw new SkippedException("The code cache usage hasn't exceeded" +
                            " the limit of " + usageThresholdLimit +
                            " (max usage reached is " + maxUsageRegistered + ")" +
                            " within test timeouts, can't test notifications");
                } else {
                    Asserts.fail("UsageThresholdLimit was set to " + usageThresholdLimit +
                            ", max usage of " + maxUsageRegistered + " have been registered" +
                            ", but no notifications issued");
                }
            }
            return false;
        });
        for (BlobType bt : BlobType.getAvailable()) {
            int expectedNotificationsAmount = bt.equals(btype) ? 1 : 0;
            CodeCacheUtils.assertEQorGTE(btype, counters.get(bt.getMemoryPool().getName()).get(),
                    expectedNotificationsAmount, String.format("Unexpected "
                            + "amount of notifications for pool: %s",
                            bt.getMemoryPool().getName()));
        }
        try {
            ((NotificationEmitter) ManagementFactory.getMemoryMXBean()).
                    removeNotificationListener(this);
        } catch (ListenerNotFoundException ex) {
            throw new AssertionError("Can't remove notification listener", ex);
        }
        System.out.printf("INFO: Scenario with %s finished%n", bean.getName());
    }

    @Override
    public void handleNotification(Notification notification, Object handback) {
        String nType = notification.getType();
        String poolName
                = CodeCacheUtils.getPoolNameFromNotification(notification);
        // consider code cache events only
        if (CodeCacheUtils.isAvailableCodeHeapPoolName(poolName)) {
            Asserts.assertEQ(MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED,
                    nType, "Unexpected event received: " + nType);
            // receiving events from available CodeCache-related beans only
            if (counters.get(poolName) != null) {
                counters.get(poolName).incrementAndGet();
                lastEventTimestamp = System.currentTimeMillis();
            }
        }
    }
}
