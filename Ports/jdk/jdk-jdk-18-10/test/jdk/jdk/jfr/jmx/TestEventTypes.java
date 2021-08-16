/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.SettingDescriptor;
import jdk.management.jfr.EventTypeInfo;
import jdk.management.jfr.FlightRecorderMXBean;
import jdk.management.jfr.SettingDescriptorInfo;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @summary Verifies that EventTypes from jmx and FlightRecorder are the same.
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.TestEventTypes
 */
public class TestEventTypes {
    public static void main(String[] args) throws Exception {
        FlightRecorderMXBean bean = JmxHelper.getFlighteRecorderMXBean();
        FlightRecorder jfr = FlightRecorder.getFlightRecorder();

        Recording r = new Recording();
        r.enable(MyEvent.class);
        new MyEvent(); // triggers <clinit>
        List<EventTypeInfo> infos = bean.getEventTypes();
        List<EventType> types = jfr.getEventTypes();
        Asserts.assertFalse(infos.isEmpty(), "No EventTypeInfos found");
        verifyMyEventType(infos);
        assertSame(infos, types);
        r.close();
    }

    @Name("MyEvent.name")
    @Label("MyEvent.label")
    @Description("MyEvent.description")
    private static class MyEvent extends Event {
        @Label("MyEvent.message")
        public String message;
    }

    private static void verifyMyEventType(List<EventTypeInfo> infos) {
        for (EventTypeInfo info : infos) {
            if ("MyEvent.name".equals(info.getName())) {
                System.out.println("EventTypeInfo for MyEvent: " + info);
                Asserts.assertEquals("MyEvent.label", info.getLabel());
                Asserts.assertEquals("MyEvent.description", info.getDescription());
                for (SettingDescriptorInfo si : info.getSettingDescriptors()) {
                    System.out.println("si=" + si);
                }
                return;
            }
        }
        Asserts.fail("Missing EventTypeInfo for MyEvent");
    }

    private static void assertSame(List<EventTypeInfo> infos, List<EventType> types) {
        List<Long> ids = new ArrayList<>();
        for (EventTypeInfo info : infos) {
            long id = info.getId();
            Asserts.assertFalse(ids.contains(id), "EventTypeInfo.id not unique:" + id);
            ids.add(id);
            boolean isFound = false;
            for (EventType type : types) {
                if (type.getId() == id) {
                    assertSame(info, type);
                    isFound = true;
                    break;
                }
            }
            if (!isFound) {
                String msg = "No EventType for EventTypeInfo";
                System.out.println(msg + ": " + info);
                Asserts.fail(msg);
            }
        }
        Asserts.assertEquals(infos.size(), types.size(), "Number of EventTypeInfos != EventTypes");
    }

    private static void assertSame(EventTypeInfo ti, EventType t) {
        try {
            Asserts.assertEquals(ti.getId(), t.getId(), "Wrong id");
            Asserts.assertEquals(ti.getName(), t.getName(), "Wrong name");
            Asserts.assertEquals(ti.getLabel(), t.getLabel(), "Wrong label");
            Asserts.assertEquals(ti.getDescription(), t.getDescription(), "Wrong description");
            Asserts.assertEquals(ti.getCategoryNames(), t.getCategoryNames(), "Wrong category names");

            for (SettingDescriptorInfo si : ti.getSettingDescriptors()) {
                String settingName = si.getName();
                boolean isFound = false;
                for (SettingDescriptor d : t.getSettingDescriptors()) {
                    if (settingName.equals(d.getName())) {
                        assertSame(si, d, t);
                        isFound = true;
                        break;
                    }
                }
                if (!isFound) {
                    Asserts.fail("No ValueDescriptor for SettingDescriptorInfo: " + si);
                }
            }
        } catch (Exception e) {
            System.out.printf("EventTypeInfo != EventType%nEventTypeInfo=%s%nEventType=%s%n", ti, t);
            throw e;
        }
    }

    private static void assertSame(SettingDescriptorInfo si, SettingDescriptor d, EventType type) {
        try {
            Asserts.assertEquals(si.getName(), d.getName(), "Wrong name");
            Asserts.assertEquals(si.getLabel(), d.getLabel(), "Wrong label");
            Asserts.assertEquals(si.getTypeName(), d.getTypeName(), "Wrong typeName");
            Asserts.assertEquals(si.getDescription(), d.getDescription(), "Wrong description");
            String typeDefaultValue = Events.getSetting(type, si.getName()).getDefaultValue();
            Asserts.assertEquals(si.getDefaultValue(), typeDefaultValue, "Wrong defaultValue");
        } catch (Exception e) {
            System.out.printf("SettingDescriptorInfo != SettingDescriptor=%s%nValueDescriptor=%s%n", si, d);
            throw e;
        }
    }
}
