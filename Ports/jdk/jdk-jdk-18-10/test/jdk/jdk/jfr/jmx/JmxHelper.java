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

package jdk.jfr.jmx;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.tools.attach.VirtualMachine;
import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import jdk.jfr.SettingDescriptor;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.management.jfr.EventTypeInfo;
import jdk.management.jfr.FlightRecorderMXBean;
import jdk.management.jfr.RecordingInfo;
import jdk.management.jfr.SettingDescriptorInfo;
import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import jdk.test.lib.jfr.CommonHelper;
import jdk.test.lib.jfr.Events;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class JmxHelper {
    private static final String LOCAL_CONNECTION_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";

    public static RecordingInfo getJmxRecording(long recId) {
        for (RecordingInfo r : getFlighteRecorderMXBean().getRecordings()) {
            if (r.getId() == recId) {
                return r;
            }
        }
        Asserts.fail("No RecordingInfo with id " + recId);
        return null;
    }

    public static Recording getJavaRecording(long recId) {
        for (Recording r : FlightRecorder.getFlightRecorder().getRecordings()) {
            if (r.getId() == recId) {
                return r;
            }
        }
        Asserts.fail("No Recording with id " + recId);
        return null;
    }

    public static void verifyState(long recId, RecordingState state, List<RecordingInfo> recordings) {
        RecordingInfo r = verifyExists(recId, recordings);
        verifyState(r, state);
    }

    public static void verifyState(RecordingInfo recording, RecordingState state) {
        final String actual = recording.getState().toString();
        final String expected = state.toString();
        Asserts.assertEquals(actual, expected, "Wrong state");
    }

    public static void verifyState(long recId, RecordingState state, FlightRecorderMXBean bean) throws Exception {
        FlightRecorder jfr = FlightRecorder.getFlightRecorder();
        Recording recording = CommonHelper.verifyExists(recId, jfr.getRecordings());
        CommonHelper.verifyRecordingState(recording, state);
        verifyState(recId, state, bean.getRecordings());
    }

    public static void verifyNotExists(long recId, List<RecordingInfo> recordings) {
        for (RecordingInfo r : recordings) {
            if (recId == r.getId()) {
                logRecordingInfos(recordings);
                Asserts.fail("Recording should not exist, id=" + recId);
            }
        }
    }

    public static RecordingInfo verifyExists(long recId, List<RecordingInfo> recordings) {
        for (RecordingInfo r : recordings) {
            if (recId == r.getId()) {
                return r;
            }
        }
        logRecordingInfos(recordings);
        Asserts.fail("Recording not found, id=" + recId);
        return null;
    }


    public static void logRecordingInfos(List<RecordingInfo> recordings) {
        System.out.println("RecordingInfos:");
        for (RecordingInfo r : recordings) {
            System.out.println(asString(r));
        }
    }

    public static void logRecordings(List<Recording> recordings) {
        System.out.println("Recordings:");
        for (Recording r : recordings) {
            System.out.println(asString(r));
        }
    }

    static File dump(long streamId, FlightRecorderMXBean bean) throws IOException {
        File f = Utils.createTempFile("stream_" + streamId + "_", ".jfr").toFile();
        try (FileOutputStream fos = new FileOutputStream(f); BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            while (true) {
                byte[] data = bean.readStream(streamId);
                if (data == null) {
                    bos.flush();
                    return f;
                }
                bos.write(data);
            }
        }
    }

    public static List<RecordedEvent> parseStream(long streamId, FlightRecorderMXBean bean) throws Exception {
        File dumpFile = dump(streamId, bean);
        System.out.println("data.length=" + dumpFile.length());
        List<RecordedEvent> events = new ArrayList<>();
        for (RecordedEvent event : RecordingFile.readAllEvents(dumpFile.toPath())) {
            System.out.println("EVENT:" + event);
            events.add(event);
        }
        return events;
    }

    public static void verifyEquals(RecordingInfo ri, Recording r) {
        String destination = r.getDestination() != null ? r.getDestination().toString() : null;
        long maxAge = r.getMaxAge() != null ? r.getMaxAge().getSeconds() : 0;
        long duration = r.getDuration() != null ? r.getDuration().getSeconds() : 0;

        Asserts.assertEquals(destination, ri.getDestination(), "Wrong destination");
        Asserts.assertEquals(r.getDumpOnExit(), ri.getDumpOnExit(), "Wrong dumpOnExit");
        Asserts.assertEquals(duration, ri.getDuration(), "Wrong duration");
        Asserts.assertEquals(r.getId(), ri.getId(), "Wrong id");
        Asserts.assertEquals(maxAge, ri.getMaxAge(), "Wrong maxAge");
        Asserts.assertEquals(r.getMaxSize(), ri.getMaxSize(), "Wrong maxSize");
        Asserts.assertEquals(r.getName(), ri.getName(), "Wrong name");
        Asserts.assertEquals(r.getSize(), ri.getSize(), "Wrong size");
        Asserts.assertEquals(toEpochMillis(r.getStartTime()), ri.getStartTime(), "Wrong startTime");
        Asserts.assertEquals(r.getState().toString(), ri.getState(), "Wrong state");
        Asserts.assertEquals(toEpochMillis(r.getStopTime()), ri.getStopTime(), "Wrong stopTime");

        verifyMapEquals(r.getSettings(), ri.getSettings());
    }

    public static String asString(RecordingInfo r) {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("RecordingInfo:%n"));
        sb.append(String.format("destination=%s%n", r.getDestination()));
        sb.append(String.format("dumpOnExit=%b%n", r.getDumpOnExit()));
        sb.append(String.format("duration=%d%n", r.getDuration()));
        sb.append(String.format("id=%d%n", r.getId()));
        sb.append(String.format("maxAge=%d%n", r.getMaxAge()));
        sb.append(String.format("maxSize=%d%n", r.getMaxSize()));
        sb.append(String.format("getName=%s%n", r.getName()));
        sb.append(String.format("size=%d%n", r.getSize()));
        sb.append(String.format("startTime=%d%n", r.getStartTime()));
        sb.append(String.format("state=%s%n", r.getState()));
        sb.append(String.format("stopTime=%d%n", r.getStopTime()));
        return sb.toString();
    }

    public static String asString(Recording r) {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("Recording:%n"));
        sb.append(String.format("destination=%s%n", r.getDestination()));
        sb.append(String.format("dumpOnExit=%b%n", r.getDumpOnExit()));
        sb.append(String.format("duration=%d%n", r.getDuration().getSeconds()));
        sb.append(String.format("id=%d%n", r.getId()));
        sb.append(String.format("maxAge=%d%n", r.getMaxAge().getSeconds()));
        sb.append(String.format("maxSize=%d%n", r.getMaxSize()));
        sb.append(String.format("getName=%s%n", r.getName()));
        sb.append(String.format("size=%d%n", r.getSize()));
        sb.append(String.format("startTime=%d%n", toEpochMillis(r.getStartTime())));
        sb.append(String.format("state=%s%n", r.getState()));
        sb.append(String.format("stopTime=%d%n", toEpochMillis(r.getStopTime())));
        return sb.toString();
    }

    public static void verifyMapEquals(Map<String, String> a, Map<String, String> b) {
        try {
            Asserts.assertEquals(a.size(), b.size(), "Wrong number of keys");
            for (String key : a.keySet()) {
                Asserts.assertTrue(a.containsKey(key), "Missing key " + key);
                Asserts.assertEquals(a.get(key), b.get(key), "Wrong values for key " + key);
                //System.out.printf("equal: %s=%s%n", key, a.get(key));
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            logMap("a", a);
            logMap("b", b);
            throw e;
        }
    }

    public static void logMap(String name, Map<String, String> map) {
        for (String key : map.keySet()) {
            System.out.printf("map %s: %s=%s%n", name, key, map.get(key));
        }
    }

    private static long toEpochMillis(Instant instant) {
        return instant != null ? instant.toEpochMilli() : 0;
    }

    public static void verifyEventSettingsEqual(EventType javaType, EventTypeInfo jmxType) {
        Map<String, SettingDescriptor> javaSettings = new HashMap<>();
        for (SettingDescriptor settingDescriptor : javaType.getSettingDescriptors()) {
            javaSettings.put(settingDescriptor.getName(), settingDescriptor);
        }
        Asserts.assertFalse(javaSettings.isEmpty(), "No ValueDescriptor for EventType " + javaType.getName());

        for (SettingDescriptorInfo jmxSetting : jmxType.getSettingDescriptors()) {
            final String name = jmxSetting.getName();
            System.out.printf("SettingDescriptorInfo: %s#%s=%s%n", jmxType.getName(), name, jmxSetting.getDefaultValue());
            SettingDescriptor javaSetting = javaSettings.remove(name);
            Asserts.assertNotNull(javaSetting, "No Setting for name " + name);
            Asserts.assertEquals(jmxSetting.getDefaultValue(), Events.getSetting(javaType, name).getDefaultValue(), "Wrong default value");
            Asserts.assertEquals(jmxSetting.getDescription(), javaSetting.getDescription(), "Wrong description");
            Asserts.assertEquals(jmxSetting.getLabel(), javaSetting.getLabel(), "Wrong label");
            Asserts.assertEquals(jmxSetting.getName(), javaSetting.getName(), "Wrong name");
            Asserts.assertEquals(jmxSetting.getTypeName(), javaSetting.getTypeName(), "Wrong type name");
            Asserts.assertEquals(jmxSetting.getContentType(), javaSetting.getContentType());
        }

        // Verify that all Settings have been matched.
        if (!javaSettings.isEmpty()) {
            for (String name : javaSettings.keySet()) {
                System.out.println("Missing setting" + name + " in EventTypeInfo for " + javaType.getName());
            }
            System.out.println();
            System.out.println(javaType.getName() + " Java API");
            System.out.println("===============");
            for (SettingDescriptor v : javaType.getSettingDescriptors()) {
                System.out.println(" - " + v.getName());
            }
            System.out.println();
            System.out.println(jmxType.getName() + " JMX API");
            System.out.println("===============");
            for (SettingDescriptorInfo v : jmxType.getSettingDescriptors()) {
                System.out.println(" - " + v.getName());
            }

            Asserts.fail("Missing setting");
        }
    }


    public static FlightRecorderMXBean getFlighteRecorderMXBean() {
        return ManagementFactory.getPlatformMXBean(FlightRecorderMXBean.class);
    }

    public static long getPID(){
        return ManagementFactory.getRuntimeMXBean().getPid();
    }

    public static FlightRecorderMXBean getFlighteRecorderMXBean(long pid) throws Exception {
        VirtualMachine targetVM = VirtualMachine.attach("" + pid);
        String jmxServiceUrl = targetVM.getAgentProperties().getProperty(LOCAL_CONNECTION_ADDRESS);
        JMXServiceURL jmxURL = new JMXServiceURL(jmxServiceUrl);
        JMXConnector connector = JMXConnectorFactory.connect(jmxURL);
        MBeanServerConnection connection = connector.getMBeanServerConnection();

        ObjectName objectName = new ObjectName("jdk.management.jfr:type=FlightRecorder");
        return JMX.newMXBeanProxy(connection, objectName, FlightRecorderMXBean.class);
    }
}
