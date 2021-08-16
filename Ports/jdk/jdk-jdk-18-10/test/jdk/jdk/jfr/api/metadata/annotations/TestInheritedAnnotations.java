/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.api.metadata.annotations;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.jfr.Category;
import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Period;
import jdk.jfr.Recording;
import jdk.jfr.Registered;
import jdk.jfr.StackTrace;
import jdk.jfr.Threshold;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.metadata.annotations.TestInheritedAnnotations
 */
public class TestInheritedAnnotations {

    private static final String FAMILY_SMITH = "Family Smith";
    private static final String FAMILY_DOE = "Family Doe";
    private static final String FAMILY_JOHNSON_STRING = "Family Johnsson";

    @Enabled(false)
    @StackTrace(false)
    @Period("1 s")
    @Threshold("20 ms")
    @Category({FAMILY_SMITH})
    private static abstract class GrandFatherEvent extends Event {
    }

    @Enabled(true)
    @StackTrace(true)
    @Threshold("0 ns")
    @Category(FAMILY_DOE)
    private static class UncleEvent extends GrandFatherEvent {
    }

    @Registered(false)
    private static class AuntEvent extends GrandFatherEvent {
    }

    private static class CousineEvent extends AuntEvent {
    }

    private static class FatherEvent extends GrandFatherEvent {
    }

    @Category(FAMILY_JOHNSON_STRING)
    @Enabled(true)
    @Threshold("0 ns")
    private static class SonEvent extends FatherEvent {
    }

    @Enabled(true)
    @Period("1 s")
    private static class DaughterEvent extends  FatherEvent {
    }

    public static void main(String... args) throws Exception {
        FlightRecorder.addPeriodicEvent(DaughterEvent.class, () -> {
        });

        try (Recording r = new Recording()) {
            r.enable(EventNames.ActiveSetting);
            r.start();
            UncleEvent u = new UncleEvent();
            u.commit();
            FatherEvent f = new FatherEvent();
            f.commit();
            SonEvent s = new SonEvent();
            s.commit();
            AuntEvent a = new AuntEvent();
            a.commit();
            CousineEvent c = new CousineEvent();
            c.commit();

            r.stop();
            Path p = Utils.createTempFile("inherited-annotations", ".jfr");
            r.dump(p);
            List<RecordedEvent> events = RecordingFile.readAllEvents(p);
            assertNoGrandFather(events);
            assertUncle(events);
            assertNoFather(events);
            assertNoAunt();
            assertNoCousine(events);
            assertSon(events);
            assertUncleSettings(events);
            assertDaughterSettings(events);
        }
    }

    private static void assertNoCousine(List<RecordedEvent> events) throws Exception {
        assertMissingEventType(CousineEvent.class.getName());
    }

    private static void assertNoAunt() throws Exception {
        assertMissingEventType(AuntEvent.class.getName());
    }

    private static void assertUncleSettings(List<RecordedEvent> events) throws Exception {
        Map<String, String> daughterSettings = findEventSettings(events, DaughterEvent.class.getName());
        assertSetting(daughterSettings,"enabled", "true");
        assertSetting(daughterSettings, "period", "1 s");
    }

    private static void assertDaughterSettings(List<RecordedEvent> events) throws Exception {
        Map<String, String> uncleSettings = findEventSettings(events, UncleEvent.class.getName());
        assertSetting(uncleSettings,"enabled", "true");
        assertSetting(uncleSettings, "threshold", "0 ns");
        assertSetting(uncleSettings, "stackTrace", "true");
    }

    private static Map<String, String> findEventSettings(List<RecordedEvent> events, String eventName) throws Exception {
        Map<String, String> settings = new HashMap<>();
        EventType targetType = findEventType(eventName);
        for (RecordedEvent e : events) {
            EventType type = e.getEventType();
            if (type.getName().equals(EventNames.ActiveSetting)) {
                Long id = e.getValue("id");
                if (targetType.getId() == id) {
                    String name = e.getValue("name");
                    String value = e.getValue("value");
                    settings.put(name, value);
                }
            }
        }
        if (settings.isEmpty()) {
            throw new Exception("Could not find setting for event " + targetType.getName());
        }
        return settings;
    }

    private static void assertSetting(Map<String, String> settings, String settingName, String expectedValue) throws Exception {
        if (!settings.containsKey(settingName)) {
            throw new Exception("Missing setting with name " + settingName);
        }
        String value = settings.get(settingName);
        if (!expectedValue.equals(value)) {
            throw new Exception("Expected setting " + settingName + " to have value " + expectedValue +", but it had " + value);
        }
    }

    private static void assertSon(List<RecordedEvent> events) throws Exception {
        String eventName = SonEvent.class.getName();
        Events.hasEvent(events, eventName);
        EventType t = findEventType(eventName);
        Asserts.assertEquals(t.getCategoryNames(), Collections.singletonList(FAMILY_JOHNSON_STRING));
    }


    private static void assertNoFather(List<RecordedEvent> events) throws Exception {
        String eventName = FatherEvent.class.getName();
        Events.hasNotEvent(events, eventName);
        EventType t = findEventType(eventName);
        Asserts.assertEquals(t.getCategoryNames(), Collections.singletonList(FAMILY_SMITH));
    }

    private static void assertUncle(List<RecordedEvent> events) throws Exception {
        String eventName = UncleEvent.class.getName();
        Events.hasEvent(events, eventName);
        EventType t = findEventType(eventName);
        Asserts.assertEquals(t.getCategoryNames(), Collections.singletonList(FAMILY_DOE));
    }

    private static void assertNoGrandFather(List<RecordedEvent> events) throws Exception {
        assertMissingEventType(GrandFatherEvent.class.getName());
    }

    private static void assertMissingEventType(String eventName) throws Exception {
        try {
            findEventType(eventName);
        } catch (Exception e) {
            // as expected
            return;
        }
        throw new Exception("Event type " + eventName + " should not be available");
    }

    private static EventType findEventType(String name) throws Exception {
        for (EventType et : FlightRecorder.getFlightRecorder().getEventTypes()) {
            if (et.getName().equals(name)) {
                return et;
            }

        }
        throw new Exception("Could not find expected type " + name);
    }

}
