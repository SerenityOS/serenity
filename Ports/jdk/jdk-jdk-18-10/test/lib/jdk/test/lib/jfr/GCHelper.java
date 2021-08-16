/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package jdk.test.lib.jfr;

import static jdk.test.lib.Asserts.assertEquals;
import static jdk.test.lib.Asserts.assertNotEquals;
import static jdk.test.lib.Asserts.assertNotNull;
import static jdk.test.lib.Asserts.assertNull;
import static jdk.test.lib.Asserts.fail;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.RecordedEvent;

/**
 * Mixed helper classes to test GC events.
 */
public class GCHelper {
    public static final String event_garbage_collection = EventNames.GarbageCollection;
    public static final String event_young_garbage_collection = EventNames.YoungGarbageCollection;
    public static final String event_old_garbage_collection = EventNames.OldGarbageCollection;
    public static final String event_parold_garbage_collection = EventNames.ParallelOldCollection;
    public static final String event_g1_garbage_collection = EventNames.G1GarbageCollection;
    public static final String event_heap_summary = EventNames.GCHeapSummary;
    public static final String event_heap_ps_summary = EventNames.PSHeapSummary;
    public static final String event_heap_metaspace_summary = EventNames.MetaspaceSummary;
    public static final String event_reference_statistics = EventNames.GCReferenceStatistics;
    public static final String event_phases_pause = EventNames.GCPhasePause;
    public static final String event_phases_level_1 = EventNames.GCPhasePauseLevel1;
    public static final String event_phases_level_2 = EventNames.GCPhasePauseLevel2;
    public static final String event_phases_level_3 = EventNames.GCPhasePauseLevel3;

    public static final String gcG1New = "G1New";
    public static final String gcDefNew = "DefNew";
    public static final String gcParallelScavenge = "ParallelScavenge";
    public static final String gcG1Old = "G1Old";
    public static final String gcG1Full = "G1Full";
    public static final String gcSerialOld = "SerialOld";
    public static final String gcPSMarkSweep = "PSMarkSweep";
    public static final String gcParallelOld = "ParallelOld";
    public static final String pauseLevelEvent = "GCPhasePauseLevel";

    private static final List<String> g1HeapRegionTypes;
    private static final List<String> shenandoahHeapRegionStates;
    private static PrintStream defaultErrorLog = null;

    public static int getGcId(RecordedEvent event) {
        return Events.assertField(event, "gcId").getValue();
    }

    public static boolean isGcEvent(RecordedEvent event) {
        for (ValueDescriptor v : event.getFields()) {
            if ("gcId".equals(v.getName())) {
                return true;
            }
        }
        return false;
    }

//    public static String getEventDesc(RecordedEvent event) {
//      final String path = event.getEventType().getName();
//        if (!isGcEvent(event)) {
//            return path;
//        }
//        if (event_garbage_collection.equals(path)) {
//            String name = Events.assertField(event, "name").getValue();
//            String cause = Events.assertField(event, "cause").getValue();
//            return String.format("path=%s, gcId=%d, endTime=%d, name=%s, cause=%s, startTime=%d",
//                    path, getGcId(event), event.getEndTime(), name, cause, event.getStartTime());
//        } else {
//            return String.format("path=%s, gcId=%d, endTime=%d", path, getGcId(event), event.getEndTime());
//        }
//    }

    public static RecordedEvent getConfigEvent(List<RecordedEvent> events) throws Exception {
        for (RecordedEvent event : events) {
            if (EventNames.GCConfiguration.equals(event.getEventType().getName())) {
                return event;
            }
        }
        fail("Could not find event " + EventNames.GCConfiguration);
        return null;
    }

    public static void callSystemGc(int num, boolean withGarbage) {
        for (int i = 0; i < num; i++) {
            if (withGarbage) {
                makeGarbage();
            }
            System.gc();
        }
    }

    private static void makeGarbage() {
        Object[] garbage = new Object[1024];
        for (int i = 0; i < 1024; i++) {
            garbage[i] = new Object();
        }
    }

    // Removes gcEvents with lowest and highest gcID. This is used to filter out
    // any incomplete GCs if the recording started/stopped in the middle of a GC.
    // We also filters out events without gcId. Those events are not needed.
    public static List<RecordedEvent> removeFirstAndLastGC(List<RecordedEvent> events) {
        int minGcId = Integer.MAX_VALUE;
        int maxGcId = Integer.MIN_VALUE;
        // Find min/max gcId
        for (RecordedEvent event : events) {
            if (Events.hasField(event, "gcId")) {
                int gcId = Events.assertField(event, "gcId").getValue();
                minGcId = Math.min(gcId, minGcId);
                maxGcId = Math.max(gcId, maxGcId);
            }
        }

        // Add all events except those with gcId = min/max gcId
        List<RecordedEvent> filteredEvents = new ArrayList<>();
        for (RecordedEvent event : events) {
            if (Events.hasField(event, "gcId")) {
                int gcId = Events.assertField(event, "gcId").getValue();
                if (gcId != minGcId && gcId != maxGcId) {
                    filteredEvents.add(event);
                }
            }
        }
        return filteredEvents;
    }

    public static Map<String, Boolean> beanCollectorTypes = new HashMap<>();
    public static Set<String> collectorOverrides = new HashSet<>();
    public static Map<String, String[]> requiredEvents = new HashMap<>();

    static {
        // young GarbageCollectionMXBeans.
        beanCollectorTypes.put("G1 Young Generation", true);
        beanCollectorTypes.put("Copy", true);
        beanCollectorTypes.put("PS Scavenge", true);

        // old GarbageCollectionMXBeans.
        beanCollectorTypes.put("G1 Old Generation", false);
        beanCollectorTypes.put("PS MarkSweep", false);
        beanCollectorTypes.put("MarkSweepCompact", false);

        // List of expected collector overrides. "A.B" means that collector A may use collector B.
        collectorOverrides.add("G1Old.G1Full");
        collectorOverrides.add("SerialOld.PSMarkSweep");

        requiredEvents.put(gcG1New, new String[] {event_heap_summary, event_young_garbage_collection});
        requiredEvents.put(gcDefNew, new String[] {event_heap_summary, event_heap_metaspace_summary, event_phases_pause, event_phases_level_1, event_young_garbage_collection});
        requiredEvents.put(gcParallelScavenge, new String[] {event_heap_summary, event_heap_ps_summary, event_heap_metaspace_summary, event_reference_statistics, event_phases_pause, event_phases_level_1, event_young_garbage_collection});
        requiredEvents.put(gcG1Old, new String[] {event_heap_summary, event_old_garbage_collection});
        requiredEvents.put(gcG1Full, new String[] {event_heap_summary, event_heap_metaspace_summary, event_phases_pause, event_phases_level_1, event_old_garbage_collection});
        requiredEvents.put(gcSerialOld, new String[] {event_heap_summary, event_heap_metaspace_summary, event_phases_pause, event_phases_level_1, event_old_garbage_collection});
        requiredEvents.put(gcParallelOld, new String[] {event_heap_summary, event_heap_ps_summary, event_heap_metaspace_summary, event_reference_statistics, event_phases_pause, event_phases_level_1, event_old_garbage_collection, event_parold_garbage_collection});

        String[] g1HeapRegionTypeLiterals = new String[] {
                                                           "Free",
                                                           "Eden",
                                                           "Survivor",
                                                           "Starts Humongous",
                                                           "Continues Humongous",
                                                           "Old",
                                                           "Archive"
                                                         };

        g1HeapRegionTypes = Collections.unmodifiableList(Arrays.asList(g1HeapRegionTypeLiterals));

        String[] shenandoahHeapRegionStateLiterals = new String[] {
                                                                    "Empty Uncommitted",
                                                                    "Empty Committed",
                                                                    "Regular",
                                                                    "Humongous Start",
                                                                    "Humongous Continuation",
                                                                    "Humongous Start, Pinned",
                                                                    "Collection Set",
                                                                    "Pinned",
                                                                    "Collection Set, Pinned",
                                                                    "Trash"
        };

        shenandoahHeapRegionStates = Collections.unmodifiableList(Arrays.asList(shenandoahHeapRegionStateLiterals));
    }

    /**
     * Contains all GC events belonging to the same GC (same gcId).
     */
    public static class GcBatch {
        private List<RecordedEvent> events = new ArrayList<>();

        public int getGcId() {
            if (events.isEmpty()) {
                return -1;
            }
            return GCHelper.getGcId(events.get(0));
        }

        public String getName() {
            RecordedEvent endEvent = getEndEvent();
            String name = endEvent == null ? null : Events.assertField(endEvent, "name").getValue();
            return name == null ? "null" : name;
        }

        public RecordedEvent getEndEvent() {
            return getEvent(event_garbage_collection);
        }

        public boolean addEvent(RecordedEvent event) {
            if (!events.isEmpty()) {
                assertEquals(getGcId(), GCHelper.getGcId(event), "Wrong gcId in event. Error in test code.");
            }
            boolean isEndEvent = event_garbage_collection.equals(event.getEventType().getName());
            if (isEndEvent) {
                // Verify that we have not already got a garbage_collection event with this gcId.
                assertNull(getEndEvent(), String.format("Multiple %s for gcId %d", event_garbage_collection, getGcId()));
            }
            events.add(event);
            return isEndEvent;
        }

        public boolean isYoungCollection() {
            boolean isYoung = containsEvent(event_young_garbage_collection);
            boolean isOld = containsEvent(event_old_garbage_collection);
            assertNotEquals(isYoung, isOld, "isYoung and isOld was same for batch: " + toString());
            return isYoung;
        }

        public int getEventCount() {
            return events.size();
        }

        public RecordedEvent getEvent(int index) {
            return events.get(index);
        }

        public List<RecordedEvent> getEvents() {
            return events;
        }

        public RecordedEvent getEvent(String eventPath) {
            for (RecordedEvent event : events) {
                if (eventPath.equals(event.getEventType().getName())) {
                    return event;
                }
            }
            return null;
        }

        public boolean containsEvent(String eventPath) {
            return getEvent(eventPath) != null;
        }

        public String toString() {
            RecordedEvent endEvent = getEndEvent();
            Instant startTime = Instant.EPOCH;
            String cause = "?";
            String name = "?";
            if (endEvent != null) {
                name = getName();
                startTime = endEvent.getStartTime();
                cause = Events.assertField(endEvent, "cause").getValue();
            }
            return String.format("GcEvent: gcId=%d, method=%s, cause=%s, startTime=%s",
                    getGcId(), name, cause, startTime);
        }

        public String getLog() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.toString() + System.getProperty("line.separator"));
            for (RecordedEvent event : events) {
                sb.append(String.format("event: %s%n", event));
            }
            return sb.toString();
        }

        // Group all events info batches.
        public static List<GcBatch> createFromEvents(List<RecordedEvent> events) throws Exception {
            Stack<Integer> openGcIds = new Stack<>();
            List<GcBatch> batches = new ArrayList<>();
            GcBatch currBatch = null;

            for (RecordedEvent event : events) {
                if (!isGcEvent(event)) {
                    continue;
                }
                int gcId = GCHelper.getGcId(event);
                if (currBatch == null || currBatch.getGcId() != gcId) {
                    currBatch = null;
                    // Search for existing batch
                    for (GcBatch loopBatch : batches) {
                        if (gcId == loopBatch.getGcId()) {
                            currBatch = loopBatch;
                            break;
                        }
                    }
                    if (currBatch == null) {
                        // No existing batch. Create new.
                        currBatch = new GcBatch();
                        batches.add(currBatch);
                        openGcIds.push(Integer.valueOf(gcId));
                    }
                }
                boolean isEndEvent = currBatch.addEvent(event);
                if (isEndEvent) {
                    openGcIds.pop();
                }
            }
            // Verify that all start_garbage_collection events have received a corresponding "garbage_collection" event.
            for (GcBatch batch : batches) {
                if (batch.getEndEvent() == null) {
                    System.out.println(batch.getLog());
                }
                assertNotNull(batch.getEndEvent(), "GcBatch has no end event");
            }
            return batches;
        }
    }

    /**
     * Contains number of collections and sum pause time for young and old collections.
     */
    public static class CollectionSummary {
        public long collectionCountOld;
        public long collectionCountYoung;
        public long collectionTimeOld;
        public long collectionTimeYoung;
        private Set<String> names = new HashSet<>();

        public void add(String collectorName, boolean isYoung, long count, long time) {
            if (isYoung) {
                collectionCountYoung += count;
                collectionTimeYoung += time;
            } else {
                collectionCountOld += count;
                collectionTimeOld += time;
            }
            if (!names.contains(collectorName)) {
                names.add(collectorName);
            }
        }

        public long sum() {
            return collectionCountOld + collectionCountYoung;
        }

        public CollectionSummary calcDelta(CollectionSummary prev) {
            CollectionSummary delta = new CollectionSummary();
            delta.collectionCountOld = this.collectionCountOld - prev.collectionCountOld;
            delta.collectionTimeOld = this.collectionTimeOld - prev.collectionTimeOld;
            delta.collectionCountYoung = this.collectionCountYoung - prev.collectionCountYoung;
            delta.collectionTimeYoung = this.collectionTimeYoung - prev.collectionTimeYoung;
            delta.names.addAll(this.names);
            delta.names.addAll(prev.names);
            return delta;
        }

        public static CollectionSummary createFromMxBeans() {
            CollectionSummary summary = new CollectionSummary();
            List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
            for (int c=0; c<gcBeans.size(); c++) {
                GarbageCollectorMXBean currBean = gcBeans.get(c);
                Boolean isYoung = beanCollectorTypes.get(currBean.getName());
                assertNotNull(isYoung, "Unknown MXBean name: " + currBean.getName());
                long collectionTime = currBean.getCollectionTime() * 1000; // Convert from millis to micros.
                summary.add(currBean.getName(), isYoung.booleanValue(), currBean.getCollectionCount(), collectionTime);
            }
            return summary;
        }

        public static CollectionSummary createFromEvents(List<GcBatch> batches) {
            CollectionSummary summary = new CollectionSummary();
            for (GcBatch batch : batches) {
                RecordedEvent endEvent = batch.getEndEvent();
                assertNotNull(endEvent, "No end event in batch with gcId " + batch.getGcId());
                String name = batch.getName();
                summary.add(name, batch.isYoungCollection(), 1, Events.assertField(endEvent, "sumOfPauses").getValue());
            }
            return summary;
        }

        public String toString() {
            StringBuilder collectorNames = new StringBuilder();
            for (String s : names) {
                if (collectorNames.length() > 0) {
                    collectorNames.append(", ");
                }
                collectorNames.append(s);
            }
            return String.format("CollectionSummary: young.collections=%d, young.time=%d, old.collections=%d, old.time=%d, collectors=(%s)",
                    collectionCountYoung, collectionTimeYoung, collectionCountOld, collectionTimeOld, collectorNames);
        }
    }

    public static PrintStream getDefaultErrorLog() {
        if (defaultErrorLog == null) {
            try {
                defaultErrorLog = new PrintStream(new FileOutputStream("error.log", true));
            } catch (IOException e) {
                e.printStackTrace();
                defaultErrorLog = System.err;
            }
        }
        return defaultErrorLog;
    }

    public static void log(Object msg) {
        log(msg, System.err);
        log(msg, getDefaultErrorLog());
    }

    public static void log(Object msg, PrintStream ps) {
        ps.println(msg);
    }

    public static boolean isValidG1HeapRegionType(final String type) {
        return g1HeapRegionTypes.contains(type);
    }

    public static boolean assertIsValidShenandoahHeapRegionState(final String state) {
        if (!shenandoahHeapRegionStates.contains(state)) {
            throw new AssertionError("Unknown state '" + state + "', valid heap region states are " + shenandoahHeapRegionStates);
        }
        return true;
    }

    /**
     * Helper function to align heap size up.
     *
     * @param value
     * @param alignment
     * @return aligned value
     */
    public static long alignUp(long value, long alignment) {
        return (value + alignment - 1) & ~(alignment - 1);
    }

    /**
     * Helper function to align heap size down.
     *
     * @param value
     * @param alignment
     * @return aligned value
     */
    public static long alignDown(long value, long alignment) {
        return value & ~(alignment - 1);
    }
}
