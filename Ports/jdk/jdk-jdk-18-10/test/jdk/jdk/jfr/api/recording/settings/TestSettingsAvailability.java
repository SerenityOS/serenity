/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.api.recording.settings;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.SettingDescriptor;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.jfr.EventNames;

/**
 * @test
 * @summary Verifies that event types has the correct type of settings
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.settings.TestSettingsAvailability
 */
public class TestSettingsAvailability {
    public static void main(String[] args) throws Throwable {
        testKnownSettings();
        testSettingPersistence();
    }

    private static void testSettingPersistence() throws IOException, Exception {
        Map<String, EventType> inMemoryTypes = new HashMap<>();
        for (EventType type : FlightRecorder.getFlightRecorder().getEventTypes()) {
            inMemoryTypes.put(type.getName(), type);
        }

        Path p = Paths.get("recording.jfr");
        try (Recording r = new Recording()) {
            r.start();
            r.stop();
            r.dump(p);
            try (RecordingFile rf = new RecordingFile(p)) {
                for (EventType parsedType : rf.readEventTypes()) {
                    EventType inMem = inMemoryTypes.get(parsedType.getName());
                    if (inMem == null) {
                        throw new Exception("Superflous event type " + parsedType.getName() + " in recording");
                    }
                    Set<String> inMemsettings = new HashSet<>();
                    for (SettingDescriptor sd : inMem.getSettingDescriptors()) {
                        inMemsettings.add(sd.getName());
                    }

                    for (SettingDescriptor parsedSetting : parsedType.getSettingDescriptors()) {
                        if (!inMemsettings.contains(parsedSetting.getName())) {
                            throw new Exception("Superflous setting " + parsedSetting.getName() + " in " + parsedType.getName());
                        }
                        inMemsettings.remove(parsedSetting.getName());
                    }
                    if (!inMemsettings.isEmpty()) {
                        throw new Exception("Missing settings " + inMemsettings + " for event type " + parsedType.getName() + " in recording");
                    }
                }
            }
        }
    }

    private static void testKnownSettings() throws Exception {
        testSetting(EventNames.JVMInformation, "enabled", "period");
        testSetting(EventNames.FileRead, "enabled", "threshold", "stackTrace");
        testSetting(EventNames.FileWrite, "enabled", "threshold","stackTrace");
        testSetting(EventNames.ExceptionStatistics, "enabled", "period");
        testSetting(EventNames.SocketRead, "enabled", "threshold", "stackTrace");
        testSetting(EventNames.SocketWrite, "enabled", "threshold", "stackTrace");
        testSetting(EventNames.ActiveRecording, "enabled", "threshold", "stackTrace");
        testSetting(EventNames.ActiveSetting, "enabled", "threshold", "stackTrace");
        testSetting(EventNames.JavaExceptionThrow, "enabled", "threshold", "stackTrace");
    }

    private static void testSetting(String eventName, String... settingNames) throws Exception {
        for (EventType type : FlightRecorder.getFlightRecorder().getEventTypes()) {
            if (eventName.equals(type.getName())) {
                Set<String> settings = new HashSet<>();
                for (SettingDescriptor sd : type.getSettingDescriptors()) {
                    settings.add(sd.getName());
                }
                for (String settingName : settingNames) {
                    if (!settings.contains(settingName)) {
                        throw new Exception("Missing setting " + settingName + " in " + eventName);
                    }
                    settings.remove(settingName);
                }
                if (!settings.isEmpty()) {
                    throw new Exception("Superflous settings " + settings + " in event " + eventName);
                }
            }
        }
    }
}
