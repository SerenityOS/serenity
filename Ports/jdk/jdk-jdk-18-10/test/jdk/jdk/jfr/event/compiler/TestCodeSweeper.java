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

package jdk.jfr.event.compiler;

import java.lang.management.MemoryPoolMXBean;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;
import sun.hotspot.WhiteBox;
import sun.hotspot.code.BlobType;
import sun.hotspot.code.CodeBlob;

/**
 * Test for events: vm/code_sweeper/sweep vm/code_cache/full vm/compiler/failure
 *
 * We verify: 1. That sweptCount >= flushedCount + zombifiedCount 2. That
 * sweepIndex increases by 1. 3. We should get at least one of each of the
 * events listed above.
 *
 * NOTE! The test is usually able to trigger the events but not always. If an
 * event is received, the event is verified. If an event is missing, we do NOT
 * fail.
 */
/**
 * @test TestCodeSweeper
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:-SegmentedCodeCache -XX:+WhiteBoxAPI jdk.jfr.event.compiler.TestCodeSweeper
 */

public class TestCodeSweeper {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    private static final int COMP_LEVEL_SIMPLE = 1;
    private static final int COMP_LEVEL_FULL_OPTIMIZATION = 4;
    private static final int SIZE = 1;
    private static final String METHOD_NAME = "verifyFullEvent";
    private static final String pathSweep = EventNames.SweepCodeCache;
    private static final String pathFull = EventNames.CodeCacheFull;
    private static final String pathFailure = EventNames.CompilationFailure;
    public static final long SEGMENT_SIZE = WhiteBox.getWhiteBox().getUintxVMFlag("CodeCacheSegmentSize");
    public static final long MIN_BLOCK_LENGTH = WhiteBox.getWhiteBox().getUintxVMFlag("CodeCacheMinBlockLength");
    public static final long MIN_ALLOCATION = SEGMENT_SIZE * MIN_BLOCK_LENGTH;
    private static final double CACHE_USAGE_COEF = 0.95d;

    public static void main(String[] args) throws Throwable {
        Asserts.assertTrue(BlobType.getAvailable().contains(BlobType.All), "Test does not support SegmentedCodeCache");

        System.out.println("************************************************");
        System.out.println("This test will warn that the code cache is full.");
        System.out.println("That is expected and is the purpose of the test.");
        System.out.println("************************************************");

        Recording r = new Recording();
        r.enable(pathSweep);
        r.enable(pathFull);
        r.enable(pathFailure);
        r.start();
        provokeEvents();
        r.stop();

        int countEventSweep = 0;
        int countEventFull = 0;
        int countEventFailure = 0;

        List<RecordedEvent> events = Events.fromRecording(r);
        Events.hasEvents(events);
        for (RecordedEvent event : events) {
            switch (event.getEventType().getName()) {
            case pathSweep:
                countEventSweep++;
                verifySingleSweepEvent(event);
                break;
            case pathFull:
                countEventFull++;
                verifyFullEvent(event);
                break;
            case pathFailure:
                countEventFailure++;
                verifyFailureEvent(event);
                break;
            }
        }

        System.out.println(String.format("eventCount: %d, %d, %d", countEventSweep, countEventFull, countEventFailure));
    }

    private static boolean canAllocate(double size, long maxSize, MemoryPoolMXBean bean) {
        // Don't fill too much to have space for adapters. So, stop after crossing 95% and
        // don't allocate in case we'll cross 97% on next allocation.
        double used = bean.getUsage().getUsed();
        return (used <= CACHE_USAGE_COEF * maxSize) &&
               (used + size <= (CACHE_USAGE_COEF + 0.02d)  * maxSize);
    }

    private static void provokeEvents() throws NoSuchMethodException, InterruptedException {
        // Prepare for later, since we don't want to trigger any compilation
        // setting this up.
        Method method = TestCodeSweeper.class.getDeclaredMethod(METHOD_NAME, new Class[] { RecordedEvent.class });
        String directive = "[{ match: \"" + TestCodeSweeper.class.getName().replace('.', '/')
                + "." + METHOD_NAME + "\", " + "BackgroundCompilation: false }]";

        // Fill up code heaps until they are almost full
        // to trigger the vm/code_sweeper/sweep event.
        ArrayList<Long> blobs = new ArrayList<>();
        MemoryPoolMXBean bean = BlobType.All.getMemoryPool();
        long max = bean.getUsage().getMax();
        long headerSize = getHeaderSize(BlobType.All);
        long minAllocationUnit = Math.max(1, MIN_ALLOCATION - headerSize);
        long stopAt = max - minAllocationUnit;
        long addr = 0;

        // First allocate big blobs to speed things up
        for (long size = 100_000 * minAllocationUnit; size > 0; size /= 10) {
            while (canAllocate(size, max, bean) &&
                   (addr = WHITE_BOX.allocateCodeBlob(size, BlobType.All.id)) != 0) {
                blobs.add(addr);
            }
        }

        // Now allocate small blobs until the heap is almost full
        while (bean.getUsage().getUsed() < stopAt &&
               (addr = WHITE_BOX.allocateCodeBlob(SIZE, BlobType.All.id)) != 0) {
            blobs.add(addr);
        }

        // Trigger the vm/code_cache/full event by compiling one more
        // method. This also triggers the vm/compiler/failure event.
        Asserts.assertTrue(WHITE_BOX.addCompilerDirective(directive) == 1);
        try {
            if (!WHITE_BOX.enqueueMethodForCompilation(method, COMP_LEVEL_FULL_OPTIMIZATION)) {
                WHITE_BOX.enqueueMethodForCompilation(method, COMP_LEVEL_SIMPLE);
            }
        } finally {
            WHITE_BOX.removeCompilerDirective(1);
        }

        // Free memory
        for (Long blob : blobs) {
            WHITE_BOX.freeCodeBlob(blob);
        }
    }

    private static void verifyFullEvent(RecordedEvent event) throws Throwable {
        Events.assertField(event, "codeBlobType").notEmpty();
        Events.assertField(event, "unallocatedCapacity").atLeast(0L);
        Events.assertField(event, "entryCount").atLeast(0);
        Events.assertField(event, "methodCount").atLeast(0);
        Events.assertField(event, "adaptorCount").atLeast(0);
        Events.assertField(event, "fullCount").atLeast(0);

        // Verify startAddress <= commitedTopAddress <= reservedTopAddress.
        // Addresses may be so big that they overflow a long (treated as a
        // negative value), convert value to an octal string and compare the
        // string.
        String startAddress = Long.toOctalString(Events.assertField(event, "startAddress").getValue());
        String commitedTopAddress = Long.toOctalString(Events.assertField(event, "commitedTopAddress").getValue());
        String reservedTopAddress = Long.toOctalString(Events.assertField(event, "reservedTopAddress").getValue());
        Asserts.assertTrue(isOctalLessOrEqual(startAddress, commitedTopAddress), "startAddress<=commitedTopAddress: " + startAddress + "<=" + commitedTopAddress);
        Asserts.assertTrue(isOctalLessOrEqual(commitedTopAddress, reservedTopAddress), "commitedTopAddress<=reservedTopAddress: " + commitedTopAddress + "<=" + reservedTopAddress);
    }

    private static void verifyFailureEvent(RecordedEvent event) throws Throwable {
        Events.assertField(event, "failureMessage").notEmpty();
        Events.assertField(event, "compileId").atLeast(0);
    }

    private static void verifySingleSweepEvent(RecordedEvent event) throws Throwable {
        int flushedCount = Events.assertField(event, "flushedCount").atLeast(0).getValue();
        int zombifiedCount = Events.assertField(event, "zombifiedCount").atLeast(0).getValue();
        Events.assertField(event, "sweptCount").atLeast(flushedCount + zombifiedCount);
        Events.assertField(event, "sweepId").atLeast(0);
        Asserts.assertGreaterThanOrEqual(event.getStartTime(), Instant.EPOCH, "startTime was < 0");
        Asserts.assertGreaterThanOrEqual(event.getEndTime(), event.getStartTime(), "startTime was > endTime");
    }

    /** Returns true if less <= bigger. */
    private static boolean isOctalLessOrEqual(String less, String bigger) {
        if (less.length() > bigger.length()) {
            return false;
        }
        if (less.length() < bigger.length()) {
            return true;
        }
        return less.compareTo(bigger) <= 0;
    }

    public static final long getHeaderSize(BlobType btype) {
        long addr = WHITE_BOX.allocateCodeBlob(0, btype.id);
        int size = CodeBlob.getCodeBlob(addr).size;
        WHITE_BOX.freeCodeBlob(addr);
        return size;
    }
}
