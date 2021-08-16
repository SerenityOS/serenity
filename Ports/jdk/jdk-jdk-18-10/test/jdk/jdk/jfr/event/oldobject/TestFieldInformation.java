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

package jdk.jfr.event.oldobject;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.internal.test.WhiteBox;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @modules jdk.jfr/jdk.jfr.internal.test
 * @run main/othervm -XX:TLABSize=2k -Xlog:gc+tlab=trace jdk.jfr.event.oldobject.TestFieldInformation
 */
public class TestFieldInformation {

    public static final Object[] testField = new Object[50];

    public static void main(String[] args) throws Exception {
        WhiteBox.setWriteAllObjectSamples(true);

        try (Recording recording = new Recording()) {
            recording.enable(EventNames.OldObjectSample).withoutStackTrace().with("cutoff", "infinity");
            recording.start();

            addToTestField();

            recording.stop();

            List<RecordedEvent> events = Events.fromRecording(recording);
            Events.hasEvents(events);
            for (RecordedEvent e : events) {
                if (hasValidField(e)) {
                    return;
                }
            }
            System.out.println(events);
            Asserts.fail("Could not find old object with field 'testField'");
        }
    }

    private static boolean hasValidField(RecordedEvent e) throws Exception {
        RecordedObject object = e.getValue("object");
        Set<Long> visited = new HashSet<>();
        while (object != null) {
            Long address = object.getValue("address");
            if (visited.contains(address)) {
                return false;
            }
            visited.add(address);
            RecordedObject referrer = object.getValue("referrer");
            RecordedObject fieldObject = referrer != null ? referrer.getValue("field") : null;
            if (fieldObject != null) {
                String name = fieldObject.getValue("name");
                if (name != null && name.equals("testField")) {
                    int modifiers = (short) fieldObject.getValue("modifiers");
                    if (!Modifier.isStatic(modifiers)) {
                        throw new Exception("Field should be static");
                    }
                    if (!Modifier.isPublic(modifiers)) {
                        throw new Exception("Field should be private");
                    }
                    if (!Modifier.isFinal(modifiers)) {
                        throw new Exception("Field should be final");
                    }
                    if (Modifier.isTransient(modifiers)) {
                        throw new Exception("Field should not be transient");
                    }
                    if (Modifier.isVolatile(modifiers)) {
                        throw new Exception("Field should not be volatile");
                    }
                    return true;
                }
            }
            object = referrer != null ? referrer.getValue("object") : null;
        }
        return false;
    }

    private static void addToTestField() {
        for (int i = 0; i < testField.length; i++) {
            // Allocate array to trigger sampling code path for interpreter / c1
            testField[i] = new Object[1_000_000];
        }
    }
}
