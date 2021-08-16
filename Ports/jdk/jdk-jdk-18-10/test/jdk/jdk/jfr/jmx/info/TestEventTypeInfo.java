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

package jdk.jfr.jmx.info;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.jfr.jmx.JmxHelper;

import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.management.jfr.EventTypeInfo;
import jdk.management.jfr.FlightRecorderMXBean;
import jdk.test.lib.Asserts;

/**
 * @test
 * @key jfr
 * @summary Test for EventTypeInfo
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.info.TestEventTypeInfo
 */
public class TestEventTypeInfo {
    public static void main(String[] args) throws Throwable {
        FlightRecorder jfr = FlightRecorder.getFlightRecorder();

        FlightRecorderMXBean bean = JmxHelper.getFlighteRecorderMXBean();
        List<EventTypeInfo> typeInfos = bean.getEventTypes();

        Map<String, EventType> types = new HashMap<>();
        for (EventType type : jfr.getEventTypes()) {
            types.put(type.getName(), type);
        }

        Asserts.assertFalse(typeInfos.isEmpty(), "No EventTypeInfos found");
        Asserts.assertFalse(types.isEmpty(), "No EventTypes found");

        for (EventTypeInfo typeInfo : typeInfos) {
            final String key = typeInfo.getName();
            System.out.println("EventType name = " + key);
            EventType type = types.get(key);
            Asserts.assertNotNull(type, "No EventType for name " + key);
            types.remove(key);

            Asserts.assertEquals(typeInfo.getCategoryNames(), type.getCategoryNames(), "Wrong category");
            Asserts.assertEquals(typeInfo.getDescription(), type.getDescription(), "Wrong description");
            Asserts.assertEquals(typeInfo.getId(), type.getId(), "Wrong id");
            Asserts.assertEquals(typeInfo.getLabel(), type.getLabel(), "Wrong label");
            Asserts.assertEquals(typeInfo.getName(), type.getName(), "Wrong name");

            JmxHelper.verifyEventSettingsEqual(type, typeInfo);
        }

        // Verify that all EventTypes have been matched.
        if (!types.isEmpty()) {
            for (String name : types.keySet()) {
                System.out.println("Found extra EventType with name " + name);
            }
            Asserts.fail("Found extra EventTypes");
        }
    }

}
