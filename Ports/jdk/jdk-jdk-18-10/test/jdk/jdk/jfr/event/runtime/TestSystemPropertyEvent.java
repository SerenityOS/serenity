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

package jdk.jfr.event.runtime;

import java.util.HashMap;
import java.util.Map;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.event.runtime.TestSystemPropertyEvent
 */
public class TestSystemPropertyEvent {

    private final static String EVENT_NAME = EventNames.InitialSystemProperty;

    public static void main(String[] args) throws Throwable {
        Map<String, String> systemProps = createInitialSystemProperties();
        // Recording should only contain properties defined at JVM start.
        // This property should not be included.
        System.setProperty("party.pooper", "buh!");

        Recording recording = new Recording();
        recording.enable(EVENT_NAME);
        recording.start();
        recording.stop();

        Map<String, String> eventProps = new HashMap<>();
        for (RecordedEvent event : Events.fromRecording(recording)) {
            String key = Events.assertField(event, "key").notEmpty().getValue();
            String value = Events.assertField(event, "value").notNull().getValue();
            if (!eventProps.containsKey(key)) {
                // Event is received at both start and end of chunk. Only log first.
                System.out.println("Event:" + event);
            }
            eventProps.put(key, value);
        }
        Asserts.assertGreaterThan(eventProps.size(), 4, "Should have at least 5 events");

        // Value of System.properties may change. We can not expect all values in
        // events and System.getProperties() to be equal.
        // To do some verification of property values we require that at least one
        // property with non-empty value is equal.
        int countEqualAndNonEmpty = 0;

        String missingKeys = "";
        for (String key : eventProps.keySet()) {
            if (!systemProps.containsKey(key)) {
                missingKeys += key + " ";
                continue;
            }
            if (isEqualAndNonEmpty(key, eventProps.get(key), systemProps.get(key))) {
                countEqualAndNonEmpty++;
            }
        }

        if (!missingKeys.isEmpty()) {
            Asserts.fail("Event properties not found in System.properties(): " + missingKeys);
        }
        Asserts.assertTrue(countEqualAndNonEmpty > 0, "No property had expected value");
    }

    private static boolean isEqualAndNonEmpty(String key, String eventValue, String systemValue) {
        if (eventValue == null || systemValue == null || eventValue.isEmpty() || systemValue.isEmpty()) {
            return false;
        }
        boolean isEquals = eventValue.equals(systemValue);
        System.out.printf("eq=%b, key='%s', event='%s', system='%s'%n", isEquals, key, eventValue, systemValue);
        return isEquals;
    }

    private static Map<String, String> createInitialSystemProperties() {
        Map<String, String> result = new HashMap<>();
        for (Object keyObject : System.getProperties().keySet()) {
            String key = (String) keyObject;
            result.put(key, System.getProperty(key));
            System.out.println("initialProp: " + key);
        }
        return result;
    }
}
