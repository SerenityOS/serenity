/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.event.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.jfr.Configuration;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.Registered;
import jdk.jfr.SettingDescriptor;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Tests that active setting are available in the ActiveSettingevent
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.event.runtime.TestActiveSettingEvent
 */
public final class TestActiveSettingEvent {

    private static class MyEvent extends Event {
    }

    @Registered(false)
    private static class MyRegistrationEvent extends Event {
    }

    private static final String ACTIVE_SETTING_EVENT_NAME = EventNames.ActiveSetting;

    public static void main(String[] args) throws Throwable {
        testDefaultSettings();;
        testProfileSettings();;
        testNewSettings();
        testChangedSetting();
        testUnregistered();
        testRegistration();
    }

    private static void testProfileSettings() throws Exception {
        testSettingConfiguration("profile");
    }

    private static void testDefaultSettings() throws Exception {
        testSettingConfiguration("default");
    }

    private static void testRegistration() throws Exception {
        // Register new
        try (Recording recording = new Recording()) {
            recording.enable(ACTIVE_SETTING_EVENT_NAME);
            recording.start();
            FlightRecorder.register(MyRegistrationEvent.class);
            recording.stop();
            List<RecordedEvent> events = Events.fromRecording(recording);
            Events.hasEvents(events);
            EventType type = EventType.getEventType(MyRegistrationEvent.class);
            assertSetting(events, type, "threshold", "0 ns");
            assertSetting(events, type, "enabled", "true");
            assertSetting(events, type, "stackTrace", "true");
        }
        // Register unregistered
        FlightRecorder.unregister(MyEvent.class);
        try (Recording recording = new Recording()) {
            recording.enable(ACTIVE_SETTING_EVENT_NAME);
            recording.start();
            FlightRecorder.register(MyRegistrationEvent.class);
            recording.stop();
            EventType type = EventType.getEventType(MyRegistrationEvent.class);
            List<RecordedEvent> events = Events.fromRecording(recording);
            Events.hasEvents(events);
            type = EventType.getEventType(MyRegistrationEvent.class);
            assertSetting(events, type, "threshold", "0 ns");
            assertSetting(events, type, "enabled", "true");
            assertSetting(events, type, "stackTrace", "true");
        }
    }

    private static void testUnregistered() throws Exception {
        FlightRecorder.register(MyEvent.class);
        EventType type = EventType.getEventType(MyEvent.class);
        FlightRecorder.unregister(MyEvent.class);
        try (Recording recording = new Recording()) {
            recording.enable(ACTIVE_SETTING_EVENT_NAME);
            recording.start();
            MyEvent m = new MyEvent();
            m.commit();
            recording.stop();
            List<RecordedEvent> events = Events.fromRecording(recording);
            Events.hasEvents(events);
            assertNotSetting(events, type, "threshold", "0 ns");
            assertNotSetting(events, type, "enabled", "true");
            assertNotSetting(events, type, "stackTrace", "true");
        }
    }

    private static void testNewSettings() throws Exception {
        try (Recording recording = new Recording()) {
            recording.enable(ACTIVE_SETTING_EVENT_NAME);
            recording.start();
            MyEvent m = new MyEvent();
            m.commit();
            recording.stop();
            List<RecordedEvent> events = Events.fromRecording(recording);
            Events.hasEvents(events);
            EventType type = EventType.getEventType(MyEvent.class);
            assertSetting(events, type, "threshold", "0 ns");
            assertSetting(events, type, "enabled", "true");
            assertSetting(events, type, "stackTrace", "true");
            assertNotSetting(events, type, "period", "everyChunk");
        }
    }

    private static void testChangedSetting() throws Exception {
        EventType type = EventType.getEventType(MyEvent.class);
        Map<String, String> base = new HashMap<>();
        base.put(ACTIVE_SETTING_EVENT_NAME + "#enabled", "true");
        try (Recording recording = new Recording()) {
            recording.setSettings(base);
            recording.start();
            Map<String, String> newS = new HashMap<>(base);
            newS.put(type.getName() + "#enabled", "true");
            newS.put(type.getName() + "#threshold", "11 ns");
            recording.setSettings(newS);
            recording.stop();
            List<RecordedEvent> events = Events.fromRecording(recording);
            Events.hasEvents(events);
            assertSetting(events, type, "threshold", "0 ns"); // initial value
            assertSetting(events, type, "enabled", "true");
            assertSetting(events, type, "threshold", "11 ns"); // changed value
        }
    }

    private static void assertSetting(List<RecordedEvent> events, EventType evenType, String settingName, String settingValue) throws Exception {
        if (!hasSetting(events, evenType, settingName, settingValue)) {
            throw new Exception("Could not find setting " + settingName + " with value " + settingValue + " for event type " + evenType.getName());
        }
    }

    private static void assertNotSetting(List<RecordedEvent> events, EventType evenType, String settingName, String settingValue) throws Exception {
        if (hasSetting(events, evenType, settingName, settingValue)) {
            throw new Exception("Found unexpected setting " + settingName + " with value " + settingValue + " for event type " + evenType.getName());
        }
    }

    private static boolean hasSetting(List<RecordedEvent> events, EventType evenType, String settingName, String settingValue) throws Exception {
        for (RecordedEvent e : events) {
            if (e.getEventType().getName().equals(ACTIVE_SETTING_EVENT_NAME)) {
                String name = e.getValue("name");
                String value = e.getValue("value");
                Long id = e.getValue("id");
                if (evenType.getId() == id && name.equals(settingName) && settingValue.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void testSettingConfiguration(String configurationName) throws Exception {
        System.out.println("Testing configuration " + configurationName);
        Configuration c = Configuration.getConfiguration(configurationName);
        Map<String, String> settingValues = c.getSettings();
        // Don't want to add these settings to the jfc-files we ship since they
        // are not useful to configure. They are however needed to make the test
        // pass.
        settingValues.put(EventNames.ActiveSetting + "#stackTrace", "false");
        settingValues.put(EventNames.ActiveSetting + "#threshold", "0 ns");
        settingValues.put(EventNames.ActiveRecording + "#stackTrace", "false");
        settingValues.put(EventNames.ActiveRecording + "#threshold", "0 ns");
        settingValues.put(EventNames.JavaExceptionThrow + "#threshold", "0 ns");
        settingValues.put(EventNames.JavaErrorThrow + "#threshold", "0 ns");
        settingValues.put(EventNames.SecurityProperty + "#threshold", "0 ns");
        settingValues.put(EventNames.TLSHandshake + "#threshold", "0 ns");
        settingValues.put(EventNames.X509Certificate + "#threshold", "0 ns");
        settingValues.put(EventNames.X509Validation + "#threshold", "0 ns");
        settingValues.put(EventNames.ProcessStart + "#threshold", "0 ns");
        settingValues.put(EventNames.Deserialization + "#threshold", "0 ns");

        try (Recording recording = new Recording(c)) {
            Map<Long, EventType> eventTypes = new HashMap<>();
            for (EventType et : FlightRecorder.getFlightRecorder().getEventTypes()) {
                eventTypes.put(et.getId(), et);
            }
            recording.start();
            Map<String, String> expectedSettings = new HashMap<>();
            for (EventType type : FlightRecorder.getFlightRecorder().getEventTypes()) {
                for (SettingDescriptor s : type.getSettingDescriptors()) {
                    String settingName = type.getName() + "#" + s.getName();
                    String value = settingValues.get(settingName);
                    if (value == null) {
                        throw new Exception("Could not find setting with name " + settingName);
                    }
                    // Prefer to have ms unit in jfc file
                    if (value.equals("0 ms")) {
                        value = "0 ns";
                    }
                    expectedSettings.put(settingName, value);
                }
            }
            recording.stop();

            for (RecordedEvent e : Events.fromRecording(recording)) {
                if (e.getEventType().getName().equals(ACTIVE_SETTING_EVENT_NAME)) {
                    Long id = e.getValue("id");
                    EventType et = eventTypes.get(id);
                    if (et == null) {
                        throw new Exception("Could not find event type with id " + id);
                    }
                    String name = e.getValue("name");
                    String settingName = et.getName() + "#" + name;
                    String value = e.getValue("value");
                    String expectedValue = expectedSettings.get(settingName);
                    if (expectedValue != null) {
                        if (value.equals("0 ms")) {
                            value = "0 ns";
                        }
                        Asserts.assertEquals(expectedValue, value, "Incorrect settings value for " + settingName + " was " + value + ", expected " + expectedValue);
                        expectedSettings.remove(settingName);
                    }
                }
            }
            if (!expectedSettings.isEmpty()) {
                throw new Exception("Not all setting in event. Missing " + expectedSettings.keySet());
            }
        }
    }
}
