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

import java.util.ArrayList;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.internal.test.WhiteBox;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @modules jdk.jfr/jdk.jfr.internal.test
 * @run main/othervm -XX:TLABSize=2k jdk.jfr.event.oldobject.TestArrayInformation
 */
public class TestArrayInformation {

    private static class ArrayLeak {
    }

    private static final int CHAIN_DEPTH = 50;
    private static final int ARRAY_SIZE = 52;
    private static final int ARRAY_INDEX = 26;

    public static List<Object> leak = new ArrayList<>(25);

    public static void main(String[] args) throws Exception {
        WhiteBox.setWriteAllObjectSamples(true);

        try (Recording recording = new Recording()) {
            recording.enable(EventNames.OldObjectSample).withoutStackTrace().with("cutoff", "infinity");
            recording.start();
            for(int i = 0; i < 25; i++) {
              leak.add( buildNestedArray(CHAIN_DEPTH));
            }
            recording.stop();
            List<RecordedEvent> events = Events.fromRecording(recording);
            Events.hasEvents(events);
            verifyObjectArray(events);
        }
    }

    private static void verifyObjectArray(List<RecordedEvent> events) throws Exception {
        for (RecordedEvent e : events) {
            RecordedObject object = e.getValue("object");
            RecordedClass objectType = object.getValue("type");
            RecordedObject referrer = object.getValue("referrer");
            System.out.println(objectType.getName());
            if (objectType.getName().equals(ArrayLeak[].class.getName())) {
                for (int i = 0; i < CHAIN_DEPTH; i++) {
                    object = referrer.getValue("object");
                    if (object == null) {
                        throw new Exception("Expected referrer object");
                    }
                    objectType = object.getValue("type");
                    if (!objectType.getName().equals(Object[].class.getName())) {
                        throw new Exception("Expect array class to be named " + Object[].class + " but found " + objectType.getName());
                    }
                    RecordedObject field = referrer.getValue("field");
                    if (field != null) {
                        throw new Exception("Didn't expect to find field");
                    }
                    RecordedObject array = referrer.getValue("array");
                    if (array == null) {
                        throw new Exception("Expected array object, but got null");
                    }
                    int index = referrer.getValue("array.index");
                    if (index != ARRAY_INDEX) {
                        throw new Exception("Expected array index: " + ARRAY_INDEX + ", but got " + index);
                    }
                    int size = referrer.getValue("array.size");
                    if (size != ARRAY_SIZE) {
                        throw new Exception("Expected array size: " + ARRAY_SIZE + ", but got " + size);
                    }
                    referrer = object.getValue("referrer");
                }
                return;
            }
        }
        throw new Exception("Could not find event with " + ArrayLeak[].class + " as (leak) object");
    }

    private static Object buildNestedArray(int depth) {
        if (depth > 0) {
            Object[] array = new Object[ARRAY_SIZE];
            array[ARRAY_INDEX] = buildNestedArray(depth - 1);
            return array;
        } else {
            return new ArrayLeak[OldObjects.MIN_SIZE];
        }
    }

}
