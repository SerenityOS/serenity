/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.event.metadata;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.jfr.EventType;
import jdk.jfr.Experimental;
import jdk.jfr.FlightRecorder;
import jdk.test.lib.Utils;
import jdk.test.lib.jfr.EventNames;

/**
 * @test Check for JFR events not covered by tests
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main jdk.jfr.event.metadata.TestLookForUntestedEvents
 */
public class TestLookForUntestedEvents {
    private static final Path jfrTestRoot = Paths.get(Utils.TEST_SRC).getParent().getParent();
    private static final String MSG_SEPARATOR = "==========================";
    private static Set<String> jfrEventTypes = new HashSet<>();

    private static final Set<String> hardToTestEvents = new HashSet<>(
        Arrays.asList(
            "DataLoss", "IntFlag", "ReservedStackActivation",
            "DoubleFlag", "UnsignedLongFlagChanged", "IntFlagChanged",
            "UnsignedIntFlag", "UnsignedIntFlagChanged", "DoubleFlagChanged")
    );

    // GC uses specific framework to test the events, instead of using event names literally.
    // GC tests were inspected, as well as runtime output of GC tests.
    // The following events below are know to be covered based on that inspection.
    private static final Set<String> coveredGcEvents = new HashSet<>(
        Arrays.asList(
            "MetaspaceGCThreshold", "MetaspaceAllocationFailure", "MetaspaceOOM",
            "MetaspaceChunkFreeListSummary", "G1HeapSummary", "ParallelOldGarbageCollection",
            "OldGarbageCollection", "G1GarbageCollection", "GCPhasePause",
            "GCPhasePauseLevel1", "GCPhasePauseLevel2", "GCPhasePauseLevel3",
            "GCPhasePauseLevel4")
    );

    // Container events are tested in hotspot/jtreg/containers/docker/TestJFREvents.java
    private static final Set<String> coveredContainerEvents = new HashSet<>(
        Arrays.asList(
            "ContainerConfiguration", "ContainerCPUUsage", "ContainerCPUThrottling",
            "ContainerMemoryUsage", "ContainerIOUsage")
    );

    // This is a "known failure list" for this test.
    // NOTE: if the event is not covered, a bug should be open, and bug number
    // noted in the comments for this set.
    private static final Set<String> knownNotCoveredEvents = new HashSet<>(
    );

    // Experimental events
    private static final Set<String> experimentalEvents = new HashSet<>(
        Arrays.asList(
            "Flush", "SyncOnValueBasedClass")
    );


    public static void main(String[] args) throws Exception {
        for (EventType type : FlightRecorder.getFlightRecorder().getEventTypes()) {
            if (type.getAnnotation(Experimental.class) == null) {
                jfrEventTypes.add(type.getName().replace("jdk.", ""));
            }
        }

        checkEventNamesClass();
        lookForEventsNotCoveredByTests();
    }

    // Look thru JFR tests to make sure JFR events are referenced in the tests
    private static void lookForEventsNotCoveredByTests() throws Exception {
        List<Path> paths = Files.walk(jfrTestRoot)
            .filter(Files::isRegularFile)
            .filter(path -> isJavaFile(path))
            .collect(Collectors.toList());

        Set<String> eventsNotCoveredByTest = new HashSet<>(jfrEventTypes);
        for (String event : jfrEventTypes) {
            for (Path p : paths) {
                if (findStringInFile(p, event)) {
                    eventsNotCoveredByTest.remove(event);
                    break;
                }
            }
        }

        // Account for hard-to-test, experimental and GC tested events
        eventsNotCoveredByTest.removeAll(hardToTestEvents);
        eventsNotCoveredByTest.removeAll(coveredGcEvents);
        eventsNotCoveredByTest.removeAll(coveredContainerEvents);
        eventsNotCoveredByTest.removeAll(knownNotCoveredEvents);

        if (!eventsNotCoveredByTest.isEmpty()) {
            print(MSG_SEPARATOR + " Events not covered by test");
            for (String event: eventsNotCoveredByTest) {
                print(event);
            }
            print(MSG_SEPARATOR);
            throw new RuntimeException("Found JFR events not covered by tests");
        }
    }

    // Make sure all the JFR events are accounted for in jdk.test.lib.jfr.EventNames
    private static void checkEventNamesClass() throws Exception {
        // jdk.test.lib.jfr.EventNames
        Set<String> eventsFromEventNamesClass = new HashSet<>();
        for (Field f : EventNames.class.getFields()) {
            String name = f.getName();
            if (!name.equals("PREFIX")) {
                String eventName = (String) f.get(null);
                eventName = eventName.replace(EventNames.PREFIX, "");
                eventsFromEventNamesClass.add(eventName);
            }
        }

        // remove experimental events from eventsFromEventNamesClass since jfrEventTypes
        // excludes experimental events
        eventsFromEventNamesClass.removeAll(experimentalEvents);

        if (!jfrEventTypes.equals(eventsFromEventNamesClass)) {
            String exceptionMsg = "Events declared in jdk.test.lib.jfr.EventNames differ " +
                         "from events returned by FlightRecorder.getEventTypes()";
            print(MSG_SEPARATOR);
            print(exceptionMsg);
            print("");
            printSetDiff(jfrEventTypes, eventsFromEventNamesClass,
                        "jfrEventTypes", "eventsFromEventNamesClass");
            print("");

            print("This could be because:");
            print("1) You forgot to write a unit test. Please do so in test/jdk/jdk/jfr/event/");
            print("2) You wrote a unit test, but you didn't reference the event in");
            print("   test/lib/jdk/test/lib/jfr/EventNames.java. ");
            print("3) It is not feasible to test the event, not even a sanity test. ");
            print("   Add the event name to test/lib/jdk/test/lib/jfr/EventNames.java ");
            print("   and a short comment why it can't be tested");
            print("4) The event is experimental. Please add 'experimental=\"true\"' to <Event> ");
            print("   element in metadata.xml if it is a native event, or @Experimental if it is a ");
            print("   Java event. The event will now not show up in JMC");
            System.out.println(MSG_SEPARATOR);
            throw new RuntimeException(exceptionMsg);
        }
    }

    // ================ Helper methods
    private static boolean isJavaFile(Path p) {
        String fileName = p.getFileName().toString();
        int i = fileName.lastIndexOf('.');
        if ( (i < 0) || (i > fileName.length()) ) {
            return false;
        }
        return "java".equals(fileName.substring(i+1));
    }

    private static boolean findStringInFile(Path p, String searchTerm) throws IOException {
        long c = 0;
        try (Stream<String> stream = Files.lines(p)) {
            c = stream
                .filter(line -> line.contains(searchTerm))
                .count();
        }
        return (c != 0);
    }

    private static void printSetDiff(Set<String> a, Set<String> b,
        String setAName, String setBName) {
        if (a.size() > b.size()) {
            a.removeAll(b);
            System.out.printf("Set %s has more elements than set %s:", setAName, setBName);
            System.out.println();
            printSet(a);
        } else {
            b.removeAll(a);
            System.out.printf("Set %s has more elements than set %s:", setBName, setAName);
            System.out.println();
            printSet(b);
        }
    }

    private static void printSet(Set<String> set) {
        for (String e : set) {
            System.out.println(e);
        }
    }

    private static void print(String s) {
        System.out.println(s);
    }
}
