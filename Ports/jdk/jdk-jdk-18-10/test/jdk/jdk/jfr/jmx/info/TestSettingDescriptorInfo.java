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
import jdk.test.lib.Asserts;

/**
 * @test
 * @key jfr
 * @summary Test for SettingDescriptorInfo. Compare infos from java API and jmx API.
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.info.TestSettingDescriptorInfo
 */
public class TestSettingDescriptorInfo {
    public static void main(String[] args) throws Throwable {

        Map<String, EventType> javaTypes = new HashMap<String, EventType>();
        for (EventType t : FlightRecorder.getFlightRecorder().getEventTypes()) {
            javaTypes.put(t.getName(), t);
        }

        List<EventTypeInfo> jmxTypes =JmxHelper.getFlighteRecorderMXBean().getEventTypes();
        Asserts.assertFalse(jmxTypes.isEmpty(), "No EventTypes found in jmx api");

        for (EventTypeInfo jmxType : jmxTypes) {
            final String name = jmxType.getName();
            EventType javaType = javaTypes.remove(name);
            Asserts.assertNotNull(javaType, "No EventType for name " + name);
            JmxHelper.verifyEventSettingsEqual(javaType, jmxType);
        }

        // Verify that all EventTypes have been matched.
        if (!javaTypes.isEmpty()) {
            for (String name : javaTypes.keySet()) {
                System.out.println("Found extra EventType that is not available using JMX " + name);
            }
            Asserts.fail("Found extra EventType");
        }
    }

}
