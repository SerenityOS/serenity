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

package jdk.jfr.api.consumer;

import java.util.List;

import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Recording;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;


/**
 * @test
 * @summary Verifies that the recorded value descriptors are correct
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm  jdk.jfr.api.consumer.TestValueDescriptorRecorded
 */
public class TestValueDescriptorRecorded {

    private static class MyEvent extends Event {
        @Label("myLabel")
        @Description("myDescription")
        int myValue;
    }

    public static void main(String[] args) throws Throwable {
        try (Recording r = new Recording()) {
            r.enable(MyEvent.class).withoutStackTrace();
            r.start();
            MyEvent event = new MyEvent();
            event.commit();
            r.stop();

            List<RecordedEvent> events = Events.fromRecording(r);
            Events.hasEvents(events);
            RecordedEvent recordedEvent = events.get(0);
            for (ValueDescriptor desc : recordedEvent.getFields()) {
                if ("myValue".equals(desc.getName())) {
                    Asserts.assertEquals(desc.getLabel(), "myLabel");
                    Asserts.assertEquals(desc.getDescription(), "myDescription");
                    Asserts.assertEquals(desc.getTypeName(), int.class.getName());
                    Asserts.assertFalse(desc.isArray());
                    Asserts.assertNull(desc.getContentType());
                }
            }
        }
    }
}
