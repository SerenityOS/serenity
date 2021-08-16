/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.gc.collection;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.GCHelper;

/**
 * Tests for event garbage_collection.
 * The test function is called from TestGCEvent*.java, with different worker threads.
 * Groups all events belonging to the same garbage collection (the same gcId).
 * The group of events belonging to the same GC is called a batch.
 *
 * This class contains the verifications done and the worker threads used to generate GCs.
 * The helper logic are in class GCHelper.
 *
 * Summary of verifications:
 *   All gcIds in garbage_collection event are unique.
 *
 *   All events in batch has the same gcId.
 *
 *   Number of garbage_collection events == GarbageCollectionMXBean.getCollectionCount()
 *
 *   garbage_collection.sum_pause_time approximately equals GarbageCollectionMXBean.getCollectionTime()
 *
 *   Batch contains expected events depending on garbage_collection.name
 *
 *   garbage_collection_start.timestamp == garbage_collection.startTime.
 *
 *   garbage_collection.timestamp >= timestamp for all other events in batch.
 *
 *   The start_garbage_collection and garbage_collection events must be synchronized.
 *     This means that there may be multiple start_garbage_collection before a garbage_collection,
 *     but garbage_collection.gcId must be equal to latest start_garbage_collection.gcId.
 *
 *   start_garbage_collection must be the first event in the batch,
 *     that means no event with same gcId before garbage_collection_start event.
 *
 *   garbage_collection.name matches what is expected by the collectors specified in initial_configuration.
 *
 *   Duration for event "vm/gc/phases/pause" >= 1. Duration for phase level events >= 0.
 *
 *
 */
public class GCEventAll {
    private String youngCollector = null;
    private String oldCollector = null;

    /**
     *  Trigger GC events by generating garbage and calling System.gc() concurrently.
     */
    public static void doTest() throws Throwable {
        // Trigger GC events by generating garbage and calling System.gc() concurrently.
        Thread[] workerThreads = new Thread[] {
                new Thread(GCEventAll.GarbageRunner.create(10)),
                new Thread(GCEventAll.SystemGcWaitRunner.create(10, 2, 1000))};
        GCEventAll test = new GCEventAll();
        test.doSingleTest(workerThreads);
    }

    /**
     * Runs the test once with given worker threads.
     * @param workerThreads Threads that generates GCs.
     * @param gcIds Set of all used gcIds
     * @throws Exception
     */
    private void doSingleTest(Thread[] workerThreads) throws Throwable {
        Recording recording = new Recording();
        enableAllGcEvents(recording);

        // Start with a full GC to minimize risk of getting extra GC between
        // getBeanCollectionCount() and recording.start().
        doSystemGc();
        GCHelper.CollectionSummary startBeanCount = GCHelper.CollectionSummary.createFromMxBeans();
        recording.start();

        for (Thread t : workerThreads) {
            t.start();
        }
        for (Thread t : workerThreads) {
            t.join();
        }

        // End with a full GC to minimize risk of getting extra GC between
        // recording.stop and getBeanCollectionCount().
        doSystemGc();
        // Add an extra System.gc() to make sure we get at least one full garbage_collection batch at
        // the end of the test. This extra System.gc() is only necessary when using "+ExplicitGCInvokesConcurrent".
        doSystemGc();

        recording.stop();
        GCHelper.CollectionSummary deltaBeanCount = GCHelper.CollectionSummary.createFromMxBeans();
        deltaBeanCount = deltaBeanCount.calcDelta(startBeanCount);

        List<RecordedEvent> events = Events.fromRecording(recording).stream()
            .filter(evt -> EventNames.isGcEvent(evt.getEventType()))
            .collect(Collectors.toList());
        RecordedEvent configEvent = GCHelper.getConfigEvent(events);
        youngCollector = Events.assertField(configEvent, "youngCollector").notEmpty().getValue();
        oldCollector = Events.assertField(configEvent, "oldCollector").notEmpty().getValue();
        verify(events, deltaBeanCount);
    }

    private void enableAllGcEvents(Recording recording) {
        FlightRecorder flightrecorder = FlightRecorder.getFlightRecorder();
        for (EventType et : flightrecorder.getEventTypes()) {
            if (EventNames.isGcEvent(et)) {
                recording.enable(et.getName());
                System.out.println("Enabled GC event: " + et.getName());
            }
        }
        System.out.println("All GC events enabled");
    }

    private static synchronized void doSystemGc() {
        System.gc();
    }

    /**
     * Does all verifications of the received events.
     *
     * @param events All flight recorder events.
     * @param beanCounts Number of collections and sum pause time reported by GarbageCollectionMXBeans.
     * @param gcIds All used gcIds. Must be unique.
     * @throws Exception
     */
    private void verify(List<RecordedEvent> events, GCHelper.CollectionSummary beanCounts) throws Throwable {
        List<GCHelper.GcBatch> gcBatches = null;
        GCHelper.CollectionSummary eventCounts = null;

        // For some GC configurations, the JFR recording may have stopped before we received the last gc event.
        try {
            gcBatches = GCHelper.GcBatch.createFromEvents(events);
            eventCounts = GCHelper.CollectionSummary.createFromEvents(gcBatches);

            verifyUniqueIds(gcBatches);
            verifyCollectorNames(gcBatches);
            verifyCollectionCause(gcBatches);
            verifyCollectionCount(eventCounts, beanCounts);
            verifyPhaseEvents(gcBatches);
            verifySingleGcBatch(gcBatches);
        } catch (Throwable t) {
            log(events, gcBatches, eventCounts, beanCounts);
            if (gcBatches != null) {
                for (GCHelper.GcBatch batch : gcBatches) {
                    System.out.println(String.format("Batch:%n%s", batch.getLog()));
                }
            }
            throw t;
        }
    }

    private boolean hasInputArgument(String arg) {
        return ManagementFactory.getRuntimeMXBean().getInputArguments().contains(arg);
    }

    private List<RecordedEvent> getEventsWithGcId(List<RecordedEvent> events, int gcId) {
        List<RecordedEvent> batchEvents = new ArrayList<>();
        for (RecordedEvent event : events) {
            if (GCHelper.isGcEvent(event) && GCHelper.getGcId(event) == gcId) {
                batchEvents.add(event);
            }
        }
        return batchEvents;
    }

    private boolean containsAnyPath(List<RecordedEvent> events, String[] paths) {
        List<String> pathList = Arrays.asList(paths);
        for (RecordedEvent event : events) {
            if (pathList.contains(event.getEventType().getName())) {
                return true;
            }
        }
        return false;
    }

    private int getLastGcId(List<RecordedEvent> events) {
        int lastGcId = -1;
        for (RecordedEvent event : events) {
            if (GCHelper.isGcEvent(event)) {
                int gcId = GCHelper.getGcId(event);
                if (gcId > lastGcId) {
                    lastGcId = gcId;
                }
            }
        }
        Asserts.assertTrue(lastGcId != -1, "No gcId found");
        return lastGcId;
    }

    /**
     * Verifies collection count reported by flight recorder events against the values
     * reported by GarbageCollectionMXBean.
     * Number of collections should match exactly.
     * Sum pause time are allowed some margin of error because of rounding errors in measurements.
     */
    private void verifyCollectionCount(GCHelper.CollectionSummary eventCounts, GCHelper.CollectionSummary beanCounts) {
        verifyCollectionCount(youngCollector, eventCounts.collectionCountYoung, beanCounts.collectionCountYoung);
        verifyCollectionCount(oldCollector, eventCounts.collectionCountOld, beanCounts.collectionCountOld);
    }

    private void verifyCollectionCount(String collector, long eventCounts, long beanCounts) {
        if (GCHelper.gcG1Old.equals(oldCollector)) {
            // MXBean does not report old collections for G1Old, so we have nothing to compare with.
            return;
        }
        // JFR events and GarbageCollectorMXBean events are not updated at the same time.
        // This means that number of collections may diff.
        // We allow a diff of +- 1 collection count.
        long minCount = Math.max(0, beanCounts - 1);
        long maxCount = beanCounts + 1;
        Asserts.assertGreaterThanOrEqual(eventCounts, minCount, "Too few event counts for collector " + collector);
        Asserts.assertLessThanOrEqual(eventCounts, maxCount, "Too many event counts for collector " + collector);
    }

    /**
     * Verifies that all events belonging to a single GC are ok.
     * A GcBatch contains all flight recorder events that belong to a single GC.
     */
    private void verifySingleGcBatch(List<GCHelper.GcBatch> batches) {
        for (GCHelper.GcBatch batch : batches) {
            //System.out.println("batch:\r\n" + batch.getLog());
            try {
                RecordedEvent endEvent = batch.getEndEvent();
                Asserts.assertNotNull(endEvent, "No end event in batch.");
                Asserts.assertNotNull(batch.getName(), "No method name in end event.");
                long longestPause = Events.assertField(endEvent, "longestPause").atLeast(0L).getValue();
                Events.assertField(endEvent, "sumOfPauses").atLeast(longestPause).getValue();
                Instant batchStartTime = endEvent.getStartTime();
                Instant batchEndTime = endEvent.getEndTime();
                for (RecordedEvent event : batch.getEvents()) {
                    if (event.getEventType().getName().contains("AllocationRequiringGC")) {
                        // Unlike other events, these are sent *before* a GC.
                        Asserts.assertLessThanOrEqual(event.getStartTime(), batchStartTime, "Timestamp in event after start event, should be sent before GC start");
                    } else {
                        Asserts.assertGreaterThanOrEqual(event.getStartTime(), batchStartTime, "startTime in event before batch start event, should be sent after GC start");
                    }
                    Asserts.assertLessThanOrEqual(event.getEndTime(), batchEndTime, "endTime in event after batch end event, should be sent before GC end");
                }

                // Verify that all required events has been received.
                String[] requiredEvents = GCHelper.requiredEvents.get(batch.getName());
                Asserts.assertNotNull(requiredEvents, "No required events specified for " + batch.getName());
                for (String requiredEvent : requiredEvents) {
                    boolean b = batch.containsEvent(requiredEvent);
                    Asserts.assertTrue(b, String.format("%s does not contain event %s", batch, requiredEvent));
                }

                // Verify that we have exactly one heap_summary "Before GC" and one "After GC".
                int countBeforeGc = 0;
                int countAfterGc = 0;
                for (RecordedEvent event : batch.getEvents()) {
                    if (GCHelper.event_heap_summary.equals(event.getEventType().getName())) {
                        String when = Events.assertField(event, "when").notEmpty().getValue();
                        if ("Before GC".equals(when)) {
                            countBeforeGc++;
                        } else if ("After GC".equals(when)) {
                            countAfterGc++;
                        } else {
                            Asserts.fail("Unknown value for heap_summary.when: '" + when + "'");
                        }
                    }
                }
                Asserts.assertEquals(1, countBeforeGc, "Unexpected number of heap_summary.before_gc");
                Asserts.assertEquals(1, countAfterGc, "Unexpected number of heap_summary.after_gc");
            } catch (Throwable e) {
                GCHelper.log("verifySingleGcBatch failed for gcEvent:");
                GCHelper.log(batch.getLog());
                throw e;
            }
        }
    }

    private Set<Integer> verifyUniqueIds(List<GCHelper.GcBatch> batches) {
        Set<Integer> gcIds = new HashSet<>();
        for (GCHelper.GcBatch batch : batches) {
            Integer gcId = new Integer(batch.getGcId());
            Asserts.assertFalse(gcIds.contains(gcId), "Duplicate gcId: " + gcId);
            gcIds.add(gcId);
        }
        return gcIds;
    }

    private void verifyPhaseEvents(List<GCHelper.GcBatch> batches) {
        for (GCHelper.GcBatch batch : batches) {
            for(RecordedEvent event : batch.getEvents()) {
                if (event.getEventType().getName().contains(GCHelper.pauseLevelEvent)) {
                    Instant batchStartTime = batch.getEndEvent().getStartTime();
                    Asserts.assertGreaterThanOrEqual(
                        event.getStartTime(), batchStartTime, "Phase startTime >= batch startTime. Event:" + event);

                    // Duration for event "vm/gc/phases/pause" must be >= 1. Other phase event durations must be >= 0.
                    Duration minDuration = Duration.ofNanos(GCHelper.event_phases_pause.equals(event.getEventType().getName()) ? 1 : 0);
                    Duration duration = event.getDuration();
                    Asserts.assertGreaterThanOrEqual(duration, minDuration, "Wrong duration. Event:" + event);
                }
            }
        }
    }

    /**
     * Verifies that the collector name in initial configuration matches the name in garbage configuration event.
     * If the names are not equal, then we check if this is an expected collector override.
     * For example, if old collector in initial config is "G1Old" we allow both event "G1Old" and "SerialOld".
     */
    private void verifyCollectorNames(List<GCHelper.GcBatch> batches) {
        for (GCHelper.GcBatch batch : batches) {
            String name = batch.getName();
            Asserts.assertNotNull(name, "garbage_collection.name was null");
            boolean isYoung = batch.isYoungCollection();
            String expectedName = isYoung ? youngCollector : oldCollector;
            if (!expectedName.equals(name)) {
                // Collector names not equal. Check if the collector has been overridden by an expected collector.
                String overrideKey = expectedName + "." + name;
                boolean isOverride = GCHelper.collectorOverrides.contains(overrideKey);
                Asserts.assertTrue(isOverride, String.format("Unexpected event name(%s) for collectors(%s, %s)", name, youngCollector, oldCollector));
            }
        }
    }

    /**
     * Verifies field "cause" in garbage_collection event.
     * Only check that at cause is not null and that at least 1 cause is "System.gc()"
     * We might want to check more cause reasons later.
     */
    private void verifyCollectionCause(List<GCHelper.GcBatch> batches) {
        int systemGcCount = 0;
        for (GCHelper.GcBatch batch : batches) {
            RecordedEvent endEvent = batch.getEndEvent();
            String cause = Events.assertField(endEvent, "cause").notEmpty().getValue();
            // A System.GC() can be consolidated into a GCLocker GC
            if (cause.equals("System.gc()") || cause.equals("GCLocker Initiated GC")) {
                systemGcCount++;
            }
            Asserts.assertNotNull(batch.getName(), "garbage_collection.name was null");
        }
        final String msg = "No event with cause=System.gc(), collectors(%s, %s)";
        Asserts.assertTrue(systemGcCount > 0, String.format(msg, youngCollector, oldCollector));
    }

    private void log(List<RecordedEvent> events, List<GCHelper.GcBatch> batches,
        GCHelper.CollectionSummary eventCounts, GCHelper.CollectionSummary beanCounts) {
        GCHelper.log("EventCounts:");
        if (eventCounts != null) {
            GCHelper.log(eventCounts.toString());
        }
        GCHelper.log("BeanCounts:");
        if (beanCounts != null) {
            GCHelper.log(beanCounts.toString());
        }
    }

    /**
     * Thread that does a number of System.gc().
     */
    public static class SystemGcRunner implements Runnable {
        private final int totalCollections;

        public SystemGcRunner(int totalCollections) {
            this.totalCollections = totalCollections;
        }

        public static SystemGcRunner create(int totalCollections) {
            return new SystemGcRunner(totalCollections);
        }

        public void run() {
            for (int i = 0; i < totalCollections; i++) {
                GCEventAll.doSystemGc();
            }
        }
    }

    /**
     * Thread that creates garbage until a certain number of GCs has been run.
     */
    public static class GarbageRunner implements Runnable {
        private final int totalCollections;
        public byte[] dummyBuffer = null;

        public GarbageRunner(int totalCollections) {
            this.totalCollections = totalCollections;
        }

        public static GarbageRunner create(int totalCollections) {
            return new GarbageRunner(totalCollections);
        }

        public void run() {
            long currCollections = GCHelper.CollectionSummary.createFromMxBeans().sum();
            long endCollections = totalCollections + currCollections;
            Random r = new Random(0);
            while (true) {
                for (int i = 0; i < 1000; i++) {
                    dummyBuffer = new byte[r.nextInt(10000)];
                }
                if (GCHelper.CollectionSummary.createFromMxBeans().sum() >= endCollections) {
                    break;
                }
            }
        }
    }

    /**
     * Thread that runs System.gc() and then wait for a number of GCs or a maximum time.
     */
    public static class SystemGcWaitRunner implements Runnable {
        private final int totalCollections;
        private final int minWaitCollections;
        private final long maxWaitMillis;

        public SystemGcWaitRunner(int totalCollections, int minWaitCollections, long maxWaitMillis) {
            this.totalCollections = totalCollections;
            this.minWaitCollections = minWaitCollections;
            this.maxWaitMillis = maxWaitMillis;
        }

        public static SystemGcWaitRunner create(int deltaCollections, int minWaitCollections, long maxWaitMillis) {
            return new SystemGcWaitRunner(deltaCollections, minWaitCollections, maxWaitMillis);
        }

        public void run() {
            long currCount = GCHelper.CollectionSummary.createFromMxBeans().sum();
            long endCount = totalCollections + currCount;
            long nextSystemGcCount = currCount + minWaitCollections;
            long now = System.currentTimeMillis();
            long nextSystemGcMillis = now + maxWaitMillis;

            while (true) {
                if (currCount >= nextSystemGcCount || System.currentTimeMillis() > nextSystemGcMillis) {
                    GCEventAll.doSystemGc();
                    currCount = GCHelper.CollectionSummary.createFromMxBeans().sum();
                    nextSystemGcCount = currCount + minWaitCollections;
                } else {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                currCount = GCHelper.CollectionSummary.createFromMxBeans().sum();
                if (currCount >= endCount) {
                    break;
                }
            }
        }
    }

}
