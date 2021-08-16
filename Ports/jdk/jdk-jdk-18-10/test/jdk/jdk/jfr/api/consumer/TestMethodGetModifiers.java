/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

import static jdk.test.lib.Asserts.assertNotNull;

import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.SimpleEvent;


/**
 * @test
 * @summary Verifies that a recorded method has the correct modifier
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm -Xint jdk.jfr.api.consumer.TestMethodGetModifiers
 */
public final class TestMethodGetModifiers {

    public static void main(String[] args) throws Throwable {
        try (Recording recording = new Recording()) {
            recording.start();

            SimpleEvent ev = new SimpleEvent();
            ev.commit();
            recording.stop();

            List<RecordedEvent> recordedEvents = Events.fromRecording(recording);
            Events.hasEvents(recordedEvents);
            RecordedEvent recordedEvent = recordedEvents.get(0);

            System.out.println(recordedEvent);

            RecordedStackTrace stacktrace = recordedEvent.getStackTrace();
            List<RecordedFrame> frames = stacktrace.getFrames();
            for (RecordedFrame frame : frames) {
                RecordedMethod method = frame.getMethod();
                if (method.getName().equals("main")) {
                    System.out.println("'main' method: " + method);
                    int modifiers = TestMethodGetModifiers.class.getDeclaredMethod("main", (Class<?>)String[].class).getModifiers();
                    System.out.println("modifiers: " + modifiers);
                    Asserts.assertEquals(method.getModifiers(), modifiers, "Incorrect method modifier reported");
                    RecordedClass type = method.getType();
                    assertNotNull(type, "Recorded class can not be null");
                }
            }
        }
    }
}
